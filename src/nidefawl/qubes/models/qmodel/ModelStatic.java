/**
 * 
 */
package nidefawl.qubes.models.qmodel;

import java.util.Arrays;
import java.util.List;

import nidefawl.qubes.gl.GLTriBuffer;
import nidefawl.qubes.gl.VertexBuffer;
import nidefawl.qubes.util.Half;
import nidefawl.qubes.util.RenderUtil;
import nidefawl.qubes.vec.Vector3f;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class ModelStatic extends ModelQModel {

    public ModelStatic(ModelLoaderQModel loader) {
        super(loader);
    }

    Vector3f tmpVec = new Vector3f();
    public void render(float f) {
        if (this.needsDraw || System.currentTimeMillis()-this.reRender>2100) {
            this.reRender = System.currentTimeMillis();
            this.needsDraw = false;
            if (buf == null)
                buf = new VertexBuffer(1024*64);
            this.buf.reset();
            List<QModelTriangle> triList = this.loader.listTri; 
            List<QModelVertex> vList = this.loader.listVertex; 
            int numIdx = triList.size()*3;
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
                        buf.put(Float.floatToRawIntBits(v.x));
                        buf.put(Float.floatToRawIntBits(v.y));
                        buf.put(Float.floatToRawIntBits(v.z));
                        int normal = RenderUtil.packNormal(triangle.normal[i]);
                        buf.put(normal);
                        int textureHalf2 = Half.fromFloat(triangle.texCoord[0][i]) << 16 | (Half.fromFloat(triangle.texCoord[1][i]));
                        buf.put(textureHalf2);
                        buf.put(0xffffffff);
//                  }
//                    idxArr[pos++] = vPos[idx];
                    buf.putIdx(vPos[idx]);
                    buf.increaseVert();
                }
                buf.increaseFace();
            }
            
            
            if (this.gpuBuf == null) {
                this.gpuBuf = new GLTriBuffer();
            }
            this.gpuBuf.upload(buf);
        }
        

        this.gpuBuf.draw();


    }
    @Override
    public QModelType getType() {
        return QModelType.STATIC;
    }
}
