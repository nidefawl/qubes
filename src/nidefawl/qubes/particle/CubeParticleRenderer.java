package nidefawl.qubes.particle;

import static org.lwjgl.opengl.GL13.GL_TEXTURE0;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Random;

import org.lwjgl.opengl.*;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.entity.PlayerSelf;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.models.render.ModelConstants;
import nidefawl.qubes.perf.TimingHelper;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.texture.array.BlockTextureArray;
import nidefawl.qubes.util.Color;
import nidefawl.qubes.util.RenderUtil;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.WorldClient;

public class CubeParticleRenderer extends AbstractParticleRenderer {
    public final static int MAX_PARTICLES       = 1024*16;
    
    public final static ShaderBuffer        ssbo_particle_cubes        = new ShaderBuffer("ParticleCube_mat_model")
            .setSize(ModelConstants.SIZE_OF_MAT4*MAX_PARTICLES);
    public final static ShaderBuffer        ssbo_particle_cubes_blockinfo = new ShaderBuffer("ParticleCube_blockinfo")
            .setSize(ModelConstants.SIZE_OF_VEC4*2*MAX_PARTICLES);
    
    private boolean startup   = true;
    private int     fireUpdate;
    private float   lastUpdate;
    boolean         pause     = false;
    float           pauseTime = 0;
    

    private Shader  particleShaderSeperateBuffer;
    private GLTriBuffer cubeFormat1;
    private GLTriBuffer cubeFormat2;
    private int selFormat;

    

    private int maxSprites=16*1024;
    int totalSpritesRendered = 0;
    int storedSprites = 0;
    int tick = 0;
    List<CubeParticle> particles = Lists.newArrayList();


    Random r = new Random(4444);

    

    public void init() {
        initShaders();
        cubeFormat1 = new GLTriBuffer(GL15.GL_STREAM_DRAW);
        cubeFormat2 = new GLTriBuffer(GL15.GL_STREAM_DRAW);
        VertexBuffer buf = new VertexBuffer(1024*1024);
        RenderUtil.makeCube(buf, 1.0f, GLVAO.vaoStaticModel);
        int data = cubeFormat1.upload(buf);
        System.out.println("uploaded "+(data*4)+" bytes for format 1");
        VertexBuffer buf2 = new VertexBuffer(1024*1024);
        RenderUtil.makeCube(buf2, 1.0f, GLVAO.vaoModel);
        int data2 = cubeFormat2.upload(buf2);
        System.out.println("uploaded "+(data2*4)+" bytes for format 2");
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
//            System.out.println("render "+this.particles.size());
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

    public void spawnParticles(WorldClient world, int x, int y, int z, int type, int arg) {
        if (particles.size()>=MAX_PARTICLES)
            return;
        Block b = Block.get(arg);
//        System.out.println("spawn "+type+","+arg+", "+x+","+y+","+z);
        if (b != null) {
            int data = world.getData(x, y, z);
            int face = r.nextInt(1);
            int tex = b.getTexture(face, data, 0);
            int color = b.getFaceColor(world, x, y, z, face, 0);
            if (tex == 0) {
                System.err.println("no texture for block "+b+" and face "+face);
                return;
            }
            float maxVelXZ = 0.1f;
            float minVelY = -0.06f;
            float maxVelY = 0.0f;
//           minVelY = 1.8f;
//           maxVelY = 15.3f;
            float rotRange = 0.03f;
            int num = type == 1 ? 16 : 4;
            for (int i = 0; i < num; i++) {
                int texture = b.getTexture(r.nextInt(6), 0, 0);
                CubeParticle p = new CubeParticle();
                Color.setColorVec(color, p.color);
                int size = r.nextInt(3);
                p.setSize(2F/8f+size/8F);
                float scX, scY, scZ;
                float scRange = 0.5f;
                scX = -1.0f+2.0f*r.nextFloat();
                scY = -1.0f+2.0f*r.nextFloat();
                scZ = -1.0f+2.0f*r.nextFloat();
                p.setPos(x+0.5f+scX*scRange, y+0.5f+scY*scRange, z+0.5f+scZ*scRange);
                float mx = (float) (-maxVelXZ+2.0*maxVelXZ*r.nextFloat());
                float mz = (float) (-maxVelXZ+2.0*maxVelXZ*r.nextFloat());
                float my = (float) (minVelY+(maxVelY-minVelY)*r.nextFloat()*r.nextFloat());
                p.setMotion(mx, my, mz);
                int toffx = r.nextInt(8);
                int toffz = r.nextInt(8);
                
                p.setTextureOffset(toffx/8F, toffz/8F);
                p.setRot(r.nextFloat(), r.nextFloat(), r.nextFloat());
                p.setRotSpeed(r.nextFloat()*rotRange, r.nextFloat()*rotRange, r.nextFloat()*rotRange);
                p.setTex(texture);
                particles.add(p);
            }
        }
    }
    public void spawnParticles(int n) {
        if (particles.size()>=MAX_PARTICLES)
            return;
        float maxVelXZ = 1.3f;
        float minVelY = 0.3f;
        float maxVelY = 3.3f;
//       minVelY = 1.8f;
//       maxVelY = 15.3f;
        float rotRange = 0.03f;
        for (int i = 0; i < n; i++) {
            CubeParticle p = new CubeParticle();
            int size = r.nextInt(4);
            p.setSize(1F/4f+size/4F);
            p.setPos(55, -32, 0);
            PlayerSelf player = Game.instance.getPlayer();
            if (player != null) {
                p.setPos((float)player.pos.x, (float)player.pos.y, (float)player.pos.z);
            }
            float mx = (float) (-maxVelXZ+2.0*maxVelXZ*r.nextFloat());
            float mz = (float) (-maxVelXZ+2.0*maxVelXZ*r.nextFloat());
            float my = (float) (minVelY+(maxVelY-minVelY)*r.nextFloat()*r.nextFloat());
            p.setMotion(mx, my, mz);
            
            int toffx = r.nextInt(8);
            int toffz = r.nextInt(8);
            
            p.setTextureOffset(toffx/8F, toffz/8F);
            p.setRot(r.nextFloat(), r.nextFloat(), r.nextFloat());
            p.setRotSpeed(r.nextFloat()*rotRange, r.nextFloat()*rotRange, r.nextFloat()*rotRange);
            p.setTex(r.nextInt(BlockTextureArray.getInstance().totalSlots));
            particles.add(p);
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

    public void tickUpdate(World world) {
        if (!pause) {
//            TimingHelper.startSilent(51);
            for (int i = 0; i < particles.size(); i++) {
                CubeParticle sprite = particles.get(i);
                
                sprite.tickUpdate(world);
                if (sprite.dead) {
                    particles.remove(i--);
                }
            }
            
//            long l1 = TimingHelper.stopSilent(51);
//            if (particles.size()>1000)
//            System.out.println(l1);
//          if (this.particles.isEmpty()) {
//              spawnParticles(1);
//          }
//            for (int a = 0; a < Math.max(1, Math.min(130, maxSprites/100)); a++)
//            if (r.nextInt(120)>1&&r.nextInt(maxSprites)>storedSprites) {
//                spawnParticles(1+r.nextInt(23));
//            }
        } else if (fireUpdate>0) {
            fireUpdate=0;
            this.preRenderUpdate(null, pauseTime);
        }
    
    }

    public void resize(int displayWidth, int displayHeight) {
    }
    public void preRenderUpdate(World world, float f) {
        for (int i = 0; i < particles.size(); i++) {
            CubeParticle cloud = particles.get(i);
            cloud.update(f);
        }
        lastUpdate = f;
    }
    public int getNumParticles() {
        return this.particles.size();
    }
    @Override
    public void preinit() {
    }
    
}
