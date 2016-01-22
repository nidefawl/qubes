package nidefawl.qubes.render.region;

import static nidefawl.qubes.render.WorldRenderer.NUM_PASSES;
import static nidefawl.qubes.meshing.BlockFaceAttr.*;

import java.nio.IntBuffer;
import java.util.Arrays;

import org.lwjgl.opengl.*;

import nidefawl.qubes.Game;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.util.Stats;
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

    
    public GLVBO[]     vbo;
    
    public GLVBO[]     vboIndices;
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
    
    public void renderRegion(float fTime, int pass) {
        Engine.bindBuffer(this.vbo[pass].getVboId());
        Engine.bindIndexBuffer(this.vboIndices[pass].getVboId());
        GL11.glDrawElements(GL11.GL_TRIANGLES, this.elementCount[pass]*3, GL11.GL_UNSIGNED_INT, 0);
        Stats.regionDrawCalls++;
    }



    public void preUploadBuffers() {
        if (this.vbo == null) {
            this.vbo = new GLVBO[NUM_PASSES];
            for (int i = 0; i < vbo.length; i++) {
                vbo[i] = new GLVBO(GL15.GL_STATIC_DRAW);
                vbo[i].getVboId();
            }
        }
        if (this.vboIndices == null) {
            this.vboIndices = new GLVBO[NUM_PASSES];
            for (int i = 0; i < vboIndices.length; i++) {
                vboIndices[i] = new GLVBO(GL15.GL_STATIC_DRAW);
                vboIndices[i].getVboId();
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
        this.elementCount[pass] = numF * 2;
        this.hasPass[pass] |= numV > 0;
        this.hasAnyPass |= numV > 0;
        this.shadowDrawMode = shadowDrawMode;
        int bufIdx = (nextBuffer++) % 4;
        ReallocIntBuffer buf = RegionRenderer.buffers[bufIdx * 4 + pass];
        ReallocIntBuffer shBuffer = RegionRenderer.idxShortBuffers[pass];
        int intlen = buffer.storeVertexData(buf);
        int intlenIdx = buffer.storeIndexData(shBuffer);
        vbo[pass].upload(GL15.GL_ARRAY_BUFFER, buf.getByteBuf(), intlen * 4L);
        vboIndices[pass].upload(GL15.GL_ELEMENT_ARRAY_BUFFER, shBuffer.getByteBuf(), intlenIdx * 4L);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        int byteSize = (intlenIdx * 4) + (intlen * 4);
        
        if (alloc[pass] != byteSize) {
            totalBytes -= this.alloc[pass];
            totalBytesPass[pass] -= this.alloc[pass];
            this.alloc[pass] = byteSize;
            totalBytes += this.alloc[pass];
            totalBytesPass[pass] += this.alloc[pass];
        }
    }
    
    
    public void release() {
        if (this.vbo != null) {
            for (int i = 0; i < vbo.length; i++) {
                this.vbo[i].release();
            }
            this.vbo = null;
        }
        if (this.vboIndices != null) {
            for (int i = 0; i < vboIndices.length; i++) {
                this.vboIndices[i].release();
            }
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
    public int getShadowDrawMode() {
        return this.shadowDrawMode;
    }
}
