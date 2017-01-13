/**
 * 
 */
package nidefawl.qubes.gl;

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
    int[] heapBuffer = new int[0];
    int pos = 0;
    private int drawCount;
    private int stride;
    private GLVAO vao;

    /**
     * 
     */
    public MultiDrawIndirectBuffer() {
        reallocBuffer(4*1024*1024);
    }
    

    public void reallocBuffer(int intLen) {
        intLen *= 4;
        if (buffers == null || buffers.capacity() < intLen) {
            if (intLen*2 < 2*1024*1024) {
                intLen = intLen*2;
            }
            int[] newBuf = new int[intLen];
            System.arraycopy(heapBuffer, 0, newBuf, 0, heapBuffer.length);
            heapBuffer = newBuf;
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
        int pos = this.pos;
        int startpos = this.pos;
        final int[] heap = this.heapBuffer;
        heap[pos++] = elementcount;
        heap[pos++] = 1;
        heap[pos++] = 0;
        heap[pos++] = 0;
        heap[pos++] = 0;

        heap[pos++] = 0;
        
        heap[pos++] = 0;
        heap[pos++] = 0;
        pos += putLong(heap, pos, iVBO.addr);
        pos += putLong(heap, pos, iVBO.size);
        for (int i = 0; i < vao.list.size(); i++) {
            
            VertexAttrib attrib = vao.list.get(i);
            heap[pos++] = i;
            heap[pos++] = 0;
            long attrAddr = vVBO.addr + attrib.offset * 4;
            long attrSize = vVBO.size - attrib.offset * 4;
            pos += putLong(heap, pos, attrAddr);
            pos += putLong(heap, pos, attrSize);
        }
        this.pos = pos;
        this.stride = (pos-startpos)<<2;
        drawCount++;
    }
    

    private int putLong(int[] heap, int pos, long l) {
        heap[pos++] = (int) (l&0xFFFFFFFF);
        heap[pos++] = (int) (l>>>32);
        return 2;
    }
    
    public void preDraw(GLVAO bindlessVAO) {
        this.vao = bindlessVAO;
        pos = 0;
        intbuffers.clear();
        buffers.clear();
        drawCount = 0;
    }
    public int getDrawCount() {
        return this.drawCount;
    }
    public void render() {
        intbuffers.clear();
        intbuffers.put(heapBuffer, 0, pos);
        intbuffers.position(0).limit(pos);
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
