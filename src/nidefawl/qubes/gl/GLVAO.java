package nidefawl.qubes.gl;

import org.lwjgl.opengl.*;

import nidefawl.qubes.Game;

public class GLVAO {
    private static int NEXT_VAO_ID = 0;
    private static GLVAO[] vaoList = new GLVAO[32];

    protected int stride;
    protected int vaoId;

    public GLVAO() {
        vaoList[NEXT_VAO_ID++] = this;
    }
    public final static void initVAOs() {
        for (int i = 0; i < vaoList.length && vaoList[i] != null; i++) {
            vaoList[i].init();
        }
    }
    void init() {
    }
    public final static GLVAO vaoBlocksShadow = new GLVAO() {
        void init() {
            this.vaoId = GL30.glGenVertexArrays();
            GL30.glBindVertexArray(vaoId);
            //POS
            GL43.glVertexAttribFormat(0, 4, GL11.GL_FLOAT, false, 0);
            int offset = 4;
            this.stride = offset * 4;
            for (int i = 0; i < 1; i++) {
                GL20.glEnableVertexAttribArray(i);
                GL43.glVertexAttribBinding(i, 0);
            }
            GL30.glBindVertexArray(0);
            Engine.checkGLError("init vao");
        }
    };
    public final static GLVAO vaoBlocksShadowTextured = new GLVAO() {
        void init() {
            this.vaoId = GL30.glGenVertexArrays();
            GL30.glBindVertexArray(vaoId);
            //POS
            GL43.glVertexAttribFormat(0, 4, GL11.GL_FLOAT, false, 0);
            int offset = 4;
            //TEXCOORD
            GL43.glVertexAttribFormat(1, 2, GL30.GL_HALF_FLOAT, false, offset * 4);
            offset += 1;
            //BLOCKINFO
            GL43.glVertexAttribIFormat(2, 2, GL11.GL_UNSIGNED_SHORT, offset * 4);
            offset += 1;
            this.stride = offset * 4;
            for (int i = 0; i < 3; i++) {
                GL20.glEnableVertexAttribArray(i);
                GL43.glVertexAttribBinding(i, 0);
            }
            GL30.glBindVertexArray(0);
            Engine.checkGLError("init vao");
        }
    };
    public final static GLVAO vaoModel = new GLVAO() {
        void init() {
            
            this.vaoId = GL30.glGenVertexArrays();
            GL30.glBindVertexArray(vaoId);
            //POS
            GL43.glVertexAttribFormat(0, 3, GL11.GL_FLOAT, false, 0);
            int offset = 3;
            //NORMAL
            GL43.glVertexAttribFormat(1, 3, GL11.GL_BYTE, false, offset * 4);
            offset += 1;
            //TEXCOORD
            GL43.glVertexAttribFormat(2, 2, GL30.GL_HALF_FLOAT, false, offset * 4);
            offset += 1;
            //COLOR
            GL43.glVertexAttribFormat(3, 4, GL11.GL_UNSIGNED_BYTE, true, offset * 4);
            offset += 1;
            this.stride = offset * 4;
            for (int i = 0; i < 4; i++) {
                GL20.glEnableVertexAttribArray(i);
                GL43.glVertexAttribBinding(i, 0);
            }
            GL30.glBindVertexArray(0);
            Engine.checkGLError("init vao");
        }
    };
    public final static GLVAO vaoModelGPUSkinned= new GLVAO() {
        void init() {
            this.vaoId = GL30.glGenVertexArrays();
            GL30.glBindVertexArray(vaoId);
            //POS
            GL43.glVertexAttribFormat(0, 3, GL11.GL_FLOAT, false, 0);
            int offset = 3;
            //NORMAL
            GL43.glVertexAttribFormat(1, 3, GL11.GL_BYTE, false, offset * 4);
            offset += 1;
            //TEXCOORD
            GL43.glVertexAttribFormat(2, 2, GL30.GL_HALF_FLOAT, false, offset * 4);
            offset += 1;
            //BONEIDX 0-3
            GL43.glVertexAttribIFormat(3, 4, GL11.GL_UNSIGNED_BYTE, offset * 4);
            offset += 1;
            //BONEIDX 4-7
            GL43.glVertexAttribIFormat(4, 4, GL11.GL_UNSIGNED_BYTE, offset * 4);
            offset += 1;
            //WEIGHTS 0-3
            GL43.glVertexAttribFormat(5, 4, GL30.GL_HALF_FLOAT, false, offset * 4);
            offset += 2;
            //WEIGHTS 4-7
            GL43.glVertexAttribFormat(6, 4, GL30.GL_HALF_FLOAT, false, offset * 4);
            offset += 2;
            this.stride = offset * 4;
            for (int i = 0; i < 7; i++) {
                GL20.glEnableVertexAttribArray(i);
                GL43.glVertexAttribBinding(i, 0);
            }
            GL30.glBindVertexArray(0);
            Engine.checkGLError("init vao");
        }
    };
    public final static GLVAO vaoBlocks = new GLVAO() {
        void init() {
            this.vaoId = GL30.glGenVertexArrays();
            GL30.glBindVertexArray(vaoId);
            //POS
            GL43.glVertexAttribFormat(0, 3, GL11.GL_FLOAT, false, 0);
            int offset = 3;
            //NORMAL
            GL43.glVertexAttribFormat(1, 4, GL11.GL_BYTE, false, offset * 4);//1 BYTE UNUSED (normal has 3 bytes)
            offset += 1; 
            //TEXCOORD
            GL43.glVertexAttribFormat(2, 2, GL30.GL_HALF_FLOAT, false, offset * 4);
            offset += 1; 
            //COLOR
            GL43.glVertexAttribFormat(3, 4, GL11.GL_UNSIGNED_BYTE, true, offset * 4);
            offset += 1; 
            //BLOCKINFO
            GL43.glVertexAttribIFormat(4, 4, GL11.GL_UNSIGNED_SHORT, offset * 4);
            offset += 2;
            //LIGHTINFO
            GL43.glVertexAttribIFormat(5, 2, GL11.GL_UNSIGNED_SHORT, offset * 4);
            offset += 1;
            this.stride = offset * 4;
            for (int i = 0; i < 6; i++) {
                GL20.glEnableVertexAttribArray(i);
                GL43.glVertexAttribBinding(i, 0);
            }
            GL30.glBindVertexArray(0);
            Engine.checkGLError("init vao");
        }
    };
    public final static GLVAO[] vaoTesselator = new GLVAO[16];
    static {
        for (int i = 0; i < 16; i++) {
            final int tessSetting = i;
            vaoTesselator[i] = new GLVAO() {
                void init() {
                    this.vaoId = GL30.glGenVertexArrays();
                    GL30.glBindVertexArray(vaoId);
                    //POS
                    GL43.glVertexAttribFormat(0, 4, GL11.GL_FLOAT, false, 0);
                    GL20.glEnableVertexAttribArray(0);
                    GL43.glVertexAttribBinding(0, 0);
                    int offset = 4;
                    boolean useNormalPtr = (tessSetting&1)!=0;
                    boolean useTexturePtr = (tessSetting&2)!=0;
                    boolean useColorPtr = (tessSetting&4)!=0;
                    boolean useUINTPtr = (tessSetting&8)!=0;
                    if (useNormalPtr) {
                        GL43.glVertexAttribFormat(1, 3, GL11.GL_BYTE, false, offset*4);
                        offset += 1;
                        GL20.glEnableVertexAttribArray(1);
                        GL43.glVertexAttribBinding(1, 0);
                    }
                    if (useTexturePtr) {
                        GL43.glVertexAttribFormat(2, 2, GL11.GL_FLOAT, false, offset*4);
                        offset += 2;
                        GL20.glEnableVertexAttribArray(2);
                        GL43.glVertexAttribBinding(2, 0);
                    }
                    if (useColorPtr) {
                        GL43.glVertexAttribFormat(3, 4, GL11.GL_UNSIGNED_BYTE, true, offset*4);
                        offset += 1;
                        GL20.glEnableVertexAttribArray(3);
                        GL43.glVertexAttribBinding(3, 0);
                    }
                    if (useUINTPtr) {
                        GL43.glVertexAttribIFormat(4, 4, GL11.GL_UNSIGNED_SHORT, offset*4);
                        offset += 2;
                        GL20.glEnableVertexAttribArray(4);
                        GL43.glVertexAttribBinding(4, 0);
                    }
                    this.stride = offset * 4;
                    GL30.glBindVertexArray(0);
                    Engine.checkGLError("init vao");
                }
            };
        }
    }
    
}
