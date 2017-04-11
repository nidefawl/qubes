package nidefawl.qubes.perf;

import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.opengl.GL15.GL_QUERY_RESULT;
import static org.lwjgl.opengl.GL15.GL_QUERY_RESULT_AVAILABLE;
import static org.lwjgl.opengl.GL15.glGetQueryObjectui;
import static org.lwjgl.opengl.GL33.GL_TIMESTAMP;
import static org.lwjgl.opengl.GL33.glGetQueryObjectui64;
import static org.lwjgl.opengl.GL33.glQueryCounter;
import static org.lwjgl.vulkan.VK10.*;

import java.util.ArrayList;
import java.util.List;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.util.Poolable;
import nidefawl.qubes.vulkan.VkQueryPool;

public class GPUTaskProfile implements Poolable {

    protected GPUTaskProfile parent;

    protected String name;
    
    protected int queries;

    protected ArrayList<GPUTaskProfile> children = new ArrayList<>(100);
    public long resultStart;
    public long resultEnd;

    protected VkQueryPool pool;

    public GPUTaskProfile() {

    }

    public GPUTaskProfile init(GPUTaskProfile parent, String name) {
        if (Engine.isVulkan) {
            this.pool = GPUProfiler.currentPool;
            this.queries = this.pool.getQueries(2);
        } else {
            this.queries = GPUProfiler.getQueries();
        }

        this.parent = parent;
        this.name = name;

        if (parent != null) {
            parent.addChild(this);
        }
        query(0);
        return this;
    }

    private void addChild(GPUTaskProfile profilerTask) {
        children.add(profilerTask);
    }
    
    public void query(int idx) {

        if (!Engine.isVulkan) {
            glQueryCounter(this.queries+idx, GL_TIMESTAMP);
        } else {
            this.pool.query(Engine.getDrawCmdBuffer(), VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, this.queries+idx);
        }
    }

    public GPUTaskProfile end() {
        query(1);
        return parent;
    }

    public GPUTaskProfile getParent() {
        return parent;
    }

    public boolean resultsAvailable() {
        return glGetQueryObjectui(queries+1, GL_QUERY_RESULT_AVAILABLE) == GL_TRUE;
    }

    public String getName() {
        return name;
    }

    public long getStartTime() {
        return this.resultStart;
    }

    public long getEndTime() {
        return this.resultEnd;
    }

    public long getTimeTaken() {
        return getEndTime() - getStartTime();
    }

    public ArrayList<GPUTaskProfile> getChildren() {
        return children;
    }

    @Override
    public void reset() {
        this.pool = null;
        queries = -1;
        resultStart = resultEnd = -1L;
        children.clear();
    }

    public void dump(List<String> to) {
        dump(to, 0, 0);
    }

    public String dumpSingle(float fFrame) {
        String s = "";
        float f = getTimeTaken() / 1000 / 1000f;
        s+= name + " : " + f + "ms";
        if (this.parent == null) {
            s+=" ("+(int)(1000.0f/f)+")";
        } else {
            float percent = (f/fFrame)*100.0f;
            String prefix = " ";
            if (percent > 20) {
                prefix = " \0udd7733";
            }
            s+= prefix + String.format("(%.2f%%)", percent);
        }
        return s;
    }
    private void dump(List<String> to, int indentation, float fFrame) {
        String s = "";
        for (int i = 0; i < indentation; i++) {
            s+="  ";
        }
        GPUProfiler.queryResult(this);
        float f = getTimeTaken() / 1000 / 1000f;
        s+= name + " : " + f + "ms";
        if (this.parent == null) {
            s+=" ("+(int)(1000.0f/f)+")";
        } else {
            float percent = (f/fFrame)*100.0f;
            String prefix = " ";
            if (percent > 20) {
                prefix = " \0udd7733";
            }
            s+= prefix + String.format("(%.2f%%)", percent);
            if (percent > 20) {
                s+= "\0\0";
            }
        }
        to.add(s);
        for (int i = 0; i < children.size(); i++) {
            children.get(i).dump(to, indentation + 1, this.parent == null ? f : fFrame);
        }
    }
}