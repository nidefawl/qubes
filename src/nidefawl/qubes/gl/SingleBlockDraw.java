/**
 * 
 */
package nidefawl.qubes.gl;

import java.nio.IntBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31;

import nidefawl.qubes.Game;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.item.StackData;
import nidefawl.qubes.render.region.MeshedRegion;
import nidefawl.qubes.render.region.RegionRenderer;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.GameMath;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class SingleBlockDraw {

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

    /**
     * 
     */
    public SingleBlockDraw() {
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
    public void drawBlock(Block block, int data, StackData stackData) {
        Shaders.singleblock.enable();
        this.modelMatrix.setIdentity();
//        System.out.println(this.x);
        this.modelMatrix.translate(this.x, this.y, this.z);
        this.modelMatrix.scale(this.scale*32);
        this.modelMatrix.scale(1, -1, 1);
        this.modelMatrix.rotate(this.rotX*GameMath.PI_OVER_180, 1,0,0);
        this.modelMatrix.rotate(this.rotY*GameMath.PI_OVER_180, 0,1,0);
        this.modelMatrix.rotate(this.rotZ*GameMath.PI_OVER_180, 0,0,1);
        this.modelMatrix.update();
        Shaders.singleblock.setProgramUniformMatrix4("in_modelMatrix", false, this.modelMatrix.get(), false);
        VertexBuffer buffer = Engine.blockRender.renderSingleBlock(block, data, stackData);
        int numInts = buffer.putIn(this.vboBuf);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindBuffer " + vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, numInts * 4L, this.vboBuf.getByteBuf(), GL15.GL_STATIC_DRAW);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBufferData ");
        if (Engine.USE_TRIANGLES) {
            numInts = VertexBuffer.createIndex(buffer.faceCount * 2, this.vboIdxBuf);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboIndices);
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("glBindBuffer " + vboIndices);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, numInts * 4, this.vboIdxBuf.getByteBuf(), GL15.GL_STATIC_DRAW);
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("glBufferData");
        }

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vbo);
        int ptrSetting = 0;
        MeshedRegion.enableVertexPtrs(ptrSetting);
        if (Engine.USE_TRIANGLES) {
            GL11.glDrawElements(GL11.GL_TRIANGLES, buffer.faceCount * 2 * 3, GL11.GL_UNSIGNED_INT, 0);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        } else {
            GL11.glDrawArrays(GL11.GL_QUADS, 0, buffer.vertexCount);
        }
        MeshedRegion.disableVertexPtrs(ptrSetting);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
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
