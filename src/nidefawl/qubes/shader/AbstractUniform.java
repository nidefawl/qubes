package nidefawl.qubes.shader;

public abstract class AbstractUniform {
    final String name;
    final int loc;
    boolean first = true;
    public AbstractUniform(String name, int loc) {
        this.name = name;
        this.loc = loc;
    }

    public boolean validLoc() {
        return this.loc >= 0;
    }
    public abstract boolean set();
    
    public void release() {
    }
}
