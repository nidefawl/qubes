/**
 * 
 */
package nidefawl.qubes.hex;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class HexCell<T> {
    public int x;
    public int z;
    protected HexagonGridStorage<T> grid;
    public HexCell(HexagonGridStorage<T> grid, int x, int z) {
        this.grid = grid;
        this.x = x;
        this.z = z;
    }
    public double getDistanceCenter(double x, double z) {
        double centerX = grid.getCenterX(this.x, this.z);
        double centerZ = grid.getCenterY(this.x, this.z);
        x-=centerX;
        z-=centerZ;
        return Math.sqrt(x*x+z*z);
    }

    public double getCenterX() {
        return grid.getCenterX(this.x, this.z);
    }

    public double getCenterY() {
        return grid.getCenterY(this.x, this.z);
    }
    public T getRight() {
        return grid.getPos(HexagonGrid.right(this.x, this.z));
    }
    public T getLeft() {
        return grid.getPos(HexagonGrid.left(this.x, this.z));
    }
    public T getTopLeft() {
        return grid.getPos(HexagonGrid.topLeft(this.x, this.z));
    }
    public T getTopRight() {
        return grid.getPos(HexagonGrid.topRight(this.x, this.z));
    }
    public T getBottomLeft() {
        return grid.getPos(HexagonGrid.bottomLeft(this.x, this.z));
    }
    public T getBottomRight() {
        return grid.getPos(HexagonGrid.bottomRight(this.x, this.z));
    }
    public int getClosesCorner(double x, double z) {
        //can be done more efficient!
        final double centerX = this.x * this.grid.width + this.z * this.grid.hwidth;
        final double centerY = this.z * this.grid.height;
        x-=centerX;
        z-=centerY;
//        Point2F min = new Point2F(0, 0);
        int min = 0;
        double minDist = 0;
        for (int i = 0; i < 6; i++) {
            double x2 = this.grid.radius * this.grid.sinCos[i * 2 + 0];
            double z2 = this.grid.radius * this.grid.sinCos[i * 2 + 1];
            double distX = x2-x;
            double distZ = z2-z;
            double dist = (distX*distX)+(distZ*distZ);
            if (i == 0 || minDist > dist) {
                minDist = dist;
//                min.x = (float) x2;
//                min.y = (float) z2;
                min = i;
            }
        }
        return min;
//        return min;
    }
    /**
     * @return the grid
     */
    public HexagonGridStorage getGrid() {
        return this.grid;
    }
}
