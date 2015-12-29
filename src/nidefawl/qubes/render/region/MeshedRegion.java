package nidefawl.qubes.render.region;

import static nidefawl.qubes.render.WorldRenderer.NUM_PASSES;
import static nidefawl.qubes.meshing.BlockFaceAttr.*;

import java.nio.IntBuffer;
import java.util.Arrays;

import org.lwjgl.opengl.*;

import nidefawl.qubes.Game;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.ReallocIntBuffer;
import nidefawl.qubes.gl.VertexBuffer;
import nidefawl.qubes.vec.AABBInt;
import nidefawl.qubes.vec.Vector3f;

public class MeshedRegion {

    public int[]     vertexCount   = new int[NUM_PASSES];
    public int[]     elementCount   = new int[NUM_PASSES];
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
    
    public int[]     vboIndices;
    private int shadowDrawMode;
    public boolean frustumStateChanged;
    int occlusionQueryState = 0; //0 init, 1 waiting, 2 waiting + drop result
    public int occlusionResult = 0;//0 no result, 1 visible, 2 occluded
    public int occlFrameSkips = 0;
    public int distance;
    Vector3f queryPos = new Vector3f();

    public MeshedRegion() {
        Arrays.fill(frustumStates, -2);
    }
    
    public static void enabledDefaultBlockPtrs() {

        final int VERT_LEN = (BLOCK_VERT_INT_SIZE)<<2;
        //POS
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, VERT_LEN, 0);
        int offset = 3;
        //NORMAL
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 3, GL11.GL_BYTE, false, VERT_LEN, offset * 4);
        offset += 1; 
        
        //1 BYTE UNUSED (normal has 3 bytes)
        
        //TEXCOORD
        GL20.glEnableVertexAttribArray(2);
        GL20.glVertexAttribPointer(2, 2, GL30.GL_HALF_FLOAT, false, VERT_LEN, offset * 4);
        offset += 1; 
        //COLOR
        GL20.glEnableVertexAttribArray(3);
        GL20.glVertexAttribPointer(3, 4, GL11.GL_UNSIGNED_BYTE, true, VERT_LEN, offset * 4);
        offset += 1; 
        //BLOCKINFO
        GL20.glEnableVertexAttribArray(4);
        GL30.glVertexAttribIPointer(4, 4, GL11.GL_UNSIGNED_SHORT, VERT_LEN, offset * 4);
        offset += 2;
        //LIGHTINFO
        GL20.glEnableVertexAttribArray(5);
        GL30.glVertexAttribIPointer(5, 2, GL11.GL_UNSIGNED_SHORT, VERT_LEN, offset * 4);
        offset += 1;
    
    }
    public void renderRegion(float fTime, int pass, int drawMode, int drawInstances) {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vbo[pass]);
        int ptrSetting = pass;
        if (ptrSetting == 2) {
            if (this.shadowDrawMode == 1) {
                ptrSetting = 4;
            }
        }
        enableVertexPtrs(ptrSetting);
        if (Engine.USE_TRIANGLES) {
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.vboIndices[pass]);
            if (drawInstances > 0) {
                GL31.glDrawElementsInstanced(drawMode, this.elementCount[pass]*3, GL11.GL_UNSIGNED_SHORT, 0, drawInstances);
            } else {
                GL11.glDrawElements(drawMode, this.elementCount[pass]*3, GL11.GL_UNSIGNED_INT, 0);
            }
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        } else {

            if (drawInstances > 0) {
                GL31.glDrawArraysInstanced(drawMode, 0, this.vertexCount[pass], drawInstances);
            } else {
                GL11.glDrawArrays(drawMode, 0, this.vertexCount[pass]);
            }
        }
        disableVertexPtrs(ptrSetting);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }



    /**
     * @param pass
     */
    public static void enableVertexPtrs(int pass) {
        switch (pass) {
            case 0:
            case 1:
            case 3:
                enabledDefaultBlockPtrs();
                return;
            case 2:
                GL20.glEnableVertexAttribArray(0);
                GL20.glVertexAttribPointer(0, 4, GL11.GL_FLOAT, false, PASS_2_BLOCK_VERT_BYTE_SIZE, 0);
                if (Game.GL_ERROR_CHECKS)
                    Engine.checkGLError("AttribPtr " + 0);
                return;
            case 4:
                {
                    GL20.glEnableVertexAttribArray(0);
                    GL20.glVertexAttribPointer(0, 4, GL11.GL_FLOAT, false, PASS_3_BLOCK_VERT_BYTE_SIZE, 0);
                    if (Game.GL_ERROR_CHECKS)
                        Engine.checkGLError("AttribPtr " + 0);
                    int offset = 4;
                    //1 == normal == unused
                    GL20.glEnableVertexAttribArray(1);
                    GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, PASS_3_BLOCK_VERT_BYTE_SIZE, offset * 4);
                    if (Game.GL_ERROR_CHECKS)
                        Engine.checkGLError("AttribPtr " + 1);
                    offset += 2; 
                    
                    GL20.glEnableVertexAttribArray(2);
                    GL30.glVertexAttribIPointer(2, 2, GL11.GL_UNSIGNED_SHORT, PASS_3_BLOCK_VERT_BYTE_SIZE, offset * 4);
                    if (Game.GL_ERROR_CHECKS)
                        Engine.checkGLError("AttribPtr " + 2);
                    offset += 1;

                }
                return;
            case 5:
            {

                final int VERT_LEN = (6)<<2;
                //POS
                GL20.glEnableVertexAttribArray(0);
                GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, VERT_LEN, 0);
                int offset = 3;
                //NORMAL
                GL20.glEnableVertexAttribArray(1);
                GL20.glVertexAttribPointer(1, 3, GL11.GL_BYTE, false, VERT_LEN, offset * 4);
                offset += 1; 
                
                //1 BYTE UNUSED (normal has 3 bytes)
                
                //TEXCOORD
                GL20.glEnableVertexAttribArray(2);
                GL20.glVertexAttribPointer(2, 2, GL30.GL_HALF_FLOAT, false, VERT_LEN, offset * 4);
                offset += 1; 
                //COLOR
                GL20.glEnableVertexAttribArray(3);
                GL20.glVertexAttribPointer(3, 4, GL11.GL_UNSIGNED_BYTE, true, VERT_LEN, offset * 4);
                offset += 1; 
            }
                return;
                
        }
    }


    /**
     * @param ptrSetting
     */
    public static void disableVertexPtrs(int ptrSetting) {
        if (ptrSetting < 2 || ptrSetting == 3) {
            for (int i = 0; i < 6; i++)
                GL20.glDisableVertexAttribArray(i);
        } else {
            GL20.glDisableVertexAttribArray(0);
            if (ptrSetting == 3||ptrSetting == 5) {
                GL20.glDisableVertexAttribArray(1);
                GL20.glDisableVertexAttribArray(2);
            }
            if (ptrSetting == 5) {
                GL20.glDisableVertexAttribArray(3);
            }
        }
    }

    public void preUploadBuffers() {
        if (this.vbo == null) {
            this.vbo = new int[NUM_PASSES];
            IntBuffer intbuf = Engine.glGenBuffers(this.vbo.length);
            for (int i = 0; i < vbo.length; i++) {
                vbo[i] = intbuf.get(i);
            }
        }
        if (this.vboIndices == null) {
            this.vboIndices = new int[NUM_PASSES];
            IntBuffer intbuf = Engine.glGenBuffers(this.vboIndices.length);
            for (int i = 0; i < vboIndices.length; i++) {
                vboIndices[i] = intbuf.get(i);
            }
        }
        Arrays.fill(this.hasPass, false);
        Arrays.fill(this.vertexCount, 0);
        Arrays.fill(this.elementCount, 0);
        this.hasAnyPass = false;
    }
    public static long totalBytes = 0;
    long alloc[] = new long[NUM_PASSES];
    public static long totalBytesPass[] = new long[NUM_PASSES];
    static int nextBuffer = 0;
    public void uploadBuffer(int pass, VertexBuffer buffer, int shadowDrawMode) {
        int numV = buffer.getVertexCount();
        int numF = buffer.getFaceCount();
        this.vertexCount[pass] = numV;
        this.elementCount[pass] = numF*2;
        this.hasPass[pass] |= numV > 0;
        this.hasAnyPass |= numV > 0;
        this.shadowDrawMode = shadowDrawMode;
        int bufIdx = (nextBuffer++)%4;
        ReallocIntBuffer buf = RegionRenderer.buffers[bufIdx*4+pass];
        int intlen = buffer.putIn(buf);
        
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo[pass]);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindBuffer " + vbo[pass]);
        if (alloc[pass] != intlen * 4) {
            totalBytes -= this.alloc[pass];
            totalBytesPass[pass] -= this.alloc[pass];
            this.alloc[pass] = intlen * 4L;
            totalBytes += this.alloc[pass];
            totalBytesPass[pass] += this.alloc[pass];
        }
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, intlen * 4L, buf.getByteBuf(), GL15.GL_STATIC_DRAW);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("glBufferData /" + buf);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        if (Engine.USE_TRIANGLES) {
           ReallocIntBuffer shBuffer = RegionRenderer.idxShortBuffers[pass];
//           int intLen = VertexBuffer.createIndex(this.elementCount[pass], shBuffer);
            int intLen = buffer.getTriIdxPos();
//            System.out.println("number tri idx on pass "+pass+" - "+intLen);
            shBuffer.put(buffer.getTriIdxBuffer(), 0, intLen);
//            System.out.println(byteSizeBuffer+"/"+(nTriangleIdx*2));
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboIndices[pass]);
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("glBindBuffer " + vboIndices[pass]);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, intLen*4, shBuffer.getByteBuf(), GL15.GL_STATIC_DRAW);
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("glBufferData /" + shBuffer);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
            
        }
    }
    
    
    public void release() {
        if (this.vbo != null) {
            Engine.deleteBuffers(this.vbo);
            this.vbo = null;
        }
        if (this.vboIndices != null) {
            Engine.deleteBuffers(this.vboIndices);
            this.vboIndices = null;
        }
        this.isValid = false;
        this.isRenderable = false;
        Arrays.fill(frustumStates, -2);
        for (int a = 0; a < NUM_PASSES; a++) {
            totalBytes-=this.alloc[a];
            totalBytesPass[a]-=this.alloc[a];
        }
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
        this.aabb.minX -= Engine.GLOBAL_OFFSET.x;
        this.aabb.minZ -= Engine.GLOBAL_OFFSET.z;
        this.aabb.maxX -= Engine.GLOBAL_OFFSET.x;
        this.aabb.maxZ -= Engine.GLOBAL_OFFSET.z;
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
