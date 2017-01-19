package nidefawl.qubes.render.gui;

import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL40;

import nidefawl.qubes.gl.*;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.vr.VR;

public class VRGui {

    Vector3f pos = new Vector3f();
    Vector3f normal = new Vector3f();
    Vector3f vTmp = new Vector3f();
    Matrix4f tmp = new Matrix4f();
    boolean hasHit = false;
    public void updatePos() {
        
    }
    public void update(Matrix4f pose, Vector3f camPos, PositionMouseOver ctrlPos) {
        Engine.composeModelView(VR.pose, camPos, true, tmp);
        tmp.invert();
        float z = -8;
        this.pos.set(0, 0, z);
        Matrix4f.transform(tmp, this.pos, this.pos);
        normal.set(0, 0, 1);
        hasHit = false;
        if (ctrlPos.vDir != null) {
            float f = Vector3f.dot(this.normal, ctrlPos.vDir);
            if (Math.abs(f) > 0.0001f) {
                Vector3f.sub(pos, ctrlPos.vOrigin, vTmp);
                float t = Vector3f.dot(this.vTmp, this.normal) / f;
                if (t >= 0) {
                    vTmp.set(ctrlPos.vDir);
                    vTmp.scale(t*1.00f);
//                    vTmp.addVec(ctrlPos.vOrigin);
//                    Vector3f.sub(vTmp, pos, vTmp);
                    hasHit = true;
                }
            }
        }
        this.pos.set(0, 0, z);
    }
    
    public void render(float fTime, int texture) {

        Shaders.textured3D.enable();
        Shaders.textured3D.setProgramUniform1f("color_brightness", 1f);
        GL.bindTexture(GL13.GL_TEXTURE0, GL_TEXTURE_2D, texture);
        
        glDisable(GL11.GL_CULL_FACE);
        Engine.setBlend(true);
        
        float scale = 0.015f;
        float rW = 1920 * scale;
        float rH = 1080 * scale;
        Tess tess = Tess.instance;
        
        GL40.glBlendFuncSeparatei(0, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        tess.setOffset(pos.x, pos.y, pos.z);
        tess.setColorF(-1, 1.0f);
        tess.add(-rW, rH, 0, 0, 1);
        tess.add(rW, rH, 0, 1, 1);
        tess.add(rW, -rH, 0, 1, 0);
        tess.add(-rW, -rH, 0, 0, 0);
        tess.draw(GL_QUADS);

        Shaders.textured3D.setProgramUniform1f("color_brightness", 0.1f);
        if (hasHit) {
//            System.out.println(vTmp);
            Shaders.colored3D.enable();
            Shaders.colored3D.setProgramUniform1f("color_brightness", 1f);
            float w = 16f*scale;
            tess.setColorF(-1, 1.0f);
            
            tess.setOffset(vTmp.x, vTmp.y, pos.z+0.1f);
            tess.add(-w, w, 0, 0, 1);
            tess.add(w, w, 0, 1, 1);
            tess.add(w, -w, 0, 1, 0);
            tess.add(-w, -w, 0, 0, 0);
            tess.draw(GL_QUADS);
            tess.setOffset(0, 0, 0);
            Shaders.colored3D.setProgramUniform1f("color_brightness", 0.1f);
        }
        Shader.disable();
        GL40.glBlendFuncSeparatei(0, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        Engine.setBlend(false);
        glEnable(GL11.GL_CULL_FACE);
    }
}
