package nidefawl.qubes.vr;

import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT;
import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT;
import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.opengl.*;

import com.sun.jna.Pointer;

import jopenvr.RenderModel_TextureMap_t;
import jopenvr.RenderModel_t;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.gl.GLVBO;

public class CGLRenderModelNative {

    RenderModel_t t;
    RenderModel_TextureMap_t tex;
    GLVBO vbo;
    GLVBO vboIndex;
    int texId;
    int m_unVertexCount;
    boolean init;

    public CGLRenderModelNative(RenderModel_t t, RenderModel_TextureMap_t tex) {
        this.t = t;
        this.tex = tex;
        this.vbo = new GLVBO(GL15.GL_STATIC_DRAW);
        this.vboIndex = new GLVBO(GL15.GL_STATIC_DRAW);
    }
    
    public void init() {
        init = true;
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vbo.getVboId());
        GL15.nglBufferData(GL15.GL_ARRAY_BUFFER, (3+3+2)*t.unVertexCount*4, Pointer.nativeValue(this.t.rVertexData.getPointer()), GL15.GL_STATIC_DRAW);

        Engine.checkGLError("nglBufferData1");
        this.vbo.makeResident(GL15.GL_ARRAY_BUFFER, true, true);
        Engine.checkGLError("makeResident1");
        
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.vboIndex.getVboId());
        GL15.nglBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, (3)*t.unTriangleCount*2, Pointer.nativeValue(this.t.rIndexData.getPointer()), GL15.GL_STATIC_DRAW);

        Engine.checkGLError("nglBufferData2");
        this.vboIndex.makeResident(GL15.GL_ELEMENT_ARRAY_BUFFER, true, true);
        Engine.checkGLError("makeResident2");
        
        this.texId = glGenTextures();
        GL.bindTexture(GL13.GL_TEXTURE0, GL_TEXTURE_2D, this.texId);
        Engine.checkGLError("glGenTextures");
        nglTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, this.tex.unWidth, this.tex.unHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, Pointer.nativeValue(this.tex.rubTextureMapData));
        Engine.checkGLError("nglTexImage2D");
        GL30.glGenerateMipmap(GL_TEXTURE_2D);
        Engine.checkGLError("glGenerateMipmap");

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        Engine.checkGLError("glTexParameteri");
        float fLargest = glGetFloat( GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT );
        glTexParameterf( GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, fLargest );
        Engine.checkGLError("GL_TEXTURE_MAX_ANISOTROPY_EXT");
        
        GL.bindTexture(GL13.GL_TEXTURE0, GL_TEXTURE_2D, 0);
        Engine.checkGLError("bindTexture 0");
//
        m_unVertexCount = t.unTriangleCount * 3;

    }
    public void render() {
        if (!init) {
            init();
        }
        GL.bindTexture(GL13.GL_TEXTURE0, GL_TEXTURE_2D, this.texId);
        Engine.checkGLError("bindTexture draw");
        Engine.bindBuffer(this.vbo);
        Engine.checkGLError("bindBuffer");
        Engine.bindIndexBuffer(this.vboIndex);
        Engine.checkGLError("bindIndexBuffer");
        glDrawElements( GL_TRIANGLES, m_unVertexCount, GL_UNSIGNED_SHORT, 0L );
        Engine.checkGLError("glDrawElements");
    }

}
