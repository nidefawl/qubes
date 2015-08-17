package nidefawl.qubes.chunk;

import static org.lwjgl.opengl.GL11.*;

import java.util.Arrays;
import java.util.LinkedList;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.render.WorldRenderer;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.vec.Mesh;
import nidefawl.qubes.world.World;

import org.lwjgl.opengl.GL11;

public class Region {
    public static final int REGION_SIZE_BITS = 2;
    public static final int REGION_SIZE      = 1 << REGION_SIZE_BITS;
    public static final int REGION_SIZE_MASK = REGION_SIZE - 1;
    public static final int STATE_INIT = 0;
    public static final int STATE_LOADING = 1;
    public static final int STATE_LOAD_COMPLETE = 2;
    //TODO: abstract render regions
    public static final int RENDER_STATE_INIT     = 0;
    public static final int RENDER_STATE_MESHING  = 1;
    public static final int RENDER_STATE_MESHED   = 2;
    public static final int RENDER_STATE_COMPILED = 3;
    
    public final int        rX;
    public final int        rZ;
    public final Chunk[][]         chunks           = new Chunk[REGION_SIZE][REGION_SIZE];
    public boolean         isCompiled;
    private DisplayList     displayList;
    private int[]           facesRendered    = new int[WorldRenderer.NUM_PASSES];
    private boolean[]       hasPass          = new boolean[WorldRenderer.NUM_PASSES];
    int                     lastcolor        = 0;
    public long             createTime       = System.currentTimeMillis();
    public int              index;

    public int        state            = STATE_INIT;
    public int        renderState            = RENDER_STATE_INIT;
    
    public Region(int regionX, int regionZ) {
        this.rX = regionX;
        this.rZ = regionZ;
        this.isCompiled = false;
        this.displayList = null;
    }

    public boolean isRendered() {
        return isCompiled;
    }

    public void setRendered(boolean isRendered) {
        this.isCompiled = isRendered;
    }
    boolean isEmpty = false;

    public boolean isEmpty() {
        return isEmpty;
    }
    public void generate(World world) {
        this.isEmpty = true;
        for (int x = 0; x < REGION_SIZE; x++) {
            for (int z = 0; z < REGION_SIZE; z++) {
                chunks[x][z] = world.generateChunk(rX << REGION_SIZE_BITS | x, rZ << REGION_SIZE_BITS | z);
                chunks[x][z].checkIsEmtpy();
                if (!chunks[x][z].isEmpty()) {
                    this.isEmpty = false;
                }
            }
        }
    }

    public int getHighestBlock() {
        int topBlock = 0;
        for (int i = 0; i < REGION_SIZE; i++) {
            for (int k = 0; k < REGION_SIZE; k++) {
                int y = chunks[i][k].getTopBlock();
                if (y > topBlock) {
                    topBlock = y;
                }
            }
        }
        return topBlock;
    }

    public void renderRegion(WorldRenderer renderer, float fTime, int pass) {
        if (displayList != null && displayList.list > 0)
            glCallList(displayList.list + pass);
    }

    public void flushBlockData() {
        for (int x = 0; x < REGION_SIZE; x++) {
            for (int z = 0; z < REGION_SIZE; z++) {
                chunks[x][z].blocks = null;
            }
        }
    }

    public boolean hasBlockData() {
        return chunks[0][0].blocks != null;
    }

    public int getFacesRendered(int i) {
        return facesRendered[i];
    }

    public void compileDisplayList(TesselatorState[] states) {
        if (isCompiled) {
            System.err.println("Already rendered!");
            return;
        }
        isCompiled = true;
        if (displayList == null)
            displayList = Engine.nextFreeDisplayList();
        this.lastcolor = 0;
        
        for (int pass = 0; pass < WorldRenderer.NUM_PASSES; pass++) {
            facesRendered[pass] = 0;
            glNewList(displayList.list + pass, GL11.GL_COMPILE);
            Tess.instance.drawState(states[pass], GL_QUADS);
            glEndList();
            int nFaces = states[pass].vertexcount/4;
            facesRendered[pass] += nFaces;
            this.hasPass[pass] = nFaces > 0;
        }
//        this.meshes = null;
    }


    public void release() {
        if (displayList != null) {
            Engine.release(displayList);
            displayList = null;
        }
        this.isCompiled = false;
    }

    public boolean hasPass(int i) {
        return this.hasPass[i];
    }

    public void setChunk(int x, int z, Chunk c) {
        this.chunks[x][z] = c;
    }


    public final int getTypeId(int i, int j, int k) {
        int id = chunks[i >> 4][k >> 4].getTypeId(i & 0xF, j, k & 0xF);
        return id;
    }

    public final int getBiome(int i, int j, int k) {
        Chunk c = chunks[i >> 4][k >> 4];
        return c.getBiome(i & 0xF, j, k & 0xF);
    }
}
