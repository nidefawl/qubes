package nidefawl.qubes.shader;

import static org.lwjgl.opengl.GL20.*;

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

    public GraphicShader(String name, ShaderSource vertCode, ShaderSource fragCode, ShaderSource geomCode) {
        super(name);

        if (vertCode.isEmpty() && fragCode.isEmpty()) {
            throw new GameError("Failed reading shader source: "+name);
        }
        if (!fragCode.isEmpty()) {
            this.fragShader = glCreateShader(GL_FRAGMENT_SHADER);
            Engine.checkGLError("glCreateShader");
            if (fragShader == 0) {
                throw new GameError("Failed creating fragment shader");
            }

            glShaderSource(this.fragShader, fragCode.getSource());
            Engine.checkGLError("glShaderSourceARB");
            glCompileShader(this.fragShader);
            String log = getLog(0, this.fragShader);
            Engine.checkGLError("getLog");
            if (getStatus(this.fragShader, GL_COMPILE_STATUS) != 1) {
                Engine.checkGLError("getStatus");
                System.err.println(log);
                throw new ShaderCompileError(fragCode, this.name+" fragment", log);
            } else if (!log.isEmpty()) {
                System.out.println(this.name+" fragment");
                System.out.println(log);
            }
        }


        if (!vertCode.isEmpty()) {
            this.vertShader = glCreateShader(GL_VERTEX_SHADER);
            Engine.checkGLError("glCreateShader");
            if (this.vertShader == 0) {
                throw new GameError("Failed creating vertex shader");
            }

            glShaderSource(this.vertShader, vertCode.getSource());
            Engine.checkGLError("glShaderSourceARB");
            glCompileShader(this.vertShader);
            String log = getLog(0, this.vertShader);
            Engine.checkGLError("getLog");
            if (getStatus(this.vertShader, GL_COMPILE_STATUS) != 1) {
                Engine.checkGLError("getStatus");
                throw new ShaderCompileError(vertCode, this.name+" vertex", log);
            } else if (!log.isEmpty()) {
                System.out.println(this.name+" vertex");
                System.out.println(log);
            }
            this.attr = vertCode.getAttrTypes();
        }


        if (!geomCode.isEmpty()) {
            this.geometryShader = glCreateShader(ARBGeometryShader4.GL_GEOMETRY_SHADER_ARB);
            Engine.checkGLError("glCreateShader");
            if (this.geometryShader == 0) {
                throw new GameError("Failed creating geometry shader");
            }

            glShaderSource(this.geometryShader, geomCode.getSource());
            Engine.checkGLError("glShaderSourceARB");
            glCompileShader(this.geometryShader);
            String log = getLog(0, this.geometryShader);
            Engine.checkGLError("getLog");
            if (getStatus(this.geometryShader, GL_COMPILE_STATUS) != 1) {
                Engine.checkGLError("getStatus");
                throw new ShaderCompileError(geomCode, this.name+" geometry", log);
            } else if (!log.isEmpty()) {
                System.out.println(this.name+" geometryShader");
                System.out.println(log);
            }
        }
        attach();
        linkProgram();
    }

    @Override
    public void attach() {
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
        if ("particle".equals(attr)) {
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
        GL30.glBindFragDataLocation(this.shader, 0, "out_Color");
        GL30.glBindFragDataLocation(this.shader, 1, "out_Normal");
        GL30.glBindFragDataLocation(this.shader, 2, "out_Material");
        GL30.glBindFragDataLocation(this.shader, 3, "out_Light");
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
            if (this.vertShader > 0)
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
