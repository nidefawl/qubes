// Copyright 2011-2012 Kevin Reid under the terms of the MIT License as detailed
// in the accompanying file README.md or <http://opensource.org/licenses/MIT>.
// modified by Michael Hept (2015)

package nidefawl.qubes.util;

import nidefawl.qubes.Game;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.vec.*;
import nidefawl.qubes.world.World;

public class RayTrace {
    public static enum HitType {
        BLOCK, NONE
    }
    public final static class RayTraceIntersection {
        public HitType hit = HitType.NONE;
        public int blockId;
        public final BlockPos blockPos = new BlockPos();
        public final BlockPos q = new BlockPos();
        public final Vector3f pos = new Vector3f();
        public float distance;
        public int face;
    }
    private static final float MIN_X = World.MIN_XZ;
    private static final float MIN_Y = 0;
    private static final float MIN_Z = World.MIN_XZ;
    private static final float MAX_X = World.MAX_XZ;
    private static final float MAX_Y = World.MAX_WORLDHEIGHT;
    private static final float MAX_Z = World.MAX_XZ;

    // Buffer for reporting faces to the callback.
    private Vector3f dirFrac = new Vector3f();
    private AABBFloat bb = new AABBFloat();
    private final RayTraceIntersection intersection = new RayTraceIntersection();
    public BlockPos quarter= new BlockPos();
    public boolean quarterMode;


    public void reset() {
        intersection.hit = HitType.NONE;
    }

    public void doRaytrace(World world, Vector3f origin, Vector3f direction, int maxDist) {
        intersection.hit = HitType.NONE;
        // From "A Fast Voxel Traversal Algorithm for Ray Tracing"
        // by John Amanatides and Andrew Woo, 1987
        // <http://www.cse.yorku.ca/~amana/research/grid.pdf>
        // <http://citeseer.ist.psu.edu/viewdoc/summary?doi=10.1.1.42.3443>
        // Extensions to the described algorithm:
        //   • Imposed a distance limit.
        //   • The face passed through to reach the current cube is provided to
        //     the callback.
        
        // The foundation of this algorithm is a parameterized representation of
        // the provided ray,
        //                    origin + t * direction,
        // except that t is not actually stored; rather, at any given point in the
        // traversal, we keep track of the *greater* t values which we would have
        // if we took a step sufficient to cross a cube boundary along that axis
        // (i.e. change the integer part of the coordinate) in the variables
        // tMaxX, tMaxY, and tMaxZ.
        
        // Cube containing origin point.
        int x = GameMath.floor(origin.x);
        int y = GameMath.floor(origin.y);
        int z = GameMath.floor(origin.z);
//        System.out.println(MIN_X+"/"+MAX_X);
        // Break out direction vector.
        float dx = direction.x;
        float dy = direction.y;
        float dz = direction.z;
        dirFrac.set(1.0f/(GameMath.isNormalFloat(dx)?dx:1.0E-5F), 1.0f/(GameMath.isNormalFloat(dy)?dy:1.0E-5F), 1.0f/(GameMath.isNormalFloat(dz)?dz:1.0E-5F));
        // Direction to increment x,y,z when stepping.
        float stepX = GameMath.signum(dx);
        float stepY = GameMath.signum(dy);
        float stepZ = GameMath.signum(dz);
        // See description above. The initial values depend on the fractional
        // part of the origin.
        float tMaxX = intbound(origin.x, dx);
        float tMaxY = intbound(origin.y, dy);
        float tMaxZ = intbound(origin.z, dz);
        // The change in t when taking a step (always positive).
        float tDeltaX = stepX/dx;
        float tDeltaY = stepY/dy;
        float tDeltaZ = stepZ/dz;
        
        // Avoids an infinite loop.

//        System.out.println(dx+"/"+dy+"/"+dz);
        if ((!GameMath.isNormalFloat(dx) && !GameMath.isNormalFloat(dy) && !GameMath.isNormalFloat(dz))) {
            System.err.println("Raycast in zero direction ("+dx+"/"+dy+"/"+dz+")!");
            return;
        }
        
        // Rescale from units of 1 cube-edge to units of 'direction' so we can
        // compare with 't'.
        float radius = maxDist*1.5F;
        radius /= Math.sqrt(dx*dx+dy*dy+dz*dz);
        int maxSteps = maxDist;
        while (/* ray has not gone past bounds of world */
               (stepX > 0 ? x < MAX_X : x >= MIN_X) &&
               (stepY > 0 ? y < MAX_Y : y >= MIN_Y) &&
               (stepZ > 0 ? z < MAX_Z : z >= MIN_Z) && maxSteps-- > 0) {
          
          // Invoke the callback, unless we are not *yet* within the bounds of the
          // world.
          if (callback(world, x, y, z, origin, direction))
            break;
          
          // tMaxX stores the t-value at which we cross a cube boundary along the
          // X axis, and similarly for Y and Z. Therefore, choosing the least tMax
          // chooses the closest cube boundary. Only the first case of the four
          // has been commented in detail.
          if (tMaxX < tMaxY) {
            if (tMaxX < tMaxZ) {
              if (tMaxX > radius) break;
              // Update which cube we are now in.
              x += stepX;
              // Adjust tMaxX to the next X-oriented boundary crossing.
              tMaxX += tDeltaX;
              // Record the normal vector of the cube face we entered.
            } else {
              if (tMaxZ > radius) break;
              z += stepZ;
              tMaxZ += tDeltaZ;
            }
          } else {
            if (tMaxY < tMaxZ) {
              if (tMaxY > radius) break;
              y += stepY;
              tMaxY += tDeltaY;
            } else {
              // Identical to the second case, repeated for simplicity in
              // the conditionals.
              if (tMaxZ > radius) break;
              z += stepZ;
              tMaxZ += tDeltaZ;
            }
          }
        }
    }

    
    private boolean callback(World world, int x, int y, int z, Vector3f origin, Vector3f direction) {
        int type = world.getType(x, y, z);
        if (type != 0) {
            Block block = Block.get(type);
//            if (block.isReplaceable()) {
//                if (Game.instance.selBlock != null && Game.instance.selBlock.id > 0) {
//                    return false;
//                }
//            }
            this.intersection.hit = HitType.NONE;
            if (rayTraceBlock(block) && block.raytrace(this, world, x, y, z, origin, direction, dirFrac)) {
                if (this.quarterMode && block != Block.quarter) {
                    this.intersection.q.set(0, 0, 0);
                    if (this.intersection.face == Dir.DIR_POS_X || this.intersection.pos.x-x >= 0.5f) {
                        this.intersection.q.x++;
                    }
                    if (this.intersection.face == Dir.DIR_POS_Y || this.intersection.pos.y-y >= 0.5f) {
                        this.intersection.q.y++;
                    }
                    if (this.intersection.face == Dir.DIR_POS_Z || this.intersection.pos.z-z >= 0.5f) {
                        this.intersection.q.z++;
                    }
                }
                this.intersection.blockId = type;
                this.intersection.blockPos.set(x, y, z);
                return true;
            }
        }
        return false;
    }

    public boolean rayTraceBlock(Block block) {
        return true;
    }

    float intbound(float s, float ds) {
//      // Find the smallest positive t such that s+t*ds is an integer.
//      if (ds < 0) {
//        return intbound(-s, -ds);
//      } else {
//        s = GameMath.mod(s, ds);
//        // problem is now s+t*ds = 1
//        return (1-s)/ds;
//      }
        return (float) ((ds > 0? Math.ceil(s)-s: s-Math.floor(s)) / Math.abs(ds));
    }
    

    public boolean hasHit() {
        return this.intersection.hit != HitType.NONE;
    }
    public RayTraceIntersection getHit() {
        return this.intersection;
    }

    /**
     * @param i
     * @return 
     */
    public AABBFloat getTempBB() {
        return this.bb;
    }

    /**
     * @param direction 
     * @param origin 
     * @param tmin
     * @param iMin
     */
    public void setIntersection(Vector3f origin, Vector3f direction, float distance, int iMin) {
        if (this.intersection.hit == HitType.NONE || distance < this.intersection.distance) {
            this.intersection.hit = HitType.BLOCK;
            Vector3f hitPoint = this.intersection.pos;
            hitPoint.set(direction);
            hitPoint.scale(distance);
            hitPoint.addVec(origin);
            
            this.intersection.q.set(this.quarter);
            this.intersection.distance = distance;
            this.intersection.face = iMin;
        }
    }
    
}
