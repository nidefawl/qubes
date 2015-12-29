package nidefawl.qubes.vec;

import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.nbt.Tag.Compound;

public class AABBInt {
    public int minX;
    public int minY;
    public int minZ;
    public int maxX;
    public int maxY;
    public int maxZ;
    public AABBInt(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public AABBInt() {
    }

    public int getWidth() {
        return this.maxX-this.minX;
    }
    public int getHeight() {
        return this.maxY-this.minY;
    }
    public int getLength() {
        return this.maxZ-this.minZ;
    }
    
    public void offset(int x, int y, int z) {
        this.minX += x;
        this.maxX += x;
        this.minY += y;
        this.maxY += y;
        this.minZ += z;
        this.maxZ += z;
    }
    
    public void expandTo(int x, int y, int z) {
        if (x < 0)
        this.minX += x;
        if (x > 0)
        this.maxX += x;
        if (y < 0)
        this.minY += y;
        if (y > 0)
        this.maxY += y;
        if (z < 0)
        this.minZ += z;
        if (z > 0)
        this.maxZ += z;
    }

    
    public void expand(int x, int y, int z) {
        this.minX -= x;
        this.maxX += x;
        this.minY -= y;
        this.maxY += y;
        this.minZ -= z;
        this.maxZ += z;
    }
    
    public void set(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }
    public AABBInt copy() {
        return new AABBInt(minX, minY, minZ, maxX, maxY, maxZ);
    }
    public void set(AABBInt b) {
        this.set(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ);
    }
    
    public int getCenterX() {
        return this.minX+(getWidth()>>1);
    }
    
    public int getCenterY() {
        return this.minY+(getHeight()>>1);
    }
    
    public int getCenterZ() {
        return this.minZ+(getLength()>>1);
    }

    public void centerXZ(int x, int y, int z) {
        int w = getWidth()>>1;
        int l = getLength()>>1;
        int h = getHeight();
        set(x-w, y, z-l, x+w, y+h, z+l);
    }

    public boolean intersects(AABBInt b) {
        if (b.maxX < this.minX) return false;
        if (b.maxY < this.minY) return false;
        if (b.maxZ < this.minZ) return false;
        if (this.maxX < b.minX) return false;
        if (this.maxY < b.minY) return false;
        if (this.maxZ < b.minZ) return false;
        return true;
    }
    
    @Override
    public String toString() {
        return "AABBInt["+String.format("%d %d %d - %d %d %d", minX, minY, minZ, maxX, maxY, maxZ)+"]";
    }

    public Tag.Compound saveTag() {
        Tag.Compound tag = new Tag.Compound();
        tag.setInt("minX", this.minX);
        tag.setInt("minY", this.minY);
        tag.setInt("minZ", this.minZ);
        tag.setInt("maxX", this.maxX);
        tag.setInt("maxY", this.maxY);
        tag.setInt("maxZ", this.maxZ);
        return tag;
    }

    public void loadTag(Compound tag) {
        this.minX = tag.getInt("minX");
        this.minY = tag.getInt("minY");
        this.minZ = tag.getInt("minZ");
        this.maxX = tag.getInt("maxX");
        this.maxY = tag.getInt("maxY");
        this.maxZ = tag.getInt("maxZ");
    }

    public boolean contains(int x, int y, int z) {
        return x >= this.minX && x <= this.maxX && y >= this.minY && y <= this.maxY && z >= this.minZ && z <= this.maxZ;
    }

    public boolean contains(BlockPos bPos) {
        return contains(bPos.x, bPos.y, bPos.z);
    }

}

