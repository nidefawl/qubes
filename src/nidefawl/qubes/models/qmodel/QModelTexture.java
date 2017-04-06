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
import nidefawl.qubes.vulkan.VkDescriptor;
import nidefawl.qubes.vulkan.VkTexture;

public class QModelTexture {
    private final int idx;
    public final String name;
    public final String path;
    private int glid;
    boolean loaded = false;
    private VkTexture vkTexture;
    public VkDescriptor descriptorSetTex;

    public QModelTexture(int i, ModelLoaderQModel loader) throws EOFException {
        this.idx = i;
        this.name = loader.readString(32);
        this.path = loader.readString(128);
//        System.out.println(loader.getModelName()+" - texture "+idx+": name "+this.name+", path "+this.path);
    }
    public void load() {
        if (!this.loaded) {
            if (Engine.isVulkan) {
                AssetTexture t = AssetManager.getInstance().loadPNGAsset("models/"+this.path.toLowerCase());
                this.vkTexture = new VkTexture(Engine.vkContext);
                TextureBinMips binMips = new TextureBinMips(t);
                this.vkTexture.build(VK_FORMAT_R8G8B8A8_UNORM, binMips);
                this.vkTexture.genView();
                
//              vkContext.descLayouts.getDescriptorSets);
                this.descriptorSetTex = Engine.vkContext.descLayouts.allocDescSetSampleSingle();
                
                this.descriptorSetTex.setBindingCombinedImageSampler(0, this.vkTexture.getView(), Engine.vkContext.samplerLinear, this.vkTexture.getImageLayout());
                this.descriptorSetTex.update(Engine.vkContext);

            } else {
                AssetTexture t = AssetManager.getInstance().loadPNGAsset("models/"+this.path.toLowerCase());
                int maxDim = Math.max(t.getWidth(), t.getHeight());
                int mipmapLevel = 1+GameMath.log2(maxDim);
                this.glid = TextureManager.getInstance().makeNewTexture(t, false, true, mipmapLevel);
            }
        }
        this.loaded = true;
    }
    public int get() {
        return this.glid;
    }

    public void release() {
        if (this.loaded) {
            if (this.glid > 0) {
                GL.deleteTexture(this.glid);
                this.glid = 0;
            }
            if (this.vkTexture != null) {
                this.vkTexture.destroy();
                this.vkTexture = null;
            }
            this.descriptorSetTex = null;
        }
    }

}
