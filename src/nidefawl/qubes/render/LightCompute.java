package nidefawl.qubes.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.GL_RGBA16F;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT;
import static org.lwjgl.opengl.GL42.glBindImageTexture;
import static org.lwjgl.opengl.GL42.glMemoryBarrier;
import static org.lwjgl.opengl.GL43.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL43;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.lighting.DynamicLight;
import nidefawl.qubes.shader.*;
import nidefawl.qubes.texture.TextureManager;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.world.WorldClient;

public class LightCompute extends AbstractRenderer {
    public Shader       shaderComputerLight;
    private int[]       lightTiles;
    int                 maxLights      = 1024;
    int                 lightFloatSize = 11;
    ShaderBuffer        lights         = new ShaderBuffer("PointLightStorageBuffer").setSize(maxLights * lightFloatSize * 4);
    ShaderBuffer        debugOutput         = new ShaderBuffer("DebugOutputBuffer").setSize(4*4);
    private int         lightTilesTex;
    private boolean     startup        = true;

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

    public LightCompute() {
    }

    public void init() {
        initShaders();

        lights.setup();
        debugOutput.setup();
        debugOutput.bind();
        ByteBuffer buf = debugOutput.map(false);
        System.out.println(buf);
        int test = buf.asIntBuffer().get(0);
        System.out.println("debug result "+test);
        debugOutput.unmap();
        debugOutput.unbind();
    }

    public void resize(int displayWidth, int displayHeight) {
        int groupsX = (int) Math.ceil(Game.displayWidth / 32.0f);
        int groupsY = (int) Math.ceil(Game.displayHeight / 32.0f);
        this.lightTiles = new int[] { groupsX, groupsY };
        if (this.lightTilesTex != 0) {
            TextureManager.getInstance().releaseTexture(this.lightTilesTex);
            this.lightTilesTex = 0;
        }
        this.lightTilesTex = glGenTextures();
        glActiveTexture(GL_TEXTURE0);
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, this.lightTilesTex);
        GL.glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA16F, groupsX, groupsY);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, 0);
        Engine.checkGLError("lightTilesTex");
    }

    public void render(WorldClient world, float fTime) {
        FrameBuffer.unbindFramebuffer();

        ArrayList<DynamicLight> lights = world.lights;
        FloatBuffer lightBuf = this.lights.getFloatBuffer();
        lightBuf.clear();
        int a = 0;
        for (; a < lights.size() && a < this.maxLights; a++) {
            DynamicLight light = lights.get(a);
            float constant = 1.0f;
            float linear = 4f;
            float quadratic = 2f;
            float lightThreshold = 0.1f;
            float maxBrightness = Math.max(Math.max(light.color.x, light.color.y), light.color.z);
            float lightL = (float) (linear * linear - 4 * quadratic * (constant - (256.0 / lightThreshold) * 1));
            float radius = (-linear + GameMath.sqrtf(lightL)) / (2 * quadratic);
            light.color.store(lightBuf);//3
            lightBuf.put((float) light.intensity);//4
            lightBuf.put(constant);//5
            lightBuf.put(linear);//6
            lightBuf.put(quadratic);//7
            light.loc.store(lightBuf);//8
            lightBuf.put(radius);//11
            //            lightPos[a].set(light.loc.x-Engine.GLOBAL_OFFSET.x, light.loc.y-Engine.GLOBAL_OFFSET.y, light.loc.z-Engine.GLOBAL_OFFSET.z);
            //            lightColors[a].set(1,1,1);
            //            lightLin[a].set(linear);
            //            lightExp[a].set(quadratic);
            //            lightSize[a].set(radius);
        }
        lightBuf.flip();
        this.lights.update();
        shaderComputerLight.enable();
        IntBuffer intBuf = this.debugOutput.getIntBuffer();
        intBuf.clear();
        intBuf.put(0);
        intBuf.put(0);
        intBuf.put(0);
        intBuf.put(0);
        intBuf.flip();
        
        this.debugOutput.update();
        glBindImageTexture(0, Engine.getSceneFB().getTexture(1), 0, false, 0, GL_READ_ONLY, GL_RGBA16F);

        glBindImageTexture(5, this.lightTilesTex, 0, false, 0, GL_WRITE_ONLY, GL_RGBA16F);
        GL.bindTexture(GL_TEXTURE1, GL_TEXTURE_2D, Engine.getSceneFB().getDepthTex());
        shaderComputerLight.setProgramUniformMatrix4("viewMatrix", false, Engine.getMatSceneV().get(), false);
        shaderComputerLight.setProgramUniformMatrix4("projectionMatrix", false, Engine.getMatSceneP().get(), false);
        shaderComputerLight.setProgramUniform1i("numActiveLights", lights.size());
        GL43.glDispatchCompute(this.lightTiles[0], this.lightTiles[1], 1);
        glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
        if (Game.GL_ERROR_CHECKS)
            Engine.checkGLError("compute light 5");
        debugOutput.bind();
        ByteBuffer buf = debugOutput.map(false);
        IntBuffer bufIntResult = buf.asIntBuffer();
        int pointLightCount = bufIntResult.get(0);
        int maxLightIndex = bufIntResult.get(1);
        int numActiveLights = bufIntResult.get(2);
        debugOutput.unmap();
        debugOutput.unbind();
        System.out.println("pointLightCount "+pointLightCount);
        System.out.println("maxLightIndex "+maxLightIndex);
        System.out.println("numActiveLights "+numActiveLights);
        if (Game.show) {
            GLDebugTextures.readTexture("compute_light", "input", Engine.getSceneFB().getTexture(0));
            GLDebugTextures.readTexture("compute_light", "output", this.lightTilesTex);
        }
    }
}
