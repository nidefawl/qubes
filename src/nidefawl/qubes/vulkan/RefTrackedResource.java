package nidefawl.qubes.vulkan;

public interface RefTrackedResource extends IVkResource {
    void flagUse(int idx);
    void unflagUse(int idx);
    boolean isFree();
}
