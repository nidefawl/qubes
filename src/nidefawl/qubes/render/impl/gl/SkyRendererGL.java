package nidefawl.qubes.render.impl.gl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_RGBA16F;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import org.lwjgl.opengl.*;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.perf.GPUProfiler;
import nidefawl.qubes.render.SkyRenderer;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.ShaderCompileError;
import nidefawl.qubes.shader.UniformBuffer;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.EResourceType;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.world.World;

public class SkyRendererGL extends SkyRenderer {

    public FrameBuffer    fbSkybox;
    Shader                spriteShader;
    public Shader         cloudsShader;
    public Shader         shaderSampleCubemap;
    private boolean       startup              = true;
    GLVBO                 vboAttr;
    GLVBO                 vboStaticQuad;
    GLVBO                 vboIdx;
    private int           vaoPos;
    private int[]         texClouds;

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
        super.init();
        initShaders();
//        skyColor = new Vector3f(0.43F, .69F, 1.F);
        this.vaoPos = GL30.glGenVertexArrays();
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
        this.numTexturesCloud = this.texClouds.length;
        redraw();

    }

    public void resize(int displayWidth, int displayHeight) {
        releaseAll(EResourceType.FRAMEBUFFER);
        fbSkybox = new FrameBuffer(SKYBOX_RES, SKYBOX_RES);
        fbSkybox.setTextureType(GL13.GL_TEXTURE_CUBE_MAP);
        fbSkybox.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA16F);
//      fbSkybox.setHasDepthAttachment();
        fbSkybox.setup(this);
    }
    @Override
    public void renderSky(World world, float fTime) {
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
//        spriteShader.enable();
//        GL30.glBindVertexArray(vaoPos);
//        for (int i = 0; i < this.texClouds.length; i++) {
//            if (texClouds.length != 1) // uploaded outside
//                storeSprites(f, i);
//            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.vboIdx.getVboId());
//            GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.texClouds[i]);
//            GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0, this.storedSprites);
//        }
//        GL30.glBindVertexArray(0);
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
    @Override
    protected void uploadData() {
        this.vboAttr.upload(GL15.GL_ARRAY_BUFFER, this.bufMat, this.bufMatFloat.limit()*4);
    }

}
