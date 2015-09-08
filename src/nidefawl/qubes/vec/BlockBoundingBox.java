package nidefawl.qubes.vec;

import nidefawl.qubes.util.TripletShortHash;

public class BlockBoundingBox {
    public final static int MIN  = 0;
    public final static int MAX  = 16;
    public final static int MINY = 0;
    public final static int MAXY = 256;
    public int              lowX;
    public int              highX;
    public int              lowY;
    public int              highY;
    public int              lowZ;
    public int              highZ;

    public BlockBoundingBox(int x1, int y1, int z1, int x2, int y2, int z2) {
        set(x1, y1, z1, x2, y2, z2);
    }

    /**
     * 
     */
    public BlockBoundingBox() {
        reset();
    }
    
    
    public void expandTo(int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ) {
        flag(minBlockX, minBlockY, minBlockZ);
        flag(maxBlockX, maxBlockY, maxBlockZ);
    }

    public void flag(int x, int y, int z) {
        checkBounds(x, y, z);
        if (this.lowY > y) {
            this.lowY = y;
        }
        if (this.highY < y) {
            this.highY = y;
        }
        if (this.lowX > x) {
            this.lowX = x;
        }
        if (this.highX < x) {
            this.highX = x;
        }
        if (this.lowZ > z) {
            this.lowZ = z;
        }
        if (this.highZ < z) {
            this.highZ = z;
        }
    }

    /**
     * @param x
     * @param y
     * @param z
     */
    public static void checkBounds(int x, int y, int z) {
        if (y < MINY || y >= MAXY || x < MINY || x >= MAX || z < MINY || z >= MAX)
            throw new IndexOutOfBoundsException("Index " + x + "," + y + "," + z + " out of bounds");
    }

    public void set(int x1, int y1, int z1, int x2, int y2, int z2) {
        this.lowX = x1;
        this.lowY = y1;
        this.lowZ = z1;
        this.highX = x2;
        this.highY = y2;
        this.highZ = z2;
    }

    public BlockBoundingBox copyTo(BlockBoundingBox box2) {
        box2.set(this.lowX, this.lowY, this.lowZ, this.highX, this.highY, this.highZ);
        return box2;
    }

    public int getLength() {
        return Math.max(0, this.highZ + 1 - this.lowZ);
    }

    public int getWidth() {
        return Math.max(0, this.highX + 1 - this.lowX);
    }

    public int getHeight() {
        return Math.max(0, this.highY + 1 - this.lowY);
    }

    public int getVolume() {
        return getWidth() * getHeight() * getLength();
    }

    public void expand() {
        if (this.lowX > 0)
            this.lowX--;
        if (this.lowY > 0)
            this.lowY--;
        if (this.lowZ > 0)
            this.lowZ--;
        if (this.highX < 15)
            this.highX++;
        if (this.highY < 255)
            this.highY++;
        if (this.highZ < 15)
            this.highZ++;
    }

    public void reset() {
        this.set(MAX, MAXY, MAX, MIN, MINY, MIN);
    }

    public boolean contains(int x, int y, int z) {
        return this.lowX <= x && this.highX >= x && this.lowY <= y && this.highY >= y && this.lowZ <= z && this.highZ >= z;
    }

    /**
     * @param blockBoundingBox
     */
    public void extend(BlockBoundingBox bb) {
        this.flag(bb.lowX, bb.lowY, bb.lowZ);
        this.flag(bb.highX, bb.highY, bb.highZ);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "BlockBB["+lowX+","+lowY+","+lowZ+","+highX+","+highY+","+highZ+"]";
    }

    /**
     * @return TripletShortHash of minimum point
     */
    public short getMinHash() {
        return TripletShortHash.toHash(lowX, lowY, lowZ);
    }
    /**
     * @return TripletShortHash of maximum point
     */
    public short getMaxHash() {
        return TripletShortHash.toHash(highX, highY, highZ);
    }

    /** Create a blockboundinbox from 2 TripleShortHash shorts
     * @param min2
     * @param max2
     * @return A new BlockBoundingBox with min/max set from the supplied short hashes
     */
    public static BlockBoundingBox fromShorts(short min, short max) {
        return new BlockBoundingBox(TripletShortHash.getX(min), TripletShortHash.getY(min), TripletShortHash.getZ(min), TripletShortHash.getX(max), TripletShortHash.getY(max), TripletShortHash.getZ(max));
    }

}
