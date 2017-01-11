package nidefawl.qubes.gl;

import org.lwjgl.opengl.*;

import com.google.common.collect.Lists;

import static org.lwjgl.opengl.NVVertexBufferUnifiedMemory.*;

import java.util.ArrayList;



public class GLVAO {
    private static int NEXT_VAO_ID = 0;
    private static GLVAO[] vaoList = new GLVAO[32];

    protected int vaoId;
    protected int vertStride;
    ArrayList<VertexAttrib> list = Lists.newArrayList();
    private boolean useBindless;
    protected int vaoIdBindless;

    public GLVAO() {
        vaoList[NEXT_VAO_ID++] = this;
    }
    public final static void initVAOs() {
        for (int i = 0; i < vaoList.length; i++) {
            if (vaoList[i] != null) {
                vaoList[i].init();
                vaoList[i].setup();
            }
        }
    }
    static class VertexAttrib {
        int     attribindex;
        int     size;
        int     type;
        boolean normalized;
        int     intLen;
        private boolean isFloat;
        public long offset;
        public VertexAttrib(boolean isFloat, int attribindex, int size, int type, boolean normalized, int intLen) {
            this.isFloat = isFloat;
            this.attribindex = attribindex;
            this.size = size;
            this.type = type;
            this.normalized = normalized;
            this.intLen = intLen;
        }
        public VertexAttrib(boolean isFloat, int attribindex, int size, int type, int intLen) {
            this.isFloat = isFloat;
            this.attribindex = attribindex;
            this.size = size;
            this.type = type;
            this.intLen = intLen;
        }
    }
    
    void vertexAttribFormat(int attribindex, int size, int type, boolean normalized, int intLen) {
        this.list.add(new VertexAttrib(true, attribindex, size, type, normalized, intLen));
    }
    void vertexAttribIFormat(int attribindex, int size, int type, int intLen) {
        this.list.add(new VertexAttrib(false, attribindex, size, type, intLen));
    }
    void init() {
        
    }
    void setup() {
        int vertStride = 0;
        for (int i = 0; i < this.list.size(); i++) {
            VertexAttrib attrib = list.get(i);
            attrib.offset = vertStride;
            vertStride += attrib.intLen;
        }
        this.vertStride = vertStride*4;
        this.vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(this.vaoId);
        for (int i = 0; i < this.list.size(); i++) {
            
            VertexAttrib attrib = list.get(i);
            if (attrib.isFloat) {
                GL43.glVertexAttribFormat(attrib.attribindex, attrib.size, attrib.type, attrib.normalized, (int) (attrib.offset*4));
            } else {
                GL43.glVertexAttribIFormat(attrib.attribindex, attrib.size, attrib.type, (int) (attrib.offset*4));
            }
            GL20.glEnableVertexAttribArray(attrib.attribindex);
            GL43.glVertexAttribBinding(attrib.attribindex, 0);
        }
        Engine.checkGLError("init vao");
        this.useBindless = GL.isBindlessSuppported();
        if (this.useBindless) {
            this.vaoIdBindless = GL30.glGenVertexArrays();
            GL30.glBindVertexArray(this.vaoIdBindless);
            for (int i = 0; i < this.list.size(); i++) {
                VertexAttrib attrib = list.get(i);
                if (attrib.isFloat) {
                    glVertexAttribFormatNV(attrib.attribindex, attrib.size, attrib.type, attrib.normalized, vertStride*4);
                } else {
                    glVertexAttribIFormatNV(attrib.attribindex, attrib.size, attrib.type, vertStride*4);
                }
                GL20.glEnableVertexAttribArray(attrib.attribindex);
            }
            Engine.checkGLError("init vao nv bindless");
        }
        GL30.glBindVertexArray(0);
    }

    public final static GLVAO vaoBlocksShadow = new GLVAO() {
        void init() {
            //POS
            vertexAttribFormat(0, 4, GL11.GL_FLOAT, false, 4);
        }
    };
    public final static GLVAO vaoBlocksShadowTextured = new GLVAO() {
        void init() {

            //POS
            vertexAttribFormat(0, 4, GL11.GL_FLOAT, false, 4);
            //TEXCOORD
            vertexAttribFormat(1, 2, GL30.GL_HALF_FLOAT, false, 1);
            //BLOCKINFO
            vertexAttribIFormat(2, 2, GL11.GL_UNSIGNED_SHORT, 1);


        }
    };
    public final static GLVAO vaoModel = new GLVAO() {
        void init() {
            

            //POS
            vertexAttribFormat(0, 3, GL11.GL_FLOAT, false, 3);
            //NORMAL
            vertexAttribFormat(1, 3, GL11.GL_BYTE, true, 1);
            //TEXCOORD
            vertexAttribFormat(2, 2, GL30.GL_HALF_FLOAT, false, 1);
            //COLOR
            vertexAttribFormat(3, 4, GL11.GL_UNSIGNED_BYTE, true, 1);


        }
    };
    public final static GLVAO vaoStaticModel = new GLVAO() {
        void init() {
            //POS
            vertexAttribFormat(0, 3, GL30.GL_HALF_FLOAT, false, 2);
            //NORMAL
            vertexAttribFormat(1, 3, GL11.GL_BYTE, true, 1);
            //TEXCOORD
            vertexAttribFormat(2, 2, GL30.GL_HALF_FLOAT, false, 1);
        }
    };
    public final static GLVAO openVRModel = new GLVAO() {
        void init() {
            //POS
            vertexAttribFormat(0, 3, GL11.GL_FLOAT, false, 3);
            //NORMAL
            vertexAttribFormat(1, 3, GL11.GL_FLOAT, true, 3);
            //TEXCOORD
            vertexAttribFormat(2, 2, GL11.GL_FLOAT, false, 2);

        }
    };
    public final static GLVAO vaoModelGPUSkinned= new GLVAO() {
        void init() {

            //POS
            vertexAttribFormat(0, 3, GL11.GL_FLOAT, false, 3);
            //NORMAL
            vertexAttribFormat(1, 3, GL11.GL_BYTE, true, 1);
            //TEXCOORD
            vertexAttribFormat(2, 2, GL30.GL_HALF_FLOAT, false, 1);
            //BONEIDX 0-3
            vertexAttribIFormat(3, 4, GL11.GL_UNSIGNED_BYTE, 1);
            //BONEIDX 4-7
            vertexAttribIFormat(4, 4, GL11.GL_UNSIGNED_BYTE, 1);
            //WEIGHTS 0-3
            vertexAttribFormat(5, 4, GL30.GL_HALF_FLOAT, false, 2);
            //WEIGHTS 4-7
            vertexAttribFormat(6, 4, GL30.GL_HALF_FLOAT, false, 2);


        }
    };
    public final static GLVAO vaoBlocks = new GLVAO() {
        void init() {

            //POS
            vertexAttribFormat(0, 3, GL11.GL_FLOAT, false, 3);
            //NORMAL
            vertexAttribFormat(1, 4, GL11.GL_BYTE, false, 1);//1 BYTE UNUSED (normal has 3 bytes)
            //TEXCOORD
            vertexAttribFormat(2, 2, GL30.GL_HALF_FLOAT, false, 1);
            //COLOR
            vertexAttribFormat(3, 4, GL11.GL_UNSIGNED_BYTE, true, 1);
            //BLOCKINFO
            vertexAttribIFormat(4, 4, GL11.GL_UNSIGNED_SHORT, 2);
            //LIGHTINFO
            vertexAttribIFormat(5, 2, GL11.GL_UNSIGNED_SHORT, 1);
        }
    };
    
    public final static GLVAO vaoBlocksBindless = new GLVAO() {
        void init() {

            //POS
            vertexAttribFormat(0, 3, GL11.GL_FLOAT, false, 3);
            //NORMAL
            vertexAttribFormat(1, 4, GL11.GL_BYTE, false, 1);//1 BYTE UNUSED (normal has 3 bytes)
            //TEXCOORD
            vertexAttribFormat(2, 2, GL30.GL_HALF_FLOAT, false, 1);
            //COLOR
            vertexAttribFormat(3, 4, GL11.GL_UNSIGNED_BYTE, true, 1);
            //BLOCKINFO
            vertexAttribIFormat(4, 4, GL11.GL_UNSIGNED_SHORT, 2);
            //LIGHTINFO
            vertexAttribIFormat(5, 2, GL11.GL_UNSIGNED_SHORT, 1);


        }
    };

    public final static GLVAO[] vaoTesselator = new GLVAO[16];
    static {
        for (int i = 0; i < 16; i++) {
            final boolean useNormalPtr = (i&1)!=0;
            final boolean useTexturePtr = (i&2)!=0;
            final boolean useColorPtr = (i&4)!=0;
            final boolean useUINTPtr = (i&8)!=0;
            vaoTesselator[i] = new GLVAO() {
                void init() {
                    //POS
                    vertexAttribFormat(0, 4, GL11.GL_FLOAT, false, 4);
                    
                    if (useNormalPtr) {
                        vertexAttribFormat(1, 3, GL11.GL_BYTE, false, 1);
                    }
                    if (useTexturePtr) {
                        vertexAttribFormat(2, 2, GL11.GL_FLOAT, false, 2);
                    }
                    if (useColorPtr) {
                        vertexAttribFormat(3, 4, GL11.GL_UNSIGNED_BYTE, true, 1);
                    }
                    if (useUINTPtr) {
                        vertexAttribIFormat(4, 4, GL11.GL_UNSIGNED_SHORT, 2);
                    }
                }
            };
        }
    }

    public boolean isBindless() {
        return this.useBindless;
    }
    
}
