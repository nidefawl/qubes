package nidefawl.qubes.vulkan;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.util.ITessState;

public class VkTesselatorState extends AbstractVkTesselatorState implements ITessState {
    BufferPair buffer;
    private boolean usageDynamic;
    private VKContext vkContext;
    public VkTesselatorState(VKContext ctxt) {
        this(ctxt, false);
    }
    public VkTesselatorState(VKContext ctxt, boolean usageDynamic) {
        this.vkContext = ctxt;
        this.buffer = ctxt.getFreeBuffer();
        this.usageDynamic = usageDynamic;
    }
    public void destroy() {
        vkContext.orphanResource(this.buffer);
    }
    public VkTesselatorState tag(String string) {
        this.buffer.tag(string);
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
    @Override
    public BufferPair getBuffer() {
        return this.buffer;
    }
    @Override
    public void swapBuffers() {
        this.vkContext.orphanResource(this.buffer);
        this.buffer = this.vkContext.getFreeBuffer();
    }


}
