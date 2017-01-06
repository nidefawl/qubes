package nidefawl.qubes.gui;

import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;

import org.lwjgl.opengl.GL11;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.shader.UniformBuffer;
import nidefawl.qubes.util.GameError;

public class LoadingScreen {
    public LoadingScreen() {
    }
    
    final float[] loadProgress = new float[2];

    public boolean render(int step, float f) {
        return render(step, f, "");
    }

    public boolean render(int step, float f, String string) {
        GameBase g = GameBase.baseInstance;
        int tw = GameBase.displayWidth;
        int th = GameBase.displayHeight;
        if (step > loadProgress.length - 1) {
            System.err.println("step > loadprogress-1!");
            step = loadProgress.length - 1;
        }
        int oldW = (int) (tw * 0.6f * loadProgress[step]);
        int newW = (int) (tw * 0.6f * (f));
        float fd = Math.abs(newW - oldW);
        if (fd < 15f && f < 1 && f > 0)
            return false;
        loadProgress[step] = f;
        int nzero = 0;
        for (int i = 0; i < loadProgress.length; i++) {
            if (loadProgress[i] == 0)
                nzero++;
            else if (nzero > 0 && loadProgress[i] > 0) {
                throw new GameError("out of order");
            }
        }
        float x = 0;
        float y = 0;
        float l = tw * 0.2f;
        float barsH = 32;
        float barsTop = (th - barsH) / 2.0f;
        if (g.isCloseRequested()) {
            g.shutdown();
            return false;
        }
        g.checkResize();
        g.updateTime();
        g.updateInput();
        Engine.updateOrthoMatrix(GameBase.displayWidth, GameBase.displayHeight);
        UniformBuffer.updateOrtho();
        glClearColor(0, 0, 0, 0F);
        glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("loadRender glClear");
        Shaders.colored.enable();
        Tess.instance.resetState();
        Tess.instance.setColor(0x0, 0xff);
        Tess.instance.add(x + tw, y, 0, 1, 1);
        Tess.instance.add(x, y, 0, 0, 1);
        Tess.instance.add(x, y + th, 0, 0, 0);
        Tess.instance.add(x + tw, y + th, 0, 1, 0);
        Tess.instance.drawQuads();
        float p = 0;
        for (int i = 0; i < loadProgress.length; i++) {
            p += loadProgress[i];
        }
        p /= (float) loadProgress.length;
        float w = tw * 0.6f * p;
        Tess.instance.setColor(0xffffff, 0xff);
        Tess.instance.add(x + l + w, y + barsTop - 2, 0, 1, 1);
        Tess.instance.add(x + l, y + barsTop - 2, 0, 0, 1);
        Tess.instance.add(x + l, y + barsTop + barsH + 2, 0, 0, 0);
        Tess.instance.add(x + l + w, y + barsTop + barsH + 2, 0, 1, 0);
        Tess.instance.drawQuads();
        FontRenderer font = FontRenderer.get(0, 16, 1);
        if (font != null) {
            Shaders.textured.enable();
            font.drawString(string, x + l + 2, y + barsTop + barsH + 10 + font.getLineHeight(), -1, true, 1.0f);
        }
        //        for (int i = 0; i < loadProgress.length; i++) {
        //            
        //             w = tw*0.6f*loadProgress[i];
        //            Tess.instance.setColor(0x666666, 0xff);
        //            Tess.instance.add(x + l + w, y + barsTop, 0, 1, 1);
        //            Tess.instance.add(x + l, y + barsTop, 0, 0, 1);
        //            Tess.instance.add(x + l, y + barsTop+barh, 0, 0, 0);
        //            Tess.instance.add(x + l + w, y + barsTop+barh, 0, 1, 0);
        //            Tess.instance.drawQuads();
        //            barsTop+=barh+2;
        //        }
        Shader.disable();
        g.updateDisplay();
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("loadRender updateDisplay");
        return true;
    }
}
