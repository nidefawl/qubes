package nidefawl.qubes.render.impl.gl;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL30.GL_RGB16F;
import static org.lwjgl.opengl.GL30.GL_RGBA16F;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.FrameBuffer;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.render.BlurRenderer;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.ShaderCompileError;
import nidefawl.qubes.util.EResourceType;
import nidefawl.qubes.util.GameMath;

public class BlurRendererGL extends BlurRenderer {

    private FrameBuffer fbBlur1;
    private FrameBuffer fbBlur2;
    private FrameBuffer fbSSRBlurredX;
    private FrameBuffer fbSSRBlurredY;
    public Shader       shaderBlurSeperate;
    public Shader       shaderBlurKawase;
    private boolean startup = true;

    public void initShaders() {
        try {
            pushCurrentShaders();
            AssetManager assetMgr = AssetManager.getInstance();
            Shader new_shaderBlur = assetMgr.loadShader(this, "filter/blur_kawase");
            Shader new_shaderBlurSeperate = assetMgr.loadShader(this, "filter/blur_seperate");
            popNewShaders();
            shaderBlurSeperate = new_shaderBlurSeperate;
            shaderBlurKawase = new_shaderBlur;
            shaderBlurSeperate.enable();
            shaderBlurSeperate.setProgramUniform1i("texColor", 0);
            shaderBlurKawase.enable();
            shaderBlurKawase.setProgramUniform1i("texColor", 0);
            Shader.disable();
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
    public int renderBlur1PassDownsample(int input) {
        FrameBuffer buffer = input == fbBlur1.getTexture(0) ? fbBlur2 : fbBlur1; // 4x downsampled
        shaderBlurKawase.enable();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, input); // Albedo
        int kawaseKernSizeSetting = 2;
        int[] kawaseKernPasses = kawaseKernelSizePasses[kawaseKernSizeSetting];
        Engine.setViewport(0, 0, buffer.getWidth(), buffer.getHeight());
        for (int p = 0; p < kawaseKernPasses.length; p++) {
            shaderBlurKawase.setProgramUniform3f("blurPassProp", w1, h1, kawaseKernPasses[p]);
            buffer.bind();
            buffer.clearFrameBuffer();
            GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, input);
            Engine.drawFSTri();
            input = buffer.getTexture(0);
            buffer = buffer == fbBlur1 ? fbBlur2 : fbBlur1;
        }
        Engine.setDefaultViewport();
        return input;
    }
    public int renderBlurSeperate(int texture, int i) {
        float maxBlurRadius = i;
        fbSSRBlurredX.bind();
        fbSSRBlurredX.clearFrameBuffer();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, texture); //SSR
        shaderBlurSeperate.enable();
        shaderBlurSeperate.setProgramUniform2f("_TexelOffsetScale", maxBlurRadius / (float)fbSSRBlurredX.getWidth(), 0f);
        Engine.drawFSTri();
        fbSSRBlurredY.bind();
        fbSSRBlurredY.clearFrameBuffer();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, fbSSRBlurredX.getTexture(0)); //COLOR
        shaderBlurSeperate.setProgramUniform2f("_TexelOffsetScale", 0f, maxBlurRadius / (float)fbSSRBlurredX.getHeight());
        Engine.drawFSTri();
        return fbSSRBlurredY.getTexture(0);
    }
    public void resize(int displayWidth, int displayHeight) {
        releaseAll(EResourceType.FRAMEBUFFER);
        initBlurKawase(displayWidth, displayHeight, 2);
        initBlurSepeate(displayWidth, displayHeight, 2);
    }
    

    public void initBlurSepeate(int displayWidth, int displayHeight, int blurDownSample) {
        this.fbSSRBlurredX =FrameBuffer.make(this, displayWidth, displayHeight, GL_RGBA16F, true);
        this.fbSSRBlurredY =FrameBuffer.make(this, displayWidth, displayHeight, GL_RGBA16F, true);
    }
    public void initBlurKawase(int inputWidth, int inputHeight, int blurDownSample) {
        this.w1 = 1.0f/(float)inputWidth;
        this.h1 = 1.0f/(float)inputHeight;
        int[] blurSize = GameMath.downsample(inputWidth, inputHeight, blurDownSample);
        fbBlur1 = FrameBuffer.make(this, blurSize[0], blurSize[1], GL_RGB16F);
        fbBlur2 = FrameBuffer.make(this, blurSize[0], blurSize[1], GL_RGB16F);
    }
    
    public void init() {
        initShaders();
    }
    @Override
    public void preinit() {
    }
}
