package nidefawl.qubes.vulkan;

import static org.lwjgl.vulkan.VK10.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.lwjgl.vulkan.VkCommandBuffer;

import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.ReallocIntBuffer;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.util.GameLogicError;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.ITess;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.vec.Vector4f;

public class VkTess extends AbstractVkTesselatorState implements ITess {
    public static int CREATE_QUAD_IDX_BUFFER = 0;
    public static int CREATE_PER_VERTEX_IDX_BUFFER = 1;
    public static int STREAM_UPLOAD = 0;
    public static int DEVICE_LOCAL_UPLOAD = 1;
    private final static boolean littleEndian = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    
    public final static VkTess instance    = new VkTess("global");
    public final static VkTess tessFont    = new VkTess("font");

    public final static int BUF_INCR  = 1024*1024;

    public int[]         rawBuffer = new int[BUF_INCR];
    public int[]         rawBufferIdx = new int[BUF_INCR];
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
    ReallocIntBuffer bufIntV;
    ReallocIntBuffer bufIntI;

    static class FrameLocalBuffers {
        private VkBuffer[] vbo;
        private VkBuffer[] vboIndices;
        private int vboIdxV;
        private int vboIdxI;
        private int offsetV;
        private int offsetI;
        final long BUFFER_SIZE = 32*1024*1024;
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
            }
        }
        public void upload(VkTess vkTess, ByteBuffer bufV, int sizeV, ByteBuffer bufI, int sizeI) {
            if (BUFFER_SIZE-offsetV < sizeV) {
                vboIdxV++;
                if (vboIdxV >= vbo.length) {
                    vboIdxV = 0;
                }
                offsetV = 0;
            }
            if (BUFFER_SIZE-offsetI < sizeI) {
                vboIdxI++;
                if (vboIdxI >= vboIndices.length) {
                    vboIdxI = 0;
                }
                offsetI = 0;
            }

            if (this.vbo[this.vboIdxV].getSize() <= sizeV) {
                System.out.println("Remake buffer with size "+BUFFER_SIZE+", req "+sizeV);
                this.vbo[this.vboIdxV].destroy();
                this.vbo[this.vboIdxV].create(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, BUFFER_SIZE, false);
            }
            if (this.vboIndices[this.vboIdxI].getSize() <= sizeI) {
                System.out.println("Remake buffer with size "+BUFFER_SIZE+", req "+sizeI);
                this.vboIndices[this.vboIdxI].destroy();
                this.vboIndices[this.vboIdxI].create(VK_BUFFER_USAGE_INDEX_BUFFER_BIT, BUFFER_SIZE, false);
            }
            vkTess.vertexOffset = offsetV;
            vkTess.indexOffset = offsetI;
            this.vbo[this.vboIdxV].upload(bufV, offsetV);
            this.vboIndices[this.vboIdxI].upload(bufI, offsetI);
            offsetV += Math.max(2048, GameMath.nextPowerOf2(sizeV));
            offsetI += Math.max(2048, GameMath.nextPowerOf2(sizeI));
        }
        public VkBuffer vbo() {
            return this.vbo[this.vboIdxV];
        }
        public VkBuffer vboIndices() {
            return this.vboIndices[this.vboIdxI];
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
                buffers[i] = new FrameLocalBuffers(this, context, 1<<0);
            }
        }
    }
    public VkTess(String s) {
        this(false, s);
    }
    public VkTess(boolean isSoftTesselator, String s) {
        this.isSoftTesselator = isSoftTesselator;
        if (!this.isSoftTesselator) {
            bufIntV = new ReallocIntBuffer();
            bufIntI = new ReallocIntBuffer();
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
    
    public void finish(int idxMode, int bufferMode, AbstractVkTesselatorState out) {
        if (isSoftTesselator()) {
            throw new IllegalStateException("Cannot draw soft tesselator");
        }
        this.idxCount = 0;
        if (vertexcount > 0) {
            int vIdx = getIdx(vertexcount);
            bufIntV.put(rawBuffer, 0, vIdx);
            this.idxCount = createIdxBuffer(idxMode);
            bufIntI.put(rawBufferIdx, 0, this.idxCount);
            
            if (out == this) {
                if (bufferMode != STREAM_UPLOAD) {
                    throw new GameLogicError("VKTess buffer is not meant for device local uploads");
                }
                int currentBuffer = VKContext.currentBuffer;
                FrameLocalBuffers buffer = buffers[currentBuffer];
                buffer.upload(this, bufIntV.getByteBuf(), vIdx*4, bufIntI.getByteBuf(), this.idxCount*4);
//                System.out.println("vertexOffset "+vertexOffset+", indexOffset "+indexOffset+", "+vIdx+","+idxCount);

//                int currentBuffer = VKContext.currentBuffer;
//                FrameLocalBuffers buffer = buffers[currentBuffer];
//                buffer.vboIdxV++;
//                if (buffer.vboIdxV >= buffer.vbo.length) {
//                    buffer.vboIdxV = 0;
//                }
//                buffer.vboIdxI++;
//                if (buffer.vboIdxI >= buffer.vboIndices.length) {
//                    buffer.vboIdxI = 0;
//                }
//                
//                this.vertexOffset = 0;
//                this.indexOffset = 0;
//                
//                upload(out.getVertexBuffer(), bufferMode, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, bufIntV.getByteBuf(), this.vertexOffset);
//                upload(out.getIndexBuffer(), bufferMode, VK_BUFFER_USAGE_INDEX_BUFFER_BIT, bufIntI.getByteBuf(), this.indexOffset);

            } else {
                this.vertexOffset = 0;
                this.indexOffset = 0;
                this.copyTo(out);
                upload(out.getVertexBuffer(), bufferMode, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, bufIntV.getByteBuf(), this.vertexOffset);
                upload(out.getIndexBuffer(), bufferMode, VK_BUFFER_USAGE_INDEX_BUFFER_BIT, bufIntI.getByteBuf(), this.indexOffset);
            }
        }
        resetState();
    }
    private int createIdxBuffer(int idxMode) {
        int pos = 0;
        if (idxMode == CREATE_QUAD_IDX_BUFFER) {
            if (this.vertexcount%4 != 0) {
                throw new IllegalStateException("Cannot make tri idx: vertexcount%4 != 0 ("+this.vertexcount+")");
            }
            int quads = this.vertexcount/4;
            for (int i = 0; i < quads; i++) {
                int vIdx = i*4;
                rawBufferIdx[pos++]=(vIdx+0);
                rawBufferIdx[pos++]=(vIdx+1);
                rawBufferIdx[pos++]=(vIdx+2);
                rawBufferIdx[pos++]=(vIdx+2);
                rawBufferIdx[pos++]=(vIdx+3);
                rawBufferIdx[pos++]=(vIdx+0);
            }
        } else if (idxMode == CREATE_PER_VERTEX_IDX_BUFFER) {
            for (int i = 0; i < this.vertexcount; i++) {
                rawBufferIdx[i]=i;
            }
            pos = vertexcount;
        } else {
            throw new GameLogicError("Mode not supported "+idxMode);
        }
        return pos;
    }

    private void upload(VkBuffer buffer, int bufferMode, int bufferType, ByteBuffer byteBuf, int bufferoffset) {
        if (buffer.getSize() < byteBuf.remaining()+1 || (bufferMode == DEVICE_LOCAL_UPLOAD) != buffer.isDeviceLocal()) {
            buffer.destroy();
            long size = Math.max(2048, GameMath.nextPowerOf2(byteBuf.remaining()));
            buffer.create(bufferType, size, (bufferMode == DEVICE_LOCAL_UPLOAD));
            System.out.println("realloc "+((bufferMode == DEVICE_LOCAL_UPLOAD)?"devicelocal":"stream")+" - "+size);
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
        this.bufIntV.release();
        this.bufIntI.release();
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

    @Override
    public void drawQuads() {
        finish(VkTess.CREATE_QUAD_IDX_BUFFER, STREAM_UPLOAD, this);
        bindAndDraw(Engine.getDrawCmdBuffer());
    }

    @Override
    public void drawTris() {
        finish(VkTess.CREATE_PER_VERTEX_IDX_BUFFER, STREAM_UPLOAD, this);
        bindAndDraw(Engine.getDrawCmdBuffer());
    }

}
