package nidefawl.qubes.render.region;

import static nidefawl.qubes.render.WorldRenderer.NUM_PASSES;
import static nidefawl.qubes.render.region.RegionRenderer.RENDER_STATE_INIT;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import org.lwjgl.opengl.*;

import nidefawl.qubes.Game;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.vec.AABB;

public class MeshedRegion {

    private int[]     vertexCount   = new int[NUM_PASSES];
    private int[]     vbo;
    private boolean[] hasPass       = new boolean[NUM_PASSES];
    public int        rX;
    public int        rZ;
    public int rY;
    public int        renderState   = RENDER_STATE_INIT;
    public boolean    isRenderable  = false;
    public boolean    xNeg;
    public boolean    xPos;
    public boolean    zPos;
    public boolean    zNeg;
    public final AABB aabb          = new AABB();
    public int[]      frustumStates = new int[Engine.NUM_PROJECTIONS];


    public MeshedRegion() {
    }

    public void renderRegion(float fTime, int pass, int drawMode, int drawInstances) {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vbo[pass]);
        GL20.glEnableVertexAttribArray(0);
        if (pass != 2) {
            int stride = 13;
            GL20.glVertexAttribPointer(0, 4, GL11.GL_FLOAT, false, stride * 4, 0);
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("AttribPtr " + 0);
            int offset = 4;
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 3, GL11.GL_BYTE, false, stride * 4, offset * 4);
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("AttribPtr " + 1);
            offset += 1;
            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, stride * 4, offset * 4);
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("AttribPtr " + 2);
            offset += 2;
            GL20.glEnableVertexAttribArray(3);
            GL20.glVertexAttribPointer(3, 4, GL11.GL_UNSIGNED_BYTE, true, stride * 4, offset * 4);
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("AttribPtr " + 3);
            offset += 1;
            GL20.glEnableVertexAttribArray(4);
            GL20.glVertexAttribPointer(4, 2, GL11.GL_SHORT, false, stride * 4, offset * 4);
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("AttribPtr " + 4);
            offset += 1;
            GL20.glEnableVertexAttribArray(5);
            GL20.glVertexAttribPointer(5, 4, GL11.GL_SHORT, false, stride * 4, offset * 4);
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("AttribPtr " + 5);
            offset += 2;
        } else {
            int stride = 4;
            GL20.glVertexAttribPointer(0, 4, GL11.GL_FLOAT, false, stride * 4, 0);
        }
        try {

            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("glDrawArrays (" + this.vertexCount[pass] + ")");
        } catch (Exception e) {
            if (Game.ticksran % 40 == 0)
                System.out.println(e.getMessage());
        }
        if (drawInstances > 0) {
            GL31.glDrawArraysInstanced(drawMode, 0, this.vertexCount[pass], drawInstances);
        } else {

            GL11.glDrawArrays(drawMode, 0, this.vertexCount[pass]);
        }

        for (int i = 0; i < Tess.attributes.length; i++)
            GL20.glDisableVertexAttribArray(i);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        //        if (displayList != null && displayList.list > 0)
        //            glCallList(displayList.list + idx);
        //          glNewList(displayList.list + idx, GL11.GL_COMPILE);
        //          Tess.instance.drawState(states[idx], GL_QUADS);
        //          glEndList();
    }

    public void compileDisplayList(Tess[] states) {
        if (this.vbo == null) {
            this.vbo = new int[NUM_PASSES];
            IntBuffer intbuf = Engine.glGenBuffers(this.vbo.length);
            for (int i = 0; i < vbo.length; i++) {
                vbo[i] = intbuf.get(i);
            }
        }
        Arrays.fill(this.hasPass, false);
        Arrays.fill(this.vertexCount, 0);

        ByteBuffer buf = Engine.getBuffer();
        IntBuffer intBuffer = Engine.getIntBuffer();
        for (int pass = 0; pass < NUM_PASSES; pass++) {
            Tess tess = states[pass];
            int len = tess.getIdx(tess.vertexcount);
            intBuffer.clear();
            intBuffer.put(tess.rawBuffer, 0, len);
            buf.position(0).limit(len * 4);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo[pass]);
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("glBindBuffer " + vbo[pass]);
            //            System.out.println(buf);
            //            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, len * 4L, buf, GL15.GL_STATIC_DRAW);
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("glBufferData /" + intBuffer);

            vertexCount[pass] = tess.vertexcount;
            this.hasPass[pass] |= tess.vertexcount > 0;
        }

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        this.isRenderable = true;
    }

    public void release() {
        if (this.vbo != null) {
            Engine.deleteBuffers(this.vbo);
            this.vbo = null;
        }
        renderState = RENDER_STATE_INIT;
        this.isRenderable = false;
    }

    public int getNumVertices(int i) {
        return vertexCount[i];
    }

    public boolean hasPass(int i) {
        return this.hasPass[i];
    }

    public void updateBB() {
        int bits = RegionRenderer.REGION_SIZE_BITS + Chunk.SIZE_BITS;
        this.aabb.minX = this.rX << (bits);
        this.aabb.minY = this.rY << (RegionRenderer.SLICE_HEIGHT_BLOCK_BITS);
        this.aabb.minZ = this.rZ << (bits);
        this.aabb.maxX = this.aabb.minX + RegionRenderer.REGION_SIZE_BLOCKS;
        this.aabb.maxY = this.aabb.minY + RegionRenderer.SLICE_HEIGHT_BLOCKS;
        this.aabb.maxZ = this.aabb.minZ + RegionRenderer.REGION_SIZE_BLOCKS;

    }

}
