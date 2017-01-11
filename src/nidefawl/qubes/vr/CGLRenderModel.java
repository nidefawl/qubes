package nidefawl.qubes.vr;

import jopenvr.*;

public class CGLRenderModel {

    public RenderModel_Vertex_t[] rVertexData;
    public short[] rIndexData;
    public int unVertexCount;
    public int unTriangleCount;
    public int diffuseTextureId;
    private String name;

    public CGLRenderModel(String s, RenderModel_t t) {
        this.name = s;
        rVertexData = (RenderModel_Vertex_t[])t.rVertexData.toArray(t.unVertexCount);
        rIndexData = t.rIndexData.getPointer().getShortArray(0, t.unTriangleCount*3);
        unVertexCount = t.unVertexCount;
        diffuseTextureId = t.diffuseTextureId;
        unTriangleCount = t.unTriangleCount;
    }

    @Override
    public String toString() {
        return "CGLRenderModel["+name+", "+unVertexCount+" vertices, texture "+this.diffuseTextureId+"]";
    }
}
