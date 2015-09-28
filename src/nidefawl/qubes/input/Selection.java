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
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.world.World;

public class Selection {
    
    public static enum SelectionMode {
        EDIT, SELECT
    }
    private SelectionMode mode = SelectionMode.EDIT;

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
    private BlockPos mouseOver;

    public void renderBlockHighlight(World world, float fTime) {
        boolean b = false;
        if (hasSelection()) {
            if (updateBB) {
                renderBB();
                updateBB = false;
            }

            glEnable(GL_BLEND);
            Shaders.colored.enable();
            b = true;
            highlightSelection.drawQuads();
            glDisable(GL_DEPTH_TEST);
            glDisable(GL_CULL_FACE);
            Shaders.wireframe.enable();
            Shaders.wireframe.setProgramUniform4f("linecolor", 1, 0.2f, 0.2f, 1);
            Shaders.wireframe.setProgramUniform1f("maxDistance", 1000);
            highlightSelection.drawQuads();
            glEnable(GL_DEPTH_TEST);
        }
        if (mouseOver != null && this.mode == SelectionMode.SELECT) {
            if (!b) {
                glEnable(GL_BLEND);
                glDisable(GL_CULL_FACE);
            }
            b = true;
            Shaders.colored.enable();
            Shaders.colored.setProgramUniform3f("in_offset", this.mouseOver.x, this.mouseOver.y, this.mouseOver.z);
            highlightBlockOver.drawQuads();
            Shaders.colored.setProgramUniform3f("in_offset", 0, 0, 0);

        }
        if (b) {

            Shader.disable();
            glEnable(GL_CULL_FACE);
            glDisable(GL_BLEND);
        }
    }
    public void renderBlockOver() {

        float ext = 1 / 32F;
        Tess tesselator = Tess.instance;
        tesselator.setColorRGBAF(1, 1, 1, 0.2F);
        BlockPos sel1 = new BlockPos();
        BlockPos sel2 = new BlockPos();
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

        tesselator.draw(GL_QUADS, highlightBlockOver);
        tesselator.resetState();
    }

    public void renderBB() {

        float ext = 1 / 32F;
        Tess tesselator = Tess.instance;
        tesselator.setColorRGBAF(1, 1, 1, 0.2F);
        BlockPos sel1 = pos[0];
        BlockPos sel2 = pos[1];
        float minX = Math.min(sel1.x, sel2.x) - ext;
        float minY = Math.min(sel1.y, sel2.y) - ext;
        float minZ = Math.min(sel1.z, sel2.z) - ext;
        float maxX = Math.max(sel1.x, sel2.x) + ext + 1;
        float maxY = Math.max(sel1.y, sel2.y) + ext + 1;
        float maxZ = Math.max(sel1.z, sel2.z) + ext + 1;
        tesselator.setNormals(0, 0, -1);
        tesselator.add(minX, maxY, minZ);
        tesselator.add(maxX, maxY, minZ);
        tesselator.add(maxX, minY, minZ);
        tesselator.add(minX, minY, minZ);

        tesselator.setNormals(0, 0, 1);
        tesselator.add(minX, minY, maxZ);
        tesselator.add(maxX, minY, maxZ);
        tesselator.add(maxX, maxY, maxZ);
        tesselator.add(minX, maxY, maxZ);

        tesselator.setNormals(0, 1, 0);
        tesselator.add(minX, maxY, maxZ);
        tesselator.add(maxX, maxY, maxZ);
        tesselator.add(maxX, maxY, minZ);
        tesselator.add(minX, maxY, minZ);

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
        tesselator.add(maxX, maxY, minZ);
        tesselator.add(maxX, maxY, maxZ);
        tesselator.add(maxX, minY, maxZ);
        tesselator.add(maxX, minY, minZ);

        tesselator.draw(GL_QUADS, highlightSelection);
        tesselator.resetState();
    }

    public void update(World world, double px, double py, double pz) {
        this.rayTrace.reset();
        if (world == null) {
            this.mouseDown = false;
            this.mouseStateChanged = false;
            return;
        }
        if (Engine.vDir != null) {
            this.rayTrace.doRaytrace(world, Engine.vOrigin, Engine.vDir, extendReach() ? 200 : 55);
            BlockPos p = rayTrace.getColl();
            this.mouseOver = p;
            if (this.mode == SelectionMode.SELECT) {
                if (p != null) {
                    if (mouseStateChanged && this.mouseDown) { // first call after mousedown
                        set(0, p);
                        set(1, p);
                    } else if (this.mouseDown) {
                        set(1, p);
                    }
                }
                this.mouseStateChanged = false;
                return;
            }
            //      if (p != null) {
            //          if (this.mouseClicked && !mouseDown) {
            //              setBlock();
            //          }
            //      }
            //TODO: add some better logic for highlighting, don't render "into" camera
            if (p != null && !(p.x == GameMath.floor(px) && p.y == GameMath.floor(py) && p.z == GameMath.floor(pz))) {
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
            this.mouseStateChanged = false;
            if (this.mouseDown && Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL)) {
                onRelease();
            }
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
            BlockPos p = pos[0];
            World world = Game.instance.getWorld();
            if (p != null && world != null) {
                int type = world.getType(p.x, p.y, p.z);
                Game.instance.selBlock = type;
            }
            return;
        }
        this.mouseStateChanged = this.mouseDown != isDown;
        this.mouseDown = isDown;
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
        if (this.mode == SelectionMode.SELECT) {
            return;
        }
        World world = Game.instance.getWorld();
        if (world != null) {
            int blocks = getNumBlocks();
//            System.out.println(blocks);
            if (blocks == 1) {
                BlockPos blockPos = rayTrace.getColl();
                if (blockPos != null) {
                    BlockPos face = rayTrace.getFace();
                    int blockX = blockPos.x;
                    int blockY = blockPos.y;
                    int blockZ = blockPos.z;
                    //            int i = this.world.getBiome(blockX, blockY, blockZ);
                    int id = world.getType(blockX, blockY, blockZ);
                    String msg = "";
                    msg += String.format("Coordinate:  %d %d %d\n", blockX, blockY, blockZ);
                    msg += String.format("Block:           %d\n", id);
                    //            msg += String.format("Biome:          %s\n", BiomeGenBase.byId[i].biomeName);
                    msg += String.format("Chunk:          %d/%d", blockX >> 4, blockZ >> 4);

                    if (Game.instance.statsOverlay != null) {
                        Game.instance.statsOverlay.setMessage(msg);
                    }
                    int block = Game.instance.selBlock;
                    if (block > 0) {
                        blockX += face.x;
                        blockY += face.y;
                        blockZ += face.z;
                        Game.instance.sendPacket(new PacketCSetBlock(world.getId(), blockX, blockY, blockZ, block));
                        world.setType(blockX, blockY, blockZ, block, Flags.MARK);
                    } else {
                        Game.instance.sendPacket(new PacketCSetBlock(world.getId(), blockX, blockY, blockZ, 0));

                        world.setType(blockX, blockY, blockZ, 0, Flags.MARK);
                    }
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
    /**
     * 
     */
    public void toggleMode() {
        if (this.mode == SelectionMode.EDIT) {
            this.mode = SelectionMode.SELECT;
        } else {
            this.mode = SelectionMode.EDIT;
        }
    }
    /**
     * 
     */
    public void reset() {
        pos[0] = new BlockPos();
        pos[1] = new BlockPos();
    }
}
