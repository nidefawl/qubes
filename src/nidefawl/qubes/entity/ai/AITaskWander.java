package nidefawl.qubes.entity.ai;

import nidefawl.qubes.entity.EntityAI;
import nidefawl.qubes.path.Path;
import nidefawl.qubes.path.RandomPosGen;
import nidefawl.qubes.vec.BlockPos;

public class AITaskWander extends AITask {
    private int ticks;
    RandomPosGen gen = new RandomPosGen();
    private BlockPos pos;
    private Path p;
    public AITaskWander(EntityAI entity) {
        super(entity);
    }

    @Override
    public boolean keepExecuting() {
        if (this.ticks > 600) {
            return false;
        }
        if (this.p != null && this.entity.getNav().getPath() == this.p && !this.p.isFinished()) {
            return true;
        }
        return false;
    }
    @Override
    public boolean shouldExecute() {
        if (entity.getRandom().nextInt(2) != 0) {
            return false;
        }
        pos = gen.find(this.entity, null, 16, 5);
        
        return pos != null;
    }
    @Override
    public void update() {
        this.ticks++;
    }
    @Override
    public void start() {
        this.ticks = 0;
        this.p = this.entity.getNav().tryMoveTo(pos.x, pos.y, pos.z, 20);
    }
    @Override
    public void stop() {
        this.pos = null;
        this.p = null;
    }
}
