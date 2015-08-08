package nidefawl.qubes;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import nidefawl.qubes.util.GameError;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

public class FrameBuffer {
    public static float renderScale = 1.0F;

    private int                fb;
    private int                renderWidth;
    private int                renderHeight;
    final IntBuffer colorTextures;

    public final IntBuffer drawBufAtt;

    private boolean hasDepth;

    private int numColorTextures;

    public FrameBuffer(boolean hasDepth, int[] textureFormats) {
        this.renderWidth = Math.round((float)GLGame.displayWidth * renderScale);
        this.renderHeight = Math.round((float)GLGame.displayHeight * renderScale);
        this.hasDepth = hasDepth;
        int numTextures = textureFormats.length;
        if (hasDepth) {
            numTextures++;
        }
        this.colorTextures = BufferUtils.createIntBuffer(numTextures);
        this.drawBufAtt = BufferUtils.createIntBuffer(textureFormats.length);
        this.numColorTextures = textureFormats.length;
        glGenTextures(this.colorTextures);
        Engine.checkGLError("FrameBuffers.glGenTextures");
        this.colorTextures.rewind();
        this.fb = GL30.glGenFramebuffers();
        Engine.checkGLError("FrameBuffers.glGenFramebuffers");
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.fb);
        Engine.checkGLError("FrameBuffers.glBindFramebuffer");
        GL20.glDrawBuffers(GL_NONE);
        Engine.checkGLError("FrameBuffers.glDrawBuffers");
        glReadBuffer(GL_NONE);
        Engine.checkGLError("FrameBuffers.glReadBuffer");
        this.colorTextures.rewind();
        for (int i = 0; i < textureFormats.length; i++) {
            int tex = this.colorTextures.get();
            setupTexture(tex, textureFormats[i]);
            createTextureAttachment(i, tex);
            this.drawBufAtt.put(GL30.GL_COLOR_ATTACHMENT0+i);
        }
        if (hasDepth) {
            createDepthTextureAttachment(this.colorTextures.get());    
        }
        

        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if(status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new GameError("Framebuffer is incomplete ("+status+")");
        }
        GL20.glDrawBuffers(this.drawBufAtt);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    public void cleanUp() {//call when closing the game
        this.colorTextures.rewind();
        if (this.fb != 0) {
            GL30.glDeleteFramebuffers(this.fb);
            Engine.checkGLError("FrameBuffers.glDeleteFramebuffers");
        }
        this.colorTextures.rewind();
        if (this.colorTextures.remaining() > 0) {
            glDeleteTextures(this.colorTextures);
            Engine.checkGLError("FrameBuffers.glDeleteTextures");
        }
        Engine.checkGLError("FrameBuffers.glDrawBuffers");
    }

    public void unbindCurrentFrameBuffer() {//call to switch to default frame buffer
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        Engine.checkGLError("FrameBuffers.glBindFramebuffer");
        glViewport(0, 0, GLGame.displayWidth, GLGame.displayHeight);
        Engine.checkGLError("FrameBuffers.glViewport");
        for (int i = 7; i >= 0; i--) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, 0);
        }
    }
    
    public void bind() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.fb);
        glViewport(0, 0, renderWidth, renderHeight);
        Engine.checkGLError("FrameBuffers.glViewport");
        for (int i = 0; i < this.numColorTextures; i++) {
            GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0 + i, this.colorTextures.get(i), 0);
        }
        if (hasDepth)
            GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, this.colorTextures.get(this.numColorTextures), 0);
        Engine.checkGLError("FrameBuffers.glBindFramebuffer");

        this.drawBufAtt.rewind();
        GL20.glDrawBuffers(this.drawBufAtt);
        Engine.checkGLError("FrameBuffers.glDrawBuffers");

    }

    public void setupTexture(int texture, int format) {
        glBindTexture(GL_TEXTURE_2D, texture);
        Engine.checkGLError("FrameBuffers.glBindTexture");
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexImage2D(GL_TEXTURE_2D, 0, format, renderWidth, renderHeight, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, (ByteBuffer) null);
        Engine.checkGLError("FrameBuffers.glTexImage2D");
    }
    public void createTextureAttachment(int unit, int texture) {
        GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0 + unit, texture, 0);
        Engine.checkGLError("FrameBuffers.glFramebufferTexture (color "+unit+")");
    }

    private void createDepthTextureAttachment(int texture) {
        glBindTexture(GL_TEXTURE_2D, texture);
        Engine.checkGLError("FrameBuffers.glBindTexture (depth)");
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST); 
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL14.GL_DEPTH_TEXTURE_MODE, GL_LUMINANCE);
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
//        glTexParameteri (GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_NONE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, renderWidth, renderHeight, 0, GL_DEPTH_COMPONENT, GL_FLOAT, (ByteBuffer)null);
        Engine.checkGLError("FrameBuffers.glTexImage2D (depth)");
        GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, texture, 0);
        Engine.checkGLError("FrameBuffers.glFramebufferTexture (depth)");
    }

    public int getTexture(int i) {
        return this.colorTextures.get(i);
    }

    public int getDepthTex() {
        return this.colorTextures.get(this.numColorTextures);
    }

    public void clearDepth() {
        if (this.hasDepth) {
            GL20.glDrawBuffers(GL_NONE);
            Engine.checkGLError("FrameBuffers.glDrawBuffers");
            glClear(GL_STENCIL_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        }
    }
    public void clear(int n, float r, float g, float b, float a) {
        if (this.numColorTextures > 0) {
            GL20.glDrawBuffers(GL30.GL_COLOR_ATTACHMENT0+n);
            glClearColor(r, g, b, a);
            glClear(GL_COLOR_BUFFER_BIT);
        }
    }
    public void setDrawAll() {
        GL20.glDrawBuffers(this.drawBufAtt);
    }
}