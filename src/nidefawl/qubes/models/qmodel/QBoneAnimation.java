/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import nidefawl.qubes.util.GameMath;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class QBoneAnimation {
    public KeyFrame[] frames;
    public float animLength;
    public float totalFrameTime;

    /**
     * @param numRotFrames
     * @param numTranslationFrames
     */
    public QBoneAnimation(int numRotFrames) {
        this.frames = new KeyFrame[numRotFrames];
        
    }

    public KeyFrame getFrameAt(int type, float absTime) {
        if (type == 1) {
            int pos = GameMath.floor(absTime);
            if (pos >= this.frames.length) {
                pos = this.frames.length-1;
            }
            return this.frames[pos];
        }
        float time = absTime%this.animLength;
        //TODO: too slow?
        for (int i = 0; i < this.frames.length-1; i++) {
            if (this.frames[i].time <= time && this.frames[i+1].time >= time) {
                return this.frames[i];
            }
        }
        return this.frames[this.frames.length-1];
    }
    /**
     * @param frame
     */
    public void addFrame(KeyFrame frame) {
        frames[frame.getIdx()] = frame;
        if (frame.getIdx() > 0) {
            frames[frame.getIdx()-1].next = frame;
        }
        frame.next = frames[0];
        float f = this.animLength;
        if (f < frame.time) {
            this.animLength = frame.time;
        }
    }
}
