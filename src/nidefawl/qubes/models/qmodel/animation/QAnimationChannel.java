/**
 * 
 */
package nidefawl.qubes.models.qmodel.animation;


import java.util.List;

import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.Matrix4f;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class QAnimationChannel {
    public KeyFrame[] frames;
    public float animLength;
    public float frameLength;
    public float startOffset;
    public int priority = 0;
    /**
     * @param numRotFrames
     * @param frameLen 
     * @param numTranslationFrames
     */
    public QAnimationChannel(int numRotFrames, float frameLen) {
        this.frames = new KeyFrame[numRotFrames];
        this.frameLength = frameLen;
    }
    public QAnimationChannel(List<KeyFrame> frames) {
        this.frames = new KeyFrame[frames.size()];
        for (KeyFrame f : frames) {
            this.addFrame(f);
        }
        
    }

    public KeyFrame getFrameAt(int type, float absTime) {
        if (type == 1) {
            int pos = GameMath.floor(absTime);
            if (pos < 0) {
                pos = 0;
            }
            if (pos >= this.frames.length) {
                pos = this.frames.length-1;
            }
            return this.frames[pos];
        }
        float time = absTime%this.animLength;
        //TODO: too slow?
        for (int i = 0; i < this.frames.length-1; i++) {
            if (this.frames[i].time-this.startOffset <= time && this.frames[i].time-this.startOffset+this.frameLength >= time) {
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
    public QAnimationChannel split(int i, int j) {
        QAnimationChannel anim = new QAnimationChannel(j-i, this.frameLength);
        for (int k = i; k < j; k++) {
            anim.frames[k-i] = this.frames[k].copy();
            if (k-i>0) {
                anim.frames[k-i-1].next = anim.frames[k-i];
            }
        }
        anim.frames[anim.frames.length-1].next = anim.frames[0];
        anim.startOffset = anim.frames[0].time;
        anim.animLength = (anim.frames[anim.frames.length-1].time - anim.frames[0].time) + this.frameLength;
        return anim;
    }
    public boolean setDeform(int animationType, float time, Matrix4f matDeform) {
        if (this.frames.length > 0) {
            float f = time;
//            if (animationType == 1) {
//                f = time*this.frames.length;
//            }
            QModelKeyFrameMatrix frame = (QModelKeyFrameMatrix) this.getFrameAt(animationType, f);
            QModelKeyFrameMatrix nextframe = (QModelKeyFrameMatrix) frame.getNext();
            //TODO: if nextframe < frame
            
            float frameInterpProgress;
            if (animationType == 1) {
                frameInterpProgress = f%1.0f;
            } else {
                float fTime1 = frame.time-startOffset;
                float totalLen = this.animLength;
                float nextT = (f%totalLen)-fTime1;
                frameInterpProgress = nextT / frameLength;
            }
            if (frameInterpProgress <= 0) {
                matDeform.load(frame.mat);
            } else if (frameInterpProgress >= 1 ) {
                matDeform.load(nextframe.mat);
            } else {
                matDeform.setZero();
                matDeform.addWeighted(frame.mat, 1.0f-frameInterpProgress);
                matDeform.addWeighted(nextframe.mat, frameInterpProgress);
            }
            return true;
        } else {
        }

        return false;
    }
}
