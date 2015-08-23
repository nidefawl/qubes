package nidefawl.qubes.gl;

import java.nio.*;

import org.lwjgl.opengl.*;

import nidefawl.qubes.Main;
import nidefawl.qubes.block.Block;

public class Tess extends TesselatorState {
    final public static String[] attributes = new String[] {
            "in_position",
            "in_normal",
            "in_texcoord",
            "in_color",
            "in_brightness",
            "in_blockinfo",
    };

    private final static boolean littleEndian = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    
    public final static Tess instance    = new Tess();
    public final static Tess tessFont    = new Tess();

    protected int           rgba;
    protected float         u, v;
    protected float         u2, v2;
    protected int           br;
    protected int           attrBlockType;
    protected int           attrBlockData;
    protected int           attrBlockRenderType;
    protected int           normal;
    protected float         offsetX;
    protected float         offsetY;
    protected float         offsetZ;
    
    private boolean reset = true;

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

    public void setBrightness(int br) {
        if (!useTexturePtr2 && vertexcount > 0) {
            throw new IllegalStateException("Cannot enable texture pointer after a vertex has been added");
        }
        this.br = br;
        this.useTexturePtr2 = true;
    }

    public void setUV(float u, float v) {
        if (!useTexturePtr && vertexcount > 0) {
            throw new IllegalStateException("Cannot enable texture pointer after a vertex has been added");
        }
        this.u = u;
        this.v = v;
        this.useTexturePtr = true;
    }

    public void setAttr(int blockId, int blockData, int renderType) {
        if (!useAttribPtr1 && vertexcount > 0) {
            throw new IllegalStateException("Cannot enable attr pointer after a vertex has been added");
        }
        this.attrBlockType = blockId;
        this.attrBlockData = blockData;
        this.attrBlockRenderType = renderType;
        this.useAttribPtr1 = true;
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
        if (useTexturePtr2) {
            rawBuffer[index++] = br;
        }
        if (useAttribPtr1) {
            rawBuffer[index++] = (attrBlockType&Block.BLOCK_MASK) | (attrBlockRenderType<<16);
            rawBuffer[index++] = attrBlockData;
        }
        vertexcount++;
    }
    
    public void copyTo(TesselatorState out) {
        int index = getIdx(vertexcount);
        super.copyTo(out, index);
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

    int vboId = 0;

    private boolean buffered;
    int vboSize = 0;
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
    public void dontReset() {
        this.reset = false;
    }
    public void draw(int mode) {
        if (isSoftTesselator()) {
            throw new IllegalStateException("Cannot draw soft tesselator");
        }
        if (vertexcount >1) {
            Engine.enableVAO();
            if (this.vboId == 0) {
                vboId = GL15.glGenBuffers();
            }
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);

            
            if (!buffered) {
                buffered = true;
                if (buffer == null || intBuffer.capacity() < rawBuffer.length) {
                    resizeDirect();
                }
                intBuffer.clear();
                intBuffer.put(rawBuffer, 0, getIdx(vertexcount));
                int len = getIdx(vertexcount) * 4;
                buffer.position(0);
                buffer.limit(len);
                if (vboSize <= len) {
                    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
                } else {
                    GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, len, buffer);
                }
                vboSize = len;
            }
            
            int stride = getVSize();
            
            
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 4, GL11.GL_FLOAT, false, stride*4, 0);
            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("AttribPtr "+0);
            
            int offset = 4;
            if (useNormalPtr) {
                GL20.glEnableVertexAttribArray(1);
                GL20.glVertexAttribPointer(1, 3, GL11.GL_BYTE, false, stride*4, offset*4);
                if (Main.GL_ERROR_CHECKS) Engine.checkGLError("AttribPtr "+1);
                offset+=1;
            }
            if (useTexturePtr) {
                GL20.glEnableVertexAttribArray(2);
                GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, stride*4, offset*4);
                if (Main.GL_ERROR_CHECKS) Engine.checkGLError("AttribPtr "+2);
                offset+=2;
            }
            if (useColorPtr) {
                GL20.glEnableVertexAttribArray(3);
                GL20.glVertexAttribPointer(3, 4, GL11.GL_UNSIGNED_BYTE, true, stride*4, offset*4);
                if (Main.GL_ERROR_CHECKS) Engine.checkGLError("AttribPtr "+3);
                offset+=1;
            }
            if (useTexturePtr2) {
                GL20.glEnableVertexAttribArray(4);
                GL20.glVertexAttribPointer(4, 2, GL11.GL_SHORT, false, stride*4, offset*4);
                if (Main.GL_ERROR_CHECKS) Engine.checkGLError("AttribPtr "+4);
                offset+=1;
            }
            if (useAttribPtr1) {
                GL20.glEnableVertexAttribArray(5);
                GL20.glVertexAttribPointer(5, 4, GL11.GL_SHORT, false, stride*4, offset*4);
                if (Main.GL_ERROR_CHECKS) Engine.checkGLError("AttribPtr "+5);
                offset+=2;
            }
            
            GL11.glDrawArrays(mode, 0, vertexcount);

            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("glDrawArrays ("+vertexcount+", texture: "+useTexturePtr+")");
            for (int i = 0; i < attributes.length; i++)
                GL20.glDisableVertexAttribArray(i);

            Engine.disableVAO();
        }
        if (this.reset)
            resetState();
    }
    

    public void resetState() {
        this.vertexcount = 0;
        this.useNormalPtr = false;
        this.useTexturePtr = false;
        this.useTexturePtr2 = false;
        this.useAttribPtr1 = false;
        this.useColorPtr = false;
        this.u = this.v = 0;
        this.attrBlockType = 0;
        this.attrBlockRenderType= 0;
        this.attrBlockData = 0;
        this.br = 0;
        this.rgba = -1;
        this.offsetX = this.offsetY = this.offsetZ = 0;
        this.reset = true;
        this.buffered = false;
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
    public void drawState(TesselatorState tesselatorState, int mode) {
        tesselatorState.copyTo(this, tesselatorState.getIdx(tesselatorState.vertexcount));
        draw(mode);
    }

}
