package nidefawl.qubes.vec;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import nidefawl.qubes.network.StreamIO;
import nidefawl.qubes.util.TripletIntHash;

public class BlockPos implements StreamIO {
    public int x, y, z;
	public BlockPos() {
	}
	public BlockPos(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	
	
	@Override
	public String toString() {
	    return "BlockPos["+x+","+y+","+z+"]";
	}

	@Override
	public boolean equals(Object obj) {
	    if (obj instanceof BlockPos) {
	        BlockPos p = (BlockPos)obj;
	        return this.x == p.x && this.y == p.y && this.z == p.z;
	    }
        return false;
	}
	
	@Override
	public int hashCode() {
	    return TripletIntHash.toHash(x, y, z);
	}
	
    public BlockPos copy() {
        return new BlockPos(this.x, this.y, this.z);
    }
    /**
     * @param x
     * @param y
     * @param z
     */
    public void set(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    /**
     * @param face
     */
    public void offset(int face) {
        this.x += Dir.getDirX(face);
        this.y += Dir.getDirY(face);
        this.z += Dir.getDirZ(face);
    }
    @Override
    public void read(DataInput in) throws IOException {
        this.x = in.readInt();
        this.y = in.readInt();
        this.z = in.readInt();
    }
    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(this.x);
        out.writeInt(this.y);
        out.writeInt(this.z);
    }
    /**
     * @param blockPos
     */
    public void set(BlockPos blockPos) {
        this.x = blockPos.x;
        this.y = blockPos.y;
        this.z = blockPos.z;
    }
    /**
     * @return
     */
    public int getVolume() {
        return this.x*this.y*this.z;
    }
    /**
     * @param offX
     * @param offY
     * @param offZ
     * @return
     */
    public boolean isEqualTo(int x, int y, int z) {
        return this.x == x && this.y == y && this.z == z;
    }
}
