package nidefawl.qubes.assets;

import java.util.Collection;
import java.util.Collections;

import nidefawl.qubes.config.WorkingEnv;
import nidefawl.qubes.shader.IShaderDef;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.ShaderSource;
import nidefawl.qubes.util.IResourceManager;

public class AssetManagerServer extends AssetManager {

    @Override
    public void toggleExternalResources() {
    }

    @Override
    public boolean isExternalResources() {
        return true;
    }

    @Override
    public AssetVoxModel loadVoxModel(String name) {
        return null;
    }

    @Override
    public Shader loadShader(IResourceManager mgr, String name) {
        return null;
    }

    @Override
    public Shader loadShader(IResourceManager mgr, String name, IShaderDef def) {
        return null;
    }

    @Override
    public ShaderSource getLastFailedShaderSource() {
        return null;
    }

    @Override
    void _init() {
        folder = WorkingEnv.getAssetFolder();
        if (folder.exists())
            assetPacks.add(new AssetPackFolder(folder));
        assetPacks.add(new AssetPackClassPath());
        Collections.reverse(assetPacks);
        System.out.println("Found "+assetPacks.size()+" asset packs");
    }

    @Override
    public Shader loadShader(IResourceManager mgr, String nameVSH, String nameFSH, String nameGSH, String nameCSH, IShaderDef def) {
        return null;
    }

}
