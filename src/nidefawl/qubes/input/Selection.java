package nidefawl.qubes.input;

import static org.lwjgl.opengl.GL11.*;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gl.TesselatorState;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.RayTrace;
import nidefawl.qubes.vec.BlockPos;
import nidefawl.qubes.world.World;

public class Selection {
    private TesselatorState highlightSelection;
    boolean                 mouseDown         = false;
    boolean                 mouseStateChanged = false;

    public void init() {

        highlightSelection = new TesselatorState();
    }

    public BlockPos[] selection = new BlockPos[] {
            new BlockPos(), new BlockPos()
    };

    public void resetSelection() {
        this.mouseDown = false;
        //        selection[0] = null;
        //        selection[1] = null;
    }

    public boolean hasSelection() {
        return selection[0] != null && selection[1] != null;
    }

    boolean updateBB = false;

    public void renderBlockHighlight(World world, float fTime) {

        if (hasSelection()) {
            if (updateBB) {
                renderBB();
            }
            glEnable(GL_BLEND);
            glDisable(GL_CULL_FACE);
            Shaders.colored.enable();
            //            Shaders.colored.setProgramUniform3f("in_offset", this.highlight.x, this.highlight.y, this.highlight.z);
            highlightSelection.drawQuads();
            //            Shaders.colored.setProgramUniform3f("in_offset", 0, 0, 0);
            Shader.disable();
            glEnable(GL_CULL_FACE);
            glDisable(GL_BLEND);
        }
    }

    public void renderBB() {

        float ext = 1 / 32F;
        Tess tesselator = Tess.instance;
        tesselator.setColorRGBAF(1, 1, 1, 0.2F);
        BlockPos sel1 = selection[0];
        BlockPos sel2 = selection[1];
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

    public void update(World world, double px, double py, double pz, RayTrace rayTrace) {
        BlockPos p = rayTrace.getColl();
        //      if (p != null) {
        //          if (this.mouseClicked && !mouseDown) {
        //              setBlock();
        //          }
        //      }
        //TODO: add some better logic for highlighting, don't render "into" camera
        if (p != null && !(p.x == GameMath.floor(px) && p.y == GameMath.floor(py) && p.z == GameMath.floor(pz))) {
            if (!mouseDown) {
                set(0, p);
                set(1, p);
            } else if (mouseStateChanged) {
                set(0, p);
            } else {
                set(1, p);
            }
        } else {
            if (!mouseDown) {
                set(1, selection[0]);
            }
            //          System.err.println("fail "+p);
        }
        this.mouseStateChanged = false;
    }

    private void set(int i, BlockPos p2) {
        BlockPos p = selection[i];
        if (p.x != p2.x || p.z != p2.z || p.y != p2.y) {
            p.x = p2.x;
            p.y = p2.y;
            p.z = p2.z;
            updateBB = true;
        }
    }

    public void clicked(int button, boolean isDown) {
        this.mouseDown = isDown;
        this.mouseStateChanged = this.mouseDown != isDown;
        if (this.mouseStateChanged) {
            if (mouseDown) {
            } else {

            }
        }
    }

    public boolean extendReach() {
        return this.mouseDown;
    }

    public BlockPos getMin() {
        return new BlockPos(Math.min(this.selection[0].x, this.selection[1].x), Math.min(this.selection[0].y, this.selection[1].y), Math.min(this.selection[0].z, this.selection[1].z));
    }

    public BlockPos getMax() {
        return new BlockPos(Math.max(this.selection[0].x, this.selection[1].x), Math.max(this.selection[0].y, this.selection[1].y), Math.max(this.selection[0].z, this.selection[1].z));
    }

    public int getNumBlocks() {
        BlockPos p1 = getMin();
        BlockPos p2 = getMax();
        return (p2.x-p1.x+1)*(p2.y-p1.y+1)*(p2.z-p1.z+1);
    }
}
