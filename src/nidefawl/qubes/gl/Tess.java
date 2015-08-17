package nidefawl.qubes.gl;

import java.nio.*;

import nidefawl.game.Main;
import nidefawl.qubes.block.Block;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

public class Tess extends TesselatorState {
    public final static int ATTR_BLOCK = 6;

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
    private ShortBuffer          shortBuffer  = null;
    private IntBuffer            intBuffer;
    private FloatBuffer          floatBuffer;

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

    public void setUV2(float u, float v) {
        if (!useTexturePtr3 && vertexcount > 0) {
            throw new IllegalStateException("Cannot enable texture pointer after a vertex has been added");
        }
        this.u2 = u;
        this.v2 = v;
        this.useTexturePtr3 = true;
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
        if (useNormalPtr) {
            rawBuffer[index++] = normal;
        }
        if (useTexturePtr) {
            rawBuffer[index++] = Float.floatToRawIntBits(u);
            rawBuffer[index++] = Float.floatToRawIntBits(v);
        }
//        if (useTexturePtr3) {
//            rawBuffer[index++] = Float.floatToRawIntBits(u2);
//            rawBuffer[index++] = Float.floatToRawIntBits(v2);
//        }
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

    private void resizeDirect() {
        buffer = ByteBuffer.allocateDirect(rawBuffer.length*4).order(ByteOrder.nativeOrder());
        intBuffer = buffer.asIntBuffer();
        floatBuffer = buffer.asFloatBuffer();
        shortBuffer = buffer.asShortBuffer();
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
            if (buffer == null || intBuffer.capacity() < rawBuffer.length) {
                resizeDirect();
            }

//            System.out.println("draw "+vertexcount+" vertex");
//            System.out.println("draw "+getIdx(vertexcount)+" ints");
//            System.out.println("getIdx(vertexcount) "+getIdx(vertexcount) );
//            System.out.println("getVSize() "+getVSize() );
//            System.out.println("rawBuffer.length "+rawBuffer.length );
//            System.out.println("intBuffer.position() "+intBuffer.position() );
//            System.out.println("intBuffer.capacity() "+intBuffer.capacity());
//            System.out.println("intBuffer.remaining() "+intBuffer.remaining());
            intBuffer.clear();
            intBuffer.put(rawBuffer, 0, getIdx(vertexcount));
            buffer.position(0);
            buffer.limit(getIdx(vertexcount) * 4);
            int stride = getVSize();
            int offset = 0;
            floatBuffer.position(0);
            GL11.glVertexPointer(3, stride*4, floatBuffer);
            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("vertexptr");
            offset+=3;
            if (useNormalPtr) {
                GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
                buffer.position(offset*4);
                GL11.glNormalPointer(stride*4, buffer);
                offset+=1;
            }
            if (useTexturePtr) {
                GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                floatBuffer.position(offset);
                GL11.glTexCoordPointer(2, stride*4, floatBuffer);
                offset+=2;
            }
//            if (useTexturePtr3) {
//                GL13.glClientActiveTexture(GL13.GL_TEXTURE1);
//                floatBuffer.position(offset);
//                GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
//                GL11.glTexCoordPointer(2, stride*4, floatBuffer);
//                GL13.glClientActiveTexture(GL13.GL_TEXTURE0);
//                offset+=2;
//            }
            if (useColorPtr) {
                GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
//                System.out.println("set c off "+offset*4);
//              System.out.println("buffer.limit() "+buffer.limit() );
                buffer.position(offset*4);
                GL11.glColorPointer(4, true, stride*4, buffer);
                offset+=1;
            }
            if (useTexturePtr2) {
                GL13.glClientActiveTexture(GL13.GL_TEXTURE1);
                shortBuffer.position(offset*2);
                GL11.glTexCoordPointer(2, stride*4, shortBuffer);
                GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                GL13.glClientActiveTexture(GL13.GL_TEXTURE0);
                offset+=1;
            }
            if (useAttribPtr1) {
                GL20.glEnableVertexAttribArray(ATTR_BLOCK);
                shortBuffer.position(offset*2);
                GL20.glVertexAttribPointer(ATTR_BLOCK, 3, true, false, stride*4, shortBuffer);
                offset+=2;
            }
            
            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("glEnableClientState");
            buffer.position(0);
            buffer.limit(getIdx(vertexcount) * 4);
            GL11.glDrawArrays(mode, 0, vertexcount);

            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("glDrawArrays ("+vertexcount+", texture: "+useTexturePtr+")");
            GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
            if (Main.GL_ERROR_CHECKS) Engine.checkGLError("glDisableClientState");

            if (useColorPtr)
                GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);

            if (useTexturePtr)
                GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);

            if (useTexturePtr2||useTexturePtr3) {
                GL13.glClientActiveTexture(GL13.GL_TEXTURE1);
                GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                GL13.glClientActiveTexture(GL13.GL_TEXTURE0);
            }
            if (useNormalPtr)
                GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);

            if (useAttribPtr1)
                GL20.glDisableVertexAttribArray(ATTR_BLOCK);
            
        }
        if (this.reset)
            resetState();
    }
    

    public void resetState() {
        this.vertexcount = 0;
        this.useNormalPtr = false;
        this.useTexturePtr = false;
        this.useTexturePtr2 = false;
        this.useTexturePtr3 = false;
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
    }

    public void destroy() {
        buffer = null;
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
