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
import nidefawl.qubes.vec.Vector3f;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class SingleBlockDraw {

    private GLVBO      vbo;
    private GLVBO      vboIdx;
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
        this.vbo = new GLVBO(GL15.GL_DYNAMIC_DRAW);
        this.vboIdx = new GLVBO(GL15.GL_DYNAMIC_DRAW);
        this.vboBuf = new ReallocIntBuffer(1024);
        this.vboIdxBuf = new ReallocIntBuffer(1024);
        this.modelMatrix = new BufferedMatrix();
        this.projMatrix = new BufferedMatrix();
    }




    public void drawBlockDefault(Block block, int data, StackData stackData) {
        SingleBlockRenderAtlas atlas = SingleBlockRenderAtlas.getInstance();
        if (atlas.needsRender(block, data, stackData)) {
            Engine.setOverrideScissorTest(false);
            Engine.setOverrideDepthMask(true);
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
            Engine.restoreScissorTest();
            Engine.restoreDepthMask();
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
        float zPos = 0;
        pxW*=2;
        Shaders.textured.enable();
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

    public void doRender(Block block, int data, StackData stackData) {
        VertexBuffer buffer = Engine.blockRender.renderSingleBlock(block, data, stackData);
        int numInts = buffer.storeVertexData(this.vboBuf);
        int numInts2 = buffer.storeIndexData(this.vboIdxBuf);
//        System.out.println("numInts2 "+numInts2);
        this.vbo.upload(GL15.GL_ARRAY_BUFFER, this.vboBuf.getByteBuf(), numInts*4);
        this.vboIdx.upload(GL15.GL_ELEMENT_ARRAY_BUFFER, this.vboIdxBuf.getByteBuf(), numInts2*4);
        Engine.bindVAO(GLVAO.vaoBlocks);
        Engine.bindBuffer(vbo);
        Engine.bindIndexBuffer(vboIdx);
        GL11.glDrawElements(GL11.GL_TRIANGLES, numInts2, GL11.GL_UNSIGNED_INT, 0);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("SingleBlockDraw.doRender");
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
