/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;
import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
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
    public QModelPoseBone rootJoint;
    private ArrayList<QModelPoseBone> poseBones;
    private QModelPoseBone head;
    private QModelPoseBone neck;
	/**
	 * @param qModelJoint 
	 * 
	 */
	public ModelQModel(ModelLoaderQModel loader) {
	    this.loader = loader;
	    this.poseBones = Lists.newArrayList();
        for (QModelBone b : loader.listBones) {
            this.poseBones.add(new QModelPoseBone(b));
        }
        for (QModelPoseBone b : poseBones) {
            QModelPoseBone parent = getPoseBone(b.restbone.parent);
            b.parent = parent;
            if (b.parent != null) {
                b.parent.addChild(b);
            }
            if (b.restbone.name.equalsIgnoreCase("head")) {
                this.head = b;
            }
            if (b.restbone.name.equalsIgnoreCase("neck")) {
                this.neck = b;
            }
        }
        this.rootJoint = this.poseBones.get(0);
//        this.head.animate = false;
	}

	/**
     * @param restbone
     * @return
     */
    private QModelPoseBone getPoseBone(QModelBone restbone) {
        for (QModelPoseBone b : poseBones) {
            if (b.restbone == restbone)
                return b;
        }
        return null;
    }

    public void animate(float fTime) {
	    float f = 1.0f/this.loader.fps;
	    float absTimeInSeconds = ((GameBase.ticksran+fTime)/GameBase.TICKS_PER_SEC);
	    float absTime = absTimeInSeconds / f;
        for (QModelPoseBone joint : this.poseBones) {
            if (!joint.animate) {
                joint.matDeform = joint.restbone.matRest;
                continue;
            }
            QBoneAnimation jt = joint.getAnimation();
            if (jt.frames[0].length > 0) {
                float totalLen = jt.animLength[0];
                QModelKeyFrameMatrix frame = (QModelKeyFrameMatrix) jt.getFrameAt(0, absTimeInSeconds);
                QModelKeyFrameMatrix nextframe = (QModelKeyFrameMatrix) frame.getNext();
                //TODO: if nextframe < frame
                float frameLen = nextframe.time - frame.time;
                float frameInterpProgress = ((absTimeInSeconds % totalLen)- frame.time) / frameLen;
                if (frameInterpProgress < 0) {
                    joint.matDeform = frame.mat;
                }
                else if (frameInterpProgress > 1 ) {

                    joint.matDeform = nextframe.mat;
                } else {
                    joint.interpolateFrame(frame.mat, nextframe.mat, frameInterpProgress);
                }

            } else {
                joint.matDeform = joint.restbone.matRest;
            }
            this.needsDraw = true; //DONT! (DEBUG)
        }
        
	}
    Quaternion q = new Quaternion();
    Matrix4f tmpMat1 = new Matrix4f();
    Matrix4f tmpMat2 = new Matrix4f();
	/** DEBUG METHOD! SLOW! RUN IN SHADER! 
	 * @param tmpVec2 
	 * @return */
    public Matrix4f buildFinalPose(QModelVertex v) {
        Matrix4f m1 = tmpMat1;
        Matrix4f m2 = tmpMat2;
        m1.setZero();
        for (int j = 0; j < v.numBones; j++) {
            QModelPoseBone jt = poseBones.get(v.bones[j]);
            m2.load(jt.restbone.matRestInv);
            m2.mulMat(jt.matDeform);
            m1.addWeighted(m2, v.weights[j]);
        }
        return v.numBones > 0 ? m1 : null;
    }
//    public void transform(QModelVertex v, Vector3f out, Vector3f tmpVec2) {
////        out.set(Vector3f.ZERO);
////        Matrix4f m1 = tmpMat1;
////        Matrix4f m2 = tmpMat2;
////        m1.setZero();
////        for (int j = 0; j < v.numBones; j++) {
////            QModelPoseBone jt = poseBones.get(v.bones[j]);
////            m2.load(jt.restbone.matRestInv);
////            m2.mulMat(jt.matDeform);
////            m1.addWeighted(m2, v.weights[j]);
////        }
////        if (v.numBones>0)
////            return m1;
////        return null;
////    }
   
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
//                  if (vPos[idx]<0) {
                        vPos[idx] = vPosI++;
                        QModelVertex v = this.loader.getVertex(idx);
                        Matrix4f pose = buildFinalPose(v);
                        if (pose != null) {
                            Matrix4f.transform(pose, v, tmpVec);
                        } else {
                            tmpVec.set(v);
                        }
                        buf.put(Float.floatToRawIntBits(tmpVec.x));
                        buf.put(Float.floatToRawIntBits(tmpVec.y));
                        buf.put(Float.floatToRawIntBits(tmpVec.z));
                        if (pose != null) {
                            pose.m30=0;
                            pose.m31=0;
                            pose.m32=0;
                            pose.m33=1;
                            pose.m03=0;
                            pose.m13=0;
                            pose.m23=0;
                            Matrix4f.transform(pose, triangle.normal[i], tmpVec);
                            tmpVec.normalise();
                        } else {
                            tmpVec.set(triangle.normal[i]);
                        }
                        int normal = packNormal(tmpVec);
                        buf.put(normal);
                        int textureHalf2 = Half.fromFloat(triangle.texCoord[0][i]) << 16 | (Half.fromFloat(triangle.texCoord[1][i]));
                        buf.put(textureHalf2);
                        buf.put(0xff999999);
//                  }
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
//        if (l2.listBones.size() != this.loader.listBones.size()) {
//            throw new GameError("Joints do not match");
//        }
        for (QModelBone jt : l2.listBones) {
            QModelBone existingJt = this.loader.findJoint(jt.name);
            if (existingJt == null) {
//                throw new GameError("Joints do not match");
                continue;
            }
            existingJt.animation = jt.animation;
        }
        
    }

    /**
     * @param yaw
     * @param pitch
     */
    public void setHeadOrientation(float yaw, float pitch) {
        //temp variable for head position in neck local space
        Vector3f headParentLocal=new Vector3f();
        {
            QModelPoseBone b = this.head;
            
            //copy rest pose matrix to deform matrix
            b.deformInterp.load(b.getMatDeform());
            // alternative:
            // should allow clamping of angles when not using restpose but already animated pose
//            b.getMatRest().toEuler(tmpVec);
//          tmpVec.x+=hyaw * GameMath.PI_OVER_180
//          tmpVec.y+=-pitch * GameMath.PI_OVER_180
//            if (tmpVec.x < -maxAngle || tmpVec.x > maxAngle ......)
//              clamp
//            b.deformInterp.setIdentity();
//            
//            b.deformInterp.rotate(tmpVec.z, 0f, 0f, 1f);
//            b.deformInterp.rotate(tmpVec.y, 0f, 1f, 0f);
//            b.deformInterp.rotate(tmpVec.x, 1f, 0f, 0f);
            //
            
            //clamp max angle 
            float hyaw = (yaw-270)*-1;
            float max = 60;
            if (hyaw < -max) {
                hyaw = -max;
            }
            if (hyaw > max) {
                hyaw = max;
            }
//            System.out.println(yaw+"/"+hyaw);
            

            //apply rotation on top of copied rest pose
            b.deformInterp.rotate(hyaw * GameMath.PI_OVER_180, 0f, 1f, 0f);
            b.deformInterp.rotate(-pitch * GameMath.PI_OVER_180, 1f, 0f, 0f);
            
            
            b.matDeform = b.deformInterp;
            
            //keep the x,y,z translation from restpose
//            b.matDeform.m30 = b.restbone.matRest.m30;
//            b.matDeform.m31 = b.restbone.matRest.m31;
//            b.matDeform.m32 = b.restbone.matRest.m32;

            //calculate joint-root-position of the head-joint and transform it into neck joint space
            tmpVec.set(0,0,0);
            tmpMat1.load(b.restbone.matRest);
            tmpMat1.mulMat(b.parent.restbone.matRestInv);
            Matrix4f.transform(tmpMat1, tmpVec, headParentLocal);
        }
        {
            QModelPoseBone b = this.neck;
            //copy rest pose matrix to deform matrix
            b.deformInterp.load(b.getMatDeform());
            //clamp max angle 
            float hyaw = (yaw);
            float max = 60;
            if (hyaw < -max) {
                hyaw = -max;
            }
            if (hyaw > max) {
                hyaw = max;
            }
            //apply rotation on top of copied rest pose
            b.deformInterp.rotate(hyaw * 0.4f * GameMath.PI_OVER_180, 0f, 1f, 0f);
            b.deformInterp.rotate(-pitch * 0.4f * GameMath.PI_OVER_180, 1f, 0f, 0f);
            b.matDeform = b.deformInterp;
            
            //keep the x,y,z translation from restpose
//            b.matDeform.m30 = b.restbone.matRest.m30;
//            b.matDeform.m31 = b.restbone.matRest.m31;
//            b.matDeform.m32 = b.restbone.matRest.m32;
            

            //calculate the new joint-root-position of the head-joint with the new neck matrix
            Matrix4f.transform(b.matDeform, headParentLocal, tmpVec);
            
            //copy the new head position back to the head-joint pose matrix
            b = this.head;
            b.matDeform.m30 = tmpVec.x;
            b.matDeform.m31 = tmpVec.y;
            b.matDeform.m32 = tmpVec.z;
        }
        this.neck.animate = true;
        this.head.animate = true;
    }
}
