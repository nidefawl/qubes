package nidefawl.qubes.perf;

import static org.lwjgl.opengl.GL15.glGenQueries;
import static org.lwjgl.opengl.GL33.GL_TIMESTAMP;
import static org.lwjgl.opengl.GL33.glQueryCounter;

import java.util.ArrayList;

import nidefawl.qubes.util.Pool;

@SuppressWarnings("unused")
public class GPUProfiler {

    public static final boolean PROFILING_ENABLED = true;

    private static Pool<GPUTaskProfile> taskPool;
    private static ArrayList<Integer>   queryObjects;

    private static int frameCounter;

    private static GPUTaskProfile currentTask;

    private static ArrayList<GPUTaskProfile> completedFrames;

    private static int perFrame;

    private static int frameCount;
    static boolean     DISABLE_FRAME = !PROFILING_ENABLED;

    static {
        taskPool = new Pool<GPUTaskProfile>(200) {
            @Override
            public GPUTaskProfile create() {
                return new GPUTaskProfile();
            }
        };
        queryObjects = new ArrayList<>();

        frameCounter = 0;

        completedFrames = new ArrayList<>();
    }

    public static void startFrame() {
        if (frameCount > 0 && frameCount > taskPool.getFree()) {
            System.err.println("pool has not enough slots free (free: " + taskPool.getFree() + ", required: " + frameCount + ")");
            DISABLE_FRAME = true;
            return;
        }
        DISABLE_FRAME = false;

        if (currentTask != null) {
            throw new IllegalStateException("Previous frame not ended properly!");
        }
        if (PROFILING_ENABLED) {

            perFrame = 1;
            currentTask = taskPool.get().init(null, "Frame " + (++frameCounter), getQuery());
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
            if (currentTask.getParent() != null) {
                throw new IllegalStateException("Error ending frame. Not all tasks finished (parent: " + currentTask.getParent() + ").");
            }
            currentTask.end(getQuery());

            if (completedFrames.size() < 55) {
                completedFrames.add(currentTask);
            } else {
                recycle(currentTask);
            }

            currentTask = null;
        }
    }

    public static GPUTaskProfile getFrameResults() {
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