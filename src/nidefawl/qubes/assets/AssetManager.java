package nidefawl.qubes.assets;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

import nidefawl.qubes.config.WorkingEnv;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.util.GameError;

public class AssetManager {
    final static AssetManager instance = new AssetManager();
    ArrayList<Asset>          assets   = new ArrayList<>();
    ArrayList<AssetPack>          assetPacks   = new ArrayList<>();
    private ShaderSource lastFailedShader;
    File                      folder;

    AssetManager() {
        
    }

    public static AssetManager getInstance() {
        return instance;
    }

    public void init() {
        folder = WorkingEnv.getAssetFolder();
//        if (!WorkingEnv.loadAssetsFromClassPath()) {
//        }
        File f = WorkingEnv.getPacksFolder();
        if (f.isDirectory()) {
            File[] fPackList = f.listFiles(new FilenameFilter() {
                
                @Override
                public boolean accept(File dir, String name) {
                    return dir.isDirectory() || (dir.isFile() && dir.getName().endsWith(".zip"));
                }
            });
            for (int i = 0;fPackList != null &&  i < fPackList.length; i++) {
                if (fPackList[i].isDirectory())
                    assetPacks.add(new AssetPackFolder(fPackList[i]));
                else {
                    try {
                        assetPacks.add(new AssetPackZip(fPackList[i]));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (folder.exists())
        assetPacks.add(new AssetPackFolder(folder));
        assetPacks.add(new AssetPackClassPath());
        Collections.reverse(assetPacks);
        System.out.println("Found "+assetPacks.size()+" asset packs");
    }
    
    public InputStream findResource(String name, boolean optional) {
        InputStream is = null;
        for (int i = 0; i < assetPacks.size(); i++) {
            AssetPack pack = assetPacks.get(i);
            try {
                is = pack.getInputStream(name);
                if (is != null) {
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

    public AssetTexture loadPNGAsset(String name) {
        InputStream is = null;
        try {
            is = findResource(name, false);
            if (is != null) {
                AssetTexture asset = new AssetTexture();
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
        throw new GameError("Cannot load asset '" + name + "': File does not exist");
    }

    public Shader loadShader(String name) {
        return loadShader(name, null);
    }
    public Shader loadShader(String name, IShaderDef def) {
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

    public ShaderSource getLastFailedShaderSource() {
        return this.lastFailedShader;
    }
}
