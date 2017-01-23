package nidefawl.qubes.render.gui;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;

import java.util.ArrayList;
import java.util.HashMap;

import org.lwjgl.opengl.*;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.entity.Player;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.gui.AbstractUI;
import nidefawl.qubes.gui.Gui;
import nidefawl.qubes.gui.Tooltip;
import nidefawl.qubes.gui.windows.GuiContext;
import nidefawl.qubes.gui.windows.GuiWindow;
import nidefawl.qubes.gui.windows.GuiWindowManager;
import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.Ease;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.Matrix4f;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.vec.Vector4f;

public class VRGuiRenderer {
    public final static float         GUI_3D_SCALE = 1f / 256f;
    private final static int          rW           = 1920;
    private final static int          rH           = 1080;
    private static final float TIME_REMOVAL = 0.5f;
    private int                       texCursor;

    final HashMap<Gui, Gui3DPosition> map          = new HashMap<>();
    final ArrayList<Gui3DPosition>    list         = new ArrayList<>();
    final ArrayList<FrameBuffer>      inUseBuffers = new ArrayList<>();
    final ArrayList<FrameBuffer>      freeBuffers  = new ArrayList<>();
    private Gui3DPosition hit;

    public static class Gui3DPosition {
        FrameBuffer    fbGUIFixed;
        final Vector3f location              = new Vector3f();
        final Vector3f planeNormalWS         = new Vector3f();
        final Vector3f vIntersection         = new Vector3f();
        final Vector3f vIntersectionWS       = new Vector3f();
        final Vector3f vIntersectionGuiSpace = new Vector3f();
        final Matrix4f rotation              = new Matrix4f();
        final Matrix4f modelMatrix           = new Matrix4f();
        final Gui      gui;
        boolean        hasHit                = false;
        double         mouseX, mouseY;
        float          timeAdded;
        public float   timeRemoved;
        float          fadeOutF = 1.0f;
        float          fadeInF = 0.0f;
        public boolean canRemove;
        public boolean fadeOut;

        public Gui3DPosition(Gui gui, Vector3f guiPos, float absTime) {
            this.gui = gui;
            this.location.set(guiPos);
            this.timeAdded = absTime;
            this.rotation.setZero();
        }

        public void updatePosition(Matrix4f pose, Vector3f camPos, Vector3f viewDir) {
            Vector3f viewDirRev = Vector3f.pool(camPos);
            viewDirRev.subtract(location);
            viewDirRev.y = 0;
            viewDirRev.normaliseZero();
            Vector3f vx = Vector3f.cross(Vector3f.pool(0, 1, 0), viewDirRev, Vector3f.pool()).normaliseZero();
            float targetRot = -GameMath.atan2(-viewDirRev.x, vx.x);
            float lastRot = -GameMath.atan2(-rotation.m20, rotation.m00);
            float diff = targetRot - lastRot;
            if (diff > GameMath.PI) {
                lastRot += GameMath.PI * 2.0f;
            }
            if (diff < -GameMath.PI) {
                lastRot -= GameMath.PI * 2.0f;
            }
            diff = targetRot - lastRot;
            float stepSize = 1.0f;
            float maxAngle = GameMath.PI/3.0f;
            if (diff > maxAngle) {
                lastRot = lastRot + (diff - maxAngle*stepSize);
            }
            if (diff < -maxAngle) {
                lastRot = lastRot + (diff + maxAngle*stepSize);
            }
            if (rotation.m33 == 0.0f) {
                lastRot = targetRot;
            }
            rotation.setIdentity();
            rotation.rotate(lastRot, 0, 1, 0);
//          Vector3f vy = Vector3f.cross(viewDirRev, vx, Vector3f.pool()).normaliseZero();
//          rotation.setIdentity();
//          rotation.m00 = vx.x;
//          rotation.m10 = vy.x;
//          rotation.m20 = viewDirRev.x;
//          rotation.m01 = vx.y;
//          rotation.m11 = vy.y;
//          rotation.m21 = viewDirRev.y;
//          rotation.m02 = vx.z;
//          rotation.m12 = vy.z;
//          rotation.m22 = viewDirRev.z;
//          rotation.toEuler(newRot);
//          System.out.println(newRot.y);
//          System.out.println(rot);
//          Vector3f diffRot = Vector3f.pool(lastRot);
//          diffRot.subtract(newRot);
//          System.out.println(diffRot);
            
            modelMatrix.load(rotation);
            modelMatrix.m30 = location.x;
            modelMatrix.m31 = location.y;
            modelMatrix.m32 = location.z;
            modelMatrix.scale(GUI_3D_SCALE);

            this.planeNormalWS.set(0, 0, 1);
            Matrix4f.transform(rotation, this.planeNormalWS, this.planeNormalWS);
            planeNormalWS.normaliseZero();
            hasHit = false;
            if (!fadeOut)
            this.fadeInF = Math.min(1.0f, (GameBase.absTime-this.timeAdded)/TIME_REMOVAL);
            if (fadeOut)
            this.fadeOutF = 1.0f-(this.fadeOut?((GameBase.absTime-this.timeRemoved)/TIME_REMOVAL):0);
        }

        public void checkIntersection(Vector3f pointerOrigin, Vector3f pointerDir) {
            float nd = Vector3f.dot(planeNormalWS, pointerDir);
            hasHit = false;
            if (nd < -0.0001f) {
                Vector3f vRayToPlane = Vector3f.pool();
                Vector3f.sub(location, pointerOrigin, vRayToPlane);
                float t = Vector3f.dot(vRayToPlane, planeNormalWS) / nd;
                if (t >= 0) {
                    vIntersection.set(pointerDir);
                    vIntersection.scale(t * 1.00f);
                    vIntersection.addVec(pointerOrigin);
                    vIntersectionWS.set(vIntersection);
                    vIntersection.subtract(location);
                    this.vIntersectionGuiSpace.set(vIntersection);
                    Matrix4f rotationInv = Matrix4f.pool(rotation);
                    rotationInv.scale(GUI_3D_SCALE, -GUI_3D_SCALE, GUI_3D_SCALE);
                    rotationInv.invert();
                    Matrix4f.transform(rotationInv, vIntersectionGuiSpace, vIntersectionGuiSpace);
                    vIntersectionGuiSpace.x += rW / 2.0f;
                    vIntersectionGuiSpace.y += rH / 2.0f;
                    this.mouseX = this.vIntersectionGuiSpace.x;
                    this.mouseY = this.vIntersectionGuiSpace.y;
                    AbstractUI n = Gui.selectedButton; //HACK
                    Gui.selectedButton = null;
                    if (this.gui.mouseOver(this.mouseX, this.mouseY)) {
                        hasHit = true;
                    }
                    Gui.selectedButton = n;
                }
            }
        }

        public void reset() {
            hasHit = false;
            mouseX = -100000;
            mouseY = -100000;
        }

        public void tickUpdate() {
            if (canRemove)
                return;
            if (this.fadeOutF <= 0f) {
                this.canRemove = true;
            }
            if (!this.fadeOut) {
                Vector3f tmp = Vector3f.pool(Engine.camera.getPosition());
                tmp.subtract(this.location);
                if (tmp.lengthSquared() > 10 * 10) {
                    if (this.gui instanceof GuiWindow) {
                        ((GuiWindow) this.gui).close();
                    } else {
                        if (Game.instance.getGui() == this.gui) {
                            Game.instance.showGUI(null);
                        } else {
                            this.onClose();
                        }

                    }
                }
            }
        }

        void onClose() {
            this.fadeOut = true;
            this.timeRemoved = GameBase.absTime;
        }
    }

    public void addGui(Gui gui, Gui prevGui, Vector3f camPos, Vector3f viewDir) {

        Vector3f guiPos = Vector3f.pool(camPos);
        Vector3f viewDirT = Vector3f.pool(viewDir);
        viewDir.scale(2);
        guiPos.add(viewDir);
        Gui3DPosition same = map.get(gui);
        if (same != null) {
            System.out.println("twice?!?!?!?");
        }
        if (prevGui != null) {
            Gui3DPosition prev = map.get(prevGui);
            if (prev != null) {
                guiPos.set(prev.location);
                prev.location.add(Vector3f.pool(prev.planeNormalWS).scale(-0.2f));
            }
        }
//        for (int i = 0; i < this.list.size(); i++) {
//            float distTo = list.get(i).location.distance(guiPos);
//            if (distTo < 1.1f) {
//                Vector3f v = Vector3f.pool(list.get(i).planeNormalWS);
//                v.scale(0.14f);
//                guiPos.addVec(v);
//                float distTo1 = list.get(i).location.distance(guiPos);
//                System.out.println(GameMath.sqrtf(distTo1));
//            }
//        }
        Gui3DPosition pos = new Gui3DPosition(gui, guiPos, GameBase.absTime);
        map.put(gui, pos);
        list.add(pos);
    }

    public void removeGui(Gui gui) {
        Gui3DPosition pos = map.get(gui);
        if (pos == null) {
            System.err.println("gui is not in 3d map");
            return;
        }
        pos.onClose();
    }
    public void tickUpdate() {
        for (int i = 0; i < this.list.size(); i++) {
            Gui3DPosition pos = this.list.get(i);
            pos.tickUpdate();
        }
        for (int i = 0; i < this.list.size(); i++) {
            Gui3DPosition pos = this.list.get(i);
            if (pos.canRemove) {
                this.map.remove(pos.gui);
                this.list.remove(i--);
                releaseFB(pos);
            }
        }
    }

    private void releaseFB(Gui3DPosition pos) {
        if (pos.fbGUIFixed != null) {
            this.inUseBuffers.remove(pos.fbGUIFixed);
            this.freeBuffers.add(pos.fbGUIFixed);
        }
    }

    public void update(Matrix4f pose, Vector3f camPos, Vector3f viewDir, PositionMouseOver ctrlPos) {

        Gui3DPosition minHit = null;
        for (int i = 0; i < this.list.size(); i++) {
            Gui3DPosition pos = this.list.get(i);
            pos.updatePosition(pose, camPos, viewDir);
        }
        if (ctrlPos.vDir != null) {
            float minDist = Float.MAX_VALUE;
            Vector3f tmp = Vector3f.pool();
            for (int i = 0; i < this.list.size(); i++) {
                Gui3DPosition pos = this.list.get(i);
                pos.checkIntersection(ctrlPos.vOrigin, ctrlPos.vDir);
                if (pos.hasHit) {
                    tmp.set(camPos);
                    tmp.subtract(pos.vIntersectionWS);
                    float len = tmp.length();
                    if (minHit == null || len < minDist) {
                        minDist = len;
                        minHit = pos;
                        continue;
                    }
                }
            }
        }
        this.hit = minHit;
//        GuiContext.hasOverride = false;
        GuiContext.hasOverride = true;
        GuiWindow newWindowFocus = null;
        if (minHit != null) {
            GuiContext.mouseX = minHit.mouseX;
            GuiContext.mouseY = minHit.mouseY;
            if (minHit.gui instanceof GuiWindow) {
                newWindowFocus = (GuiWindow) minHit.gui;
            }
        } else {
            GuiContext.mouseX = -100000;
            GuiContext.mouseY = -100000;
        }
        GuiContext.mouseOverOverride = newWindowFocus;
        GuiWindowManager.do_setWindowFocus(newWindowFocus);
    }

    public void render(float fTime) {
        if (this.list.isEmpty()) {
            return;
        }
//        GL11.glDisable(GL_DEPTH_TEST);
        Matrix4f mSt = Matrix4f.pool(Engine.getMatSceneM());

        Shaders.textured3DAlphaTest.enable();
        Shaders.textured3DAlphaTest.setProgramUniform1f("color_brightness", 1f);
//        GL11.glDisable(GL11.GL_CULL_FACE);
        for (int i = 0; i < this.list.size(); i++) {

            Gui3DPosition pos = list.get(i);
            if (pos.fbGUIFixed != null) {


                Tess tess = Tess.instance;
//                System.out.println("one");

                Matrix4f m = Matrix4f.pool(mSt);
                Matrix4f.mul(m, pos.modelMatrix, m);
                Engine.setModelMatrix(m);

                GL.bindTexture(GL13.GL_TEXTURE0, GL_TEXTURE_2D, pos.fbGUIFixed.getTexture(0));
                float t = Math.max(0, Math.min(pos.fadeOutF, pos.fadeInF));
                float posF = Ease.cubicOut(t);
                float alpha = 1.0f-Ease.cubicIn(1.0f-t);
                float tMax = 1.2f-1*0.2f;
                float tMin = -0.2f+1*0.2f;
                Shaders.textured3DAlphaTest.enable();
                Shaders.textured3DAlphaTest.setProgramUniform1f("color_brightness", 1f);
                tess.setColorF(-1, alpha);
                tess.setOffset(-rW / 2.0f, -rH / 2.0f-(1-posF)*rH*0.3f, 0);
                tess.add(rW, rH, 0, tMax, tMax);
                tess.add(0, rH, 0, tMin, tMax);
                tess.add(0, 0, 0, tMin, tMin);
                tess.add(rW, 0, 0, tMax, tMin);
                tess.draw(GL_QUADS);
                Shaders.textured3DAlphaTest.enable();
                Shaders.textured3DAlphaTest.setProgramUniform1f("color_brightness", 1f);
                //disable depth test + draw cursor
                if (pos == this.hit && pos.hasHit&&!pos.fadeOut) {
                    GL.bindTexture(GL13.GL_TEXTURE0, GL_TEXTURE_2D, this.texCursor);
                    GL11.glDepthFunc(GL11.GL_GREATER);
                    float fCursorSize = 32.0f;
                    float fMx = (float) pos.mouseX;
                    float fMy = (float) (rH - 1 - pos.mouseY) - fCursorSize;
                    tess.setColorF(-1, 1.0f);
                    tess.setOffset(-rW / 2.0f, -rH / 2.0f, 0);
                    float zMouse = -0.05f;
                    tess.add(fMx + fCursorSize, fMy + fCursorSize, zMouse, 1, 1);
                    tess.add(fMx, fMy + fCursorSize, zMouse, 0, 1);
                    tess.add(fMx, fMy, zMouse, 0, 0);
                    tess.add(fMx + fCursorSize, fMy, zMouse, 1, 0);
                    tess.draw(GL_QUADS);
                    GL11.glDepthFunc(GL11.GL_LEQUAL);
                }
                tess.setOffset(0, 0, 0);
                //        float mX = this.vIntersectionGuiSpace.x;
                //        float mY = this.vIntersectionGuiSpace.z;
                //        System.out.println(mX+"/"+mY);

//                Shaders.colored3D.enable();
//                Shaders.colored3D.setProgramUniform1f("color_brightness", 1f);
//                if (pos.hasHit && false) {
//                    pos.modelMatrix.setIdentity();
//                    pos.modelMatrix.translate(pos.location);
//                    pos.modelMatrix.translate(pos.vIntersection);
//                    Matrix4f.mul(pos.modelMatrix, pos.rotation, m);
//                    Matrix4f.mul(mSt, m, m);
//                    m.scale(GUI_3D_SCALE);
//                    //            Matrix4f.mul(mSt, modelMatrix, m);
//                    //            Matrix4f.mul(m, rotation, m);
//
//                    //            Matrix4f.mul(m, rotation, m);
//                    //            m.m30 += vIntersection.x;
//                    //            m.m31 += vIntersection.y;
//                    //            m.m32 += vIntersection.z;
//                    Engine.setModelMatrix(m);
//
//                    float w = 6f;
//                    tess.setColorF(-1, 1.0f);
//                    tess.setOffset(0, 0, 0.01f);
//                    tess.add(-w, w, 0, 0, 1);
//                    tess.add(w, w, 0, 1, 1);
//                    tess.add(w, -w, 0, 1, 0);
//                    tess.add(-w, -w, 0, 0, 0);
//                    tess.setOffset(0, 0, 0);
//                    tess.draw(GL_QUADS);
//                    //            Vector3f tmp = Vector3f.pool();
//                    //            Vector3f tmp2 = Vector3f.pool();
//                    //            for (int i = 0; i < 3; i++) {
//                    //                tmp.set(0, 0, 0);
//                    //                tmp2.set(0, 0, 0);
//                    //                tmp2.setElement(i, 1.0f);
//                    //                tess.setColorRGBAF(tmp2.x, tmp2.y, tmp2.z, 1.0f);
//                    //                tmp.setElement(i, -w);
//                    //                tess.add(tmp);
//                    //                tmp.setElement(i, w);
//                    //                tess.add(tmp);
//                    //            }
//                    //            GL11.glLineWidth(3);
//                    //            tess.draw(GL_LINES);
//                }
//                Shaders.colored3D.setProgramUniform1f("color_brightness", 0.1f);

                //        modelMatrix.setIdentity();
                //        modelMatrix.m30 = location.x;
                //        modelMatrix.m31 = location.y;
                //        modelMatrix.m32 = location.z;
                //        Matrix4f.mul(mSt, modelMatrix, m);
                //        Engine.setModelMatrix(m);
                //        GL11.glLineWidth(3);
                //        tess.setColorF(-1, 1.0f);
                //        tess.setColorF(0xff00ff, 1.0f);
                //        tess.add(0, 0, 0);
                //        tess.add(planeNormalWS);
                //        tess.draw(GL_LINES);
            }
        }
        Shaders.textured3DAlphaTest.setProgramUniform1f("color_brightness", 0.1f);
        Engine.setModelMatrix(mSt);
    }

    private FrameBuffer getFrameBuffer() {
        FrameBuffer fb;
        if (this.freeBuffers.isEmpty()) {
            fb = new FrameBuffer(rW, rH);
            fb.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA8);
            fb.setFilter(GL_COLOR_ATTACHMENT0, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR);
            fb.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
            fb.setDepthFmt(GL14.GL_DEPTH_COMPONENT16);
            fb.setAnisotropicFilterLevel(16);
            fb.setMipmapLevels(3);
            fb.setHasDepthAttachment();
            fb.setup(null);
            fb.bind();
            fb.clearFrameBuffer();
//            fb = FrameBuffer.make(null, 1920, 1080, GL_RGBA8, false, true);
        } else {
            fb = this.freeBuffers.remove(this.freeBuffers.size() - 1);
        }
        this.inUseBuffers.add(fb);
        return fb;
    }

    public boolean hasAny() {
        return !this.list.isEmpty();
    }
    public void reset() {
        this.hit = null;
        while (!this.list.isEmpty()) {
            Gui3DPosition n = this.list.remove(0);
            this.map.remove(n.gui);
            this.releaseFB(n);
        }
    }

    public boolean isMouseOverGui() {
        return this.hit != null;
    }
    
    public void init() {
        AssetTexture tex = AssetManager.getInstance().loadPNGAsset("textures/gui/cursor.png");
        texCursor = TextureManager.getInstance().makeNewTexture(tex, false, true, -1);
    }

    public void renderGUIs(float fTime) {
        //      Gui.RENDER_BACKGROUNDS = false;
        GL40.glBlendFuncSeparatei(0, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE);
        int nDrawn = 0;
        for (int i = 0; i < this.list.size(); i++) {

            Gui3DPosition pos = list.get(i);
            if (pos.fbGUIFixed == null) {
                pos.fbGUIFixed = getFrameBuffer();
            }
            pos.fbGUIFixed.bind();
            pos.fbGUIFixed.setClearColor(GL_COLOR_ATTACHMENT0, 0.71F, 0.82F, 1.00F, 0F);
            pos.fbGUIFixed.clearFrameBuffer();
            Engine.checkGLError("fbGUIFixed bind and clear");
            double mouseX = -100000;
            double mouseY = -100000;
            if (pos == this.hit && pos.hasHit) {
                Gui btnGui = (Gui) (Gui.selectedButton!=null&&(Gui.selectedButton.parent instanceof Gui)?Gui.selectedButton.parent:null);
                if ((btnGui == null || btnGui == pos.gui)) {
                    mouseX = pos.mouseX;
                    mouseY = pos.mouseY;
                }
            }
            GuiWindowManager.setTooltip(null);
            pos.gui.render3D(fTime, mouseX, mouseY);
            if (pos.hasHit) {
                Tooltip tip = GuiWindowManager.getTooltip();
                Player player = Game.instance != null ? Game.instance.getPlayer() : null;
                BaseStack stack = player == null ? null : player.getInventory().getCarried();
                if (stack != null) {
                    int slotW = 48;
                    float inset = 4;
                    float inset2 = 2;
                    Engine.itemRender.drawItem(stack, (float) mouseX + inset - slotW / 2, (float) mouseY + inset - slotW / 2, slotW - inset * 2, slotW - inset * 2);
                    Shaders.textured.enable();
                    Engine.itemRender.drawItemOverlay(stack, (float) mouseX + inset - slotW / 2, (float) mouseY + inset - slotW / 2, slotW - inset * 2, slotW - inset * 2);
                } else {
                    if (tip != null && tip.getTooltipOwner() == pos.gui) {
                        tip.render(fTime, pos.mouseX, pos.mouseY);
                    }
                }
                
            }
            if (pos.gui instanceof GuiWindow) {
                nDrawn++;
            }
            pos.fbGUIFixed.generateMipMaps(0);
            if (GLDebugTextures.isShow()) {
                GLDebugTextures.readTexture("VR_GUI", "texColor" + i, pos.fbGUIFixed.getTexture(0), 0x8);
            }
//            Game.instance.renderGui(fTime, mouseX, mouseY);
//            if (this.gui == null && this.world != null) {
//                glEnable(GL_DEPTH_TEST);
//                GuiWindowManager.getInstance().render(fTime, 0, 0);
//                glDisable(GL_DEPTH_TEST);
//            }
        }
//
        if (nDrawn > 0) {
        }

        //
        Gui.RENDER_BACKGROUNDS = true;
        GL40.glBlendFuncSeparatei(0, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        FrameBuffer.unbindFramebuffer();
    }

}
