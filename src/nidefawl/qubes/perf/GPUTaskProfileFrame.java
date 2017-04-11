package nidefawl.qubes.perf;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.vulkan.PostRenderTask;

public class GPUTaskProfileFrame extends GPUTaskProfile implements PostRenderTask {


    public GPUTaskProfileFrame init(String name) {
        if (Engine.isVulkan) {
            this.pool = GPUProfiler.currentPool;
            this.queries = this.pool.getQueries(2);
        } else {
            this.queries = GPUProfiler.getQueries();
        }
        this.parent = null;
        this.name = name;
        query(0);
        return this;
    }
    
    @Override
    public void onComplete() {
        GPUProfiler.completedFrames.add(this);
    }

}