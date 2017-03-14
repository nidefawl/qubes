package nidefawl.qubes.vulkan;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.util.ITessState;

public class VkTesselatorState extends AbstractVkTesselatorState implements ITessState {
    VkBuffer bufferV;
    VkBuffer bufferI;
    private boolean usageDynamic;
    public VkTesselatorState(VKContext ctxt) {
        this(ctxt, false);
    }
    public VkTesselatorState(VKContext ctxt, boolean usageDynamic) {
        bufferV = new VkBuffer(ctxt);
        bufferI = new VkBuffer(ctxt);
        this.usageDynamic = usageDynamic;
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
    @Override
    public void drawQuads() {
        bindAndDraw(Engine.getDrawCmdBuffer());
    }
    @Override
    public boolean isDynamic() {
        return this.usageDynamic;
    }


}
