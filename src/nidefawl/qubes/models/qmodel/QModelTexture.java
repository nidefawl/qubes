package nidefawl.qubes.models.qmodel;

import java.io.EOFException;

import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.models.qmodel.loader.ModelLoaderQModel;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.GameMath;

public class QModelTexture {
    private final int idx;
    public final String name;
    public final String path;
    private int glid;

    public QModelTexture(int i, ModelLoaderQModel loader) throws EOFException {
        this.idx = i;
        this.name = loader.readString(32);
        this.path = loader.readString(128);
//        System.out.println(loader.getModelName()+" - texture "+idx+": name "+this.name+", path "+this.path);
    }
    
    public int get() {
        //TODO: load textures in thread/dynamic, globally
        if (this.glid <= 0) {
            AssetTexture t = AssetManager.getInstance().loadPNGAsset("models/"+this.path);
            int maxDim = Math.max(t.getWidth(), t.getHeight());
            int mipmapLevel = 1+GameMath.log2(maxDim);
            this.glid = TextureManager.getInstance().makeNewTexture(t, false, true, mipmapLevel);
        }
        return this.glid;
    }

    public void release() {
        GL.deleteTexture(this.glid);
        this.glid = 0;
    }

}
