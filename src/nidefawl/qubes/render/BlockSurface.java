package nidefawl.qubes.render;

public class BlockSurface {

    public BlockSurface() {
    }
    public boolean transparent;
    public int type;
    public int face;
    public int axis;
    public int x;
    public int y;
    public int z;
    public int pass;
    
    
    public boolean mergeWith(BlockSurface c) {
        if (c.type == this.type && c.face == this.face && c.pass == this.pass) {
//            if (Math.abs(this.x-c.x)>3||Math.abs(this.z-c.z)>3) {
//                System.out.println("no");
//                return false;
//            }
//            int bface = this.axis<<1|this.face;
//            if (bface==2) {
//                return false;
//            }
            return true;
        }
        return false;
    }

}
