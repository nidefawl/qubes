package nidefawl.qubes.render;

import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.entity.PlayerSelf;
import nidefawl.qubes.particle.AbstractParticleRenderer;
import nidefawl.qubes.particle.CubeParticle;
import nidefawl.qubes.texture.array.TextureArrays;
import nidefawl.qubes.util.Color;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.WorldClient;

public abstract class CubeParticleRenderer extends AbstractParticleRenderer {
    public final static int MAX_PARTICLES       = 1024*16;
    protected int                fireUpdate;
    protected float              lastUpdate;
    protected boolean            pause                = false;
    protected float              pauseTime            = 0;
    protected int                selFormat;

    protected int                maxSprites           = 16 * 1024;
    protected int                totalSpritesRendered = 0;
    protected int                storedSprites        = 0;
    protected int                tick                 = 0;
    protected List<CubeParticle> particles            = Lists.newArrayList();
    protected Random             r                    = new Random(4444);

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
            p.setTex(r.nextInt(TextureArrays.blockTextureArray.totalSlots));
            particles.add(p);
        }
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
    public void init() {
    }

    @Override
    public void resize(int displayWidth, int displayHeight) {
    }

}
