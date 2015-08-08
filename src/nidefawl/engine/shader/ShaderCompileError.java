package nidefawl.engine.shader;

import nidefawl.engine.util.GameError;

public class ShaderCompileError extends GameError {

    private String name;
    private String log;

    public ShaderCompileError(String string, String log) {
        super("Failed to compile "+string+" shader");
        this.name = string;
        this.log = log;
    }
    public String getName() {
        return name;
    }
    public String getLog() {
        return log;
    }

}
