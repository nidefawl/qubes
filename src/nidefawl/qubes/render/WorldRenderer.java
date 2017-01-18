package nidefawl.qubes.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.List;

import org.lwjgl.opengl.*;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.assets.AssetVoxModel;
import nidefawl.qubes.config.WorkingEnv;
import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.entity.Player;
import nidefawl.qubes.entity.PlayerSelf;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.input.DigController;
import nidefawl.qubes.item.*;
import nidefawl.qubes.models.*;
import nidefawl.qubes.models.qmodel.ModelRigged;
import nidefawl.qubes.models.qmodel.QModelProperties;
import nidefawl.qubes.models.qmodel.loader.ModelLoaderQModel;
import nidefawl.qubes.models.render.QModelBatchedRender;
import nidefawl.qubes.models.voxel.ModelVox;
import nidefawl.qubes.path.PathPoint;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.Color;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.*;
import nidefawl.qubes.vr.VR;
import nidefawl.qubes.world.World;

public class WorldRenderer extends AbstractRenderer {

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
//    public Vector3f           fogColor        = new Vector3f(0.7F, 0.82F, 1F);
    public Vector3f           fogColor        = new Vector3f(0.7F, 0.82F, 1F);
    public HashMap<Integer, AABB> debugBBs = new HashMap<>();
    public HashMap<Integer, List<PathPoint>> debugPaths = new HashMap<>();

    

    public int rendered;

    private boolean startup = true;


    public int                  texWaterNoise;
    public Shader       terrainShader;
    public Shader       terrainShaderFar;
    //    public Shader       skyShader;
    //    public Shader       skyCloudShader;
    public Shader       waterShader;
    public Shader       shaderModelVoxel;
    
    public Shader       shaderModelfirstPerson;
    
    //    private TesselatorState skybox1;
    //    private TesselatorState skybox2;
        private Shader shaderZPre;
    private Shader skybox;
    
    //  ModelVox vox;
//    private Shader skyShader2;

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
            Shader skybox = assetMgr.loadShader(this, "sky/skybox_cubemap");
//            Shader sky = assetMgr.loadShader(this, "sky/sky");
//            Shader sky2 = assetMgr.loadShader(this, "sky/clouds");
            popNewShaders();
            this.terrainShader = terrain;
            this.terrainShaderFar = terrainFar;
            this.skybox = skybox;
//            this.skyShader = sky;
//            this.skyShader2 = sky2;
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

//            this.skyShader2.enable();
//            this.skyShader2.setProgramUniform1i("tex0", 0);

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
    int idx = -1;
    private int texNoise3D;
    public void reloadModel() {
        File[] list = (new File(WorkingEnv.getAssetFolder(), "models")).listFiles(new FileFilter() {
            
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile()&&pathname.getName().endsWith(".vox");
            }
        });
        if (list != null&&list.length>0) {
            
            idx++;
            if (idx >= list.length) {
                idx = 0;
            }
            String mName = list[idx].getName();

//            AssetVoxModel asset = AssetManager.getInstance().loadVoxModel("models/" + mName);
//            if (this.vox != null)
//                this.vox.release();
//            vox = new ModelVox(asset);   
        }

//        AssetTexture tex = AssetManager.getInstance().loadPNGAsset("textures/normals_psd_03.png");
//        AssetTexture tex1 = AssetManager.getInstance().loadPNGAsset("textures/heightmap_03.png");
//        
//        if (tex.getWidth() == tex1.getWidth() && tex.getHeight() == tex1.getHeight()) {
//            for (int x = 0; x < tex.getWidth(); x++) {
//                for (int y = 0; y < tex.getHeight(); y++) {
//                    int idx = y*tex.getWidth()+x;
//                    int height = tex1.getData()[idx*4+0]&0xFF;
//                    height-=118;
//                    height*=2;
//                    if (height < 0 || height > 255)
//                    System.out.println(height);
//                    if (height > 255) {
//                        height = 255;
//                    }
//                    if (height < 0) height = 0;
//                    tex.getData()[idx*4+3] = (byte) height;
//                }
//            }
//            System.err.println("copied alpha channel");
//        } else {
//            System.err.println("!");
//        }
//        this.texNormalTest = TextureManager.getInstance().makeNewTexture(tex, true, true, 10);
        

//        byte[] rgba = tex.getData();
//        int[] rgba_int = TextureUtil.toIntRGBA(rgba);
//        int w = tex.getWidth();
//        int h = tex.getWidth();
//        for (int x = 0; x < 10; x++) {
//
//            for (int y = 0; y < 10; y++) {
//                int idx = y*w+x;
//                int pixel = rgba_int[idx];
//                String s = Integer.toHexString(pixel);
//                while(s.length()<8) {
//                    s = "0"+s;
//                }
//                float a = (((pixel>>24)&0xFF) / 255.0f);
//                float nx = (((pixel>>16)&0xFF) / 255.0f) * 2.0f - 1.0f;
//                float ny = (((pixel>>8)&0xFF) / 255.0f) * 2.0f - 1.0f;
//                float nz = (((pixel>>0)&0xFF) / 255.0f) * 2.0f - 1.0f;
//                System.out.println(""+x+","+y+" = 0x"+s+" = "+String.format("%.2f %.2f %.2f %.2f", nx, ny, nz, a));
////                break;
//            }
//        }
//        
        
    }

    public void init() {
//        skyColor = new Vector3f(0.43F, .69F, 1.F);
        initShaders();
//        skybox1 = new TesselatorState(GL15.GL_STATIC_DRAW);
//        skybox2 = new TesselatorState(GL15.GL_STATIC_DRAW);
        AssetTexture tex = AssetManager.getInstance().loadPNGAsset("textures/water/noise.png");
        texWaterNoise = TextureManager.getInstance().makeNewTexture(tex, true, true, 10);

        AssetTexture t = AssetManager.getInstance().loadPNGAsset("textures/tex10.png");
        this.texNoise3D = TextureManager.getInstance().makeNewTexture(t, true, true, 0);
//        reloadModel();
        
//
//        AssetTexture texNormalTest = AssetManager.getInstance().loadPNGAsset("textures/normalmaptest.png");
//        
//        this.texNormalTest = TextureManager.getInstance().makeNewTexture(texNormalTest, true, true, 10);

//        byte[] rgba = tex.getData();
//        int[] rgba_int = TextureUtil.toIntRGBA(rgba);
//        int w = tex.getWidth();
//        int h = tex.getWidth();
//        for (int x = 0; x < 10; x++) {
//
//            for (int y = 0; y < 10; y++) {
//                int idx = y*w+x;
//                int pixel = rgba_int[idx];
//                String s = Integer.toHexString(pixel);
//                while(s.length()<8) {
//                    s = "0"+s;
//                }
//                float nx = (((pixel>>16)&0xFF) / 255.0f) * 2.0f - 1.0f;
//                float ny = (((pixel>>8)&0xFF) / 255.0f) * 2.0f - 1.0f;
//                float nz = (((pixel>>0)&0xFF) / 255.0f) * 2.0f - 1.0f;
//                System.out.println(""+x+","+y+" = 0x"+s+" = "+String.format("%.2f %.2f %.2f", nx, ny, nz));
//            }
//        }
//        
//        AssetVoxModel asset = AssetManager.getInstance().loadVoxModel("models/dragon.vox");
//        if (this.vox != null) this.vox.release();
//        vox = new ModelVox(asset);
        reloadModel();

    }

    public void renderWorld(World world, float fTime) {

        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.start("sky+sun+clouds");
        Engine.enableDepthMask(false);
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.texNoise3D);
//        skyShader2.enable();
        Engine.drawFullscreenQuad();
        
//        skyShader.enable();
//        skybox1.bindAndDraw(GL_QUAD_STRIP);
//        skybox2.bindAndDraw(GL_QUADS);
//        if (Game.GL_ERROR_CHECKS)
//            Engine.checkGLError("skyShader.drawSkybox");
//        Shader.disable();
        skybox.enable();

        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_CUBE_MAP, Engine.skyRenderer.fbSkybox.getTexture(0));
        Engine.drawFullscreenQuad();
        Engine.enableDepthMask(true);
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.end();
        


        GLDebugTextures.readTexture("Sky", "skyColor", Engine.getSceneFB().getTexture(0));
        

        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("terrain shader");
        
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
            Engine.regionRenderer.renderMain(world, fTime, this);
//        }
//        Engine.regionRenderer.renderRegions(world, fTime, PASS_LOD, 0, Frustum.FRUSTUM_INSIDE);
        rendered = Engine.regionRenderer.rendered;
        
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("renderFirstPass");
//
//        
//        Engine.setBlend(false);
//        shaderModelVoxel.enable();
//        renderVoxModels(shaderModelVoxel, PASS_SOLID, fTime);
//        Shader.disable();
//        if (this.qmodel != null)
//            this.qmodel.animate(fTime);
        Engine.particleRenderer.renderParticles(world, PASS_SOLID, fTime);
        renderEntities(world, PASS_SOLID, fTime, null, 0);
        

    }


    public void renderVoxModels(Shader modelShader, int pass, float fTime) {
//        if (vox != null) {
//            GLVAO vao = GLVAO.vaoBlocks;
//            if (pass == PASS_SHADOW_SOLID) {
//                vao = GLVAO.vaoBlocksShadow;
//            }
//            Engine.bindVAO(vao);
//            BufferedMatrix mat = Engine.getTempMatrix();
//            BufferedMatrix mat2 = Engine.getTempMatrix2();
//            float modelScale = 1 / 16f;
//            mat.setIdentity();
//            mat.translate(mPos.x, mPos.y, mPos.z);
//            mat.translate(-Engine.GLOBAL_OFFSET.x, -Engine.GLOBAL_OFFSET.y, -Engine.GLOBAL_OFFSET.z);
//            float w = (vox.size.x);
//            float l = (vox.size.z);
//            mat.translate(w * 0.5f * modelScale, 0, l * 0.5f * modelScale);
//            mat.rotate((float) Math.toRadians(this.lastModelRot + (this.modelRot - this.lastModelRot) * fTime), 0, 1, 0);
//
//            mat.translate(-w * 0.5f * modelScale, 0, -l * 0.5f * modelScale);
//            mat.scale(modelScale);
//            mat.update();
//            modelShader.setProgramUniformMatrix4("model_matrix", false, mat.get(), false);
//            mat2.setIdentity();
//            mat2.rotate((float) Math.toRadians(this.lastModelRot + (this.modelRot - this.lastModelRot) * fTime), 0, 1, 0);
//            mat2.invert().transpose();
//            mat2.update();
//            UniformBuffer.setNormalMat(mat2.get());
//            vox.render(pass);
//            UniformBuffer.setNormalMat(Engine.getMatSceneNormal().get());
//        }
//    
        
        
    }
    
    /**
     * @param world 
     * @param pass
     * @param fTime 
     */

    final public static int HALF_EXTRA_RENDER = 0;
    final public static int EXTRA_RENDER = 1;//(HALF_EXTRA_RENDER*2)*(HALF_EXTRA_RENDER*2);
    final public static float RDIST = 4;
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
        Engine.regionRenderer.renderRegions(world, fTime, PASS_TRANSPARENT, 0, Frustum.FRUSTUM_INSIDE);
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
        this.modelRot=this.lastModelRot=4;
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
            glLineWidth(3.0F);
            Engine.checkGLError("glLineWidth");
            Engine.regionRenderer.renderRegions(world, fTime, PASS_SOLID, 0, Frustum.FRUSTUM_INSIDE);
//            Engine.regionRenderer.renderRegions(world, fTime, PASS_TRANSPARENT, 0, Frustum.FRUSTUM_INSIDE);
            Engine.regionRenderer.renderRegions(world, fTime, PASS_LOD, 0, Frustum.FRUSTUM_INSIDE);
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
        Engine.regionRenderer.renderRegions(world, fTime, 0, 0, Frustum.FRUSTUM_INSIDE);
//        Shaders.wireframe.setProgramUniform4f("linecolor", 1, 1, 0.2f, 1);
//        Engine.regionRenderer.renderRegions(world, fTime, 1, 0, Frustum.FRUSTUM_INSIDE);
//        Shaders.wireframe.setProgramUniform4f("linecolor",  1, 0.2f, 0.2f, 1);
//        Engine.regionRenderer.renderRegions(world, fTime, PASS_LOD, 0, Frustum.FRUSTUM_INSIDE);
        Engine.pxStack.pop();
        Shader.disable();
    }
    
    public int getNumRendered() {
        return this.rendered;
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
                
                glLineWidth(4.0F);
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
            glLineWidth(4.0F);
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
        float ext = 1/32F;
        float zero = -ext;
        float one = 1+ext;
        Tess tesselator = Tess.instance;
//        tesselator.setColorRGBAF(1, 1, 1, 0.2F);
//        tesselator.add(zero, zero, zero);
//        tesselator.add(one, zero, zero);
//        tesselator.add(one, one, zero);
//        tesselator.add(zero, one, zero);
//        tesselator.add(zero, one, one);
//        tesselator.add(one, one, one);
//        tesselator.add(one, zero, one);
//        tesselator.add(zero, zero, one);
//        tesselator.add(one, zero, one);
//        tesselator.add(one, one, one);
//        tesselator.add(one, one, zero);
//        tesselator.add(one, zero, zero);
//        tesselator.add(zero, one, one);
//        tesselator.add(zero, zero, one);
//        tesselator.add(zero, zero, zero);
//        tesselator.add(zero, one, zero);
//        tesselator.add(zero, zero, one);
//        tesselator.add(one, zero, one);
//        tesselator.add(one, zero, zero);
//        tesselator.add(zero, zero, zero);
//        tesselator.add(one, one, one);
//        tesselator.add(zero, one, one);
//        tesselator.add(zero, one, zero);
//        tesselator.add(one, one, zero);
//        tesselator.draw(GL_QUADS, highlightCube);
//        tesselator.resetState();

        
/*
        int scale = (int) (Engine.zfar / 1.43F);
        int x = -scale;
        int y = -scale / 16;
        int z = -scale;
        int x2 = scale;
        int y2 = scale / 16;
        int z2 = scale;
        int rgbai = 0;
        rgbai = ((int) (fogColor.x * 255.0F)) << 16 | ((int) (fogColor.y * 255.0F)) << 8 | ((int) (fogColor.z * 255.0F));
        //      Shaders.colored.enable();
        tesselator.setColor(rgbai, 255);
        tesselator.add(x, y2, z);
        tesselator.add(x, y, z);
        tesselator.add(x2, y2, z);
        tesselator.add(x2, y, z);
        tesselator.add(x2, y2, z2);
        tesselator.add(x2, y, z2);
        tesselator.add(x, y2, z2);
        tesselator.add(x, y, z2);
        tesselator.add(x, y2, z);
        tesselator.add(x, y, z);
        tesselator.draw(GL_QUAD_STRIP, skybox1);
        //      tesselator.draw(GL_TRIANGLE_STRIP);

        rgbai = ((int) (skyColor.x * 255.0F)) << 16 | ((int) (skyColor.y * 255.0F)) << 8 | ((int) (skyColor.z * 255.0F));
        tesselator.setColor(-1, 255);
        tesselator.add(x, y, z2);
        tesselator.add(x2, y, z2);
        tesselator.add(x2, y, z);
        tesselator.add(x, y, z);
        tesselator.add(x, y2, z);
        tesselator.add(x2, y2, z);
        tesselator.add(x2, y2, z2);
        tesselator.add(x, y2, z2);
        //    tesselator.draw(GL_TRIANGLES);
        tesselator.draw(GL_QUADS, skybox2);
        */
    }

    /**
     * @param x
     * @param y
     * @param z
     */
    Vector3f mPos = new Vector3f();
    public void setModelPos(float x, float y, float z) {
        mPos.set(x, y, z);;
    }
    float modelRot, lastModelRot;
    public void tickUpdate() {
        this.lastModelRot = modelRot;
//        this.modelRot+=3.8f;
        if (this.modelRot > 180) {
            this.modelRot -= 360;
            this.lastModelRot-=360;
        }
    }


    public boolean isNormalMappingActive() {
        return Game.instance.settings.normalMapping > 0 && !Game.VR_SUPPORT;
    }
}
