package nidefawl.qubes.shader;

import nidefawl.qubes.util.GameError;

public class ShaderCompileError extends GameError {

    private static final long serialVersionUID = -4538958766169500694L;
    
    private String name;
    private String log;

    public ShaderCompileError(ShaderSource geomCode, String string, String log) {
        super("Failed to compile "+string+" shader");
        
        this.name = string;
        this.log = log;
        this.log = geomCode.decorateErrors(log);
    }
    
    public ShaderCompileError(String line, String string, String log) {
        super("Failed to compile "+string+" shader");
        this.name = string;
        this.log = log +"\r\n"+line;
    }
    public String getName() {
        return name;
    }
    public String getLog() {
        return log;
    }

}
