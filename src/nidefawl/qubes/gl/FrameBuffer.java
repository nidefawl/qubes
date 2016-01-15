package nidefawl.qubes.gl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LEVEL;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_RGBA16UI;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.opengl.*;

import nidefawl.qubes.Game;
import nidefawl.qubes.util.*;

public class FrameBuffer implements IManagedResource {
    private static final int MAX_COLOR_ATT    = 8;
    public static int FRAMEBUFFERS = 0;
    private final int        renderWidth;
    private final int        renderHeight;
    private int              fb;
    private IntBuffer        drawBufAtt;
    private boolean          hasDepth;
    private boolean isShadowDepthBuffer;
    private int              numColorTextures;
    private int              depthTexture;
    private final int[]      colorAttTextures   = new int[MAX_COLOR_ATT];
    private final int[]      colorAttFormats    = new int[MAX_COLOR_ATT];
    private final int[]      colorAttMinFilters = new int[MAX_COLOR_ATT];
    private final int[]      colorAttMagFilters = new int[MAX_COLOR_ATT];
    private final boolean[]      clearBuffer = new boolean[MAX_COLOR_ATT];
    private final float[][]      clearColor = new float[MAX_COLOR_ATT][];
    private int colorTexExtFmt=GL12.GL_BGRA;
    private int colorTexExtType=GL12.GL_UNSIGNED_INT_8_8_8_8_REV;
    /**
     * @param colorTexExtFmt the colorTexExtFmt to set
     */
    public void setColorTexExtFmt(int colorTexExtFmt) {
        this.colorTexExtFmt = colorTexExtFmt;
    }
    /**
     * @param colorTexExtType the colorTexExtType to set
     */
    public void setColorTexExtType(int colorTexExtType) {
        this.colorTexExtType = colorTexExtType;
    }

    public FrameBuffer(int renderWidth, int renderHeight) {
        this.renderWidth = renderWidth;
        this.renderHeight = renderHeight;
        for (int a = 0; a < clearColor.length; a++) {
            clearColor[a] = new float[4];
        }
        FRAMEBUFFERS++;
    }
    public static FrameBuffer make(IResourceManager resMgr, int renderWidth, int renderHeight, int type, boolean clearBlack) {
        FrameBuffer f = new FrameBuffer(renderWidth, renderHeight);
        f.setColorAtt(GL_COLOR_ATTACHMENT0, type);
        f.setFilter(GL_COLOR_ATTACHMENT0, GL_LINEAR, GL_LINEAR);
        if (clearBlack) {
            f.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
        } else {
            f.setClearColor(GL_COLOR_ATTACHMENT0, 1.0F, 1.0F, 1.0F, 1.0F);
        }
        f.setup(resMgr);
        f.clearFrameBuffer();
        return f;
    }
    public static FrameBuffer make(IResourceManager resMgr, int renderWidth, int renderHeight, int type) {
        return make(resMgr, renderWidth, renderHeight, type, false);
    }

    public void setColorAtt(int att, int fmt) {
        att -= GL_COLOR_ATTACHMENT0;
        if (colorAttFormats[att] != 0) {
            throw new IllegalArgumentException("GL_COLOR_ATTACHMENT" + att + " is already set");
        }
        colorAttFormats[att] = fmt;
        colorAttMinFilters[att] = GL_LINEAR;
        colorAttMagFilters[att] = GL_LINEAR;
    }


    public void setFilter(int att, int filter, int magfilter) {
        att -= GL_COLOR_ATTACHMENT0;
        if (colorAttFormats[att] == 0) {
            throw new IllegalArgumentException("GL_COLOR_ATTACHMENT" + att + " not set");
        }
        colorAttMinFilters[att] = filter;
        colorAttMagFilters[att] = magfilter;
    }
    public void setClearColor(int att, float r, float g, float b, float a) {
        att -= GL_COLOR_ATTACHMENT0;
        if (colorAttFormats[att] == 0) {
            throw new IllegalArgumentException("GL_COLOR_ATTACHMENT" + att + " not set");
        }
        this.clearColor[att][0] = r;
        this.clearColor[att][1] = g;
        this.clearColor[att][2] = b;
        this.clearColor[att][3] = a;
        this.clearBuffer[att] = true;
    }

    public void setHasDepthAttachment() {
        hasDepth = true;
    }

    public void setShadowBuffer() {
        hasDepth = true;
        isShadowDepthBuffer = true;
    }

    public void setup(IResourceManager resMgr) {
        if (resMgr != null)
        resMgr.addResource(this);
        int numTextures = 0;
        for (int i = 0; i < colorAttFormats.length; i++) {
            if (colorAttFormats[i] != 0) {
                numTextures++;
            }
        }
        this.numColorTextures = numTextures;
        if (hasDepth) {
            numTextures++;
        }
        if (numTextures == 0) {
            throw new IllegalStateException("No textures defined");
        }
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glGenFramebuffers");
        this.drawBufAtt = Memory.createIntBufferGC(this.numColorTextures);

        IntBuffer colorTextures = Memory.createIntBufferGC(numTextures);
        
        glGenTextures(colorTextures);
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glGenTextures");
        colorTextures.rewind();
        this.fb = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.fb);
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glBindFramebuffer");
        GL20.glDrawBuffers(GL_NONE);
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glDrawBuffers");
        glReadBuffer(GL_NONE);
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glReadBuffer");
        colorTextures.rewind();
        for (int i = 0; i < colorAttFormats.length; i++) {
            if (colorAttFormats[i] != 0) {
                int att = GL_COLOR_ATTACHMENT0 + i;
                int tex = colorTextures.get();
                setupTexture(tex, colorAttFormats[i], colorAttMinFilters[i], colorAttMagFilters[i]);
                this.colorAttTextures[i] = tex;
                GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, att, tex, 0);
                if (Game.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glFramebufferTexture (color " + i + ")");
                this.drawBufAtt.put(att);
            }
        }
        drawBufAtt.rewind();

        if (hasDepth) {
            this.depthTexture = colorTextures.get();
            createDepthTextureAttachment(this.depthTexture);
        }
        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new GameError("Framebuffer is incomplete (" + status + ")");
        }
        GL20.glDrawBuffers(this.drawBufAtt);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    public static void unbindFramebuffer() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glUnbindCurrentFrameBuffer");
    }
    public static void unbindReadFramebuffer() {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glUnbindCurrentReadBuffer");
    }

    public void bind() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.fb);
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glBindFramebuffer");
    }
    public void bindRead() {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, this.fb);
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glBindFramebuffer");
    }

    public void setupTexture(int texture, int format, int minfilter, int magFilter) {
        glBindTexture(GL_TEXTURE_2D, texture);
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glBindTexture");
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, minfilter);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, magFilter);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);
        if (format == GL_RGBA16UI) {
            glTexImage2D(GL_TEXTURE_2D, 0, format, renderWidth, renderHeight, 0, GL30.GL_BGRA_INTEGER, GL11.GL_UNSIGNED_INT, (ByteBuffer) null);
        } else {
            glTexImage2D(GL_TEXTURE_2D, 0, format, renderWidth, renderHeight, 0, colorTexExtFmt, colorTexExtType, (ByteBuffer) null);
        }
        
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glTexImage2D");
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private void createDepthTextureAttachment(int texture) {
        glBindTexture(GL_TEXTURE_2D, texture);
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glBindTexture (depth)");
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        if (this.isShadowDepthBuffer) {

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
//            glTexParameteri(GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, GL30.GL_COMPARE_REF_TO_TEXTURE);
        } else {

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
//            glTexParameteri(GL_TEXTURE_2D, GL14.GL_DEPTH_TEXTURE_MODE, GL_LUMINANCE);
        }
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glTexParameteri (depth)");

//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST); 
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
//        glTexParameteri (GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_NONE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT32, renderWidth, renderHeight, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (ByteBuffer) null);

//        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, renderWidth, renderHeight, 0, GL_DEPTH_COMPONENT, GL_FLOAT, (ByteBuffer) null);
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glTexImage2D (depth)");
        glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, texture, 0);
        if (Game.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glFramebufferTexture (depth)");
        glBindTexture(GL_TEXTURE_2D, 0);
    }
    
    public int getTexture(int i) {
        if (this.colorAttFormats[i] == 0) {
            throw new IllegalStateException("Framebuffer does not have texture for attachment "+i);
        }
        return this.colorAttTextures[i];
    }

    public int getDepthTex() {
        if (!hasDepth) {
            throw new IllegalStateException("Framebuffer does not have depth texture");
        }
        return this.depthTexture;
    }

    
    public void clearDepth() {
        if (this.hasDepth) {
            glClear(GL_DEPTH_BUFFER_BIT);
        }
    }

    
    public void clearColor() {
        GL20.glDrawBuffers(GL_COLOR_ATTACHMENT0);
        glClear(GL_COLOR_BUFFER_BIT);
        GL20.glDrawBuffers(this.drawBufAtt);
    }

    
    public void clearColorBlack() {
        GL20.glDrawBuffers(GL_COLOR_ATTACHMENT0);
        glClearColor(0,0,0,0);
        glClear(GL_COLOR_BUFFER_BIT);
        glClearColor(this.clearColor[0][0], this.clearColor[0][1], this.clearColor[0][2], this.clearColor[0][3]);
        GL20.glDrawBuffers(this.drawBufAtt);
    }
    
    
    public void setDrawAll() {
        if (this.numColorTextures > 0)
            GL20.glDrawBuffers(this.drawBufAtt);
    }
    
    public void clearFrameBuffer() {
        this.clearDepth();
        int cleared = 0;
        for (int i = 0; i < clearBuffer.length; i++) {
            if (this.clearBuffer[i]) {
                if (this.numColorTextures > 1) {
                    int att = GL_COLOR_ATTACHMENT0 + i;
                    GL20.glDrawBuffers(att);
                    cleared++;
                }
                glClearColor(this.clearColor[i][0], this.clearColor[i][1], this.clearColor[i][2], this.clearColor[i][3]);
                glClear(GL_COLOR_BUFFER_BIT);
            }
        }
        if (cleared > 0) {
            this.setDrawAll();
        }
    }

    public void release() {
        if (this.fb != 0) {
            GL30.glDeleteFramebuffers(this.fb);
            if (Game.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glDeleteFramebuffers");
        }
        IntBuffer colorTextures = Memory.createIntBufferGC(MAX_COLOR_ATT+1);
        for (int i = 0; i < colorAttTextures.length; i++) {
            if (colorAttTextures[i] != 0) {
                colorTextures.put(colorAttTextures[i]);
            }   
        }
        if (hasDepth) {
            colorTextures.put(this.depthTexture);
        }
        colorTextures.flip();
        if (colorTextures.remaining() > 0) {
            glDeleteTextures(colorTextures);
            if (Game.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glDeleteTextures");
        }
        FRAMEBUFFERS--;
    }

    public int getWidth() {
        return this.renderWidth;
    }

    public int getHeight() {
        return this.renderHeight;
    }
    /**
     * @return
     */
    public int getFB() {
        return this.fb;
    }
    @Override
    public EResourceType getType() {
        return EResourceType.FRAMEBUFFER;
    }
}