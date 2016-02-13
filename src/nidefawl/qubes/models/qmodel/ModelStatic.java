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
import nidefawl.qubes.vec.Vector3f;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class ModelStatic extends ModelQModel {
    public boolean needsDraw = true;

    public ModelStatic(ModelLoaderQModel loader) {
        super(loader);
    }

    Vector3f tmpVec = new Vector3f();
    private VertexBuffer vBuf;
    public void render(int object, int group, float f) {
        QModelObject obj = this.loader.listObjects.get(object);
        QModelGroup grp = obj.listGroups.get(group);
        ModelRenderObject rObj = this.getGroup(object);
        ModelRenderGroup rGroup = rObj.getGroup(group);
        if (this.needsDraw || System.currentTimeMillis()-rGroup.reRender>2100) {
            rGroup.reRender = System.currentTimeMillis();
            this.needsDraw = false;
            if (this.vBuf == null)
                this.vBuf = new VertexBuffer(1024*64);
            this.vBuf.reset();
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
                        this.vBuf.put(Float.floatToRawIntBits(v.x));
                        this.vBuf.put(Float.floatToRawIntBits(v.y));
                        this.vBuf.put(Float.floatToRawIntBits(v.z));
                        int normal = RenderUtil.packNormal(triangle.normal[i]);
                        this.vBuf.put(normal);
                        int textureHalf2 = Half.fromFloat(triangle.texCoord[0][i]) << 16 | (Half.fromFloat(triangle.texCoord[1][i]));
                        this.vBuf.put(textureHalf2);
                        this.vBuf.put(0xffffffff);
//                  }
//                    idxArr[pos++] = vPos[idx];
                    this.vBuf.putIdx(vPos[idx]);
                    this.vBuf.increaseVert();
                }
                this.vBuf.increaseFace();
            }
            
            
            if (rGroup.gpuBuf == null) {
                rGroup.gpuBuf = new GLTriBuffer(GL15.GL_DYNAMIC_DRAW);
            }
            rGroup.gpuBuf.upload(vBuf);
        }
        

        rGroup.gpuBuf.draw();


    }

    @Override
    public QModelType getType() {
        return QModelType.STATIC;
    }
}
