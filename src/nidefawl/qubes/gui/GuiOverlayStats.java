package nidefawl.qubes.gui;

import java.util.ArrayList;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Camera;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.render.region.RegionRenderer;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.Stats;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.World;

public class GuiOverlayStats extends Gui {

    final public FontRenderer font;
    final FontRenderer fontSmall;

    ArrayList<String>  info        = new ArrayList<String>();

    private String     stats2;
    private String     stats3;
    private String     stats       = "";
    private String     statsRight  = "";
    long               messageTime = System.currentTimeMillis() - 5000L;
    String             message     = "";
    private String     stats4;
    boolean render = false;
    private String stats5;
    public GuiOverlayStats() {
        this.font = FontRenderer.get("Arial", 18, 0, 20);
        this.fontSmall = FontRenderer.get("Arial", 14, 0, 16);
    }


    public void refresh() {
        float memJVMTotal = Runtime.getRuntime().maxMemory() / 1024F / 1024F;
        float memJVMUsed = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024F / 1024F;
        stats = String.format("FPS: %d%s (%.2f), %d ticks/s", Game.instance.lastFPS, Game.instance.getVSync() ? (" (VSync)") : "",
                Stats.avgFrameTime, (int)Math.round(Game.instance.tick/Stats.fpsInteval));
        statsRight = String.format("Memory used: %.2fMb / %.2fMb", memJVMUsed, memJVMTotal);
        Camera cam = Engine.camera;
        Vector3f v = cam.getPosition();
        Game.instance.tick = 0;
        this.stats2 = String.format("%d setUniform/frame ", Stats.uniformCalls);
        World world = Game.instance.getWorld();
        if (world != null) {
            int numChunks = world.getChunkManager().getChunksLoaded();
            this.stats3 = String.format("Chunks %d - R %d/%d", numChunks, Engine.worldRenderer.rendered, Engine.regionRenderer.numRegions);
            this.stats4 = String.format("Follow: %s", Game.instance.follow ? "On" : "Off");

            this.stats5 = "";
            BlockPos p = Engine.selection.selection[0];
            BlockPos p2 = Engine.selection.selection[1];
            if (p != null && p2 != null) {
                this.stats5 = String.format("%d %d %d - %d %d %d", p.x, p.y, p.z, p2.x, p2.y, p2.z);
            }
            else if (p != null) {
                this.stats5 = String.format("%d %d %d (Region %d %d)", p.x, p.y, p.z, 
                        p.x>>(RegionRenderer.REGION_SIZE_BITS+Chunk.SIZE_BITS), p.z>>(RegionRenderer.REGION_SIZE_BITS+Chunk.SIZE_BITS));
            }
        } else {
            this.stats3 = null;
        }

        info.clear();
        info.add(String.format("Meshing: %.2fms", Stats.timeMeshing));
        info.add(String.format("Rendering: %.2fms", Stats.timeRendering));
        info.add(String.format("TerrainGen: %.2fms", Stats.timeWorldGen));
        info.add(String.format("yaw/pitch: %.2f, %.2f", cam.getYaw(), cam.getPitch()));
        info.add(String.format("x: %.2f", v.x));
        info.add(String.format("y: %.2f", v.y));
        info.add(String.format("z: %.2f", v.z));
        info.addAll(Game.instance.glProfileResults);
        
        Block b = Block.get(Game.instance.selBlock);
        
        info.add(String.format("Selected: %s", b == null ? "destroy" : b.getName()));
        render = true;
    }

    public void render(float fTime, double mx, double mY) {
        Shaders.textured.enable();

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
            if (stats4 != null) {
                font.drawString(stats4, 5, y, 0xFFFFFF, true, 1.0F);
                y += font.getLineHeight() * 1.2F;
            }
            if (stats5 != null) {
                font.drawString(stats5, GameBase.displayWidth / 2, 22, 0xFFFFFF, true, 1.0F, 2);
                y += font.getLineHeight() * 1.2F;
            }
            for (String st : info) {
                fontSmall.drawString(st, 5, y, 0xFFFFFF, true, 1.0F);
                y += fontSmall.getLineHeight() * 1.2F;
            }

        if (System.currentTimeMillis() - messageTime < 5000) {
            int strwidth = 0;

            String[] split = message.split("\n");
            for (int i = 0; i < split.length; i++) {
                strwidth = Math.max(font.getStringWidth(split[i]), strwidth);
            }
            for (int i = 0; i < split.length; i++) {
                font.drawString(split[i], GameBase.displayWidth / 2 - strwidth / 2, ((int)30)+2+(i+1)*24, 0xFFFFFF, true, 1.0F);    
            }
        }
        Shader.disable();
 
    }

    public void setMessage(String message) {
        this.messageTime = System.currentTimeMillis();
        this.message = message;
    }
    @Override
    public void initGui(boolean first) {
    }
}
