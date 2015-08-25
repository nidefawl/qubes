package nidefawl.qubes.assets;

import java.io.*;
import java.util.ArrayList;

import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.util.GameError;

public class AssetManager {
    final static AssetManager instance = new AssetManager();
    File                      folder   = new File("res");
    ArrayList<Asset>          assets   = new ArrayList<>();

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
        InputStream fis = null;
        InputStream fis2 = null;
        InputStream fis3 = null;
        try {
            fis = findResource(name + ".fsh");
            fis2 = findResource(name + ".vsh");
            fis3 = findResource(name + ".gsh");
            shader.load(fis, fis2, fis3);
        } catch (GameError e) {
            throw e;
        } catch (Exception e) {
            throw new GameError("Cannot load asset '" + name + "': " + e, e);
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
                if (fis2 != null) {
                    fis2.close();
                }
                if (fis3 != null) {
                    fis3.close();
                }
            } catch (Exception e) {
                throw new GameError("Failed closing file", e);
            }
        }
        return shader;
    }
}
