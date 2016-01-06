/**
 * 
 */
package nidefawl.qubes.hex;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import nidefawl.qubes.util.GameMath;

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

    public Collection<Long> getChunks() {
        final double centerX = x * grid.width + z * grid.hwidth;
        final double centerY = z * grid.height;
        Point2F[] points = new Point2F[6];
        for (int i = 0; i < 6; i++) {
            double x = centerX + grid.radius * grid.sinCos[i * 2 + 0];
            double y = centerY + grid.radius * grid.sinCos[i * 2 + 1];
            points[i] = new Point2F(x, y);
            //            System.out.printf("%d %.2f, %.2f\n", i, x, y);
        }
        HashSet<Long> l = Sets.newHashSet();
        //    {
        //
        //        
        //        int minX = GameMath.floor(points[2].x/16);
        //        int maxX = GameMath.floor(points[0].x/16);
        //        int minZ = GameMath.floor(points[4].y/16);
        //        int maxZ = GameMath.floor(points[1].y/16);
        //        for (int x = minX; x<=maxX; x++)
        //            for (int z = minZ; z<=maxZ; z++)
        //                l.add(GameMath.toLong(x, z));
        //    }
        {

        }
        int x1 = GameMath.floor(points[2].x / 16);
        int x2 = GameMath.floor(points[5].x / 16);
        int z1 = GameMath.floor(points[5].y / 16);
        int z2 = GameMath.floor(points[2].y / 16);
        int xCenter = GameMath.floor(points[4].x / 16);
        for (int x = x1; x <= x2; x++)
            for (int z = z1; z <= z2; z++)
                l.add(GameMath.toLong(x, z));

        {//topleft

            int minX = GameMath.floor(points[3].x / 16);
            int maxX = xCenter;
            int minZ = GameMath.floor(points[4].y / 16);
            int maxZ = z1-1;
            Point2F A = points[4];
            Point2F B = points[3];
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    int Cx = ((x + 1) * 16);
                    int Cy = ((z + 1) * 16);
                    float s = (B.x - A.x) * (Cy - A.y) - (B.y - A.y) * (Cx - A.x);
                    if (s <= 0) {
                        l.add(GameMath.toLong(x, z));
                    }
                }
            }
        }
        {//topright

            int minX = xCenter+1;
            int maxX = GameMath.floor(points[5].x / 16);
            int minZ = GameMath.floor(points[4].y / 16);
            int maxZ = z1-1;
            Point2F A = points[5];
            Point2F B = points[4];
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    int Cx = ((x) * 16);
                    int Cy = ((z + 1) * 16);
                    float s = (B.x - A.x) * (Cy - A.y) - (B.y - A.y) * (Cx - A.x);
                    if (s <= 0) {
                        l.add(GameMath.toLong(x, z));
                    }
                }
            }
        }
        {//bottomright

            int minX = xCenter+1;
            int maxX = GameMath.floor(points[0].x / 16);
            int minZ = z2+1;
            int maxZ = GameMath.floor(points[1].y / 16);
            Point2F A = points[1];
            Point2F B = points[0];
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    int Cx = ((x) * 16);
                    int Cy = ((z) * 16);
                    float s = (B.x - A.x) * (Cy - A.y) - (B.y - A.y) * (Cx - A.x);
                    if (s <= 0) {
                        l.add(GameMath.toLong(x, z));
                    }
                }
            }
        }
        {//bottomleft
            int minX = GameMath.floor(points[2].x / 16);
            int maxX = xCenter;
            int minZ = z2+1;
            int maxZ = GameMath.floor(points[1].y / 16);
            Point2F A = points[2];
            Point2F B = points[1];
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    int Cx = ((x + 1) * 16);
                    int Cy = ((z) * 16);
                    float s = (B.x - A.x) * (Cy - A.y) - (B.y - A.y) * (Cx - A.x);
                    if (s <= 0) {
                        l.add(GameMath.toLong(x, z));
                    }
                }
            }
        }
        return l;
    }
}
