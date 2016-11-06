package nidefawl.qubes.models.qmodel.loader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.google.common.collect.Lists;

import nidefawl.qubes.assets.AssetBinary;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.io.BinaryStreamReader;
import nidefawl.qubes.models.qmodel.*;
import nidefawl.qubes.models.qmodel.animation.*;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.vec.*;

public class ModelAnimationLoader extends BinaryStreamReader {


    public ModelAnimationLoader() {
    }


    private String path;

    public QModelAction loadAnimation(String path, ModelRigged model) {
        AssetBinary bin = AssetManager.getInstance().loadBin(path);
        return loadAnimation(bin, model);
    }

    public QModelAction loadAnimation(AssetBinary bin, ModelRigged model) {
        try {
            this.asset = bin;
            this.path = bin.getPack()+":"+bin.getName();
            resetOffset();
            QModelAction action = new QModelAction();
            int nChannels = readInt();
            double ticksPerSecond = readDouble();
            double ticksDuration = readDouble();
            float timeScale = (float) (1.0f/ticksPerSecond);
            System.out.println("CHANNELS "+nChannels);
            System.out.println("ticksPerSecond "+ticksPerSecond);
            System.out.println("ticksDuration "+ticksDuration);
            action.name = bin.getName();
            action.idx = 0;
            for (int i = 0; i < nChannels; i++) {
                String chanName = readString(128);
                int lenPos = readInt();
                int lenRot = readInt();
                int lenScale = readInt();
                double minTime = Double.MAX_VALUE;
                double maxTime = 0;
              System.out.println("chanName "+chanName+" "+lenPos);
//            System.out.println("chanName "+chanName+" "+listPos.size()+","+listRot.size()+","+listScale.size());

                ArrayList<KeyFrame> listPos = Lists.newArrayList();
                for (int j = 0; j < lenPos; j++) {
                    double time = readDouble();
                    if (Double.isNaN(time)) {
                        throw new IOException("NaN");
                    }
                    Vector4f pos4f = readVec4();
                    Vector3f pos3f = new Vector3f(pos4f.z, pos4f.x, pos4f.y);
                    listPos.add(new QModelKeyFramePosition(j, (float) time, pos3f));
                    minTime = Math.min(time, minTime);
                    maxTime = Math.max(time, maxTime);
                }

                ArrayList<KeyFrame> listRot = Lists.newArrayList();
                for (int j = 0; j < lenRot; j++) {
                    double time = readDouble();
                    if (Double.isNaN(time)) {
                        throw new IOException("NaN");
                    }
                    Quaternion pos4f = new Quaternion(readVec4());
                    listRot.add(new QModelKeyFrameRotation(j, (float) time, pos4f));
                    minTime = Math.min(time, minTime);
                    maxTime = Math.max(time, maxTime);
                }
                
                ArrayList<KeyFrame> listScale = Lists.newArrayList();
                for (int j = 0; j < lenScale; j++) {
                    double time = readDouble();
                    if (Double.isNaN(time)) {
                        throw new IOException("NaN");
                    }
                    Vector4f pos4f = readVec4();
                    Vector3f pos3f = new Vector3f(pos4f.z, pos4f.y, pos4f.x);
                    listScale.add(new QModelKeyFrameScale(j, (float) time, pos3f));
                    minTime = Math.min(time, minTime);
                    maxTime = Math.max(time, maxTime);
                }
                Comparator<KeyFrame> fr = new Comparator<KeyFrame>() {
                    @Override
                    public int compare(KeyFrame o1, KeyFrame o2) {
                        int n = Double.compare(o1.time, o2.time);
                        return n;
                    }
                };
                Collections.sort(listPos, fr);
                Collections.sort(listRot, fr);
                Collections.sort(listScale, fr);
                int iMaxTime = (int) Math.ceil(maxTime);
                if (iMaxTime > 10000) {
                    throw new IOException("animation too long: "+iMaxTime);
                }
                QAnimationChannel animPos = new QAnimationChannel(listPos);
                QAnimationChannel animRot = new QAnimationChannel(listRot);
                QAnimationChannel animScale = new QAnimationChannel(listScale);
                ArrayList<KeyFrame> listMat = Lists.newArrayList();
                QModelBone joint = model.loader.findJoint(chanName);
                for (int f = 0; f < iMaxTime; f++) {
                    QModelKeyFramePosition frpos = (QModelKeyFramePosition) (listPos.isEmpty() ? null : animPos.getFrameAt(0, f));
                    QModelKeyFrameRotation frrot = (QModelKeyFrameRotation) (listRot.isEmpty() ? null : animRot.getFrameAt(0, f));
                    QModelKeyFrameScale frscale = (QModelKeyFrameScale) (listScale.isEmpty() ? null : animScale.getFrameAt(0, f));
                    Matrix4f mat = new Matrix4f();
                    mat.setIdentity();
                    if (joint != null) {
                        mat.load(joint.matRest);
                    }
                    if (frpos != null) {
                        float x = frpos.pos.x;
                        float y = frpos.pos.y;
                        float z = frpos.pos.z;
//                        System.out.println(frpos.pos);
//                        mat.scale(0.05f);
                        mat.translate(frpos.pos);
                        
                    }
                    if (frrot != null) {
                        Matrix4f rotMat = Matrix4f.convertQuaternionToMatrix4f(frrot.q);
//                        rotMat.rotate(90, 0, 0, 1);
                        mat.mulMat(rotMat);
                    }
//                    if (joint != null) {
//                        mat.load(joint.matRest);
//                    }
                    if (frscale != null) {
//                        mat.scale(frscale.scale);
                    }
                    QModelKeyFrameMatrix frMat = new QModelKeyFrameMatrix(f, f*timeScale, mat);
                    listMat.add(frMat);
                }
                QAnimationChannel animMat = new QAnimationChannel(listMat);
//                if ("CATNeck".equals(chanName))
                action.map.put(chanName, animMat);
                
            }
            
            return action;
        } catch (Exception e) {
            throw new GameError("Failed loading model "+path, e);
        }
    }


}
