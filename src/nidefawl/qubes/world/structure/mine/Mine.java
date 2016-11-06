package nidefawl.qubes.world.structure.mine;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.nbt.Tag.Compound;
import nidefawl.qubes.util.Flags;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.WorldServer;
import nidefawl.qubes.world.biomes.HexBiome;
import nidefawl.qubes.world.structure.Structure;

public class Mine extends Structure {
    
    public Mine() {
    }

    public int dir;
    public BlockPos pos;
    
    @Override
    public Compound save() {
        Compound cmp = super.save();
        cmp.setInt("dir", this.dir);
        cmp.setBlockPos("pos", this.pos);
        return cmp;
    }
    
    @Override
    public void load(Compound cmpTree) {
        super.load(cmpTree);
        this.dir = cmpTree.getInt("dir");
    }

    public void regen(WorldServer world) {
        BlockPos pos = this.pos;
        HexBiome hex = world.getHex(pos.x, pos.z);
        Block b = Block.get(hex.biome.getOre(world, pos.x, pos.y, pos.z, hex, world.getRand()));
        int r = 5;
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = -r; y <= r; y++) {
                    float dist = GameMath.distSq3Di(x, y, z, 0,0,0);
                    if (dist+world.getRand().nextFloat()<5.0f) {
                        world.setType(pos.x+x, pos.y+y, pos.z+z, b.id, Flags.MARK|Flags.LIGHT);
                    }       
                }
            }
        }
    }
}
