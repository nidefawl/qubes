/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import java.io.EOFException;
import java.util.Map;

import com.google.common.collect.Maps;

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
    Map<String, QBoneAnimation> map = Maps.newHashMap();
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
        int numBones = loader.readUShort();
        for (int boneIdx = 0; boneIdx < numBones; boneIdx++) {
            String boneName = loader.readString(32);
            int numFrames = loader.readUShort();
            QBoneAnimation anim = new QBoneAnimation(numFrames);
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
            map.put(boneName, anim);
        }
    }

}
