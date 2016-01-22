package nidefawl.qubes.assets;

import java.io.IOException;
import java.util.Collections;

import nidefawl.qubes.config.WorkingEnv;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.IResourceManager;

public class AssetManagerClient extends AssetManager {

    private ShaderSource lastFailedShader;
    private boolean      externalResources = true;


    public void toggleExternalResources() {
        this.externalResources = !externalResources;
        _init();
    }

    public boolean isExternalResources() {
        return this.externalResources;
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
        ShaderSourceBundle shaderSrc = new ShaderSourceBundle(name);
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
            shaderSrc.load(this, path, fname, def);
            Shader shader = shaderSrc.compileShader();
            if (mgr != null && shader != null)
                mgr.addResource(shader);
            if (shader != null) {
                shader.setSource(shaderSrc);
            }
            return shader;
        } catch (ShaderCompileError e) {
            this.lastFailedShader = e.getShaderSource();
            throw e;
        } catch (GameError e) {
            throw e;
        } catch (Exception e) {
            throw new GameError("Cannot load asset '" + name + "': " + e, e);
        }
    }

    public ShaderSource getLastFailedShaderSource() {
        return this.lastFailedShader;
    }
    
    @Override
    void _init() {
        folder = WorkingEnv.getAssetFolder();
//      if (!WorkingEnv.loadAssetsFromClassPath()) {
//      }
//      File f = WorkingEnv.getPacksFolder();
//      if (f.isDirectory()) {
//          File[] fPackList = f.listFiles(new FilenameFilter() {
//              
//              @Override
//              public boolean accept(File dir, String name) {
//                  return dir.isDirectory() || (dir.isFile() && dir.getName().endsWith(".zip"));
//              }
//          });
//          for (int i = 0;fPackList != null &&  i < fPackList.length; i++) {
//              if (fPackList[i].isDirectory())
//                  assetPacks.add(new AssetPackFolder(fPackList[i]));
//              else {
//                  try {
//                      assetPacks.add(new AssetPackZip(fPackList[i]));
//                  } catch (IOException e) {
//                      e.printStackTrace();
//                  }
//              }
//          }
//      }
      if (this.externalResources) {
          if (folder.exists())
              assetPacks.add(new AssetPackFolder(folder));
      }
      assetPacks.add(new AssetPackClassPath());
      Collections.reverse(assetPacks);
      System.out.println("Found "+assetPacks.size()+" asset packs");
  
    }
}
