package nidefawl.qubes.vulkan;

public class VkTesselatorState extends AbstractVkTesselatorState {
    VkBuffer bufferV;
    VkBuffer bufferI;
    public VkTesselatorState(VKContext ctxt) {
        bufferV = new VkBuffer(ctxt);
        bufferI = new VkBuffer(ctxt);
    }
    @Override
    public VkBuffer getVertexBuffer() {
        return bufferV;
    }

    @Override
    public VkBuffer getIndexBuffer() {
        return bufferI;
    }
    public void destroy() {
        this.bufferI.destroy();
        this.bufferV.destroy();
    }
    public VkTesselatorState tag(String string) {
        this.bufferI.tag(string+"_index");
        this.bufferV.tag(string+"_vertex");
        return this;
    }


}
