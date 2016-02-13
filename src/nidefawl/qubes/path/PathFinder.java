package nidefawl.qubes.path;

import java.util.Map;

import com.google.common.collect.Maps;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.TripletLongHash;
import nidefawl.qubes.vec.AABB;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.vec.Vec3D;

public class PathFinder {

    final PathList path = new PathList();
    final Map<Long, PathPoint> block = Maps.newHashMap();
    BlockPos size = new BlockPos();
    BlockPos blockPos = new BlockPos();
    private PathPoint[] pathOptions = new PathPoint[4];
    public Path findPath(Entity e, Vec3D pos, float maxDist) {
        this.path.reset();
        this.block.clear();
        AABB aabb = e.getAabb();
        int x = GameMath.floor(aabb.minX);
        int y = GameMath.floor(aabb.minY+0.5D);
        int z = GameMath.floor(aabb.minZ);
        PathPoint start = addPoint(x, y, z);
        PathPoint end = addPoint(GameMath.floor(pos.x-(e.width/2.0f)), GameMath.floor(pos.y), GameMath.floor(pos.z-(e.length/2.0f)));

        size.set(GameMath.ceil(e.width), GameMath.ceil(e.height), GameMath.ceil(e.length));
        Path p = findPath(e, start, end, maxDist);
        return p;
    }
    private Path findPath(Entity e, PathPoint start, PathPoint end, float maxDist) {
//        System.out.println("findPath");
        start.totalPathDistance = 0.0f;
        start.distanceToNext = start.distanceTo(end);
        start.distanceToTarget = start.distanceToNext;
        path.addPoint(start);
        PathPoint point = start;
//        System.out.println("findPath");
        while (!path.isEmpty()) {
            PathPoint point1 = path.pop();
            if (point1.equals(end)) {
                return createPath(start, end);
            }
            if (point1.distanceTo(end)<point.distanceTo(end)) {
                point = point1;
            }
            point1.isFirst = true;
            int i = findPathOptions(e, point1, end, maxDist);
            int j = 0;
            while (j < i) {
                PathPoint point2 = pathOptions[j];
                float f = point1.totalPathDistance+point1.distanceTo(point2);
                if (!point2.inUse() || f < point2.totalPathDistance) {
                    point2.previous = point1;
//                    System.out.println("point2.previous");
                    point2.totalPathDistance = f;
                    point2.distanceToNext = point2.distanceTo(end);
                    if (point2.inUse()) {
                        path.updateCost(point2, point2.totalPathDistance+point2.distanceToNext);
                    } else {
                        point2.distanceToTarget=point2.totalPathDistance+point2.distanceToNext;
                        path.addPoint(point2);
                    }
                }
                j++;
            }
        }
        if (point.equals(start)) {
            return null;
        }
//        System.out.println("2 start "+start+", end "+end+", goal "+point);
        return createPath(start, point);
    }
    private int findPathOptions(Entity e, PathPoint point1, PathPoint end, float maxDist) {
        int state = getBlockState(e, point1.x, point1.y, point1.z);
        int yoffset=1;
        if (state == 1) {
            yoffset=1;
        }
        PathPoint pointZP = getPoint(e, point1.x, point1.y, point1.z + 1, yoffset);
        PathPoint pointXN = getPoint(e, point1.x - 1, point1.y, point1.z, yoffset);
        PathPoint pointXP = getPoint(e, point1.x + 1, point1.y, point1.z, yoffset);
        PathPoint pointZN = getPoint(e, point1.x, point1.y, point1.z - 1, yoffset);
        int i = 0;

        if (pointZP != null && !pointZP.isFirst && pointZP.distanceTo(end) < maxDist) {
            pathOptions[i++] = pointZP;
        }
        if (pointXN != null && !pointXN.isFirst && pointXN.distanceTo(end) < maxDist) {
            pathOptions[i++] = pointXN;
            
        }
        if (pointXP != null && !pointXP.isFirst && pointXP.distanceTo(end) < maxDist) {
            pathOptions[i++] = pointXP;
            
        }
        if (pointZN != null && !pointZN.isFirst && pointZN.distanceTo(end) < maxDist) {
            pathOptions[i++] = pointZN;
            
        }
        return i;
    }
    private PathPoint getPoint(Entity e, int x1, int y1, int z1, int yoffset) {
        int state = getBlockState(e, x1, y1, z1);
        PathPoint point = null;
        if (state == 1) {
            point = addPoint(x1, y1, z1);
        }
        if (point == null && yoffset > 0 && state != -3 && state != -4 && getBlockState(e, x1, y1+yoffset, z1) == 1) {
            point = addPoint(x1, y1+yoffset, z1);
            y1+=yoffset;
        }
        if (point != null) {
            int j = 0;
            int k= 0;
            while (y1 >= 0) {
                k = getBlockState(e, x1, y1-1, z1);
//                if ((path))
                if (k != 1)
                    break;
                if (j++ > 2) {
                    return null;
                }
                y1--;
                if (y1 > 0) {
                    point = addPoint(x1, y1, z1);
                }
            }
            if (k == 2) {
                return null;
            }
        }
        return point;
    }
    private int getBlockState(Entity e, int x1, int y1, int z1) {
        for (int x = x1; x < x1+size.x; x++) {
            for (int y = y1; y < y1+size.y; y++) {
                for (int z = z1; z < z1+size.z; z++) {
                    int i = e.world.getType(x, y, z);
                    if (i <= 0) {
                        continue;
                    }
                    Block b = Block.get(i);
                    if (b == Block.water) {
                        return -1;
                    }
                    if (b.canWalkThru(e.world, x1, y1, z1, e)) {
                        continue;
                    }
                    return 0;
                }
            }
            
        }
        return 1;
    }
    private Path createPath(PathPoint start, PathPoint end) {
        int len = 1;
        for (PathPoint p =end; p.previous != null; p = p.previous) {
            len++;
        }
        PathPoint[] arr = new PathPoint[len];
        PathPoint p = end;
//        System.out.println("         ");
        for (arr[--len] = p; p.previous != null; arr[--len] = p) {
//            System.out.println(p+" - "+p.distanceToNext+"/"+p.distanceToTarget+"/"+p.totalPathDistance);
            p = p.previous;
        }
//        System.out.println("         ");
        return new Path(arr);
    }
    private PathPoint addPoint(int x, int y, int z) {
        long l = TripletLongHash.toHash(x, y, z);
        PathPoint pos = block.get(l);
        if (pos == null) {
            pos = new PathPoint(x, y, z);
            block.put(l, pos);
        } else {

//               Thread.dumpStack();
//            System.out.println("reuse "+pos);
        }
//        if (pos.inUse()) {
//            System.out.println("inuse pos "+x+","+y+","+z);
//        }
        return pos;
    }
}
