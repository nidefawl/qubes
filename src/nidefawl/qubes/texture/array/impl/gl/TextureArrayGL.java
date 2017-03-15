package nidefawl.qubes.texture.array.impl.gl;

import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LEVEL;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.Arrays;


import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.texture.array.TextureArray;

public abstract class TextureArrayGL extends TextureArray {
    public int    glid;

    public TextureArrayGL(int maxTextures) {
        super(maxTextures);
        this.internalFormat = GL_RGBA8;
    }
    protected void unload() {
        if (!firstInit) {
            GL.deleteTexture(this.glid);
            this.texNameToAssetMap.clear();
            this.blockIDToAssetList.clear();
            this.slotTextureMap.clear();
            Arrays.fill(this.textures, 0);
        }
    }
    public void init() {
        glid = GL11.glGenTextures();
    }

    @Override
    protected void uploadTextures() {
    }

    @Override
    protected void collectTextures(AssetManager mgr) {
    }

    @Override
    protected void postUpload() {
    } 
    @Override
    protected void initStorage() {
//      System.err.println(glid+"/"+numMipmaps+"/"+this.tileSize+"/"+this.numTextures);
      GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, this.glid);
      Engine.checkGLError("pre glTexStorage3D");
      nidefawl.qubes.gl.GL.glTexStorage3D(GL30.GL_TEXTURE_2D_ARRAY, numMipmaps, 
              this.internalFormat,              //Internal format
              this.tileSize, this.tileSize,   //width,height
              this.numTextures       //Number of layers
      );
      glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAX_LEVEL, numMipmaps-1);
      Engine.checkGLError("glTexStorage3D");
  }
    @Override
    public void destroy() {
        unload();
    }

}
