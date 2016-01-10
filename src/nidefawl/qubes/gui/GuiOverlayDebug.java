package nidefawl.qubes.gui;

import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.opengl.GL13;

import nidefawl.qubes.Game;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;

public class GuiOverlayDebug extends Gui {

    final FontRenderer fontSmall;
    final static BufferedMatrix mat = new BufferedMatrix();

	
	public GuiOverlayDebug() {
        this.fontSmall = FontRenderer.get("NotoSans-Bold", 12, 1, 14);
	}

	public void render(float fTime, double mx, double mY) {
	    Shaders.textured.enable();
	    Engine.pxStack.push(250, -280, 0);
        glBindTexture(GL_TEXTURE_2D, Engine.fbDbg.getTexture(0));
        Engine.drawFullscreenQuad();
        Engine.pxStack.pop();
        Shader.disable();
	}
    public void preDbgFB(boolean clear) {
        Engine.fbDbg.bind();
        if (clear) {
            Engine.fbDbg.clearFrameBuffer();
        }
        glPushAttrib(-1);
        glDisable(3008);
        glDepthFunc(519);
        Engine.enableDepthMask(false);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    public void postDbgFB() {
        glEnable(GL_TEXTURE_2D);
        glDepthFunc(GL_LEQUAL);
        Engine.enableDepthMask(true);
        glPopAttrib();
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("fbDbg.glPopAttrib");
        FrameBuffer.unbindFramebuffer();
    }

    public void drawDbgTexture(int stage, int side, int num, int texture, String string) {
        drawDbgTexture(stage, side, num, texture, string, null, 0, 0);
    }
    
    public void drawDbgTexture(int stage, int side, int num, int texture, String string, Shader depthBufShader, float f1, float f2) {
        int w1 = 120;
        int gap = 24;
        int wCol = w1 * 2 + gap;
        int hCol = Math.min(450, height - 20);
        int yCol = Math.min(290, height - hCol);
        int h = (int) (w1 * 0.6);
        int gapy = 4;
        int b = 4;
        mat.setIdentity();
        mat.translate((wCol + gap)*stage, yCol+50, 0);
        w1-=5;
        mat.translate(gap/2+(w1+gap/2)*side, 0, 0);
        mat.translate(0, gapy+(gapy+h)*num, 0);
        Engine.setOrthoMV(mat);
        GL.bindTexture(GL13.GL_TEXTURE0, GL_TEXTURE_2D, texture);
        Tess.tessFont.setColor(-1, 255);
        Tess.tessFont.add(0, 0 + h, 0, 0, 0);
        Tess.tessFont.add(w1, 0 + h, 0, 1, 0);
        Tess.tessFont.add(w1, 0, 0, 1, 1);
        Tess.tessFont.add(0, 0, 0, 0, 1);
        if (depthBufShader != null) {
            depthBufShader.enable();
            depthBufShader.setProgramUniform1i("depthSampler", 0);
            depthBufShader.setProgramUniform2f("zbufparam", f1, f2);
        } else {

            if (texture ==  Engine.getSceneFB().getTexture(2)) {
                Shaders.renderUINT.enable();
                Shaders.renderUINT.setProgramUniform1i("tex0", 0);
                GL.bindTexture(GL13.GL_TEXTURE0, GL_TEXTURE_2D, texture);
            } else {
                Shaders.textured.enable();    
            }   
            
        }
        Tess.tessFont.draw(7);
        Shaders.colored.enable();
        Tess.tessFont.setColorF(222, 0.5F);
        Tess.tessFont.add(0, 0 + h, 0, 0, 0);
        Tess.tessFont.add(w1, 0 + h, 0, 1, 0);
        Tess.tessFont.add(w1, 0 + h-20, 0, 1, 1);
        Tess.tessFont.add(0, 0 + h-20, 0, 0, 1);
        Tess.tessFont.draw(7);
        Shaders.textured.enable();
        glEnable(GL_BLEND);
        fontSmall.drawString(string, 2, h-2, -1, true, 1.0F);
        fontSmall.drawString(""+texture+"", w1-2, h-2, -1, true, 1.0F, 1);
        Shader.disable();
        Engine.restoreOrtho();
    }
    TesselatorState state = new TesselatorState();
    public void drawDebug() {
        String[] names = new String[] {
                    "Deferred",
                    "Blur",
                    "Final",
                    "AA EdgeDetect",
            };
        mat.setIdentity();
        Engine.setOrthoMV(mat);
            int w1 = 120;
            int gap = 24;
            int wCol = w1 * 2 + gap;
            int hCol = Math.min(450, height - 20);
            int yCol = Math.min(290, height - hCol);
            int b = 4;
            Shaders.colored.enable();
            Tess.tessFont.setColorRGBAF(.8F, .8F, .8F, 0.6F);
            Tess.tessFont.add(0, yCol + hCol, 0);
            Tess.tessFont.add(wCol, yCol + hCol, 0);
            Tess.tessFont.add(wCol, yCol, 0);
            Tess.tessFont.add(0, yCol, 0);
            Tess.tessFont.draw(GL_QUADS, state);
            for (int i = 0; i < names.length; i++) {
                state.drawQuads();
                mat.translate(wCol + gap, 0, 0);
                Engine.setOrthoMV(mat);
            }
            mat.setIdentity();
            Engine.setOrthoMV(mat);
            Tess.tessFont.setColorRGBAF(.4F, .4F, .4F, 0.8F);
            Tess.tessFont.add(b, yCol + hCol - b, 0);
            Tess.tessFont.add(wCol - b, yCol + hCol - b, 0);
            Tess.tessFont.add(wCol - b, yCol + b, 0);
            Tess.tessFont.add(b, yCol + b, 0);
            Tess.tessFont.draw(GL_QUADS, state);
            for (int i = 0; i < names.length; i++) {
                state.drawQuads();
                mat.translate(wCol + gap, 0, 0);
                Engine.setOrthoMV(mat);
            }
            mat.setIdentity();
            Engine.setOrthoMV(mat);
            glEnable(GL_BLEND);
            Shaders.textured.enable();
            for (int i = 0; i < names.length; i++) {
                fontSmall.drawString(names[i], 8, yCol+20, -1, true, 1.0F);
                fontSmall.drawString("INPUT", 12, yCol+50, -1, true, 1.0F);
                fontSmall.drawString("OUTPUT", 8+w1+gap/2, yCol+50, -1, true, 1.0F);
                mat.translate(wCol + gap, 0, 0);
                Engine.setOrthoMV(mat);
            }
            Shader.disable();
            Engine.restoreOrtho();
    }

    @Override
    public void initGui(boolean first) {
    }
}
