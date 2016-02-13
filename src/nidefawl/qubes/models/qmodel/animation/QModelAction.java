/**
 * 
 */
package nidefawl.qubes.models.qmodel.animation;

import java.io.EOFException;
import java.util.Map;

import com.google.common.collect.Maps;

import nidefawl.qubes.models.qmodel.loader.ModelLoaderQModel;
import nidefawl.qubes.vec.Matrix4f;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class QModelAction {

    public int idx;
    public int flags;
    public String name;
    public float fps;
    public int startFrame;
    public int endFrame;
    public int totalFrames;
    public Map<String, QAnimationChannel> map = Maps.newHashMap();
    public QAnimationChannel armatureAnim;
    public float lenTime;
    public QModelAction() {
    }

    /**
     * @param i
     * @param modelLoaderQModel
     * @throws EOFException 
     */
    public QModelAction(int i, ModelLoaderQModel loader) throws EOFException {
        this.idx = i;
        this.flags = loader.readUByte();
        this.name = loader.readString(32);
        this.fps = loader.readFloat();
        this.startFrame = loader.readUShort();
        this.endFrame = loader.readUShort();
        this.lenTime = (this.endFrame-this.startFrame+1)/this.fps;
        float frameLen = 1.0f / this.fps;
        int numBones = loader.readUShort();
        for (int boneIdx = 0; boneIdx < numBones; boneIdx++) {
            String boneName = loader.readString(32);
            int numFrames = loader.readUShort();
            QAnimationChannel anim = new QAnimationChannel(numFrames, frameLen);
            for (int frameIdx = 0; frameIdx < numFrames; frameIdx++) {
                float time = loader.readFloat();
                Matrix4f matAnim = new Matrix4f();
                float[] mat = new float[16];
                for (int l = 0; l < 16; l++) {
                    mat[l] = loader.readFloat();
                }
                matAnim.load(mat);
                QModelKeyFrameMatrix frame = new QModelKeyFrameMatrix(frameIdx, time, matAnim);
                anim.addFrame(frame);
            }
            if (boneName.equals("Armature"))
                this.armatureAnim = anim;
            else
                map.put(boneName, anim);
        }
    }

    public QModelAction split(String act, int i, int j) {
        Map<String, QAnimationChannel> newMap = Maps.newHashMap();
        for (String s : this.map.keySet()) {
            QAnimationChannel anim = this.map.get(s);
            newMap.put(s, anim.split(i, j));
        }
        QModelAction action = new QModelAction();
        if (this.armatureAnim != null) {
            action.armatureAnim = this.armatureAnim.split(i, j);
        }
        action.flags = this.flags;
        action.name = act;
        action.fps = this.fps; 
        action.startFrame = i;
        action.endFrame = j;
        action.lenTime = (action.endFrame-action.startFrame+1)/action.fps;
        action.map = newMap;
        return action;
    }

}
