package nidefawl.qubes.render;

public abstract class BlurRenderer extends AbstractRenderer {
    protected final int[][] kawaseKernelSizePasses = new int[] [] {
        {0,0},
        {0, 1, 1},
        {0, 1, 2, 2, 3},
        {0, 1, 2, 3, 4, 4, 5},
        {0, 1, 2, 3, 4, 5, 7, 8, 9, 10},
    };
    protected float w1;
    protected float h1;

}
