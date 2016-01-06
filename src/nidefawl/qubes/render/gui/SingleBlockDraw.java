/**
 * 
 */
package nidefawl.qubes.render.gui;

import static org.lwjgl.opengl.GL13.GL_TEXTURE0;

import java.nio.IntBuffer;

import org.lwjgl.opengl.*;


import nidefawl.qubes.Game;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.item.StackData;
import nidefawl.qubes.render.region.MeshedRegion;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.util.GameMath;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class SingleBlockDraw {

    private int      vbo;
    private int      vboIndices;
    ReallocIntBuffer vboBuf;
    ReallocIntBuffer vboIdxBuf;
    private BufferedMatrix modelMatrix;
    private BufferedMatrix projMatrix;
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
        this.projMatrix = new BufferedMatrix();
    }




    public void drawBlockDefault(Block block, int data, StackData stackData) {
        SingleBlockRenderAtlas atlas = SingleBlockRenderAtlas.getInstance();
        if (atlas.needsRender(block, data, stackData)) {
            this.modelMatrix.setIdentity();
            this.projMatrix.setZero();
            atlas.preRender(block, data, stackData, projMatrix, modelMatrix);
            this.modelMatrix.scale(1, -1, 1);
            this.modelMatrix.rotate(this.rotX*GameMath.PI_OVER_180, 1,0,0);
            this.modelMatrix.rotate(this.rotY*GameMath.PI_OVER_180, 0,1,0);
            this.modelMatrix.rotate(this.rotZ*GameMath.PI_OVER_180, 0,0,1);
            this.modelMatrix.update();
            this.projMatrix.update();
            Shaders.singleblock.enable();
            Shaders.singleblock.setProgramUniformMatrix4("in_modelMatrix", false, this.modelMatrix.get(), false);
            Shaders.singleblock.setProgramUniformMatrix4("in_projectionMatrix", false, this.projMatrix.get(), false);
            GL.bindTexture(GL_TEXTURE0, GL30.GL_TEXTURE_2D_ARRAY, TMgr.getBlocks());
            doRender(block, data, stackData);
            atlas.postRender();
            Shaders.textured.enable();
        }
        int tex = atlas.getTexture(block, data, stackData);
        int texIdx = atlas.getTextureIdx(block, data, stackData);
        GL.bindTexture(GL13.GL_TEXTURE0, GL11.GL_TEXTURE_2D, tex);
        float texX = SingleBlockRenderAtlas.getX(texIdx);
        float texY = SingleBlockRenderAtlas.getY(texIdx);
        float texW = SingleBlockRenderAtlas.getTexW();
        float pxW = scale*32;
        float xPos = x-pxW;
        float yPos = y-pxW;
        float zPos = z;
        pxW*=2;
        Tess.instance.setColorF(-1, 1);
        Tess.instance.add(xPos, yPos+pxW, zPos, texX, texY);
        Tess.instance.add(xPos+pxW, yPos+pxW, zPos, texX+texW, texY);
        Tess.instance.add(xPos+pxW, yPos, zPos, texX+texW, texY+texW);
        Tess.instance.add(xPos, yPos, zPos, texX, texY+texW);
        Tess.instance.draw(GL11.GL_QUADS);
    }
    public void drawBlock(Block block, int data, StackData stackData) {
        Shaders.singleblock.enable();
        this.modelMatrix.setIdentity();
        this.modelMatrix.translate(this.x, this.y, this.z);
        this.modelMatrix.scale(this.scale*32);
        this.modelMatrix.scale(1, -1, 1);
        this.modelMatrix.rotate(this.rotX*GameMath.PI_OVER_180, 1,0,0);
        this.modelMatrix.rotate(this.rotY*GameMath.PI_OVER_180, 0,1,0);
        this.modelMatrix.rotate(this.rotZ*GameMath.PI_OVER_180, 0,0,1);
        this.modelMatrix.update();
        Shaders.singleblock.setProgramUniformMatrix4("in_modelMatrix", false, this.modelMatrix.get(), false);
        Shaders.singleblock.setProgramUniformMatrix4("in_projectionMatrix", false, Engine.getMatOrtho3DP().get(), false);
        GL.bindTexture(GL_TEXTURE0, GL30.GL_TEXTURE_2D_ARRAY, TMgr.getBlocks());
        doRender(block, data, stackData);
    }

    protected void doRender(Block block, int data, StackData stackData) {
        VertexBuffer buffer = Engine.blockRender.renderSingleBlock(block, data, stackData);
        int numInts = buffer.putIn(this.vboBuf);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindBuffer " + vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, numInts * 4L, this.vboBuf.getByteBuf(), GL15.GL_STATIC_DRAW);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBufferData ");
        if (Engine.USE_TRIANGLES) {
            this.vboIdxBuf.put(buffer.getTriIdxBuffer(), 0, buffer.getTriIdxPos());
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboIndices);
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("glBindBuffer " + vboIndices);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, buffer.getTriIdxPos() * 4, this.vboIdxBuf.getByteBuf(), GL15.GL_STATIC_DRAW);
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("glBufferData");
        }
        Engine.bindVAO(GLVAO.vaoBlocks);
        Engine.bindBuffer(vbo);
        Engine.bindIndexBuffer(vboIndices);
        if (Engine.USE_TRIANGLES) {
            GL11.glDrawElements(GL11.GL_TRIANGLES, buffer.faceCount * 2 * 3, GL11.GL_UNSIGNED_INT, 0);
        } else {
            GL11.glDrawArrays(GL11.GL_QUADS, 0, buffer.vertexCount);
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
