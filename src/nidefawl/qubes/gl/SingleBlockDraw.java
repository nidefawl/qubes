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

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class SingleBlockDraw {

    private int      vbo;
    private int      vboIndices;
    ReallocIntBuffer vboBuf;
    ReallocIntBuffer vboIdxBuf;

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
    }

    /**
     * @param stackData 
     * @param stone
     * @param i
     */
    public void drawBlock(Block block, int data, StackData stackData) {
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

}
