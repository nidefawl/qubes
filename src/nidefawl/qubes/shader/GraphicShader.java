package nidefawl.qubes.shader;

import static org.lwjgl.opengl.GL20.*;

import java.util.HashMap;
import java.util.Map.Entry;

import org.lwjgl.opengl.ARBGeometryShader4;
import org.lwjgl.opengl.GL30;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.meshing.BlockFaceAttr;
import nidefawl.qubes.util.GameError;

public class GraphicShader extends Shader {
    int fragShader = -1;
    int vertShader = -1;
    int geometryShader = -1;
    private String attr = "";
    HashMap<String, Integer> customOutputLocations = new HashMap<>();

    public GraphicShader(String name, ShaderSource vertCode, ShaderSource fragCode, ShaderSource geomCode, IShaderDef def) {
        super(name);

        if (fragCode.isEmpty()) {
            throw new GameError("Failed reading shader source: "+name);
        }
        this.customOutputLocations.putAll(fragCode.customOutputLocations);
        
        this.fragShader = compileShader(GL_FRAGMENT_SHADER, fragCode, name);
        
        if (!vertCode.isEmpty()) {
            this.vertShader = compileShader(GL_VERTEX_SHADER, vertCode, name);
            this.attr = vertCode.getAttrTypes();
        } else {
            this.vertShader = Shader.shVertexFullscreenTri;
            this.attr = "none";
        }

        if (!geomCode.isEmpty()) {
            this.geometryShader = compileShader(ARBGeometryShader4.GL_GEOMETRY_SHADER_ARB, geomCode, name);
        }
        attach(def);
        linkProgram();
    }

    @Override
    public void attach(IShaderDef def) {
        this.shader = glCreateProgram();
        SHADERS++;
        Engine.checkGLError("glCreateProgramObjectARB");
        if (this.fragShader > 0) {
            glAttachShader(this.shader, this.fragShader);
            Engine.checkGLError("glAttachObjectARB");
        }
        if (this.vertShader > 0) {
            glAttachShader(this.shader, this.vertShader);
            Engine.checkGLError("glAttachObjectARB");
        }
        if (this.geometryShader > 0) {
            glAttachShader(this.shader, this.geometryShader);
            Engine.checkGLError("glAttachObjectARB");
        }
        if ("none".equals(attr)) {
            //skip
        } else if ("particle".equals(attr)) {
            glBindAttribLocation(this.shader, 0, "in_texcoord");
            glBindAttribLocation(this.shader, 1, "in_position");
            glBindAttribLocation(this.shader, 2, "in_color");
        } else if ("staticmodel".equals(attr)) {
            glBindAttribLocation(this.shader, 0, "in_position");
            glBindAttribLocation(this.shader, 1, "in_normal");
            glBindAttribLocation(this.shader, 2, "in_texcoord");
            glBindAttribLocation(this.shader, 3, "in_offset");
            glBindAttribLocation(this.shader, 4, "in_color");
        } else if ("model".equals(attr)) {
            glBindAttribLocation(this.shader, 0, "in_position");
            glBindAttribLocation(this.shader, 1, "in_normal");
            glBindAttribLocation(this.shader, 2, "in_texcoord");
            glBindAttribLocation(this.shader, 3, "in_bones1");
            glBindAttribLocation(this.shader, 4, "in_bones2");
            glBindAttribLocation(this.shader, 5, "in_weights1");
            glBindAttribLocation(this.shader, 6, "in_weights2");
        } else if ("shadow".equals(attr)) {
            glBindAttribLocation(this.shader, 0, "in_position");
            glBindAttribLocation(this.shader, 1, "in_texcoord");
            glBindAttribLocation(this.shader, 2, "in_blockinfo");
        } else {
            for (int i = 0; i < Tess.attributes.length; i++) {
                glBindAttribLocation(this.shader, i, Tess.attributes[i]);
                if (Game.GL_ERROR_CHECKS)
                    Engine.checkGLError("glBindAttribLocationARB "+this.name +" ("+this.shader+"): "+Tess.attributes[i]+" = "+i);
            }
            for (int i = 0; i < BlockFaceAttr.attributes.length; i++) {
                glBindAttribLocation(this.shader, i+Tess.attributes.length, BlockFaceAttr.attributes[i]);
                if (Game.GL_ERROR_CHECKS)
                    Engine.checkGLError("glBindAttribLocationARB "+this.name +" ("+this.shader+"): "+BlockFaceAttr.attributes[i]+" = "+i);
            }
        }
        if (def instanceof IGraphicsShaderDef) {
            ((IGraphicsShaderDef)def).bindFragDataLocations(this.shader);
        } else if (customOutputLocations.isEmpty()) {
            GL30.glBindFragDataLocation(this.shader, 0, "out_Color");
            GL30.glBindFragDataLocation(this.shader, 1, "out_Normal");
            GL30.glBindFragDataLocation(this.shader, 2, "out_Material");
            GL30.glBindFragDataLocation(this.shader, 3, "out_Light");
            GL30.glBindFragDataLocation(this.shader, 1, "out_FinalMaterial");
            GL30.glBindFragDataLocation(this.shader, 2, "out_Velocity");
        } else {
            for (Entry<String, Integer> s : customOutputLocations.entrySet()) {
//                System.out.println("bind output "+s.getKey()+" to "+s.getValue());
                GL30.glBindFragDataLocation(this.shader, s.getValue(), s.getKey());
            }
        }
    }
    public void release() {
        this.valid = false;
        if (this.shader > 0) {
            if (this.vertShader > 0)
                glDetachShader(this.shader, this.vertShader);
            if (this.fragShader > 0)
                glDetachShader(this.shader, this.fragShader);
            if (this.geometryShader > 0)
                glDetachShader(this.shader, this.geometryShader);
            if (this.fragShader > 0)
                glDeleteShader(this.fragShader);
            if (this.vertShader > 0 && !isGlobalProgram(this.vertShader))
                glDeleteShader(this.vertShader);
            if (this.geometryShader > 0)
                glDeleteShader(this.geometryShader);
            
            glDeleteProgram(this.shader);
            this.shader = this.vertShader = this.fragShader = this.geometryShader = -1;
            for (AbstractUniform uni : this.uniforms.values()) {
                uni.release();
            }
            this.uniforms.clear();
            if (Game.GL_ERROR_CHECKS)
                Engine.checkGLError("release Shader "+this.name +" ("+this.shader+")");
        }
        SHADERS--;
    }


}
