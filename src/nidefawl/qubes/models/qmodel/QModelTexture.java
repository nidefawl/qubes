package nidefawl.qubes.models.qmodel;

import static org.lwjgl.vulkan.VK10.*;

import java.io.EOFException;

import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.models.qmodel.loader.ModelLoaderQModel;
import nidefawl.qubes.texture.TextureBinMips;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vulkan.VkTexture;

public class QModelTexture {
    private final int idx;
    public final String name;
    public final String path;
    private int glid;
    boolean loaded = false;
    private VkTexture vkTexture;

    public QModelTexture(int i, ModelLoaderQModel loader) throws EOFException {
        this.idx = i;
        this.name = loader.readString(32);
        this.path = loader.readString(128);
//        System.out.println(loader.getModelName()+" - texture "+idx+": name "+this.name+", path "+this.path);
    }
    
    public int get() {
        //TODO: load textures in thread/dynamic, globally
        if (!this.loaded) {
            this.loaded = true;
            AssetTexture t = AssetManager.getInstance().loadPNGAsset("models/"+this.path.toLowerCase());
            if (Engine.isVulkan) {
                this.vkTexture = new VkTexture(Engine.vkContext);
                TextureBinMips binMips = new TextureBinMips(t);
                this.vkTexture.build(VK_FORMAT_R8G8B8A8_UNORM, binMips);
            }
            if (this.glid <= 0) {
                int maxDim = Math.max(t.getWidth(), t.getHeight());
                int mipmapLevel = 1+GameMath.log2(maxDim);
                this.glid = TextureManager.getInstance().makeNewTexture(t, false, true, mipmapLevel);
            }
        }
        return this.glid;
    }

    public void release() {
        GL.deleteTexture(this.glid);
        this.glid = 0;
    }

}
