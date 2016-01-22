/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31;

import com.google.common.collect.Lists;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.GLTriBuffer;
import nidefawl.qubes.gl.VertexBuffer;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vec.Quaternion;
import nidefawl.qubes.vec.Vector3f;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class ModelRigged extends ModelQModel {

    public QModelPoseBone rootJoint;
    private ArrayList<QModelPoseBone> poseBones;
    private QModelPoseBone head;
    private QModelPoseBone neck;
    private QModelAction action;
    private int numIdx;
    public GLTriBuffer gpuBufRest = null;
	/**
	 * @param qModelJoint 
	 * 
	 */
	public ModelRigged(ModelLoaderQModel loader) {
	    super(loader);
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
        this.action = loader.listActions.get(0);
        this.numIdx = loader.listTri.size()*3;
//        this.head.animate = false;
	}
	public void setAction(int idx) {
	    if (idx < 0) idx = 0;
	    if (idx >= this.loader.listActions.size()) {
	        idx = this.loader.listActions.size()-1;
	    }
        this.action = loader.listActions.get(idx);
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

    public void rest() {
        for (QModelPoseBone joint : this.poseBones) {
            joint.matDeform = joint.restbone.matRest;
        }
    }
    public void animate(int animationType, float time) {
//	    float f = 1.0f/this.loader.fps;
//	    float absTime = absTimeInSeconds / f;
        for (QModelPoseBone joint : this.poseBones) {
            QBoneAnimation anim = this.action == null ? null :this.action.map.get(joint.restbone.name);
            if (anim == null || !joint.animate) {
                joint.matDeform = joint.restbone.matRest;
                continue;
            }
            
            if (anim.frames.length > 0) {
                float f = time;
                if (animationType == 1) {
                    f = time*anim.frames.length;
                }
                QModelKeyFrameMatrix frame = (QModelKeyFrameMatrix) anim.getFrameAt(animationType, f);
                QModelKeyFrameMatrix nextframe = (QModelKeyFrameMatrix) frame.getNext();
                //TODO: if nextframe < frame
                
                float frameInterpProgress;
                if (animationType == 1) {
                    frameInterpProgress = f%1.0f;
                } else {
                    float totalLen = anim.animLength;
                    float frameLen = nextframe.time - frame.time;
                    frameInterpProgress = ((f % totalLen)- frame.time) / frameLen;
                }
                if (frameInterpProgress <= 0) {
                    joint.matDeform = frame.mat;
                } else if (frameInterpProgress >= 1 ) {
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

    public void getMatrices() {
        Matrix4f m2 = tmpMat2;
        for (int j = 0; j < this.poseBones.size(); j++) {
            QModelPoseBone jt = poseBones.get(j);
            m2.load(jt.restbone.matRestInv);
            m2.mulMat(jt.matDeform);
        }
    }
    public int storeMatrices(FloatBuffer buffer) {
        Matrix4f m2 = tmpMat2;
        for (int j = 0; j < this.poseBones.size(); j++) {
            QModelPoseBone jt = poseBones.get(j);
            m2.load(jt.restbone.matRestInv);
            m2.mulMat(jt.matDeform);
            m2.store(buffer);
        }
        return this.poseBones.size();
    }
    public void renderRestModel(int instances) {
        if (this.gpuBufRest == null) {
            this.reRender = System.currentTimeMillis();
            VertexBuffer buf = new VertexBuffer(1024*64);
            buf.reset();
            int vPosI = 0;
//            int[] vPos = new int[this.loader.listTri.size()*3];
//            Arrays.fill(vPos, -1);
            for (QModelTriangle triangle : this.loader.listTri) {
                for (int i = 0; i < 3; i++) {
                    int idx = triangle.vertIdx[i];
//                    if (vPos[idx] < 0) { // shared vertices require per vertex UVs -> requires exporter to be adjusted
                    // but also gives worse performance
//                        vPos[idx] =
//                                vPosI++;
                        QModelVertex v = this.loader.getVertex(idx);
                        buf.put(Float.floatToRawIntBits(v.x));
                        buf.put(Float.floatToRawIntBits(v.y));
                        buf.put(Float.floatToRawIntBits(v.z));
                        tmpVec.set(triangle.normal[i]);
                        buf.put(RenderUtil.packNormal(tmpVec));
                        buf.put(Half.fromFloat(triangle.texCoord[0][i]) << 16 | (Half.fromFloat(triangle.texCoord[1][i])));
                        int bones03 = 0;
                        int bones47 = 0;
                        for (int w = 0; w < 4; w++) {
                            int boneIdx = (0 + w) >= v.numBones ? 0xFF : v.bones[0 + w];
                            int boneIdx2 = (4 + w) >= v.numBones ? 0xFF : v.bones[4 + w];
                            bones03 |= (boneIdx) << (w * 8);
                            bones47 |= (boneIdx2) << (w * 8);
                        }
                        buf.put(bones03);
                        buf.put(bones47);
                        for (int w = 0; w < 4; w++) {
                            buf.put(Half.fromFloat(v.weights[w * 2 + 1]) << 16 | (Half.fromFloat(v.weights[w * 2 + 0])));
                        }
                        buf.increaseVert();
//                    } else {
//                        System.out.println("reuse vert");
//                    }
                    buf.putIdx(vPosI++);
                }
                buf.increaseFace();
            }
            this.gpuBufRest = new GLTriBuffer(GL15.GL_DYNAMIC_DRAW);

            int bytes = this.gpuBufRest.upload(buf);
            System.out.println("byte size upload "+bytes+", "+this.gpuBufRest.getVertexCount());
            System.out.println(""+this.gpuBufRest.getVertexCount()+" vertices, "+this.gpuBufRest.getTriCount()+" tris, "+this.gpuBufRest.getIdxCount()+" indexes");

        }
//        this.gpuBufRest.draw();
        Stats.modelDrawCalls++;
        
        Engine.bindBuffer(this.gpuBufRest.getVbo().getVboId());
        Engine.bindIndexBuffer(this.gpuBufRest.getVboIndices().getVboId());
        GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, this.gpuBufRest.getTriCount()*3, GL11.GL_UNSIGNED_INT, 0, instances);
    }
    public void render(float f) {
        this.needsDraw = true;
        if (this.needsDraw || System.currentTimeMillis()-this.reRender>42200) {
            this.reRender = System.currentTimeMillis();
            this.needsDraw = false;
            if (buf == null)
                buf = new VertexBuffer(1024*64);
            this.buf.reset();
            int vPosI = 0;
            int pos = 0;
            int[] vPos = new int[this.loader.listTri.size()*3];
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
                        int normal = RenderUtil.packNormal(tmpVec);
                        buf.put(normal);
                        int textureHalf2 = Half.fromFloat(triangle.texCoord[0][i]) << 16 | (Half.fromFloat(triangle.texCoord[1][i]));
                        buf.put(textureHalf2);
                        buf.put(0xff999999);
//                  }
                    buf.putIdx(vPos[idx]);
                    buf.increaseVert();
                }
                buf.increaseFace();
            }
            
            
            if (this.gpuBuf == null) {
                this.gpuBuf = new GLTriBuffer(GL15.GL_DYNAMIC_DRAW);
            }
            this.gpuBuf.upload(buf);
        }
        

        this.gpuBuf.draw();


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
    @Override
    public QModelType getType() {
        return QModelType.RIGGED;
    }
}
