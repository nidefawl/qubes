/**
 * 
 */
package nidefawl.qubes.gl;

import static org.lwjgl.opengl.NVVertexBufferUnifiedMemory.GL_VERTEX_ATTRIB_ARRAY_ADDRESS_NV;
import static org.lwjgl.opengl.NVVertexBufferUnifiedMemory.glBufferAddressRangeNV;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.NVBindlessMultiDrawIndirect;

import nidefawl.qubes.Game;
import nidefawl.qubes.gl.GLVAO.VertexAttrib;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class MultiDrawIndirectBuffer {

    ByteBuffer buffers;
    IntBuffer intbuffers;
    boolean inUse = true;
    private int drawCount;
    private int stride;
    private int vertexBufferCount;
    private GLVAO vao;
    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }
    public boolean isInUse() {
        return this.inUse;
    }
    
    /**
     * @param i
     */
    public MultiDrawIndirectBuffer(int i) {
        if (i > 0) {
            reallocBuffer(i);
        }
    }

    /**
     * 
     */
    public MultiDrawIndirectBuffer() {
    }

    public void reallocBuffer(int intLen) {
        intLen *= 4;
        if (buffers == null || buffers.capacity() < intLen) {
            if (intLen*2 < 2*1024*1024) {
                intLen = intLen*2;
            }
            if (buffers != null) {
                buffers = Memory.reallocByteBufferAligned(buffers, 64, intLen);
            } else {
                buffers = Memory.createByteBufferAligned(64, intLen);
            }
            intbuffers = buffers.asIntBuffer();
        }
    }
    public void release() {
        if (this.buffers != null) {
            Memory.free(this.buffers);
            this.buffers = null;
            this.intbuffers = null;
        }
    }

    /**
     * @param idx
     */
    public void put(int[] buffer) {
        put(buffer, 0, buffer.length);
    }

    /**
     * @param buffer
     * @param i
     * @param intLen
     */
    public void put(int[] buffer, int offset, int len) {
        reallocBuffer(offset+len);
        intbuffers.clear();
        intbuffers.put(buffer, offset, len);
        buffers.position(0).limit(len*4);
    }

    /**
     * @return
     */
    public ByteBuffer getByteBuf() {
        return this.buffers;
    }
    /*
      typedef struct {
          GLuint count;
          GLuint primCount;
          GLuint firstIndex;
          GLint  baseVertex;
          GLuint reservedMustBeZero;
        } DrawElementsIndirectCommand;
        typedef struct {
          GLuint   index;
          GLuint   reserved; 
          GLuint64 address;
          GLuint64 length;
        } BindlessPtrNV; 
    
        typedef struct {
          DrawElementsIndirectCommand cmd;
          GLuint                      reserved; 
          BindlessPtrNV               indexBuffer;
          BindlessPtrNV               vertexBuffers[];
        } DrawElementsIndirectBindlessCommandNV;
     */
    
    public void add(GLVBO vVBO, GLVBO iVBO, int elementcount) {
        int pos = this.intbuffers.position();
        this.intbuffers.put(elementcount);
        this.intbuffers.put(1);
        this.intbuffers.put(0);
        this.intbuffers.put(0);
        this.intbuffers.put(0);

        this.intbuffers.put(0);

        this.intbuffers.put(0);
        this.intbuffers.put(0);
        putLong(iVBO.addr);
        putLong(iVBO.size);
        for (int i = 0; i < vao.list.size(); i++) {
            
            VertexAttrib attrib = vao.list.get(i);
            this.intbuffers.put(i);
            this.intbuffers.put(0);
            long attrAddr = vVBO.addr + attrib.offset * 4;
            long attrSize = vVBO.size - attrib.offset * 4;
            putLong(attrAddr);
            putLong(attrSize);
        }

        this.stride = (this.intbuffers.position()-pos)<<2;
//        while (this.intbuffers.position()%64!=0) {
//            this.intbuffers.put(0);
//        }
//        System.out.println(this.intbuffers.position()+"/"+drawCount);

        drawCount++;
    }
    private void putLong(long l) {
        this.intbuffers.put((int) (l&0xFFFFFFFF));
        int msb = (int) (l>>>32);
//        System.out.println(msb);
        this.intbuffers.put(msb);
    }
    int[] emptyData = new int[1024*1024];
    public void reset(GLVAO bindlessVAO) {
        this.vao = bindlessVAO;
        put(emptyData, 0, emptyData.length);
        intbuffers.clear();
        buffers.clear();
        drawCount = 0;
    }
    public int getDrawCount() {
        return this.drawCount;
    }
    public void render() {
////
//        for (int i = 0; i < vao.list.size(); i++) {
//            VertexAttrib attrib = vao.list.get(i);
//            glBufferAddressRangeNV(GL_VERTEX_ATTRIB_ARRAY_ADDRESS_NV, i, 0 + attrib.offset*4, 0 - attrib.offset*4);
////            System.out.println(vbo.addr+"/"+vbo.size+" - "+attrib.offset);
//        }
        intbuffers.flip();
//        for (int i = 0; i < intbuffers.limit(); i++) {
//            System.out.println(intbuffers.get(i));
//        }
        buffers.position(0).limit(intbuffers.limit()<<2);
        Engine.bindVAO(this.vao, true);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("bindless Engine.bindVAO");
        Engine.enableBindless();
        Engine.checkGLError("bindless Engine.enableBindless");
//        stride = 6+6+6*this.vao.list.size();
//        stride<<=2;
        NVBindlessMultiDrawIndirect.glMultiDrawElementsIndirectBindlessNV(GL11.GL_TRIANGLES, GL11.GL_UNSIGNED_INT,
                buffers, drawCount, stride, this.vao.list.size());
        Engine.checkGLError("bindless NVBindlessMultiDrawIndirect.glMultiDrawElementsIndirectBindlessNV");
        Engine.disableBindless();
        Engine.checkGLError("bindless Engine.disableBindless");
//        GL11.glDrawElements(GL11.GL_TRIANGLES, this.elementCount[pass], GL11.GL_UNSIGNED_INT, 0);
    }
}
