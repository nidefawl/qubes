/**
 * 
 */
package nidefawl.qubes.models.qmodel;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class QJointAnimation {
    public KeyFrame[][] frames = new KeyFrame[2][];
    public float[] animLength = new float[2];
    public float totalFrameTimeTranslate;
    public float totalFrameTimeRot;

    /**
     * @param numRotFrames
     * @param numTranslationFrames
     */
    public QJointAnimation(int numRotFrames, int numTranslationFrames) {
        this.frames[0] = new KeyFrame[numRotFrames];
        this.frames[1] = new KeyFrame[numTranslationFrames];
        
    }
    public KeyFrame[] getFrames(int type) {
        return frames[type];
    }
    public KeyFrame getFrame(int type, int frame) {
        KeyFrame[] arr = getFrames(type);
        return arr[frame];
    }
    public KeyFrame getFrameAt(int type, float absTime) {
        float fTime = this.animLength[type];
        KeyFrame[] arr = getFrames(type);
        float time = absTime%fTime;
        //TODO: too slow?
        for (int i = 0; i < arr.length-1; i++) {
            if (arr[i].time <= time && arr[i+1].time >= time) {
                return arr[i];
            }
        }
        return arr[arr.length-1];
    }
    /**
     * @param frame
     */
    public void addFrame(KeyFrame frame) {
        KeyFrame[] frames = getFrames(frame.getType());
        frames[frame.getIdx()] = frame;
        if (frame.getIdx() > 0) {
            frames[frame.getIdx()-1].next = frame;
        }
        frame.next = frames[0];
        float f = this.animLength[frame.getType()];
        if (f < frame.time) {
            this.animLength[frame.getType()] = frame.time;
        }
    }
}
