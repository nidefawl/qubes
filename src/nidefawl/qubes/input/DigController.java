/**
 * 
 */
package nidefawl.qubes.input;

import static org.lwjgl.opengl.GL11.GL_QUADS;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gl.TesselatorState;
import nidefawl.qubes.network.packet.PacketCDigState;
import nidefawl.qubes.network.packet.PacketCSetBlock;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.RayTrace.RayTraceIntersection;
import nidefawl.qubes.vec.AABBFloat;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.world.World;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class DigController {
    boolean digging = false;
    int speed = 60;
    int tick = 0;
    public BlockPos mouseOver;
    public BlockPos lastMouseOver;
    private RayTraceIntersection intersect;
    float lastdestroyProgress = 0;
    float destroyProgress = 0;
    /**
     * @param button
     * @param isDown
     */
    public void onMouseClick(int button, boolean isDown) {
        if (button == 0) {
            if (!isDown) {
                endDigging();
            } else {
                startDigging();
            }
        }
        speed = 15;
    }


    /**
     * 
     */
    public int getTicks() {
        return this.tick;
    }

    /**
     * 
     */
    public int getSpeed() {
        return this.speed;
    }

    /**
     * @return
     */
    public boolean isDigAnimation() {
        return digging || tick != 0;
    }

    /**
     * @param grabbed
     */
    public void onGrabChange(boolean grabbed) {
        if (!grabbed)
            endDigging();
    }
    private void startDigging() {
        if (!this.digging) {
            sendDigState(0);
            this.digging = true;
        }
    }
    private void endDigging() {
        if (this.digging) {
            sendDigState(1);
            this.digging = false;
        }
    }

    /**
     * 
     */
    public void preRenderUpdate() {
    }
    public void update() {
        lastdestroyProgress = destroyProgress;
        if (!digging||!equalMouseOver()) {
            destroyProgress = 0;
        }
        if (digging) {
            sendDigState(2);
            tick++;
        } else if (tick != 0) {
            tick++;
        }
        if (digging && tick == 8) {
            destroyProgress+=0.1f;
            if (destroyProgress >= 1.0f) {
                System.out.println(">= 1.0f");
                destroyProgress = 0;
                sendDigState(3);
            }
        }
        if (tick >= speed) {
            tick = 0;
        }
        lastMouseOver = mouseOver;
    }

    private boolean equalMouseOver() {
        if (lastMouseOver != null && mouseOver == null) {
            return false;
        }
        if (lastMouseOver == null && mouseOver != null) {
            return false;
        }
        return lastMouseOver == null || lastMouseOver.equals(mouseOver);
    }


    private void sendDigState(int state) {
        if (intersect != null) {
            int faceHit = intersect.face;
            BlockPos pos = intersect.blockPos;
            Game.instance.sendPacket(new PacketCDigState(Game.instance.getWorld().getId(), state, pos, intersect.pos, faceHit, Game.instance.getPlayer().getEquippedItem()));

        }
    }

    public void setBlock(RayTraceIntersection hit, BlockPos mouseOver) {
        if (this.mouseOver == null || !mouseOver.equals(this.mouseOver)) {
//            reset();
        }
        this.mouseOver = mouseOver;
        this.intersect = hit;
    }

    private void reset() {
        tick = 0;
        sendDigState(1);
    }


    public void init() {
        this.fullBlock = new TesselatorState();
        renderBlockOver(this.fullBlock, new AABBFloat(0, 0, 0, 1, 1, 1));
    }


    private TesselatorState fullBlock;
    private int[] stageTex = new int[10];
    public void reloadTextures() {
        for (int i = 0; i < stageTex.length; i++) {
            AssetTexture tex = AssetManager.getInstance().loadPNGAsset("textures/blocks/destroy/destroy_stage_"+i+".png");
            int maxDim = Math.max(tex.getWidth(), tex.getHeight());
            int mipmapLevel = 1+GameMath.log2(maxDim);
            this.stageTex[i] = TextureManager.getInstance().makeNewTexture(tex, true, false, mipmapLevel);
        }
        
    }

    public void renderBlockOver(TesselatorState out, AABBFloat box) {

        float ext = 1 / 96F;
        float w = 1/32f;
        Tess tesselator = Tess.instance;
        float br = 1f;
        tesselator.setColorRGBAF(br,br,br, 0.8f);
        float minX = box.minX - ext;
        float minY = box.minY - ext;
        float minZ = box.minZ - ext;
        float maxX = box.maxX + ext;
        float maxY = box.maxY + ext;
        float maxZ = box.maxZ + ext;
        tesselator.setNormals(0, 0, -1);
        tesselator.add(minX, maxY, minZ, 0, 1);
        tesselator.add(maxX, maxY, minZ, 1, 1);
        tesselator.add(maxX, minY, minZ, 1, 0);
        tesselator.add(minX, minY, minZ, 0, 0);

        tesselator.setNormals(0, 0, 1);
        tesselator.add(minX, maxY, maxZ, 0, 1);
        tesselator.add(maxX, maxY, maxZ, 1, 1);
        tesselator.add(maxX, minY, maxZ, 1, 0);
        tesselator.add(minX, minY, maxZ, 0, 0);

        tesselator.setNormals(0, 1, 0);
        tesselator.add(minX, maxY, maxZ, 0, 1);
        tesselator.add(minX, maxY, minZ, 0, 0);
        tesselator.add(maxX, maxY, minZ, 1, 0);
        tesselator.add(maxX, maxY, maxZ, 1, 1);

        tesselator.setNormals(0, -1, 0);
        tesselator.add(minX, minY, maxZ, 0, 1);
        tesselator.add(minX, minY, minZ, 0, 0);
        tesselator.add(maxX, minY, minZ, 1, 0);
        tesselator.add(maxX, minY, maxZ, 1, 1);

        tesselator.setNormals(-1, 0, 0);
        tesselator.add(minX, minY, maxZ, 0, 1);
        tesselator.add(minX, minY, minZ, 0, 0);
        tesselator.add(minX, maxY, minZ, 1, 0);
        tesselator.add(minX, maxY, maxZ, 1, 1);

        tesselator.setNormals(1, 0, 0);
        tesselator.add(maxX, minY, maxZ, 0, 1);
        tesselator.add(maxX, minY, minZ, 0, 0);
        tesselator.add(maxX, maxY, minZ, 1, 0);
        tesselator.add(maxX, maxY, maxZ, 1, 1);

        tesselator.draw(GL_QUADS, out);
        tesselator.resetState();
    }


    public void renderDigging(World world, float fTime) {
        if (intersect != null && destroyProgress>0) {
            float pr = (tick)/(float)(speed);
            pr*=10;
            int n = (int) (Math.floor(destroyProgress*10));
            if (n >9)n=9;
            renderBlockOver(this.fullBlock, new AABBFloat(0, 0, 0, 1, 1, 1));
            BlockPos pos = intersect.blockPos;
            Shaders.textured3D.enable();
            Shaders.textured3D.setProgramUniform3f("in_offset", pos.x, pos.y, pos.z);
            int tex = stageTex[n];
            GL.bindTexture(GL13.GL_TEXTURE0, GL11.GL_TEXTURE_2D, tex);
            this.fullBlock.drawQuads();
            Shaders.textured3D.setProgramUniform3f("in_offset", 0, 0, 0);
        }
    }


    public float getSwingProgress(float fTime) {
        if (!isDigAnimation()) {
            return 0f;
        }
        float pr = (tick+fTime)/(float)(speed);
        return pr>1?1:pr;
    }
}
