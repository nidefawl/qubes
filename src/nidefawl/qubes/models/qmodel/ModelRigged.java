/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31;

import com.google.common.collect.Lists;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.GLTriBuffer;
import nidefawl.qubes.gl.VertexBuffer;
import nidefawl.qubes.models.qmodel.animation.QAnimationChannel;
import nidefawl.qubes.models.qmodel.animation.QModelAction;
import nidefawl.qubes.models.qmodel.animation.QModelKeyFrameMatrix;
import nidefawl.qubes.models.qmodel.loader.ModelLoaderQModel;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.perf.TimingHelper;
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
    public ArrayList<QModelPoseBone> poseBones;
    private QModelPoseBone head;
    private QModelPoseBone neck;
    private boolean needsDraw;

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
            if (b.restbone.name.equalsIgnoreCase("CATHead")) {
                this.head = b;
            }
            if (b.restbone.name.equalsIgnoreCase("CATNeck")) {
                this.neck = b;
            }
        }
        this.rootJoint = this.poseBones.isEmpty() ? null : this.poseBones.get(0);
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

    /**
     * @param i
     * @param f
     */
    public void animate(QModelProperties properties, float fabs, float f) {
        for (QModelPoseBone joint : this.poseBones) {
            int idx = properties.getChannelIdx(joint.restbone.name);
            QAnimationChannel anim = properties.getActionChannel(idx, joint.restbone.name);
            float offset = properties.getActionOffset(idx);
            if (anim != null && anim.setDeform(0, fabs-offset, tmpMat1)) {
                joint.matDeform.load(tmpMat1);
            } else {
                joint.matDeform.load(joint.getMatRest());
            }
            joint.updateNormalMat();
        }
    }
    /**
     * @param i
     * @param f
     */
    public void animateNodes(QModelProperties properties, float fabs, float f) {
        for (QModelNode empty : this.loader.listEmpties) {
            int idx = properties.getChannelIdx(empty.name);
            QAnimationChannel anim = properties.getActionChannel(idx, empty.name);
            float offset = properties.getActionOffset(idx);
            if (anim != null && anim.setDeform(0, fabs-offset, tmpMat1)) {
                empty.matDeform.load(this.tmpMat1);
            } else {
                empty.matDeform.load(empty.localMat);
            }
            QModelBone bone = empty.getAttachmentBone();
            if (bone != null) {
                QModelPoseBone pbone = bone.posebone;
                empty.matDeform.m31 += pbone.restbone.boneLength;
                empty.matDeform.mulMat(pbone.matDeform);
            }
            empty.updateNormalMat();
        }
    }
    Quaternion q = new Quaternion();
    public Matrix4f tmpMat1 = new Matrix4f();
    Matrix4f tmpMat2 = new Matrix4f();
	/** DEBUG METHOD! SLOW! RUN IN SHADER! 
	 * @param obj 
	 * @param tmpVec2 
	 * @return */
    public Matrix4f buildFinalPose(QModelObject obj, QModelVertex v) {
        Matrix4f m1 = tmpMat1;
        Matrix4f m2 = tmpMat2;
        m1.setZero();
        for (int j = 0; j < v.numBones; j++) {
            QModelBone bone = obj.listBones.get(v.bones[j]);
            m2.load(bone.matRestInv);
            m2.mulMat(bone.posebone.matDeform);
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
    Vector3f tmpVec2 = new Vector3f();

    public void renderRestModel(int object, int group, int instances) {
        
    }
    public void renderRestModel(QModelObject obj, QModelGroup grp, int instances) {
        ModelRenderObject rObj = this.getGroup(obj.idx);
        ModelRenderGroup rGroup = rObj.getGroup(grp.idx);
        if (rGroup.gpuBufRest == null /*|| (System.currentTimeMillis()-rGroup.reRender>1000)*/) {
            if (rGroup.gpuBufRest != null) {
                rGroup.gpuBufRest.release();
            }
            rGroup.reRender = System.currentTimeMillis();
            if (this.vbuf == null)
                this.vbuf = new VertexBuffer(1024*64);
            this.vbuf.reset();
            int vPosI = 0;
//            int[] vPos = new int[this.loader.listTri.size()*3];
//            Arrays.fill(vPos, -1);
            for (QModelTriangle triangle : grp.listTri) {
                for (int i = 0; i < 3; i++) {
                    int idx = triangle.vertIdx[i];
//                    if (vPos[idx] < 0) { // shared vertices require per vertex UVs -> requires exporter to be adjusted
                    // but also gives worse performance
//                        vPos[idx] =
//                                vPosI++;
                        QModelVertex v = obj.listVertex.get(idx);
                        vbuf.put(Float.floatToRawIntBits(v.x));
                        vbuf.put(Float.floatToRawIntBits(v.y));
                        vbuf.put(Float.floatToRawIntBits(v.z));
                        tmpVec.set(triangle.normal[i]);
                        vbuf.put(RenderUtil.packNormal(tmpVec));
                        vbuf.put(Half.fromFloat(triangle.texCoord[0][i]) << 16 | (Half.fromFloat(triangle.texCoord[1][i])));
                        int bones03 = 0;
                        int bones47 = 0;
                        for (int w = 0; w < 4; w++) {
                            int boneIdx = (0 + w) >= v.numBones ? 0xFF : v.bones[0 + w];
                            int boneIdx2 = (4 + w) >= v.numBones ? 0xFF : v.bones[4 + w];
                            bones03 |= (boneIdx) << (w * 8);
                            bones47 |= (boneIdx2) << (w * 8);
                        }
                        vbuf.put(bones03);
                        vbuf.put(bones47);
                        for (int w = 0; w < 4; w++) {
                            vbuf.put(Half.fromFloat(v.weights[w * 2 + 1]) << 16 | (Half.fromFloat(v.weights[w * 2 + 0])));
                        }
                        vbuf.increaseVert();
//                    } else {
//                        System.out.println("reuse vert");
//                    }
                    vbuf.putIdx(vPosI++);
                }
                vbuf.increaseFace();
            }
            rGroup.gpuBufRest = new GLTriBuffer(GL15.GL_DYNAMIC_DRAW);

            int bytes = rGroup.gpuBufRest.upload(vbuf);
//            System.out.println("byte size upload "+bytes+", "+rGroup.gpuBufRest.getVertexCount());
//            System.out.println(""+rGroup.gpuBufRest.getVertexCount()+" vertices, "+rGroup.gpuBufRest.getTriCount()+" tris, "+rGroup.gpuBufRest.getIdxCount()+" indexes");

        }
//        this.gpuBufRest.draw();
        Stats.modelDrawCalls++;

        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.start("render_"+this.loader.getModelName()+"_"+obj.name+"_"+grp.name);
        Engine.bindBuffer(rGroup.gpuBufRest.getVbo().getVboId());
        Engine.bindIndexBuffer(rGroup.gpuBufRest.getVboIndices().getVboId());
//        System.out.println(instances);
        GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, rGroup.gpuBufRest.getTriCount()*3, GL11.GL_UNSIGNED_INT, 0, instances);

        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.end();
    }
    private VertexBuffer vbuf;
    public void render(int object, int group, float f) {
        QModelObject obj = this.loader.listObjects.get(object);
        QModelGroup grp = obj.listGroups.get(group);
        ModelRenderObject rObj = this.getGroup(object);
        ModelRenderGroup rGroup = rObj.getGroup(group);
        this.needsDraw = true;
        if (this.needsDraw || System.currentTimeMillis()-rGroup.reRender>42200) {
            rGroup.reRender = System.currentTimeMillis();
            this.needsDraw = false;
            if (this.vbuf == null)
                this.vbuf = new VertexBuffer(1024*64);
            this.vbuf.reset();
            int vPosI = 0;
            int pos = 0;
            int[] vPos = new int[obj.listTri.size()*3];
            for (QModelTriangle triangle : grp.listTri) {
                for (int i = 0; i < 3; i++) {
                    int idx = triangle.vertIdx[i];
//                      if (vPos[idx]<0) {
                        vPos[idx] = vPosI++;
                        QModelVertex v = obj.listVertex.get(idx);
                        Matrix4f pose = buildFinalPose(obj, v);
                        if (pose != null) {
                            Matrix4f.transform(pose, v, tmpVec);
                        } else {
                            tmpVec.set(v);
                        }
                        
                        QModelBone bone = obj.getAttachmentBone();
                        if(bone != null) {
                            tmpVec.addVec(bone.posebone.getTailLocal());
                            Matrix4f.transform(bone.posebone.matDeform, tmpVec, tmpVec);
                        }
                        
                        QModelAbstractNode node = obj.getAttachementNode();
                        if(node != null) {
                            Matrix4f.transform(node.getMatDeform(), tmpVec, tmpVec);
                        }
                        
                        this.vbuf.put(Float.floatToRawIntBits(tmpVec.x));
                        this.vbuf.put(Float.floatToRawIntBits(tmpVec.y));
                        this.vbuf.put(Float.floatToRawIntBits(tmpVec.z));
                        
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
                        
                        if(bone != null) {
                            Matrix4f.transform(bone.posebone.matDeformNormal, tmpVec, tmpVec);
                            tmpVec.normalise();
                        }
                        
                        if(node != null) {
                            Matrix4f.transform(node.getMatDeformNormal(), tmpVec, tmpVec);
                            tmpVec.normalise();
                        }
                        
                        int normal = RenderUtil.packNormal(tmpVec);
                        this.vbuf.put(normal);
                        int textureHalf2 = Half.fromFloat(triangle.texCoord[0][i]) << 16 | (Half.fromFloat(triangle.texCoord[1][i]));
                        this.vbuf.put(textureHalf2);
                        this.vbuf.put(0xff999999);
//                      }
                        this.vbuf.putIdx(vPos[idx]);
                    this.vbuf.increaseVert();
                }
                this.vbuf.increaseFace();
            }
            
            
            if (rGroup.gpuBuf == null) {
                rGroup.gpuBuf = new GLTriBuffer(GL15.GL_DYNAMIC_DRAW);
            }
            int bytes = rGroup.gpuBuf.upload(this.vbuf);
//                System.out.println("byte size upload "+bytes+", "+this.gpuBuf.getVertexCount());
//                System.out.println(""+this.gpuBuf.getVertexCount()+" vertices, "+this.gpuBuf.getTriCount()+" tris, "+this.gpuBuf.getIdxCount()+" indexes");

        }
        

        rGroup.gpuBuf.draw();


    }
    /**
     * @param yaw
     * @param pitch
     */
    public void setHeadOrientation(float yaw, float pitch) {
        if (this.head == null) {
            return;
        }
        //temp variable for head position in neck local space
        Vector3f headParentLocal=this.tmpVec2;
        {
            QModelPoseBone b = this.head;
            
            //copy rest pose matrix to deform matrix
//            b.deformInterp.load(b.getMatDeform());
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
            float max = 30;
            if (hyaw < -max) {
                hyaw = -max;
            }
            if (hyaw > max) {
                hyaw = max;
            }
            float max1 = 60;
            float hpitch = pitch*0.8f;
            if (hpitch < -max1) {
                hpitch = -max1;
            }
            if (hpitch > max1) {
                hpitch = max1;
            }
//            System.out.println(yaw+"/"+hyaw);
            

            //apply rotation on top of copied rest pose
            b.matDeform.rotate(hyaw * GameMath.PI_OVER_180, 1f, 0f, 0f);
            b.matDeform.rotate(hpitch * GameMath.PI_OVER_180, 0f, 0f, 1f);
            
            
//            b.matDeform.load(b.deformInterp);
            
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
//            b.deformInterp.load(b.getMatDeform());
            //clamp max angle 
            float hyaw = (yaw-270)*-1;
            float max = 40;
            if (hyaw < -max) {
                hyaw = -max;
            }
            if (hyaw > max) {
                hyaw = max;
            }
            float max1 = 60;
            float hpitch = pitch*0.5f;
            if (hpitch < -max1) {
                hpitch = -max1;
            }
            if (hpitch > max1) {
                hpitch = max1;
            }
            //apply rotation on top of copied rest pose
//            b.matDeform.rotate(hyaw * 0.4f * GameMath.PI_OVER_180, 0f, 1f, 0f);
            b.matDeform.rotate(hyaw * GameMath.PI_OVER_180, 1f, 0f, 0f);
            b.matDeform.rotate(hpitch * GameMath.PI_OVER_180, 0f, 0f, 1f);
//            b.matDeform.load(b.deformInterp);
            
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
