package nidefawl.qubes.gl;

import java.nio.*;

import org.lwjgl.opengl.*;

import nidefawl.qubes.block.Block;

public class Tess extends TesselatorState {
    final public static String[] attributes = new String[] {
            "in_position",
            "in_normal",
            "in_texcoord",
            "in_color",
    };

    private final static boolean littleEndian = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    
    public final static Tess instance    = new Tess();
    public final static Tess tessFont    = new Tess();
    public static boolean useClientStates;

    public final static int BUF_INCR  = 1024;

    public int[]         rawBuffer = new int[BUF_INCR];
    protected int           rgba;
    protected float         u, v;
    protected int           normal;
    protected float         offsetX;
    protected float         offsetY;
    protected float         offsetZ;
    
    private final boolean isSoftTesselator;
    ByteBuffer                   buffer       = null;
    private IntBuffer            intBuffer;

    public Tess() {
        this(false);
    }
    public Tess(boolean isSoftTesselator) {
        this.isSoftTesselator = isSoftTesselator;
    }
    
    public boolean isSoftTesselator() {
        return isSoftTesselator;
    }

    public void add(float x, float y, float z, float u, float v) {
        if (!useTexturePtr && vertexcount > 0) {
            throw new IllegalStateException("Cannot enable texture pointer after a vertex has been added");
        }
        this.u = u;
        this.v = v;
        this.useTexturePtr = true;
        add(x, y, z);
    }

    public void setUV(float u, float v) {
        if (!useTexturePtr && vertexcount > 0) {
            throw new IllegalStateException("Cannot enable texture pointer after a vertex has been added");
        }
        this.u = u;
        this.v = v;
        this.useTexturePtr = true;
    }

    public void add(float x, float y) {
        add(x, y, 0);
    }

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
        vertexcount++;
    }
    

    void resizeBuffer() {
        int[] oldBuffer = this.rawBuffer;
        int[] newBuffer = new int[oldBuffer.length + BUF_INCR];
        System.arraycopy(oldBuffer, 0, newBuffer, 0, oldBuffer.length);
        this.rawBuffer = newBuffer;
        if (!isSoftTesselator()) {
            resizeDirect();   
        }
    }

    private void resizeDirect() {
        buffer = ByteBuffer.allocateDirect(rawBuffer.length*4).order(ByteOrder.nativeOrder());
        intBuffer = buffer.asIntBuffer();
    }

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

    public void setColorF(int rgb, float alpha) {
        setColor(rgb&0xFFFFFF, (int) Math.max(0, Math.min(255, alpha * 255F)));
    }
    
    public void draw(int mode, TesselatorState out) {
        if (isSoftTesselator()) {
            throw new IllegalStateException("Cannot draw soft tesselator");
        }
        if (vertexcount >1) {
            if (buffer == null || intBuffer.capacity() < rawBuffer.length) {
                resizeDirect();
            }
            int vIdx = getIdx(vertexcount);
            int len = vIdx * 4;
            intBuffer.clear();
            intBuffer.put(rawBuffer, 0, vIdx);
            buffer.position(0);
            buffer.limit(len);
            if (useClientStates) {
            } else {
                out.bindVBO();
                if (out.vboSize <= len) {
                    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
                } else {
                    GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, len, buffer);
                }
                out.vboSize = len;
            }
            if (out == this) {
                if (useClientStates) {
                    setClientStates(buffer);
                } else {
                    setAttrPtr();
                }
                drawVBO(mode);
                if (useClientStates) {
                    GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
                    GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
                    GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                    GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
                } else {
                    for (int i = 0; i < 3; i++)
                        GL20.glDisableVertexAttribArray(i);
                }
            } else {
                this.copyTo(out);
            }

        }
        resetState();
    }
    
    public void draw(int mode) {
        this.draw(mode, this);
    }
    


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
        buffer = null;
        GL15.glDeleteBuffers(this.vboId);
    }

    public void setOffset(float f, float j, float g) {
        this.offsetX=f;
        this.offsetY=j;
        this.offsetZ=g;
    }

    public static void destroyAll() {
        instance.destroy();
        tessFont.destroy();
    }

}
