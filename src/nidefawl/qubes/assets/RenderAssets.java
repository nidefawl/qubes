package nidefawl.qubes.assets;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;

import nidefawl.qubes.async.AsyncTask;
import nidefawl.qubes.async.AsyncTasks;
import nidefawl.qubes.config.RenderSettings;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gui.LoadingScreen;
import nidefawl.qubes.models.BlockModelManager;
import nidefawl.qubes.models.EntityModelManager;
import nidefawl.qubes.models.ItemModelManager;
import nidefawl.qubes.render.gui.SingleBlockRenderAtlas;
import nidefawl.qubes.texture.array.*;

public class RenderAssets {

    public static void load(RenderSettings renderSettings, LoadingScreen loadingScreen) {
        if (loadingScreen != null) loadingScreen.setProgress(0, 0.1f, "Loading...");
        AsyncTasks.submit(new AsyncTask() {

            @Override
            public void pre() {
                ItemModelManager.getInstance().release();
            }
            @Override
            public void post() {
                ItemModelManager.getInstance().redraw();
            }
            @Override
            public Void call() throws Exception {
                ItemModelManager.getInstance().reload();
                return null;
            }
            @Override
            public TaskType getType() {
                return TaskType.LOAD_TEXTURES;
            }  
        });
        AsyncTasks.submit(new AsyncTask() {

            @Override
            public void pre() {
                BlockModelManager.getInstance().release();
            }
            @Override
            public void post() {
            }
            @Override
            public Void call() throws Exception {
                BlockModelManager.getInstance().reload();
                return null;
            }
            @Override
            public TaskType getType() {
                return TaskType.LOAD_TEXTURES;
            }  
        });
        AsyncTasks.submit(new AsyncTask() {

            @Override
            public void pre() {
                EntityModelManager.getInstance().release();
            }
            @Override
            public void post() {
                EntityModelManager.getInstance().redraw();
            }
            @Override
            public Void call() throws Exception {
                EntityModelManager.getInstance().reload();
                return null;
            }
            @Override
            public TaskType getType() {
                return TaskType.LOAD_TEXTURES;
            }  
        });
        TextureArray[] arrays = TextureArrays.init();
        if (renderSettings != null)
            TextureArrays.blockTextureArray.setAnisotropicFiltering(renderSettings.anisotropicFiltering);
        else {

            TextureArrays.blockTextureArray.setAnisotropicFiltering(16);
        }
        if (TextureArrays.blockTextureArrayGL != null)
        TextureArrays.blockTextureArrayGL.internalFormat = Engine.useSRGBTextures()?GL21.GL_SRGB8_ALPHA8:GL11.GL_RGBA8;
        for (int i = 0; i < arrays.length; i++) {
            final TextureArray arr = arrays[i];
            AsyncTasks.submit(new AsyncTask() {
                @Override
                public void pre() {
                    try {
                        arr.preUpdate();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void post() {
                    arr.postUpdate();
                }
                @Override
                public Void call() throws Exception {
                    try {
                        arr.load();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
                @Override
                public TaskType getType() {
                    return TaskType.LOAD_TEXTURES;
                }
            });
        }
        while(!AsyncTasks.completeTasks()) {
            int nToLoad = 0;
            int nLoaded = 0;
            for (int i = 0; i < arrays.length; i++) {
                nToLoad+=arrays[i].numTotalTextures;
                nLoaded+=arrays[i].numLoaded;
                nLoaded+=arrays[i].numUploaded;
//                System.out.println(arrays[i].numLoaded+","+arrays[i].numUploaded+","+arrays[i].numTotalTextures);
            }
            float pr = nLoaded / (float) nToLoad;
            if (loadingScreen != null) loadingScreen.setProgress(1, pr, "Loading...");
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
        }
        postResourceLoad();
    }

    public static void reload() {
        if (TextureArrays.blockTextureArrayGL != null)
        TextureArrays.blockTextureArrayGL.internalFormat = Engine.useSRGBTextures()?GL21.GL_SRGB8_ALPHA8:GL11.GL_RGBA8;
        TextureArray[] arrays = TextureArrays.allArrays;
        for (int i = 0; i < arrays.length; i++) {
            arrays[i].reload();
        }
        SingleBlockRenderAtlas.getInstance().reset();
        postResourceLoad();
    }

    public static void destroy() {
        TextureArray[] arrays = TextureArrays.allArrays;
        for (int i = 0; arrays != null && i < arrays.length; i++) {
            if (arrays[i] != null) {
                arrays[i].destroy();
                arrays[i] = null;
            }
        }
    }

    private static void postResourceLoad() {
        if (Engine.worldRenderer != null)
            Engine.worldRenderer.onResourceReload();
        if (Engine.itemRender != null)
            Engine.itemRender.onResourceReload();
    }

}
