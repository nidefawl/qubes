package nidefawl.qubes.perf;

import static org.lwjgl.opengl.GL15.glGenQueries;
import static org.lwjgl.opengl.GL33.GL_TIMESTAMP;
import static org.lwjgl.opengl.GL33.glQueryCounter;

import java.util.ArrayList;

import nidefawl.qubes.util.Pool;

@SuppressWarnings("unused")
public class GPUProfiler {

    public static final boolean PROFILING_ENABLED = false;

    private static Pool<GPUTaskProfile> taskPool;
    private static Pool<GPUTaskProfileFrame> taskPoolFrame;
    private static ArrayList<Integer>   queryObjects;

    private static int frameCounter;

    private static GPUTaskProfileFrame currentFrame;
    private static GPUTaskProfile currentTask;

    private static ArrayList<GPUTaskProfileFrame> completedFrames;

    private static int perFrame;

    private static int frameCount;
    static boolean     DISABLE_FRAME = !PROFILING_ENABLED;

    static {
        taskPool = new Pool<GPUTaskProfile>(400) {
            @Override
            public GPUTaskProfile create() {
                return new GPUTaskProfile();
            }
        };
        taskPoolFrame = new Pool<GPUTaskProfileFrame>(400) {
            @Override
            public GPUTaskProfileFrame create() {
                return new GPUTaskProfileFrame();
            }
        };
        queryObjects = new ArrayList<>();

        frameCounter = 0;

        completedFrames = new ArrayList<>();
    }

    public static void startFrame() {
        if (frameCount > 0 && frameCount > taskPoolFrame.getFree()) {
            System.err.println("pool has not enough slots free (free: " + taskPool.getFree() + ", required: " + frameCount + ")");
            DISABLE_FRAME = true;
            return;
        }
        DISABLE_FRAME = false;

        if (currentFrame != null) {
            throw new IllegalStateException("Previous frame not ended properly!");
        }
        if (PROFILING_ENABLED) {

            perFrame = 1;
            currentFrame = taskPoolFrame.get().init("Frame " + (++frameCounter), getQuery());
            currentTask = currentFrame;
        }
    }

    public static void start(String name) {
        if (DISABLE_FRAME)
            return;
        if (PROFILING_ENABLED && currentTask != null) {
            perFrame++;
            currentTask = taskPool.get().init(currentTask, name, getQuery());
        }
    }

    public static void end() {
        if (DISABLE_FRAME)
            return;
        if (PROFILING_ENABLED && currentTask != null) {
            currentTask = currentTask.end(getQuery());
        }

    }

    public static void endFrame() {
        if (DISABLE_FRAME)
            return;
        if (frameCount == 0) {
            frameCount = perFrame;
        }
        if (PROFILING_ENABLED) {
            if (currentFrame == null) {
                throw new IllegalStateException("frame not started properly!");
            }
            if (currentTask.getParent() != null) {
                throw new IllegalStateException("Error ending frame. Not all tasks finished (parent: " + currentTask.getParent() + ").");
            }
            currentFrame.end(getQuery());

            completedFrames.add(currentFrame);
            currentFrame = null;
            currentTask = null;
        }
    }

    public static GPUTaskProfileFrame getFrameResults() {
        if (completedFrames.isEmpty()) {
            return null;
        }

        GPUTaskProfile frame = completedFrames.get(0);
        if (frame.resultsAvailable()) {
            return completedFrames.remove(0);
        } else {
            return null;
        }
    }
    public static void recycleFrame(GPUTaskProfileFrame task) {
        queryObjects.add(task.getStartQuery());
        queryObjects.add(task.getEndQuery());
        ArrayList<GPUTaskProfile> children = task.getChildren();
        for (int i = 0; i < children.size(); i++) {
            recycle(children.get(i));
        }
        taskPoolFrame.recycle(task);
    }

    public static void recycle(GPUTaskProfile task) {
        queryObjects.add(task.getStartQuery());
        queryObjects.add(task.getEndQuery());
        ArrayList<GPUTaskProfile> children = task.getChildren();
        for (int i = 0; i < children.size(); i++) {
            recycle(children.get(i));
        }
        taskPool.recycle(task);
    }

    private static int getQuery() {
        int query;
        if (!queryObjects.isEmpty()) {
            query = queryObjects.remove(queryObjects.size() - 1);
        } else {
            query = glGenQueries();
        }

        glQueryCounter(query, GL_TIMESTAMP);

        return query;
    }
}