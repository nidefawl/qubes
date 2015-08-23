package nidefawl.qubes.render;

import static org.lwjgl.opengl.GL11.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import static nidefawl.qubes.render.RegionRenderer.*;
import static nidefawl.qubes.render.WorldRenderer.*;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import nidefawl.qubes.Main;
import nidefawl.qubes.chunk.Region;
import nidefawl.qubes.gl.*;

public class MeshedRegion {
    public static final int LAYER_MAIN        = 0;
    public static final int LAYER_EXTRA_FACES_XPOS = 1;
    public static final int LAYER_EXTRA_FACES_XNEG = 2;
    public static final int LAYER_EXTRA_FACES_ZPOS = 3;
    public static final int LAYER_EXTRA_FACES_ZNEG = 4;
    public static final int NUM_LAYERS        = 5;
//    private RegionDisplayList    displayList;
    private int[]           vertexCount       = new int[NUM_PASSES*NUM_LAYERS];
    private int[]           vbo;
    private boolean[]       hasPass           = new boolean[NUM_PASSES];
    public int        rX;
    public int        rZ;
    public int              renderState       = RENDER_STATE_INIT;
    public boolean          isRenderable      = false;
    public boolean xNeg;
    public boolean xPos;
    public boolean zPos;
    public boolean zNeg;

//    public void swap(MeshedRegion meshedRegion) {
//        meshedRegion.displayList = this.displayList;
//        System.arraycopy(this.vertexCount, 0, meshedRegion.vertexCount, 0, this.vertexCount.length);
//        System.arraycopy(this.hasPass, 0, meshedRegion.hasPass, 0, this.hasPass.length);
//        meshedRegion.vertexCount = this.vertexCount;
//        meshedRegion.rX = this.rX;
//        meshedRegion.rZ = this.rZ;
//        meshedRegion.renderState = this.renderState;
//        meshedRegion.isRenderable = this.isRenderable;
//        this.displayList = null;
//    }
    public MeshedRegion(int regionX, int regionZ) {
        this.rX = regionX;
        this.rZ = regionZ;
    }

    public MeshedRegion() {
        // TODO Auto-generated constructor stub
    }

    public void renderRegion(float fTime, int pass, int layer, int drawMode) {
        int idx = pass + (NUM_PASSES * layer);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vbo[idx]);
        int stride = 13;
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 4, GL11.GL_FLOAT, false, stride*4, 0);
        if (Main.GL_ERROR_CHECKS) Engine.checkGLError("AttribPtr "+0);
        
        int offset = 4;
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 3, GL11.GL_BYTE, false, stride*4, offset*4);
        if (Main.GL_ERROR_CHECKS) Engine.checkGLError("AttribPtr "+1);
        offset+=1;
        GL20.glEnableVertexAttribArray(2);
        GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, stride*4, offset*4);
        if (Main.GL_ERROR_CHECKS) Engine.checkGLError("AttribPtr "+2);
        offset+=2;
        GL20.glEnableVertexAttribArray(3);
        GL20.glVertexAttribPointer(3, 4, GL11.GL_UNSIGNED_BYTE, true, stride*4, offset*4);
        if (Main.GL_ERROR_CHECKS) Engine.checkGLError("AttribPtr "+3);
        offset+=1;
        GL20.glEnableVertexAttribArray(4);
        GL20.glVertexAttribPointer(4, 2, GL11.GL_SHORT, false, stride*4, offset*4);
        if (Main.GL_ERROR_CHECKS) Engine.checkGLError("AttribPtr "+4);
        offset+=1;
        GL20.glEnableVertexAttribArray(5);
        GL20.glVertexAttribPointer(5, 4, GL11.GL_SHORT, false, stride*4, offset*4);
        if (Main.GL_ERROR_CHECKS) Engine.checkGLError("AttribPtr "+5);
        offset+=2;
//        GL11.gldrawe
        try {

            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("glDrawArrays ("+this.vertexCount[idx]+")");
        } catch (Exception e) {
            if (Main.ticksran%40==0)
            System.out.println(e.getMessage());
        }
        GL11.glDrawArrays(drawMode, 0, this.vertexCount[idx]);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
//        if (displayList != null && displayList.list > 0)
//            glCallList(displayList.list + idx);
//          glNewList(displayList.list + idx, GL11.GL_COMPILE);
//          Tess.instance.drawState(states[idx], GL_QUADS);
//          glEndList();
    }
    
    public void compileDisplayList(Tess[] states) {
        if (this.vbo == null) {
            this.vbo = new int[NUM_PASSES*NUM_LAYERS];
            ByteBuffer buf = Engine.getBuffer();
            buf.clear();
            IntBuffer intbuf = Engine.getIntBuffer();
            intbuf.clear();
            GL15.glGenBuffers(this.vbo.length, buf);
            for (int i = 0; i < vbo.length; i++) {
                vbo[i] = intbuf.get(i);
            }
        }
        Arrays.fill(this.hasPass, false);
        Arrays.fill(this.vertexCount, 0);
        
        ByteBuffer buf = Engine.getBuffer();
        IntBuffer intBuffer = Engine.getIntBuffer();
        for (int layer = 0; layer < NUM_LAYERS; layer++) {
            for (int pass = 0; pass < NUM_PASSES; pass++) {
                int idx = pass + (NUM_PASSES * layer);
                Tess tess = states[idx];
                int len = tess.getIdx(tess.vertexcount);
                intBuffer.clear();
                intBuffer.put(tess.rawBuffer, 0, len);
                buf.position(0).limit(len*4);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo[idx]);
                if (Main.GL_ERROR_CHECKS) Engine.checkGLError("glBindBuffer "+vbo[idx]);
//                System.out.println(buf);
//                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, len*4L, buf, GL15.GL_STATIC_DRAW);
                if (Main.GL_ERROR_CHECKS) Engine.checkGLError("glBufferData /"+intBuffer);
                
                vertexCount[idx] = tess.vertexcount;
                this.hasPass[pass] |= tess.vertexcount > 0;
            }
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        this.isRenderable = true;
    }

    public void release() {
        if (this.vbo != null) {
            ByteBuffer buf = Engine.getBuffer();
            IntBuffer intbuf = Engine.getIntBuffer();
            intbuf.clear();
            intbuf.put(this.vbo);
            buf.position(0).limit(this.vbo.length*4);
            GL15.glDeleteBuffers(this.vbo.length, buf);
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

    public void translate() {
        int xOff = this.rX << (Region.REGION_SIZE_BITS + 4);
        int zOff = this.rZ << (Region.REGION_SIZE_BITS + 4);
//        long seed = this.rX*19+this.rZ*23;
//        float yOffset = ((GameMath.randomI(seed)%10) / 20F)-0.25F;
        glTranslatef(xOff, 0, zOff);
    }

}
