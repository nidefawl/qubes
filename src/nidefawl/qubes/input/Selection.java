package nidefawl.qubes.input;

import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.RayTrace;
import nidefawl.qubes.util.RayTrace.RayTraceIntersection;
import nidefawl.qubes.vec.AABBFloat;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.World;

public class Selection {

    public Selection() {
    }

    public GameMode getMode() {
        return Game.instance.getMode();
    }

    private TesselatorState highlightSelection;
    private TesselatorState fullBlock;
    private TesselatorState customBB;
    AABBFloat               lastCustomBB;
    public boolean          quarterMode       = false;
    boolean                 mouseDown         = false;
    int                     buttonDown        = 0;
    boolean                 mouseStateChanged = false;
    private TesselatorState renderBB;
    public RayTrace         rayTrace;

    public void init() {

        highlightSelection = new TesselatorState(GL15.GL_DYNAMIC_DRAW);
        fullBlock = new TesselatorState(GL15.GL_STATIC_DRAW);
        customBB = new TesselatorState(GL15.GL_DYNAMIC_DRAW);
        this.rayTrace = new RayTrace();
        renderBlockOver(this.fullBlock, new AABBFloat(0, 0, 0, 1, 1, 1));
    }

    public BlockPos[] pos = new BlockPos[] { new BlockPos(), new BlockPos() };

    public void resetSelection() {
        this.mouseDown = false;
    }

    public boolean hasSelection() {
        if (getMode() == GameMode.PLAY || getMode() == GameMode.BUILD) {
            return this.rayTrace.hasHit();
        }
        return pos[0] != null && pos[1] != null;
    }

    boolean           updateBB = false;
    public BlockPos   mouseOver;
    private AABBFloat selBB    = new AABBFloat();
    private int timer;

    public void renderBlockHighlight(World world, float fTime) {
        if (mouseOver != null) {

            if (!(getMode() == GameMode.SELECT && hasSelection())) {
                renderMouseOver();
            }
        }
        if (getMode() == GameMode.PLAY) {
            return;
        }
        if (getMode() == GameMode.BUILD) {
            return;
        }
        if (hasSelection()) {
            int blocks = getNumBlocks();
            if (blocks > 1) {
                if (updateBB) {
                    renderBB();
                    updateBB = false;
                }
                Shaders.colored3D.enable();
                highlightSelection.drawQuads();
                Shaders.wireframe.enable();
                Shaders.wireframe.setProgramUniform1i("num_vertex", 4);
                Shaders.wireframe.setProgramUniform1f("thickness", 0.7f);
                Shaders.wireframe.setProgramUniform4f("linecolor", 1, 0f, 1f, 1);
                Shaders.wireframe.setProgramUniform1f("maxDistance", 220);
                glDisable(GL11.GL_DEPTH_TEST);
                highlightSelection.drawQuads();
                glEnable(GL11.GL_DEPTH_TEST);
                Shader.disable();
            }
        }
    }

    /**
     * 
     */
    private void renderMouseOver() {
        if (this.renderBB != null) {
            Shaders.colored3D.enable();
            glEnable(GL_POLYGON_OFFSET_FILL);
            glPolygonOffset(-1.375f*(Engine.isInverseZ?-1:1), 1);
            Engine.setDepthFunc(GL_LESS);
            Shaders.colored3D.setProgramUniform4f("color_uniform", 1,1,1,0.3f);
            Engine.pxStack.push(this.mouseOver.x, this.mouseOver.y, this.mouseOver.z);
//            glDisable(GL_CULL_FACE);
            this.renderBB.drawQuads();
//            glEnable(GL_CULL_FACE);
            Engine.pxStack.pop();
            Shaders.colored3D.setProgramUniform4f("color_uniform", 1,1,1, 1);
            Engine.setDepthFunc(GL_LEQUAL);
            glDisable(GL_POLYGON_OFFSET_FILL);
        }

    }

    public void renderBlockOver(TesselatorState out, AABBFloat box) {

        if (this.lastCustomBB != null && this.lastCustomBB.isEqual(box)) {
            return;
        }
        if (this.lastCustomBB == null) {
            this.lastCustomBB = new AABBFloat();
        }
        this.lastCustomBB.set(box);
        float ext = 1 / 256f;
        float w = 1 / 32f;
        Tess tesselator = Tess.instance;
        float br = 1f;
        float alpha = 1f;
        tesselator.setColorRGBAF(br, br, br, alpha);
        float minX = box.minX - ext;
        float minY = box.minY - ext;
        float minZ = box.minZ - ext;
        float maxX = box.maxX + ext;
        float maxY = box.maxY + ext;
        float maxZ = box.maxZ + ext;
        tesselator.setNormals(0, 0, -1);
        tesselator.add(minX, maxY, minZ);
        tesselator.add(minX + w, maxY, minZ);
        tesselator.add(minX + w, minY, minZ);
        tesselator.add(minX, minY, minZ);
        tesselator.add(maxX - w, maxY, minZ);
        tesselator.add(maxX, maxY, minZ);
        tesselator.add(maxX, minY, minZ);
        tesselator.add(maxX - w, minY, minZ);
        tesselator.add(maxX - w, maxY, minZ);
        tesselator.add(minX + w, maxY, minZ);
        tesselator.add(minX + w, maxY - w, minZ);
        tesselator.add(maxX - w, maxY - w, minZ);
        tesselator.add(maxX - w, minY + w, minZ);
        tesselator.add(minX + w, minY + w, minZ);
        tesselator.add(minX + w, minY, minZ);
        tesselator.add(maxX - w, minY, minZ);

        tesselator.setNormals(0, 0, 1);
        tesselator.add(minX, maxY, maxZ);
        tesselator.add(minX + w, maxY, maxZ);
        tesselator.add(minX + w, minY, maxZ);
        tesselator.add(minX, minY, maxZ);
        tesselator.add(maxX - w, maxY, maxZ);
        tesselator.add(maxX, maxY, maxZ);
        tesselator.add(maxX, minY, maxZ);
        tesselator.add(maxX - w, minY, maxZ);
        tesselator.add(maxX - w, maxY, maxZ);
        tesselator.add(minX + w, maxY, maxZ);
        tesselator.add(minX + w, maxY - w, maxZ);
        tesselator.add(maxX - w, maxY - w, maxZ);
        tesselator.add(maxX - w, minY + w, maxZ);
        tesselator.add(minX + w, minY + w, maxZ);
        tesselator.add(minX + w, minY, maxZ);
        tesselator.add(maxX - w, minY, maxZ);

        tesselator.setNormals(0, 1, 0);
        tesselator.add(minX, maxY, maxZ);
        tesselator.add(minX + w, maxY, maxZ);
        tesselator.add(minX + w, maxY, minZ);
        tesselator.add(minX, maxY, minZ);
        tesselator.add(maxX - w, maxY, maxZ);
        tesselator.add(maxX, maxY, maxZ);
        tesselator.add(maxX, maxY, minZ);
        tesselator.add(maxX - w, maxY, minZ);
        tesselator.add(minX + w, maxY, maxZ);
        tesselator.add(maxX - w, maxY, maxZ);
        tesselator.add(maxX - w, maxY, maxZ - w);
        tesselator.add(minX + w, maxY, maxZ - w);
        tesselator.add(minX + w, maxY, minZ + w);
        tesselator.add(maxX - w, maxY, minZ + w);
        tesselator.add(maxX - w, maxY, minZ);
        tesselator.add(minX + w, maxY, minZ);

        tesselator.setNormals(0, -1, 0);
        tesselator.add(minX, minY, maxZ);
        tesselator.add(minX + w, minY, maxZ);
        tesselator.add(minX + w, minY, minZ);
        tesselator.add(minX, minY, minZ);
        tesselator.add(maxX - w, minY, maxZ);
        tesselator.add(maxX, minY, maxZ);
        tesselator.add(maxX, minY, minZ);
        tesselator.add(maxX - w, minY, minZ);
        tesselator.add(minX + w, minY, maxZ);
        tesselator.add(maxX - w, minY, maxZ);
        tesselator.add(maxX - w, minY, maxZ - w);
        tesselator.add(minX + w, minY, maxZ - w);
        tesselator.add(minX + w, minY, minZ + w);
        tesselator.add(maxX - w, minY, minZ + w);
        tesselator.add(maxX - w, minY, minZ);
        tesselator.add(minX + w, minY, minZ);

        tesselator.setNormals(-1, 0, 0);
        tesselator.add(minX, minY, minZ);
        tesselator.add(minX, minY, minZ + w);
        tesselator.add(minX, maxY, minZ + w);
        tesselator.add(minX, maxY, minZ);
        tesselator.add(minX, minY, maxZ - w);
        tesselator.add(minX, minY, maxZ);
        tesselator.add(minX, maxY, maxZ);
        tesselator.add(minX, maxY, maxZ - w);
        tesselator.add(minX, minY, minZ + w);
        tesselator.add(minX, minY, maxZ - w);
        tesselator.add(minX, minY + w, maxZ - w);
        tesselator.add(minX, minY + w, minZ + w);
        tesselator.add(minX, maxY - w, minZ + w);
        tesselator.add(minX, maxY - w, maxZ - w);
        tesselator.add(minX, maxY, maxZ - w);
        tesselator.add(minX, maxY, minZ + w);

        tesselator.setNormals(1, 0, 0);
        tesselator.add(maxX, minY, minZ);
        tesselator.add(maxX, minY, minZ + w);
        tesselator.add(maxX, maxY, minZ + w);
        tesselator.add(maxX, maxY, minZ);
        tesselator.add(maxX, minY, maxZ - w);
        tesselator.add(maxX, minY, maxZ);
        tesselator.add(maxX, maxY, maxZ);
        tesselator.add(maxX, maxY, maxZ - w);
        tesselator.add(maxX, minY, minZ + w);
        tesselator.add(maxX, minY, maxZ - w);
        tesselator.add(maxX, minY + w, maxZ - w);
        tesselator.add(maxX, minY + w, minZ + w);
        tesselator.add(maxX, maxY - w, minZ + w);
        tesselator.add(maxX, maxY - w, maxZ - w);
        tesselator.add(maxX, maxY, maxZ - w);
        tesselator.add(maxX, maxY, minZ + w);

        tesselator.draw(GL_QUADS, out);
        tesselator.resetState();
    }

    public void renderBB() {

        float ext = 1 / 32.0f;
        Tess tesselator = Tess.instance;
        tesselator.setColorRGBAF(0.4f, 0.4f, 0.4f, 1F);
        BlockPos sel1 = pos[0];
        BlockPos sel2 = pos[1];
        float minX = Math.min(sel1.x, sel2.x) - ext;
        float minY = Math.min(sel1.y, sel2.y) - ext;
        float minZ = Math.min(sel1.z, sel2.z) - ext;
        float maxX = Math.max(sel1.x, sel2.x) + ext + 1;
        float maxY = Math.max(sel1.y, sel2.y) + ext + 1;
        float maxZ = Math.max(sel1.z, sel2.z) + ext + 1;
        tesselator.setNormals(0, 0, -1);
        tesselator.add(minX, minY, minZ);
        tesselator.add(maxX, minY, minZ);
        tesselator.add(maxX, maxY, minZ);
        tesselator.add(minX, maxY, minZ);

        tesselator.setNormals(0, 0, 1);
        tesselator.add(minX, maxY, maxZ);
        tesselator.add(maxX, maxY, maxZ);
        tesselator.add(maxX, minY, maxZ);
        tesselator.add(minX, minY, maxZ);

        tesselator.setNormals(0, 1, 0);
        tesselator.add(minX, maxY, minZ);
        tesselator.add(maxX, maxY, minZ);
        tesselator.add(maxX, maxY, maxZ);
        tesselator.add(minX, maxY, maxZ);

        tesselator.setNormals(0, -1, 0);
        tesselator.add(minX, minY, minZ);
        tesselator.add(maxX, minY, minZ);
        tesselator.add(maxX, minY, maxZ);
        tesselator.add(minX, minY, maxZ);

        tesselator.setNormals(-1, 0, 0);
        tesselator.add(minX, minY, minZ);
        tesselator.add(minX, minY, maxZ);
        tesselator.add(minX, maxY, maxZ);
        tesselator.add(minX, maxY, minZ);

        tesselator.setNormals(1, 0, 0);
        tesselator.add(maxX, minY, minZ);
        tesselator.add(maxX, minY, maxZ);
        tesselator.add(maxX, maxY, maxZ);
        tesselator.add(maxX, maxY, minZ);

        tesselator.draw(GL_QUADS, highlightSelection);
        tesselator.resetState();
    }
    public void update(World world) {
        if (timer>0)
            timer--;
        if (world == null) {
            return;
        }
        if (getMode() == GameMode.BUILD) {
            
            if (timer<=0&&this.mouseDown) {
                if (rayTrace.hasHit()) {
                    timer += 5;
                    RayTraceIntersection intersect = rayTrace.getHit();
                    Game.instance.getWPCtrl().blockClicked(world, intersect, this.buttonDown, this.quarterMode);
                }
                Game.instance.getPlayer().swing();
            }
        }
    }

    public void update(World world, PositionMouseOver mouseOver, Vector3f cam) {
        this.rayTrace.reset();
        this.mouseOver = null;
        if (world == null) {
            this.mouseDown = false;
            this.mouseStateChanged = false;
            return;
        }
        if (mouseOver.vDir != null) {
            this.rayTrace.quarterMode = this.quarterMode;
            this.rayTrace.doRaytrace(world, mouseOver.vOrigin, mouseOver.vDir, extendReach() ? 200 : 55);
            if (this.rayTrace.hasHit()) {
                RayTraceIntersection hit = this.rayTrace.getHit();
                BlockPos p = hit.blockPos.copy();
                setMouseOver(hit);
                if (getMode() == GameMode.SELECT) {
                    if (mouseStateChanged && this.mouseDown) { // first call after mousedown
                        set(0, p);
                        set(1, p);
                    } else if (this.mouseDown) {
                        set(1, p);
                    }
                    this.mouseStateChanged = false;
                    return;
                }

                if (getMode() == GameMode.EDIT) {
                    //EDIT MODE
                    //TODO: add some better logic for highlighting, don't render "into" camera
                    if (p != null && !(p.x == GameMath.floor(cam.x) && p.y == GameMath.floor(cam.y) && p.z == GameMath.floor(cam.z))) {
                        if (this.buttonDown == 1) {
                            p = p.copy();
                            p.offset(hit.face);
                        }
                        if (mouseStateChanged || !mouseDown || (Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL))) { // reset, set initial
                            set(0, p);
                            set(1, p);
                        } else {
                            set(1, p);
                        }
                    } else {
                        if (!mouseDown) {
                            set(1, pos[0]);
                        }
                        //          System.err.println("fail "+p);
                    }
                }
            }
        }
        this.mouseStateChanged = false;
        if (this.mouseDown && Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL)) {
            onRelease(0);
        }
    }

    /**
     * @param hit
     */
    private void setMouseOver(RayTraceIntersection hit) {
        BlockPos newMouseOver = hit.blockPos.copy();
        World world = Game.instance.getWorld();
        if (newMouseOver != null && world != null) {
            Game.instance.dig.setBlock(hit, newMouseOver);
            this.mouseOver = newMouseOver;
            int type = world.getType(this.mouseOver.x, this.mouseOver.y, this.mouseOver.z);
            int bbType = Block.get(type).setSelectionBB(world, hit, this.mouseOver, this.selBB);
            if (bbType != 2 && this.quarterMode) {

                int x = hit.q.x;
                int y = hit.q.y;
                int z = hit.q.z;
                selBB.set(x * 0.5f, y * 0.5f, z * 0.5f, x * 0.5f + 0.5f, y * 0.5f + 0.5f, z * 0.5f + 0.5f);
                //              System.out.println(hit.pos.x);
                //              this.selBB.set(0, 0, 0, 0.5f, 0.5f, 0.5f);
                //              if (hit.face == Dir.DIR_POS_X || hit.pos.x-this.mouseOver.x >= 0.5f) {
                //                  this.selBB.offset(0.5f, 0, 0);
                //              }
                //              if (hit.face == Dir.DIR_POS_Y || hit.pos.y-this.mouseOver.y >= 0.5f) {
                //                  this.selBB.offset(0, 0.5f, 0);
                //              }
                //              if (hit.face == Dir.DIR_POS_Z || hit.pos.z-this.mouseOver.z >= 0.5f) {
                //                  this.selBB.offset(0, 0, 0.5f);
                //              }
                renderBlockOver(this.customBB, this.selBB);
                this.renderBB = customBB;
                return;
            }
            if (bbType > 0) {
                renderBlockOver(this.customBB, this.selBB);
                this.renderBB = customBB;
            } else {
                this.renderBB = fullBlock;
            }
        } else {
            this.mouseOver = null;
        }
    }

    private void set(int i, BlockPos p2) {
        BlockPos p = pos[i];
        if (p2!=null&&p != null)
        if (p.x != p2.x || p.z != p2.z || p.y != p2.y) {
            p.x = p2.x;
            p.y = p2.y;
            p.z = p2.z;
            updateBB = true;
        }
    }
    public void clicked(int button, boolean isDown) {
        World world = Game.instance.getWorld();
        if (world == null) {
            return;
        }
//        if (isDown)
//        Game.instance.getPlayer().swing();
        if (getMode() == GameMode.PLAY) {
            return;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            if (isDown && this.mouseOver != null) {
                Game.instance.getWPCtrl().pickBlock(world, this.mouseOver, getHit());
            }
            return;
        }
        if (button == 3) {
            if (isDown && this.mouseOver != null) {
                Game.instance.getWPCtrl().jumpTo(world, this.mouseOver, getHit());
            }
            return;
        }
        this.mouseStateChanged = this.mouseDown != isDown;
        this.mouseDown = isDown;
        if (this.mouseDown) {
            this.buttonDown = button;
            RayTraceIntersection hit = this.rayTrace.getHit();
            BlockPos p = this.rayTrace.hasHit()? hit.blockPos.copy() : null ;
            set(0, p);
            set(1, p);
        } 
        if (!this.mouseDown) {
            onRelease(button);
        }
    }

    private void onRelease(int button) {
        World world = Game.instance.getWorld();
        if (world == null) {
            return;
        }

        if (!quarterMode && getMode() == GameMode.EDIT) {
            if (!Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL)) {
                if (rayTrace.hasHit()) {
                    Game.instance.getWPCtrl().setMultiple(world, rayTrace.getHit(), button, this.getMin(), this.getMax());
                }
            }
        }
    }

    public boolean extendReach() {
        return this.mouseDown || GameBase.baseInstance.isGrabbed();
    }

    public BlockPos getMin() {
        return new BlockPos(Math.min(this.pos[0].x, this.pos[1].x), Math.min(this.pos[0].y, this.pos[1].y), Math.min(this.pos[0].z, this.pos[1].z));
    }

    public BlockPos getMax() {
        return new BlockPos(Math.max(this.pos[0].x, this.pos[1].x), Math.max(this.pos[0].y, this.pos[1].y), Math.max(this.pos[0].z, this.pos[1].z));
    }

    public int getNumBlocks() {
        BlockPos p1 = getMin();
        BlockPos p2 = getMax();
        return (p2.x - p1.x + 1) * (p2.y - p1.y + 1) * (p2.z - p1.z + 1);
    }

    public void reset() {
        this.rayTrace.reset();
        this.mouseOver = null;
        this.mouseDown = false;
        this.mouseStateChanged = false;
//        pos[0] = null;
//        pos[1] = null;
    }

    /**
     * @return
     */
    public RayTraceIntersection getHit() {
        if (this.rayTrace.hasHit()) {
            return this.rayTrace.getHit();
        }
        return null;
    }

    /**
     * @param ix
     * @param iy
     * @param iz
     * @return
     */
    public boolean contains(int ix, int iy, int iz) {
        if (this.pos[0] == null || this.pos[1] == null) {
            return false;
        }
        BlockPos sel1 = pos[0];
        BlockPos sel2 = pos[1];
        int minX = Math.min(sel1.x, sel2.x);
        int minY = Math.min(sel1.y, sel2.y);
        int minZ = Math.min(sel1.z, sel2.z);
        int maxX = Math.max(sel1.x, sel2.x);
        int maxY = Math.max(sel1.y, sel2.y);
        int maxZ = Math.max(sel1.z, sel2.z);
        return ix >= minX && ix <= maxX && iy >= minY && iy <= maxY && iz >= minZ && iz <= maxZ;
    }

    public void toggleQuarterMode() {
        this.quarterMode = !this.quarterMode;
    }

}
