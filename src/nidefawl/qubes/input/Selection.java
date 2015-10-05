package nidefawl.qubes.input;

import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.glfw.GLFW;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gl.TesselatorState;
import nidefawl.qubes.network.packet.PacketCSetBlock;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.Flags;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.RayTrace;
import nidefawl.qubes.util.RayTrace.RayTraceIntersection;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.vec.Dir;
import nidefawl.qubes.world.World;

public class Selection {
    
    public static enum SelectionMode {
        PLAY, EDIT, SELECT
    }
    private SelectionMode mode = SelectionMode.PLAY;

    /**
     * @return the mode
     */
    public SelectionMode getMode() {
        return this.mode;
    }
    /**
     * @param mode the mode to set
     */
    public void setMode(SelectionMode mode) {
        this.mode = mode;
    }
    private TesselatorState highlightSelection;
    private TesselatorState highlightBlockOver;
    boolean                 mouseDown         = false;
    boolean                 mouseStateChanged = false;
    private RayTrace rayTrace;

    public void init() {

        highlightSelection = new TesselatorState();
        highlightBlockOver = new TesselatorState();
        this.rayTrace = new RayTrace(); 
        renderBlockOver();
    }

    public BlockPos[] pos = new BlockPos[] {
            new BlockPos(), new BlockPos()
    };

    public void resetSelection() {
        this.mouseDown = false;
        //        selection[0] = null;
        //        selection[1] = null;
    }

    public boolean hasSelection() {
        return pos[0] != null && pos[1] != null;
    }

    boolean updateBB = false;
    public BlockPos mouseOver;
    private BlockPos lastMouseOver;

    public void renderBlockHighlight(World world, float fTime) {
        if (mouseOver != null) {

            if (!(this.mode == SelectionMode.SELECT && hasSelection())) {
                Shaders.colored3D.enable();
                renderMouseOver();
                Shader.disable();
            }
        }
        if (this.mode == SelectionMode.PLAY) {
            return;
        }
        if (hasSelection()) {
            int blocks = getNumBlocks();
            if (blocks > 1) {
                Shaders.colored3D.enable();
                if (updateBB) {
                    renderBB();
                    updateBB = false;
                }
                glDisable(GL_DEPTH_TEST);
                highlightSelection.drawQuads();
                Shaders.wireframe.enable();
                Shaders.wireframe.setProgramUniform4f("linecolor", 1, 0.2f, 0.2f, 1);
                Shaders.wireframe.setProgramUniform1f("maxDistance", 1000);
                highlightSelection.drawQuads();
                glEnable(GL_DEPTH_TEST);
                Shader.disable();
            }
        } 
    }
    /**
     * 
     */
    private void renderMouseOver() {
        glDepthFunc(GL_LESS);
        renderBlockOver();
        Shaders.colored3D.setProgramUniform3f("in_offset", this.mouseOver.x, this.mouseOver.y, this.mouseOver.z);
        highlightBlockOver.drawQuads();
        Shaders.colored3D.setProgramUniform3f("in_offset", 0, 0, 0);
        glDepthFunc(GL_LEQUAL);
        
    }
    public void renderBlockOver() {

        float ext = 1 / 96F;
        float w = 1/32f;
        Tess tesselator = Tess.instance;
        float br = 0.6f;
        tesselator.setColorRGBAF(br,br,br, 0.5f);
        BlockPos sel1 = new BlockPos();
        BlockPos sel2 = new BlockPos();
//        BlockPos sel1 = pos[0];
//        BlockPos sel2 = pos[1];
        float minX = Math.min(sel1.x, sel2.x) - ext;
        float minY = Math.min(sel1.y, sel2.y) - ext;
        float minZ = Math.min(sel1.z, sel2.z) - ext;
        float maxX = Math.max(sel1.x, sel2.x) + ext + 1;
        float maxY = Math.max(sel1.y, sel2.y) + ext + 1;
        float maxZ = Math.max(sel1.z, sel2.z) + ext + 1;
        tesselator.setNormals(0, 0, -1);
        tesselator.add(minX, maxY, minZ);
        tesselator.add(minX+w, maxY, minZ);
        tesselator.add(minX+w, minY, minZ);
        tesselator.add(minX, minY, minZ);
        tesselator.add(maxX-w, maxY, minZ);
        tesselator.add(maxX, maxY, minZ);
        tesselator.add(maxX, minY, minZ);
        tesselator.add(maxX-w, minY, minZ);
        tesselator.add(maxX-w, maxY, minZ);
        tesselator.add(minX+w, maxY, minZ);
        tesselator.add(minX+w, maxY-w, minZ);
        tesselator.add(maxX-w, maxY-w, minZ);
        tesselator.add(maxX-w, minY+w, minZ);
        tesselator.add(minX+w, minY+w, minZ);
        tesselator.add(minX+w, minY, minZ);
        tesselator.add(maxX-w, minY, minZ);

        tesselator.setNormals(0, 0, 1);
        tesselator.add(minX, maxY, maxZ);
        tesselator.add(minX+w, maxY, maxZ);
        tesselator.add(minX+w, minY, maxZ);
        tesselator.add(minX, minY, maxZ);
        tesselator.add(maxX-w, maxY, maxZ);
        tesselator.add(maxX, maxY, maxZ);
        tesselator.add(maxX, minY, maxZ);
        tesselator.add(maxX-w, minY, maxZ);
        tesselator.add(maxX-w, maxY, maxZ);
        tesselator.add(minX+w, maxY, maxZ);
        tesselator.add(minX+w, maxY-w, maxZ);
        tesselator.add(maxX-w, maxY-w, maxZ);
        tesselator.add(maxX-w, minY+w, maxZ);
        tesselator.add(minX+w, minY+w, maxZ);
        tesselator.add(minX+w, minY, maxZ);
        tesselator.add(maxX-w, minY, maxZ);

        tesselator.setNormals(0, 1, 0);
        tesselator.add(minX, maxY, maxZ);
        tesselator.add(minX+w, maxY, maxZ);
        tesselator.add(minX+w, maxY, minZ);
        tesselator.add(minX, maxY, minZ);
        tesselator.add(maxX-w, maxY, maxZ);
        tesselator.add(maxX, maxY, maxZ);
        tesselator.add(maxX, maxY, minZ);
        tesselator.add(maxX-w, maxY, minZ);
        tesselator.add(minX+w, maxY, maxZ);
        tesselator.add(maxX-w, maxY, maxZ);
        tesselator.add(maxX-w, maxY, maxZ-w);
        tesselator.add(minX+w, maxY, maxZ-w);
        tesselator.add(minX+w, maxY, minZ+w);
        tesselator.add(maxX-w, maxY, minZ+w);
        tesselator.add(maxX-w, maxY, minZ);
        tesselator.add(minX+w, maxY, minZ);

        tesselator.setNormals(0, -1, 0);
        tesselator.add(minX, minY, maxZ);
        tesselator.add(minX+w, minY, maxZ);
        tesselator.add(minX+w, minY, minZ);
        tesselator.add(minX, minY, minZ);
        tesselator.add(maxX-w, minY, maxZ);
        tesselator.add(maxX, minY, maxZ);
        tesselator.add(maxX, minY, minZ);
        tesselator.add(maxX-w, minY, minZ);
        tesselator.add(minX+w, minY, maxZ);
        tesselator.add(maxX-w, minY, maxZ);
        tesselator.add(maxX-w, minY, maxZ-w);
        tesselator.add(minX+w, minY, maxZ-w);
        tesselator.add(minX+w, minY, minZ+w);
        tesselator.add(maxX-w, minY, minZ+w);
        tesselator.add(maxX-w, minY, minZ);
        tesselator.add(minX+w, minY, minZ);

        tesselator.setNormals(-1, 0, 0);
        tesselator.add(minX, minY, minZ);
        tesselator.add(minX, minY, minZ+w);
        tesselator.add(minX, maxY, minZ+w);
        tesselator.add(minX, maxY, minZ);
        tesselator.add(minX, minY, maxZ-w);
        tesselator.add(minX, minY, maxZ);
        tesselator.add(minX, maxY, maxZ);
        tesselator.add(minX, maxY, maxZ-w);
        tesselator.add(minX, minY, minZ+w);
        tesselator.add(minX, minY, maxZ-w);
        tesselator.add(minX, minY+w, maxZ-w);
        tesselator.add(minX, minY+w, minZ+w);
        tesselator.add(minX, maxY-w, minZ+w);
        tesselator.add(minX, maxY-w, maxZ-w);
        tesselator.add(minX, maxY, maxZ-w);
        tesselator.add(minX, maxY, minZ+w);

        tesselator.setNormals(1, 0, 0);
        tesselator.add(maxX, minY, minZ);
        tesselator.add(maxX, minY, minZ+w);
        tesselator.add(maxX, maxY, minZ+w);
        tesselator.add(maxX, maxY, minZ);
        tesselator.add(maxX, minY, maxZ-w);
        tesselator.add(maxX, minY, maxZ);
        tesselator.add(maxX, maxY, maxZ);
        tesselator.add(maxX, maxY, maxZ-w);
        tesselator.add(maxX, minY, minZ+w);
        tesselator.add(maxX, minY, maxZ-w);
        tesselator.add(maxX, minY+w, maxZ-w);
        tesselator.add(maxX, minY+w, minZ+w);
        tesselator.add(maxX, maxY-w, minZ+w);
        tesselator.add(maxX, maxY-w, maxZ-w);
        tesselator.add(maxX, maxY, maxZ-w);
        tesselator.add(maxX, maxY, minZ+w);

        tesselator.draw(GL_QUADS, highlightBlockOver);
        tesselator.resetState();
    }

    public void renderBB() {

        float ext = 1 / 64.0f;
        Tess tesselator = Tess.instance;
        tesselator.setColorRGBAF(0.4f, 0.4f, 0.4f, 0.5F);
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

    public void update(World world, double px, double py, double pz) {
        this.rayTrace.reset();
        this.mouseOver = null;
        if (world == null) {
            this.mouseDown = false;
            this.mouseStateChanged = false;
            return;
        }
        if (Engine.vDir != null) {
            this.rayTrace.doRaytrace(world, Engine.vOrigin, Engine.vDir, extendReach() ? 200 : 55);
            if (this.rayTrace.hasHit()) {
                RayTraceIntersection hit = this.rayTrace.getHit();
                BlockPos p = hit.blockPos.copy();
                this.mouseOver = p;
                if (this.mode == SelectionMode.PLAY) {
                    return;
                }
                if (this.mode == SelectionMode.SELECT) {
                    if (mouseStateChanged && this.mouseDown) { // first call after mousedown
                        set(0, p);
                        set(1, p);
                    } else if (this.mouseDown) {
                        set(1, p);
                    }
                    this.mouseStateChanged = false;
                    return;
                }
                
                //EDIT MODE
                //TODO: add some better logic for highlighting, don't render "into" camera
                if (p != null && !(p.x == GameMath.floor(px) && p.y == GameMath.floor(py) && p.z == GameMath.floor(pz))) {
                    if (Game.instance.selBlock != 0) {
                        p = p.copy();
                        p.offset(hit.face);
                    }
                    if (!mouseDown ||(Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL))) {
                        set(0, p);
                        set(1, p);
                    } else if (mouseStateChanged) {
                        set(0, p);
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
        this.mouseStateChanged = false;
        if (this.mouseDown && Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL)) {
            onRelease();
        }
    }

    private void set(int i, BlockPos p2) {
        BlockPos p = pos[i];
        if (p.x != p2.x || p.z != p2.z || p.y != p2.y) {
            p.x = p2.x;
            p.y = p2.y;
            p.z = p2.z;
            updateBB = true;
        }
    }

    public void clicked(int button, boolean isDown) {
        if (button == 2) {
            if (!isDown)
                return;
            BlockPos p = this.mouseOver;
            World world = Game.instance.getWorld();
            if (p != null && world != null) {
                int type = world.getType(p.x, p.y, p.z);
                Game.instance.selBlock = type;
            }
            return;
        }
        this.mouseStateChanged = this.mouseDown != isDown;
        this.mouseDown = isDown;
        if (this.mode == SelectionMode.PLAY) {
            if (isDown && this.mouseOver != null) {
                onRelease();
            }
            return;
        }
        if (!this.mouseDown && !Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL)) {
            onRelease();
        }
    }

    public boolean extendReach() {
        return this.mouseDown || Game.instance.movement.grabbed();
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
        return (p2.x-p1.x+1)*(p2.y-p1.y+1)*(p2.z-p1.z+1);
    }
    
    

    private void onRelease() {
        if (this.mode == SelectionMode.PLAY) {
            if (rayTrace.hasHit()) {
                RayTraceIntersection intersect = rayTrace.getHit();
                Game.instance.blockClicked(intersect);
                
            }
            return;
        }
        if (this.mode == SelectionMode.SELECT) {
            return;
        }
        World world = Game.instance.getWorld();
        if (world != null) {
            if (rayTrace.hasHit()) {

                int blocks = getNumBlocks();
                if (blocks == 1) {
                    RayTraceIntersection intersect = rayTrace.getHit();
                    int offset = intersect.face;
                    int block = Game.instance.selBlock;
                    BlockPos p = intersect.blockPos.copy();
                    if (block > 0) {
                        if (mode == SelectionMode.SELECT || (mode == SelectionMode.EDIT && blocks > 1)) {
                            p.x += Dir.getDirX(offset);
                            p.y += Dir.getDirY(offset);
                            p.z += Dir.getDirZ(offset);
                        }
                        Game.instance.sendPacket(new PacketCSetBlock(world.getId(), p, offset, block, 0));
                    } else {
                        Game.instance.sendPacket(new PacketCSetBlock(world.getId(), p, offset, 0, 0));
                    }
                } else {
                    BlockPos p1 = getMin();
                    BlockPos p2 = getMax();

                    EditBlockTask task = new EditBlockTask(p1, p2, Game.instance.selBlock);
                    task.hollow = Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_ALT);
                    Game.instance.edits.add(task);
                    Game.instance.step = 0;
                    task.apply(world);
                    
                }
            }
                
        }
    }
    /**
     * 
     */
    public void toggleMode() {
        reset();
        if (this.mode == SelectionMode.EDIT) {
            this.mode = SelectionMode.SELECT;
        } else if (this.mode == SelectionMode.SELECT) {
            this.mode = SelectionMode.PLAY;
        } else {
            this.mode = SelectionMode.EDIT;
        }
    }
    /**
     * 
     */
    public void reset() {
        this.rayTrace.reset();
        this.mouseOver = null;
        this.mouseDown = false;
        this.mouseStateChanged = false;
        pos[0] = new BlockPos();
        pos[1] = new BlockPos();
    }
}
