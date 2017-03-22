package nidefawl.qubes.shader;

import static org.lwjgl.opengl.GL20.*;

import org.lwjgl.opengl.GL43;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.Engine;

public class ComputeShader extends Shader {
    int computeShader = -1;

    public ComputeShader(String name, ShaderSource source, IShaderDef def) {
        super(name);
        this.computeShader = compileShader(GL43.GL_COMPUTE_SHADER, source, name);
        attach(def);
        linkProgram();
    }

    @Override
    public void attach(IShaderDef def) {
        this.shader = glCreateProgram();
        SHADERS++;
        Engine.checkGLError("glCreateProgramObjectARB");
        if (this.computeShader > 0) {
            glAttachShader(this.shader, this.computeShader);
            Engine.checkGLError("glAttachObjectARB");
        }
    }
    public void destroy() {
        this.valid = false;
        if (this.shader > 0) {
            if (this.computeShader > 0)
                glDetachShader(this.shader, this.computeShader);
            if (this.computeShader > 0)
                glDeleteShader(this.computeShader);
            
            glDeleteProgram(this.shader);
            this.shader = this.computeShader = -1;
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
