package nidefawl.qubes.models.qmodel;

import java.util.Arrays;
import java.util.List;

import org.lwjgl.opengl.GL15;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.GLTriBuffer;
import nidefawl.qubes.gl.VertexBuffer;
import nidefawl.qubes.models.qmodel.ModelQModel.ModelRenderGroup;
import nidefawl.qubes.models.qmodel.ModelQModel.ModelRenderObject;
import nidefawl.qubes.models.qmodel.loader.ModelLoaderQModel;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.RenderUtil;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.vec.Vector3f;

public class ModelBlock extends ModelQModel {
    public final QModelGroup[] faceGroups = new QModelGroup[6];
    public VertexBuffer vBuf;
    public boolean needsDraw = true;
    public long reRender=0;
    public ModelBlock(ModelLoaderQModel loader) {
        super(loader);
        QModelObject obj = loader.listObjects.get(0);
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
            QModelGroup g = null;
            for (int j = 0; j < obj.listGroups.size(); j++) {
                QModelGroup group = obj.listGroups.get(j);
                if (group.material.name.equals(Dir.asString(axisSwap))) {
                    g = group;
                    break;
                }
            }
            if (g == null) {
                throw new GameError(this.loader.getModelName()+": Invalid block model, group "+Dir.asString(i)+ " missing");
            }
            this.faceGroups[i] = g;
        }
    }

    @Override
    public QModelType getType() {
        return QModelType.BLOCK;
    }


    Vector3f tmpVec = new Vector3f();
    public void render(int object, int group, float f) {
        QModelObject obj = this.loader.listObjects.get(object);
        QModelGroup grp = obj.listGroups.get(group);
        ModelRenderObject rObj = this.getGroup(object);
        ModelRenderGroup rGroup = rObj.getGroup(group);
        if (this.needsDraw || System.currentTimeMillis()-this.reRender>1) {
            this.reRender = System.currentTimeMillis();
            this.needsDraw = false;
            if (this.vBuf == null)
                this.vBuf = new VertexBuffer(1024*64);
            this.vBuf.reset();
            List<QModelTriangle> triList = obj.listTri; 
            List<QModelVertex> vList = obj.listVertex; 
            int numIdx = triList.size()*3;
            int[] idxArr = new int[numIdx];
            int[] vPos = new int[vList.size()];
            int vPosI = 0;
            Arrays.fill(vPos, -1);
            int pos = 0;
            for (QModelTriangle triangle : triList) {
                for (int i = 0; i < 3; i++) {
                    int idx = triangle.vertIdx[i];
//                      if (vPos[idx]<0) {
                        vPos[idx] = vPosI++;
                        QModelVertex v = obj.listVertex.get(idx);
                        vBuf.put(Float.floatToRawIntBits(v.x));
                        vBuf.put(Float.floatToRawIntBits(v.y));
                        vBuf.put(Float.floatToRawIntBits(v.z));
                        int normal = RenderUtil.packNormal(triangle.normal[i]);
                        vBuf.put(normal);
                        int textureHalf2 = RenderUtil.packTexCoord(triangle.texCoord[0][i], triangle.texCoord[1][i]);
                        vBuf.put(textureHalf2);
                        vBuf.put(0xffffffff);
                        vBuf.increaseVert();
//                      }
                    idxArr[pos++] = vPos[idx];
                    vBuf.putIdx(vPos[idx]);
                }
            }
            
            
            if (rGroup.gpuBuf == null) {
                rGroup.gpuBuf = new GLTriBuffer(GL15.GL_DYNAMIC_DRAW);
            }
            rGroup.gpuBuf.upload(vBuf);
        }
        

        rGroup.gpuBuf.draw();


    }
}
