package nidefawl.qubes.vec;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.network.StreamIO;

public class ChunkPos implements StreamIO {
    public int x, z;
    public ChunkPos() {
    }
    public ChunkPos(int x, int z) {
        this.x = x;
        this.z = z;
    }
    
    
    
    @Override
    public String toString() {
        return "ChunkPos["+x+","+z+"]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BlockPos) {
            BlockPos p = (BlockPos)obj;
            return this.x == p.x && this.z == p.z;
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return x<<16|z; //watch out for collisions 
    }
    
    public ChunkPos copy() {
        return new ChunkPos(this.x, this.z);
    }
    @Override
    public void read(DataInput in) throws IOException {
        this.x = in.readInt();
        this.z = in.readInt();
    }
    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(this.x);
        out.writeInt(this.z);
    }
    /**
     * @param blockPos
     */
    public void set(BlockPos blockPos) {
        this.x = blockPos.x;
        this.z = blockPos.z;
    }
    public boolean isEqualTo(int x, int y, int z) {
        return this.x == x && this.z == z;
    }
}
