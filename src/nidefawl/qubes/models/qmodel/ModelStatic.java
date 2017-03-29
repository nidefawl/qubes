/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import java.util.Arrays;
import java.util.List;

import org.lwjgl.opengl.GL15;

import nidefawl.qubes.gl.GLTriBuffer;
import nidefawl.qubes.gl.VertexBuffer;
import nidefawl.qubes.models.qmodel.ModelQModel.ModelRenderGroup;
import nidefawl.qubes.models.qmodel.ModelQModel.ModelRenderObject;
import nidefawl.qubes.models.qmodel.loader.ModelLoaderQModel;
import nidefawl.qubes.util.Half;
import nidefawl.qubes.util.RenderUtil;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class ModelStatic extends ModelQModel {

    public ModelStatic(ModelLoaderQModel loader) {
        super(loader);
    }
    public void render(int object, int group, float f) {
        ModelRenderObject rObj = this.getGroup(object);
        if (rObj == null) {
            return;
        }
        ModelRenderGroup rGroup = rObj.getGroup(group);
        if (rGroup.gpuBuf == null) {
            System.err.println("Model is not pre-drawn "+this.getName());
            return;
        }
        rGroup.gpuBuf.draw();
    }
    
    @Override
    public void draw() {
        VertexBuffer vbuf = new VertexBuffer(1024*64);
        for (QModelObject obj : this.loader.listObjects) {
            ModelRenderObject rObj = this.getGroup(obj.idx);
            for (QModelGroup grp : obj.listGroups) {
                ModelRenderGroup rGroup = rObj.getGroup(grp.idx);
                vbuf.reset();
                List<QModelTriangle> triList = obj.listTri; 
                List<QModelVertex> vList = obj.listVertex; 
                int numIdx = triList.size()*3;
                int[] vPos = new int[vList.size()];
                int vPosI = 0;
                Arrays.fill(vPos, -1);
                int pos = 0;
                for (QModelTriangle triangle : triList) {
                    for (int i = 0; i < 3; i++) {
                        int idx = triangle.vertIdx[i];
    //                  if (vPos[idx]<0) {
                            vPos[idx] = vPosI++;
                            QModelVertex v = obj.listVertex.get(idx);
                            vbuf.put(Float.floatToRawIntBits(v.x));
                            vbuf.put(Float.floatToRawIntBits(v.y));
                            vbuf.put(Float.floatToRawIntBits(v.z));
                            int normal = RenderUtil.packNormal(triangle.normal[i]);
                            vbuf.put(normal);
                            int textureHalf2 = Half.fromFloat(triangle.texCoord[0][i]) << 16 | (Half.fromFloat(triangle.texCoord[1][i]));
                            vbuf.put(textureHalf2);
                            vbuf.put(0xffffffff);
                            vbuf.increaseVert();
    //                  }
    //                    idxArr[pos++] = vPos[idx];
                        vbuf.putIdx(vPos[idx]);
                    }
                }
                
                
                if (rGroup.gpuBuf == null) {
                    rGroup.gpuBuf = new GLTriBuffer(true);
                }
                rGroup.gpuBuf.upload(vbuf);
            }
        }
        for (QModelObject obj : this.loader.listObjects) {
            ModelRenderObject rObj = this.getGroup(obj.idx);
            for (QModelGroup grp : obj.listGroups) {
                ModelRenderGroup rGroup = rObj.getGroup(grp.idx);
                if (rGroup.gpuBufRest == null /*|| (System.currentTimeMillis()-rGroup.reRender>1000)*/) {
                    if (rGroup.gpuBufRest != null) {
                        rGroup.gpuBufRest.release();
                    }
                    vbuf.reset();
                    int vPosI = 0;
//                    int[] vPos = new int[this.loader.listTri.size()*3];
//                    Arrays.fill(vPos, -1);
                    for (QModelTriangle triangle : grp.listTri) {
                        for (int i = 0; i < 3; i++) {
                            int idx = triangle.vertIdx[i];
//                            if (vPos[idx] < 0) { // shared vertices require per vertex UVs -> requires exporter to be adjusted
                            // but also gives worse performance
//                                vPos[idx] =
//                                        vPosI++;
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
//                            } else {
//                                System.out.println("reuse vert");
//                            }
                            vbuf.putIdx(vPosI++);
                        }
                    }
                    rGroup.gpuBufRest = new GLTriBuffer(false);

                    int bytes = rGroup.gpuBufRest.upload(vbuf);

                }
            }
        }

    }

    @Override
    public QModelType getType() {
        return QModelType.STATIC;
    }
}
