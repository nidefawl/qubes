package nidefawl.qubes.assets;

import java.io.*;
import java.util.ArrayList;

import nidefawl.qubes.config.WorkingEnv;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.ShaderCompileError;
import nidefawl.qubes.shader.ShaderSource;
import nidefawl.qubes.util.GameError;

public class AssetManager {
    final static AssetManager instance = new AssetManager();
    ArrayList<Asset>          assets   = new ArrayList<>();
    private ShaderSource lastFailedShader;
    File                      folder;

    AssetManager() {
        
    }

    public static AssetManager getInstance() {
        return instance;
    }

    public void init() {
        folder = WorkingEnv.getAssetFolder();
    }
    
    public InputStream findResource(String name) {
        Exception e = null;
        if (!WorkingEnv.loadAssetsFromClassPath()) {
            try {

                if (folder.exists()) {
                    File f = new File(folder, name);
                    if (f.exists()) {
                        FileInputStream fis = new FileInputStream(f);
                        BufferedInputStream bif = new BufferedInputStream(fis);
                        return bif;
                    }
                }
            } catch (IOException ioe) {
                e = ioe;
            }
        }
        InputStream is = null;
        try {
            is = getClass().getResourceAsStream("/res/"+name);
        } catch (Exception e2) {
            throw e2;
        }

        if (is == null && e != null) {
            throw new RuntimeException(e);
        }
        return is;
    }

    public AssetTexture loadPNGAsset(String name) {
        InputStream is = null;
        try {
            is = findResource(name);
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
        Shader shader = new Shader(name);
        try {
            int idx = name.lastIndexOf("/");
            String path;
            String fname;
            if (idx == 0) {
                path = "";
                fname = name;
            } else {
                path = name.substring(0, idx);
                fname = name.substring(idx+1);
            }
            shader.load(this, path, fname);
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
