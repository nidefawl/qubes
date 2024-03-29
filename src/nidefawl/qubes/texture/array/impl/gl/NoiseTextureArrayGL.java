package nidefawl.qubes.texture.array.impl.gl;

import static org.lwjgl.opengl.GL11.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import org.lwjgl.opengl.*;

import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.gl.Engine;

public class NoiseTextureArrayGL extends TextureArrayGL {
    
    public NoiseTextureArrayGL() {
        super(64);
    }

    @Override
    protected void uploadTextures() {
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, this.glid);
        int nBlock = 0;
        int totalTex = this.blockIDToAssetList.size();
        ByteBuffer directBuf = null;
        Iterator<Entry<Integer, ArrayList<AssetTexture>>> it = blockIDToAssetList.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Integer, ArrayList<AssetTexture>> entry = it.next();
            AssetTexture tex = entry.getValue().get(0);
            //            System.out.println("put data with dim "+tex.getWidth()+"x"+tex.getHeight()+" in tex slot "+slot+" with size "+this.tileSize+"x"+this.tileSize);
            directBuf = put(directBuf, tex.getData());
            GL12.glTexSubImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0,                     //Mipmap number
                    0, 0, entry.getKey(),                 //xoffset, yoffset, zoffset
                    this.tileSize, this.tileSize, 1,                 //width, height, depth
                    GL_RGBA,                //format
                    GL_UNSIGNED_BYTE,      //type
                    directBuf);                //pointer to data
            Engine.checkGLError("GL12.glTexSubImage3D");
            this.numUploaded++;
        }
        this.totalSlots = nBlock;
    }

    @Override
    protected void postUpload() {
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, this.glid);
        glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL12.GL_TEXTURE_MAX_LEVEL, 0);
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
    }

    @Override
    public void load() {
        super.load();
        this.numMipmaps = 1;
    }

    @Override
    protected void collectTextures(AssetManager mgr) {
        collectNoiseTextures(mgr);
    }

}
