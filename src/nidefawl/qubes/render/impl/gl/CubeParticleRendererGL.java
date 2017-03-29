package nidefawl.qubes.render.impl.gl;

import static org.lwjgl.opengl.GL13.GL_TEXTURE0;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.opengl.*;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.models.render.ModelConstants;
import nidefawl.qubes.particle.CubeParticle;
import nidefawl.qubes.render.CubeParticleRenderer;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.util.RenderUtil;
import nidefawl.qubes.world.World;

public class CubeParticleRendererGL extends CubeParticleRenderer {
    
    public final static ShaderBuffer        ssbo_particle_cubes        = new ShaderBuffer("ParticleCube_mat_model")
            .setSize(ModelConstants.SIZE_OF_MAT4*MAX_PARTICLES);
    public final static ShaderBuffer        ssbo_particle_cubes_blockinfo = new ShaderBuffer("ParticleCube_blockinfo")
            .setSize(ModelConstants.SIZE_OF_VEC4*2*MAX_PARTICLES);
    
    private boolean startup   = true;
    

    private Shader  particleShaderSeperateBuffer;
    private GLTriBuffer cubeFormat1;
    private GLTriBuffer cubeFormat2;




    

    public void init() {
        initShaders();
        cubeFormat1 = new GLTriBuffer(false);
        cubeFormat2 = new GLTriBuffer(false);
        VertexBuffer buf = new VertexBuffer(1024*1024);
        RenderUtil.makeCube(buf, 1.0f, GLVAO.vaoStaticModel);
        cubeFormat1.upload(buf);
        VertexBuffer buf2 = new VertexBuffer(1024*1024);
        RenderUtil.makeCube(buf2, 1.0f, GLVAO.vaoModel);
        cubeFormat2.upload(buf2);
    }
    public void initShaders() {
        try {
            pushCurrentShaders();
            AssetManager assetMgr = AssetManager.getInstance();

            Shader particleSeperateBuffer = assetMgr.loadShader(this, "particle/cube", new IShaderDef() {
                @Override
                public String getDefinition(String define) {
                    return null;
                }
            });
            popNewShaders();
            particleShaderSeperateBuffer = particleSeperateBuffer;
            particleShaderSeperateBuffer.enable();
            particleShaderSeperateBuffer.setProgramUniform1i("blockTextures", 0);
            particleShaderSeperateBuffer.setProgramUniform1i("noisetex", 1);
            particleShaderSeperateBuffer.setProgramUniform1i("normalTextures", 2);
            Shader.disable();
            startup = false;
        } catch (ShaderCompileError e) {
            releaseNewShaders();
            System.out.println("shader " + e.getName() + " failed to compile");
            System.out.println(e.getLog());
            if (startup) {
                throw e;
            } else {
                Game.instance.addDebugOnScreen("\0uff3333shader " + e.getName() + " failed to compile");
            }
        }
        startup = false;
    }

    public void renderParticles(World world, float iPass, float fTime) {
        if (!this.particles.isEmpty()) {
            storeParticles(world, iPass, fTime);
            Engine.checkGLError("storeparticles");
            particleShaderSeperateBuffer.enable();
            GL.bindTexture(GL_TEXTURE0, GL30.GL_TEXTURE_2D_ARRAY, TMgr.getBlocks());
            Engine.checkGLError("bind texture");
            GLVAO vao = selFormat == 0 ? GLVAO.vaoStaticModel : GLVAO.vaoModel;
            GLTriBuffer buffer = selFormat == 0 ? cubeFormat1 : cubeFormat2;
            Engine.bindVAO(vao, false);
            Engine.bindBuffer(buffer.getVbo());
            Engine.bindIndexBuffer(buffer.getVboIndices());
            GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, buffer.getTriCount()*3, GL11.GL_UNSIGNED_INT, 0, storedSprites);
            Engine.checkGLError("draw");
//            Engine.bindVAO(null);
        }
    }

    
    void storeParticles(World w, float iPass, float fTime) {
        ssbo_particle_cubes.nextFrame();
        ssbo_particle_cubes_blockinfo.nextFrame();
        IntBuffer bufBlockInfo = ssbo_particle_cubes_blockinfo.getIntBuffer();
        FloatBuffer bufModelMat = ssbo_particle_cubes.getFloatBuffer();
        storedSprites = 0;
        int offset=0;
        for (int i = 0; i < particles.size(); i++) {
            if (i >= MAX_PARTICLES) {
                System.err.println("too many particles");
                break;
            }
            CubeParticle cloud = particles.get(i);
            storedSprites+=cloud.store(offset, bufModelMat, bufBlockInfo);
            offset++;
        }
        ssbo_particle_cubes.update();
        ssbo_particle_cubes_blockinfo.update();
        
    }
    public void resize(int displayWidth, int displayHeight) {
    }

    @Override
    public void preinit() {
    }
    
}
