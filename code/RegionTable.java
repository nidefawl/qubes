package nidefawl.qubes.chunk;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import nidefawl.qubes.util.GameMath;


public class RegionTable implements Iterable<Region> {


    Region[][] values;
    int size;
    int halfSize;
    private int approxLoaded;

    /* this fields will store the lowest and highest values used */
    int mswLow;
    int mswHigh;
    int lswLow;
    int lswHigh;


    public RegionTable(int size) {
        this.values = new Region[size][];
        this.size = size;
        this.halfSize = this.size/2;
        mswLow = this.size - 1;
        mswHigh = 0;
        lswLow = this.size - 1;
        lswHigh = 0;
    }

    public boolean put(int msw, int lsw, Region chunk) {
        msw += halfSize;
        lsw += halfSize;
        if (msw >= 0 && msw < size && lsw > 0 && lsw < size) {
            if (values[msw] == null) {
                values[msw] = new Region[size];
            }
            if (values[msw][lsw] != null) { // test synchronization
                System.err.println("attempt to overwrite a chunk :O");
                Thread.dumpStack();
            }
            values[msw][lsw] = chunk;
            updateBounds(msw, lsw);
            return true;
        }
        return false;
    }


    private void updateBounds(int msw, int lsw) {
        if (mswLow > msw) mswLow = msw;
        if (lswLow > lsw) lswLow = lsw;
        if (mswHigh < msw+1) mswHigh = msw+1;
        if (lswHigh < lsw+1) lswHigh = lsw+1;
    }

    public Region get(long chunkcoordinates) {
        return get(GameMath.lhToX(chunkcoordinates), GameMath.lhToZ(chunkcoordinates));
    }

    public Region get(int msw, int lsw) {
        msw += halfSize;
        lsw += halfSize;
        if (msw >= 0 && msw < size && lsw >= 0 && lsw < size) {
            return values[msw] != null ? values[msw][lsw] : null;
        }
        return null;
    }


    public Region remove(int msw, int lsw) {
        msw += halfSize;
        lsw += halfSize;
        Region v = null;
        if (msw >= 0 && msw < size && lsw >= 0 && lsw < size && values[msw] != null) {
        	v = values[msw][lsw];
            values[msw][lsw] = null;
        }
		return (Region) v;
    }

    public boolean containsKey(int msw, int lsw) {
        msw += halfSize;
        lsw += halfSize;
        if (msw >= 0 && msw < size && lsw >= 0 && lsw < size && values[msw] != null) {
            return values[msw][lsw] != null;
        }
        return false;
    }
    public int approxSize() {
        return this.approxLoaded;
    }
    public int size() {
        int loaded = 0;
        for (int msw = mswLow; msw < mswHigh; msw++) {
            Object[] row = values[msw];
            if (row != null) {
                for (int lsw = lswLow; lsw < lswHigh; lsw++) {
                    Object o = row[lsw];
                    if (o != null) {
                    	loaded++;
                    }
                }
            }
        }
        this.approxLoaded = loaded;
        return loaded;
    }


    public Region[] getEntries() {
        return (Region[]) asList().toArray();
    }

	public int getFixedSize() {
		int loaded = 0;
        for (int msw = mswLow; msw < mswHigh; msw++) {
            Object[] row = values[msw];
            if (row != null) {
                for (int lsw = lswLow; lsw < lswHigh; lsw++) {
                	loaded++;
                }
            }
        }
	    return loaded;
    }

	public boolean allRegionsLoaded(int x, int z, int distance) {
        x += halfSize;
        z += halfSize;
        if(x < 0 || x >= size || z < 0 || z >= size) return false;
        for (int msw = x-distance; msw <= x+distance; msw++) {
            Region[] row = values[msw];
            if (row == null)  {
                return false;
            }
            for (int lsw = z-distance; lsw <= z+distance; lsw++) {
                Region c = row[lsw];
            	if (c == null) return false;
            }
        }
        return true;
	}

	public void clear() {
        this.values = new Region[size][];
    }

    public Region remove(long chunkcoordinates) {
        return remove(GameMath.lhToX(chunkcoordinates), GameMath.lhToZ(chunkcoordinates));
    }


    public List<Region> asList() {
        LinkedList<Region> list = new LinkedList<Region>();
        for (int msw = mswLow; msw < mswHigh; msw++) {
            Region[] row = values[msw];
            if (row != null) {
                for (int lsw = lswLow; lsw < lswHigh; lsw++) {
                    Region o = row[lsw];
                    if (o != null) {
                        list.add(o);
                    }
                }
            }
        }
        return list;
    }


    public class ChunkRegionTableIterator implements Iterator<Region> {
        Region value;
        int msw;
        int lsw;
        private RegionTable table;
        public ChunkRegionTableIterator(RegionTable table) {
            msw = mswLow;
            lsw = lswLow;
            value = null;
            this.table = table;
        }
        @Override
        public boolean hasNext() {
            if (value != null) return true;
            for (; msw < mswHigh; msw++) {
                Region[] row = this.table.values[msw];
                if (row != null) {
                    for (; lsw < lswHigh; lsw++) {
                        value = row[lsw];
                        if (value != null) {
                            lsw++;
                            return true;
                        }
                    }
                }
                lsw = lswLow;
            }
            return false;
        }

        @Override
        public Region next() {
            Region c = value;
            value = null;
            return c;
        }

        @Override
        public void remove() {
        }

    }


    public void replace(int msw, int lsw, Region region) {
        msw += halfSize;
        lsw += halfSize;
        if (msw >= 0 && msw < size && lsw > 0 && lsw < size) {
            if (values[msw] == null) {
                values[msw] = new Region[size];
            }
            values[msw][lsw] = region;
        }
    }

    @Override
    public Iterator<Region> iterator() {
        return new ChunkRegionTableIterator(this);
    }
}
