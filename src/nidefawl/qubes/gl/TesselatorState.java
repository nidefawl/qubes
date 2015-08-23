package nidefawl.qubes.gl;

public class TesselatorState {
    public final static int BUF_INCR  = 1024;
    public int[]         rawBuffer = new int[BUF_INCR];
    public int           vertexcount;

    public boolean       useColorPtr;
    public boolean       useTexturePtr;
    public boolean       useTexturePtr2;
    public boolean       useNormalPtr;
    public boolean       useAttribPtr1;
    
    public void copyTo(TesselatorState out, int len) {
        if (len >= out.rawBuffer.length) {
            out.rawBuffer = new int[len];
        }
        System.arraycopy(this.rawBuffer, 0, out.rawBuffer, 0, len);
        out.vertexcount = this.vertexcount;
        out.useColorPtr = this.useColorPtr;
        out.useTexturePtr = this.useTexturePtr;
        out.useTexturePtr2 = this.useTexturePtr2;
        out.useNormalPtr = this.useNormalPtr;
        out.useAttribPtr1 = this.useAttribPtr1;
    }
    


    public int getIdx(int v) {
        return getVSize() * v;
    }
    
    public int getVSize() {
        int stride = 4;
        if (useColorPtr)
            stride++;
        if (useNormalPtr)
            stride+=3;
        if (useTexturePtr)
            stride+=2;
        if (useTexturePtr2)
            stride+=1;
        if (useAttribPtr1)
            stride+=2;
        return stride;
    }
    
    public TesselatorState copyState() {
        TesselatorState state = new TesselatorState();
        this.copyTo(state, this.getIdx(this.vertexcount));
        return state;
    }
}
