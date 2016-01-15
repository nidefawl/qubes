package nidefawl.qubes.shader;

import static org.lwjgl.opengl.GL20.*;

import org.lwjgl.opengl.GL43;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.util.GameError;

public class ComputeShader extends Shader {
    int computeShader = -1;

    public ComputeShader(String name, ShaderSource source) {
        super(name);
        this.computeShader = glCreateShader(GL43.GL_COMPUTE_SHADER);
        Engine.checkGLError("glCreateShader");
        if (computeShader == 0) {
            throw new GameError("Failed creating compute shader");
        }

        glShaderSource(this.computeShader, source.getSource());
        Engine.checkGLError("glShaderSourceARB");
        glCompileShader(this.computeShader);
        String log = getLog(0, this.computeShader);
        Engine.checkGLError("getLog");
        if (getStatus(this.computeShader, GL_COMPILE_STATUS) != 1) {
            Engine.checkGLError("getStatus");
            throw new ShaderCompileError(source, this.name+" compute", log);
        } else if (!log.isEmpty()) {
            System.out.println(this.name+" compute");
            System.out.println(log);
        }
        attach();
        linkProgram();
    }

    @Override
    public void attach() {
        this.shader = glCreateProgram();
        SHADERS++;
        Engine.checkGLError("glCreateProgramObjectARB");
        if (this.computeShader > 0) {
            glAttachShader(this.shader, this.computeShader);
            Engine.checkGLError("glAttachObjectARB");
        }
    }
    public void release() {
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
