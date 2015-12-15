package nidefawl.qubes.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.GL_TEXTURE2;

import java.awt.Color;
import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.List;

import org.lwjgl.opengl.GL30;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.assets.AssetVoxModel;
import nidefawl.qubes.config.WorkingEnv;
import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.entity.Player;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.input.DigController;
import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.item.Item;
import nidefawl.qubes.item.ItemStack;
import nidefawl.qubes.models.ItemModel;
import nidefawl.qubes.models.qmodel.ModelLoaderQModel;
import nidefawl.qubes.models.qmodel.ModelRigged;
import nidefawl.qubes.models.qmodel.ModelStatic;
import nidefawl.qubes.models.voxel.ModelVox;
import nidefawl.qubes.perf.TimingHelper;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.AABB;
import nidefawl.qubes.vec.Frustum;
import nidefawl.qubes.vec.Vector3f;
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

    

    public int rendered;

    private boolean startup = true;


    public int                  texWaterNoise;
    public Shader       terrainShader;
    public Shader       terrainShaderFar;
    public Shader       skyShader;
    public Shader       waterShader;
    public Shader       shaderModelVoxel;
    public Shader       shaderModelQ;

    public Shader       shaderModelfirstPerson;
    
    private TesselatorState skybox1;
    private TesselatorState skybox2;
    private Shader shaderZPre;

    ModelVox vox;
    public ModelRigged qmodel;

    public void initShaders() {
        try {
            pushCurrentShaders();
            AssetManager assetMgr = AssetManager.getInstance();
            Shader new_waterShader = assetMgr.loadShader(this, "terrain/water");
            Shader terrain = assetMgr.loadShader(this, "terrain/terrain");
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
            Shader modelQ = assetMgr.loadShader(this, "model/model");
            Shader shaderModelfirstPerson = assetMgr.loadShader(this, "model/firstperson");
            Shader sky = assetMgr.loadShader(this, "sky/sky");
            popNewShaders();
            this.terrainShader = terrain;
            this.terrainShaderFar = terrainFar;
            this.skyShader = sky;
            this.waterShader = new_waterShader;
            this.shaderModelVoxel = modelVoxel;
            this.shaderModelQ = modelQ;
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
            this.terrainShader.setProgramUniform1i("normalTextures", 2);

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
    private int image;
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

            AssetVoxModel asset = AssetManager.getInstance().loadVoxModel("models/" + mName);
            if (this.vox != null)
                this.vox.release();
            vox = new ModelVox(asset);   
        }

        if (this.qmodel != null) {
            this.qmodel.release();
            this.qmodel = null;
        }

        ModelLoaderQModel l = new ModelLoaderQModel();
        l.loadModel("models/test.qmodel");
        this.qmodel = (ModelRigged) l.buildModel();
        if (this.image > 0) {
            TextureManager.getInstance().releaseTexture(this.image); 
        }
        AssetTexture t = AssetManager.getInstance().loadPNGAsset("models/human_adj_uv.png");
        this.image = TextureManager.getInstance().makeNewTexture(t, false, true, 0);
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
        skybox1 = new TesselatorState();
        skybox2 = new TesselatorState();
        AssetTexture tex = AssetManager.getInstance().loadPNGAsset("textures/water/noise.png");
        texWaterNoise = TextureManager.getInstance().makeNewTexture(tex, true, true, 10);
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
        AssetVoxModel asset = AssetManager.getInstance().loadVoxModel("models/dragon.vox");
        if (this.vox != null) this.vox.release();
        vox = new ModelVox(asset);
        reloadModel();
    }

    public void renderWorld(World world, float fTime) {

        if (Game.DO_TIMING)
            TimingHelper.startSec("setupView");

        glDisable(GL_BLEND);
        if (Game.DO_TIMING)
            TimingHelper.endStart("Sky");
        glDepthMask(false);
        skyShader.enable();
        skybox1.bindAndDraw(GL_QUAD_STRIP);
        skybox2.bindAndDraw(GL_QUADS);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("skyShader.drawSkybox");
        Shader.disable();
        glDepthMask(true);
        if (Game.DO_TIMING)
            TimingHelper.endStart("setupView2");

        if (Game.DO_TIMING)
            TimingHelper.endStart("testShader");

        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("terrain shader");
        
        GL.bindTexture(GL_TEXTURE0, GL30.GL_TEXTURE_2D_ARRAY, TMgr.getBlocks());
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, TMgr.getNoise());
        GL.bindTexture(GL_TEXTURE2, GL30.GL_TEXTURE_2D_ARRAY, TMgr.getNormals());
        
        if (Game.DO_TIMING)
            TimingHelper.endStart("renderFirstPass");
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("pre renderMain");
        boolean zPre = false; //sucks, not faster
        if (zPre) {
            glColorMask(false, false, false, false);
            glDepthFunc(GL_LESS);
            shaderZPre.enable();
            Engine.regionRenderer.renderMainPre(world, fTime, this);
            glColorMask(true, true, true, true);
            glDepthFunc(GL_EQUAL);
            glDepthMask(false);
            terrainShader.enable();
            Engine.regionRenderer.renderMainPost(world, fTime, this);
            glDepthFunc(GL_LEQUAL);
            glDepthMask(true);
        } else {
            terrainShader.enable();
            Engine.regionRenderer.renderMain(world, fTime, this);
        }
//        Engine.regionRenderer.renderRegions(world, fTime, PASS_LOD, 0, Frustum.FRUSTUM_INSIDE);
        rendered = Engine.regionRenderer.rendered;
        
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("renderFirstPass");
//
//        if (Game.DO_TIMING)
//            TimingHelper.endStart("renderSecondPass");
//        waterShader.enable();
//
//        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, this.texWaterNormals);
//        Engine.regionRenderer.renderRegions(world, fTime, 1, 0, Frustum.FRUSTUM_INSIDE);
//        glDisable(GL_BLEND);
//        Engine.checkGLError("renderRegions");
//        this.rendered = Engine.regionRenderer.rendered;
//        if (Game.GL_ERROR_CHECKS)
//            Engine.checkGLError("renderSecondPass");
//        
        glDisable(GL_BLEND);
//        shaderModelVoxel.enable();
//        renderVoxModels(shaderModelVoxel, PASS_SOLID, fTime);
//        Shader.disable();
        shaderModelQ.enable();
//        if (this.qmodel != null)
//            this.qmodel.animate(fTime);
        renderQModels(world, shaderModelQ, PASS_SOLID, fTime);

        if (Game.DO_TIMING)
            TimingHelper.endSec();
    }

    public void renderVoxModels(Shader modelShader, int pass, float fTime) {
        if (vox != null) {
            BufferedMatrix mat = Engine.getTempMatrix();
            BufferedMatrix mat2 = Engine.getTempMatrix2();
            float modelScale = 1 / 16f;
            mat.setIdentity();
            mat.translate(mPos.x, mPos.y, mPos.z);
            mat.translate(-Engine.GLOBAL_OFFSET.x, -Engine.GLOBAL_OFFSET.y, -Engine.GLOBAL_OFFSET.z);
            float w = (vox.size.x);
            float l = (vox.size.z);
            mat.translate(w * 0.5f * modelScale, 0, l * 0.5f * modelScale);
            mat.rotate((float) Math.toRadians(this.lastModelRot + (this.modelRot - this.lastModelRot) * fTime), 0, 1, 0);

            mat.translate(-w * 0.5f * modelScale, 0, -l * 0.5f * modelScale);
            mat.scale(modelScale);
            mat.update();
            modelShader.setProgramUniformMatrix4("model_matrix", false, mat.get(), false);
            mat2.setIdentity();
            mat2.rotate((float) Math.toRadians(this.lastModelRot + (this.modelRot - this.lastModelRot) * fTime), 0, 1, 0);
            mat2.invert().transpose();
            mat2.update();
            UniformBuffer.setNormalMat(mat2.get());
            vox.render(pass);
            UniformBuffer.setNormalMat(Engine.getMatSceneNormal().get());
        }
    
        
        
    }
    /**
     * @param world 
     * @param pass
     * @param fTime 
     */
    public void renderModelsUsingProgram(World world, Shader modelShader, int pass, float fTime) {
//        renderVoxModels(modelShader, pass, fTime);
        renderQModels(world, modelShader, pass, fTime);
    }

    /**
     * @param world 
     * @param pass
     * @param fTime 
     */
    public void renderQModels(World world, Shader modelShader, int pass, float fTime) {

        if (qmodel != null) {
            //TODO: IMPORTANT sort entities by renderer/model/shader client side
            //TODO: move in own render per-entity class
            List<Entity> ents = world.getEntityList();
            int size = ents.size();
            float absTimeInSeconds = ((GameBase.ticksran+fTime)/GameBase.TICKS_PER_SEC);
            for (int i = 0; i < size; i++) {
                Entity e = ents.get(i);
                if (e == Game.instance.getPlayer() && !Game.instance.thirdPerson)
                    continue;
                int type = 0;
                this.qmodel.setAction(0);
                //TODO: abstract this per render/model class
                //TODO: Add animation mixing (with bone mask + priority?!)
                if (e instanceof Player && ((Player)e).punchTicks>0) {
                    int maxTicks = 8;
                    absTimeInSeconds = (maxTicks-(((Player)e).punchTicks-fTime))/((float)maxTicks-1);
                    this.qmodel.setAction(1);
                    type = 1;
                } else if (e.pos.distanceSq(e.lastPos) > 1.0E-4) {
                    this.qmodel.setAction(2);
                    absTimeInSeconds *= 8;
                    absTimeInSeconds %= 4f;
                    absTimeInSeconds /= 4f;
                    type = 1;
                }
                //TODO: Implement different animation timing types (continues/one-shot)
                this.qmodel.animate(type, absTimeInSeconds);
                Vector3f pos = e.getRenderPos(fTime);
                Vector3f rot = e.getRenderRot(fTime);
                float headYaw = rot.x;
                float yaw = rot.y;
                float pitch = rot.z;
                this.qmodel.setHeadOrientation(270+headYaw, pitch);
                yaw -= headYaw;
                this.modelRot=this.lastModelRot=-1*yaw-90;
//                System.out.println(e.yaw);
                BufferedMatrix mat = Engine.getTempMatrix();
                float modelScale = 1 / 3.7f;
                mat.setIdentity();

                mat.translate(pos.x, pos.y, pos.z);
                mat.translate(-Engine.GLOBAL_OFFSET.x, -Engine.GLOBAL_OFFSET.y, -Engine.GLOBAL_OFFSET.z);
                mat.rotate((float) Math.toRadians(this.lastModelRot + (this.modelRot - this.lastModelRot) * fTime), 0, 1, 0);
                mat.rotate(-90 * GameMath.PI_OVER_180, 1, 0, 0);
                mat.rotate(-90 * GameMath.PI_OVER_180, 0, 0, 1);
                mat.scale(modelScale);
                mat.update();
//                System.out.println(modelShader.getName());
                modelShader.setProgramUniformMatrix4("model_matrix", false, mat.get(), false);
                if (modelShader == this.shaderModelQ) {
                    BufferedMatrix mat2 = Engine.getTempMatrix2();
                    mat2.setIdentity();
                    mat2.rotate((float) Math.toRadians(this.lastModelRot + (this.modelRot - this.lastModelRot) * fTime), 0, 1, 0);
                    mat2.rotate(-90 * GameMath.PI_OVER_180, 1, 0, 0);
                    mat2.rotate(-90 * GameMath.PI_OVER_180, 0, 0, 1);
                    mat2.invert().transpose();
                    mat2.update();
                    UniformBuffer.setNormalMat(mat2.get());
                }
                GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.image);
                qmodel.render(Game.ticksran+fTime);
                UniformBuffer.setNormalMat(Engine.getMatSceneNormal().get());
            }
        }
    }

    public void renderTransparent(World world, float fTime) {
//      glEnable(GL_BLEND);
//        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        if (Game.DO_TIMING)
            TimingHelper.startSec("renderSecondPass");
        waterShader.enable();
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, this.texWaterNoise);
        Engine.regionRenderer.renderRegions(world, fTime, PASS_TRANSPARENT, 0, Frustum.FRUSTUM_INSIDE);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("renderSecondPass");

        Shader.disable();
        if (Game.DO_TIMING)
            TimingHelper.endSec();

    }
    //MOve somewhere else?!
    public void renderFirstPerson(World world, float fTime) {
        Player p = Game.instance.getPlayer();
        if (p == null) {
            return;
        }
        BaseStack stack = p.getEquippedItem();
        if (stack == null) {
            return;
        }
        if (!stack.isItem()) {
            return;
        }
        ItemStack itemstack = (ItemStack) stack;
        Item item = itemstack.getItem();
        ItemModel model = item.getItemModel();
        if (model == null) {
            return;
        }
        shaderModelfirstPerson.enable();
        this.modelRot=this.lastModelRot=4;
//            System.out.println(e.yaw);
        BufferedMatrix mat = Engine.getTempMatrix();
        float modelScale = 1 / 2.8f;
        float angle = 0;
        float f1=0;
        DigController dig = Game.instance.dig;
        if (dig.isDigAnimation()) {
            int ticks = Game.instance.dig.getTicks();
            int iTicks = ticks;
            int iStart = 4;
            int iforward = 4;
            int iback = 7;
            if (iTicks+fTime <= iStart) {
                float fTicks = (iTicks+fTime)/(float)iStart;
                float progress = fTicks;
                if (progress>0.5f) {
                    progress = 1.0f-progress;
                }
                angle = -20*progress;
                f1 = 0;
            }
            else if (iTicks+fTime <= iStart+iforward) {
                iTicks -= iStart;
                float fTicks = (iTicks+fTime)/(float)(iforward);
                float progress = 1-fTicks;
                progress=progress*progress;
                progress=1-progress;
                angle = 60*progress;
                f1 = 0;
            }
            else if (iTicks+fTime <= iStart+iforward+iback) {
                iTicks -= iStart;
                iTicks -= iforward;
                float fTicks = (iTicks+fTime)/(float)(iback);
                float progress = fTicks;
                progress=progress*progress;
                angle = 60*(1-progress);
                f1 = 0;
            }
//            if (p.punchTicks > 30)
//            float progress = (Math.max(0, (maxTicks-p.punchTicks)+fTime))/((float)maxTicks);
//            float forward = progress/
//            progress=1-progress;
//            progress=progress*progress*progress;
//            progress=1-progress;
//            progress = Math.min(1, progress);
//            float updown = GameMath.sin(progress*2*GameMath.PI);
//            if (updown<0)updown*=3.4f;
//            else updown*=2.4f;
//            angle = -updown*18;
//            f1 = updown>0?-updown/5.0f:updown/5.0f;
//            f1=0;
        
        }
        
        mat.setIdentity();
//        mat.translate(0.8f-(f1*0.165f), -0.9f-(f1*0.765f), -0.8f-(f1*0.8f));
        mat.translate(0.8f+(f1*0.565f), -0.9f-(f1*0.4f), -0.8f);
        mat.rotate((-100-(angle*1.2f)) * GameMath.PI_OVER_180, 1, 0, 0);
        mat.rotate((-220-(angle*-0.2f)) * GameMath.PI_OVER_180, 0, 0, 1);
        mat.rotate((21+(angle*0.4f)) * GameMath.PI_OVER_180, 0, 0, 1);
        mat.scale(modelScale);
        mat.update();
        shaderModelfirstPerson.setProgramUniformMatrix4("model_matrix", false, mat.get(), false);
        if (true) { //TODO: fix me (Normal mat)

            BufferedMatrix mat2 = Engine.getTempMatrix2();
            mat2.load(Engine.getMatSceneV());
            mat2.invert();
            mat.mulMat(mat2);
            mat.invert().transpose();
            mat.update();
            UniformBuffer.setNormalMat(mat.get());
        }
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, model.loadedTextures[0]);
        //first person needs clear depth buffer, move somewhere else
//            glDisable(GL_DEPTH_TEST);
        model.loadedModels[0].render(Game.ticksran+fTime);
//            glEnable(GL_DEPTH_TEST);
        UniformBuffer.setNormalMat(Engine.getMatSceneNormal().get());
           
    }

    public void renderNormals(World world, float fTime) {
        if (Shaders.normals != null) {            
            Shaders.normals.enable();
            glLineWidth(3.0F);
            Engine.checkGLError("glLineWidth");
//            Engine.regionRenderer.setDrawMode(ARBGeometryShader4.GL_T);
            Engine.regionRenderer.setDrawMode(-1);
            Engine.regionRenderer.renderRegions(world, fTime, PASS_SOLID, 0, Frustum.FRUSTUM_INSIDE);
//            Engine.regionRenderer.renderRegions(world, fTime, PASS_TRANSPARENT, 0, Frustum.FRUSTUM_INSIDE);
            Engine.regionRenderer.renderRegions(world, fTime, PASS_LOD, 0, Frustum.FRUSTUM_INSIDE);
            renderModelsUsingProgram(world, Shaders.normals, PASS_SOLID, fTime);
            BufferedMatrix mat = Engine.getIdentityMatrix();
            Shaders.normals.setProgramUniformMatrix4("model_matrix", false, mat.get(), false);
            Engine.regionRenderer.setDrawMode(-1);
//            glLineWidth(2.0F);

//            Shaders.colored.enable();
//            Engine.regionRenderer.renderDebug(world, fTime);
            Shader.disable();
        }
    }

    public void renderTerrainWireFrame(World world, float fTime) {
        Shaders.wireframe.enable();
        Shaders.wireframe.setProgramUniform3f("in_offset", Engine.GLOBAL_OFFSET.x, Engine.GLOBAL_OFFSET.y, Engine.GLOBAL_OFFSET.z);
        Shaders.wireframe.setProgramUniform1i("num_vertex", 4);
        Shaders.wireframe.setProgramUniform1f("thickness", 0.2f);
        Shaders.wireframe.setProgramUniform1f("maxDistance", 110);
        Shaders.wireframe.setProgramUniform4f("linecolor", 1, 0.2f, 0.2f, 1);
        Engine.regionRenderer.renderRegions(world, fTime, 0, 0, Frustum.FRUSTUM_INSIDE);
        Shaders.wireframe.setProgramUniform4f("linecolor", 1, 1, 0.2f, 1);
        Engine.regionRenderer.renderRegions(world, fTime, 1, 0, Frustum.FRUSTUM_INSIDE);
//        Shaders.wireframe.setProgramUniform4f("linecolor",  1, 0.2f, 0.2f, 1);
        Engine.regionRenderer.renderRegions(world, fTime, PASS_LOD, 0, Frustum.FRUSTUM_INSIDE);
        Shader.disable();
    }
    
    public int getNumRendered() {
        return this.rendered;
    }

    public void renderDebugBB(World world, float fTime) {
        if (!this.debugBBs.isEmpty()) {
            glPushAttrib(-1);
            glEnable(GL_BLEND);
            glDepthFunc(GL_LEQUAL);
          glDisable(GL_DEPTH_TEST);
            Shaders.colored3D.enable();
            for (Integer i : debugBBs.keySet()) {
                AABB bb = debugBBs.get(i);
                int iColor = GameMath.randomI(i*19)%33;
                iColor = Color.getHSBColor(iColor/33F, 0.8F, 1.0F).getRGB();
                
                float fMinX = (float) bb.minX;
                float fMinY = (float) bb.minY;
                float fMinZ = (float) bb.minZ;
                float fMaxX = (float) bb.maxX;
                float fMaxY = (float) bb.maxY;
                float fMaxZ = (float) bb.maxZ;
                
                glLineWidth(2.0F);
                float ext = 1/32F;
                float zero = -ext;
                float one = 1+ext;
                Tess.instance.setColor(iColor, 120);
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
                
                Tess.instance.setColor(iColor, 120);
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

}
