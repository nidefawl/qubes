package nidefawl.qubes.util;

import java.util.Collection;
import java.util.HashSet;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

public class RegionMap<T extends RegionEntry> {
    private final Multimap<Integer, T> mmap = LinkedListMultimap.create();
    private byte                 bits = 4;
    private int                  halfSize;

    public RegionMap(int bits) {
        this.bits = (byte) bits;
        this.halfSize = 1 << (this.bits * 2);
        if (this.halfSize >= 0xFFFF >> 1) {
            throw new IllegalArgumentException("Max value for bits is 7");
        }
    }

    private int toRegion(int val) {
        return (val >> this.bits) + this.halfSize;
    }

    private int toBlock(int val) {
        return (val - this.halfSize) << this.bits;
    }

    public void add(T entry) {
        int x = toRegion(entry.getMinX());
        int maxX = toRegion(entry.getMaxX());
        int minZ = toRegion(entry.getMinZ());
        int maxZ = toRegion(entry.getMaxZ());
        for (; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int key = rkey(x, z);
                mmap.put(key, entry);
                entry.addKey(key);
            }
        }
    }

    public void remove(T entry) {
        for (Integer key : entry.getKeys()) {
            mmap.get(key).remove(entry);
        }
        entry.getKeys().clear();
    }


    public void debug() {
        int print = 0;
        for (Integer key : mmap.asMap().keySet()) {
            print++;
            System.out.println(String.format("entry %12d => %4d %4d", key, x(key), z(key)));
        }
        if (print==0) {
            System.out.println("empty!");
        }
    }

    public boolean contains(T d) {
        return mmap.values().contains(d);
    }

    private int rkey(int x, int z) {
        return (x << 16) | z;
    }

    private int key(int x, int z) {
        return (toRegion(x) << 16) | toRegion(z);
    }

    public Collection<T> getRegion(int x, int z) {
        return mmap.get(key(x, z));
    }

    private int x(int key) {
        return toBlock((key >> 16) & 0xFFFF);
    }

    private int z(int key) {
        return toBlock(key & 0xFFFF);
    }

    public Collection<T> getRegions(int blockX, int blockZ, int i) {
        HashSet<T> result = new HashSet<T>();
        int x = toRegion(blockX - i);
        int maxX = toRegion(blockX + i);
        int minZ = toRegion(blockZ - i);
        int maxZ = toRegion(blockZ + i);
        for (; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int key = rkey(x, z);
                result.addAll(mmap.get(key));
            }
        }
        return result;
    }
    public Collection<T> getRegions(int minX, int minZ, int maxX, int maxZ) {
        HashSet<T> result = null;
        int x1 = minX >> this.bits;
        int x2 = maxX >> this.bits;
        int z1 = minZ >> this.bits;
        int z2 = maxZ >> this.bits;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                int key = ((x+halfSize) << 16) | (z+halfSize);
                Collection<T> present = mmap.get(key);
                if (!present.isEmpty()) {
                    if (result == null) {
                        result = new HashSet<T>();
                    }
                    result.addAll(present);
                }
            }
        }
        return result;
    }

    public boolean containsAny(int blockX, int blockZ, int i) {
        int x = toRegion(blockX - i);
        int maxX = toRegion(blockX + i);
        int minZ = toRegion(blockZ - i);
        int maxZ = toRegion(blockZ + i);
        for (; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int key = rkey(x, z);
                if (!mmap.get(key).isEmpty())
                    return true;
            }
        }
        return false;
    }

    public T getNearest(int blockX, int blockZ) {
        int beginX = toRegion(blockX);
        int beginZ = toRegion(blockZ);
        T nearest = null;
        int nearestDistSq = 0;
        SnakeIterator snakeIterator = new SnakeIterator();
        while (snakeIterator.hasMore(500)) {
            int regionX = beginX+snakeIterator.getX();
            int regionZ = beginZ+snakeIterator.getZ();
            int key = rkey(regionX, regionZ);
            Collection<T> present = mmap.get(key);
            if (!present.isEmpty()) {
                for (T regionEntry : present) {
                    int distance = getDistanceSq(regionEntry, blockX, blockZ);
                    if (nearest == null) {
                        nearest = regionEntry;
                        nearestDistSq = distance;
                        continue;
                    }
                    if (distance < nearestDistSq) {
                        nearest = regionEntry;
                        nearestDistSq = distance;
                    }
                }
            }
            snakeIterator.next();
        }
        return nearest;
    }

    public int getDistanceSq(T regionEntry, int blockX, int blockZ) {
        int middleX = regionEntry.getMinX() + (regionEntry.getMaxX()-regionEntry.getMinX());
        int middleZ = regionEntry.getMinZ() + (regionEntry.getMaxZ()-regionEntry.getMinZ());
        int distX = blockX - middleX;
        int distZ = blockZ - middleZ;
        return distX*distX+distZ*distZ;
    }

    public void clear() {
        mmap.clear();
    }

    public Collection<T> values() {
        return this.mmap.values();
    }

}