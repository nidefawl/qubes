package nidefawl.qubes.entity.ai;

import java.util.ArrayList;

import nidefawl.qubes.entity.EntityAI;
import nidefawl.qubes.network.packet.PacketSDebugPath;
import nidefawl.qubes.path.Path;
import nidefawl.qubes.path.PathFinder;
import nidefawl.qubes.path.PathPoint;
import nidefawl.qubes.perf.TimingHelper;
import nidefawl.qubes.vec.Vec3D;
import nidefawl.qubes.world.WorldServer;

public class AINav {
    PathFinder finder = new PathFinder();
    Vec3D goalPos = new Vec3D();
    Vec3D tmp = new Vec3D();
    private Path path;
    int ticks = 0;
    private float speed = 0.3f;
    private EntityAI entity;
    public AINav(EntityAI entity) {
        this.entity = entity;
    }
    public Path tryMoveTo(int x, int y, int z, float maxDist) {
        tmp.set(x, y, z);
//        TimingHelper.startSilent(1);;
        Path path = finder.findPath(this.entity, tmp, maxDist);
//        long l = TimingHelper.stopSilent(1);;
//        System.out.println(l);
        speed = 0.9f;
        return setPath(path);
    }
    private Path setPath(Path p) {
        if (p!=null&&p.getLength()<=0) {
            p = null;
        }
        this.ticks = 0;
        this.path = p;
//        if (this.path != null) {
//            ArrayList<PathPoint> list = new ArrayList<>();
//            for (int i = 0; i < path.getLength(); i++) {
//                list.add(path.get(i));
//            }
//            PacketSDebugPath path = new PacketSDebugPath(list);
//            ((WorldServer)entity.world).broadcastPacket(path);
//        }
        if (this.path != null) {
            PathPoint end = this.path.getEnd();
            end.getPosition(entity, this.goalPos);    
        }
        return p;
    }
    public void update() {
        if (!hasPath()) {
            return;
        }
        ticks++;
        double dmax = Math.min(this.entity.width, this.entity.length);
        dmax*=dmax;
        double dxxx = this.entity.pos.distanceSq(this.goalPos);
//        System.out.println("goal dist "+dxxx+", max "+dmax);
        if (dxxx < dmax) {
//            System.out.println("nullllL");
            setPath(null);
            return;
        }
        if (!hasPath()) {
            return;
        }
        if (canUpdatePath()) {
            updatePath();
        }
        if (!hasPath()) {
            return;
        }
        PathPoint p = this.path.get();
        if (p != null) {
            p.getPosition(entity, tmp);
            entity.getMove().moveTowards(tmp.x, tmp.y, tmp.z, this.speed);
        }
    }
    public boolean hasPath() {
        return this.path != null && !this.path.isFinished();
    }
    private void updatePath() {
        int pos = this.path.getPos();
        int len = this.path.getLength();
        double minXZ = 0;
        int minIdx = 0;
        
        for (int i = 0; i < len; i++) {
            PathPoint p = this.path.get(i);
            p.getPosition(entity, tmp);
            double d = tmp.distanceSq(this.entity.pos);
            if (i==0||d < minXZ) {
                minXZ = d;
                minIdx = i+1;
            }
        }
        path.setPos(minIdx);
        
        if (path.isFinished()) {
            path = null;
        }
    }
    private boolean canUpdatePath() {
        return this.entity.hitGround;
    }
    public Path getPath() {
        return this.path;
    }
}
