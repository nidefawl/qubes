package nidefawl.qubes.gui;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static nidefawl.qubes.GLGame.*;

import java.util.ArrayList;

import nidefawl.game.Main;
import nidefawl.qubes.*;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Camera;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;

import org.lwjgl.Sys;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

public class GuiOverlayDebug extends Gui {

    final FontRenderer fontSmall;

	
	public GuiOverlayDebug() {
        this.fontSmall = FontRenderer.get("Arial", 12, 1, 14);
	}

	public void update() {

	}

	public void render(float fTime) {
        glBindTexture(GL_TEXTURE_2D, Engine.fbDbg.getTexture(0));
        {
            int tw = displayWidth;
            int th = displayHeight;
            float x = 0;
            float y = 0;
            Tess.instance.add(x + tw, y, 0, 1, 1);
            Tess.instance.add(x, y, 0, 0, 1);
            Tess.instance.add(x, y + th, 0, 0, 0);
            Tess.instance.add(x + tw, y + th, 0, 1, 0);
        }
        Tess.instance.draw(GL_QUADS);
	}
    public void preDbgFB(boolean clear) {
        Engine.fbDbg.bind();
        if (clear) {
            Engine.fbDbg.clear(0, 0, 0, 0, 0F);
            Engine.fbDbg.clearDepth();
            Engine.fbDbg.setDrawAll();
        }
        glPushAttrib(-1);
        Engine.checkGLError("fbDbg.bind");
        glMatrixMode(5888);
        glPushMatrix();
        glLoadIdentity();
        glMatrixMode(5889);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0.0D, displayWidth, displayHeight, 0.0D, 0.0D, 1.0D);
        Engine.checkGLError("fbDbg.ortho");
        glDisable(3008);
        glDepthFunc(519);
        glDepthMask(false);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_ALPHA_TEST);
        glDisable(GL_LIGHTING);
        glDisable(GL_LIGHT0);
        glDisable(GL_LIGHT1);
        //          glDisable(GL_CULL_FACE);
        glDisable(GL_COLOR_MATERIAL);
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glDisable(GL_ALPHA_TEST);
    }

    public void postDbgFB() {
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_ALPHA_TEST);
        glDepthFunc(GL_LEQUAL);
        glDepthMask(true);
        glMatrixMode(5889);
        Engine.checkGLError("fbDbg.glMatrixMode");
        glPopMatrix();
        Engine.checkGLError("fbDbg.glPopMatrix");
        glMatrixMode(5888);
        Engine.checkGLError("fbDbg.glMatrixMode");
        glPopMatrix();
        Engine.checkGLError("fbDbg.glPopMatrix");
        glPopAttrib();
        Engine.checkGLError("fbDbg.glPopAttrib");
        Engine.fbDbg.unbindCurrentFrameBuffer();
    }

    public void drawDbgTexture(int stage, int side, int num, int texture, String string) {
        float aspect = displayHeight / (float) displayWidth;
        int w1 = 120;
        int gap = 24;
        int wCol = w1 * 2 + gap;
        int hCol = displayHeight - 180;
        int xCol = 4;
        int yCol = 160;
        int h = (int) (w1 * 0.6);
        int gapy = 4;
        int b = 4;

        glPushMatrix();
        glTranslatef((wCol + gap)*stage, yCol+50, 0);
        w1-=5;
        glTranslatef(gap/2+(w1+gap/2)*side, 0, 0);
        glTranslatef(0, gapy+(gapy+h)*num, 0);
        glDisable(GL_ALPHA_TEST);
        glEnable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, texture);
        Tess.instance2.add(0, 0 + h, 0, 0, 0);
        Tess.instance2.add(w1, 0 + h, 0, 1, 0);
        Tess.instance2.add(w1, 0, 0, 1, 1);
        Tess.instance2.add(0, 0, 0, 0, 1);
        Tess.instance2.draw(7);
        glEnable(GL_ALPHA_TEST);
        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        Tess.instance2.setColorF(0, 0.5F);
        Tess.instance2.add(0, 0 + h, 0, 0, 0);
        Tess.instance2.add(w1, 0 + h, 0, 1, 0);
        Tess.instance2.add(w1, 0 + h-20, 0, 1, 1);
        Tess.instance2.add(0, 0 + h-20, 0, 0, 1);
        Tess.instance2.draw(7);
        glEnable(GL_TEXTURE_2D);
        fontSmall.drawString(string, 2, h-2, -1, true, 1.0F);
        fontSmall.drawString(""+texture+"", w1-2, h-2, -1, true, 1.0F, 1);
        glPopMatrix();
    }
    public void drawDebug() {
        String[] names = {
                "Composite0",
                "Composite1",
                "Composite2",
                "Final",
        };
            float aspect = displayHeight / (float) displayWidth;
            int w1 = 120;
            int gap = 24;
            int wCol = w1 * 2 + gap;
            int hCol = Math.min(450, displayHeight - 20);
            int xCol = 4;
            int yCol = 160;
            int h = (int) (w1 * 0.6);
            int gapy = 4;
            int b = 4;
            Tess.instance2.dontReset();
            Tess.instance2.add(0, yCol + hCol, 0);
            Tess.instance2.add(wCol, yCol + hCol, 0);
            Tess.instance2.add(wCol, yCol, 0);
            Tess.instance2.add(0, yCol, 0);
            glDisable(GL_TEXTURE_2D);
            glPushMatrix();
            for (int i = 0; i < names.length; i++) {
                glColor4f(.8F, .8F, .8F, 0.6F);
                Tess.instance2.draw(GL_QUADS);
                glTranslatef(wCol + gap, 0, 0);
            }
            glPopMatrix();
            Tess.instance2.resetState();
            Tess.instance2.dontReset();
            Tess.instance2.add(b, yCol + hCol - b, 0);
            Tess.instance2.add(wCol - b, yCol + hCol - b, 0);
            Tess.instance2.add(wCol - b, yCol + b, 0);
            Tess.instance2.add(b, yCol + b, 0);
            glPushMatrix();
            for (int i = 0; i < names.length; i++) {
                glColor4f(.4F, .4F, .4F, 0.8F);
                Tess.instance2.draw(GL_QUADS);
                glTranslatef(wCol + gap, 0, 0);
            }
            glPopMatrix();
            Tess.instance2.resetState();
            glColor4f(1F, 1F, 1F, 1.0F);
            glEnable(GL_ALPHA_TEST);
            glEnable(GL_BLEND);
            glEnable(GL_TEXTURE_2D);
            glPushMatrix();
            for (int i = 0; i < names.length; i++) {
                fontSmall.drawString(names[i], 8, yCol+20, -1, true, 1.0F);
                fontSmall.drawString("INPUT", 12, yCol+50, -1, true, 1.0F);
                fontSmall.drawString("OUTPUT", 8+w1+gap/2, yCol+50, -1, true, 1.0F);
                glTranslatef(wCol + gap, 0, 0);
            }
            glPopMatrix();
    }

}