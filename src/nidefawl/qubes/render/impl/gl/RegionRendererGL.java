package nidefawl.qubes.render.impl.gl;

import static nidefawl.qubes.render.WorldRenderer.PASS_LOD;
import static nidefawl.qubes.render.WorldRenderer.PASS_SHADOW_SOLID;
import static nidefawl.qubes.render.WorldRenderer.PASS_SOLID;

import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.ARBOcclusionQuery2;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import com.google.common.collect.Queues;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.render.RenderersGL;
import nidefawl.qubes.render.region.MeshedRegion;
import nidefawl.qubes.render.region.RegionRenderer;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.ShaderCompileError;
import nidefawl.qubes.util.Stats;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.vec.Frustum;
import nidefawl.qubes.vulkan.CommandBuffer;
import nidefawl.qubes.world.World;

public class RegionRendererGL extends RegionRenderer {
    
    
    private Shader occlQueryShader;
    private boolean startup;
    private int[] occlQueries = new int[MAX_OCCL_QUERIES];
    protected static MultiDrawIndirectBuffer buffer = new MultiDrawIndirectBuffer();
    
    
    /*
     * typedef struct {
     *   GLuint   index;
     *   GLuint   reserved;
     *   GLuint64 address;
     *   GLuint64 length;
     * } BindlessPtrNV;
     * 
     * typedef struct {
     *   uint count;
     *   uint primCount;
     *   uint firstIndex;
     *   uint baseVertex;
     *   uint baseInstance;
     *   DrawElementsIndirectCommand cmd;
     *   GLuint                      reserved;
     *   BindlessPtrNV               indexBuffer;
     *   BindlessPtrNV               vertexBuffers[];
     * } DrawElementsIndirectBindlessCommandNV;
     */
    

    public void init() {
        initShaders();
        IntBuffer intbuf = Engine.glGenBuffers(this.occlQueries.length);
        intbuf.get(this.occlQueries);
        super.init();
    }
    
    public void initShaders() {
        try {
            pushCurrentShaders();
            AssetManager assetMgr = AssetManager.getInstance();
            Shader new_occlQueryShader = assetMgr.loadShader(this, "terrain/occlusion_query");
            popNewShaders();
            occlQueryShader = new_occlQueryShader;
            startup = false;
        } catch (ShaderCompileError e) {
            releaseNewShaders();
            System.out.println("shader " + e.getName() + " failed to compile");
            System.out.println(e.getLog());
            if (startup) {
                throw e;
            } else {
                Game.instance.addDebugOnScreen("\0uff3333shader " + e.getName() + " failed to compile");
            }
        }
        startup = false;
    }

    int framecall = 0;
    ArrayDeque<MeshedRegion> queue = Queues.newArrayDeque();
    ArrayList<MeshedRegion> regions = new ArrayList<>();
    public void renderMainTrav(float fTime) {
        framecall++;
        int rY = rChunkY < 0 ? 0 : rChunkY >= HEIGHT_SLICES ? HEIGHT_SLICES-1 : rChunkY;
        regions.clear();
        MeshedRegion start = getByRegionCoord(rChunkX, rY, rChunkZ);
        if (start != null) {
            start.setFrame(framecall);
            start.facing = -1;
            start.traverseDirs = 0;
            queue.add(start);
            while (!queue.isEmpty()) {
                MeshedRegion region = queue.poll(); 
                regions.add(region);
                for (int dir = 0; dir < 6; dir++) {
                    if ((region.traverseDirs & (1 << Dir.opposite(dir))) > 0) {
                        continue;
                    }
                    if (region.facing > -1 && region.visCache != null && !region.visCache.isVisible(Dir.opposite(region.facing), dir)) {
                        continue;
                    }
                    MeshedRegion next = getByRegionCoord(region.rX+Dir.getDirX(dir), region.rY+Dir.getDirY(dir), region.rZ+Dir.getDirZ(dir));
                    if (next == null || next.frame == framecall) {
                        continue;
                    }
                    if (!next.isRenderable) {
                        continue;
                    }
                    next.setFrame(framecall);
                    if (next.isRenderable && next.hasAnyPass() && next.frustumStates[0] < Frustum.FRUSTUM_INSIDE) {
                        continue;
                    }
                    next.facing = dir;
                    next.traverseDirs = (region.traverseDirs) | (1<<dir);
                    queue.add(next);
                }
            }
        }

        int size = regions.size();
//        long ld = System.currentTimeMillis();
//        double ltd = (ld%4000L) / 4000.0;
//        size = GameMath.floor(size*ltd);
//        System.out.println(size);
        this.occlCulled=0;
        this.numV = 0;
        int totalv=0;
        int LOD_DISTANCE = 16; //TODO: move solid/slab blocks out of LOD PASS
        boolean bindless = GL.isBindlessSuppported() && Engine.userSettingUseBindless;
        if (bindless) {
            buffer.preDraw(GLVAO.vaoBlocksBindless);
        } else {
            Engine.bindVAO(GLVAO.vaoBlocksBindless);
        }
        for (int dist = 0; dist < 2; dist++)  {
            for (int i = 0; i < size; i++) {
                MeshedRegion r = regions.get(i);
                if (!r.hasAnyPass()) {
                    continue;
                }
                if (r.frustumStates[0] < Frustum.FRUSTUM_INSIDE) {
                    continue;
                }
                if ((dist == 1) && (r.distance > LOD_DISTANCE)) continue;

                if (ENABLE_OCCL && r.distance > MIN_DIST_OCCL && r.occlusionResult == 1) {
                    this.occlCulled++;
                    continue;
                }
                if (dist == 0)
                if (r.hasPass(PASS_SOLID)) {
                    //            System.out.println(glGetInteger(GL_DEPTH_FUNC));
                    if (bindless) {
                        r.renderRegionIndirect(buffer, PASS_SOLID);
                    } else {
                        r.renderRegion(fTime, PASS_SOLID);
                    }
                    this.numV += r.getNumVertices(PASS_SOLID);
                    this.rendered++;  
                }
                if (dist == 1) {
                    if (r.hasPass(PASS_LOD)) {
                        if (bindless) {
                            r.renderRegionIndirect(buffer, PASS_LOD);
                        } else {
                            r.renderRegion(fTime, PASS_LOD);
                        }
                        this.numV += r.getNumVertices(PASS_LOD);
                        this.rendered++;  
                    }
//                    if (numV > 1000000) {
//                        break;
//                    }
                }
//                if (numV > 3000000) {
//                    break;
//                }
            }
            totalv+=numV;
            numV=0;
//            if (dist == 0) {
//                cur = worldRenderer.terrainShaderFar;
//                cur.enable();
//            }
        }
//        if (occlTestThisFrame > 0) {
//            System.out.println(occlTestThisFrame);
//        }
//        
        if (bindless && buffer.getDrawCount() > 0) {
            buffer.render();
            Stats.regionDrawCalls++;
        }
        numV=totalv;
    }

    public void renderMain(float fTime) {
        int size = renderList.size();
        this.occlCulled=0;
        this.numV = 0;
        int totalv=0;
        int LOD_DISTANCE = 16; //TODO: move solid/slab blocks out of LOD PASS
        Shader cur = RenderersGL.worldRenderer.terrainShader;
        int occlTestThisFrame = 0;
        boolean bindless = GL.isBindlessSuppported() && Engine.userSettingUseBindless;
        Engine.bindVAO(GLVAO.vaoBlocksBindless);
        if (bindless) {
            buffer.preDraw(GLVAO.vaoBlocksBindless);
        }
        for (int dist = 0; dist < 2; dist++)  {
            for (int i = 0; i < size; i++) {
                MeshedRegion r = renderList.get(i);
                if (!r.hasAnyPass()) {
                    continue;
                }
                if (r.frustumStates[0] < Frustum.FRUSTUM_INSIDE) {
                    continue;
                }
                if ((dist == 1) && (r.distance > LOD_DISTANCE)) continue;
                if (ENABLE_OCCL && occlTestThisFrame < 1&& queriesRunning < occlQueriesRunning.length 
                        && r.distance > MIN_DIST_OCCL 
                        && r.frustumStates[0] >= MIN_STATE_OCCL) {
                    if (r.occlusionQueryState == 0 && r.occlusionResult < 1) {
                        int idx = -1;
                        int j = 0;
                        for (; j < this.occlQueriesRunning.length; j++) {
                            if (this.occlQueriesRunning[j] == null) {
                                idx = j;
                                break;
                            }
                        }
                        if (bindless && buffer.getDrawCount() > 0) {
                            Stats.regionDrawCalls++;
                            buffer.render();
                            buffer.preDraw(GLVAO.vaoBlocksBindless);
                        }
                        occlTestThisFrame++;
                        r.occlusionQueryState = 1;
                        occlQueriesRunning[idx] = r;
                        r.queryPos.set(camX, camY, camZ);
                        occlQueryShader.enable();
                        GL15.glBeginQuery(ARBOcclusionQuery2.GL_ANY_SAMPLES_PASSED, occlQueries[idx]);
                        GL11.glColorMask(false, false, false, false);
                        Engine.enableDepthMask(false);
                        if (r.getShadowDrawMode() == 1) {
                            Engine.bindVAO(GLVAO.vaoBlocksShadowTextured);
                        } else {
                            Engine.bindVAO(GLVAO.vaoBlocksShadow);
                        }
                        r.renderRegion(fTime, PASS_SHADOW_SOLID);
//                        r.renderRegion(fTime, PASS_TRANSPARENT);
//                        r.renderRegion(fTime, PASS_LOD);
                        Engine.bindVAO(GLVAO.vaoBlocksBindless);
                        cur.enable();
                        GL11.glColorMask(true, true, true, true);
                        Engine.enableDepthMask(true);
                        GL15.glEndQuery(ARBOcclusionQuery2.GL_ANY_SAMPLES_PASSED);
                        queriesRunning++;
                    }
                }
                this.rendered++;  
                if (ENABLE_OCCL && r.distance > MIN_DIST_OCCL && r.occlusionResult == 1) {
                    this.occlCulled++;
                    continue;
                }
                if (dist == 0)
                if (r.hasPass(PASS_SOLID)) {
                    //            System.out.println(glGetInteger(GL_DEPTH_FUNC));
                    if (bindless) {
                        r.renderRegionIndirect(buffer, PASS_SOLID);
                    } else {
                        r.renderRegion(fTime, PASS_SOLID);
                    }
                    this.numV += r.getNumVertices(PASS_SOLID);
                }
                if (dist == 1) {
                    if (r.hasPass(PASS_LOD)) {
                        if (bindless) {
                            r.renderRegionIndirect(buffer, PASS_LOD);
                        } else {
                            r.renderRegion(fTime, PASS_LOD);
                        }
                        this.numV += r.getNumVertices(PASS_LOD);
                    }
                    if (numV > 1000000) {
                        break;
                    }
                }
                if (numV > 3000000) {
                    break;
                }
            }
            totalv+=numV;
            numV=0;
//            if (dist == 0) {
//                cur = worldRenderer.terrainShaderFar;
//                cur.enable();
//            }
        }
        if (bindless && buffer.getDrawCount() > 0) {
            buffer.render();
            Stats.regionDrawCalls++;
        }
//        if (occlTestThisFrame > 0) {
//            System.out.println(occlTestThisFrame);
//        }
//        
        numV=totalv;
    }

    public void renderRegions(World world, float fTime, int pass, int nFrustum, int frustumState) {
        GLVAO vao = GLVAO.vaoBlocks;
        if (pass == PASS_SHADOW_SOLID) {
            vao = GLVAO.vaoBlocksShadow;
            if (GameBase.getSettings().renderSettings.shadowDrawMode == 1) {
                vao = GLVAO.vaoBlocksShadowTextured;
            }
        }
        int requiredShadowMode = GameBase.getSettings().renderSettings.shadowDrawMode;
        boolean bindless = GL.isBindlessSuppported() && Engine.userSettingUseBindless;

        Engine.bindVAO(vao);
        List<MeshedRegion> list = pass == PASS_SHADOW_SOLID ? this.shadowRenderList : this.renderList;
        int size = list.size();
        if (bindless) {
            buffer.preDraw(vao);
        }
        int nDraw = 0;
        for (int i = 0; i < size; i++) {
            MeshedRegion r = list.get(i);
            if (!r.hasPass(pass)) {
                continue;
            }
            if (r.frustumStates[nFrustum] < frustumState) {
                continue;
            }
            if (r.getShadowDrawMode() != requiredShadowMode) {
                continue;
            }
            if (bindless) {
                r.renderRegionIndirect(buffer, pass);
            } else {
                r.renderRegion(fTime, pass);
//                if (pass == PASS_SHADOW_SOLID) {
//                    System.out.println(r.vertexCount[pass]);
//                }
                nDraw++;
            }
//            if (vao == GLVAO.vaoBlocksShadow ){
//                System.out.println("success "+i);
//            }
        }
        
        if (pass == PASS_SHADOW_SOLID) {
//            System.out.println(r.vertexCount[pass]);
//            System.out.println(nDraw);
        }
        if (bindless && buffer.getDrawCount() > 0) {
            buffer.render();
        }
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("renderRegions");
    }
    
    @Override
    public void updateOcclQueries() {

        for (int i = 0; ENABLE_OCCL && i < occlQueriesRunning.length; i++) {
            MeshedRegion mr = occlQueriesRunning[i];
            if (mr == null)
                continue;
            if (mr.isValid && mr.occlusionQueryState == 1) {
                if (mr.queryPos.distanceSq(camX, camY, camZ) < 3) {
                    int done = GL15.glGetQueryObjecti(occlQueries[i], GL15.GL_QUERY_RESULT_AVAILABLE);
                    if (done == 0) {
                        continue;
                    }
                    mr.occlusionResult = 1+GL15.glGetQueryObjecti(occlQueries[i], GL15.GL_QUERY_RESULT);
                    mr.occlFrameSkips = 0;
//                    System.out.println("got result for "+mr+" = "+mr.occlusionResult+ " - "+mr.getNumVertices(0));
                    mr.occlusionQueryState = 0;
                }
            }
            occlQueriesRunning[i] = null;
            queriesRunning--;
        
        }
    }
    
    @Override
    public void resize(int displayWidth, int displayHeight) {
    }

}
