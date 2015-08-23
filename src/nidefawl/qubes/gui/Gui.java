package nidefawl.qubes.gui;

import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glVertex2f;

import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.util.Renderable;

public abstract class Gui implements Renderable {


	public void setColor(int color) {
        float cr = ((color >> 16) & 0xFF) / 255F;
        float cg = ((color >> 8) & 0xFF) / 255F;
        float cb = (color & 0xFF) / 255F;
        glColor3f(cr, cg, cb);
    }

	public float rleft   = 0F;
    public float rright  = 0F;
    public float rtop    = 0F;
    public float rbottom = 0F;
    public int width;
	public int height;
	public int posX;
	public int posY;

	public void drawRect() {
        Tess.instance.add(rleft, rbottom);
        Tess.instance.add(rright, rbottom);
        Tess.instance.add(rright, rtop);
        Tess.instance.add(rleft, rtop);
        Tess.instance.draw(GL_QUADS);
    }
	public void setSize(int w, int h) {
		this.width = w;
		this.height = h;
	}
	public void setPos(int x, int y) {
		this.posX = x;
		this.posY = y;
	}

    public void update(float dTime) {
    }
}
