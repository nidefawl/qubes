/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import java.util.Arrays;
import java.util.List;

import org.lwjgl.opengl.GL15;

import nidefawl.qubes.gl.GLTriBuffer;
import nidefawl.qubes.gl.VertexBuffer;
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
        QModelObject obj = this.loader.listObjects.get(object);
        QModelGroup grp = obj.listGroups.get(group);
        ModelRenderObject rObj = this.getGroup(object);
        ModelRenderGroup rGroup = rObj.getGroup(group);
        if (this.needsDraw || System.currentTimeMillis()-rGroup.reRender>2100) {
            rGroup.reRender = System.currentTimeMillis();
            this.needsDraw = false;
            if (this.vbuf == null)
                this.vbuf = new VertexBuffer(1024*64);
            this.vbuf.reset();
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
                        this.vbuf.put(Float.floatToRawIntBits(v.x));
                        this.vbuf.put(Float.floatToRawIntBits(v.y));
                        this.vbuf.put(Float.floatToRawIntBits(v.z));
                        int normal = RenderUtil.packNormal(triangle.normal[i]);
                        this.vbuf.put(normal);
                        int textureHalf2 = Half.fromFloat(triangle.texCoord[0][i]) << 16 | (Half.fromFloat(triangle.texCoord[1][i]));
                        this.vbuf.put(textureHalf2);
                        this.vbuf.put(0xffffffff);
                        this.vbuf.increaseVert();
//                  }
//                    idxArr[pos++] = vPos[idx];
                    this.vbuf.putIdx(vPos[idx]);
                }
            }
            
            
            if (rGroup.gpuBuf == null) {
                rGroup.gpuBuf = new GLTriBuffer(GL15.GL_DYNAMIC_DRAW);
            }
            rGroup.gpuBuf.upload(vbuf);
        }
        

        rGroup.gpuBuf.draw();


    }

    @Override
    public QModelType getType() {
        return QModelType.STATIC;
    }
}
