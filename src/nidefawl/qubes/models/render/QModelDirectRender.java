package nidefawl.qubes.models.render;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.models.qmodel.*;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.ShaderCompileError;
import nidefawl.qubes.texture.TMgr;

public class QModelDirectRender extends QModelRender {
    public ArrayList<QModelModelObject> rendered = Lists.newArrayList();
    public List<QModelModelObject> subLists = Lists.newArrayList();
    public static class QModelModelObject {
        public QModelObject modelObject;
        public ModelQModel model;
        public BufferedMatrix modelMat;
        private BufferedMatrix normalMat;
        public QModelModelObject(ModelQModel model, QModelObject modelObject, BufferedMatrix modelMat, BufferedMatrix normalMat) {
            this.modelMat = new BufferedMatrix();
            this.normalMat = new BufferedMatrix();
            this.modelMat.load(modelMat);
            this.modelMat.update();
            this.normalMat.load(normalMat);
            this.normalMat.update();
            this.model = model;
            this.modelObject = modelObject;
        }
    }

    private Shader shaderModel;

    boolean startup = true;
    static public class QModelRenderSubList {
        
    }
    
    @Override
    public void initShaders() {
        try {
            pushCurrentShaders();
            AssetManager assetMgr = AssetManager.getInstance();
            Shader newShaderModel = assetMgr.loadShader(this, "model/model_viewer");
            popNewShaders();
            this.shaderModel = newShaderModel;
            this.shaderModel.enable();
            this.shaderModel.setProgramUniform1i("tex0", 0);
            Shader.disable();
            startup = false;
        } catch (ShaderCompileError e) {
            releaseNewShaders();
            throw e;
        }
        startup = false;
    
    }
    public void reset() {
        this.subLists.clear();
    }

    @Override
    public void render(float fTime) {

        Engine.bindVAO(GLVAO.vaoModel);
        this.shaderModel.enable();

        rendered.clear();
        for (QModelModelObject obj : subLists) {
            this.shaderModel.setProgramUniformMatrix4("model_matrix", false, obj.modelMat.get(), false);
            this.shaderModel.setProgramUniformMatrix4("normal_matrix", false, obj.normalMat.get(), false);
            for (QModelGroup grp : obj.modelObject.listGroups) {

                QModelTexture tex = grp.material.getBoundTexture();
                if (tex != null) {
                    GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, tex.get());
                } else {
                    GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, TMgr.getEmptyWhite());
                }
                renderGroup( obj.model, obj.modelObject, grp, fTime);
            }
            rendered.add(obj);
        }
    }

    @Override
    public void addObject(ModelQModel model, QModelObject modelObject) {
//        System.out.println("add model "+model.getName()+" obj "+modelObject.name+", parent "+modelObject.parent_name+","+modelObject.getAttachmentBone()+","+modelObject.getAttachementNode());
        subLists.add(new QModelModelObject(model, modelObject, this.modelMat, this.normalMat));
    }
    @Override
    public void init() {
        initShaders();
    }

}
