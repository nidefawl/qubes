package nidefawl.qubes.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import nidefawl.qubes.Main;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.FrameBuffer;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.gui.GuiOverlayDebug;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.util.TimingHelper;

public class FinalRendererAdv extends FinalRendererBase {
    public Shader      composite1;
    public Shader      composite2;
    public Shader      composite3;
    public Shader      compositeFinal;
    public FrameBuffer fbComposite0;
    public FrameBuffer fbComposite1;
    public FrameBuffer fbComposite2;
    public FrameBuffer sceneFB;

    @Override
    public void render(float fTime) {
        boolean enableShaders = true;
        GuiOverlayDebug dbg = Main.instance.debugOverlay;
        if (Main.show) {
            if (Main.DO_TIMING) TimingHelper.startSec("DebugOverlay");
            dbg.preDbgFB(true);
            dbg.drawDebug();
            dbg.postDbgFB();
            if (Main.DO_TIMING) TimingHelper.endSec();
        }


        if (Main.DO_TIMING) TimingHelper.startSec("Composite0");
        Shader.disable();
        fbComposite0.bind();
        fbComposite0.clearFrameBuffer();
        if (Main.GL_ERROR_CHECKS) Engine.checkGLError("bind fbo composite1");
        if (enableShaders) {
            composite1.enable();
            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("enable shader composite1");
            Shaders.setUniforms(composite1, fTime);
        }
        GL.bindTexture(GL_TEXTURE6, GL_TEXTURE_2D, Engine.fbShadow.getDepthTex());
        GL.bindTexture(GL_TEXTURE5, GL_TEXTURE_2D, Engine.getSceneFB().getDepthTex());
        GL.bindTexture(GL_TEXTURE4, GL_TEXTURE_2D, TMgr.getNoise());
        for (int i = 0; i < 4; i++) {
            GL.bindTexture(GL_TEXTURE0 + i, GL_TEXTURE_2D, Engine.getSceneFB().getTexture(i));
        }

        Engine.drawFullscreenQuad();
        
        if (Main.DO_TIMING) TimingHelper.endSec();
        if (Main.show) {
            if (Main.DO_TIMING) TimingHelper.startSec("DebugOverlay");
            fbComposite0.unbindCurrentFrameBuffer();
            Shader.disable();
            dbg.preDbgFB(false);
            for (int i = 0; i < 4; i++) {
                dbg.drawDbgTexture(0, 0, i, Engine.getSceneFB().getTexture(i), "TexUnit " + i);
            }
            dbg.drawDbgTexture(0, 0, 4, TMgr.getNoise(), "TexUnit " + 4);
            dbg.drawDbgTexture(0, 0, 5, Engine.getSceneFB().getDepthTex(), "Depth", Shaders.depthBufShader, Engine.znear, Engine.zfar);
            dbg.drawDbgTexture(0, 0, 6, Engine.fbShadow.getDepthTex(), "Shadow", Shaders.depthBufShader, 0.05F, 256.0F);
            for (int i = 0; i < 4; i++) {
                dbg.drawDbgTexture(0, 1, i, fbComposite0.getTexture(i), "ColAtt " + i);
            }
            dbg.postDbgFB();
            if (Main.DO_TIMING) TimingHelper.endSec();
        }
        if (Main.DO_TIMING) TimingHelper.startSec("GenMipMap0");

        GL.generateTextureMipmap(fbComposite0.getTexture(0), GL_TEXTURE_2D);
        GL.generateTextureMipmap(fbComposite0.getTexture(3), GL_TEXTURE_2D);
        
        if (Main.DO_TIMING) TimingHelper.endSec();

        if (Main.DO_TIMING) TimingHelper.startSec("Composite1");
        
        
        fbComposite1.bind();
        fbComposite1.clearFrameBuffer();
        if (enableShaders) {
            composite2.enable();
            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("enable shader composite2");
            Shaders.setUniforms(composite2, fTime);
        }
//        GL.bindTexture(GL_TEXTURE6, GL_TEXTURE_2D, Engine.fbShadow.getDepthTex());
//        GL.bindTexture(GL_TEXTURE5, GL_TEXTURE_2D, Engine.getSceneFB().getDepthTex());
//        GL.bindTexture(GL_TEXTURE4, GL_TEXTURE_2D, TMgr.getNoise());
        for (int i = 0; i < 4; i++) {
            GL.bindTexture(GL_TEXTURE0 + i, GL_TEXTURE_2D, fbComposite0.getTexture(i));
        }
        GL20.glDrawBuffers(GL30.GL_COLOR_ATTACHMENT2);
        Engine.drawFullscreenQuad();
        if (Main.DO_TIMING) TimingHelper.endSec();
        
        if (Main.show) {
            fbComposite1.unbindCurrentFrameBuffer();
            Shader.disable();

            dbg.preDbgFB(false);
            for (int i = 0; i < 4; i++) {
                dbg.drawDbgTexture(1, 0, i, fbComposite0.getTexture(i), "TexUnit " + i);
            }
            dbg.drawDbgTexture(1, 0, 5, Engine.getSceneFB().getDepthTex(), "Depth", Shaders.depthBufShader, Engine.znear, Engine.zfar);
            dbg.drawDbgTexture(1, 0, 6, Engine.fbShadow.getDepthTex(), "Shadow", Shaders.depthBufShader, 0.05F, 256.0F);
            dbg.drawDbgTexture(1, 1, 2, fbComposite1.getTexture(2), "ColAtt " + 2);
            dbg.postDbgFB();
        }

        if (Main.DO_TIMING) TimingHelper.startSec("GenMipMap1");
        GL.generateTextureMipmap(fbComposite1.getTexture(2), GL_TEXTURE_2D);
        if (Main.DO_TIMING) TimingHelper.endSec();
        
        if (Main.DO_TIMING) TimingHelper.startSec("Composite2");

        fbComposite2.bind();
        fbComposite2.clearFrameBuffer();
        if (enableShaders) {
            composite3.enable();
            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("enable shader composite3");
            Shaders.setUniforms(composite3, fTime);
        }
//        GL.bindTexture(GL_TEXTURE6, GL_TEXTURE_2D, Engine.fbShadow.getDepthTex());
//        GL.bindTexture(GL_TEXTURE5, GL_TEXTURE_2D, Engine.getSceneFB().getDepthTex());
//        GL.bindTexture(GL_TEXTURE4, GL_TEXTURE_2D, TMgr.getNoise());
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, fbComposite0.getTexture(0));
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, fbComposite0.getTexture(1));
        GL.bindTexture(GL_TEXTURE2, GL_TEXTURE_2D, fbComposite1.getTexture(2));
        GL.bindTexture(GL_TEXTURE3, GL_TEXTURE_2D, fbComposite0.getTexture(3));
        
        GL20.glDrawBuffers(GL30.GL_COLOR_ATTACHMENT0);
        Engine.drawFullscreenQuad();
        if (Main.DO_TIMING) TimingHelper.endSec();

        fbComposite2.unbindCurrentFrameBuffer();
        if (Main.show) {
            Shader.disable();
            dbg.preDbgFB(false);
            dbg.drawDbgTexture(2, 0, 0, fbComposite0.getTexture(0), "TexUnit " + 0);
            dbg.drawDbgTexture(2, 0, 1, fbComposite0.getTexture(1), "TexUnit " + 1);
            dbg.drawDbgTexture(2, 0, 2, fbComposite1.getTexture(2), "TexUnit " + 2);
            dbg.drawDbgTexture(2, 0, 3, fbComposite0.getTexture(3), "TexUnit " + 3);
            dbg.drawDbgTexture(2, 0, 5, Engine.getSceneFB().getDepthTex(), "Depth", Shaders.depthBufShader, Engine.znear, Engine.zfar);
            dbg.drawDbgTexture(2, 0, 6, Engine.fbShadow.getDepthTex(), "Shadow", Shaders.depthBufShader, 0.05F, 256.0F);
            dbg.drawDbgTexture(2, 1, 0, fbComposite2.getTexture(0), "ColAtt " + 0);
            dbg.drawDbgTexture(3, 0, 0, fbComposite2.getTexture(0), "TexUnit " + 0);
            dbg.drawDbgTexture(3, 0, 1, fbComposite0.getTexture(1), "TexUnit " + 1);
            dbg.drawDbgTexture(3, 0, 2, fbComposite1.getTexture(2), "TexUnit " + 2);
            dbg.drawDbgTexture(3, 0, 3, fbComposite0.getTexture(3), "TexUnit " + 3);
            dbg.drawDbgTexture(3, 0, 5, Engine.getSceneFB().getDepthTex(), "Depth", Shaders.depthBufShader, Engine.znear, Engine.zfar);
            dbg.drawDbgTexture(3, 0, 6, Engine.fbShadow.getDepthTex(), "Shadow", Shaders.depthBufShader, 0.05F, 256.0F);
            //    drawDbgTexture(2, 1, 0, Engine.fbComposite2.getTexture(0), "GL_TEXTURE"+0);
            dbg.postDbgFB();
        }

    }
    @Override
    public void renderFinal(float fTime) {

        if (Main.DO_TIMING) TimingHelper.startSec("CompositeFinal");
        boolean enableShaders = true;
        if (enableShaders) {
            compositeFinal.enable();
            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("enable shader compositeF");
            Shaders.setUniforms(compositeFinal, fTime);
        }
//        GL.bindTexture(GL_TEXTURE6, GL_TEXTURE_2D, Engine.fbShadow.getDepthTex());
//        GL.bindTexture(GL_TEXTURE5, GL_TEXTURE_2D, Engine.getSceneFB().getDepthTex());
//        GL.bindTexture(GL_TEXTURE4, GL_TEXTURE_2D, TMgr.getNoise());
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, fbComposite2.getTexture(0));
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, fbComposite0.getTexture(1));
        GL.bindTexture(GL_TEXTURE2, GL_TEXTURE_2D, fbComposite1.getTexture(2));
        GL.bindTexture(GL_TEXTURE3, GL_TEXTURE_2D, fbComposite0.getTexture(3));

        Engine.drawFullscreenQuad();
        
        Shader.disable();
        if (Main.DO_TIMING) TimingHelper.endSec();
    }
    
    @Override
    public void initShaders() {
        try {
            AssetManager assetMgr = AssetManager.getInstance();
            Shader new_composite1 = assetMgr.loadShader("shaders/adv/composite");
            Shader new_composite2 = assetMgr.loadShader("shaders/adv/composite1");
            Shader new_composite3 = assetMgr.loadShader("shaders/adv/composite2");
            Shader new_compositeFinal = assetMgr.loadShader("shaders/adv/final");
            releaseShaders();
            composite1 = new_composite1;
            composite2 = new_composite2;
            composite3 = new_composite3;
            compositeFinal = new_compositeFinal;
        } catch (ShaderCompileError e) {
            e.printStackTrace();
            Main.instance.addDebugOnScreen("\0uff3333shader "+e.getName()+" failed to compile");
            System.out.println("shader "+e.getName()+" failed to compile");
            System.out.println(e.getLog());
        }
    }
    
    @Override
    public void resize(int displayWidth, int displayHeight) {
        releaseFrameBuffers();
        sceneFB = new FrameBuffer(displayWidth, displayHeight);
        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGB16);
        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT1, GL_RGB8);
        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT2, GL_RGB16);
        sceneFB.setColorAtt(GL_COLOR_ATTACHMENT3, GL_RGB8);
        sceneFB.setClearColor(GL_COLOR_ATTACHMENT0, 1.0F, 1.0F, 1.0F, 1.0F);
        sceneFB.setClearColor(GL_COLOR_ATTACHMENT1, 1.0F, 1.0F, 1.0F, 1.0F);
        sceneFB.setClearColor(GL_COLOR_ATTACHMENT2, 0F, 0F, 0F, 0F);
        sceneFB.setClearColor(GL_COLOR_ATTACHMENT3, 0F, 0F, 0F, 0F);
        sceneFB.setHasDepthAttachment();
        sceneFB.setup();
        Engine.setSceneFB(sceneFB);
        fbComposite0 = new FrameBuffer(displayWidth, displayHeight);
        fbComposite0.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGB16);
        fbComposite0.setFilter(GL_COLOR_ATTACHMENT0, GL_LINEAR_MIPMAP_LINEAR);
        fbComposite0.setClearColor(GL_COLOR_ATTACHMENT0, 1.0F, 1.0F, 1.0F, 1.0F);
        fbComposite0.setColorAtt(GL_COLOR_ATTACHMENT1, GL_RGB8);
        fbComposite0.setClearColor(GL_COLOR_ATTACHMENT1, 1.0F, 1.0F, 1.0F, 1.0F);
        fbComposite0.setColorAtt(GL_COLOR_ATTACHMENT2, GL_RGB16);
        fbComposite0.setClearColor(GL_COLOR_ATTACHMENT2, 0F, 0F, 0F, 0F);
        fbComposite0.setColorAtt(GL_COLOR_ATTACHMENT3, GL_RGB8);
        fbComposite0.setClearColor(GL_COLOR_ATTACHMENT3, 0F, 0F, 0F, 0F);
        fbComposite0.setFilter(GL_COLOR_ATTACHMENT3, GL_LINEAR_MIPMAP_LINEAR);
        fbComposite0.setup();
//        fbComposite0 = new FrameBuffer(false, new int[] { GL_RGB16, GL_RGB8, GL_RGB16, GL_RGB8 });
        fbComposite1 = new FrameBuffer(displayWidth, displayHeight);
        fbComposite1.setColorAtt(GL_COLOR_ATTACHMENT2, GL_RGB16);
        fbComposite1.setClearColor(GL_COLOR_ATTACHMENT2, 0F, 0F, 0F, 0F);
        fbComposite1.setFilter(GL_COLOR_ATTACHMENT2, GL_LINEAR_MIPMAP_LINEAR);
        fbComposite1.setup();
        fbComposite2 = new FrameBuffer(displayWidth, displayHeight);
        fbComposite2.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGB16);
        fbComposite2.setClearColor(GL_COLOR_ATTACHMENT0, 1.0F, 1.0F, 1.0F, 1.0F);
        fbComposite2.setup();
    }

    
    void releaseShaders() {
        if (composite1 != null) {
            composite1.release();
            composite1 = null;
        }
        if (composite2 != null) {
            composite2.release();
            composite2 = null;
        }
        if (composite3 != null) {
            composite3.release();
            composite3 = null;
        }
        if (compositeFinal != null) {
            compositeFinal.release();
            compositeFinal = null;
        }
    }
    void releaseFrameBuffers() {

        if (fbComposite0 != null) {
            fbComposite0.cleanUp();
            fbComposite0 = null;
        }
        if (fbComposite1 != null) {
            fbComposite1.cleanUp();
            fbComposite1 = null;
        }
        if (fbComposite2 != null) {
            fbComposite2.cleanUp();
            fbComposite2 = null;
        }
        if (sceneFB != null) {
            sceneFB.cleanUp();
            sceneFB = null;
        }
        Engine.setSceneFB(null);
    }

    @Override
    public void release() {
        releaseShaders();
        releaseFrameBuffers();
    }

    @Override
    public void init() {
        initShaders();
    }
}
