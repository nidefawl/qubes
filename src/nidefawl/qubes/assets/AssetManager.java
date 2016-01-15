package nidefawl.qubes.assets;

import java.io.File;
import java.io.IOException;
import java.util.*;

import nidefawl.qubes.config.WorkingEnv;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.util.*;

public abstract class AssetManager {
    private static AssetManager instance;

    public static AssetManager getInstance() {
        return instance;
    }
    ArrayList<AssetPack> assetPacks        = new ArrayList<>();
    File                 folder;

    AssetManager() {

    }

    public static void init() {
        if (GameContext.getSide() == Side.CLIENT) {
            instance = new AssetManagerClient();
        } else {
            instance = new AssetManagerServer(); 
        }
        instance._init();
    }
    abstract void _init();

    public abstract void toggleExternalResources();

    public abstract boolean isExternalResources();

    public abstract AssetVoxModel loadVoxModel(String name);

    public abstract Shader loadShader(IResourceManager mgr, String name);

    public abstract Shader loadShader(IResourceManager mgr, String name, IShaderDef def);

    public abstract ShaderSource getLastFailedShaderSource();
    
    public AssetBinary loadBin(String name) {
        AssetInputStream is = null;
        try {
            is = findResource(name, false);
            if (is != null && is.inputStream != null) {
                AssetBinary asset = new AssetBinary(name);
                asset.load(is);
                return asset;
            }
        } catch (Exception e) {
            throw new GameError("Cannot load asset '" + name + "': " + e, e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new GameError("Error while closing inputstream", e);
                }
            }
        }
        throw new GameError("Cannot load asset '" + name + "': File does not exist");
    }

    public Collection<AssetPath> collectAssets(String path, String extension) {
        LinkedHashSet<AssetPath> assets = new LinkedHashSet<>();
        for (int i = 0; i < assetPacks.size(); i++) {
            AssetPack pack = assetPacks.get(i);
            pack.collectAssets(path, extension, assets);
        }
        return assets;
    }
    public AssetInputStream findResource(String name, boolean optional) {
        AssetInputStream is = null;
        for (int i = 0; i < assetPacks.size(); i++) {
            AssetPack pack = assetPacks.get(i);
            try {
                is = pack.getInputStream(name);
                if (is != null && is != null && is.inputStream != null) {
                    return is;
                }
            } catch (IOException e1) {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        throw new GameError("Failed loading resource "+name+" from "+pack, e);
                    }
                }
                throw new GameError("Failed loading resource "+name+" from "+pack, e1);
            }
        }

        if (optional)
            return null;
        throw new RuntimeException("Missing resource "+name);
    }


    public void reloadPNGAsset(AssetTexture asset) {
        String name = asset.getName();
        AssetInputStream is = null;
        try {
            is = findResource(name, false);
            if (is != null && is.inputStream != null) {
                if (!asset.loadPNGDecoder(is)) {
                    is = is.source.getInputStream(name);
                    asset.loadImageIO(is);
                }
                return;
            }
        } catch (Exception e) {
            throw new GameError("Cannot load asset '" + name + "': " + e, e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new GameError("Error while closing inputstream", e);
                }
            }
        }
        throw new GameError("Cannot load asset '" + name + "': File does not exist");
    }
    public AssetTexture loadPNGAsset(String name) {
        return loadPNGAsset(name, false);
    }
    public AssetTexture loadPNGAsset(String name, boolean optional) {
        AssetInputStream is = null;
        try {
            is = findResource(name, optional);
            if (is != null && is.inputStream != null) {
                AssetTexture asset = new AssetTexture(name);
                if (!asset.loadPNGDecoder(is)) {
                    is = is.source.getInputStream(name);
                    asset.loadImageIO(is);
                }
                return asset;
            }
        } catch (Exception e) {
            throw new GameError("Cannot load asset '" + name + "': " + e, e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new GameError("Error while closing inputstream", e);
                }
            }
        }
        if (optional) 
            return null;
        throw new GameError("Cannot load asset '" + name + "': File does not exist");
    }
}
