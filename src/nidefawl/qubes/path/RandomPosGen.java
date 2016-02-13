package nidefawl.qubes.path;


import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.vec.Vec3D;

public class RandomPosGen {
    final BlockPos tmp = new BlockPos();
    public BlockPos find(Entity ent, Vec3D to, int distance, int maxHeightDistance) {
        if (to != null) {
            
        }
        int x, y, z;
        x = GameMath.floor(ent.pos.x);
        y = GameMath.floor(ent.pos.y);
        z = GameMath.floor(ent.pos.z);
        tmp.set(x, y, z);
        float fWeight = 0;
        for (int i = 0; i < 10; i++) {
            int rx, ry, rz;
            rx = ent.getRandom().nextInt(2*distance+1)-distance;
            rz = ent.getRandom().nextInt(2*distance+1)-distance;
            ry = maxHeightDistance == 0 ? 0 : (ent.getRandom().nextInt(2*maxHeightDistance+1)-maxHeightDistance);
            rx+=x;
            ry+=y;
            rz+=z;
            
            float fWeight2 = ent.getPathWeight(rx, ry, rz);
            if (i == 0 || fWeight2 > fWeight) {
                fWeight = fWeight2;
                tmp.set(rx, ry, rz);
            }
        }
        return tmp;
    }
}
