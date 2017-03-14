package nidefawl.qubes.render.impl.gl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import java.util.HashMap;
import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.entity.Player;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.input.DigController;
import nidefawl.qubes.item.*;
import nidefawl.qubes.models.EntityModel;
import nidefawl.qubes.models.ItemModel;
import nidefawl.qubes.models.qmodel.QModelProperties;
import nidefawl.qubes.models.render.QModelBatchedRender;
import nidefawl.qubes.path.PathPoint;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.render.AbstractRenderer;
import nidefawl.qubes.render.RenderersGL;
import nidefawl.qubes.render.WorldRenderer;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.Color;
import nidefawl.qubes.util.EResourceType;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.*;
import nidefawl.qubes.vr.VR;
import nidefawl.qubes.world.World;

public class WorldRendererGL extends WorldRenderer {

    private boolean                          startup    = true;

    public int                               texWaterNoise;
    private int                              texNoise3D;

    public Shader                            terrainShader;
    public Shader                            terrainShaderFar;
    public Shader                            waterShader;
    public Shader                            shaderModelVoxel;

    public Shader                            shaderModelfirstPerson;

    private Shader                           shaderZPre;

    public void initShaders() {
        try {
            pushCurrentShaders();
            AssetManager assetMgr = AssetManager.getInstance();
            Shader new_waterShader = assetMgr.loadShader(this, "terrain/water");
            Shader terrain = assetMgr.loadShader(this, "terrain/terrain", new IShaderDef() {
                @Override
                public String getDefinition(String define) {
                    if ("NORMAL_MAPPING".equals(define)) {
                        if (isNormalMappingActive()) {
                        return "#define NORMAL_MAPPING";
                        }
                    }
                    return null;
                }
                
            });
            Shader terrainFar = assetMgr.loadShader(this, "terrain/terrain", new IShaderDef() {
                @Override
                public String getDefinition(String define) {
                    if ("FAR_BLOCKFACE".equals(define)) {
                        return "#define FAR_BLOCKFACE";
                    }
                    return null;
                }
                
            });

            Shader shaderZPre = assetMgr.loadShader(this, "terrain/terrain_pre");
            Shader modelVoxel = assetMgr.loadShader(this, "terrain/terrain", new IShaderDef() {
                @Override
                public String getDefinition(String define) {
                    if ("MODEL_RENDER".equals(define)) {
                        return "#define MODEL_RENDER";
                    }
                    return null;
                }
                
            });
            Shader shaderModelfirstPerson = assetMgr.loadShader(this, "model/firstperson");
            popNewShaders();
            this.terrainShader = terrain;
            this.terrainShaderFar = terrainFar;
            this.waterShader = new_waterShader;
            this.shaderModelVoxel = modelVoxel;
            this.shaderModelfirstPerson = shaderModelfirstPerson;
            this.shaderZPre = shaderZPre;
            this.shaderZPre.enable();
            this.shaderZPre.setProgramUniform1i("blockTextures", 0);
            
            this.terrainShader.enable();
            this.terrainShader.setProgramUniform1i("blockTextures", 0);
            this.terrainShader.setProgramUniform1i("noisetex", 1);
            this.terrainShader.setProgramUniform1i("normalTextures", 2);
            

            this.terrainShaderFar.enable();
            this.terrainShaderFar.setProgramUniform1i("blockTextures", 0);
            this.terrainShaderFar.setProgramUniform1i("noisetex", 1);
            this.terrainShaderFar.setProgramUniform1i("normalTextures", 2);

            this.waterShader.enable();
            this.waterShader.setProgramUniform1i("blockTextures", 0);
            this.waterShader.setProgramUniform1i("waterNoiseTexture", 1);
            this.shaderModelVoxel.enable();
            this.shaderModelVoxel.setProgramUniform1i("blockTextures", 0);
            this.shaderModelVoxel.setProgramUniform1i("waterNormals", 1);
            Shader.disable();
            startup = false;
        } catch (ShaderCompileError e) {
            releaseNewShaders();
            System.out.println("shader " + e.getName() + " failed to compile");
            System.out.println(e.getLog());
            if (startup) {
                throw e;
            } else {
                Game.instance.addDebugOnScreen("\0uff3333shader " + e.getName() + " failed to compile");
            }
        }
        startup = false;
    }
    
    public void init() {
        initShaders();
        AssetTexture tex = AssetManager.getInstance().loadPNGAsset("textures/water/noise.png");
        texWaterNoise = TextureManager.getInstance().makeNewTexture(tex, true, true, 10);

        AssetTexture t = AssetManager.getInstance().loadPNGAsset("textures/tex10.png");
        this.texNoise3D = TextureManager.getInstance().makeNewTexture(t, true, true, 0);
    }

    public void renderWorld(World world, float fTime) {


        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.start("terrain");
        

        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("skybox");
//
//
//        if (GLDebugTextures.isShow()) {
////            GLDebugTextures.readTexture(false, "Sky", "skyCubemap", Engine.skyRenderer.fbSkybox.getTexture(0), 1);
//            GLDebugTextures.readTexture(true, "Sky", "skyColor", Engine.getSceneFB().getTexture(0), 1);
//        }
        
        GL.bindTexture(GL_TEXTURE0, GL30.GL_TEXTURE_2D_ARRAY, TMgr.getBlocks());
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, TMgr.getNoise());
        GL.bindTexture(GL_TEXTURE2, GL30.GL_TEXTURE_2D_ARRAY, TMgr.getNormals());
        
        
        //TODO check what effect blendign has on alpha testing (writing to z-buffer)
//        Engine.setBlend(true);
//        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("pre renderMain");
//        boolean zPre = false; //sucks, not faster
//        if (zPre) {
//            glColorMask(false, false, false, false);
//            glDepthFunc(GL_LESS);
//            shaderZPre.enable();
//            Engine.regionRenderer.renderMainPre(world, fTime, this);
//            glColorMask(true, true, true, true);
//            glDepthFunc(GL_EQUAL);
//            Engine.enableDepthMask(false);
//            terrainShader.enable();
//            Engine.regionRenderer.renderMainPost(world, fTime, this);
//            glDepthFunc(GL_LEQUAL);
//            Engine.enableDepthMask(true);
//        } else {
            terrainShader.enable();
            Engine.regionRenderer.renderMain(world, fTime);
//        }
//        Engine.regionRenderer.renderRegions(world, fTime, PASS_LOD, 0, Frustum.FRUSTUM_INSIDE);
        rendered = Engine.regionRenderer.rendered;
        
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("renderFirstPass");
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.end();
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.start("particles");
//
//        
//        Engine.setBlend(false);
//        shaderModelVoxel.enable();
//        renderVoxModels(shaderModelVoxel, PASS_SOLID, fTime);
//        Shader.disable();
//        if (this.qmodel != null)
//            this.qmodel.animate(fTime);
        Engine.particleRenderer.renderParticles(world, PASS_SOLID, fTime);
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.end();
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.start("entities");
        renderEntities(world, PASS_SOLID, fTime, null, 0);
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.end();
        

    }


    
    /**
     * @param world 
     * @param pass
     * @param fTime 
     */

    public void renderEntities(World world, int pass, float fTime, Shader shader, int shadowVP) {
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.start("renderEntities");
//        if ((GameBase.ticksran/40f)%1.0f<0.5f) {
            //TODO: implement
            if (shader == Shaders.normals) {
                if (GPUProfiler.PROFILING_ENABLED)
                    GPUProfiler.end();
                return;
            }
            renderEntitiesBatched(world, pass, fTime, shader, shadowVP);
//        } else {
//            renderEntitiesSingle(world, pass, fTime, shader);    
//        }
        
        
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("renderEntities");
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.end();
    }
    QModelProperties modelProperties = new QModelProperties();
    public void renderEntitiesBatched(World world, int pass, float fTime, Shader sg, int shadowVP) {
        List<Entity> ents = world.getEntityList();
        int size = ents.size();
        if (size == 0) {
            return;
        }
        QModelBatchedRender modelRender = Engine.renderBatched;

        
        modelRender.setPass(pass, shadowVP);
        // Single Model render logic, move into BatchedRenderer class
        boolean needStateSetup = true;
        boolean mappedBuffer = false;
        if (pass == PASS_SOLID) {
            modelRender.reset();
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
                if (!mappedBuffer) {
                    mappedBuffer = true;
                    modelRender.begin();
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
                
                modelRender.setModel(model.model);
                model.setActions(modelRender, renderProps, GameBase.absTime, fTime);
                model.setPose(modelRender, renderProps, GameBase.absTime, fTime);
                if (Game.GL_ERROR_CHECKS)
                    Engine.checkGLError("setPose");
            }
            if (mappedBuffer) {
                modelRender.end();
            }
        }

        modelRender.render(fTime);
    }
//    void _renderBatch(EntityModel model, Shader shader, int pass, int shadowVP, boolean needStateSetup) {
//        if (Game.GL_ERROR_CHECKS)
//            Engine.checkGLError("_renderBatch pre");
//        if (needStateSetup) {
//            shader.enable();
//            if (pass == PASS_SHADOW_SOLID)
//                shader.setProgramUniform1i("shadowSplit", shadowVP);
//            model.getModel().bindTextures(0);
//            Engine.bindVAO(GLVAO.vaoModelGPUSkinned);
//        }
//        ((ModelRigged) model.getModel()).renderRestModel(0, 0, rend.getNumModels());
//        if (Game.GL_ERROR_CHECKS)
//            Engine.checkGLError("_renderBatch post");
//    }
    public void renderTransparent(World world, float fTime) {
//      Engine.setBlend(true);
//        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        waterShader.enable();
//        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, this.texWaterNoise);
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, TMgr.getNoise());
        RenderersGL.regionRenderer.renderRegions(world, fTime, PASS_TRANSPARENT, 0, Frustum.FRUSTUM_INSIDE);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("renderSecondPass");

        Shader.disable();
    }
    //MOve somewhere else?!
    public void renderFirstPerson(World world, float fTime) {
        Player p = Game.instance.getPlayer();
        if (p == null) {
            return;
        }
        float modelScale = 1 / 2.7f;
        float f1=0;
        DigController dig = Game.instance.dig;
        float roffsetPArm = p.armOffsetPitchPrev+(p.armOffsetPitch-p.armOffsetPitchPrev)*fTime;
        float roffsetYArm = p.armOffsetYawPrev+(p.armOffsetYaw-p.armOffsetYawPrev)*fTime;
        float swingF = p.getSwingProgress(fTime);

        BaseStack stack = p.getActiveItem(0);
        BlockStack bstack = Game.instance.selBlock;
        if (true) { //TODO: fix me (Normal mat)

            BufferedMatrix mat2 = Engine.getTempMatrix2();
            mat2.load(Engine.getMatSceneV());
            mat2.transpose();
            mat2.update();
            UniformBuffer.setNormalMat(mat2.get());
        }
        if (stack != null) {
            ItemStack itemstack = (ItemStack) stack;
            Item item = itemstack.getItem();
            ItemModel model = item.getItemModel();
            if (model != null) {
                //        mat.translate(0, 3, -4);
                BufferedMatrix mat = Engine.getTempMatrix();
                mat.setIdentity();
                Engine.camera.addCameraShake(mat);
                float angleX = -110;
                float angleY = 180;
                float angleZ = 0;
                float swingProgress = dig.getSwingProgress(fTime);
                float f17 = GameMath.sin(GameMath.PI*swingProgress);
                float f23 = GameMath.sin(GameMath.sqrtf(swingProgress)*GameMath.PI);
                mat.translate(-f23*0.25f, f17*0.1f+ GameMath.sin(GameMath.sqrtf(swingProgress)*GameMath.PI*2.0f)*0.3f, f17*-0.11f);
                float f7 = 0.8f;
                mat.translate(0.7F * f7, -0.55F * f7 - (1.0F - f1) * 0.6F, -1.2F * f7);
                float f18 = GameMath.sin(swingProgress*swingProgress*GameMath.PI);
                float f24 = GameMath.sin(GameMath.sqrtf(swingProgress)*GameMath.PI);
                mat.rotate(angleY * GameMath.PI_OVER_180, 0, 1, 0);
                mat.rotate(angleZ * GameMath.PI_OVER_180, 0, 0, 1);
                mat.rotate(angleX * GameMath.PI_OVER_180, 1, 0, 0);
                mat.rotate(f18*-17f * GameMath.PI_OVER_180, 0, 1, 0);
                mat.rotate(f24*16f * GameMath.PI_OVER_180, 0, 0, 1);
                mat.rotate(f24*40f * GameMath.PI_OVER_180, 1, 0, 0);
                mat.scale(modelScale);
//                mat.translate(0, -1, -4);
                mat.rotate(90 * GameMath.PI_OVER_180, 1, 0, 0);
                mat.rotate(-180 * GameMath.PI_OVER_180, 0, 1, 0);
                mat.update();

                if (Game.VR_SUPPORT) {
                    mat.setIdentity();
                    mat.load(Engine.getMatSceneV());
                    float t = -0.5f;
                    Matrix4f.mul(mat, VR.poseMatrices[3], mat);
//                    mat.translate(t,t,t);
                    mat.scale(modelScale);
                    mat.scale(0.4f);
                    mat.translate(0, 0, 2);
                    mat.rotate(-90 * GameMath.PI_OVER_180, 1, 0, 0);
//                    mat.rotate(-180 * GameMath.PI_OVER_180, 0, 1, 0);
                    
                    mat.update();
                    BufferedMatrix mat2 = Engine.getTempMatrix2();
                    mat2.load(mat);
//                    mat2.clearTranslation();
                    mat2.invert().transpose();
                    mat2.update();
                    UniformBuffer.setNormalMat(mat2.get());
                }
                
                shaderModelfirstPerson.enable();
                shaderModelfirstPerson.setProgramUniformMatrix4("model_matrix", false, mat.get(), false);
                model.loadedModels[0].bindTextures(0);
                //first person needs clear depth buffer, move somewhere else
//                    glDisable(GL_DEPTH_TEST);
                Engine.bindVAO(GLVAO.vaoModel);
                model.loadedModels[0].render(0, 0, Game.ticksran+fTime);
//                    glEnable(GL_DEPTH_TEST);
            }
            return;
        } else if (bstack!=null&&bstack.id>0) {
            
            Shaders.singleblock3D.enable();
            BufferedMatrix mat = Engine.getTempMatrix();
            mat.setIdentity();
            Engine.camera.addCameraShake(mat);
            mat.rotate((p.pitch - roffsetPArm) * 0.12f * GameMath.PI_OVER_180, 1, 0, 0);
            mat.rotate((p.yaw - roffsetYArm) * 0.12f * GameMath.PI_OVER_180, 0, 1, 0);
            float fsqrt = GameMath.sqrtf(swingF) * GameMath.PI;
            if (swingF > 0) {
                float scale = 1.5f;
                mat.translate(
                        scale*-GameMath.sin(fsqrt) * 0.4F, 
                        scale*GameMath.sin(fsqrt * 2.0F) * 0.17F, 
                        scale*-GameMath.sin(swingF * GameMath.PI) * 0.2F
                        );
            }
            mat.translate(0.8f, -0.6f, -1f);
            mat.rotate(50F * GameMath.PI_OVER_180, 0.0F, 1.0F, 0.0F);
            if (swingF > 0) {
                float f18 = GameMath.sin(swingF * swingF * GameMath.PI);
                float f24 = GameMath.sin(fsqrt);
                mat.rotate(f18 * 18F * GameMath.PI_OVER_180, 0.0F, 1.0F, 0.0F);
                mat.rotate(f24 * 15f * GameMath.PI_OVER_180, 0.0F, 0.0F, 1.0F);
                mat.rotate(-GameMath.sin(fsqrt) * -33F * GameMath.PI_OVER_180, 1.0F, 0.0F, 0.0F);
            }
            mat.scale(0.5f);
            mat.update();
            if (Game.VR_SUPPORT) {
                mat.setIdentity();
                mat.load(Engine.getMatSceneV());
                float t = -0.5f;
                Matrix4f.mul(mat, VR.poseMatrices[3], mat);
//                mat.translate(t,t,t);
                mat.scale(0.4f);
                
                mat.update();
                BufferedMatrix mat2 = Engine.getTempMatrix2();
                mat2.load(mat);
//                mat2.clearTranslation();
                mat2.invert().transpose();
                mat2.update();
                UniformBuffer.setNormalMat(mat2.get());
            }
//            glDisable(GL11.GL_CULL_FACE); //face culling is off already
            Shaders.singleblock3D.setProgramUniformMatrix4("in_modelMatrix", false, mat.get(), false);
            GL.bindTexture(GL_TEXTURE0, GL30.GL_TEXTURE_2D_ARRAY, TMgr.getBlocks());
            Engine.blockDraw.doRender(bstack.getBlock(), 0, null);
//            glEnable(GL11.GL_CULL_FACE);
        }
        UniformBuffer.setNormalMat(Engine.getMatSceneNormal().get());
        
        
           
    }

    public void renderNormals(World world, float fTime) {
        if (Shaders.normals != null) {            
            Shaders.normals.enable();
            Engine.lineWidth(3.0F);
            Engine.checkGLError("glLineWidth");
            RenderersGL.regionRenderer.renderRegions(world, fTime, PASS_SOLID, 0, Frustum.FRUSTUM_INSIDE);
//            Engine.regionRenderer.renderRegions(world, fTime, PASS_TRANSPARENT, 0, Frustum.FRUSTUM_INSIDE);
            RenderersGL.regionRenderer.renderRegions(world, fTime, PASS_LOD, 0, Frustum.FRUSTUM_INSIDE);
            Shaders.normals.enable();
            renderEntities(world, PASS_SOLID, fTime, Shaders.normals, 0);
            BufferedMatrix mat = Engine.getIdentityMatrix();
            Shaders.normals.setProgramUniformMatrix4("model_matrix", false, mat.get(), false);
            
//            glLineWidth(2.0F);

//            Shaders.colored.enable();
//            Engine.regionRenderer.renderDebug(world, fTime);
            Shader.disable();
        }
    }

    public void renderTerrainWireFrame(World world, float fTime) {
        Shaders.wireframe.enable();
        Engine.pxStack.push(Engine.GLOBAL_OFFSET.x, Engine.GLOBAL_OFFSET.y, Engine.GLOBAL_OFFSET.z);
        Shaders.wireframe.setProgramUniform1i("num_vertex", 4);
        Shaders.wireframe.setProgramUniform1f("thickness", 0.2f);
        Shaders.wireframe.setProgramUniform1f("maxDistance", 110);
        Shaders.wireframe.setProgramUniform4f("linecolor", 1, 0.2f, 0.2f, 1);
        RenderersGL.regionRenderer.renderRegions(world, fTime, 0, 0, Frustum.FRUSTUM_INSIDE);
//        Shaders.wireframe.setProgramUniform4f("linecolor", 1, 1, 0.2f, 1);
//        Engine.regionRenderer.renderRegions(world, fTime, 1, 0, Frustum.FRUSTUM_INSIDE);
//        Shaders.wireframe.setProgramUniform4f("linecolor",  1, 0.2f, 0.2f, 1);
//        Engine.regionRenderer.renderRegions(world, fTime, PASS_LOD, 0, Frustum.FRUSTUM_INSIDE);
        Engine.pxStack.pop();
        Shader.disable();
    }

    public void renderDebugBB(World world, float fTime) {
        if (!this.debugBBs.isEmpty()) {
            glPushAttrib(-1);
//            Engine.setBlend(true);
//            Engine.setBlend(false);
//            glDepthFunc(GL_LEQUAL);
//          glDisable(GL_DEPTH_TEST);
            Shaders.colored3D.enable();
            for (Integer i : debugBBs.keySet()) {
                AABB bb = debugBBs.get(i);
                int iColor = GameMath.randomI(i*19)%33;
                iColor = Color.HSBtoRGB(iColor/33F, 0.8F, 1.0F);
                
                float fMinX = (float) bb.minX;
                float fMinY = (float) bb.minY;
                float fMinZ = (float) bb.minZ;
                float fMaxX = (float) bb.maxX;
                float fMaxY = (float) bb.maxY;
                float fMaxZ = (float) bb.maxZ;
                
                Engine.lineWidth(4.0F);
                float ext = 1/32F;
                float zero = -ext;
                float one = 1+ext;
                Tess.instance.setColor(iColor, 255);
                Tess.instance.add(fMinX, fMinY, fMinZ);
                Tess.instance.add(fMaxX, fMinY, fMinZ);
                Tess.instance.add(fMaxX, fMinY, fMaxZ);
                Tess.instance.add(fMinX, fMinY, fMaxZ);
                Tess.instance.add(fMinX, fMinY, fMinZ);
                Tess.instance.add(fMinX, fMaxY, fMinZ);
                Tess.instance.add(fMaxX, fMaxY, fMinZ);
                Tess.instance.add(fMaxX, fMaxY, fMaxZ);
                Tess.instance.add(fMinX, fMaxY, fMaxZ);
                Tess.instance.add(fMinX, fMaxY, fMinZ);
                Tess.instance.draw(GL_LINE_STRIP);
                
                Tess.instance.setColor(iColor, 255);
                Tess.instance.add(fMinX, fMinY, fMaxZ);
                Tess.instance.add(fMinX, fMaxY, fMaxZ);
                Tess.instance.add(fMaxX, fMinY, fMaxZ);
                Tess.instance.add(fMaxX, fMaxY, fMaxZ);
                Tess.instance.add(fMaxX, fMinY, fMinZ);
                Tess.instance.add(fMaxX, fMaxY, fMinZ);
                Tess.instance.draw(GL_LINES);
            }
            Shader.disable();
            glPopAttrib();
        }
        if (!this.debugPaths.isEmpty()) {
            glPushAttrib(-1);
            Engine.setBlend(false);
//            glDepthFunc(GL_LEQUAL);
//          glDisable(GL_DEPTH_TEST);
            Shaders.colored3D.enable();
            Engine.lineWidth(4.0F);
            for (Integer i : debugPaths.keySet()) {
                List<PathPoint> bb = debugPaths.get(i);
                int iColor = GameMath.randomI(i*19)%33;
                iColor = Color.HSBtoRGB(iColor/33F, 0.8F, 1.0F);
                Tess.instance.setColor(iColor, 255);
                for (PathPoint p : bb) {
                    Tess.instance.add(p.x+0.5f, p.y+0.1f, p.z+0.5f);
                }
                Tess.instance.draw(GL11.GL_LINE_STRIP);
            }
            Shader.disable();
            glPopAttrib();
        }
    
    }

    public void resize(int displayWidth, int displayHeight) {
    }

    public void tickUpdate() {
    }
}
