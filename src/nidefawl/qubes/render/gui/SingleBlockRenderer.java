/**
 * 
 */
package nidefawl.qubes.render.gui;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.chunk.blockdata.BlockData;
import nidefawl.qubes.gl.VertexBuffer;
import nidefawl.qubes.item.StackData;
import nidefawl.qubes.meshing.BlockFaceAttr;
import nidefawl.qubes.meshing.BlockRenderer;
import nidefawl.qubes.util.SingleBlockWorld;
import nidefawl.qubes.vec.BlockPos;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class SingleBlockRenderer extends BlockRenderer {
    final static BlockPos offset = new BlockPos(64, 64, 64);
    final SingleBlockWorld singleBlockWorld;
    private VertexBuffer singleBlockBuffer;
    public SingleBlockRenderer() {
        this.singleBlockWorld = new SingleBlockWorld();
        this.singleBlockBuffer = new VertexBuffer(1024);
        this.w = this.singleBlockWorld;
        this.attr = new BlockFaceAttr();
    }

    public VertexBuffer renderSingleBlock(Block block, int data, StackData stackData) {

        this.singleBlockBuffer.reset();
        this.singleBlockWorld.set(offset.x, offset.y, offset.z, block.id, data);
        BlockData bdata = stackData != null ? stackData.getBlockData() : null;
        this.singleBlockWorld.setBlockData(bdata);
        attr.setOffset(-offset.x-0.5f, -offset.y-0.5f, -offset.z-0.5f);
        super.render(offset.x, offset.y, offset.z);
        int z = 10;
//        attr.setOffset(0, 0, 0);
//        attr.v0.setPos(-10, 10, z);
//        attr.v1.setPos(10, 10, z);
//        attr.v2.setPos(10, -10, z);
//        attr.v3.setPos(-10, -10, z);
//        attr.v3.setUV(0.5f, 0.5f);
//        attr.put(this.singleBlockBuffer);
        return this.singleBlockBuffer;
    }

    @Override
    protected void putBuffer(Block block, int targetBuffer) {
        attr.put(this.singleBlockBuffer);
    }
    protected void putSingleVert(Block block, int targetBuffer, int attrIdx) {
        attr.putSingleVert(attrIdx, this.singleBlockBuffer);
    }
    protected void putTriIndex(Block block, int targetBuffer, int[] vertexIdx, int numIdx, int numVerts) {
        singleBlockBuffer.putTriVertIndex(vertexIdx, numIdx, numVerts);
    }
    protected boolean isInventoryBlockRender() {
        return true;
    }

}
