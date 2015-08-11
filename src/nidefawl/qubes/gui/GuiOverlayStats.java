package nidefawl.qubes.gui;

import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;

import nidefawl.game.Main;
import nidefawl.qubes.GLGame;
import nidefawl.qubes.chunk.Region;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Camera;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.render.RegionRenderThread;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.world.World;

import org.lwjgl.Sys;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

public class GuiOverlayStats extends Gui {

    final FontRenderer font;
    final FontRenderer fontSmall;

    ArrayList<String>  info        = new ArrayList<String>();

    private String     stats2;
    private String stats3;
    private String     stats       = "";
    private String     statsRight  = "";
    long               messageTime = System.currentTimeMillis() - 5000L;
    String             message     = "";

    public GuiOverlayStats() {
        this.font = FontRenderer.get("Arial", 18, 0, 20);
        this.fontSmall = FontRenderer.get("Arial", 14, 0, 16);
        //        boolean isWindows, is64bit;
        //        isWindows = System.getProperty("os.name").contains("Windows");
        //        if (isWindows) {
        //            is64bit = (System.getenv("ProgramFiles(x86)") != null);
        //        } else {
        //            is64bit = (System.getProperty("os.arch").indexOf("64") != -1);
        //        }
        //        info.add("OS: " + System.getProperty("os.name") + " (" + System.getProperty("os.arch") + " on " + (is64bit ? "x64 OS" : "x86 OS") + ") version " + System.getProperty("os.version"));
        //        info.add(new StringBuilder("Java: ").append(System.getProperty("java.version")).append(", ").append(System.getProperty("java.vendor")).toString());
        //
        //        long mem = Runtime.getRuntime().maxMemory() / 1048576;
        //        info.add("JVM Memory: " + mem + "MB");
        //        info.add(new StringBuilder("VM: ").append(System.getProperty("java.vm.name")).append(" (").append(System.getProperty("java.vm.info")).append("), ").append(System.getProperty("java.vm.vendor")).toString());
        //        info.add(new StringBuilder("LWJGL: ").append(Sys.getVersion()).toString());
        //        info.add(new StringBuilder("OpenGL: ").append(GL11.glGetString(7937 /*GL_RENDERER*/)).append(" version ").append(GL11.glGetString(7938 /*GL_VERSION*/)).append(", ").append(GL11.glGetString(7936 /*GL_VENDOR*/)).toString());
    }

    public void update(float dTime) {
        float memJVMTotal = Runtime.getRuntime().maxMemory() / 1024F / 1024F;
        float memJVMUsed = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024F / 1024F;
        stats = String.format("FPS: %d%s (%.2f), %d ticks/s", Main.instance.lastFPS, Main.instance.fpsLimit > 0 ? (" (Limit " + Main.instance.fpsLimit + ")") : "",
                Main.instance.avgFrameTime, Main.instance.tick);
        statsRight = String.format("Memory used: %.2fMb / %.2fMb", memJVMUsed, memJVMTotal);
        Camera cam = Engine.camera;
        Vector3f v = cam.getPosition();
        Main.instance.tick = 0;
        this.stats2 = String.format("%d setUniform/frame ", Main.instance.uniformCalls);
        World world = Main.instance.getWorld();
        if (world != null) {
            RegionRenderThread loader = world.loader;
            int numRegions = loader.getRegionsLoaded();
            int chunks = numRegions * Region.REGION_SIZE * Region.REGION_SIZE;
            this.stats3 = String.format("Chunks - loaded %d - with blockdata: %d - blockfaces %d", 
                    chunks, Region.REGION_SIZE * Region.REGION_SIZE * loader.getRegionsWithData() , Engine.worldRenderer.getNumRendered());

        } else {
            this.stats3 = null;
        }

        info.clear();
        info.add(String.format("yaw/pitch: %.2f, %.2f", cam.getYaw(), cam.getPitch()));
        info.add(String.format("x: %.2f", v.x));
        info.add(String.format("y: %.2f", v.y));
        info.add(String.format("z: %.2f", v.z));
    }

    public void render(float fTime) {
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        //            glBegin(GL_QUADS);
        //            glTexCoord2f(1, 1);
        //            glVertex2f(300, 300);
        //            glTexCoord2f(0, 1);
        //            glVertex2f(0, 300);
        //            glTexCoord2f(0,0);
        //            glVertex2f(0, 0);
        //            glTexCoord2f(1, 0);
        //            glVertex2f(300, 0);
        //            glEnd();
        glEnable(GL_TEXTURE_2D);
        int y = 20;
        font.drawString(stats, 5, y, 0xFFFFFF, true, 1.0F);
        font.drawString(statsRight, width - 5, y, 0xFFFFFF, true, 1.0F, 1);
        y += font.getLineHeight() * 1.2F;
        font.drawString(stats2, 5, y, 0xFFFFFF, true, 1.0F);
        y += font.getLineHeight() * 1.2F;
        if (stats3 != null) {
            font.drawString(stats3, 5, y, 0xFFFFFF, true, 1.0F);
            y += font.getLineHeight() * 1.2F;
        }
        for (String st : info) {
            fontSmall.drawString(st, 5, y, 0xFFFFFF, true, 1.0F);
            y += fontSmall.getLineHeight() * 1.2F;
        }
        if (System.currentTimeMillis() - messageTime < 5000) {
            int strwidth = font.getStringWidth(this.message);
            rleft = GLGame.displayWidth / 2 - strwidth / 2 - 8F;
            rright = GLGame.displayWidth / 2 + strwidth / 2 + 8F;
            rtop = GLGame.displayHeight / 3 - 22;
            rbottom = GLGame.displayHeight / 3 + 3;
            setColor(0x121212);
            glDisable(GL_BLEND);
            glDisable(GL_TEXTURE_2D);
            drawRect();
            glEnable(GL_BLEND);
            glEnable(GL_TEXTURE_2D);
            font.drawString(this.message, GLGame.displayWidth / 2 - strwidth / 2, GLGame.displayHeight / 3, 0xFFFFFF, true, 1.0F);

            setColor(-1);
        }
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
    }

    public void setMessage(String message) {
        this.messageTime = System.currentTimeMillis();
        this.message = message;
    }
}
