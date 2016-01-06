package nidefawl.qubes.models.qmodel;

import java.util.Arrays;
import java.util.List;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.GLTriBuffer;
import nidefawl.qubes.gl.VertexBuffer;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.RenderUtil;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.vec.Vector3f;

public class ModelBlock extends ModelQModel {
    public final QModelTriGroup[] groups = new QModelTriGroup[6];
    public ModelBlock(ModelLoaderQModel loader) {
        super(loader);
        for (int i = 0; i < 6; i++) {
            int axisSwap = i;
            if (i == Dir.DIR_NEG_Z) {
                axisSwap = Dir.DIR_NEG_Y;
            }
            if (i == Dir.DIR_NEG_Y) {
                axisSwap = Dir.DIR_NEG_Z;
            }
            if (i == Dir.DIR_POS_Z) {
                axisSwap = Dir.DIR_POS_Y;
            }
            if (i == Dir.DIR_POS_Y) {
                axisSwap = Dir.DIR_POS_Z;
            }
            if (i == Dir.DIR_POS_X) {
                axisSwap = Dir.DIR_NEG_X;
            }
            if (i == Dir.DIR_NEG_X) {
                axisSwap = Dir.DIR_POS_X;
            }
            QModelTriGroup group = this.loader.getGroup(Dir.asString(axisSwap));
            if (group == null) {
                throw new GameError(this.loader.getModelName()+": Invalid block model, group "+Dir.asString(i)+ " missing");
            }
            this.groups[i] = group;
        }
    }

    @Override
    public QModelType getType() {
        return QModelType.BLOCK;
    }


    Vector3f tmpVec = new Vector3f();
    public void render(float f) {
        if (this.needsDraw || System.currentTimeMillis()-this.reRender>1) {
            int side = (Game.ticksran/20)%6;
            this.reRender = System.currentTimeMillis();
            this.needsDraw = false;
            if (buf == null)
                buf = new VertexBuffer(1024*64);
            this.buf.reset();
            List<QModelTriangle> triList = this.loader.listTri; 
            List<QModelVertex> vList = this.loader.listVertex; 
            int numIdx = triList.size()*3;
            int[] idxArr = new int[numIdx];
            int[] vPos = new int[vList.size()];
            int vPosI = 0;
            Arrays.fill(vPos, -1);
            int pos = 0;
            QModelTriGroup group = this.loader.listGroups.get(side);
            for (QModelTriangle triangle : group.listTri) {
                if (triangle.group != side)
                    continue;
                for (int i = 0; i < 3; i++) {
                    int idx = triangle.vertIdx[i];
//                      if (vPos[idx]<0) {
                        vPos[idx] = vPosI++;
                        QModelVertex v = this.loader.getVertex(idx);
                        buf.put(Float.floatToRawIntBits(v.x));
                        buf.put(Float.floatToRawIntBits(v.y));
                        buf.put(Float.floatToRawIntBits(v.z));
                        int normal = RenderUtil.packNormal(triangle.normal[i]);
                        buf.put(normal);
                        int textureHalf2 = RenderUtil.packTexCoord(triangle.texCoord[0][i], triangle.texCoord[1][i]);
                        buf.put(textureHalf2);
                        buf.put(0xffffffff);
//                      }
                    idxArr[pos++] = vPos[idx];
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
}
