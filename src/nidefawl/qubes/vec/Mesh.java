package nidefawl.qubes.vec;

public class Mesh {
    public final int type;
    public final int v0[];
    public final int v1[];
    public final int v2[];
    public final int v3[];
    public final int du[];
    public final int dv[];
    public final byte normal[];
    public int faceDir;

    public Mesh(int type, int[] v0, int[] v1, int[] v2, int[] v3, int[] du, int[] dv, byte[] normal, int faceDir) {
        this.v0 = v0;
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.du = du;
        this.dv = dv;
        this.normal = normal;
        this.type = type;
        this.faceDir = faceDir;
    }
}