package nidefawl.qubes.util;

import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.vec.Vector4f;

public interface ITess {

    boolean isSoftTesselator();

    void add(float x, float y, float z, float u, float v);

    void setUV(float u, float v);

    void add(float x, float y);

    void setNormals(float x, float y, float z);

    void add(float x, float y, float z);

    void setColorRGBAF(float r, float g, float b, float a);

    void setUIntLSB(int i);

    void setUIntMSB(int i);

    void setColor(int rgb, int i);

    void setColorF(int rgb, float alpha);

    void resetState();

    void setOffset(float f, float j, float g);

    void add(Vector4f tmp1);

    void add(Vector3f tmp1);

    void drawQuads();

    void drawTris();

    void drawQuads(ITessState fullBlock);

}
