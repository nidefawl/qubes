package nidefawl.qubes.assets;

import java.io.*;
import java.util.ArrayList;

import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.ShaderCompileError;
import nidefawl.qubes.shader.ShaderSource;
import nidefawl.qubes.util.GameError;

public class AssetManager {
    final static AssetManager instance = new AssetManager();
    File                      folder   = new File("res");
    ArrayList<Asset>          assets   = new ArrayList<>();
    private ShaderSource lastFailedShader;

    AssetManager() {
        
    }

    public static AssetManager getInstance() {
        return instance;
    }

    public void init() {
        folder.mkdirs();
    }
    public InputStream findResource(String name) {
        try {

            if (folder.exists()) {
                File f = new File(folder, name);
                if (f.exists()) {
                    FileInputStream fis = new FileInputStream(f);
                    BufferedInputStream bif = new BufferedInputStream(fis);
                    return bif;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return getClass().getResourceAsStream("/res/"+name);
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
