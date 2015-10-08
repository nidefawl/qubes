/**
 * 
 */
package nidefawl.qubes.meshing;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class SlicedBlockFaceInfo {
    int[] tex = new int[4];
    private int q;
    public void setFace(int i, int tex) {
        this.tex[i] = tex;
    }
    /**
     * @param info
     * @param info2
     * @return 
     */
    public int getVisible(SlicedBlockFaceInfo info, SlicedBlockFaceInfo info2) {
        int d,n=0;
        for (int i = 0; i < 4; i++) {
            
            if (info2.tex[i] == 0) {
                d = info.tex[i];
            } else {
                d = 0;
                n++;
            }
            this.tex[i] = d;
        }
        return n;
    }
    /**
     * @param i
     */
    public void fill(int d) {
        for (int i = 0; i < 4; i++) {
            this.tex[i] = d;
        }
    }
    /**
     * @param i
     */
    public void setQ(int i) {
        fill(0);
        this.q = i;
    }
    /**
     * @return the q
     */
    public int getQ() {
        return this.q;
    }

}
