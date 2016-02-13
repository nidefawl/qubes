/**
 * 
 */
package nidefawl.qubes.models.qmodel.animation;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public abstract class KeyFrame {
    public float time;
    public int idx;
    KeyFrame next;
    public KeyFrame(int idx, float time) {
        this.idx = idx;
        this.time = time;
    }
    /**
     * @return
     */
    public abstract int getType();
    /**
     * @return
     */
    public int getIdx() {
        return idx;
    }
    /**
     * @return
     */
    public KeyFrame getNext() {
        return this.next;
    }
    public KeyFrame copy() {
        return null;
    }

}
