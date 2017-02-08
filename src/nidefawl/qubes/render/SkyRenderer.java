package nidefawl.qubes.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_RGBA16F;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.meshing.BlockFaceAttr;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.ShaderCompileError;
import nidefawl.qubes.shader.UniformBuffer;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.EResourceType;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.Half;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.WorldClient;

public class SkyRenderer extends AbstractRenderer {
    final static int      MAX_SPRITES          = 1024 * 64;
    final static int      SKYBOX_RES           = 256; //crappy, but works

    public FrameBuffer    fbSkybox;
    Shader                spriteShader;
    public Shader         cloudsShader;
    public Shader         shaderSampleCubemap;
    private boolean       startup              = true;
    GLVBO                 vboAttr;
    GLVBO                 vboStaticQuad;
    GLVBO                 vboIdx;
    final CubeMapCamera   cubeMatrix           = new CubeMapCamera();
    BlockFaceAttr         attr                 = new BlockFaceAttr();
    private int           vaoPos;
    List<Cloud>           clouds               = Lists.newArrayList();
    ReallocIntBuffer      vertexUploadDirectBuf;
    int                   storedSprites        = 0;
    int                   totalSpritesRendered = 0;
    private VertexBuffer  vertexBuf;
    final static Vector3f tmp                  = new Vector3f();
    private int[]         texClouds;
    private ByteBuffer    bufMat;
    private FloatBuffer   bufMatFloat;

    static class Cloud {
        public int texture;

        List<PointSprite> sprites = Lists.newArrayList();

        Vector3f mot;
        Vector3f pos, lastPos, renderPos;
        public Cloud() {
            this.pos = new Vector3f();
            this.lastPos = new Vector3f();
            this.renderPos = new Vector3f();
            this.mot = new Vector3f();
        }
        public int store(FloatBuffer bufMatFloat) {
            for (PointSprite s : this.sprites) {
                tmp.set(this.renderPos);
                tmp.addVec(s.renderPos);
                tmp.store(bufMatFloat);
                bufMatFloat.put(s.renderSize);
                s.renderCol.store(bufMatFloat);
                bufMatFloat.put(s.renderRot);
            }
            return this.sprites.size();
        }
        public void update(float f) {
            Vector3f.interp(this.lastPos, this.pos, f, this.renderPos);

            for (PointSprite s : this.sprites) {
                s.update(f);
            }
        }
        public void tick() {
            
            this.lastPos.set(this.pos);
//          this.pos.addVec(this.mot);
            for (PointSprite s : this.sprites) {
                s.tick();
            }
        }
    }
    static class PointSprite {
        public float size, initSize, lastSize, renderSize;
        public float rotspeed;
        public float rot, lastRot, renderRot;
        public float xoffset;
        public float yoffset;
        Vector3f posOffset;
        public Vector3f col, lastCol, initCol, renderCol;
        private Vector3f renderPos;
        int tick = 0;
        public PointSprite() {
            this.renderPos = new Vector3f();
            this.posOffset = new Vector3f();
            this.col = new Vector3f();
            this.lastCol = new Vector3f();
            this.initCol = new Vector3f();
            this.renderCol = new Vector3f();
        }
        public void setSize(float size) {
            this.initSize = this.size = this.lastSize = this.renderSize = size;
        }
        public void setCol(float x, float y, float z) {
            this.col.set(x, y, z);
            this.lastCol.set(x, y, z);
            this.initCol.set(x, y, z);
            this.renderCol.set(x, y, z);
        }
        public void update(float f) {
            renderSize = lastSize+(size-lastSize)*f;
            renderRot = lastRot+(rot-lastRot)*f;
            Vector3f.interp(lastCol, col, f, renderCol);
            this.renderPos.set(this.posOffset);
//            {
//                float f2 = (tick+f+xoffset)/15520.0f;
//                f2 = (f2*GameMath.PI)%GameMath.PI*2;
//                posOffset.x += 0.0001f*GameMath.sin(f2);
//            }
//            {
//                float f2 = (tick+f+yoffset)/21220.0f;
//                f2 = (f2*GameMath.PI)%GameMath.PI*2;
//                posOffset.y += 0.0001f*GameMath.sin(f2);
//            }
            
        }

        public void tick() {
            lastSize = size;
            lastRot = rot;
            this.lastCol.set(this.col);
            float weatherStr = (1-WEATHER);
            weatherStr = GameMath.powf(weatherStr, 2.2f);
            this.col.x = this.initCol.x*(weatherStr);
            this.col.y = this.initCol.y*(weatherStr);
            this.col.z = this.initCol.z*(weatherStr);
            size = initSize*(WEATHER*0.5f+0.5f);
            rot += rotspeed;
            tick++;
        }
    }
    public static float WEATHER = 0.40f;
    public void increaseClouds() {
        WEATHER += 0.01f;
        if (WEATHER > 1)
            WEATHER = 1;
    }
    public void decreaseClouds() {
        WEATHER -= 0.01f;
        if (WEATHER < 0)
            WEATHER = 0;
    }
    public void initShaders() {
        try {
            pushCurrentShaders();
            AssetManager assetMgr = AssetManager.getInstance();
            Shader spriteShader = assetMgr.loadShader(this, "particle/clouds");
            Shader cloudsShader = assetMgr.loadShader(this, "sky/skybox_generate");
            Shader shaderSampleCubemap = assetMgr.loadShader(this, "sky/skybox_sample_cubemap");
            popNewShaders();
            this.spriteShader = spriteShader;
            this.cloudsShader = cloudsShader;
            this.shaderSampleCubemap = shaderSampleCubemap;
            this.shaderSampleCubemap.enable();
            this.shaderSampleCubemap.setProgramUniform1i("tex0", 0);
            this.spriteShader.enable();
            this.spriteShader.setProgramUniform1i("tex0", 0);
            this.cloudsShader.enable();
            this.cloudsShader.setProgramUniform1i("tex0", 0);
            Shader.disable();
            startup = false;
        } catch (ShaderCompileError e) {
            releaseNewShaders();
            System.out.println("shader " + e.getName() + " failed to compile");
            System.out.println(e.getLog());
            if (startup) {
                throw e;
            } else {
                if (Game.instance != null)
                Game.instance.addDebugOnScreen("\0uff3333shader " + e.getName() + " failed to compile");
            }
        }
        startup = false;
//        redraw();
    }
    public void init() {
//        skyColor = new Vector3f(0.43F, .69F, 1.F);
        cubeMatrix.init();
        initShaders();
        this.bufMat = Memory.createByteBufferAligned(64, 16*4*MAX_SPRITES);
        this.bufMatFloat = this.bufMat.asFloatBuffer();
        this.vertexUploadDirectBuf = new ReallocIntBuffer();
        this.vaoPos = GL30.glGenVertexArrays();
        this.vertexBuf = new VertexBuffer(1024*1024);
        this.vboStaticQuad = new GLVBO(GL15.GL_STREAM_DRAW);
        this.vboAttr = new GLVBO(GL15.GL_STREAM_DRAW);
        this.vboIdx = new GLVBO(GL15.GL_STREAM_DRAW);
        
        GL30.glBindVertexArray(vaoPos);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vboStaticQuad.getVboId());
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL30.GL_HALF_FLOAT, false, 4, 0);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vboAttr.getVboId());
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 4, GL11.GL_FLOAT, false, 32, 0);
        GL33.glVertexAttribDivisor(1, 1);
        GL20.glEnableVertexAttribArray(2);
        GL20.glVertexAttribPointer(2, 4, GL11.GL_FLOAT, false, 32, 16);
        GL33.glVertexAttribDivisor(2, 1);
        GL30.glBindVertexArray(0);
        
        
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.vboIdx.getVboId());
        ByteBuffer buf = Memory.createByteBufferAligned(64, 6*4);
        IntBuffer idxBuf = buf.asIntBuffer();
        idxBuf.put(0);
        idxBuf.put(1);
        idxBuf.put(2);
        idxBuf.put(2);
        idxBuf.put(3);
        idxBuf.put(0);
        idxBuf.flip();
        buf.limit(idxBuf.limit()*4);
        this.vboIdx.upload(GL15.GL_ELEMENT_ARRAY_BUFFER, buf, idxBuf.limit()*4);
        Memory.free(buf);
        this.vertexBuf.reset();
        buildQuad(this.vertexBuf);
        int intsize = this.vertexBuf.storeVertexData(this.vertexUploadDirectBuf);
        this.vboStaticQuad.upload(GL15.GL_ARRAY_BUFFER, this.vertexUploadDirectBuf.getByteBuf(), intsize*4);
        ArrayList<AssetTexture> clouds = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            AssetTexture tex = AssetManager.getInstance().loadPNGAsset("textures/sky/cloud"+i+".png", i>0);
            if (tex == null)
                break;
            clouds.add(tex);
        }
        this.texClouds = new int[clouds.size()];
        for (int i = 0; i < clouds.size(); i++) {
            this.texClouds[i] = TextureManager.getInstance().makeNewTexture(clouds.get(i), false, true, -1);
        }
        redraw();

    }

    void updateSpritesTick() {
        for (int i = 0; i < clouds.size(); i++) {
            Cloud sprite = clouds.get(i);
            sprite.tick();
        }
    }
    void updateSprites(float ftime) {
        for (int i = 0; i < clouds.size(); i++) {
            Cloud cloud = clouds.get(i);
            cloud.update(ftime);
        }
        
    }
    void storeSprites(float ftime, int n) {
        this.bufMatFloat.clear();
        storedSprites = 0;
        for (int i = 0; i < clouds.size(); i++) {
            Cloud cloud = clouds.get(i);
            if (cloud.texture == n) {
                storedSprites+=cloud.store(this.bufMatFloat);
            }
        }
        this.bufMatFloat.flip();
        this.bufMat.position(0).limit(this.bufMatFloat.limit()*4);
        this.vboAttr.upload(GL15.GL_ARRAY_BUFFER, this.bufMat, this.bufMatFloat.limit()*4);
//      System.out.println("totalSprites "+totalSprites);
        
    }
    public void redraw() {
        clouds.clear();
        Random r = new Random(4444);
        float l = 2.2f;
        float hl = 0.6f;
        float hu = 1.6f;
        float motRange = 0.05f;
        float rotRange = 0.0005f;
        float minBr=0.25f;
        float maxBr=1.0f;
        float sizeScale = 0.5f;
        float minSize = 33*sizeScale;
        float maxSize = 100*sizeScale;
        float l2 = 0.5f;
        float h2 = 0.2f;
        for (int i = 0; i < 12; i++) {
            Cloud cloud = new Cloud();
            cloud.texture = r.nextInt(this.texClouds.length);
            cloud.pos.x = r.nextFloat()*l*2.0f-l;
            cloud.pos.y = hl+(hu-hl)*r.nextFloat();
            cloud.pos.z = r.nextFloat()*l*2.0f-l;
            float fSize2 = 0.5f*(cloud.pos.y-hl)/(hu-hl);
//          cloud.mot.x = (r.nextFloat()*2.0f-1.0f)*motRange;
//          cloud.mot.y = 0;
//          cloud.mot.z = (r.nextFloat()*2.0f-1.0f)*motRange;
            for (int j = 0; j < 12; j++) {
                PointSprite sprite = new PointSprite();
                sprite.xoffset = r.nextFloat();
                sprite.yoffset = r.nextFloat();
                sprite.posOffset.x = r.nextFloat()*l2*2.0f-l2;
                sprite.posOffset.y = r.nextFloat()*h2*2.0f-h2;
                sprite.posOffset.z = r.nextFloat()*l2*2.0f-l2;
                float size = minSize+(r.nextFloat()+fSize2)*(maxSize-minSize);
//              sprite.setSize(size*0.5f+fSize2*(minSize));
              sprite.setSize(53);
                float f1 = (minBr+r.nextFloat()*(maxBr-minBr));
                float f2 = f1*0.9f+0.1f*(minBr+r.nextFloat()*(maxBr-minBr));
                sprite.setCol(f1, f1, f2);
                sprite.rot = sprite.lastRot = r.nextFloat()*0.43f;
                
                sprite.rotspeed = (r.nextFloat()*2.0f-1.0f)*rotRange;
                cloud.sprites.add(sprite);
            }
            clouds.add(cloud);
        }
        
    
        
        
    }
    private void buildQuad(VertexBuffer vertexBuf) {
        vertexBuf.put(Half.fromFloat(0) << 16 | Half.fromFloat(1));
        vertexBuf.put(Half.fromFloat(1) << 16 | Half.fromFloat(1));
        vertexBuf.put(Half.fromFloat(1) << 16 | Half.fromFloat(0));
        vertexBuf.put(Half.fromFloat(0) << 16 | Half.fromFloat(0));
    }
    public void resize(int displayWidth, int displayHeight) {
        releaseAll(EResourceType.FRAMEBUFFER);
        fbSkybox = new FrameBuffer(SKYBOX_RES, SKYBOX_RES);
        fbSkybox.setTextureType(GL13.GL_TEXTURE_CUBE_MAP);
        fbSkybox.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA16F);
//      fbSkybox.setHasDepthAttachment();
        fbSkybox.setup(this);
    }
    public void renderSky(WorldClient world, float fTime) {
        renderSky(world.getDayTime(), fTime);
    }
    public void renderSky(long daytime, float fTime) {
        if (GPUProfiler.PROFILING_ENABLED) {
            GPUProfiler.start("clouds");
        }
        this.updateSprites(fTime);
        glDisable(GL11.GL_DEPTH_TEST);
        UniformBuffer.uboMatrix3D_Temp.bind();
        this.fbSkybox.bind();
//        GL11.glFinish();
//        GL11.glFlush();
        Engine.setViewport(0, 0, this.fbSkybox.getWidth(), this.fbSkybox.getHeight());
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        float weatherStr = (WEATHER);
        weatherStr = GameMath.powf(weatherStr*0.9f, 1.6f);
        cloudsShader.enable();
        cloudsShader.setProgramUniform1f("rainStrength", WEATHER);
        cloudsShader.setProgramUniform1i("worldTime", (int) daytime);
        spriteShader.enable();
        spriteShader.setProgramUniform1f("transparency", weatherStr);
        spriteShader.setProgramUniform1f("spritebrightness", 5f);
        Engine.setBlend(true);
        if (this.texClouds.length == 1) { //TODO: optimize multi texture by using multiple buffers
            storeSprites(fTime, 0);
        }
        for (int c = 0; c < 6; c++) {
            if (GPUProfiler.PROFILING_ENABLED) {
                GPUProfiler.start("clouds_"+c);
            }
            this.fbSkybox.bindCubeMapFace(c);
            cubeMatrix.setupScene(c, Engine.camera.getPosition());
            renderSkyBox(fTime);
            if (GPUProfiler.PROFILING_ENABLED) {
                GPUProfiler.end();
            }
        }
//        Engine.setDefaultViewport();
        Engine.setBlend(false);
        
        UniformBuffer.uboMatrix3D.bind();
        
        Engine.enableDepthMask(true);
        glEnable(GL11.GL_DEPTH_TEST);
        if (GPUProfiler.PROFILING_ENABLED) {
            GPUProfiler.end();
        }
    }

    private void renderSkyBox(float f) {
//        Engine.setBlend(false);
        cloudsShader.enable();
        Engine.checkGLError("cloudsShader1");
        Engine.drawFSTri();
        Engine.checkGLError("cloudsShader2");
//        Engine.setBlend(true);
        spriteShader.enable();
        GL30.glBindVertexArray(vaoPos);
        for (int i = 0; i < this.texClouds.length; i++) {
            if (texClouds.length != 1) // uploaded outside
                storeSprites(f, i);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.vboIdx.getVboId());
            GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.texClouds[i]);
            GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0, this.storedSprites);
        }
        GL30.glBindVertexArray(0);
        Engine.bindVAO(null);
    }
    public void tickUpdate() {
        updateSpritesTick();
    }
    public void renderSkybox() {

        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.start("sky+sun+clouds");
        Engine.enableDepthMask(false);
        
        shaderSampleCubemap.enable();

        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_CUBE_MAP, fbSkybox.getTexture(0));

        Engine.drawFSTri();
        Engine.enableDepthMask(true);
        if (GPUProfiler.PROFILING_ENABLED)
            GPUProfiler.end();
    }

}
