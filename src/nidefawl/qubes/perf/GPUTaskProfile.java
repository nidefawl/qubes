package nidefawl.qubes.perf;

import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.opengl.GL15.GL_QUERY_RESULT;
import static org.lwjgl.opengl.GL15.GL_QUERY_RESULT_AVAILABLE;
import static org.lwjgl.opengl.GL15.glGetQueryObjectui;
import static org.lwjgl.opengl.GL33.glGetQueryObjectui64;

import java.util.ArrayList;
import java.util.List;

import nidefawl.qubes.util.Poolable;

public class GPUTaskProfile implements Poolable {

    private GPUTaskProfile parent;

    private String name;

    private int startQuery, endQuery;

    private ArrayList<GPUTaskProfile> children = new ArrayList<>(100);

    public GPUTaskProfile() {

    }

    public GPUTaskProfile init(GPUTaskProfile parent, String name, int startQuery) {

        this.parent = parent;
        this.name = name;
        this.startQuery = startQuery;

        if (parent != null) {
            parent.addChild(this);
        }

        return this;
    }

    private void addChild(GPUTaskProfile profilerTask) {
        children.add(profilerTask);
    }

    public GPUTaskProfile end(int endQuery) {
        this.endQuery = endQuery;
        return parent;
    }

    public GPUTaskProfile getParent() {
        return parent;
    }

    public boolean resultsAvailable() {
        return glGetQueryObjectui(endQuery, GL_QUERY_RESULT_AVAILABLE) == GL_TRUE;
    }

    public String getName() {
        return name;
    }

    public int getStartQuery() {
        return startQuery;
    }

    public int getEndQuery() {
        return endQuery;
    }

    public long getStartTime() {
        return glGetQueryObjectui64(startQuery, GL_QUERY_RESULT);
    }

    public long getEndTime() {
        return glGetQueryObjectui64(endQuery, GL_QUERY_RESULT);
    }

    public long getTimeTaken() {
        return getEndTime() - getStartTime();
    }

    public ArrayList<GPUTaskProfile> getChildren() {
        return children;
    }

    @Override
    public void reset() {
        startQuery = -1;
        endQuery = -1;
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