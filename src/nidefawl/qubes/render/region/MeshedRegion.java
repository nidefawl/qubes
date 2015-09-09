package nidefawl.qubes.render.region;

import static nidefawl.qubes.render.WorldRenderer.NUM_PASSES;
import static nidefawl.qubes.meshing.BlockFaceAttr.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import org.lwjgl.opengl.*;

import nidefawl.qubes.Game;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.meshing.BlockFaceAttr;
import nidefawl.qubes.vec.AABB;
import nidefawl.qubes.vec.AABBInt;

public class MeshedRegion {

    public int[]     vertexIndex   = new int[NUM_PASSES];
    public int[]     vertexCount   = new int[NUM_PASSES];
    public boolean[] hasPass       = new boolean[NUM_PASSES];
    public int        rX;
    public int        rZ;
    public int rY;
    public boolean    xNeg;
    public boolean    xPos;
    public boolean    zPos;
    public boolean    zNeg;
    public final AABBInt aabb          = new AABBInt();
    public int[]      frustumStates = new int[Engine.NUM_PROJECTIONS];
    public boolean    needsUpdate   = true;
    public boolean    isUpdating   = false;
    public boolean    isRenderable  = false;
    public boolean    isValid  = true;
    public int failedCached;
    public boolean hasAnyPass;

    
    public int[]     vbo;
    public int[][] buffer = new int[][] { new int[0],  new int[0], new int[0] };
    

    public MeshedRegion() {
        Arrays.fill(frustumStates, -2);
    }
    
    public static void enabledDefaultBlockPtrs() {
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 4, GL11.GL_FLOAT, false, BLOCK_VERT_BYTE_SIZE, 0);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("AttribPtr " + 0);
        int offset = 4;
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 3, GL11.GL_BYTE, false, BLOCK_VERT_BYTE_SIZE, offset * 4);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("AttribPtr " + 1);
        offset += 1; //5
        GL20.glEnableVertexAttribArray(2);
        GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, BLOCK_VERT_BYTE_SIZE, offset * 4);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("AttribPtr " + 2);
        offset += 2; //7
        GL20.glEnableVertexAttribArray(3);
        GL20.glVertexAttribPointer(3, 4, GL11.GL_UNSIGNED_BYTE, true, BLOCK_VERT_BYTE_SIZE, offset * 4);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("AttribPtr " + 3);
        offset += 1; //8
        GL20.glEnableVertexAttribArray(4);
        GL30.glVertexAttribIPointer(4, 4, GL11.GL_UNSIGNED_SHORT, BLOCK_VERT_BYTE_SIZE, offset * 4);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("AttribPtr " + 4);
        offset += 2;//10
        GL20.glEnableVertexAttribArray(5);
        GL30.glVertexAttribIPointer(5, 4, GL11.GL_UNSIGNED_SHORT, BLOCK_VERT_BYTE_SIZE, offset * 4);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("AttribPtr " + 5);
        offset += 2;//12
    }
    public void renderRegion(float fTime, int pass, int drawMode, int drawInstances) {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vbo[pass]);
        enableVertexPtrs(pass);
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
        disableVertexPtrs(pass);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }



    /**
     * @param pass
     */
    public static void enableVertexPtrs(int pass) {
        if (pass != 2) {
            enabledDefaultBlockPtrs();
        } else {
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 4, GL11.GL_FLOAT, false, PASS_2_BLOCK_VERT_BYTE_SIZE, 0);
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("AttribPtr " + 0);
        }
    }


    /**
     * @param pass
     */
    public static void disableVertexPtrs(int pass) {
        int attr = pass != 2 ? 6 : 1;
        for (int i = 0; i < attr; i++)
            GL20.glDisableVertexAttribArray(i);
    }

    public void preUploadBuffers() {
        if (this.vbo == null) {
            this.vbo = new int[NUM_PASSES];
            IntBuffer intbuf = Engine.glGenBuffers(this.vbo.length);
            for (int i = 0; i < vbo.length; i++) {
                vbo[i] = intbuf.get(i);
            }
        }
        Arrays.fill(this.hasPass, false);
        Arrays.fill(this.vertexCount, 0);
        this.hasAnyPass = false;
    }
    public void uploadBuffer(int pass, int[] buffer, int len, int numV) {
        this.buffer[pass] = Arrays.copyOf(buffer, len);
        ByteBuffer buf = Engine.getBuffer();
        IntBuffer intBuffer = Engine.getIntBuffer();
        intBuffer.clear();
        intBuffer.put(buffer, 0, len);
        buf.position(0).limit(len * 4);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo[pass]);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindBuffer " + vbo[pass]);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, len * 4L, buf, GL15.GL_STATIC_DRAW);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBufferData /" + intBuffer);

        this.vertexCount[pass] = numV;
        this.hasPass[pass] |= numV > 0;
        this.hasAnyPass |= numV > 0;

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }
    
    
    public void release() {
        if (this.vbo != null) {
            Engine.deleteBuffers(this.vbo);
            this.vbo = null;
        }
        this.isValid = false;
        this.isRenderable = false;
        Arrays.fill(frustumStates, -2);
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

    @Override
    public String toString() {
        return "MeshedRegion[x="+this.rX+",y="+this.rY+",z="+this.rZ+"]";
    }
    /**
     * @return
     */
    public boolean hasAnyPass() {
        return this.hasAnyPass;
    }
}
