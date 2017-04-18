package nidefawl.qubes.render.impl.vk;

import static nidefawl.qubes.render.WorldRenderer.PASS_LOD;
import static nidefawl.qubes.render.WorldRenderer.PASS_SHADOW_SOLID;
import static nidefawl.qubes.render.WorldRenderer.PASS_SOLID;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Queues;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.render.region.MeshedRegion;
import nidefawl.qubes.render.region.RegionRenderer;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.IRenderComponent;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.vec.Frustum;
import nidefawl.qubes.vulkan.CommandBuffer;
import nidefawl.qubes.world.World;

public class RegionRendererVK extends RegionRenderer implements IRenderComponent {

    int framecall = 0;
    ArrayDeque<MeshedRegion> queue = Queues.newArrayDeque();
    ArrayList<MeshedRegion> regions = new ArrayList<>();
    public void renderMainTrav(CommandBuffer commandBuffer, World world, float fTime) {
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
                    r.renderRegionVK(commandBuffer, fTime, PASS_SOLID);
                    this.numV += r.getNumVertices(PASS_SOLID);
                    this.rendered++;  
                }
                if (dist == 1) {
                    if (r.hasPass(PASS_LOD)) {
                        r.renderRegionVK(commandBuffer, fTime, PASS_LOD);
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
        numV=totalv;
    }
    
    public void renderMain(CommandBuffer commandBuffer, World world, float fTime) {
        int size = renderList.size();
        this.occlCulled=0;
        this.numV = 0;
        int totalv=0;
        int LOD_DISTANCE = 16; //TODO: move solid/slab blocks out of LOD PASS
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

                if (ENABLE_OCCL && r.distance > MIN_DIST_OCCL && r.occlusionResult == 1) {
                    this.occlCulled++;
                    continue;
                }
                if (dist == 0)
                if (r.hasPass(PASS_SOLID)) {
                    //            System.out.println(glGetInteger(GL_DEPTH_FUNC));
                    r.renderRegionVK(commandBuffer, fTime, PASS_SOLID);
                    this.numV += r.getNumVertices(PASS_SOLID);
                    this.rendered++;  
                }
                if (dist == 1) {
                    if (r.hasPass(PASS_LOD)) {
                        r.renderRegionVK(commandBuffer, fTime, PASS_LOD);
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
        numV=totalv;
    
    }

    public void renderRegions(CommandBuffer commandBuffer, World world, float fTime, int pass, int nFrustum, int frustumState) {
        int requiredShadowMode = Game.instance.settings.renderSettings.shadowDrawMode;
        List<MeshedRegion> list = pass == PASS_SHADOW_SOLID ? this.shadowRenderList : this.renderList;
        int size = list.size();
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
            r.renderRegionVK(commandBuffer, fTime, pass);
        }
        
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("renderRegions");
    
    }

    @Override
    public void updateOcclQueries() {
    }
    

}
