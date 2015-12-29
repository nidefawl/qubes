package nidefawl.qubes.gl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import nidefawl.qubes.Game;

public abstract class AbstractTesselatorState {
    public int           vertexcount;

    public boolean       useColorPtr;
    public boolean       useTexturePtr;
    public boolean       useNormalPtr;
    public boolean       useUINTPtr;
    
    public void copyTo(AbstractTesselatorState out) {
        out.vertexcount = this.vertexcount;
        out.useColorPtr = this.useColorPtr;
        out.useTexturePtr = this.useTexturePtr;
        out.useNormalPtr = this.useNormalPtr;
        out.useUINTPtr = this.useUINTPtr;
    }
    public abstract GLVBO getVBO();


    public int getIdx(int v) {
        return getVSize() * v;
    }
    
    public int getVSize() {
        int stride = 4;
        if (useColorPtr)
            stride++;
        if (useNormalPtr)
            stride+=3;
        if (useTexturePtr)
            stride+=2;
        if (useUINTPtr)
            stride+=4;
        return stride;
    }


    public void setAttrPtr() {
        int stride = getVSize();
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 4, GL11.GL_FLOAT, false, stride*4, 0);
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("AttribPtr "+0);
        
        int offset = 4;
        if (useNormalPtr) {
            GL20.glEnableVertexAttribArray(1);
            GL20.glVertexAttribPointer(1, 3, GL11.GL_BYTE, false, stride*4, offset*4);
            if (Game.GL_ERROR_CHECKS) Engine.checkGLError("AttribPtr "+1);
            offset+=1;
        }
        if (useTexturePtr) {
            GL20.glEnableVertexAttribArray(2);
            GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, stride*4, offset*4);
            if (Game.GL_ERROR_CHECKS) Engine.checkGLError("AttribPtr "+2);
            offset+=2;
        }
        if (useColorPtr) {
            GL20.glEnableVertexAttribArray(3);
            GL20.glVertexAttribPointer(3, 4, GL11.GL_UNSIGNED_BYTE, true, stride*4, offset*4);
            if (Game.GL_ERROR_CHECKS) Engine.checkGLError("AttribPtr "+3);
            offset+=1;
        }
        if (useUINTPtr) {
            //BLOCKINFO
            GL20.glEnableVertexAttribArray(4);
            GL30.glVertexAttribIPointer(4, 4, GL11.GL_UNSIGNED_SHORT, stride*4, offset*4);
            if (Game.GL_ERROR_CHECKS) Engine.checkGLError("AttribPtr "+4);
            offset+=2;
        }
    }
    public void drawVBO(int mode) {
        GL11.glDrawArrays(mode, 0, vertexcount);
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("glDrawArrays ("+vertexcount+", texture: "+useTexturePtr+")");
    }

    public void drawQuads() {
        bindAndDraw(GL11.GL_QUADS);
    }

    public void bindAndDraw(int mode) {
        getVBO().bind();
        setAttrPtr();
        drawVBO(mode);
    }
}
