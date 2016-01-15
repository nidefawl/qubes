package nidefawl.qubes.shader;

import nidefawl.qubes.util.GameError;

public class ShaderCompileError extends GameError {

    private static final long serialVersionUID = -4538958766169500694L;

    private String name;
    private String log;

    private ShaderSource code;


    public ShaderCompileError(ShaderSource src, String string, String log) {
        super("Failed to compile " + string + " shader");
        this.name = string;
        this.log = log;
        if ( src != null)
            this.log =  src.decorateErrors(log);
        this.code =  src;
    }

    public ShaderCompileError(String line, String string, String log) {
        super("Failed to compile " + string + " shader");
        this.name = string;
        this.log = log + "\r\n" + line;
    }

    public String getName() {
        return this.name;
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
