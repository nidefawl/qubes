package nidefawl.qubes.assets;

import java.io.IOException;
import java.util.Collections;

import nidefawl.qubes.config.WorkingEnv;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.shader.ShaderSource.ProcessMode;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.IResourceManager;
import nidefawl.qubes.vulkan.VKContext;
import nidefawl.qubes.vulkan.VkShader;

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
    public Shader loadShaderBinary(IResourceManager mgr, String nameFSH, String nameVSH, IShaderDef def) {

        final String[] pathNameFSH = splitPath(nameFSH);
        final String[] pathNameVSH = nameVSH != null ? splitPath(nameVSH) : null;

        try {
            AssetBinary binFrag = loadBin(pathNameFSH[0]+"/"+pathNameFSH[1]);
            AssetBinary binVert = nameVSH != null ? loadBin(pathNameVSH[0]+"/"+pathNameVSH[1]) : null;
            GraphicShader shader = new GraphicShader(pathNameFSH[1], binFrag, binVert, def);
            if (mgr != null && shader != null)
                mgr.addResource(shader);
            return shader;
        } catch (ShaderCompileError e) {
            this.lastFailedShader = e.getShaderSource();
            throw e;
        } catch (GameError e) {
            throw e;
        } catch (Exception e) {
            throw new GameError("Cannot load asset '" + nameVSH + "': " + e, e);
        }
        
    }
    public VkShader loadVkShaderBin(VKContext ctxt, String path, int stage) {

        final String[] pathSplit = splitPathBase(path);

        try {
            AssetBinary binFrag = loadBin(pathSplit[0]+"/"+pathSplit[1]);
//            System.out.println(new String(binFrag.getData(), "UTF-8"));
            VkShader shader = new VkShader(ctxt, stage, pathSplit[1], binFrag.getData());
            return shader;
        } catch (ShaderCompileError e) {
            this.lastFailedShader = e.getShaderSource();
            throw e;
        } catch (GameError e) {
            throw e;
        } catch (Exception e) {
            throw new GameError("Cannot load asset '" + path + "': " + e, e);
        }
    }
    
    @Override
    public ShaderSource loadVkShaderSource(String path, int stage, IShaderDef def) {
        final String[] pathSplit = splitPath(path);
        try {
            ShaderSource src = new ShaderSource(null, ProcessMode.VULKAN);
            src.setVersionString("#version 450");
            src.addEnabledExtensions("GL_ARB_separate_shader_objects", "GL_ARB_shading_language_420pack");
            src.load(this, pathSplit[0], pathSplit[1], def, stage);
            return src;
        } catch (ShaderCompileError e) {
            this.lastFailedShader = e.getShaderSource();
            throw e;
        } catch (GameError e) {
            throw e;
        } catch (Exception e) {
            throw new GameError("Cannot load asset '" + path + "': " + e, e);
        }
    }

    public Shader loadShader(IResourceManager mgr, String name, IShaderDef def) {
        return loadShader(mgr, name, null, null, null, def);
    }
    static String[] splitPathBase(String name) {
        String path;
        String fname;
        int idx = name.lastIndexOf("/");
        if (idx <= 0) {
            path = "";
            fname = name;
        } else {
            path = name.substring(0, idx);
            fname = name.substring(idx+1);
        }
        return new String[] { path, fname };
    }
    
    static String[] splitPath(String name) {
        String path;
        String fname;
        int idx = name.lastIndexOf("/");
        if (idx <= 0) {
            path = "";
            fname = name;
        } else {
            path = name.substring(0, idx);
            fname = name.substring(idx+1);
        }
        if (path.isEmpty()) {
            path = "shaders";
        } else if (!path.startsWith("/")) {
            path = "shaders/"+path;
        }
        return new String[] { path, fname };
    }
    
    public Shader loadShader(IResourceManager mgr, String nameFSH, String nameVSH, String nameGSH, String nameCSH, IShaderDef def) {

        
        if (nameVSH == null)
            nameVSH = nameFSH;
        if (nameGSH == null)
            nameGSH = nameFSH;
        if (nameCSH == null)
            nameCSH = nameFSH;
        

        ShaderSourceBundle shaderSrc = new ShaderSourceBundle(nameFSH);
        try {

            final String[] pathNameFSH = splitPath(nameFSH);
            final String[] pathNameVSH = splitPath(nameVSH);
            final String[] pathNameGSH = splitPath(nameGSH);
            final String[] pathNameCSH = splitPath(nameCSH);
            
            shaderSrc.load(this, pathNameFSH, pathNameVSH, pathNameGSH, pathNameCSH, def);
            Shader shader = shaderSrc.compileShader(def);
            if (mgr != null && shader != null)
                mgr.addResource(shader);
            if (shader != null) {
                shader.setSource(shaderSrc);
                shader.initDebug(shaderSrc.getDebugVars());
            }
            return shader;
        } catch (ShaderCompileError e) {
            this.lastFailedShader = e.getShaderSource();
            throw e;
        } catch (GameError e) {
            throw e;
        } catch (Exception e) {
            throw new GameError("Cannot load asset '" + nameVSH + "': " + e, e);
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
