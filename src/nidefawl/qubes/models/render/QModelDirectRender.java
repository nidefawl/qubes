package nidefawl.qubes.models.render;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.models.EntityModel;
import nidefawl.qubes.models.qmodel.*;
import nidefawl.qubes.shader.IShaderDef;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.ShaderCompileError;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.vec.Matrix4f;

public class QModelDirectRender extends QModelRender {
    public List<QModelObject> subLists = Lists.newArrayList();

    private Shader shaderModel;

    boolean startup = true;
    
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
        this.shaderModel.setProgramUniformMatrix4("model_matrix", false, this.modelMat.get(), false);
        this.shaderModel.setProgramUniformMatrix4("normal_matrix", false, this.normalMat.get(), false);

        rendered.clear();
        for (QModelObject obj : subLists) {
            for (QModelGroup grp : obj.listGroups) {

                QModelTexture tex = grp.material.getBoundTexture();
                if (tex != null) {
                    GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, tex.get());
                } else {
                    GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, TMgr.getEmptyWhite());
                }
                renderGroup(this.model, obj, grp, fTime);
            }
            rendered.add(obj);
        }
    }

    @Override
    public void addObject(QModelObject model) {
        subLists.add(model);
    }
    @Override
    public void init() {
        initShaders();
    }

}
