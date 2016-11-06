package nidefawl.qubes.gui;

import java.util.ArrayList;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.blockdata.BlockData;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.render.WorldRenderer;
import nidefawl.qubes.render.region.MeshedRegion;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.RayTrace.RayTraceIntersection;
import nidefawl.qubes.util.Stats;
import nidefawl.qubes.util.SysInfo;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.vec.Vector3f;
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
        this.font = FontRenderer.get(0, 18, 0);
        this.fontSmall = FontRenderer.get(0, 16, 0);
    }
    SysInfo sysInfo = null;

    public void refresh() {
        int smallSize = 12;
        this.fontSmall = FontRenderer.get(0, smallSize, 1);
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
        Vector3f v = Game.instance.vPlayer;
        Game.instance.tick = 0;
        info1.clear();
//        info1.add( String.format("%d setUniform/frame ", Stats.uniformCalls) );
        WorldClient world = (WorldClient) Game.instance.getWorld();
        info.clear();
        if (world != null) {
            int numChunks = world.getChunkManager().getChunksLoaded();
            if (sysInfo == null) {
                sysInfo = new SysInfo();
            }
            String ss = sysInfo.openGLVersion; 
            while (ss.length() > 0) {
                int n = ss.length() < 30 ? -1 : ss.substring(30).indexOf(" ");
                if (n >= 0 && n < ss.length()-1) {
                    n+=30;
                    info.add(ss.substring(0, n));
                    ss = ss.substring(n+1);
                } else {

                    info.add(ss);
                    break;
                }
            }
            info.add( String.format("Shaders %d - FBOs %d", Shader.SHADERS, FrameBuffer.FRAMEBUFFERS));
            info.add( String.format("VBOs %d (%d terrain)", GLVBO.ALLOC_VBOS, GLVBO.ALLOC_VBOS_TERRAIN));
            info.add( String.format("Chunks %d - R %d/%d - V %.2fM", numChunks, Engine.worldRenderer.rendered,
                    Engine.regionRenderer.occlCulled,
                    Engine.regionRenderer.numV/1000000.0) );
            info.add( String.format("Drawcalls %d, Upload %db/f", Stats.lastFrameDrawCalls, Stats.uploadBytes));
            if (GL.isBindlessSuppported()) {
                info.add( "Bindless  supported" );
                info.add( String.format("Bindless: %b", Engine.userSettingUseBindless) );
            } else if (Engine.userSettingUseBindless) {
                info.add( "Bindless not supported" );
                
            }
            info.add( String.format("Lights: %d", world.lights.size()) );
            info.add( String.format("UpdateRenderers (R): %s", Game.instance.updateRenderers ? "On" : "Off") );
            info.add( String.format("External resources (F11): %s", AssetManager.getInstance().isExternalResources() ? "On" : "Off") );

            this.stats5 = "";
            BlockPos p = Game.instance.selection.pos[0];
            BlockPos p2 = Game.instance.selection.pos[1];
            if (p != null && p2 != null && !p.equals(p2)) {
                this.stats5 = String.format("%d %d %d - %d %d %d", p.x, p.y, p.z, p2.x, p2.y, p2.z);
            } else {

                RayTraceIntersection intersect = Game.instance.selection.getHit();
                if (intersect != null) {
                    p = intersect.blockPos;
                    int lvl = world.getLight(p.x, p.y, p.z);
                    int lvlNX = Dir.getDirX(intersect.face);
                    int lvlNY = Dir.getDirY(intersect.face);
                    int lvlNZ = Dir.getDirZ(intersect.face);
                    int lvl1 = world.getLight(p.x+lvlNX, p.y+lvlNY, p.z+lvlNZ);
                    int bType = world.getType(p.x, p.y, p.z);
                    int bMETA = world.getData(p.x, p.y, p.z);
                    BlockData bData = world.getBlockData(p.x, p.y, p.z);
                    int block1 = lvl&0xF;
                    int sky1 = (lvl>>4)&0xF;
                    int block2 = lvl1&0xF;
                    int sky2 = (lvl1>>4)&0xF;
                    String s = Block.get(bType).getName();
                    this.stats5 = String.format("Light: %d/%d ("+(intersect!=null?"Face":"+1")+": %d/%d)\n %d/%d/%d = %s (%d:%d" + 
                            (bData == null ?" -" :(" "+bData.toString()))+")",  
                            sky1, block1, sky2, block2, p.x, p.y, p.z, s, bType, bMETA);
                }
            
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
        
        info.add(String.format("Mode: %s", Game.instance.selection.getMode().toString()));
        info.add(Game.instance.selection.quarterMode ? "Quarter" : "Full");
        render = true;
    }

    public void render(float fTime, double mx, double mY) {
        Shaders.textured.enable();

        int y = 20;
        float maxW = 250;
            font.drawString(stats, 5, y, 0xFFFFFF, true, 1.0F);
            font.drawString(statsRight, width - 5, y, 0xFFFFFF, true, 1.0F, 1);
            y += font.getLineHeight();
            for (String s : info1) {
                font.drawString(s, 5, y, 0xFFFFFF, true, 1.0F);
                y += font.getLineHeight();
            }
            if (stats5 != null) {
                font.drawString(stats5, GameBase.displayWidth / 2, 22, 0xFFFFFF, true, 1.0F, 2);
                y += font.getLineHeight();
            }
            float totalHeight = (fontSmall.getLineHeight())*info.size();
            y-=font.getLineHeight()*1.7f;
            Tess.instance.setColorF(0, 0.8f);
            Tess.instance.add(0, y+totalHeight+8);
            Tess.instance.add(maxW, y+totalHeight+8);
            Tess.instance.add(maxW, y);
            Tess.instance.add(0, y);
            Shaders.colored.enable();
            Tess.instance.drawQuads();
            y+=fontSmall.getLineHeight()*1.2f;
            Shaders.textured.enable();
            for (String st : info) {
                fontSmall.drawString(st, 5, y, 0xFFFFFF, true, 1.0F);
                y += fontSmall.getLineHeight();
            }

        if (System.currentTimeMillis() - messageTime < 5000) {
            float strwidth = 0;

            String[] split = message.split("\n");
            for (int i = 0; i < split.length; i++) {
                strwidth = Math.max(font.getStringWidth(split[i]), strwidth);
            }
            for (int i = 0; i < split.length; i++) {
                font.drawString(split[i], GameBase.displayWidth / 2 - strwidth / 2, ((int)70)+2+(i+1)*24, 0xFFFFFF, true, 1.0F);    
            }
        }
        int w = 64;
        int x = 5;
        y+=5;
        Shaders.gui.enable();
        float wBg = w+16;
        shadowSigma = 2;
        extendx = 1;
        extendy = 1;
        round = 5;
        renderRoundedBoxShadowInverse(x, y, -4, wBg, wBg, -1, 0.8f, true);
        float inset2 = 2;
        this.round = 3;
        renderRoundedBoxShadowInverse(x+inset2, y+inset2, 32, wBg-inset2*2, wBg-inset2*2, -1, 0.6f, false);
        resetShape();
        x+=8;
        y+=3;
        Shaders.colored.enable();
        Tess.instance.drawQuads();
        
        if (Game.instance.selBlock.getBlock()!=Block.air) {
            Engine.itemRender.drawItem(Game.instance.selBlock, x, y+5, w, w);
        }
        Block b = Game.instance.selBlock.getBlock();
        Shaders.textured.enable();
        if (b != null)
            font.drawString(b.getName(), 5, y+wBg+12, -1, true, 1.0f);

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
