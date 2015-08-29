package nidefawl.qubes.vec;

public class AABB {
    public double minX;
    public double minY;
    public double minZ;
    public double maxX;
    public double maxY;
    public double maxZ;
    public AABB(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public AABB() {
    }

    public double getWidth() {
        return this.maxX-this.minX;
    }
    public double getHeight() {
        return this.maxY-this.minY;
    }
    public double getLength() {
        return this.maxZ-this.minZ;
    }
    
    public void offset(double x, double y, double z) {
        this.minX += x;
        this.maxX += x;
        this.minY += y;
        this.maxY += y;
        this.minZ += z;
        this.maxZ += z;
    }
    
    public void expandTo(double x, double y, double z) {
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

    
    public void expand(double x, double y, double z) {
        this.minX -= x;
        this.maxX += x;
        this.minY -= y;
        this.maxY += y;
        this.minZ -= z;
        this.maxZ += z;
    }
    
    public void set(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }
    public AABB copy() {
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
    public void set(AABB b) {
        this.set(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ);
    }
    
    public double getCenterX() {
        return this.minX+getWidth()/2.0D;
    }
    
    public double getCenterY() {
        return this.minY+getHeight()/2.0D;
    }
    
    public double getCenterZ() {
        return this.minZ+getLength()/2.0D;
    }

    public void centerXZ(double x, double y, double z) {
        double w = getWidth()/2.0D;
        double l = getLength()/2.0D;
        double h = getHeight();
        set(x-w, y, z-l, x+w, y+h, z+l);
    }

    public boolean intersects(AABB b) {
        if (b.maxX < this.minX) return false;
        if (b.maxY < this.minY) return false;
        if (b.maxZ < this.minZ) return false;
        if (this.maxX < b.minX) return false;
        if (this.maxY < b.minY) return false;
        if (this.maxZ < b.minZ) return false;
        return true;
    }

    public double getXOffset(AABB b, double mx) {
        if (b.minZ < this.maxZ && b.maxZ > this.minZ && b.minY < this.maxY && b.maxY > this.minY) {
            if (mx > 0) {
                if (this.minX >= b.maxX) {
                    double d = this.minX-b.maxX;
                    if (d < mx)
                        return d;
                }
            }
            if (mx < 0) {
                if (this.maxX <= b.minX) {
                    double d =this.maxX- b.minX;
                    if (d > mx)
                        return d;
                }
            }
        }
        return mx;
    }

    public double getZOffset(AABB b, double mx) {
        if (b.minX < this.maxX && b.maxX > this.minX && b.minY < this.maxY && b.maxY > this.minY) {
            if (mx > 0) {
                if (this.minZ >= b.maxZ) {
                    double d = this.minZ-b.maxZ;
                    if (d < mx)
                        return d;
                }
            }
            if (mx < 0) {
                if (this.maxZ <= b.minZ) {
                    double d =this.maxZ- b.minZ;
                    if (d > mx)
                        return d;
                }
            }
        }
        return mx;
    }

    public double getYOffset(AABB b, double mx) {
        if (b.minX < this.maxX && b.maxX > this.minX && b.minZ < this.maxZ && b.maxZ > this.minZ) {
            if (mx > 0) {
                if (this.minY >= b.maxY) {
                    double d = this.minY-b.maxY;
                    if (d < mx)
                        return d;
                }
            }
            if (mx < 0) {
                if (this.maxY <= b.minY) {
                    double d =this.maxY- b.minY;
                    if (d > mx)
                        return d;
                }
            }
        }
        return mx;
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return "AABB["+String.format("%.2f %.2f %.2f - %.2f %.2f %.2f", minX, minY, minZ, maxX, maxY, maxZ)+"]";
    }

}

