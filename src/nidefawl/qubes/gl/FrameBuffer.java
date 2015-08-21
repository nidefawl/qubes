package nidefawl.qubes.gl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import nidefawl.qubes.Main;
import nidefawl.qubes.util.GameError;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

public class FrameBuffer implements IFrameBuffer {
    private static final int MAX_COLOR_ATT    = 8;
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
    private final boolean[]      clearBuffer = new boolean[MAX_COLOR_ATT];
    private final float[][]      clearColor = new float[MAX_COLOR_ATT][];

    public FrameBuffer(int renderWidth, int renderHeight) {
        this.renderWidth = renderWidth;
        this.renderHeight = renderHeight;
        for (int a = 0; a < clearColor.length; a++) {
            clearColor[a] = new float[4];
        }
    }

    public void setColorAtt(int att, int fmt) {
        att -= GL_COLOR_ATTACHMENT0;
        if (colorAttFormats[att] != 0) {
            throw new IllegalArgumentException("GL_COLOR_ATTACHMENT" + att + " is already set");
        }
        colorAttFormats[att] = fmt;
        colorAttMinFilters[att] = GL_LINEAR;
    }


    public void setFilter(int att, int filter) {
        att -= GL_COLOR_ATTACHMENT0;
        if (colorAttFormats[att] == 0) {
            throw new IllegalArgumentException("GL_COLOR_ATTACHMENT" + att + " not set");
        }
        colorAttMinFilters[att] = filter;
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

    public void setup() {
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
        if (Main.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glGenFramebuffers");
        this.drawBufAtt = BufferUtils.createIntBuffer(this.numColorTextures);
        IntBuffer colorTextures = BufferUtils.createIntBuffer(numTextures);
        glGenTextures(colorTextures);
        if (Main.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glGenTextures");
        colorTextures.rewind();
        this.fb = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.fb);
        if (Main.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glBindFramebuffer");
        GL20.glDrawBuffers(GL_NONE);
        if (Main.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glDrawBuffers");
        glReadBuffer(GL_NONE);
        if (Main.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glReadBuffer");
        colorTextures.rewind();
        for (int i = 0; i < colorAttFormats.length; i++) {
            if (colorAttFormats[i] != 0) {
                int att = GL_COLOR_ATTACHMENT0 + i;
                int tex = colorTextures.get();
                setupTexture(tex, colorAttFormats[i], colorAttMinFilters[i]);
                this.colorAttTextures[i] = tex;
                GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, att, tex, 0);
                if (Main.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glFramebufferTexture (color " + i + ")");
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

    public void unbindCurrentFrameBuffer() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        if (Main.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glBindFramebuffer");
    }

    public void bind() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.fb);
        if (Main.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glBindFramebuffer");
    }

    public void setupTexture(int texture, int format, int minfilter) {
        glBindTexture(GL_TEXTURE_2D, texture);
        if (Main.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glBindTexture");
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, minfilter);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexImage2D(GL_TEXTURE_2D, 0, format, renderWidth, renderHeight, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, (ByteBuffer) null);
        if (Main.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glTexImage2D");
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private void createDepthTextureAttachment(int texture) {
        glBindTexture(GL_TEXTURE_2D, texture);
        if (Main.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glBindTexture (depth)");
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
        if (this.isShadowDepthBuffer) {

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, GL30.GL_COMPARE_REF_TO_TEXTURE);
        } else {

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL14.GL_DEPTH_TEXTURE_MODE, GL_LUMINANCE);
        }
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST); 
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
//        glTexParameteri (GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_NONE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, renderWidth, renderHeight, 0, GL_DEPTH_COMPONENT, GL_FLOAT, (ByteBuffer) null);
        if (Main.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glTexImage2D (depth)");
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, texture, 0);
        if (Main.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glFramebufferTexture (depth)");
        glBindTexture(GL_TEXTURE_2D, 0);
    }
    @Override
    public int getTexture(int i) {
        if (this.colorAttFormats[i] == 0) {
            throw new IllegalStateException("Framebuffer does not have texture for attachment "+i);
        }
        return this.colorAttTextures[i];
    }

    @Override
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
    
    
    @Override
    public void setDrawAll() {
        if (this.numColorTextures > 0)
        GL20.glDrawBuffers(this.drawBufAtt);
    }
    
    @Override
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

    public void cleanUp() {
        if (this.fb != 0) {
            GL30.glDeleteFramebuffers(this.fb);
            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glDeleteFramebuffers");
        }
        IntBuffer colorTextures = BufferUtils.createIntBuffer(MAX_COLOR_ATT+1);
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
            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("FrameBuffers.glDeleteTextures");
        }
    }
}