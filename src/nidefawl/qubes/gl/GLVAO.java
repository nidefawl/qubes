package nidefawl.qubes.gl;

import org.lwjgl.opengl.*;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.util.GameLogicError;
import nidefawl.qubes.vulkan.VkVertexDescriptors;

import static org.lwjgl.opengl.NVVertexBufferUnifiedMemory.*;

import java.util.ArrayList;



public class GLVAO {
    private static int NEXT_VAO_ID = 0;
    private static GLVAO[] vaoList = new GLVAO[32];

    protected int vaoId;
    protected int vertStride;
    public ArrayList<VertexAttrib> list = Lists.newArrayList();
    private boolean useBindless;
    protected int vaoIdBindless;
    public int idx;
    private VkVertexDescriptors vkVertexDesc;

    public int getVertStride() {
        return this.vertStride;
    }
    public VkVertexDescriptors getVkVertexDesc() {
        return this.vkVertexDesc;
    }
    public GLVAO() {
        this.idx = NEXT_VAO_ID;
        vaoList[NEXT_VAO_ID++] = this;
    }
    public final static void initVAOs(boolean isVulkan) {
        for (int i = 0; i < vaoList.length; i++) {
            if (vaoList[i] != null) {
                vaoList[i].init();
                if (isVulkan) {
                    vaoList[i].setupVulkan();
                } else {
                    vaoList[i].setupGL();
                }
                
            }
        }
        if (isVulkan&&Game.GL_ERROR_CHECKS)
            Engine.checkGLError("init vertex formats");
    }
    public static void destroy() {
        for (int i = 0; i < vaoList.length; i++) {
            if (vaoList[i] != null) {
                vaoList[i].destroyVertexFormat();
            }
        }
    }
    private void destroyVertexFormat() {
        if (Engine.isVulkan) {
            this.vkVertexDesc.destroy();
        }
    }
    public static class VertexAttrib {
        public int     attribindex;
        public int     size;
        public int     type;
        public boolean normalized;
        public int     intLen;
        public boolean isFloat;
        public long offset;
        public String name;
        
        public VertexAttrib(String name, boolean isFloat, int attribindex, int size, int type, boolean normalized, int intLen) {
            this.name = name;
            this.isFloat = isFloat;
            this.attribindex = attribindex;
            this.size = size;
            this.type = type;
            this.normalized = normalized;
            this.intLen = intLen;
        }
        public VertexAttrib(String name, boolean isFloat, int attribindex, int size, int type, int intLen) {
            this.name = name;
            this.isFloat = isFloat;
            this.attribindex = attribindex;
            this.size = size;
            this.type = type;
            this.intLen = intLen;
        }
    }
    
    void vertexAttribFormat(String string, int attribindex, int size, int type, boolean normalized, int intLen) {
        this.list.add(new VertexAttrib(string, true, attribindex, size, type, normalized, intLen));
    }
    void vertexAttribIFormat(String string, int attribindex, int size, int type, int intLen) {
        this.list.add(new VertexAttrib(string, false, attribindex, size, type, intLen));
    }
    void init() {
        
    }
    private void setupVulkan() {
        this.vkVertexDesc = new VkVertexDescriptors(this.idx, this.list);
        
    }
    private void setupGL() {
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
        this.useBindless = makeBindless()&&GL.isBindlessSuppported();
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

    protected boolean makeBindless() {
        return true;
    }

    public final static GLVAO vaoBlocksShadow = new GLVAO() {
        void init() {
            //POS
            vertexAttribFormat("in_position", 0, 4, GL11.GL_FLOAT, false, 4);
        }
    };
    public final static GLVAO vaoBlocksShadowTextured = new GLVAO() {
        void init() {

            //POS
            vertexAttribFormat("in_position", 0, 4, GL11.GL_FLOAT, false, 4);
            //TEXCOORD
            vertexAttribFormat("in_texcoord", 1, 2, GL30.GL_HALF_FLOAT, false, 1);
            //BLOCKINFO
            vertexAttribIFormat("in_blockinfo", 2, 2, GL11.GL_UNSIGNED_SHORT, 1);


        }
    };
    public final static GLVAO vaoEmpty = new GLVAO() {
        void init() {
        }

        protected boolean makeBindless() {
            return false;
        }
    };
    public final static GLVAO vaoModel = new GLVAO() {
        void init() {
            

            //POS
            vertexAttribFormat("in_position", 0, 3, GL11.GL_FLOAT, false, 3);
            //NORMAL
            vertexAttribFormat("in_normal", 1, 3, GL11.GL_BYTE, true, 1);
            //TEXCOORD
            vertexAttribFormat("in_texcoord", 2, 2, GL30.GL_HALF_FLOAT, false, 1);
            //COLOR
            vertexAttribFormat("in_color", 3, 4, GL11.GL_UNSIGNED_BYTE, true, 1);


        }
    };
    public final static GLVAO vaoStaticModel = new GLVAO() {
        void init() {
            //POS
            vertexAttribFormat("in_position", 0, 3, GL30.GL_HALF_FLOAT, false, 2);
            //NORMAL
            vertexAttribFormat("in_normal", 1, 3, GL11.GL_BYTE, true, 1);
            //TEXCOORD
            vertexAttribFormat("in_texcoord", 2, 2, GL30.GL_HALF_FLOAT, false, 1);
        }
    };
    public final static GLVAO openVRModel = new GLVAO() {
        void init() {
            //POS
            vertexAttribFormat("in_position", 0, 3, GL11.GL_FLOAT, false, 3);
            //NORMAL
            vertexAttribFormat("in_normal", 1, 3, GL11.GL_FLOAT, true, 3);
            //TEXCOORD
            vertexAttribFormat("in_texcoord", 2, 2, GL11.GL_FLOAT, false, 2);

        }
    };
    public final static GLVAO vaoModelGPUSkinned= new GLVAO() {
        void init() {

            //POS
            vertexAttribFormat("in_position", 0, 3, GL11.GL_FLOAT, false, 3);
            //NORMAL
            vertexAttribFormat("in_normal", 1, 3, GL11.GL_BYTE, true, 1);
            //TEXCOORD
            vertexAttribFormat("in_texcoord", 2, 2, GL30.GL_HALF_FLOAT, false, 1);
            //BONEIDX 0-3
            vertexAttribIFormat("in_bones1", 3, 4, GL11.GL_UNSIGNED_BYTE, 1);
            //BONEIDX 4-7
            vertexAttribIFormat("in_bones2", 4, 4, GL11.GL_UNSIGNED_BYTE, 1);
            //WEIGHTS 0-3
            vertexAttribFormat("in_weights1", 5, 4, GL30.GL_HALF_FLOAT, false, 2);
            //WEIGHTS 4-7
            vertexAttribFormat("in_weights2", 6, 4, GL30.GL_HALF_FLOAT, false, 2);


        }
    };
    public final static GLVAO vaoBlocks = new GLVAO() {
        void init() {

            //POS
            vertexAttribFormat("in_position", 0, 3, GL11.GL_FLOAT, false, 3);
            //NORMAL
            vertexAttribFormat("in_normal", 1, 4, GL11.GL_BYTE, false, 1);//1 BYTE UNUSED (normal has 3 bytes)
            //TEXCOORD
            vertexAttribFormat("in_texcoord", 2, 2, GL30.GL_HALF_FLOAT, false, 1);
            //COLOR
            vertexAttribFormat("in_color", 3, 4, GL11.GL_UNSIGNED_BYTE, true, 1);
            //BLOCKINFO
            vertexAttribIFormat("in_blockinfo", 4, 4, GL11.GL_UNSIGNED_SHORT, 2);
            //LIGHTINFO
            vertexAttribIFormat("in_light", 5, 2, GL11.GL_UNSIGNED_SHORT, 1);
        }
    };
    
    public final static GLVAO vaoBlocksBindless = new GLVAO() {
        void init() {

            //POS
            vertexAttribFormat("in_position", 0, 3, GL11.GL_FLOAT, false, 3);
            //NORMAL
            vertexAttribFormat("in_normal", 1, 4, GL11.GL_BYTE, false, 1);//1 BYTE UNUSED (normal has 3 bytes)
            //TEXCOORD
            vertexAttribFormat("in_texcoord", 2, 2, GL30.GL_HALF_FLOAT, false, 1);
            //COLOR
            vertexAttribFormat("in_color", 3, 4, GL11.GL_UNSIGNED_BYTE, true, 1);
            //BLOCKINFO
            vertexAttribIFormat("in_blockinfo", 4, 4, GL11.GL_UNSIGNED_SHORT, 2);
            //LIGHTINFO
            vertexAttribIFormat("in_light", 5, 2, GL11.GL_UNSIGNED_SHORT, 1);


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
                    vertexAttribFormat("in_position", 0, 4, GL11.GL_FLOAT, false, 4);
                    
                    if (useNormalPtr) {
                        vertexAttribFormat("in_normal", 1, 3, GL11.GL_BYTE, false, 1);
                    }
                    if (useTexturePtr) {
                        vertexAttribFormat("in_texcoord", 2, 2, GL11.GL_FLOAT, false, 2);
                    }
                    if (useColorPtr) {
                        vertexAttribFormat("in_color", 3, 4, GL11.GL_UNSIGNED_BYTE, true, 1);
                    }
                    if (useUINTPtr) {
                        vertexAttribIFormat("in_blockinfo", 4, 4, GL11.GL_UNSIGNED_SHORT, 2);
                    }
                }
            };
        }
    }

    public boolean isBindless() {
        return this.useBindless;
    }
    
}
