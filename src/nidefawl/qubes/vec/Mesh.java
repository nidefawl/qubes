package nidefawl.qubes.vec;

public class Mesh {
    public final int type;
    public final int v0[];
    public final int v1[];
    public final int v2[];
    public final int v3[];
    public final byte normal[];

    public Mesh(int type, int[] v0, int[] v1, int[] v2, int[] v3, byte[] normal) {
        this.v0 = v0;
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.normal = normal;
        this.type = type;
    }
}