package nidefawl.game;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;

import java.nio.ByteBuffer;

import nidefawl.engine.*;
import nidefawl.engine.assets.AssetManager;
import nidefawl.engine.assets.AssetTexture;
import nidefawl.engine.font.FontRenderer;
import nidefawl.engine.shader.Shader;
import nidefawl.engine.texture.TextureManager;
import nidefawl.engine.util.GameMath;
import nidefawl.engine.vec.BlockPos;
import nidefawl.engine.vec.Vec3;
import nidefawl.game.entity.Entity;
import nidefawl.game.entity.PlayerSelf;
import nidefawl.game.gui.Gui;
import nidefawl.game.gui.GuiOverlay;
import nidefawl.game.input.Movement;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Vector3f;

public class Main extends GLGame {
    static int               initWidth  = 1024;
    static int               initHeight = 512;
    static {
        GLGame.appName = "Qubes";
    }
    static public final Main instance   = new Main();

    public static void main(String[] args) {
        instance.startGame();
    }

    boolean              limitFPS      = true;
    boolean              show      = true;
    long                 lastClickTime = System.currentTimeMillis() - 5000L;

    private GuiOverlay   debugOverlay;
    private Gui          gui;

    private Shader       waterShader;
    private Shader       waterShader2;
    private Shader       composite1;
    private Shader       composite2;
    private Shader       composite3;
    private Shader       compositeFinal;
    private Shader       depthBufShader;
    private Shader       testShader;
    private Shader       sky;
    private Shader       sky2;
    boolean              handleClick   = false;
    float                winX, winY;
    float sunAngle = 0.30F;
    Vector3f sun = new Vector3f(-0.31F, 0.95F, 0.00F);
    Vector3f up = new Vector3f(0, 100, 0);
    Vector3f skyColor = new Vector3f(0.43F, .69F, 1.F);
    Vector3f fogColor = new Vector3f(0.7F, 0.8F, 1F);
    Vector3f moonPosition = new Vector3f(-100, -300, -100);

     FontRenderer fontSmall;
    final PlayerSelf     entSelf       = new PlayerSelf(1);
    Movement             movement      = new Movement();
    private int noise;

    public Main() {
        super(20);
        GLGame.displayWidth = initWidth;
        GLGame.displayHeight = initHeight;
    }

    public void initGame() throws LWJGLException {
        super.initGame();
        this.debugOverlay = new GuiOverlay();
        this.debugOverlay.setPos(0, 0);
        this.debugOverlay.setSize(displayWidth, displayHeight);
        this.fontSmall = FontRenderer.get("Arial", 12, 1, 14);
        initShaders();
        genNoise();
        Engine.checkGLError("Post startup");
        glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }


    private void genNoise() {
        int w = 64;
        byte[] data = new byte[w*w*3];
        for (int x = 0; x < w; x++)
            for (int z = 0; z < w; z++)
                for (int y = 0; y < 3; y++) {
                    int seed = (GameMath.randomI(x*5)-79 + GameMath.randomI(y * 37)) * 1+GameMath.randomI((z-2) * 73);
                    data[(x*64+z)*3+y]=(byte) (GameMath.randomI(seed)%128);
                }
        if (this.noise == 0) {
            this.noise = glGenTextures();
        }
        glBindTexture(GL_TEXTURE_2D, this.noise);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR); 
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        ByteBuffer buf = BufferUtils.createByteBuffer(data.length);
        buf.put(data);
        buf.position(0).limit(data.length);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, w, w, 0, GL_RGB, GL_UNSIGNED_BYTE, buf);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void input(float fTime) {
        double mdX = Mouse.getDX();
        double mdY = Mouse.getDY();
        boolean b = Mouse.isGrabbed();

        // Break up our movement into components along the X, Y and Z axis
        while (Keyboard.next()) {
            int key = Keyboard.getEventKey();
            boolean isDown = Keyboard.getEventKeyState();
            switch (key) {
                case Keyboard.KEY_F8:
                    if (isDown) {
                        limitFPS = !limitFPS;
                    }
                    break;
                case Keyboard.KEY_F3:
                    if (isDown) {
                        show = !show;
                    }
                    break;
                case Keyboard.KEY_F5:
                    if (isDown) {
                        genNoise();
                    }
                    break;
                case Keyboard.KEY_F2:
                    if (isDown) {
                        Engine.setShadow();
                    }
                    break;
                case Keyboard.KEY_ESCAPE:
                    shutdown();
                    break;
            }
        }
        while (Mouse.next()) {
            int key = Mouse.getEventButton();
            boolean isDown = Mouse.getEventButtonState();
            long timeSinceLastClick = System.currentTimeMillis() - lastClickTime;
            switch (key) {
                case 0:
                    if (isDown) {
                        handleClick(Mouse.getEventX(), Mouse.getEventY());
                    }
                    break;
                case 1:
                    if (isDown ) {
                        setGrabbed(!b);
                        b = !b;
                    }
                    break;
            }
        }
        if (b != this.movement.grabbed()) {
            setGrabbed(b);
        }
        this.movement.update(mdX, mdY);
    }

    private void setGrabbed(boolean b) {
        this.movement.setGrabbed(b);
        Mouse.setCursorPosition(displayWidth / 2, displayHeight / 2);
        Mouse.setGrabbed(b);
    }

    private void handleClick(int eventX, int eventY) {
        this.handleClick = true;
        winX = (float) Mouse.getX();
        winY = (float) Mouse.getY();
    }

    private void handleDoubleClick(int eventX, int eventY) {
        float scaledX = (float) eventX / (float) displayWidth;
        float scaledY = (float) eventY / (float) displayHeight;
        float scale = 1200F;
        scaledX -= 0.5F;
        scaledY -= 0.5F;
        if (this.debugOverlay != null) {
            this.debugOverlay.setMessage("double click at " + scaledX + "/" + scaledY);
        }

    }

    private void initShaders() {

        if (this.waterShader != null)
            this.waterShader.release();
        this.waterShader = AssetManager.getInstance().loadShader("shaders/water");
        if (this.waterShader2 != null)
            this.waterShader2.release();
        this.waterShader2 = AssetManager.getInstance().loadShader("shaders/water2");
        if (this.depthBufShader != null)
            this.depthBufShader.release();
        this.depthBufShader = AssetManager.getInstance().loadShader("shaders/renderdepth");
        if (this.composite1 != null)
            this.composite1.release();
        this.composite1 = AssetManager.getInstance().loadShader("shaders/composite");
        if (this.composite2 != null)
            this.composite2.release();
        this.composite2 = AssetManager.getInstance().loadShader("shaders/composite1");
        if (this.composite3 != null)
            this.composite3.release();
        this.composite3 = AssetManager.getInstance().loadShader("shaders/composite2");
        if (this.compositeFinal != null)
            this.compositeFinal.release();
        this.compositeFinal = AssetManager.getInstance().loadShader("shaders/final");
        if (this.testShader != null)
            this.testShader.release();
        this.testShader = AssetManager.getInstance().loadShader("shaders/test");
        if (this.sky != null)
            this.sky.release();
        this.sky = AssetManager.getInstance().loadShader("shaders/sky");
        if (this.sky2 != null)
            this.sky2.release();
        this.sky2 = AssetManager.getInstance().loadShader("shaders/sky");
    }
    Vec3 mousePos = new Vec3();
    
    public void render(float fTime) {
        fogColor = new Vector3f(0.7F, 0.82F, 0.9999F);
        fogColor.scale(0.4F);
        skyColor = new Vector3f(0.43F, .69F, 1.F);
        skyColor.scale(0.4F);
        Vec3 vUnproject = null;
        Engine.fb.bind();
        Engine.checkGLError("drawDebug");

        //         glDisable(GL_CULL_FACE);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_CULL_FACE); // Cull back facing polygons
        //        glDisable(GL_CULL_FACE);
        clearFrameBuffer(Engine.fb);
        glActiveTexture(GL_TEXTURE0);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glLoadMatrix(Engine.getProjectionMatrix());
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glLoadMatrix(Engine.getViewMatrix());
        glDisable(GL_TEXTURE_2D);
        glDepthMask(false);
        glDisable(GL_ALPHA_TEST);
        Engine.setFog(fogColor, 1);
        glNormal3f(0.0F, -1.0F, 0.0F);
        glColor4f(1F, 1F, 1F, 1F);
        glFogi(GL_FOG_MODE, GL_LINEAR);
        glFogf(GL_FOG_START, 0);
        glFogf(GL_FOG_START, Engine.zfar/2F);
        glEnable(GL_FOG);
        glEnable(GL_COLOR_MATERIAL);
        glColorMaterial(GL_FRONT, GL_AMBIENT );
        glDisable(GL_BLEND);
        {
            sky.enable();
            drawSkybox();
            Shader.disable();
        }
        glDisable(GL_FOG);
        glDepthMask(true);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        //        Engine.enableLighting();
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glLoadMatrix(Engine.getModelViewMatrix());

        float w = 16F;
        float v = 1F;
        int num = 4;
        Tess.instance.setNormals(0, 1, 0);
        for (int x = -num; x <= num; x++)
            for (int z = -num; z <= num; z++) {
                Tess.instance.setOffset(x * w * 2, 0, z * w * 2);
                Tess.instance.add(-w, 0, w, 0, v);
                Tess.instance.add(w, 0, w, v, v);
                Tess.instance.add(w, 0, -w, v, 0);
                Tess.instance.add(-w, 0, -w, 0, 0);
            }

        this.waterShader2.enable();
        setUniforms(this.waterShader2);
        this.waterShader2.setProgramUniform1i("texture", 0);
        this.waterShader2.setProgramUniform1i("specular", 1);
        this.waterShader2.setProgramUniform1i("normals", 2);
        this.waterShader2.setProgramUniform1i("noisetex", 3);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, 0);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, 0);
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, 0);
        glActiveTexture(GL_TEXTURE3);
        glBindTexture(GL_TEXTURE_2D, this.noise);
        glActiveTexture(GL_TEXTURE0);
        Tess.instance.draw(GL_QUADS);
        {
            int x = 10;
            int y = 20;
            int z = 1;
            int tw = 22;
            int th = 22;
            Tess.instance.setNormals(0, 0, 1);
            Tess.instance.setColor(0xFFFFFF, 255);
            Tess.instance.add(x + tw, y + th, z, 1, 0);
            Tess.instance.add(x, y + th, z, 0, 0);
            Tess.instance.add(x, y, z, 0, 1);
            Tess.instance.add(x + tw, y, z, 1, 1);
        }
        Tess.instance.draw(GL_QUADS);
        Shader.disable();
        //            this.debugOverlay.setMessage(""+Engine.readDepth(0,0));
        if (this.handleClick) {
            this.handleClick = false;
            vUnproject = Engine.unproject(winX, winY);
        }
        Engine.fb.unbindCurrentFrameBuffer();
        glActiveTexture(GL_TEXTURE0);

        //                glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

        glPushMatrix();
        Engine.set2DMode(0, displayWidth, 0, displayHeight);
        glDisable(GL_LIGHTING);
        glDisable(GL_COLOR_MATERIAL);
        glDisable(GL_LIGHT0);
        glDisable(GL_LIGHT1);
        glDisable(GL_CULL_FACE);
        preDbgFB(true);
        drawDebug();
        postDbgFB();

        glEnable(GL_TEXTURE_2D);
        glDisable(GL_ALPHA_TEST);
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_ALWAYS);
        glDepthMask(false);
        glDisable(GL_LIGHTING);
        Tess.instance.dontReset();
        {
            int tw = displayWidth;
            int th = displayHeight;
            float x = 0;
            float y = 0;
            Tess.instance.add(x + tw, y + th, 0, 1, 0);
            Tess.instance.add(x, y + th, 0, 0, 0);
            Tess.instance.add(x, y, 0, 0, 1);
            Tess.instance.add(x + tw, y, 0, 1, 1);
        }

        Shader.disable();

        /*
         * 
        compositeShader.setProgramUniform1i("gcolor", 0);
        compositeShader.setProgramUniform1i("gdepth", 1);
        compositeShader.setProgramUniform1i("gnormal", 2);
        compositeShader.setProgramUniform1i("shadow", 1);
        compositeShader.setProgramUniform1i("composite", 3);
        compositeShader.setProgramUniform1i("gdepthtex", 5);
        compositeShader.setProgramUniform1i("noisetex", 4);
         */
        Engine.fbComposite0.bind();
        clearFrameBuffer(Engine.fbComposite0);
        Engine.checkGLError("bind fbo composite1");
        this.composite1.enable();
        Engine.checkGLError("enable shader composite1");
        setUniforms(this.composite1);
        glActiveTexture(GL_TEXTURE5);
        glBindTexture(GL_TEXTURE_2D, Engine.fb.getDepthTex());
        glActiveTexture(GL_TEXTURE4);
        glBindTexture(GL_TEXTURE_2D, this.noise);
        for (int i = 0; i < 4; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, Engine.fb.getTexture(i));
        }
        Tess.instance.draw(GL_QUADS);
        Engine.fbComposite0.unbindCurrentFrameBuffer();

        Shader.disable();
        preDbgFB(false);
        glActiveTexture(GL_TEXTURE0);
        for (int i = 0; i < 4; i++) {
            drawDbgTexture(0, 0, i, Engine.fb.getTexture(i), "TexUnit "+i);
        }
        drawDbgTexture(0, 0, 4, this.noise, "TexUnit "+4);
        for (int i = 0; i < 4; i++) {
            drawDbgTexture(0, 1, i, Engine.fbComposite0.getTexture(i), "ColAtt "+i);
        }
        postDbgFB();


        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, Engine.fbComposite0.getTexture(0));
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, 9987);
        GL30.glGenerateMipmap(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, 0);
        GL13.glActiveTexture(GL13.GL_TEXTURE3);
        glBindTexture(GL_TEXTURE_2D, Engine.fbComposite0.getTexture(3));
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, 9987);
        GL30.glGenerateMipmap(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, 0);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        Engine.fbComposite1.bind();
        clearFrameBuffer(Engine.fbComposite1);
        this.composite2.enable();
        Engine.checkGLError("enable shader composite2");
        setUniforms(this.composite2);
        glActiveTexture(GL_TEXTURE5);
        glBindTexture(GL_TEXTURE_2D, Engine.fb.getTexture(4));
        glActiveTexture(GL_TEXTURE4);
        glBindTexture(GL_TEXTURE_2D, this.noise);
        for (int i = 0; i < 4; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, Engine.fbComposite0.getTexture(i));
        }
        GL20.glDrawBuffers(GL30.GL_COLOR_ATTACHMENT2);
        Tess.instance.draw(GL_QUADS);
        for (int i = 0; i < 6; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, 0);
        }
        Engine.fbComposite1.unbindCurrentFrameBuffer();

        Shader.disable();
        preDbgFB(false);
        glActiveTexture(GL_TEXTURE0);
        for (int i = 0; i < 4; i++) {
            drawDbgTexture(1, 0, i, Engine.fbComposite0.getTexture(i), "TexUnit "+i);
        }
        drawDbgTexture(1, 1, 2, Engine.fbComposite1.getTexture(2), "ColAtt "+2);
        postDbgFB();

        GL13.glActiveTexture(GL13.GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, Engine.fbComposite1.getTexture(2));
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, 9987);
        GL30.glGenerateMipmap(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, 0);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        
        Engine.fbComposite2.bind();
        clearFrameBuffer(Engine.fbComposite2);
        this.composite3.enable();
        Engine.checkGLError("enable shader composite3");
        setUniforms(this.composite3);
        glActiveTexture(GL_TEXTURE5);
        glBindTexture(GL_TEXTURE_2D, Engine.fb.getTexture(4));
        glActiveTexture(GL_TEXTURE4);
        glBindTexture(GL_TEXTURE_2D, this.noise);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, Engine.fbComposite0.getTexture(0));
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, Engine.fbComposite0.getTexture(1));
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, Engine.fbComposite1.getTexture(2));
        glActiveTexture(GL_TEXTURE3);
        glBindTexture(GL_TEXTURE_2D, Engine.fbComposite0.getTexture(3));
        GL20.glDrawBuffers(GL30.GL_COLOR_ATTACHMENT0);
        Tess.instance.draw(GL_QUADS);
        for (int i = 0; i < 6; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, 0);
        }
        Engine.fbComposite2.unbindCurrentFrameBuffer();

        Shader.disable();
        preDbgFB(false);
        glActiveTexture(GL_TEXTURE0);
        drawDbgTexture(2, 0, 0, Engine.fbComposite0.getTexture(0), "TexUnit "+0);
        drawDbgTexture(2, 0, 1, Engine.fbComposite0.getTexture(1), "TexUnit "+1);
        drawDbgTexture(2, 0, 2, Engine.fbComposite1.getTexture(2), "TexUnit "+2);
        drawDbgTexture(2, 0, 3, Engine.fbComposite0.getTexture(3), "TexUnit "+3);
        drawDbgTexture(2, 1, 0, Engine.fbComposite2.getTexture(0), "ColAtt "+0);
        glActiveTexture(GL_TEXTURE0);
        drawDbgTexture(3, 0, 0, Engine.fbComposite2.getTexture(0), "TexUnit "+0);
        drawDbgTexture(3, 0, 1, Engine.fbComposite0.getTexture(1), "TexUnit "+1);
        drawDbgTexture(3, 0, 2, Engine.fbComposite1.getTexture(2), "TexUnit "+2);
        drawDbgTexture(3, 0, 3, Engine.fbComposite0.getTexture(3), "TexUnit "+3);
//        drawDbgTexture(2, 1, 0, Engine.fbComposite2.getTexture(0), "GL_TEXTURE"+0);
        postDbgFB();

        
        
        glDepthMask(true);
        glClearColor(0,0,0,0);
        glClear(GL_COLOR_BUFFER_BIT | GL_STENCIL_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        glEnable(GL_TEXTURE_2D);
        glDisable(GL_ALPHA_TEST);
        glDisable(GL_BLEND);
        glDisable(GL_DEPTH_TEST);
        glDepthFunc(GL_ALWAYS);
        glDepthMask(false);
//        
        this.compositeFinal.enable();
        Engine.checkGLError("enable shader compositeF");
        setUniforms(this.compositeFinal);
        glActiveTexture(GL_TEXTURE5);
        glBindTexture(GL_TEXTURE_2D, Engine.fb.getTexture(4));
        glActiveTexture(GL_TEXTURE4);
        glBindTexture(GL_TEXTURE_2D, this.noise);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, Engine.fbComposite2.getTexture(0));
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, Engine.fbComposite0.getTexture(1));
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, Engine.fbComposite1.getTexture(2));
        glActiveTexture(GL_TEXTURE3);
        glBindTexture(GL_TEXTURE_2D, Engine.fbComposite0.getTexture(3));
        Tess.instance.draw(GL_QUADS);
        Shader.disable();
        for (int i = 0; i < 7; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, 0);
        }
        glActiveTexture(GL_TEXTURE0);
        glEnable(GL_TEXTURE_2D);
//        glBindTexture(GL_TEXTURE_2D, Engine.fb.getTexture(0));
//        Tess.instance.draw(GL_QUADS);
        glEnable(GL_ALPHA_TEST);
        glEnable(GL_BLEND);
        
        if (show) {

            glBindTexture(GL_TEXTURE_2D, Engine.fbDbg.getTexture(0));
            glPushMatrix();
//            glScalef(0.5F, 0.5F, 0F);
//            glColor4f(1, 1, 1, 1);
            Tess.instance.draw(GL_QUADS);
            glPopMatrix();
        }
        Tess.instance.resetState();
        
        
        glDepthMask(true);
        glDisable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glEnable(GL_BLEND);

        if (vUnproject != null) {

            mousePos.set(vUnproject);
            BlockPos blockPos = mousePos.toBlock();
            int blockX = blockPos.x;
            int blockY = blockPos.y;
            int blockZ = blockPos.z;
            //            int i = this.world.getBiome(blockX, blockY, blockZ);
            //            int id = this.world.getTypeId(blockX, blockY, blockZ);
            String msg = "";
            msg += String.format("win:  %d %d\n", (int) winX, (int) winY);
            msg += String.format("Coordinate:  %d %d %d\n", blockX, blockY, blockZ);
            //            msg += String.format("Block:           %d\n", id);
            //            msg += String.format("Biome:          %s\n", BiomeGenBase.byId[i].biomeName);
            msg += String.format("Chunk:          %d/%d", blockX >> 4, blockZ >> 4);

            if (this.debugOverlay != null) {
                this.debugOverlay.setMessage(msg);
            }
        }
        glEnable(GL_ALPHA_TEST);
        glEnable(GL_TEXTURE_2D);

        if (this.debugOverlay != null) {
            this.debugOverlay.render(fTime);
        }

        Engine.set3DMode();
        glPopMatrix();
    }


    private void clearFrameBuffer(FrameBuffer fb) {
        fb.clear(0, 1.0F, 1.0F, 1.0F, 1.0F);
        fb.clear(1, 1.0F, 1.0F, 1.0F, 1.0F);
        fb.clear(2, 1.0F, 1.0F, 1.0F, 1.0F);
        fb.clear(3, 1.0F, 1.0F, 1.0F, 1.0F);
        fb.clearDepth();
        fb.setDrawAll();
    }

    private void preDbgFB(boolean clear) {
        Engine.fbDbg.bind();
        if (clear) {
            Engine.fbDbg.clear(0, 0, 0, 0, 0F);
            Engine.fbDbg.clearDepth();
            Engine.fbDbg.setDrawAll();
        }
        glPushAttrib(-1);
        Engine.checkGLError("fbDbg.bind");
        glMatrixMode(5888);
        glPushMatrix();
        glLoadIdentity();
        glMatrixMode(5889);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0.0D, displayWidth, displayHeight, 0.0D, 0.0D, 1.0D);
        Engine.checkGLError("fbDbg.ortho");
        glDisable(3008);
        glDepthFunc(519);
        glDepthMask(false);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_ALPHA_TEST);
        glDisable(GL_LIGHTING);
        glDisable(GL_LIGHT0);
        glDisable(GL_LIGHT1);
        //          glDisable(GL_CULL_FACE);
        glDisable(GL_COLOR_MATERIAL);
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glDisable(GL_ALPHA_TEST);
    }

    private void postDbgFB() {
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_ALPHA_TEST);
        glDepthFunc(GL_LEQUAL);
        glDepthMask(true);
        glMatrixMode(5889);
        Engine.checkGLError("fbDbg.glMatrixMode");
        glPopMatrix();
        Engine.checkGLError("fbDbg.glPopMatrix");
        glMatrixMode(5888);
        Engine.checkGLError("fbDbg.glMatrixMode");
        glPopMatrix();
        Engine.checkGLError("fbDbg.glPopMatrix");
        glPopAttrib();
        Engine.checkGLError("fbDbg.glPopAttrib");
        Engine.fbDbg.unbindCurrentFrameBuffer();
    }

    private void drawDbgTexture(int stage, int side, int num, int texture, String string) {
        float aspect = displayHeight / (float) displayWidth;
        int w1 = 120;
        int gap = 24;
        int wCol = w1 * 2 + gap;
        int hCol = displayHeight - 180;
        int xCol = 4;
        int yCol = 160;
        int h = (int) (w1 * 0.6);
        int gapy = 4;
        int b = 4;

        glPushMatrix();
        glTranslatef((wCol + gap)*stage, yCol+50, 0);
        w1-=5;
        glTranslatef(gap/2+(w1+gap/2)*side, 0, 0);
        glTranslatef(0, gapy+(gapy+h)*num, 0);
        glDisable(GL_ALPHA_TEST);
        glEnable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, texture);
        Tess.instance2.add(w1, 0 + h, 0, 1, 0);
        Tess.instance2.add(0, 0 + h, 0, 0, 0);
        Tess.instance2.add(0, 0, 0, 0, 1);
        Tess.instance2.add(w1, 0, 0, 1, 1);
        Tess.instance2.draw(7);
        glEnable(GL_ALPHA_TEST);
        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        Tess.instance2.setColorF(0, 0.5F);
        Tess.instance2.add(w1, 0 + h, 0, 1, 0);
        Tess.instance2.add(0, 0 + h, 0, 0, 0);
        Tess.instance2.add(0, 0 + h-20, 0, 0, 1);
        Tess.instance2.add(w1, 0 + h-20, 0, 1, 1);
        Tess.instance2.draw(7);
        glEnable(GL_TEXTURE_2D);
        fontSmall.drawString(string, 2, h-2, -1, true, 1.0F);
        fontSmall.drawString(""+texture+"", w1-2, h-2, -1, true, 1.0F, 1);
        glPopMatrix();
    }
    private void drawDebug() {
        String[] names = {
                "Composite0",
                "Composite1",
                "Composite2",
                "Final",
        };
            float aspect = displayHeight / (float) displayWidth;
            int w1 = 120;
            int gap = 24;
            int wCol = w1 * 2 + gap;
            int hCol = Math.min(450, displayHeight - 20);
            int xCol = 4;
            int yCol = 160;
            int h = (int) (w1 * 0.6);
            int gapy = 4;
            int b = 4;
            Tess.instance2.dontReset();
            Tess.instance2.add(wCol, yCol + hCol, 0);
            Tess.instance2.add(0, yCol + hCol, 0);
            Tess.instance2.add(0, yCol, 0);
            Tess.instance2.add(wCol, yCol, 0);
            glDisable(GL_TEXTURE_2D);
            glPushMatrix();
            for (int i = 0; i < names.length; i++) {
                glColor4f(.8F, .8F, .8F, 0.6F);
                Tess.instance2.draw(GL_QUADS);
                glTranslatef(wCol + gap, 0, 0);
            }
            glPopMatrix();
            Tess.instance2.resetState();
            Tess.instance2.dontReset();
            Tess.instance2.add(wCol - b, yCol + hCol - b, 0);
            Tess.instance2.add(b, yCol + hCol - b, 0);
            Tess.instance2.add(b, yCol + b, 0);
            Tess.instance2.add(wCol - b, yCol + b, 0);
            glPushMatrix();
            for (int i = 0; i < names.length; i++) {
                glColor4f(.4F, .4F, .4F, 0.8F);
                Tess.instance2.draw(GL_QUADS);
                glTranslatef(wCol + gap, 0, 0);
            }
            glPopMatrix();
            Tess.instance2.resetState();
            glColor4f(1F, 1F, 1F, 1.0F);
            glEnable(GL_ALPHA_TEST);
            glEnable(GL_BLEND);
            glEnable(GL_TEXTURE_2D);
            glPushMatrix();
            for (int i = 0; i < names.length; i++) {
                fontSmall.drawString(names[i], 8, yCol+20, -1, true, 1.0F);
                fontSmall.drawString("INPUT", 12, yCol+50, -1, true, 1.0F);
                fontSmall.drawString("OUTPUT", 8+w1+gap/2, yCol+50, -1, true, 1.0F);
                glTranslatef(wCol + gap, 0, 0);
            }
            glPopMatrix();
    }

    private void setUniforms(Shader compositeShader) {
        compositeShader.setProgramUniform1f("near", Engine.znear);
        compositeShader.setProgramUniform1f("far", Engine.zfar);
        compositeShader.setProgramUniform1f("viewWidth", displayWidth);
        compositeShader.setProgramUniform1f("viewHeight", displayHeight);
        compositeShader.setProgramUniform1f("rainStrength", 0F);
        compositeShader.setProgramUniform1f("wetness", 0);
        compositeShader.setProgramUniform1f("aspectRatio", displayWidth / (float) displayHeight);
        compositeShader.setProgramUniform1f("sunAngle", sunAngle);
        compositeShader.setProgramUniform1f("frameTimeCounter", (renderTime + ticksran)/20F);
        compositeShader.setProgramUniform3f("cameraPosition", Engine.camera.getPosition());
        compositeShader.setProgramUniform3f("upPosition", this.up);
        compositeShader.setProgramUniform3f("sunPosition", this.sun);
        compositeShader.setProgramUniform3f("moonPosition", this.moonPosition);
        compositeShader.setProgramUniform3f("skyColor", this.skyColor);
        compositeShader.setProgramUniform1i("isEyeInWater", 0);
        compositeShader.setProgramUniform1i("heldBlockLightValue", 0);
        compositeShader.setProgramUniform1i("worldTime", ticksran%24000);
        compositeShader.setProgramUniform1i("gcolor", 0);
        compositeShader.setProgramUniform1i("gdepth", 1);
        compositeShader.setProgramUniform1i("gnormal", 2);
        compositeShader.setProgramUniform1i("shadow", 1);
        compositeShader.setProgramUniform1i("composite", 3);
        compositeShader.setProgramUniform1i("gdepthtex", 5);
        compositeShader.setProgramUniform1i("noisetex", 4);
        compositeShader.setProgramUniform1i("eyeAltitude", 4);
        compositeShader.setProgramUniform1i("fogMode", 1);
        compositeShader.setProgramUniform2i("eyeBrightness", 0, 0);
        compositeShader.setProgramUniform2i("eyeBrightnessSmooth", 0, 0);
        compositeShader.setProgramUniformMatrix4ARB("gbufferModelView", false, Engine.getModelViewMatrix(), false);
        compositeShader.setProgramUniformMatrix4ARB("gbufferModelViewInverse", false, Engine.getModelViewMatrixInv(), false);
        compositeShader.setProgramUniformMatrix4ARB("gbufferPreviousModelView", false, Engine.getModelViewMatrixPrev(), false);
        compositeShader.setProgramUniformMatrix4ARB("gbufferProjection", false, Engine.getProjectionMatrix(), false);
        compositeShader.setProgramUniformMatrix4ARB("gbufferProjectionInverse", false, Engine.getProjectionMatrixInv(), false);
        compositeShader.setProgramUniformMatrix4ARB("gbufferPreviousProjection", false, Engine.getProjectionMatrixPrev(), false);
        
        compositeShader.setProgramUniformMatrix4ARB("shadowModelView", false, Engine.getShadowModelViewMatrix(), false);
        compositeShader.setProgramUniformMatrix4ARB("shadowModelViewInverse", false, Engine.getShadowModelViewMatrixInv(), false);
        compositeShader.setProgramUniformMatrix4ARB("shadowProjection", false, Engine.getShadowProjectionMatrix(), false);
        compositeShader.setProgramUniformMatrix4ARB("shadowProjectionInverse", false, Engine.getShadowProjectionMatrixInv(), false);
    }

    private void drawSkybox() {
        int scale = (int) (Engine.zfar/2F);
        int x = -scale;
        int y = -scale/16;
        int z = -scale;
        int x2 = scale;
        int y2 = scale/16;
        int z2 = scale;
        Tess.instance.resetState();
        int rgbai = ((int)(fogColor.x*255))<<16|((int)(fogColor.y*255))<<8|((int)(fogColor.z*255));
        Tess.instance.setColor(rgbai, 255);
        Tess.instance.add(x, y2, z);
        Tess.instance.add(x, y, z);
        Tess.instance.add(x2, y2, z);
        Tess.instance.add(x2, y, z);
        Tess.instance.add(x2, y2, z2);
        Tess.instance.add(x2, y, z2);
        Tess.instance.add(x, y2, z2);
        Tess.instance.add(x, y, z2);
        Tess.instance.add(x, y2, z);
        Tess.instance.add(x, y, z);
        Tess.instance.draw(GL_QUAD_STRIP);
        rgbai = ((int)(skyColor.x*255))<<16|((int)(skyColor.y*255))<<8|((int)(skyColor.z*255));
        Tess.instance.setColor(rgbai, 255);
        Tess.instance.add(x, y, z2);
        Tess.instance.add(x2, y, z2);
        Tess.instance.add(x2, y, z);
        Tess.instance.add(x, y, z);
        Tess.instance.add(x, y2, z);
        Tess.instance.add(x2, y2, z);
        Tess.instance.add(x2, y2, z2);
        Tess.instance.add(x, y2, z2);
        Tess.instance.draw(GL_QUADS);
    }

    long lastShaderLoadTime = 0L;
    @Override
    public void onStatsUpdated() {
        if (this.debugOverlay != null) {
            this.debugOverlay.update();
        }
        if (System.currentTimeMillis()-lastShaderLoadTime > 1000 && Keyboard.isKeyDown(Keyboard.KEY_F9)) {
            lastShaderLoadTime = System.currentTimeMillis();
            initShaders();
        }
    }

    @Override
    public void preRenderUpdate(float f) {
        if (limitFPS) {
            limitFpsTo(20);
        }
        this.entSelf.updateInputDirect(movement);
        Engine.camera.set(this.entSelf, f);
//        this.camera.
    }

    @Override
    public void onResize() {
        if (this.debugOverlay != null) {
            this.debugOverlay.setSize(displayWidth, displayHeight);
            this.debugOverlay.setPos(0, 0);
        }
    }
    @Override
    public void tick() {
        this.entSelf.tickUpdate();
    }
}
