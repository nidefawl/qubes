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

    public AssetTexture loadPNGAsset(String name) {
        File f = new File(folder, name);
        if (f.exists()) {
            try {
                AssetTexture asset = new AssetTexture(f);
                asset.load();
                assets.add(asset);
                return asset;
            } catch (Exception e) {
                throw new GameError("Cannot load asset '" + name + "': " + e, e);
            }
        }
        throw new GameError("Cannot load asset '" + f + "': File does not exist");
    }

    public Shader loadShader(String name) {
        File f = new File(folder, name);
        if (f.getParentFile().exists()) {
            Shader shader = new Shader(name);
            InputStream fis = null;
            InputStream fis2 = null;
            try {
                fis = new FileInputStream(new File(folder, name + ".fsh"));
                fis = new BufferedInputStream(fis);
                fis2 = new FileInputStream(new File(folder, name + ".vsh"));
                fis2 = new BufferedInputStream(fis2);
                shader.load(fis, fis2);
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
                } catch (Exception e) {
                    throw new GameError("Failed closing file", e);
                }
            }
            return shader;
        
        }
        throw new GameError("Cannot load asset '" + f.getParentFile() + "': File does not exist");
    }
}
