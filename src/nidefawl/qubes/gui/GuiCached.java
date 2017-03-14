//package nidefawl.qubes.gui;
//
//import static org.lwjgl.opengl.GL11.*;
//import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
//
//import org.lwjgl.opengl.*;
//import static org.lwjgl.vulkan.VK10.*;
//
//import nidefawl.qubes.gl.Engine;
//import nidefawl.qubes.gl.FrameBuffer;
//import nidefawl.qubes.gl.GL;
//import nidefawl.qubes.shader.Shaders;
//import nidefawl.qubes.vulkan.VkRenderPasses;
//
//public class GuiCached extends Gui {
//    private Gui gui;
//    private FrameBuffer fbDbg;
//    private boolean refresh;
//    private nidefawl.qubes.vulkan.FrameBuffer fbVk;
//
//    public GuiCached(Gui gui) {
//        this.gui = gui;
//    }
//    @Override
//    public void setPos(int x, int y) {
//        super.setPos(x, y);
//        this.gui.setPos(x, y);
//    }
//
//    @Override
//    public void setSize(int w, int h) {
//        if (w != this.width || h != this.height) {
//            super.setSize(w, h);
//            if (!Engine.isVulkan) {
//                if (fbDbg != null) fbDbg.release();
//                fbDbg = new FrameBuffer(w, h);
//                fbDbg.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA16);
//                fbDbg.setFilter(GL_COLOR_ATTACHMENT0, GL_NEAREST, GL_NEAREST);
//                fbDbg.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
//                fbDbg.setHasDepthAttachment();
//                fbDbg.setup(null);
//            } else {
////                fbVk = new nidefawl.qubes.vulkan.FrameBuffer(Engine.vkContext);
////                fbVk.fromRenderpass(VkRenderPasses.passFramebuffer, 0, VK_IMAGE_USAGE_SAMPLED_BIT);
//            }
//            this.gui.setSize(w, h);
//        }
//    }
//
//    public void render(float fTime, double mx, double mY) {
//        if (!Engine.isVulkan) {
//            Shaders.textured.enable();
//            Engine.pxStack.push(0, 0, -20);
//            GL.bindTexture(GL13.GL_TEXTURE0, GL11.GL_TEXTURE_2D, fbDbg.getTexture(0));
//            Engine.drawFullscreenQuad();
//            Engine.pxStack.pop();
//        } else {
//            this.gui.render(0, 0, 0);
//        }
//    }
//    public void refresh() {
//        ((GuiOverlayStats) this.gui).refresh(); // TODO: make interface, or something
//        this.refresh = true;
//    }
//    @Override
//    public void update() {
//        super.update();
//    }
//    @Override
//    public void initGui(boolean first) {
//    }
//    public void preRenderUpdate() {
//        if (refresh) {
//            refresh = false;
//            if (!Engine.isVulkan) {
//                fbDbg.bind();
//                fbDbg.clearFrameBuffer();
//                GL40.glBlendFuncSeparatei(0, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
//                Engine.setBlend(true);
//            } else {
////                Engine.setPipeStateTextured2D();
//            }
//            this.gui.render(0, 0, 0);
//            if (!Engine.isVulkan) {
//                GL40.glBlendFuncSeparatei(0, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
//                FrameBuffer.unbindFramebuffer();
//            }
//        }
//    }
//    
//}
