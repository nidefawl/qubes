/**
 * 
 */
package nidefawl.qubes.render.post;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.lwjgl.opengl.*;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetBinary;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.util.EResourceType;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.SimpleResourceManager;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class SMAA {

    SimpleResourceManager mgr = new SimpleResourceManager();

    public Shader       shaderAAEdge;
    public Shader       shaderAABlendWeight;
    public Shader       shaderAANeighborBlend;
    private FrameBuffer fbFlipInput;
    private FrameBuffer fbAAEdge;
    private FrameBuffer fbAAWeightBlend;
    private int areaTex;
    private int searchTex;

    private boolean srgb;
    /**
     * 
     */
    public final static int SMAA_PRESET_LOW = 0;
    public final static int SMAA_PRESET_MEDIUM = 1;
    public final static int SMAA_PRESET_HIGH = 2;
    public final static int SMAA_PRESET_ULTRA = 3;
    final static String[] qualDefines = { "SMAA_PRESET_LOW", "SMAA_PRESET_MEDIUM", "SMAA_PRESET_HIGH", "SMAA_PRESET_ULTRA" };
    public static String[] qualDesc = { "Low", "Medium", "High", "Ultra" };
    public SMAA(final int quality) {
        this(quality, false);
    }
    public SMAA(final int quality, boolean srgb) {
        System.out.println("SMAA init");
        this.srgb = srgb;
        AssetManager assetMgr = AssetManager.getInstance();
        AssetBinary areaTexData = assetMgr.loadBin("textures/areatex.bin");
        AssetBinary searchTexData = assetMgr.loadBin("textures/searchtex.bin");
        areaTex = makeAATexture(areaTexData.getData(), areaTex, 160, 560, 320, GL_RG8);
        searchTex = makeAATexture(searchTexData.getData(), searchTex, 64, 16, 64, GL_R8);
        IShaderDef def = new IShaderDef() {
            @Override
            public String getDefinition(String define) {
                if ("SMAA_QUALITY".equals(define)) {
                    return "#define "+qualDefines[quality];
                }
                return null;
            }
        };
        

        try {
            Shader new_AAEdge = assetMgr.loadShader(mgr, "post/SMAA/SMAA_edgedetection", def);
            Shader new_BlendWeight = assetMgr.loadShader(mgr, "post/SMAA/SMAA_blend_weight", def);
            Shader new_neighbor_blend = assetMgr.loadShader(mgr, "post/SMAA/SMAA_neighbor_blend", def);
            shaderAAEdge = new_AAEdge;
            shaderAABlendWeight = new_BlendWeight;
            shaderAANeighborBlend = new_neighbor_blend;
            shaderAAEdge.enable();
            shaderAAEdge.setProgramUniform1i("texColor", 0);
            shaderAABlendWeight.enable();
            shaderAABlendWeight.setProgramUniform1i("edgesTex", 0);
            shaderAABlendWeight.setProgramUniform1i("areaTex", 1);
            shaderAABlendWeight.setProgramUniform1i("searchTex", 2);
            shaderAANeighborBlend.enable();
            shaderAANeighborBlend.setProgramUniform1i("texColor", 0);
            shaderAANeighborBlend.setProgramUniform1i("blendTex", 1);
            Shader.disable();
        } catch (ShaderCompileError e) {
            e.printStackTrace();
            System.out.println("shader " + e.getName() + " failed to compile");
            System.out.println(e.getLog());
            throw new GameError(e);
        }
    }
    
    public void init(int displayWidth, int displayHeight) {
//        this.fbFlipInput = FrameBuffer.make(mgr, displayWidth, displayHeight, GL_RGBA8);
        int format = srgb?GL21.GL_SRGB8_ALPHA8:GL_RGBA8;
        this.fbFlipInput = FrameBuffer.make(mgr, displayWidth, displayHeight, format, true, false);
        this.fbAAEdge = FrameBuffer.make(mgr, displayWidth, displayHeight, format, true, true);
        this.fbAAWeightBlend = FrameBuffer.make(mgr, displayWidth, displayHeight, format, true, true);
        FrameBuffer.unbindFramebuffer();
    }
    

    private int makeAATexture(byte[] dataIn, int i, int w, int h, int stride, int format) {
        if (i<=0) {
            i = GL11.glGenTextures();
        }
        if (format == GL_RG8 && w*h*2 != dataIn.length) {
            throw new GameError("Invalid SMAA area texture");
        }
        if (format == GL_R8 && w*h != dataIn.length) {
            throw new GameError("Invalid SMAA search texture");
        }

        byte[] data =  dataIn;
        
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, i);
        glTexParameteri(GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
        glTexParameteri(GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        Engine.checkGLError("glTexParameteri");
        ByteBuffer directBuf = ByteBuffer.allocateDirect(data.length).order(ByteOrder.nativeOrder());
        directBuf.clear();
        directBuf.put(data, 0, data.length);
        directBuf.position(0).limit(data.length);
        GL11.glTexImage2D(GL_TEXTURE_2D, 0, format, w, h, 0, format == GL_R8 ? GL_RED : GL_RG, GL11.GL_UNSIGNED_BYTE, directBuf);
        Engine.checkGLError("GL11.glTexImage2D");
        return i;
    }
    
    public void releaseAll(EResourceType t) {
        if (t == null) this.mgr.release();
        else this.mgr.releaseAll(t);
    }

    /**
     * @param texture
     * @param finalTarget 
     */
    public void render(int texture, int debugTexture, FrameBuffer finalTarget) {
        if (true||Game.GL_ERROR_CHECKS) {
            boolean b = glGetBoolean(GL_DEPTH_TEST);
            if (!b) {
                System.err.println("NEED DEPTH TESTING!");
            }
            if (Engine.isBlend()) {
                System.err.println("NEED disabled blend!");
            }
            b = glGetBoolean(GL_DEPTH_WRITEMASK);
            if (!b) {
                System.err.println("NEED GL_DEPTH_WRITEMASK for discard!");
            }
            int i = glGetInteger(GL_DEPTH_FUNC);
            if (i != GL_LEQUAL) {
                System.err.println("GL_DEPTH_FUNC != GL_LEQUAL, CHECK STATES");
            }
           
        }

        Engine.updateOrthoMatrix(Game.displayWidth, Game.displayHeight, true);
        UniformBuffer.updateOrtho();
        fbFlipInput.bind();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, texture);
        Shaders.textured.enable();
        Engine.drawFSQuad(3);
        if (debugTexture != 1) {
            renderSMAA(fbFlipInput.getTexture(0), debugTexture, finalTarget);
        }
        Engine.updateOrthoMatrix(Game.displayWidth, Game.displayHeight, false);
        UniformBuffer.updateOrtho();
        if (debugTexture > 0) {
            if (finalTarget == null) {
                FrameBuffer.unbindFramebuffer();
            } else {
                finalTarget.bind();
                finalTarget.clearFrameBuffer();
            }
            int tex = 0;
            switch (debugTexture) {
                case 1:
                    Shaders.textured.enable();
                    tex = fbFlipInput.getTexture(0);
                    break;
                case 2:
                    Shaders.textured_to_srgb.enable();
                    tex = fbAAEdge.getTexture(0);
                    break;
                case 3:
                    Shaders.textured_to_srgb.enable();
                    tex = fbAAWeightBlend.getTexture(0);
                    break;
            }
            GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, tex);
            Engine.drawFSQuad(1);
        }
    }
    public void renderSMAA(int texture, int debugTexture, FrameBuffer finalTarget) {
        
        
        fbAAEdge.bind();
        fbAAEdge.clearFrameBuffer();
        shaderAAEdge.enable();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, texture);
        Engine.drawFSQuad(2);
        if (debugTexture == 1) {
            return;
        }
        fbAAWeightBlend.bind();
        fbAAWeightBlend.clearFrameBuffer();
        
        //copy over the depth buffer
        fbAAEdge.bindRead();
        GL30.glBlitFramebuffer(0, 0, fbAAEdge.getWidth(), fbAAEdge.getHeight(), 0, 0, fbAAWeightBlend.getWidth(), fbAAWeightBlend.getHeight(), GL_DEPTH_BUFFER_BIT, GL_NEAREST);
        FrameBuffer.unbindReadFramebuffer();
        
        shaderAABlendWeight.enable();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, fbAAEdge.getTexture(0));
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, this.areaTex);
        GL.bindTexture(GL_TEXTURE2, GL_TEXTURE_2D, this.searchTex);
        
        glDepthFunc(GL_EQUAL); // only draw equal z fragments, +30% speed
//        Shaders.colored.enable();
        Engine.drawFSQuad(2);
        glDepthFunc(GL_LEQUAL);
        if (debugTexture == 2) {
            return;
        }
        if (finalTarget == null) FrameBuffer.unbindFramebuffer();
        else {
            finalTarget.bind();
            finalTarget.clearFrameBuffer();
        }
        shaderAANeighborBlend.enable();
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, texture);
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, fbAAWeightBlend.getTexture(0));
        Engine.drawFSQuad(3);
    }


}
