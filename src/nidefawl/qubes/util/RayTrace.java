// Copyright 2011-2012 Kevin Reid under the terms of the MIT License as detailed
// in the accompanying file README.md or <http://opensource.org/licenses/MIT>.
// modified by Michael Hept (2015)

package nidefawl.qubes.util;

import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.vec.Vec3;
import nidefawl.qubes.world.World;

public class RayTrace {
    private static final float MIN_X = World.MIN_XZ;
    private static final float MIN_Y = 0;
    private static final float MIN_Z = World.MIN_XZ;
    private static final float MAX_X = World.MAX_XZ;
    private static final float MAX_Y = World.MAX_WORLDHEIGHT;
    private static final float MAX_Z = World.MAX_XZ;
    
    private BlockPos coll;
    private int collType;
    private BlockPos face;


    public void reset() {
        coll = null;
        collType = 0;
        face = null;
    }

    public void doRaytrace(World world, Vec3 origin, Vec3 direction) {
        coll = null;
        collType = 0;
        face = null;
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
//        System.out.println(x+"/"+y+"/"+z);
//        System.out.println(MIN_X+"/"+MAX_X);
        // Break out direction vector.
        float dx = direction.x;
        float dy = direction.y;
        float dz = direction.z;
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
        // Buffer for reporting faces to the callback.
        BlockPos face = new BlockPos();
        
        // Avoids an infinite loop.
        
        if ((!GameMath.isNormalFloat(dx) && !GameMath.isNormalFloat(dy) && !GameMath.isNormalFloat(dz))) {
            System.err.println("Raycast in zero direction ("+dx+"/"+dy+"/"+dz+")!");
            return;
        }
        
        // Rescale from units of 1 cube-edge to units of 'direction' so we can
        // compare with 't'.
        float radius = 120*1.5F;
        radius /= Math.sqrt(dx*dx+dy*dy+dz*dz);
        int maxSteps = 120;
        while (/* ray has not gone past bounds of world */
               (stepX > 0 ? x < MAX_X : x >= MIN_X) &&
               (stepY > 0 ? y < MAX_Y : y >= MIN_Y) &&
               (stepZ > 0 ? z < MAX_X : z >= MIN_Z) && maxSteps-- > 0) {
          
          // Invoke the callback, unless we are not *yet* within the bounds of the
          // world.
          if (!(x < MIN_X || y < MIN_Y || z < MIN_Z || x >= MAX_X || y >= MAX_Y || z >= MAX_Z))
            if (callback(world, x, y, z, face))
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
              face.x = -GameMath.signum(stepX);
              face.y = 0;
              face.z = 0;
            } else {
              if (tMaxZ > radius) break;
              z += stepZ;
              tMaxZ += tDeltaZ;
              face.x = 0;
              face.y = 0;
              face.z = -GameMath.signum(stepZ);
            }
          } else {
            if (tMaxY < tMaxZ) {
              if (tMaxY > radius) break;
              y += stepY;
              tMaxY += tDeltaY;
              face.x = 0;
              face.y = -GameMath.signum(stepY);
              face.z = 0;
            } else {
              // Identical to the second case, repeated for simplicity in
              // the conditionals.
              if (tMaxZ > radius) break;
              z += stepZ;
              tMaxZ += tDeltaZ;
              face.x = 0;
              face.y = 0;
              face.z = -GameMath.signum(stepZ);
            }
          }
        }
    }

    
    private boolean callback(World world, int x, int y, int z, BlockPos face) {
        int type = world.getType(x, y, z);
        if (type != 0) {
            this.coll = new BlockPos(x, y, z);
            this.face = face;
            this.collType = type;
            return true;
        }
        return false;
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
    
    public BlockPos getColl() {
        return coll;
    }
    public int getCollType() {
        return collType;
    }
    
    public BlockPos getFace() {
        return face;
    }
    
}
