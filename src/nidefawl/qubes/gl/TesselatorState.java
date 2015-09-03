package nidefawl.qubes.gl;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import nidefawl.qubes.Game;

public class TesselatorState {
    public int           vertexcount;

    public boolean       useColorPtr;
    public boolean       useTexturePtr;
    public boolean       useTexturePtr2;
    public boolean       useNormalPtr;
    public boolean       useAttribPtr1;
    public int vboId = 0;
    public int vboSize = 0;
    
    public void copyTo(TesselatorState out) {
        out.vertexcount = this.vertexcount;
        out.useColorPtr = this.useColorPtr;
        out.useTexturePtr = this.useTexturePtr;
        out.useTexturePtr2 = this.useTexturePtr2;
        out.useNormalPtr = this.useNormalPtr;
        out.useAttribPtr1 = this.useAttribPtr1;
    }



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
        if (useTexturePtr2)
            stride+=1;
        if (useAttribPtr1)
            stride+=2;
        return stride;
    }

    public void bindVBO() {
        if (this.vboId == 0) {
            vboId = GL15.glGenBuffers();
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
    }
    
    public void setClientStates(ByteBuffer buffer) {
        int stride = getVSize();
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glVertexPointer(4, GL11.GL_FLOAT, stride*4, (ByteBuffer) buffer.position(0));
        int offset = 4;
        if (useNormalPtr) {
            GL11.glNormalPointer(GL11.GL_BYTE, stride*4, (ByteBuffer) buffer.position(offset*4));
            if (Game.GL_ERROR_CHECKS) Engine.checkGLError("glNormalPointer");
            GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
            offset+=1;
        }
        if (useTexturePtr) {
            GL11.glTexCoordPointer(2, GL11.GL_FLOAT, stride*4, (ByteBuffer) buffer.position(offset*4));
            if (Game.GL_ERROR_CHECKS) Engine.checkGLError("glTexCoordPointer");
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            offset+=2;
        }
        if (useColorPtr) {
            GL11.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, stride*4, (ByteBuffer) buffer.position(offset*4));
            if (Game.GL_ERROR_CHECKS) Engine.checkGLError("glTexCoordPointer");
            GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
            offset+=1;
        }
        if (useTexturePtr2) {
//            GL11.glTexCoordPointer(2, GL11.GL_SHORT, stride, (ByteBuffer) buffer.position(offset*4));
//            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("glTexCoordPointer");
//            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            offset+=1;
        }
        if (useAttribPtr1) {
//            GL20.glEnableVertexAttribArray(5);
//            GL20.glVertexAttribPointer(5, 4, GL11.GL_SHORT, false, stride*4, offset*4);
//            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("AttribPtr "+5);
//            offset+=2;
        }
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
        if (useTexturePtr2) {
            GL20.glEnableVertexAttribArray(4);
            GL20.glVertexAttribPointer(4, 2, GL11.GL_SHORT, false, stride*4, offset*4);
            if (Game.GL_ERROR_CHECKS) Engine.checkGLError("AttribPtr "+4);
            offset+=1;
        }
        if (useAttribPtr1) {
            GL20.glEnableVertexAttribArray(5);
            GL20.glVertexAttribPointer(5, 4, GL11.GL_SHORT, false, stride*4, offset*4);
            if (Game.GL_ERROR_CHECKS) Engine.checkGLError("AttribPtr "+5);
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
        bindVBO();
        setAttrPtr();
        drawVBO(mode);
    }
}
