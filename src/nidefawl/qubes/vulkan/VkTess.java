package nidefawl.qubes.vulkan;

import static org.lwjgl.vulkan.VK10.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.lwjgl.vulkan.VkCommandBuffer;

import nidefawl.qubes.gl.ReallocIntBuffer;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.ITess;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.vec.Vector4f;

public class VkTess extends AbstractVkTesselatorState implements ITess {
    public static int CREATE_QUAD_IDX_BUFFER = 0;
    public static int CREATE_TRI_IDX_BUFFER = 1;
    public static int STREAM_UPLOAD = 0;
    public static int DEVICE_LOCAL_UPLOAD = 1;
    private final static boolean littleEndian = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    
    public final static VkTess instance    = new VkTess("global");
    public final static VkTess tessFont    = new VkTess("font");

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

    static class FrameLocalBuffers {
        private int vboIdx;
        private VkBuffer[] vbo;
        private VkBuffer[] vboIndices;
        public FrameLocalBuffers(VkTess tess, VKContext context, int numbuffers) {
            vbo = new VkBuffer[numbuffers];
            vboIndices = new VkBuffer[numbuffers];
            for (int i = 0; i < numbuffers; i++) {
                this.vbo[i] = new VkBuffer(context).tag("tess_"+tess.tag+"_frame"+i+"_vertex");
                this.vboIndices[i] = new VkBuffer(context).tag("tess_"+tess.tag+"_frame"+i+"_index");
            }
        }
        public void destroy() {
            for (int i = 0; i < this.vbo.length; i++) {
                this.vbo[i].destroy();
                this.vboIndices[i].destroy();
                Thread.dumpStack();
            }
        }
        public VkBuffer vbo() {
            return this.vbo[vboIdx];
        }
        public VkBuffer vboIndices() {
            return this.vboIndices[vboIdx];
        }
    }
    FrameLocalBuffers[] buffers = new FrameLocalBuffers[0];
    private String tag;
    public static void init(VKContext context, int numImages) {
        instance.initInstance(context, numImages);
        tessFont.initInstance(context, numImages);
    }

    private void initInstance(VKContext context, int numImages) {
        if (!this.isSoftTesselator) {
            for (int i = 0; i < buffers.length; i++) {
                if (buffers[i] != null) {
                    buffers[i].destroy();
                }
            }
            
            buffers = new FrameLocalBuffers[numImages];
            for (int i = 0; i < buffers.length; i++) {
                buffers[i] = new FrameLocalBuffers(this, context, 1<<4);
            }
        }
    }
    public VkTess(String s) {
        this(false, s);
    }
    public VkTess(boolean isSoftTesselator, String s) {
        this.isSoftTesselator = isSoftTesselator;
        if (!this.isSoftTesselator) {
            bufInt = new ReallocIntBuffer();
        }
        this.tag = s;
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

    public void setUIntLSB(int i) {
        if (!useUINTPtr && vertexcount > 0) {
            throw new IllegalStateException("Cannot enable uint potr after a vertex has been added");
        }
        this.useUINTPtr = true;
        uintLSB = i;  
    }
    public void setUIntMSB(int i) {
        if (!useUINTPtr && vertexcount > 0) {
            throw new IllegalStateException("Cannot enable uint potr after a vertex has been added");
        }
        this.useUINTPtr = true;
        uintMSB = i;  
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
    
    public void finish(int idxMode, int bufferMode, AbstractVkTesselatorState out, int bufferOffset) {
        if (isSoftTesselator()) {
            throw new IllegalStateException("Cannot draw soft tesselator");
        }
        if (vertexcount >1) {
            int currentBuffer = VKContext.currentBuffer;
            FrameLocalBuffers buffer = buffers[currentBuffer];
            if (out == this) {
                buffer.vboIdx++;
                if (buffer.vboIdx >= buffer.vbo.length) {
                    buffer.vboIdx = 0;
                }
            }
            int vIdx = getIdx(vertexcount);
            bufInt.put(rawBuffer, 0, vIdx);
            VkBuffer bufferVertex = out.getVertexBuffer();
            VkBuffer bufferIdx = out.getIndexBuffer();
            upload(bufferVertex, bufferMode, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, bufInt.getByteBuf(), bufferOffset);
            int pos = 0;
            if (idxMode == CREATE_QUAD_IDX_BUFFER) {
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
            } else if (idxMode == CREATE_TRI_IDX_BUFFER) {
                for (int i = 0; i < this.vertexcount; i++) {
                    rawBuffer[i]=i;
                }
                pos = vertexcount;
            } else {
                System.out.println("Mode not supported "+idxMode);
//                Thread.dumpStack();
            }
            this.idxCount = pos;
            bufInt.put(rawBuffer, 0, pos);
            upload(bufferIdx, bufferMode, VK_BUFFER_USAGE_INDEX_BUFFER_BIT, bufInt.getByteBuf(), bufferOffset);
            if (out != this) {
                this.copyTo(out);
            }

        }
        resetState();
    }
    private void upload(VkBuffer buffer, int bufferMode, int bufferType, ByteBuffer byteBuf, int bufferoffset) {
        if (buffer.getSize() < byteBuf.remaining()+1 || (bufferMode == DEVICE_LOCAL_UPLOAD) != buffer.isDeviceLocal()) {
            buffer.destroy();
            buffer.create(bufferType, GameMath.nextPowerOf2(byteBuf.remaining()), (bufferMode == DEVICE_LOCAL_UPLOAD));
        }
        buffer.upload(byteBuf, bufferoffset);
        
    }
    public FrameLocalBuffers buffer() {
        return this.buffers[VKContext.currentBuffer];
    }
    @Override
    public VkBuffer getVertexBuffer() {
        return buffer().vbo();
    }
    @Override
    public VkBuffer getIndexBuffer() {
        return buffer().vboIndices();
    }
    
    public void finish(int mode) {
        this.finish(mode, STREAM_UPLOAD, this, 0);
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
        this.bufInt.release();
        for (int i = 0; i < buffers.length; i++) {
            this.buffers[i].destroy();
            this.buffers[i] = null;
        }
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
    public void add(Vector4f tmp1) {
        add(tmp1.x, tmp1.y, tmp1.z);
    }
    public void add(Vector3f tmp1) {
        add(tmp1.x, tmp1.y, tmp1.z);
    }

}
