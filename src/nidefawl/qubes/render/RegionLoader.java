package nidefawl.qubes.render;

import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

import nidefawl.game.Main;
import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.chunk.Region;
import nidefawl.qubes.chunk.RegionTable;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.world.World;

public class RegionLoader implements Runnable {
    static int                LOAD_DIST        = 1;                                        
    //1 << (8-Region.REGION_SIZE_BITS*Region.REGION_SIZE_BITS);
    public static final int   MAX_REGIONS      = (LOAD_DIST * 2 + 1) * (LOAD_DIST * 2 + 1);
    final RegionTable         regions;
    LinkedBlockingQueue<Long> loadQueue        = new LinkedBlockingQueue();
    private World             world;
    public final static int   NUM_LOAD_THREADS = 1;
    public Thread[]           loadThreads      = new Thread[NUM_LOAD_THREADS];

    public RegionLoader(World world) {
        this.world = world;
        this.regions = new RegionTable(512);
        for (int i = 0; i < loadThreads.length; i++) {
            this.loadThreads[i] = new Thread(this);
            this.loadThreads[i].start();
        }
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
        this.regionLoadReqMap.clear();
        this.regions.clear();
        this.loadQueue.clear();
    }

    public static long timeGen;
    public static long timeMesh;
    volatile int       threadid = 0;

    @Override
    public void run() {
        int threadID = threadid++;
        int na = 0;
        System.out.println("Main.instance.isRunning() "+Main.instance.isRunning() );
        while (Main.instance.isRunning()) {
            try {

                //                while (Main.pauseLoad) {
                //                    try {Thread.sleep(10);}catch(Exception e){
                //                        Thread.interrupted();
                //                    };
                //                }
                Long hash = null;
                try {
                    hash = loadQueue.take();
                } catch (InterruptedException e) {
                    Thread.interrupted();
                    continue;
                }
                if (hash == null)
                    continue;

                int regionX = GameMath.lhToX(hash);
                int regionZ = GameMath.lhToZ(hash);
                //                System.out.println("work "+regionX+"/"+regionZ);
                if (!this.regionLoadReqMap.contains(hash)) {
                    //                    System.out.println("ski1 "+regionX+"/"+regionZ);
                    continue;
                }
                Region region = null;
                synchronized (this.regions) {
                    region = this.regions.get(hash);

                }
                if (region == null)
                    region = new Region(regionX, regionZ);
                //                else
                //                    System.out.println("region refresh "+regionX+"/"+regionZ);
                long measStart = System.currentTimeMillis();
                region.generate(this.world);
                timeGen += System.currentTimeMillis() - measStart;
                if (!this.regionLoadReqMap.contains(hash)) {
                    //                    System.out.println("ski2 "+regionX+"/"+regionZ);
                    continue;
                }
                measStart = System.currentTimeMillis();
                region.doMeshing(this.world);
                timeMesh += System.currentTimeMillis() - measStart;
                if (!this.regionLoadReqMap.contains(hash)) {
                    //                    System.out.println("ski3 "+regionX+"/"+regionZ);
                    continue;
                }
                synchronized (this.regions) {
                    if (!this.regionLoadReqMap.contains(hash)) {
                        //                        System.out.println("ski4 "+regionX+"/"+regionZ);
                        continue;
                    }
                    this.regions.put(regionX, regionZ, region);
                    Engine.worldRenderer.regionGenerated(regionX, regionZ, region);
                }
                //                if (threadID == 0 && na++%10==0) {
                //                    TimingHelper.dump();
                //                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void interruptLoading() {
        for (int i = 0; i < loadThreads.length; i++) {
            this.loadThreads[i].interrupt();
        }
    }

    public Region getRegion(int regionX, int regionZ) {
        return regions.get(regionX, regionZ);
    }

    final int[][] direction        = new int[][] { { 1, 0 }, { 0, 1 }, { -1, 0 }, { 0, -1 } };
    HashSet<Long> regionLoadReqMap = new HashSet<Long>();

    public void updateRegions(int x, int z) {

        //        for (int xx = -RegionLoader.LOAD_DIST; xx <= RegionLoader.LOAD_DIST; xx++) {
        //            for (int zz = -RegionLoader.LOAD_DIST; zz <= RegionLoader.LOAD_DIST; zz++) {
        //                Region r = getRegion(xx+x, zz+z);
        //                if (r != null && r.isRendered()) {
        //                    if (System.currentTimeMillis() - r.createTime > 3000) {
        //                        if (this.loadQueue.addSafe(x, z)) {
        //                        }
        //                    }
        //                }
        //            }
        //        }
        //        if (this.regions.size() >= RegionLoader.MAX_REGIONS) {
        //            Iterator<Long> it = this.regionLoadReqMap.iterator();
        //            for (;it.hasNext();) {
        //                Long l = it.next();
        //                int rx = LongHash.msw(l);
        //                int rz = LongHash.lsw(l);
        //                int dX = Math.abs(x-rx);
        //                int dZ = Math.abs(z-rz);
        //                if (dX > LOAD_DIST || dZ > LOAD_DIST) {
        //                    synchronized (this.regions) {
        //                        it.remove();
        //                        this.loadQueue.remove(l);
        //                        Region r = this.regions.remove(l);
        //                        if (r != null) {
        //                            if (r.isRendered())
        //                                r.release();
        //                            callback.onRemove(r);
        //                        }
        //                    }
        //                }
        //            }
        //        }
    }

    public void loadRegions(int x, int z, boolean follow) {
        if (follow && this.regions.size() >= RegionLoader.MAX_REGIONS - 20) {
            Iterator<Long> it = this.regionLoadReqMap.iterator();
            for (; it.hasNext();) {
                Long l = it.next();
                int rx = GameMath.lhToX(l);
                int rz = GameMath.lhToZ(l);
                int dX = Math.abs(x - rx);
                int dZ = Math.abs(z - rz);
                if (dX > LOAD_DIST || dZ > LOAD_DIST) {
                    synchronized (this.regions) {
                        it.remove();
                        this.loadQueue.remove(l);
                        Region r = this.regions.remove(l);
                        if (r != null) {
                            r.release();
                            Engine.worldRenderer.regionRemoved(r);
                        }
                    }
                }
            }
        }

        for (int xx = -RegionLoader.LOAD_DIST; xx <= RegionLoader.LOAD_DIST; xx++) {
            for (int zz = -RegionLoader.LOAD_DIST; zz <= RegionLoader.LOAD_DIST; zz++) {
                loadRegion(xx + x, zz + z);
            }
        }
    }

    void loadRegion(int x, int z) {
        Long pos = GameMath.toLong(x, z);
        if (this.regionLoadReqMap.contains(pos)||this.loadQueue.contains(pos)) {
            return;
        }

        if (this.regions.size() >= RegionLoader.MAX_REGIONS) {
            return;
        }
        this.regionLoadReqMap.add(pos);
        if (this.loadQueue.add(pos)) {
//            System.out.println("load "+x+"/"+z);
        }
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
            if (r.hasBlockData()) {
                data++;
            }
        }
        return data;
    }

    public int getRegionsLoaded() {
        return this.regions.size();
    }
}
