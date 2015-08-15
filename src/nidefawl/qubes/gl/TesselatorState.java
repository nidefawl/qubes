package nidefawl.qubes.gl;

public class TesselatorState {
    public final static int BUF_INCR  = 1024;
    protected int[]         rawBuffer = new int[BUF_INCR];
    public int           vertexcount;

    protected boolean       useColorPtr;
    protected boolean       useTexturePtr;
    protected boolean       useTexturePtr2;
    protected boolean       useTexturePtr3;
    protected boolean       useNormalPtr;
    protected boolean       useAttribPtr1;
    
    public void copyTo(TesselatorState out, int len) {
        if (len >= out.rawBuffer.length) {
            out.rawBuffer = new int[len];
        }
        System.arraycopy(this.rawBuffer, 0, out.rawBuffer, 0, len);
        out.vertexcount = this.vertexcount;
        out.useColorPtr = this.useColorPtr;
        out.useTexturePtr = this.useTexturePtr;
        out.useTexturePtr2 = this.useTexturePtr2;
        out.useTexturePtr3 = this.useTexturePtr3;
        out.useNormalPtr = this.useNormalPtr;
        out.useAttribPtr1 = this.useAttribPtr1;
    }
    


    int getIdx(int v) {
        return getVSize() * v;
    }
    
    public int getVSize() {
        int stride = 3;
        if (useColorPtr)
            stride++;
        if (useNormalPtr)
            stride+=3;
        if (useTexturePtr)
            stride+=2;
        if (useTexturePtr2)
            stride+=1;
        if (useTexturePtr3)
            stride+=2;
        if (useAttribPtr1)
            stride+=2;
        return stride;
    }
}
