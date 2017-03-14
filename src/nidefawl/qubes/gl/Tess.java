package nidefawl.qubes.gl;

import java.nio.ByteOrder;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import nidefawl.qubes.util.ITess;
import nidefawl.qubes.util.ITessState;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.vec.Vector4f;

public class Tess extends AbstractTesselatorState implements ITess {
    final public static String[] attributes = new String[] {
            "in_position",
            "in_normal",
            "in_texcoord",
            "in_color",
    };

    private final static boolean littleEndian = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    
    public final static Tess instance    = new Tess();
    public final static Tess tessFont    = new Tess();

    public final static int BUF_INCR  = 1024*1024;

    public int[]         rawBuffer = new int[BUF_INCR];
    protected int           rgba;
    protected int           uintLSB;
    protected int           uintMSB;
    protected float         u, v;
    protected int           normal;
    protected float         offsetX;
    protected float         offsetY;
    protected float         offsetZ;
    
    private final boolean isSoftTesselator;
//    ByteBuffer                   buffer       = null;
//    private IntBuffer            intBuffer;
    ReallocIntBuffer bufInt;

    private int vboIdx;

    private GLVBO[] vbo = new GLVBO[1<<4];
    private GLVBO[] vboIndices = new GLVBO[1<<4];


    public Tess() {
        this(false);
    }
    public Tess(boolean isSoftTesselator) {
        this.isSoftTesselator = isSoftTesselator;
        if (!this.isSoftTesselator) {
            bufInt = new ReallocIntBuffer();
            for (int i = 0; i < this.vbo.length; i++) {
                this.vbo[i] = new GLVBO(GL15.GL_DYNAMIC_DRAW);
            }
            for (int i = 0; i < this.vboIndices.length; i++) {
                this.vboIndices[i] = new GLVBO(GL15.GL_DYNAMIC_DRAW);
            }
        }
    }
    
    @Override
    public boolean isSoftTesselator() {
        return isSoftTesselator;
    }

    @Override
    public void add(float x, float y, float z, float u, float v) {
        if (!useTexturePtr && vertexcount > 0) {
            throw new IllegalStateException("Cannot enable texture pointer after a vertex has been added");
        }
        this.u = u;
        this.v = v;
        this.useTexturePtr = true;
        add(x, y, z);
    }

    @Override
    public void setUV(float u, float v) {
        if (!useTexturePtr && vertexcount > 0) {
            throw new IllegalStateException("Cannot enable texture pointer after a vertex has been added");
        }
        this.u = u;
        this.v = v;
        this.useTexturePtr = true;
    }

    @Override
    public void add(float x, float y) {
        add(x, y, 0);
    }

    @Override
    public void setNormals(float x, float y, float z) {
        if (!useNormalPtr && vertexcount > 0) {
            throw new IllegalStateException("Cannot enable normal pointer after a vertex has been added");
        }
        this.useNormalPtr = true;
        byte byte0 = (byte)(int)(x * 127F);
        byte byte1 = (byte)(int)(y * 127F);
        byte byte2 = (byte)(int)(z * 127F);
        normal = byte0 & 0xff | (byte1 & 0xff) << 8 | (byte2 & 0xff) << 16;
    }

    @Override
    public void add(float x, float y, float z) {
        x+=offsetX;
        y+=offsetY;
        z+=offsetZ;
        int index = getIdx(vertexcount);
        if (index + getVSize() >= rawBuffer.length) {
            resizeBuffer();
        }
        if (offsetX != 0) {
//            System.out.println(vertexcount+ " - "+x+"/"+y+"/"+z);
        }
        rawBuffer[index++] = Float.floatToRawIntBits(x);
        rawBuffer[index++] = Float.floatToRawIntBits(y);
        rawBuffer[index++] = Float.floatToRawIntBits(z);
        rawBuffer[index++] = Float.floatToRawIntBits(1);
        if (useNormalPtr) {
            rawBuffer[index++] = normal;
        }
        if (useTexturePtr) {
            rawBuffer[index++] = Float.floatToRawIntBits(u);
            rawBuffer[index++] = Float.floatToRawIntBits(v);
        }

        if (useColorPtr) {
            rawBuffer[index++] = rgba;
        }
        if (useUINTPtr) {
            rawBuffer[index++] = uintLSB;
            rawBuffer[index++] = uintMSB;
        }
        vertexcount++;
    }
    

    void resizeBuffer() {
        int[] oldBuffer = this.rawBuffer;
        int[] newBuffer = new int[oldBuffer.length + BUF_INCR];
        System.arraycopy(oldBuffer, 0, newBuffer, 0, oldBuffer.length);
        this.rawBuffer = newBuffer;
    }


    @Override
    public void setColorRGBAF(float r, float g, float b, float a) {
        if (!useColorPtr && vertexcount > 0) {
            throw new IllegalStateException("Cannot enable color pointer after a vertex has been added");
        }
        int iR = (int) (r*255.0F);
        int iG = (int) (g*255.0F);
        int iB = (int) (b*255.0F);
        int iA = (int) (a*255.0F);
        this.useColorPtr = true;
        int rgb;
        if (littleEndian) {
            rgb = iB << 16 | iG <<8 | iR; 
        } else {
            rgb = iR << 16 | iG <<8 | iB;
        }
        rgba = rgb|iA<<24;
    }

    @Override
    public void setUIntLSB(int i) {
        if (!useUINTPtr && vertexcount > 0) {
            throw new IllegalStateException("Cannot enable uint potr after a vertex has been added");
        }
        this.useUINTPtr = true;
        uintLSB = i;  
    }
    @Override
    public void setUIntMSB(int i) {
        if (!useUINTPtr && vertexcount > 0) {
            throw new IllegalStateException("Cannot enable uint potr after a vertex has been added");
        }
        this.useUINTPtr = true;
        uintMSB = i;  
    }
    @Override
    public void setColor(int rgb, int i) {
        if (!useColorPtr && vertexcount > 0) {
            throw new IllegalStateException("Cannot enable color pointer after a vertex has been added");
        }
        this.useColorPtr = true;
        if (littleEndian) {
            rgb = (rgb>>16)&0xFF|(rgb&0xFF00)|(rgb&0xFF)<<16;
        }
        rgba = rgb|i<<24;  
        
    }

    @Override
    public void setColorF(int rgb, float alpha) {
        setColor(rgb&0xFFFFFF, (int) Math.max(0, Math.min(255, alpha * 255F)));
    }
    
    public void draw(int mode, AbstractTesselatorState out) {
        if (isSoftTesselator()) {
            throw new IllegalStateException("Cannot draw soft tesselator");
        }
        if (vertexcount >1) {
            int vIdx = getIdx(vertexcount);
            int len = vIdx * 4;
            bufInt.put(rawBuffer, 0, vIdx);
            GLVBO vbo = out.getVBO();
            GLVBO vboIdx = out.getVBOIndices();
            vbo.upload(GL15.GL_ARRAY_BUFFER, bufInt.getByteBuf(), len, false);
            int pos = 0;
            if (mode == GL11.GL_QUADS) {
                mode = GL11.GL_TRIANGLES;
                if (this.vertexcount%4 != 0) {
                    throw new IllegalStateException("Cannot make tri idx: vertexcount%4 != 0");
                }
                int quads = this.vertexcount/4;
                for (int i = 0; i < quads; i++) {
                    vIdx = i*4;
                    rawBuffer[pos++]=(vIdx+0);
                    rawBuffer[pos++]=(vIdx+1);
                    rawBuffer[pos++]=(vIdx+2);
                    rawBuffer[pos++]=(vIdx+2);
                    rawBuffer[pos++]=(vIdx+3);
                    rawBuffer[pos++]=(vIdx+0);
                }
            } else if (mode == GL11.GL_LINES
                    ||mode == GL11.GL_LINE_STRIP
                    ||mode == GL11.GL_TRIANGLES
                    ||mode == GL11.GL_POLYGON) {
                for (int i = 0; i < this.vertexcount; i++) {
                    rawBuffer[i]=i;
                }
                pos = vertexcount;
            } else {
                System.out.println("Mode not supported "+mode);
//                Thread.dumpStack();
            }
            this.idxCount = pos;
            bufInt.put(rawBuffer, 0, pos);
            vboIdx.upload(GL15.GL_ELEMENT_ARRAY_BUFFER, bufInt.getByteBuf(), pos*4, false);
            if (out == this) {
                bindAndDraw(mode);
                this.vboIdx++;
                if (this.vboIdx >= this.vbo.length) {
                    this.vboIdx = 0;
                }
            } else {
                this.copyTo(out);
            }

        }
        resetState();
    }
    @Override
    public GLVBO getVBO() {
        return this.vbo[this.vboIdx];
    }
    @Override
    public GLVBO getVBOIndices() {
        return this.vboIndices[this.vboIdx];
    }
    
    public void draw(int mode) {
        this.draw(mode, this);
    }
    
    
    public void drawQuads() {
        this.draw(GL11.GL_QUADS);
    }
    


    @Override
    public void resetState() {
        this.vertexcount = 0;
        this.useNormalPtr = false;
        this.useTexturePtr = false;
        this.useColorPtr = false;
        this.u = this.v = 0;
        this.rgba = -1;
        this.offsetX = this.offsetY = this.offsetZ = 0;
    }

    public void destroy() {
        this.bufInt.release();
        for (int i = 0; i < vbo.length; i++) {
            this.vbo[i].release();
        }
        for (int i = 0; i < vboIndices.length; i++) {
            this.vboIndices[i].release();
        }
    }

    @Override
    public void setOffset(float f, float j, float g) {
        this.offsetX=f;
        this.offsetY=j;
        this.offsetZ=g;
    }

    public static void destroyAll() {
        instance.destroy();
        tessFont.destroy();
    }
    @Override
    public void add(Vector4f tmp1) {
        add(tmp1.x, tmp1.y, tmp1.z);
    }
    @Override
    public void add(Vector3f tmp1) {
        add(tmp1.x, tmp1.y, tmp1.z);
    }
    
    @Override
    public void drawTris() {
        this.draw(GL11.GL_TRIANGLES);
    }
    @Override
    public boolean isDynamic() {
        return false;
    }
    @Override
    public void drawQuads(ITessState tesstate) {
        this.draw(GL11.GL_QUADS, (AbstractTesselatorState) tesstate);
    }

}
