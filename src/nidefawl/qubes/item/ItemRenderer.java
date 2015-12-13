/**
 * 
 */
package nidefawl.qubes.item;

import static org.lwjgl.opengl.GL13.GL_TEXTURE0;

import java.nio.IntBuffer;

import org.lwjgl.opengl.*;

import nidefawl.qubes.Game;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.render.region.MeshedRegion;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.texture.ItemTextureArray;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.util.GameMath;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class ItemRenderer {

    private int      vbo;
    private int      vboIndices;
    ReallocIntBuffer vboBuf;
    ReallocIntBuffer vboIdxBuf;
    private BufferedMatrix modelMatrix;
    private float x;
    private float y;
    private float z;
    private float scale;
    private float rotX;
    private float rotY;
    private float rotZ;

    public ItemRenderer() {
    }

    /**
     * 
     */
    public void init() {
        IntBuffer buff = Engine.glGenBuffers(2);
        this.vbo = buff.get(0);
        this.vboIndices = buff.get(1);
        this.vboBuf = new ReallocIntBuffer(1024);
        this.vboIdxBuf = new ReallocIntBuffer(1024);
        this.modelMatrix = new BufferedMatrix();
    }


    /**
     * @param stackData 
     * @param stone
     * @param i
     */
    public void drawItem(BaseStack stack, float x, float y) {
        if (stack.isItem()) {
            Shaders.textured.enable();
            ItemStack blockStack = (ItemStack) stack;
            
            int tex = ItemTextureArray.getInstance().getTextureIdx(blockStack.id, 0);
            GL.bindTexture(GL_TEXTURE0, GL30.GL_TEXTURE_2D_ARRAY, tex);
            Tess.instance.setColorF(-1, 1);
            Tess.instance.add(x+32, y+0, 0);
            Tess.instance.add(x+0, y+0, 0);
            Tess.instance.add(x+0, y+32, 0);
            Tess.instance.add(x+32, y+32, 0);
            Tess.instance.draw(GL11.GL_QUADS);
        } else {
            BlockStack blockStack = (BlockStack) stack;
            Engine.blockDraw.drawBlock(blockStack.getBlock(), blockStack.data, blockStack.getStackdata());
        }
    }

    /**
     * @param f
     * @param g
     */
    public void setOffset(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * @param i
     */
    public void setScale(float scale) {
        this.scale = scale;
    }
    public void reset() {
        this.modelMatrix.setIdentity();
        this.modelMatrix.update();
    }
    /**
     * @param fRot
     */
    public void setRotation(float x, float y, float z) {
        this.rotX = x;
        this.rotY = y;
        this.rotZ = z;
    }


}
