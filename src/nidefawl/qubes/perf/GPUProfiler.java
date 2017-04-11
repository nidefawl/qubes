package nidefawl.qubes.perf;

import static org.lwjgl.opengl.GL15.GL_QUERY_RESULT;
import static org.lwjgl.opengl.GL15.glGenQueries;
import static org.lwjgl.opengl.GL33.GL_TIMESTAMP;
import static org.lwjgl.opengl.GL33.glGetQueryObjectui64;
import static org.lwjgl.opengl.GL33.glQueryCounter;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkQueryPoolCreateInfo;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Memory;
import nidefawl.qubes.util.GameLogicError;
import nidefawl.qubes.util.Pool;
import nidefawl.qubes.vulkan.*;

@SuppressWarnings("unused")
public class GPUProfiler {

    public static final boolean PROFILING_ENABLED = false;

    private static Pool<GPUTaskProfile> taskPool;
    private static Pool<GPUTaskProfileFrame> taskPoolFrame;
    private static ArrayList<Integer>   queryObjects;

    private static int frameCounter;

    private static GPUTaskProfileFrame currentFrame;
    private static GPUTaskProfile currentTask;

    static ArrayList<GPUTaskProfileFrame> completedFrames;

    private static int perFrame;

    private static int frameCount;
    static boolean     DISABLE_FRAME = !PROFILING_ENABLED;

    private static LongBuffer buffer;

    private static IntBuffer ids;

    static VkQueryPool currentPool;

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
        if (Engine.isVulkan) {
            currentPool = Engine.vkContext.getFreeQueryPool();
            if (currentPool.inUse) {
                throw new IllegalStateException("currentPool.inUse");
            }
            currentPool.reset(Engine.getDrawCmdBuffer());
            currentPool.inUse = true;
        }
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
            currentFrame = taskPoolFrame.get().init("Frame " + (++frameCounter));
            currentTask = currentFrame;
        }
    }

    public static void start(String name) {
//        System.out.println("START "+name);
        if (DISABLE_FRAME)
            return;
        if (PROFILING_ENABLED && currentTask != null) {
            perFrame++;
            currentTask = taskPool.get().init(currentTask, name);
        }
    }

    public static void end() {
        if (DISABLE_FRAME)
            return;
        if (PROFILING_ENABLED && currentTask != null) {
            currentTask = currentTask.end();
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
            currentFrame.end();
            if (Engine.isVulkan) {
                Engine.getDrawCmdBuffer().addPostRenderTask(currentFrame);
            } else {
                completedFrames.add(currentFrame);
            }
            currentFrame = null;
            currentTask = null;
        }
    }

    public static GPUTaskProfileFrame getFrameResults() {
        if (completedFrames.isEmpty()) {
            return null;
        }

        GPUTaskProfileFrame frame = completedFrames.get(0);
        if (frame == null) {
            throw new IllegalStateException("frame is null, list is supposed to be empty: "+completedFrames);
        }
        if (Engine.isVulkan) {
            if (frame.pool==currentPool)
                return null;
            int err = vkGetQueryPoolResults(Engine.vkContext.device, frame.pool.get(), 0, frame.pool.getPos(), buffer, 0L, VK_QUERY_RESULT_WITH_AVAILABILITY_BIT);
            if (err == VK_NOT_READY) {
                System.out.println("NOT AVAIL");
                return null;
            }
            if (err != VK_SUCCESS) {
                throw new AssertionError("vkGetQueryPoolResults failed: " + VulkanErr.toString(err));
            }            
            for (int j = 0; j < frame.pool.getPos(); j++) {
                if (buffer.get(j) == 0L) {
                    return null;
                }
            }
            return completedFrames.remove(0);
        } else {
            if (frame.resultsAvailable()) {
                return completedFrames.remove(0);
            } 
        }
        return null;
    }
    public static void recycleFrame(GPUTaskProfileFrame task) {
        if (!Engine.isVulkan)
            queryObjects.add(task.queries);
        else {
            task.pool.inUse = false;
        }
        ArrayList<GPUTaskProfile> children = task.getChildren();
        for (int i = 0; i < children.size(); i++) {
            recycle(children.get(i));
        }
        taskPoolFrame.recycle(task);
        
    }

    static void recycle(GPUTaskProfile task) {
        if (!Engine.isVulkan)
            queryObjects.add(task.queries);
        ArrayList<GPUTaskProfile> children = task.getChildren();
        for (int i = 0; i < children.size(); i++) {
            recycle(children.get(i));
        }
        taskPool.recycle(task);
    }

    static int getQueries() {
        int queries;
        if (!queryObjects.isEmpty()) {
            queries = queryObjects.remove(queryObjects.size() - 1);
        } else {
            glGenQueries(ids);
            queries = ids.get(0);
        }
        return queries;
    }

    public static void initPool() {
        buffer = MemoryUtil.memCallocLong(512);
        ids = Memory.createIntBufferGC(2);
    }

    public static void queryResult(GPUTaskProfile gpuTaskProfile) {


        if (Engine.isVulkan) {
            if (gpuTaskProfile.queries>=gpuTaskProfile.pool.getPos()) {
                throw new GameLogicError("Invalid pos "+gpuTaskProfile.pool.getPos()+" vs "+gpuTaskProfile.queries);
            }
            vkGetQueryPoolResults(Engine.vkContext.device, gpuTaskProfile.pool.get(), gpuTaskProfile.queries, 2, buffer, 0L, VK_QUERY_RESULT_WITH_AVAILABILITY_BIT);
            gpuTaskProfile.resultStart = buffer.get(0);
            gpuTaskProfile.resultEnd = buffer.get(1);
        } else {
            gpuTaskProfile.resultStart = glGetQueryObjectui64(gpuTaskProfile.queries+0, GL_QUERY_RESULT);
            gpuTaskProfile.resultEnd = glGetQueryObjectui64(gpuTaskProfile.queries+1, GL_QUERY_RESULT);
        }
    }
}