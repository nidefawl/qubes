package nidefawl.qubes.shader;

import nidefawl.qubes.util.GameError;

public class ShaderCompileError extends GameError {

    private static final long serialVersionUID = -4538958766169500694L;

    private String name;
    private String log;

    private ShaderSource code;

    public ShaderCompileError(ShaderSource shader, String string, String log) {
        super("Failed to compile " + string + " shader");

        this.name = string;
        this.log = log;
        if (shader != null)
            this.log = shader.decorateErrors(log);
        this.code = shader;
    }

    public ShaderCompileError(String line, String string, String log) {
        super("Failed to compile " + string + " shader");
        this.name = string;
        this.log = log + "\r\n" + line;
    }

    public String getName() {
        return name;
    }

    public String getLog() {
        return log;
    }

    public String getCode() {
        return code == null ? "" : code.getSource();
    }

    public ShaderSource getShaderSource() {
        return code;
    }

}
