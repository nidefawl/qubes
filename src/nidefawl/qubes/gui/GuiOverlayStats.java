package nidefawl.qubes.gui;

import java.util.ArrayList;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.blockdata.BlockData;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.input.Selection.SelectionMode;
import nidefawl.qubes.render.WorldRenderer;
import nidefawl.qubes.render.region.MeshedRegion;
import nidefawl.qubes.render.region.RegionRenderer;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.RayTrace.RayTraceIntersection;
import nidefawl.qubes.util.Stats;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.WorldClient;

public class GuiOverlayStats extends Gui {

    final public FontRenderer font;
    FontRenderer fontSmall;

    ArrayList<String>  info1        = new ArrayList<String>();
    ArrayList<String>  info        = new ArrayList<String>();

    private String     stats       = "";
    private String     statsRight  = "";
    long               messageTime = System.currentTimeMillis() - 5000L;
    String             message     = "";
    boolean render = false;
    private String stats5;
    public GuiOverlayStats() {
        this.font = FontRenderer.get("Arial", 18, 0, 20);
        this.fontSmall = FontRenderer.get("Arial", 16, 0, 18);
    }


    public void refresh() {
        int smallSize = 12;
        this.fontSmall = FontRenderer.get("Arial", smallSize, 1, smallSize+2);
        float memJVMTotal = Runtime.getRuntime().maxMemory() / 1024F / 1024F / 1024F;
        float memJVMUsed = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024F / 1024F;
        stats = String.format("FPS: %d%s (%.2f), %d ticks/s", Game.instance.lastFPS, Game.instance.getVSync() ? (" (VSync)") : "",
                Stats.avgFrameTime, (int)Math.round(Game.instance.tick/Stats.fpsInteval));
        statsRight = String.format("JHeap: %.0fMB/%.0fGB\nBuffers: %dMB\nGPU: %dMB", 
                memJVMUsed, memJVMTotal, Memory.mallocd/1024/1024, MeshedRegion.totalBytes/1024/1024);
        for (int i = 0; i < WorldRenderer.NUM_PASSES; i++) {
            statsRight += "\n"+WorldRenderer.getPassName(i)+": "+(MeshedRegion.totalBytesPass[i]/1024/1024)+"MB";
        }
        Camera cam = Engine.camera;
        Vector3f v = cam.getPosition();
        Game.instance.tick = 0;
        info1.clear();
//        info1.add( String.format("%d setUniform/frame ", Stats.uniformCalls) );
        WorldClient world = (WorldClient) Game.instance.getWorld();
        info.clear();
        if (world != null) {
            int numChunks = world.getChunkManager().getChunksLoaded();
            info.add( String.format("Shaders %d - FBOs %d", Shader.SHADERS, FrameBuffer.FRAMEBUFFERS));
            info.add( String.format("Chunks %d - R %d/%d - V %.2fM", numChunks, Engine.worldRenderer.rendered,
                    Engine.regionRenderer.occlCulled,
                    Engine.regionRenderer.numV/1000000.0) );
            info.add( String.format("UpdateRenderers: %s", Game.instance.updateRenderers ? "On" : "Off") );
            info.add( String.format("Primitive: %s", Engine.USE_TRIANGLES ? "Idxed Triangles" : "Quads") );

            this.stats5 = "";
            BlockPos p = Game.instance.selection.pos[0];
            BlockPos p2 = Game.instance.selection.pos[1];
            RayTraceIntersection intersect = null;
            if (Game.instance.selection.getMode() == SelectionMode.PLAY) {
                p = null;
                p2 = null;
                intersect = Game.instance.selection.getHit();
                if (intersect != null) {
                    p = intersect.blockPos;
                }
            }
            if (p != null && p2 != null && !p.equals(p2)) {
                this.stats5 = String.format("%d %d %d - %d %d %d", p.x, p.y, p.z, p2.x, p2.y, p2.z);
            }
            else if (p != null) {
                int lvl = world.getLight(p.x, p.y, p.z);
                int lvlNX = intersect != null ? Dir.getDirX(intersect.face) : 0;
                int lvlNY = intersect != null ? Dir.getDirY(intersect.face) : 1;
                int lvlNZ = intersect != null ? Dir.getDirZ(intersect.face) : 0;
                int lvl1 = world.getLight(p.x+lvlNX, p.y+lvlNY, p.z+lvlNZ);
                int bType = world.getType(p.x, p.y, p.z);
                int bMETA = world.getData(p.x, p.y, p.z);
                BlockData bData = world.getBlockData(p.x, p.y, p.z);
                int block1 = lvl&0xF;
                int sky1 = (lvl>>4)&0xF;
                int block2 = lvl1&0xF;
                int sky2 = (lvl1>>4)&0xF;
                int h = world.getHeight(p.x, p.z);
                this.stats5 = String.format("Light: %d/%d ("+(intersect!=null?"Face":"+1")+": %d/%d)\n %d/%d/%d = %d:%d" + 
                        (bData == null ?" -" :(" "+bData.toString())),  
                        sky1, block1, sky2, block2, p.x, p.y, p.z, bType, bMETA);
            }
        }

        info.add(String.format("M: %.1fs (%.2fk calls %.2fms/call)", (Stats.timeMeshing)/1000.0, Stats.regionUpdates/1000.0, Stats.timeMeshing/(float)(Stats.regionUpdates+1)));
        info.add(String.format("GPU-upload: %.2fms", Stats.timeRendering));
        if (world != null) {
            info.add(String.format("World: %s", world.getName()));
        }
        info.add(String.format("x: %.2f", v.x));
        info.add(String.format("y: %.2f", v.y));
        info.add(String.format("z: %.2f", v.z));
        info.addAll(Game.instance.glProfileResults);
        
        Block b = Game.instance.selBlock.getBlock();

        info.add(String.format("Selected: %s", b == null ? "destroy" : b.getName()));
        info.add(String.format("Mode: %s", Game.instance.selection.getMode().toString()));
        info.add(Game.instance.selection.quarterMode ? "Quarter" : "Full");
        render = true;
    }

    public void render(float fTime, double mx, double mY) {
        Shaders.textured.enable();

        int y = 20;
            font.drawString(stats, 5, y, 0xFFFFFF, true, 1.0F);
            font.drawString(statsRight, width - 5, y, 0xFFFFFF, true, 1.0F, 1);
            y += font.getLineHeight() * 1.2F;
            for (String s : info1) {
                font.drawString(s, 5, y, 0xFFFFFF, true, 1.0F);
                y += font.getLineHeight() * 1.2F;
            }
            if (stats5 != null) {
                font.drawString(stats5, GameBase.displayWidth / 2, 22, 0xFFFFFF, true, 1.0F, 2);
                y += font.getLineHeight() * 1.2F;
            }
            float totalHeight = (fontSmall.getLineHeight() * 1.2F)*info.size();
            float maxW = 250;
            y-=font.getLineHeight()*1.7f;
            Tess.instance.setColorF(0, 0.8f);
            Tess.instance.add(0, y+totalHeight+4);
            Tess.instance.add(maxW, y+totalHeight+4);
            Tess.instance.add(maxW, y-4);
            Tess.instance.add(0, y-4);
            Shaders.colored.enable();
            Tess.instance.drawQuads();
            y+=fontSmall.getLineHeight()*1.2f;
            Shaders.textured.enable();
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
                font.drawString(split[i], GameBase.displayWidth / 2 - strwidth / 2, ((int)70)+2+(i+1)*24, 0xFFFFFF, true, 1.0F);    
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


    /**
     * @param is
     */
    public void blockClicked(RayTraceIntersection is) {
        String msg = "";
        msg += String.format("Coordinate:  %d %d %d\n", is.blockPos.x, is.blockPos.y, is.blockPos.z);
        msg += String.format("Block:           %d\n", is.blockId);
        //            msg += String.format("Biome:          %s\n", BiomeGenBase.byId[i].biomeName);
        msg += String.format("Chunk:          %d/%d", is.blockPos.x >> 4, is.blockPos.z >> 4);

        setMessage(msg);
    }
}
