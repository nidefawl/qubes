package nidefawl.qubes.shader;

public class AbstractUniform {
    final String name;
    final int loc;
    boolean first = true;
    public AbstractUniform(String name, int loc) {
        this.name = name;
        this.loc = loc;
    }
}
