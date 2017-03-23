package nidefawl.qubes.render;

import static nidefawl.qubes.render.SkyRenderer.MAX_SPRITES;
import static nidefawl.qubes.render.SkyRenderer.tmp;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;

import nidefawl.qubes.gl.*;
import nidefawl.qubes.meshing.BlockFaceAttr;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.Half;
import nidefawl.qubes.util.IRenderComponent;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.WorldClient;

public abstract class SkyRenderer extends AbstractRenderer {
    protected final static int      MAX_SPRITES          = 1024 * 64;
    public final static int      SKYBOX_RES           = 256; //crappy, but works

    List<Cloud>           clouds               = Lists.newArrayList();
    final protected CubeMapCamera   cubeMatrix           = new CubeMapCamera();
    protected BlockFaceAttr         attr                 = new BlockFaceAttr();
    protected ReallocIntBuffer      vertexUploadDirectBuf;
    final static Vector3f tmp                  = new Vector3f();
    protected int                   storedSprites        = 0;
    protected int                   totalSpritesRendered = 0;
    protected VertexBuffer  vertexBuf;
    protected ByteBuffer    bufMat;
    protected FloatBuffer   bufMatFloat;
    protected int numTexturesCloud;
    protected static class Cloud {
        public int texture;

        List<PointSprite> sprites = Lists.newArrayList();

        Vector3f mot;
        Vector3f pos, lastPos, renderPos;
        public Cloud() {
            this.pos = new Vector3f();
            this.lastPos = new Vector3f();
            this.renderPos = new Vector3f();
            this.mot = new Vector3f();
        }
        public int store(FloatBuffer bufMatFloat) {
            for (PointSprite s : this.sprites) {
                tmp.set(this.renderPos);
                tmp.addVec(s.renderPos);
                tmp.store(bufMatFloat);
                bufMatFloat.put(s.renderSize);
                s.renderCol.store(bufMatFloat);
                bufMatFloat.put(s.renderRot);
            }
            return this.sprites.size();
        }
        public void update(float f) {
            Vector3f.interp(this.lastPos, this.pos, f, this.renderPos);

            for (PointSprite s : this.sprites) {
                s.update(f);
            }
        }
        public void tick() {
            
            this.lastPos.set(this.pos);
//          this.pos.addVec(this.mot);
            for (PointSprite s : this.sprites) {
                s.tick();
            }
        }
    }
    protected static class PointSprite {
        public float size, initSize, lastSize, renderSize;
        public float rotspeed;
        public float rot, lastRot, renderRot;
        public float xoffset;
        public float yoffset;
        Vector3f posOffset;
        public Vector3f col, lastCol, initCol, renderCol;
        private Vector3f renderPos;
        int tick = 0;
        public PointSprite() {
            this.renderPos = new Vector3f();
            this.posOffset = new Vector3f();
            this.col = new Vector3f();
            this.lastCol = new Vector3f();
            this.initCol = new Vector3f();
            this.renderCol = new Vector3f();
        }
        public void setSize(float size) {
            this.initSize = this.size = this.lastSize = this.renderSize = size;
        }
        public void setCol(float x, float y, float z) {
            this.col.set(x, y, z);
            this.lastCol.set(x, y, z);
            this.initCol.set(x, y, z);
            this.renderCol.set(x, y, z);
        }
        public void update(float f) {
            renderSize = lastSize+(size-lastSize)*f;
            renderRot = lastRot+(rot-lastRot)*f;
            Vector3f.interp(lastCol, col, f, renderCol);
            this.renderPos.set(this.posOffset);
//            {
//                float f2 = (tick+f+xoffset)/15520.0f;
//                f2 = (f2*GameMath.PI)%GameMath.PI*2;
//                posOffset.x += 0.0001f*GameMath.sin(f2);
//            }
//            {
//                float f2 = (tick+f+yoffset)/21220.0f;
//                f2 = (f2*GameMath.PI)%GameMath.PI*2;
//                posOffset.y += 0.0001f*GameMath.sin(f2);
//            }
            
        }

        public void tick() {
            lastSize = size;
            lastRot = rot;
            this.lastCol.set(this.col);
            float weatherStr = (1-WEATHER);
            weatherStr = GameMath.powf(weatherStr, 2.2f);
            this.col.x = this.initCol.x*(weatherStr);
            this.col.y = this.initCol.y*(weatherStr);
            this.col.z = this.initCol.z*(weatherStr);
            size = initSize*(WEATHER*0.5f+0.5f);
            rot += rotspeed;
            tick++;
        }
    }
    public static float WEATHER = 0.40f;
    public void increaseClouds() {
        WEATHER += 0.01f;
        if (WEATHER > 1)
            WEATHER = 1;
    }
    public void decreaseClouds() {
        WEATHER -= 0.01f;
        if (WEATHER < 0)
            WEATHER = 0;
    }

    protected void updateSpritesTick() {
        for (int i = 0; i < clouds.size(); i++) {
            Cloud sprite = clouds.get(i);
            sprite.tick();
        }
    }
    protected void updateSprites(float ftime) {
        for (int i = 0; i < clouds.size(); i++) {
            Cloud cloud = clouds.get(i);
            cloud.update(ftime);
        }
        
    }
    protected void storeSprites(float ftime, int n) {
        this.bufMatFloat.clear();
        storedSprites = 0;
        for (int i = 0; i < clouds.size(); i++) {
            Cloud cloud = clouds.get(i);
            if (cloud.texture == n) {
                storedSprites+=cloud.store(this.bufMatFloat);
            }
        }
        this.bufMatFloat.flip();
        this.bufMat.position(0).limit(this.bufMatFloat.limit()*4);
        uploadData();
//      System.out.println("totalSprites "+totalSprites);
        
    }
    
    public void redraw() {
        clouds.clear();
        Random r = new Random(4444);
        float l = 2.2f;
        float hl = 0.6f;
        float hu = 1.6f;
        float motRange = 0.05f;
        float rotRange = 0.0005f;
        float minBr=0.25f;
        float maxBr=1.0f;
        float sizeScale = 0.5f;
        float minSize = 33*sizeScale;
        float maxSize = 100*sizeScale;
        float l2 = 0.5f;
        float h2 = 0.2f;
        for (int i = 0; i < 12; i++) {
            Cloud cloud = new Cloud();
            cloud.texture = r.nextInt(this.numTexturesCloud);
            cloud.pos.x = r.nextFloat()*l*2.0f-l;
            cloud.pos.y = hl+(hu-hl)*r.nextFloat();
            cloud.pos.z = r.nextFloat()*l*2.0f-l;
            float fSize2 = 0.5f*(cloud.pos.y-hl)/(hu-hl);
//          cloud.mot.x = (r.nextFloat()*2.0f-1.0f)*motRange;
//          cloud.mot.y = 0;
//          cloud.mot.z = (r.nextFloat()*2.0f-1.0f)*motRange;
            for (int j = 0; j < 12; j++) {
                PointSprite sprite = new PointSprite();
                sprite.xoffset = r.nextFloat();
                sprite.yoffset = r.nextFloat();
                sprite.posOffset.x = r.nextFloat()*l2*2.0f-l2;
                sprite.posOffset.y = r.nextFloat()*h2*2.0f-h2;
                sprite.posOffset.z = r.nextFloat()*l2*2.0f-l2;
                float size = minSize+(r.nextFloat()+fSize2)*(maxSize-minSize);
//              sprite.setSize(size*0.5f+fSize2*(minSize));
              sprite.setSize(53);
                float f1 = (minBr+r.nextFloat()*(maxBr-minBr));
                float f2 = f1*0.9f+0.1f*(minBr+r.nextFloat()*(maxBr-minBr));
                sprite.setCol(f1, f1, f2);
                sprite.rot = sprite.lastRot = r.nextFloat()*0.43f;
                
                sprite.rotspeed = (r.nextFloat()*2.0f-1.0f)*rotRange;
                cloud.sprites.add(sprite);
            }
            clouds.add(cloud);
        }
        
    
        
        
    }
    protected void buildQuad(VertexBuffer vertexBuf) {
        vertexBuf.put(Half.fromFloat(0) << 16 | Half.fromFloat(1));
        vertexBuf.put(Half.fromFloat(1) << 16 | Half.fromFloat(1));
        vertexBuf.put(Half.fromFloat(1) << 16 | Half.fromFloat(0));
        vertexBuf.put(Half.fromFloat(0) << 16 | Half.fromFloat(0));
    }
    @Override
    public void init() {
        cubeMatrix.init();
        this.bufMat = Memory.createByteBufferAligned(64, 16*4*MAX_SPRITES);
        this.bufMatFloat = this.bufMat.asFloatBuffer();
        this.vertexUploadDirectBuf = new ReallocIntBuffer();
        this.vertexBuf = new VertexBuffer(1024*1024);
    }
    protected abstract void uploadData();
    public abstract void tickUpdate();
    public abstract void renderSky(World world, float fTime);
}
