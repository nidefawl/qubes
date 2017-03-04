package nidefawl.qubes.render.gui;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.util.ITess;
import nidefawl.qubes.vec.Vector3f;

public class LineGUI {
    static class LineSegment {
        int rgbStart, rgbEnd;
        float alphaStart, alphaEnd;
        Vector3f start = new Vector3f();
        Vector3f end = new Vector3f();
    }
	public final static LineGUI INST = new LineGUI();
	LineSegment[] segments = new LineSegment[32];
	
	int nSegment;
    private float width;
	public LineGUI() {
	    for (int i = 0; i < segments.length; i++) {
	        segments[i] = new LineSegment();
	    }
	    r();
	}
    public static void reset() {
        INST.r();
    }
	public void r() {
	    nSegment = 0;
    }
    public void start(float f) {
        r();
        width=f;
    }
    public void add(float x, float y, float z, int rgb, float alpha) {
        if (INST.nSegment == 0) {
            segments[0].start.set(x, y, z);
            segments[0].rgbStart = rgb;
            segments[0].alphaStart = alpha;
        } else {
            segments[nSegment-1].end.set(x, y, z);
            segments[nSegment-1].rgbEnd = rgb;
            segments[nSegment-1].alphaEnd = alpha;
            segments[nSegment].start.set(x, y, z);
            segments[nSegment].rgbStart = rgb;
            segments[nSegment].alphaStart = alpha;
        }
        nSegment++;
    }

    public void drawLines() {
        int n = 0;
        ITess tess = Engine.getTess();
        Vector3f z = Vector3f.pool(0, 0, 1);
        for (int i = 0; i < nSegment - 1; i++) {
            LineSegment s = segments[i];
            Vector3f dir = Vector3f.pool();
            Vector3f.sub(s.end, s.start, dir);
            Vector3f perp = Vector3f.pool();

//            System.out.println(s.start+","+s.end);
            dir = dir.normaliseNull();
            if (dir != null) {
                Vector3f.cross(dir, z, perp);
                perp.scale(this.width/2.0f);
                tess.setColorF(s.rgbStart, s.alphaStart);
                tess.add(s.start.x + perp.x, s.start.y + perp.y, s.start.z);
                tess.add(s.start.x - perp.x, s.start.y - perp.y, s.start.z);
                tess.setColorF(s.rgbEnd, s.alphaEnd);
                tess.add(s.end.x - perp.x, s.end.y - perp.y, s.end.z);
                tess.add(s.end.x + perp.x, s.end.y + perp.y, s.end.z);
                n++;
            }
        }
        if (n > 0)
        tess.drawQuads();
        else
            tess.resetState();
    }
}
