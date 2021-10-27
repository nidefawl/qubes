package nidefawl.qubes.gl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE4;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP;

import java.util.*;

import org.lwjgl.opengl.*;

import com.google.common.collect.Maps;

import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.render.RenderersGL;
import nidefawl.qubes.shader.Shaders;

public class GLDebugTextures {

    public int format;
    public String name;
    public String pass;
    public int h;
    public int w;
    public int d;
    public int tex;
    public int target;
    public int flags;
    public boolean valid;
    private boolean isOutput;
    public static boolean show    = false;

    GLDebugTextures(String pass, String name, int format, int w, int h, int d, int target, int flags, boolean isOutput) {
        this.pass = pass;
        this.name = name;
        this.format = format;
        this.w = w;
        this.h = h;
        this.d = d;
        this.target = target;
        this.flags = flags;
        this.valid = true;
        this.isOutput = isOutput;
    }
    static class StageMap {
        final HashMap<String, GLDebugTextures> map = new HashMap<>();
        final ArrayList<String> inputs = new ArrayList<>();
        final ArrayList<String> outputs = new ArrayList<>();
        public GLDebugTextures get(String string) {
            return map.get(string);
        }
        public void put(boolean isOutput, String string, GLDebugTextures tex) {
            map.put(string, tex);
            if (isOutput) {
                outputs.add(string);
            } else {
                inputs.add(string);
            }
        }
        public Collection<GLDebugTextures> values() {
            return map.values();
        }
        public Iterator<String> keysOrderedIterator() {
            return new Iterator<String>() {
                int idx = 0;
                @Override
                public String next() {
                    int curIdx = idx;
                    idx++;
                    if (curIdx < inputs.size())
                        return inputs.get(curIdx);
                    curIdx-=inputs.size();
                    return outputs.get(curIdx);
                }
                
                @Override
                public boolean hasNext() {
                    return (idx) < (inputs.size()+outputs.size());
                }
            };
        }
    }
    static HashMap<String, StageMap> textures = Maps.newLinkedHashMap();
    static HashMap<Integer, GLDebugTextures> alltextures = Maps.newLinkedHashMap();
    private static GLDebugTextures selTex;
    private static boolean triggered;

    public static void readTexture(boolean isOutput, String name, String string, int texture) {
        readTexture(isOutput, name, string, texture, 0);
    }
    public static int readTexture(boolean isOutput, String name, String string, int texture, int flags) {
        return readTexture(isOutput, name, string, texture, flags, GL11.GL_TEXTURE_2D);
    }
    public static int readTexture(boolean isOutput, String name, String string, int texture, int flags, int target) {
        StageMap texMap = textures.get(name);
        if (texMap == null) {
            texMap = new StageMap();
            textures.put(name, texMap);
        }
        Engine.checkGLError("pre readTexture "+name+":"+string);
        GLDebugTextures tex = texMap.get(string);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL.bindTexture(GL13.GL_TEXTURE0, target, texture);
        Engine.checkGLError("bindTexture "+name+":"+string);
        int w, h, d;
        int int_format;
        if (target == GL_TEXTURE_CUBE_MAP) {
            w = GL11.glGetTexLevelParameteri(GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, GL11.GL_TEXTURE_WIDTH);
            h = GL11.glGetTexLevelParameteri(GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, GL11.GL_TEXTURE_HEIGHT);
            d = 6;
            int_format = GL11.glGetTexLevelParameteri(GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, GL11.GL_TEXTURE_INTERNAL_FORMAT);
            Engine.checkGLError("glGetTexLevelParameteri "+name+":"+string);
        } else {
            w = GL11.glGetTexLevelParameteri(target, 0, GL11.GL_TEXTURE_WIDTH);
            Engine.checkGLError("glGetTexLevelParameteri "+name+":"+string+ " GL_TEXTURE_WIDTH");
            h = GL11.glGetTexLevelParameteri(target, 0, GL11.GL_TEXTURE_HEIGHT);
            Engine.checkGLError("glGetTexLevelParameteri "+name+":"+string+ " GL_TEXTURE_HEIGHT");
            d = GL11.glGetTexLevelParameteri(target, 0, GL12.GL_TEXTURE_DEPTH);
            Engine.checkGLError("glGetTexLevelParameteri "+name+":"+string+ " GL_TEXTURE_DEPTH");
            int_format = GL11.glGetTexLevelParameteri(target, 0, GL11.GL_TEXTURE_INTERNAL_FORMAT);
            Engine.checkGLError("glGetTexLevelParameteri "+name+":"+string);
        }
//
        boolean realloc = false;
        if (tex != null && (tex.w != w || tex.h != h || tex.d != d || tex.flags != flags)) {
            tex.w=w;
            tex.h=h;
            tex.d=d;
            tex.flags=flags;
            alltextures.remove(tex.tex);
            GL.deleteTexture(tex.tex);
            realloc = true;
        }
        if (tex == null) {
            tex = new GLDebugTextures(name, string, int_format, w, h, d, target, flags, isOutput);
            texMap.put(isOutput, string, tex);
            realloc = true;
        }
        if (realloc) {
            tex.tex = GL11.glGenTextures();
            if (int_format == GL11.GL_RGBA) {
                int_format = GL11.GL_RGBA8;
            }
            GL.bindTexture(GL13.GL_TEXTURE0, target, tex.tex);
            Engine.checkGLError("bindTexture "+name+":"+string);
            GL.glTexStorage2D(target, 1, int_format, w, h);
            Engine.checkGLError("glTexStorage2D "+name+":"+string);
            alltextures.put(tex.tex, tex);
        }
        ARBCopyImage.glCopyImageSubData(texture, target, 0, 0, 0, 0, tex.tex, target, 0, 0, 0, 0, w, h, d);
        Engine.checkGLError("glCopyImageSubData "+name+":"+string);
        GL.bindTexture(GL13.GL_TEXTURE0, target, 0);
        Engine.checkGLError("readtexture "+name+":"+string);
        return tex.tex;
    }

    private void release() {
        this.valid = false;
        GL.deleteTexture(this.tex);
    }
    public static void onResize() {
        for (StageMap texMap : textures.values()) {
            for (GLDebugTextures tex : texMap.values()) {
                tex.release();
            }
        }
        textures.clear();
    }

    public static void drawAll(int displayWidth, int displayHeight) {
        glPushAttrib(-1);
        glDepthFunc(GL_ALWAYS);
        Engine.enableDepthMask(false);
        glDisable(GL_DEPTH_TEST);
        GL30.glEnablei(GL_BLEND, 0);
//        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        Iterator<String> itMaps = textures.keySet().iterator();
        float w = displayWidth/(textures.keySet().size()+1);
        
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
        double mouseX = Mouse.getX();
        double mouseY = Mouse.getY();
        boolean grab = !Mouse.isGrabbed();
        GLDebugTextures mouseOver = null;
        int titleY = 0;
        int n = 0;
        while (itMaps.hasNext()) {
            float yposOffset = ypos;
            String mapName = itMaps.next();
            StageMap map = textures.get(mapName);
            Iterator<String> it = map.keysOrderedIterator();
            int y = 0;
            float left = xpos+x*(w+gap);
            float right = left+w;
            titleY=w < 150 ? ((x%2)*16) : 0;
            FontRenderer.get(0, 18, 0).drawString(mapName, left, yposOffset-titleY, -1, true, 1);
            boolean isOutput = false;
            Engine.checkGLError("pre draw debug textures");
            while (it.hasNext()) {
                GLDebugTextures tex = map.get(it.next());
                Engine.checkGLError("bindTexture");
                float top = yposOffset+y*(h+gap);
                float bottom = top+h;
                if (bottom >= displayHeight-gap*2) {
                    x++;
                    y= 0;
                    if (isOutput) {
                        yposOffset-=40;
                    }
                     top = yposOffset+y*(h+gap);
                     bottom = top+h;
                     left = xpos+x*(w+gap);
                     right = left+w;
                }
                if (tex.isOutput && !isOutput) {
//                  y = 0;
//                  x = 0;
                  isOutput = true;
                  Shaders.colored.enable();
                  float c = left+(right-left)/2;
                  float topt = yposOffset+y*(h+gap);
                  float bottomt = top+32;
                  Tess.instance.setColorF(0xababab, 1);
                  Tess.instance.add(c-32, topt, 0, 0, 1);
                  Tess.instance.add(c, bottomt, 0, 1, 0);
                  Tess.instance.add(c+32, topt, 0, 1, 1);
                  Tess.instance.draw(GL11.GL_TRIANGLES);
                  yposOffset+=40;
                  top = yposOffset+y*(h+gap);
                  bottom = top+h;
              }
                Shaders.colored.enable();
                Tess.instance.setColorF(background, 1);
                if (grab && mouseX > left && mouseX < right && mouseY > top && mouseY < bottom) {
                    mouseOver = tex;
                }
                Tess.instance.add(left, bottom, 0, 0, 1);
                Tess.instance.add(right, bottom, 0, 1, 1);
                Tess.instance.add(right, top, 0, 1, 0);
                Tess.instance.add(left, top, 0, 0, 0);
                Tess.instance.draw(GL11.GL_QUADS);
                GL.bindTexture(GL13.GL_TEXTURE0, tex.target, tex.tex);
                tex.bindShader();
                Tess.instance.setColorF(-1, 1);
                Tess.instance.add(left, bottom, 0, 0, 0);
                Tess.instance.add(right, bottom, 0, 1, 0);
                Tess.instance.add(right, top, 0, 1, 1);
                Tess.instance.add(left, top, 0, 0, 1);
                Tess.instance.draw(GL11.GL_QUADS);

                n++;
                Shaders.colored.enable();
                Tess.tessFont.setColorF(222, 0.5F);
                Tess.tessFont.add(left, bottom, 0, 0, 0);
                Tess.tessFont.add(right, bottom, 0, 1, 0);
                Tess.tessFont.add(right, bottom-20, 0, 1, 1);
                Tess.tessFont.add(left, bottom-20, 0, 0, 1);
                Tess.tessFont.draw(7);
                int bColor = border;
                int texColor = -1;
                if (mouseOver == tex) {
                    bColor = 0x9999FF;
                    texColor = 0xFF4444;
                }
                Tess.instance.setColorF(bColor, 1);
                Tess.instance.add(left, bottom, 0, 0, 1);
                Tess.instance.add(right, bottom, 0, 1, 1);
                Tess.instance.add(right, top, 0, 1, 0);
                Tess.instance.add(left, top, 0, 0, 0);
                Tess.instance.add(left, bottom, 0, 0, 1);
                Engine.lineWidth(2);
                Tess.instance.draw(GL11.GL_LINE_STRIP);
                Shaders.textured.enable();
                FontRenderer.get(0, 16, 0).drawString(tex.name +" ("+tex.tex+")", left, bottom, texColor, true, 1);
                if (!it.hasNext()) {
                    break;
                }
                Engine.checkGLError("post draw texture entry");
                y++;
            }
            x++;
        }
//        //draw skybox cubemap using Equirectangular projection
//        {
//
//            float yposOffset = ypos;
//            String mapName = itMaps.next();
//            StageMap map = textures.get(mapName);
//            Iterator<String> it = map.keysOrderedIterator();
//            int y = 0;
//
//            w = displayWidth*2.0f/3.0f;
//            h = displayWidth/4.0f;
//            float left = displayWidth-w;
//            float right = left+w;
//            float top = displayHeight/2.0f;
//            float bottom = displayHeight;
//            titleY=w < 150 ? ((x%2)*16) : 0;
//            GL.bindTexture(GL_TEXTURE4, GL_TEXTURE_CUBE_MAP, RenderersGL.skyRenderer.fbSkybox.getTexture(0));//SKYBOX CUBEMAP
//            Tess.instance.setColorF(-1, 1);
//            Tess.instance.add(left, bottom, 0, 0, 0);
//            Tess.instance.add(right, bottom, 0, 1, 0);
//            Tess.instance.add(right, top, 0, 1, 1);
//            Tess.instance.add(left, top, 0, 0, 1);
//            Tess.instance.draw(GL11.GL_QUADS);
//        }
        if (Mouse.isButtonDown(0)) {
            if (!triggered) {
                triggered = true;
                selTex = mouseOver;
            }
        } else {
            triggered = false;
        }
        Engine.enableDepthMask(true);
        glPopAttrib();
    
    }

    public void bindShader() {
        if ((this.flags & 0x10) != 0) {
            Shaders.cubemap2dProjection.enable();
            return;
        }
        if ((this.flags & 0x8) != 0) {
            Shaders.textured.enable();
            return;
        }
        if (this.format == GL30.GL_RGBA16UI) {
            Shaders.renderUINT.enable();
        } else if ((this.flags & 0x1) != 0) {
            Shaders.tonemap.enable();
        } else if ((this.flags & 0x2) != 0) {
            Shaders.depthBufShader.enable();
        } else if ((this.flags & 0x4) != 0) {
            Shaders.tonemap.enable();
            Shaders.tonemap.setProgramUniform1f("constexposure", 30);
        } else {

            Shaders.textured.enable();
        }
    }
    public static int getTexture(String s, String s2) {
        StageMap map = textures.get(s);
        GLDebugTextures tex = map == null ? null : map.get(s2);
        return tex == null ? 0 : tex.tex;
    }
    
    public static GLDebugTextures getSelected() {    
        return selTex != null && selTex.valid ? selTex : null;
    }
    public int get() {
        return this.tex;
    }
    public static void drawFullScreen(GLDebugTextures t) {
        try {
            t.bindShader();
            GL.bindTexture(GL13.GL_TEXTURE0, t.getTarget(), t.get());
            Engine.drawFullscreenQuad();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public int getTarget() {
        return this.target;
    }
    public static boolean isShow() {
        return show || selTex != null;
    }
    public static void setShow(boolean show) {
        GLDebugTextures.show = show;
    }
    public static void toggleDebugTex() {
        
        GLDebugTextures lightOut1 = null;
        GLDebugTextures lightOut2 = null;
        StageMap map = textures.get("compute_light_0");
        if (map != null) {
            lightOut1 = map.get("output");
            lightOut2 = map.get("output2");
            if ( lightOut1 == null) { //start up updates
                show = true;
            } else {
                show = false;
            }
        }
        if (selTex == null) {
            selTex = lightOut1;
        } else if (selTex == lightOut1) {
            selTex = lightOut2;
        } else {
            selTex = null;
        }
    }

}
