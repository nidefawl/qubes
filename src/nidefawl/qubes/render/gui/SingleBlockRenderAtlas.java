package nidefawl.qubes.render.gui;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Maps;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.item.StackData;
import nidefawl.qubes.render.RenderFramebufferCached;

public class SingleBlockRenderAtlas {

    static SingleBlockRenderAtlas instance = new SingleBlockRenderAtlas();
    static final int cols = 16;
    static final int tileSize = 128;
    static final int slots = cols*cols;
    static final int texSize = tileSize*cols;
    static byte[] defaultData = new byte[texSize*texSize*4];
    static {
        for (int x = 0; x < texSize; x++)
            for (int y = 0; y < texSize; y++) {
                int idx = (x+y*texSize)*4;
                defaultData[idx++] = 0;
                defaultData[idx++] = 0;
                defaultData[idx++] = 0;
                defaultData[idx++] = 0;
            }
    }
    public static class TextureAtlas {
//        int glId;
        int[] hashes = new int[slots];
        int idx = 0;
//        public FrameBuffer frameBuffer;
//        public nidefawl.qubes.vulkan.FrameBuffer frameBufferVk;
        public RenderFramebufferCached renderBuffer;
        public boolean needsSetup = true;
        public TextureAtlas(int idx) {
            this.idx = idx;
            Arrays.fill(this.hashes, -1);
            
//            this.glId = TextureManager.getInstance().makeNewTexture(defaultData, tileSize, tileSize, false, true, -1); 
        }
        public int getTextureIdx(int hash) {
            int free = -1;
            for (int i = 0; i < slots; i++) {
                if (hashes[i] == hash) {
                    return i;
                }
                if (hashes[i] == -1) {
                    free = i;
                }
            }
            return free;
        }
        public boolean hasFree() {
            return getTextureIdx(-1) != -1;
        }
    }
    public static float getTexW() {
        return tileSize/(float)texSize;
    }
    public static int getTexSize() {
        return texSize;
    }
    public static float getX(int idx) {
        return (idx%cols)/(float)cols;
    }
    public static float getY(int idx) {
        return (idx/cols)/(float)cols;
    }
    public static int getXPx(int idx) {
        return (idx%cols)*tileSize;
    }
    public static int getYPx(int idx) {
        return (idx/cols)*tileSize;
    }

    TextureAtlas[] textures = new TextureAtlas[10];
    
    public TextureAtlas getFirstFreeTextureAtlas() {
        for (int i = 0; i < textures.length; i++) {
            if (textures[i] != null) {
                if (textures[i].hasFree()) {
                    return textures[i];
                }
            }
            if (textures[i] == null) {
                textures[i] = new TextureAtlas(i);
                return textures[i];
            }
        }
//        throw new GameError("TextureAtlas overflow");
        return null;
    }
    
    

    public static SingleBlockRenderAtlas getInstance() {
        return instance;
    }
    
    SingleBlockRenderAtlas() {
    }

    public boolean needsRender(Block block, int data, StackData stackData) {
        int hash = block.id<<8|data;
        TextureAtlas atlas = getAtlas(hash, false);
        if (atlas == null)
            return true;
        int idx = atlas.getTextureIdx(hash);
        return idx < 0;
    }
    Map<Integer, TextureAtlas> map = Maps.newHashMap();
    private TextureAtlas getAtlas(int hash, boolean make) {
        TextureAtlas atlas = map.get(hash);
        if (atlas == null && make) {
            atlas = getFirstFreeTextureAtlas();
            if (atlas == null)
                return null;

            map.put(hash, atlas);
        }
        return atlas;
    }
    public TextureAtlas getAtlas(Block block, int data, StackData stackData) {
        int hash = block.id<<8|data;
        TextureAtlas atlas = getAtlas(hash, true);
        return atlas;
    }



    public void init() {
    }


    public void setupTextureAtlas(TextureAtlas atlas) {
        atlas.renderBuffer = new RenderFramebufferCached(false, true, false).tag("BlockSelect_UI_Cached");
        atlas.renderBuffer.init();
        atlas.renderBuffer.setSize(texSize, texSize);
        atlas.renderBuffer.clear();
//        if (Engine.isVulkan) {
//            atlas.frameBufferVk = new nidefawl.qubes.vulkan.FrameBuffer(Engine.vkContext);
//            atlas.frameBufferVk.fromRenderpass(VkRenderPasses.passFramebuffer, 0, VK10.VK_IMAGE_USAGE_SAMPLED_BIT);
//            atlas.frameBufferVk.build(VkRenderPasses.passFramebuffer, texSize, texSize);
//        } else {
//
//            atlas.frameBuffer = new FrameBuffer(texSize, texSize);
//            atlas.frameBuffer.setColorAtt(GL_COLOR_ATTACHMENT0, GL_RGBA8);
//            atlas.frameBuffer.setFilter(GL_COLOR_ATTACHMENT0, GL_LINEAR, GL_LINEAR);
//            atlas.frameBuffer.setClearColor(GL_COLOR_ATTACHMENT0, 0F, 0F, 0F, 0F);
//            atlas.frameBuffer.setHasDepthAttachment();
//            atlas.frameBuffer.setup(null);
//            atlas.frameBuffer.bind();
//            atlas.frameBuffer.clearFrameBuffer();
//        }
    }

    public void reset() {
        Arrays.fill(this.textures, null);
        for (Entry<Integer, TextureAtlas> entry : this.map.entrySet()) {
            TextureAtlas atlas = entry.getValue();
            if (atlas != null) {
                atlas.renderBuffer.destroy();
            }
        }
        this.map.clear();
    }



    public RenderFramebufferCached getTexture(Block block, int data, StackData stackData) {
        int hash = block.id<<8|data;
        TextureAtlas atlas = getAtlas(hash, false);
        if (atlas != null) {
            return atlas.renderBuffer;
        }
        return null;
    }

    public int getTextureIdx(Block block, int data, StackData stackData) {
        int hash = block.id<<8|data;
        TextureAtlas atlas = getAtlas(hash, false);
        if (atlas != null) {
            return atlas.getTextureIdx(hash);
        }
        return -1;
    }
    public int getHash(Block block, int data, StackData stackData) {
        int hash = block.id<<8|data;
        return hash;
    }
    public TextureAtlas getAtlasAtIdx(int i) {
        return this.textures[i];
    }
}
