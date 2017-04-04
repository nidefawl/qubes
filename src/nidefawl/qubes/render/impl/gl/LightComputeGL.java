package nidefawl.qubes.render.impl.gl;

import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL15.GL_READ_ONLY;
import static org.lwjgl.opengl.GL15.GL_WRITE_ONLY;
import static org.lwjgl.opengl.GL30.GL_RGBA16F;
import static org.lwjgl.opengl.GL42.glBindImageTexture;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.opengl.*;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.GL;
import nidefawl.qubes.gl.GLDebugTextures;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.render.LightCompute;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TMgr;
import nidefawl.qubes.util.Stats;
import nidefawl.qubes.world.WorldClient;

public class LightComputeGL extends LightCompute {

    public final static ShaderBuffer        ssbo_lights = new ShaderBuffer("PointLightStorageBuffer").setSize(Engine.MAX_LIGHTS * SIZE_OF_STRUCT_LIGHT);
    public Shader       shaderComputerLight;

    private boolean     startup        = true;

    @Override
    public void preinit() {
        
    }
    public void initShaders() {
        try {
            pushCurrentShaders();
            AssetManager assetMgr = AssetManager.getInstance();
            Shader new_shaderCSLight = assetMgr.loadShader(this, "post/light");
            popNewShaders();
            this.shaderComputerLight = new_shaderCSLight;
            //
            this.shaderComputerLight.enable();
            this.shaderComputerLight.setProgramUniform1i("depthBuffer", 1);
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

    public LightComputeGL() {
    }

    public void init() {
        initShaders();
        GL15.glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
    }

    public void resize(int displayWidth, int displayHeight) {
        super.resize(displayWidth, displayHeight);
        GL.deleteTexture(this.lightTilesTex);
        this.lightTilesTex = GL.genStorage(displayWidth, displayHeight, GL_RGBA16F, GL_LINEAR, GL12.GL_CLAMP_TO_EDGE);
        Engine.checkGLError("lightTilesTex");
//        Engine.debugOutput.update();
//        ssbo_lights.update();
    }

    public void updateLights(WorldClient world, float fTime) {
        ssbo_lights.nextFrame();
        FloatBuffer lightBuf = ssbo_lights.getFloatBuffer();
        updateAndStoreLights(world, fTime, lightBuf);
        ssbo_lights.update();
    }
    public void render(WorldClient world, float fTime, int pass) {

        if (this.numLights > 0) {
            shaderComputerLight.enable();
            glBindImageTexture(0, Engine.getSceneFB().getTexture(1), 0, false, 0, GL_READ_ONLY, GL_RGBA16F);

            glBindImageTexture(5, this.lightTilesTex, 0, false, 0, GL_WRITE_ONLY, GL_RGBA16F);
            GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, Engine.getSceneFB().getDepthTex());
            shaderComputerLight.setProgramUniform1i("numActiveLights", this.numLights);
            GL43.glDispatchCompute(this.lightTiles[0], this.lightTiles[1], 1);
            
            GL42.glMemoryBarrier(GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
            GL42.glMemoryBarrier(GL42.GL_ALL_BARRIER_BITS);
//          int ret = ARBSync.glClientWaitSync(sync, ARBSync.GL_SYNC_FLUSH_COMMANDS_BIT, 1000000);
//          if (ret == ARBSync.GL_WAIT_FAILED || ret == ARBSync.GL_TIMEOUT_EXPIRED) {
//              System.err.println("sync failed with "+ret);
//          }
            
        }
        ssbo_lights.sync();
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("compute light 5");
        if (GLDebugTextures.isShow()) {
            GLDebugTextures.readTexture(false, "compute_light_"+pass, "input", Engine.getSceneFB().getTexture(0));
            if (this.numLights > 0) {
                GLDebugTextures.readTexture(true, "compute_light_"+pass, "output", this.getTexture(), 0);    
            } else {
                GLDebugTextures.readTexture(true, "compute_light_"+pass, "unlit", this.getTexture(), 4);
            }
            
        }
    }

    public void renderDebug() {
        try {
            Engine.debugOutput.bind();
            ByteBuffer buf = Engine.debugOutput.map(false);
            
            float[] debugVals = new float[16];
            FloatBuffer fbuf = buf.asFloatBuffer();
            fbuf.get(debugVals);
            IntBuffer ibuf = buf.asIntBuffer();
            ibuf.position(16);
            if (this.debugResults == null || this.debugResults.length != ibuf.remaining()) {
                this.debugResults = new int[ibuf.remaining()]; 
            }
            ibuf.get(this.debugResults);
            Engine.debugOutput.unmap();
            Engine.debugOutput.unbind();
            if(Stats.fpsCounter==0) {
                System.out.println("minDepth "+String.format("%10f", debugVals[0]));
                System.out.println("maxDepth "+String.format("%10f", debugVals[1]));
            }

            Tess t = Tess.instance;
            float chunkWPx = 32.0f;
            float screenX = 0;
            float screenZ = 0;
            float border = 1;
            Engine.setBlend(true);
            for (int x = 0; x < this.lightTiles[0]; x++) {
                for (int y = 0; y < this.lightTiles[1]; y++) {
                    int idx = y * this.lightTiles[0] + x;
                    t.setOffset((screenX + chunkWPx * x), (screenZ + chunkWPx * y), 0);

                    int n = this.debugResults[idx];
                    if (n <= 0) {
                        t.setColorF(0xff0000, 0.3f);
                    } else /*if (!region.isRenderable) {
                           t.setColorF(0x555500, 1);
                           } else*/
                    {
                        int r = 0x777777;
                        //                    if (Math.abs(region.rX) % 2 == Math.abs(region.rZ) % 2)
                        //                        r = 0;
                        //
                        t.setColorF(r, 0.5f);
                    }
                    //                t.add(0, chunkWPx);
                    //                t.add(chunkWPx, chunkWPx);
                    //                t.add(chunkWPx, 0);
                    //                t.add(0, 0);
                    //                if (c == null) {
                    //                    t.setColorF(0x993333, 1);
                    //                } else if (!c.isValid) {
                    //                    t.setColorF(0x999933, 1);
                    //                } else if (worldChunkX == icX && worldChunkZ == icZ) {
                    //                    t.setColorF(0x333399, 1);
                    //                } else {
                    //                    t.setColorF(0x339933, 1);
                    //                }
                    t.add(0, chunkWPx);
                    t.add(border, chunkWPx);
                    t.add(border, 0);
                    t.add(0, 0);
                    t.add(chunkWPx, border);
                    t.add(chunkWPx, 0);
                    t.add(0, 0);
                    t.add(0, border);

                    //                this.numTiles[y*this.lightTiles[0]+x]= ibuf.get(y*this.lightTiles[0]+x);
                    //                System.out.printf("%d,%d = %d lights\n", x, y, n);
                }

            }
            Shaders.colored.enable();
            t.drawQuads();
            Shaders.textured.enable();
            FontRenderer font = FontRenderer.get(0, 12, 1);
            for (int x = 0; x < this.lightTiles[0]; x++) {
                for (int y = 0; y < this.lightTiles[1]; y++) {
                    int idx = y * this.lightTiles[0] + x;
                    int n = this.debugResults[idx];
                    float screenY = (screenZ + chunkWPx * y);
                    font.drawString("" + n, screenX + chunkWPx * x + chunkWPx / 3, screenY + font.getLineHeight() / 2.0f + chunkWPx / 2.0f, -1, true, 0.7f);
                }
            }
            Engine.setBlend(false);
            Shader.disable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getTexture() {
        return this.numLights > 0 ? lightTilesTex : TMgr.getEmpty();
    }
}
