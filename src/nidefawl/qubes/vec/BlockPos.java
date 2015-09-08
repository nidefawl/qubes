package nidefawl.qubes.vec;

import nidefawl.qubes.util.TripletIntHash;

public class BlockPos {
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
}
