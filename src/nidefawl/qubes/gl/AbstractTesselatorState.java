package nidefawl.qubes.gl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import nidefawl.qubes.Game;
import nidefawl.qubes.util.ITessState;
import nidefawl.qubes.util.Stats;

public abstract class AbstractTesselatorState implements ITessState {
    public int           vertexcount;

    public boolean       useColorPtr;
    public boolean       useTexturePtr;
    public boolean       useNormalPtr;
    public boolean       useUINTPtr;
    public int idxCount;
    
    public void copyTo(AbstractTesselatorState out) {
        out.vertexcount = this.vertexcount;
        out.useColorPtr = this.useColorPtr;
        out.useTexturePtr = this.useTexturePtr;
        out.useNormalPtr = this.useNormalPtr;
        out.useUINTPtr = this.useUINTPtr;
        out.idxCount = this.idxCount;
    }
    public abstract GLVBO getVBO();
    public abstract GLVBO getVBOIndices();


    public int getIdx(int v) {
        return getVSize() * v;
    }

    public int getVSize() {
        int stride = 4;
        if (useColorPtr)
            stride++;
        if (useNormalPtr)
            stride++;
        if (useTexturePtr)
            stride+=2;
        if (useUINTPtr)
            stride+=2;
        return stride;
    }

    static void drawVBO(int mode, int idxCount) {
        Stats.tessDrawCalls++;
//        GL11.glDrawArrays(mode, 0, vertexcount);
        if (mode == GL11.GL_LINES) {
//            System.out.println("draw lines");
        }
        GL11.glDrawElements(mode, idxCount, GL11.GL_UNSIGNED_INT, 0);
        
        
    }

    public void drawQuads() {
        bindAndDraw(GL11.GL_TRIANGLES);
    }

    public void bindAndDraw(int mode) {
        int tessSetting = getSetting();
        Engine.bindVAO(GLVAO.vaoTesselator[tessSetting], false);
        Engine.bindBuffer(getVBO());
        Engine.bindIndexBuffer(getVBOIndices());
        drawVBO(mode, idxCount);
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("glDrawArrays ("+vertexcount+","+idxCount+")");
    }
    public int getSetting() {
        int s = 0;
        if (this.useNormalPtr)
            s|=1;
        if (this.useTexturePtr)
            s|=2;
        if (this.useColorPtr)
            s|=4;
        if (this.useUINTPtr)
            s|=8;
        return s;
    }
}
