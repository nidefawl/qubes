package nidefawl.qubes.gl;

import java.nio.*;

import nidefawl.game.Main;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class Tess {

    private final static boolean littleEndian = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    
    public final static Tess instance    = new Tess();
    public final static Tess instance2    = new Tess();
//    public final static int  VERTEX_SIZE = 4 * 3 + 4 * 2 + 4 * 4;
    public final static int  BUF_INCR = 1024;
    int                      vertexcount;
    int                      rgba;
    int[]                   rawBuffer   = new int[BUF_INCR];

    ByteBuffer                   buffer       = null;
    private ShortBuffer          shortBuffer  = null;
    int                          vaoId;
    int                          vboId;
    boolean                      useColorPtr;
    boolean                      useTexturePtr;
    boolean                      useTexturePtr2;
    boolean                      useTexturePtr3;
    boolean                      useNormalPtr;
    float                        u, v;
    float                        u2, v2;
    private int br;
    private IntBuffer intBuffer;
    private FloatBuffer floatBuffer;
    private int normal;
    float offsetX;
    float offsetY;
    float offsetZ;
    private boolean reset = true;

    public Tess() {
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

    public void add(float x, float y) {
        add(x, y, 0);
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
        return stride;
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
        vertexcount++;
    }

    void resizeBuffer() {
        int[] oldBuffer = this.rawBuffer;
        int[] newBuffer = new int[oldBuffer.length + BUF_INCR];
        System.arraycopy(oldBuffer, 0, newBuffer, 0, oldBuffer.length);
        this.rawBuffer = newBuffer;
        resizeDirect();
    }

    private void resizeDirect() {
        buffer = ByteBuffer.allocateDirect(rawBuffer.length*4).order(ByteOrder.nativeOrder());
        intBuffer = buffer.asIntBuffer();
        floatBuffer = buffer.asFloatBuffer();
        shortBuffer = buffer.asShortBuffer();
    }

    int getIdx(int v) {
        return getVSize() * v;
    }

    public void setColor(int rgb, int i) {
        if (!useColorPtr && vertexcount > 0) {
            throw new IllegalStateException("Cannot enable color pointer after a vertex has been added");
        }
        this.useColorPtr = true;
        if (littleEndian) {
            rgb = (rgb>>16)&0xFF|(rgb&0xFF00)|(rgb&0xFF)<<16;
//            rgba = Integer.reverseBytes((rgb&0xFFFFFF) | i << 24);
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
        this.useColorPtr = false;
        this.u = this.v = 0;
        this.br = 0;
        this.rgba = -1;
        this.offsetX = this.offsetY = this.offsetZ = 0;
        this.reset = true;
    }

    public void destroy() {
    }

    public void setOffset(float f, float j, float g) {
        this.offsetX=f;
        this.offsetY=j;
        this.offsetZ=g;
    }

}
