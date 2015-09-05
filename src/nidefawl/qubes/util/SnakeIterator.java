package nidefawl.qubes.util;

public class SnakeIterator {
    private int dx;
    private int dz;
    private int currentFace;
    private int pos;
    private int legPos;
    private int legsize = 0;
    private int currentLeg = 0;
    private int max = 0;
    private final int[][] direction = new int[][] { { 1, 0 }, { 0, 1 }, { -1, 0 }, { 0, -1 } };
    public SnakeIterator() {
        reset();
    }

    public void reset() {
         dx = 0;
         dz = 0;
         currentFace = 0;
         pos = 0;
         currentLeg = 0;
         legsize = 0;
         legPos = 0;
    }
    public boolean hasMore(int max) {
        return pos < max;
    }
    public void next() {
        while (true) {
            while (currentLeg < 2) {
                final int[] dir = this.direction[currentFace];
                if (legPos < legsize) {
                    dx += dir[0];
                    dz += dir[1];
                    ++legPos;
                    pos++;
                    if (max>0&&(dx<-max||dx>max||dz<-max||dz>max)) {
                        reset();
                    }
                    return;
                }
                currentFace++;
                currentFace = currentFace % 4;
                currentLeg++;
                legPos = 0;
            }
            currentLeg = 0;
            ++legsize;
        }
    }
    public int getX() {
        return dx;
    }
    public int getZ() {
        return dz;
    }
    public int getPos() {
        return pos;
    }

    public void setMax(int max) {
        this.max = max;
    }
}