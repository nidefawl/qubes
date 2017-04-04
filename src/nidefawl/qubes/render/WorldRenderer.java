package nidefawl.qubes.render;

import java.util.HashMap;
import java.util.List;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.item.Item;
import nidefawl.qubes.item.ItemStack;
import nidefawl.qubes.models.EntityModel;
import nidefawl.qubes.models.ItemModel;
import nidefawl.qubes.models.qmodel.QModelProperties;
import nidefawl.qubes.models.render.QModelBatchedRender;
import nidefawl.qubes.models.render.impl.vk.QModelBatchedRenderVK;
import nidefawl.qubes.path.PathPoint;
import nidefawl.qubes.render.impl.gl.WorldRendererGL;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.util.IRenderComponent;
import nidefawl.qubes.vec.AABB;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.World;

public abstract class WorldRenderer extends AbstractRenderer {
    public static final int NUM_PASSES        = 4;
    public static final int PASS_SOLID        = 0;
    public static final int PASS_TRANSPARENT  = 1;
    public static final int PASS_SHADOW_SOLID = 2;
    public static final int PASS_LOD          = 3;
    public static final String getPassName(int i) {
        switch (i) {
            case PASS_SOLID:
                return "Main";
            case PASS_TRANSPARENT:
                return "Transparent";
            case PASS_SHADOW_SOLID:
                return "Shadow";
            case PASS_LOD:
                return "LOD";
        }
        return "PASS_"+i;
    }
    public Vector3f           skyColor        = new Vector3f(0.43F, .69F, 1.F);
    public Vector3f           fogColor        = new Vector3f(0.7F, 0.82F, 1F);
    public HashMap<Integer, AABB> debugBBs = new HashMap<>();
    public HashMap<Integer, List<PathPoint>> debugPaths = new HashMap<>();

    public int                               rendered;
    final public static int HALF_EXTRA_RENDER = 0;
    final public static int EXTRA_RENDER = 1;//(HALF_EXTRA_RENDER*2)*(HALF_EXTRA_RENDER*2);
    final public static float RDIST = 4;
    
    public int getNumRendered() {
        return this.rendered;
    }


    public boolean isNormalMappingActive() {
        return Engine.RENDER_SETTINGS.normalMapping > 0 && !Game.VR_SUPPORT;
    }


    public abstract void initShaders();
    public abstract void tickUpdate();




    public abstract void onResourceReload();
    QModelProperties modelProperties = new QModelProperties();
    public void prepareEntitiesBatched(World world, float fTime) {
        QModelBatchedRender modelRender = Engine.renderBatched;
        modelRender.reset();
        List<Entity> ents = world.getEntityList();
        int size = ents.size();
        if (size == 0) {
            return;
        }
        for (int i = 0; i < size*EXTRA_RENDER; i++) {

            Entity e = ents.get(i/EXTRA_RENDER);
            if (e == Game.instance.getPlayer() && !Game.instance.thirdPerson)
                continue;


            BaseStack stack = e.getActiveItem(0);
            ItemModel itemmodel = null;
            if (stack != null && stack.isItem()) {
                ItemStack itemstack = (ItemStack) stack;
                Item item = itemstack.getItem();
                itemmodel = item.getItemModel();
            }
            QModelProperties renderProps = this.modelProperties;
            this.modelProperties.clear();
            if (itemmodel != null) {
                renderProps.setModelAtt(itemmodel.loadedModels[0]);
            } else {

                renderProps.setModelAtt(null);
            }
            Vector3f pos = e.getRenderPos(fTime);
            Vector3f rot = e.getRenderRot(fTime);
            EntityModel model = e.getEntityModel();
            renderProps.setPos(pos);
            renderProps.setRot(rot);
            renderProps.setEntity(e);
            e.adjustRenderProps(renderProps, fTime);
            
            model.setActions(modelRender, renderProps, GameBase.absTime, fTime);
            model.setPoseAndSubmit(modelRender, renderProps, GameBase.absTime, fTime);
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("setPose");
        }
    

        modelRender.upload(fTime);
    }


    public void onNormalMapSettingChanged() {
    }
}
