package nidefawl.qubes.chunk;

import java.util.Iterator;

import nidefawl.qubes.render.region.MeshedRegion;

public class RegionLoader {
    public static final int              LOAD_DIST        = 5;
    //1 << (8-Region.REGION_SIZE_BITS*Region.REGION_SIZE_BITS);
    public static final int MAX_REGION_XZ      = 64;
    public static final int MAX_REGIONS      = (MAX_REGION_XZ*2)*(MAX_REGION_XZ*2);
    final RegionTable       regions;
    final int[][]           direction        = new int[][] { { 1, 0 }, { 0, 1 }, { -1, 0 }, { 0, -1 } };
    final RegionLoaderThread thread = new RegionLoaderThread(3);

    public RegionLoader() {
        this.regions = new RegionTable(MAX_REGION_XZ*2);
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
        Iterator<Region> it = this.regions.iterator();
        while (it.hasNext()) {
            Region r = it.next();
            MeshedRegion m = r.meshedRegion;
            if (m != null)
                m.release();
            r.release();
        }
        this.regions.clear();
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

    public boolean cacheRegions(int rX, int rZ, int renderChunkX, int renderChunkZ, RegionCache cache) {
        int offsetX = rX-renderChunkX;
        int offsetZ = rZ-renderChunkZ;
        boolean minXReq = offsetX > 0;
        boolean maxXReq = offsetX < LOAD_DIST-1;
        boolean minZReq = offsetZ > 0;
        boolean maxZReq = offsetZ < LOAD_DIST-1;
//        minXReq=maxXReq=minZReq=maxZReq=true;
        
        Region r = this.regions.get(rX, rZ);
        cache.set(0, 0, r);
        r = this.regions.get(rX - 1, rZ - 1);
        if (r != null && r.isChunkLoaded(Region.REGION_SIZE-1, Region.REGION_SIZE-1)) {
            cache.set(-1, -1, r);
        } else if (minXReq && minZReq) return false;
        r = this.regions.get(rX + 1, rZ + 1);
        if (r != null && r.isChunkLoaded(Region.REGION_SIZE-1, Region.REGION_SIZE-1)) {
            cache.set(1, 1, r);
        } else if (maxXReq && maxZReq) return false;
        r = this.regions.get(rX + 1, rZ - 1);
        if (r != null && r.isChunkLoaded(0, Region.REGION_SIZE-1)) {
            cache.set(1, -1, r);
        } else if (maxXReq && minZReq) return false;
        r = this.regions.get(rX - 1, rZ + 1);
        if (r != null && r.isChunkLoaded(Region.REGION_SIZE-1, 0)) {
            cache.set(-1, 1, r);
        } else if (minXReq && maxZReq) return false;
        r = this.regions.get(rX - 1, rZ);
        if (r != null && r.allLoadedX(Region.REGION_SIZE-1)) {
            cache.set(-1, 0, r);
        } else if (minXReq) return false;
        r = this.regions.get(rX + 1, rZ);
        if (r != null && r.allLoadedX(0)) {
            cache.set(1, 0, r);
        } else if (maxXReq) return false;
        r = this.regions.get(rX, rZ - 1);
        if (r != null && r.allLoadedZ(Region.REGION_SIZE-1)) {
            cache.set(0, -1, r);
        } else if (minZReq) return false;
        r = this.regions.get(rX, rZ + 1);
        if (r != null && r.allLoadedZ(0)) {
            cache.set(0, 1, r);
        } else if (maxZReq) return false;
        return true;
    }

    public boolean hasAllNeighBours(Region r) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Region rNeighbour = this.regions.get(r.rX + x, r.rZ + z);
                if (rNeighbour == null)
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
