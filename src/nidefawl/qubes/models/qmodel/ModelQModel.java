/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import static nidefawl.qubes.meshing.BlockFaceAttr.BLOCK_VERT_INT_SIZE;

import java.util.Arrays;
import java.util.List;

import org.lwjgl.opengl.*;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.GLTriBuffer;
import nidefawl.qubes.gl.VertexBuffer;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.Half;
import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vec.Quaternion;
import nidefawl.qubes.vec.Vector3f;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class ModelQModel {

	public ModelLoaderQModel loader;
    public VertexBuffer buf = new VertexBuffer(1024*64);
//    public VertexBuffer shadowBuf = new VertexBuffer(1024*16);
    public GLTriBuffer gpuBuf = null;
//    public GLTriBuffer gpuShadowBuf = null;
    public boolean needsDraw = true;
    public long reRender=0;
    public QModelJoint rootJoint;
    private int lastFrame;
	/**
	 * @param qModelJoint 
	 * 
	 */
	public ModelQModel(QModelJoint qModelJoint) {
	    this.rootJoint = qModelJoint;
	}

    int frameTicks1;
    int frameTicks2;
    long lastPrint = System.currentTimeMillis();
    Vector3f translate = new Vector3f();
    Quaternion rot = new Quaternion();
    Matrix4f tmpAnimMat = new Matrix4f();
	public void animate(float fTime) {
	    float f = 1.0f/this.loader.fps;
	    float absTimeInSeconds = ((Game.ticksran+fTime)/(float)Game.TICKS_PER_SEC);
	    float absTime = absTimeInSeconds / f;
	    int curFrame = GameMath.floor(absTime);
	    if (curFrame != lastFrame) {
	        frameTicks1++;
	        lastFrame = curFrame; 
	    }
//	    if (System.currentTimeMillis()-lastPrint>1000) {
//	        lastPrint = System.currentTimeMillis();
//	        System.out.println(fProgress);
//	        frameTicks1 = 0;
//	    }
	    int frameIDx = 5;
        for (QModelJoint joint : this.loader.listJoints) {
            tmpAnimMat.setIdentity();
            QJointAnimation jt = joint.animation;
            if (jt.frames[0].length > 0) {
                float totalLen = jt.animLength[0];
                QModelKeyframeRot frame = (QModelKeyframeRot) jt.getFrameAt(0, absTimeInSeconds);
                QModelKeyframeRot nextframe = (QModelKeyframeRot) frame.getNext();
                //TODO: if nextframe < frame
                float frameLen = nextframe.time - frame.time;
                float frameInterpProgress = ((absTimeInSeconds % totalLen)- frame.time) / frameLen;
                if (frameInterpProgress < 0) frameInterpProgress = 0;
                if (frameInterpProgress > 1 ) frameInterpProgress = 1;

                //interpolate distance with nlerp
                Quaternion quatCur = frame.param;
                Quaternion quatNext = nextframe.param;
//                rot.set(quatCur);
                rot.set(quatNext);
                rot.sub(quatCur);
                rot.scale(frameInterpProgress);
                rot.add(quatCur);
                rot.normalise(rot);
                tmpAnimMat.setIdentity();
                if (joint.parent != null) {
                    tmpAnimMat.setFromQuat(rot.x, rot.y, rot.z, rot.w);
                }
            }
            if (jt.frames[1].length > 0) {
                float totalLen = jt.animLength[1];
//                QModelKeyframeTrans frame = (QModelKeyframeTrans) jt.getFrameAt(1, absTimeInSeconds);
                QModelKeyframeTrans frame = (QModelKeyframeTrans) jt.getFrameAt(1, absTimeInSeconds);
                QModelKeyframeTrans nextframe = (QModelKeyframeTrans) frame.getNext();
//              System.out.println(curFrame+"/"+frame.idx+" - "+frame.hashCode()+"/"+frame.getType()+"/"+frame.time);
              //TODO: if nextframe < frame
              float frameLen = nextframe.time - frame.time;
              float frameInterpProgress = ((absTimeInSeconds % totalLen)- frame.time) / frameLen;
//            System.out.println(frameInterpProgress);
              translate.set(nextframe.param);
              translate.subtract(frame.param);
              translate.scale(frameInterpProgress);
              translate.addVec(frame.param);
              tmpAnimMat.translate(translate);
            }
            if (joint.parent != null) {
                joint.matFinal.load(tmpAnimMat);
                joint.matFinal.mulMat(joint.matLocal);
                joint.matFinal.mulMat(joint.parent.matFinal);
            }else {

                joint.matFinal.load(tmpAnimMat);
                joint.matFinal.mulMat(joint.matLocal);
            }
//          System.out.println(joint.matLocal.m30);
            this.needsDraw = true; //DONT! (DEBUG)
        }
	}
    Matrix4f tmpMat = new Matrix4f();
	/** DEBUG METHOD! SLOW! RUN IN SHADER! */
    public Matrix4f getWeightedMat(QModelVertex v) {
        tmpMat.setZero();
        float total = 0;
        for (int j = 0; j < v.numBones; j++) {
            QModelJoint jt = loader.listJoints.get(v.bones[j]);
            float left = 1.0f - total;
            if (left >= v.weights[j]) {
                total += v.weights[j];
                tmpMat.addWeighted(jt.matFinal, v.weights[j]);
            } else {
                tmpMat.addWeighted(jt.matFinal, left);
                total = 1;
            }
        }
//        float left = 1.0f - total;
//        if (left > 0) {
//            tmpMat.addWeighted(loader.listJoints.get(0).matFinal, left); //????????????????????????
//        }
        if (v.numBones == 0)
            tmpMat.setIdentity();
        return tmpMat;
    }
	Vector3f tmpVec = new Vector3f();
    public void render(float f) {
        this.needsDraw = true;
        if (this.needsDraw || System.currentTimeMillis()-this.reRender>42200) {
            this.reRender = System.currentTimeMillis();
            this.needsDraw = false;
            this.buf.reset();
            List<QModelTriangle> triList = this.loader.listTri; 
            List<QModelVertex> vList = this.loader.listVertex; 
            int numIdx = triList.size()*3;
            int[] idxArr = new int[numIdx];
            int[] vPos = new int[vList.size()];
            int vPosI = 0;
            Arrays.fill(vPos, -1);
            int pos = 0;
            for (QModelTriangle triangle : this.loader.listTri) {
            	for (int i = 0; i < 3; i++) {
            		int idx = triangle.vertIdx[i];
//            		if (vPos[idx]<0) {
            			vPos[idx] = vPosI++;
            			QModelVertex v = this.loader.getVertex(idx);
                        Matrix4f mat = getWeightedMat(v);
                        tmpVec.set(v);
                        Matrix4f.transform(mat, v, tmpVec);
        				buf.put(Float.floatToRawIntBits(tmpVec.x));
        				buf.put(Float.floatToRawIntBits(tmpVec.y));
        				buf.put(Float.floatToRawIntBits(tmpVec.z));
        				int normal = packNormal(triangle.normal[i]);
        				buf.put(normal);
        	            int textureHalf2 = Half.fromFloat(triangle.texCoord[0][i]) << 16 | (Half.fromFloat(triangle.texCoord[1][i]));
        				buf.put(textureHalf2);
        				buf.put(0xff999999);
//            		}
					idxArr[pos++] = vPos[idx];
    	            buf.increaseVert();
            	}
            	buf.increaseFace();
        	}
            
            
            if (this.gpuBuf == null) {
                this.gpuBuf = new GLTriBuffer();
            }
            this.gpuBuf.upload(buf, idxArr);
        }
        

        this.gpuBuf.bind();
        this.gpuBuf.draw(5);


    }
    /**
	 * @param vector3f
	 * @return
	 */
	private int packNormal(Vector3f v) {
        byte byte0 = (byte)(int)(v.x * 127F);
        byte byte1 = (byte)(int)(v.y * 127F);
        byte byte2 = (byte)(int)(v.z * 127F);
        int normal = byte0 & 0xff | (byte1 & 0xff) << 8 | (byte2 & 0xff) << 16;
		return normal;
	}


	public void release() {
        if (this.gpuBuf != null) {
            this.gpuBuf.release();
            this.gpuBuf = null;
//            this.gpuShadowBuf.release();
//            this.gpuShadowBuf = null;
            this.buf = null;
//            this.shadowBuf = null;
        }
    }
    /**
     * @param string
     * @param l2
     */
    public void addAnimation(String string, ModelLoaderQModel l2) {
        if (l2.listJoints.size() != this.loader.listJoints.size()) {
            throw new GameError("Joints do not match");
        }
        for (QModelJoint jt : l2.listJoints) {
            QModelJoint existingJt = this.loader.findJoint(jt.name);
            if (existingJt == null) {
                throw new GameError("Joints do not match");
            }
            existingJt.animation = jt.animation;
            existingJt.rotation = jt.rotation;
            existingJt.position = jt.position;
            existingJt.matAbs = jt.matAbs;
            existingJt.matLocal = jt.matLocal;
            existingJt.matFinal = jt.matFinal;
            existingJt.matFinal2 = jt.matFinal2;
//            System.out.println(jt.animation.frames[0].length);
        }
        
    }
}
