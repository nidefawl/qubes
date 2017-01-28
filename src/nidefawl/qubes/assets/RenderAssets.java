package nidefawl.qubes.assets;

import nidefawl.qubes.async.AsyncTask;
import nidefawl.qubes.async.AsyncTasks;
import nidefawl.qubes.async.AsyncTask.TaskType;
import nidefawl.qubes.config.RenderSettings;
import nidefawl.qubes.gui.LoadingScreen;
import nidefawl.qubes.models.BlockModelManager;
import nidefawl.qubes.models.EntityModelManager;
import nidefawl.qubes.models.ItemModelManager;
import nidefawl.qubes.texture.array.*;

public class RenderAssets {

    public static void load(RenderSettings renderSettings, LoadingScreen loadingScreen) {
        if (loadingScreen != null) loadingScreen.setProgress(0, 0.8f, "Loading... Item Models");
        ItemModelManager.getInstance().reload();
        if (loadingScreen != null) loadingScreen.setProgress(0, 0.9f, "Loading... Block Models");
        BlockModelManager.getInstance().reload();
        if (loadingScreen != null) loadingScreen.setProgress(0, 1f, "Loading... Entity Models");
        EntityModelManager.getInstance().reload();
        if (loadingScreen != null) loadingScreen.setProgress(0, 1f, "Loading... Item Textures");
        BlockTextureArray.getInstance().setAnisotropicFiltering(renderSettings.anisotropicFiltering);
        TextureArray[] arrays = {
                ItemTextureArray.getInstance(),
                BlockNormalMapArray.getInstance(),
                BlockTextureArray.getInstance(),
                NoiseTextureArray.getInstance(),
        };
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
            float pr = 0;
            for (int i = 0; i < arrays.length; i++) {
                pr+=arrays[i].getProgress();
            }
            pr/=(float)arrays.length;
            if (loadingScreen != null) loadingScreen.setProgress(1, pr, "Loading...");
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
        }
    }

}
