package nidefawl.qubes.chunk;

import java.util.Iterator;

public class RegionLoader {
    public static int              LOAD_DIST        = 1;
    //1 << (8-Region.REGION_SIZE_BITS*Region.REGION_SIZE_BITS);
    public static final int MAX_REGIONS      = (LOAD_DIST * 2 + 1) * (LOAD_DIST * 2 + 1);
    final RegionTable       regions;
    final int[][]           direction        = new int[][] { { 1, 0 }, { 0, 1 }, { -1, 0 }, { 0, -1 } };
    final RegionLoaderThread thread = new RegionLoaderThread(3);

    public RegionLoader() {
        this.regions = new RegionTable(512);
    }

    public void init() {
        thread.init();
    }
    public void stop() {
        thread.stopThread();
    }

    public boolean isChunkLoaded(int i, int j) {
        int toRegionX = i >> Region.REGION_SIZE_BITS;
        int toRegionZ = j >> Region.REGION_SIZE_BITS;
        return regions.containsKey(toRegionX, toRegionZ);
    }

    public Chunk get(int chunkX, int chunkZ) {
        int toRegionX = chunkX >> Region.REGION_SIZE_BITS;
        int toRegionZ = chunkZ >> Region.REGION_SIZE_BITS;
        Region region = regions.get(toRegionX, toRegionZ);
        if (region == null)
            return null;
        return region.chunks[chunkX & Region.REGION_SIZE_MASK][chunkZ & Region.REGION_SIZE_MASK];
    }

    public void flush() {
        thread.flush();
        Iterator<Region> it = this.regions.iterator();
        while (it.hasNext()) {
            Region r = it.next();
            r.release();
        }
        this.regions.clear();
    }

    public void reRender() {
        Iterator<Region> it = this.regions.iterator();
        while (it.hasNext()) {
            Region r = it.next();
            if (r.renderState == Region.RENDER_STATE_COMPILED) {
                r.renderState = Region.RENDER_STATE_MESHED;
                r.isCompiled = false;
            }
        }
    }

    public Region getRegion(int regionX, int regionZ) {
        return regions.get(regionX, regionZ);
    }

    public int updateRegions(int x, int z, boolean follow) {
        int loaded = 0;
//        if (follow && this.regions.size() >= MAX_REGIONS - 20   ) {
//            Iterator<Long> it = this.regionLoadReqMap.iterator();
//            for (; it.hasNext();) {
//                Long l = it.next();
//                int rx = GameMath.lhToX(l);
//                int rz = GameMath.lhToZ(l);
//                int dX = Math.abs(x - rx);
//                int dZ = Math.abs(z - rz);
//                if (dX > LOAD_DIST || dZ > LOAD_DIST) {
//                    synchronized (this.regions) {
//                        it.remove();
//                        this.loadQueue.remove(l);
//                        Region r = this.regions.remove(l);
//                        if (r != null) {
//                            r.release();
//                            Engine.worldRenderer.regionRemoved(r);
//                        }
//                    }
//                }
//            }
//        }
        if (!this.thread.busy()) {
            for (int xx = -LOAD_DIST; xx <= LOAD_DIST; xx++) {
                for (int zz = -LOAD_DIST; zz <= LOAD_DIST; zz++) {
                    Region r = regions.get(xx + x, zz + z);
                    if (r == null) {
                        r = new Region(xx + x, zz + z);
                        regions.put(xx + x, zz + z, r);
                    }
                    if (r.state == Region.STATE_INIT) {
//                        System.out.println("thread.offer");
                        if (this.thread.offer(r)) {
                          System.out.println("thread offer == true");
                            loaded++;
                        } else {

//                            System.out.println("thread seems busys");
                        }
                        if (this.thread.busy())
                            break;
                    }
                }
            }
        }
        return loaded;
    }

    public boolean hasAllNeighBours(Region r) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Region rNeighbour = this.regions.get(r.rX + x, r.rZ + z);
                if (rNeighbour == null || !rNeighbour.isRendered())
                    return false;
            }
        }
        return true;
    }

    public int getRegionsWithData() {
        int data = 0;
        Iterator<Region> it = this.regions.iterator();
        while (it.hasNext()) {
            Region r = it.next();
            if (r.state == Region.STATE_LOAD_COMPLETE && r.hasBlockData()) {
                data++;
            }
        }
        return data;
    }

    public int getRegionsLoaded() {
        return this.regions.size();
    }

    public void finishTasks() {
        this.thread.finishTasks();
    }

}
