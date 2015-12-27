package nidefawl.qubes.gl;

import static org.lwjgl.opengl.GL11.*;

import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Iterator;

import org.lwjgl.opengl.*;

import com.google.common.collect.Maps;

import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.shader.Shaders;

public class GLDebugTextures {

    private int format;
    private String name;
    private String pass;
    private int h;
    private int w;
    private int tex;
    private int d;
    private int flags;

    GLDebugTextures(String pass, String name, int format, int w, int h, int d, int flags) {
        this.pass = pass;
        this.name = name;
        this.format = format;
        this.w = w;
        this.h = h;
        this.d = d;
        this.flags = flags;
    }
    static HashMap<String, HashMap<String, GLDebugTextures>> textures = Maps.newLinkedHashMap();

    public static void readTexture(String name, String string, int texture) {
        readTexture(name, string, texture, 0);
    }
    public static void readTexture(String name, String string, int texture, int flags) {
        HashMap<String, GLDebugTextures> texMap = textures.get(name);
        if (texMap == null) {
            texMap = Maps.newLinkedHashMap();
            textures.put(name, texMap);
        }
        GLDebugTextures tex = texMap.get(string);
        int target = GL11.GL_TEXTURE_2D;
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL.bindTexture(GL13.GL_TEXTURE0, target, texture);
        int w = GL11.glGetTexLevelParameteri(target, 0, GL11.GL_TEXTURE_WIDTH);
        int h = GL11.glGetTexLevelParameteri(target, 0, GL11.GL_TEXTURE_HEIGHT);
        int d = GL11.glGetTexLevelParameteri(target, 0, GL12.GL_TEXTURE_DEPTH);
        int int_format = GL11.glGetTexLevelParameteri(target, 0, GL11.GL_TEXTURE_INTERNAL_FORMAT);
//
        if (tex != null && (tex.w != w || tex.h != h || tex.d != d)) {
            tex.release();
            tex = null;
        }
        if (tex == null) {
            tex = new GLDebugTextures(name, string, int_format, w, h, d, flags);
            texMap.put(string, tex);
            tex.tex = GL11.glGenTextures();
            if (int_format == GL11.GL_RGBA) {
                int_format = GL11.GL_RGBA8;
            }
            GL.bindTexture(GL13.GL_TEXTURE0, target, tex.tex);
            GL.glTexStorage2D(target, 1, int_format, w, h);
        }
        ARBCopyImage.glCopyImageSubData(texture, target, 0, 0, 0, 0, tex.tex, target, 0, 0, 0, 0, w, h, d);
        GL.bindTexture(GL13.GL_TEXTURE0, target, 0);
    }

    private void release() {
        GL11.glDeleteTextures(this.tex);
    }
    public static void onResize() {
        for (HashMap<String, GLDebugTextures> texMap : textures.values()) {
            for (GLDebugTextures tex : texMap.values()) {
                tex.release();
            }
        }
        textures.clear();
    }

    public static void drawAll(int displayWidth, int displayHeight) {
        glPushAttrib(-1);
        glDisable(3008);
        glDepthFunc(519);
        glDepthMask(false);
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        Iterator<String> itMaps = textures.keySet().iterator();
        float w = 160;
        
        float gap = 10;
        float maxW = 200;
        
        int background = 0x333333;
        int border = 0x888888;
        if (w > maxW) w = maxW;
        float h = w*(displayHeight/(float)displayWidth);
        Shaders.textured.enable();
        float xpos = gap*4;
        float ypos = gap*4;
        int x = 0;
        while (itMaps.hasNext()) {
            String mapName = itMaps.next();
            HashMap<String, GLDebugTextures> map = textures.get(mapName);
            Iterator<GLDebugTextures> it = map.values().iterator();
            int y = 0;
            float left = xpos+x*(w+gap);
            float right = left+w;
            FontRenderer.get("Arial", 18, 0, 20).drawString(mapName, left, ypos, -1, true, 1);
            while (it.hasNext()) {
                GLDebugTextures tex = it.next();
                GL.bindTexture(GL13.GL_TEXTURE0, GL11.GL_TEXTURE_2D, tex.tex);
                float top = ypos+y*(h+gap);
                float bottom = top+h;
                if (bottom >= displayHeight-gap*2) {
                    x++;
                    y= 0;
                     top = ypos+y*(h+gap);
                     bottom = top+h;
                     left = xpos+x*(w+gap);
                     right = left+w;
                }
                Shaders.colored.enable();
                Tess.instance.setColorF(background, 1);
                Tess.instance.add(left, bottom, 0, 0, 1);
                Tess.instance.add(right, bottom, 0, 1, 1);
                Tess.instance.add(right, top, 0, 1, 0);
                Tess.instance.add(left, top, 0, 0, 0);
                Tess.instance.draw(GL11.GL_QUADS);

                if (tex.format == GL30.GL_RGBA16UI) {
                    Shaders.renderUINT.enable();
                } else if ((tex.flags&0x1)!=0) {
                    Shaders.tonemap.enable();
                } else if ((tex.flags&0x2)!=0) {
                    Shaders.depthBufShader.enable();
                } else {

                    Shaders.textured.enable();
                }
                Tess.instance.setColorF(-1, 1);
                Tess.instance.add(left, bottom, 0, 0, 0);
                Tess.instance.add(right, bottom, 0, 1, 0);
                Tess.instance.add(right, top, 0, 1, 1);
                Tess.instance.add(left, top, 0, 0, 1);
                Tess.instance.draw(GL11.GL_QUADS);
                Shaders.colored.enable();
                Tess.tessFont.setColorF(222, 0.5F);
                Tess.tessFont.add(left, bottom, 0, 0, 0);
                Tess.tessFont.add(right, bottom, 0, 1, 0);
                Tess.tessFont.add(right, bottom-20, 0, 1, 1);
                Tess.tessFont.add(left, bottom-20, 0, 0, 1);
                Tess.tessFont.draw(7);
                Tess.instance.setColorF(border, 1);
                Tess.instance.add(left, bottom, 0, 0, 1);
                Tess.instance.add(right, bottom, 0, 1, 1);
                Tess.instance.add(right, top, 0, 1, 0);
                Tess.instance.add(left, top, 0, 0, 0);
                Tess.instance.add(left, bottom, 0, 0, 1);
                GL11.glLineWidth(2);
                Tess.instance.draw(GL11.GL_LINE_STRIP);
                Shaders.textured.enable();
                FontRenderer.get("Arial", 16, 0, 20).drawString(tex.name, left, bottom, -1, true, 1);
                if (!it.hasNext()) {
                    break;
                }
                y++;
            }
            x++;
        }
        glDepthFunc(GL_LEQUAL);
        glDepthMask(true);
        glPopAttrib();
    
    }

}
