package nidefawl.qubes.render;

import static org.lwjgl.opengl.GL11.*;
import static nidefawl.qubes.render.RegionRenderer.*;
import static nidefawl.qubes.render.WorldRenderer.*;

import org.lwjgl.opengl.GL11;

import nidefawl.qubes.chunk.Region;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.util.GameMath;

public class MeshedRegion {
    public static final int LAYER_MAIN        = 0;
    public static final int LAYER_EXTRA_FACES_XPOS = 1;
    public static final int LAYER_EXTRA_FACES_XNEG = 2;
    public static final int LAYER_EXTRA_FACES_ZPOS = 3;
    public static final int LAYER_EXTRA_FACES_ZNEG = 4;
    public static final int NUM_LAYERS        = 5;
    private RegionDisplayList    displayList;
    private int[]           vertexCount       = new int[NUM_PASSES];
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

    public void renderRegion(float fTime, int pass, int layer) {
        int idx = pass + (NUM_PASSES * layer);
        if (displayList != null && displayList.list > 0)
            glCallList(displayList.list + idx);
    }

    public void compileDisplayList(TesselatorState[] states) {
        if (displayList == null)
            displayList = Engine.nextFreeDisplayList();

        for (int pass = 0; pass < NUM_PASSES; pass++) {
            vertexCount[pass] = 0;
            this.hasPass[pass] = false;
        }
        for (int layer = 0; layer < NUM_LAYERS; layer++) {
            for (int pass = 0; pass < NUM_PASSES; pass++) {
                int idx = pass + (NUM_PASSES * layer);
                glNewList(displayList.list + idx, GL11.GL_COMPILE);
                Tess.instance.drawState(states[idx], GL_QUADS);
                glEndList();
                int nFaces = states[idx].vertexcount;
                vertexCount[pass] += nFaces;
                this.hasPass[pass] |= nFaces > 0;
            }
        }
        this.isRenderable = true;
        //        this.meshes = null;
    }

    public void release() {
        if (displayList != null) {
            Engine.release(displayList);
            displayList = null;
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
