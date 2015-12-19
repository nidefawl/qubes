package nidefawl.qubes.assets;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

import nidefawl.qubes.config.WorkingEnv;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.IResourceManager;

public class AssetManager {
    final static AssetManager instance = new AssetManager();
    ArrayList<Asset>          assets   = new ArrayList<>();
    ArrayList<AssetPack>          assetPacks   = new ArrayList<>();
    private ShaderSource lastFailedShader;
    File                      folder;
    private boolean externalResources = true;
    public void toggleExternalResources() {
        this.externalResources = !externalResources;
        init();
    }
    public boolean isExternalResources() {
        return this.externalResources;
    }

    AssetManager() {
        
    }

    public static AssetManager getInstance() {
        return instance;
    }
    public Collection<AssetPath> collectAssets(String path, String extension) {
        LinkedHashSet<AssetPath> assets = new LinkedHashSet<>();
        for (int i = 0; i < assetPacks.size(); i++) {
            AssetPack pack = assetPacks.get(i);
            pack.collectAssets(path, extension, assets);
        }
        return assets;
    }

    public void init() {
        folder = WorkingEnv.getAssetFolder();
//        if (!WorkingEnv.loadAssetsFromClassPath()) {
//        }
//        File f = WorkingEnv.getPacksFolder();
//        if (f.isDirectory()) {
//            File[] fPackList = f.listFiles(new FilenameFilter() {
//                
//                @Override
//                public boolean accept(File dir, String name) {
//                    return dir.isDirectory() || (dir.isFile() && dir.getName().endsWith(".zip"));
//                }
//            });
//            for (int i = 0;fPackList != null &&  i < fPackList.length; i++) {
//                if (fPackList[i].isDirectory())
//                    assetPacks.add(new AssetPackFolder(fPackList[i]));
//                else {
//                    try {
//                        assetPacks.add(new AssetPackZip(fPackList[i]));
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }
        if (this.externalResources) {
            if (folder.exists())
                assetPacks.add(new AssetPackFolder(folder));
        }
        assetPacks.add(new AssetPackClassPath());
        Collections.reverse(assetPacks);
        System.out.println("Found "+assetPacks.size()+" asset packs");
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
                asset.load(is);
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
                asset.load(is);
                assets.add(asset);
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
    public AssetVoxModel loadVoxModel(String name) {
        AssetInputStream is = null;
        try {
            is = findResource(name, false);
            if (is != null && is.inputStream != null) {
                AssetVoxModel asset = new AssetVoxModel(name);
                asset.load(is);
//                assets.add(asset);
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

    public Shader loadShader(IResourceManager mgr, String name) {
        return loadShader(mgr, name, null);
    }
    public Shader loadShader(IResourceManager mgr, String name, IShaderDef def) {
        if (!name.startsWith("/"))
            name = "shaders/" + name;
        Shader shader = new Shader(name);
        try {
            int idx = name.lastIndexOf("/");
            String path;
            String fname;
            if (idx <= 0) {
                path = "";
                fname = name;
            } else {
                path = name.substring(0, idx);
                fname = name.substring(idx+1);
            }
            if (mgr != null)
            mgr.addResource(shader);
            shader.load(this, path, fname, def);
        } catch (ShaderCompileError e) {
            this.lastFailedShader = e.getShaderSource();
            throw e;
        } catch (GameError e) {
            throw e;
        } catch (Exception e) {
            throw new GameError("Cannot load asset '" + name + "': " + e, e);
        }
        return shader;
    }
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

    public ShaderSource getLastFailedShaderSource() {
        return this.lastFailedShader;
    }
}
