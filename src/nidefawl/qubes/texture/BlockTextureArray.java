package nidefawl.qubes.texture;

import static org.lwjgl.opengl.GL11.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.Map.Entry;

import nidefawl.qubes.Main;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.GameMath;

import org.lwjgl.opengl.*;

public class BlockTextureArray {
    public static final int BLOCK_TEXTURE_BITS = 4;
    static final BlockTextureArray instance = new BlockTextureArray();

    public static BlockTextureArray getInstance() {
        return instance;
    }

    
    int[]      textures;
    public int glid;
    public int tileSize = 0;
    private int maxTextures;

    BlockTextureArray() {
    }

    public void init() {
        glid = GL11.glGenTextures();
    }

    public void reload() {
        ByteBuffer directBuf = null;
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, glid);
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("glBindTexture(GL30.GL_TEXTURE_2D_ARRAY)");

        int maxTileW = 0;

        int maxTextures = 0;
        HashMap<Integer, ArrayList<AssetTexture>> text = new HashMap<>();
        for (int i = 0; i < Block.block.length; i++) {
            Block b = Block.block[i];
            if (b != null) {
                String[] textures = b.getTextures();
                if (textures != null) {
                    ArrayList<AssetTexture> blockTextures = new ArrayList<>();
                    for (String s : textures) {
                        AssetTexture tex = AssetManager.getInstance().loadPNGAsset(s);
                        if (tex == null) {
                            throw new GameError("Failed loading block texture " + s);

                        }
                        int texW = Math.max(tex.getWidth(), tex.getHeight());
                        maxTileW = Math.max(maxTileW, texW);
                        blockTextures.add(tex);
                        maxTextures++;
                    }
                    text.put(b.id, blockTextures);
                }
            }
        }
        this.tileSize = maxTileW;
        this.maxTextures = maxTextures;
        this.textures = new int[Block.NUM_BLOCKS<<BLOCK_TEXTURE_BITS];
        int w = GameMath.log2(this.tileSize);

        nidefawl.game.GL.glTexStorage3D(GL30.GL_TEXTURE_2D_ARRAY, w, GL_RGBA8,              //Internal format
                this.tileSize, this.tileSize,   //width,height
                this.maxTextures       //Number of layers
        );
        if (Main.GL_ERROR_CHECKS)
            Engine.checkGLError("GL42.glTexStorage3D");
        Iterator<Entry<Integer, ArrayList<AssetTexture>>> it = text.entrySet().iterator();
        int slot = 0;
        while (it.hasNext()) {
            Entry<Integer, ArrayList<AssetTexture>> entry = it.next();
            int blockId = entry.getKey();
            ArrayList<AssetTexture> blockTexture = entry.getValue();
            for (int i = 0; i < blockTexture.size(); i++) {
                AssetTexture tex = blockTexture.get(i);
                byte[] data = tex.getData();
                if (directBuf == null || directBuf.capacity() < data.length) {
                    directBuf = ByteBuffer.allocateDirect(data.length).order(ByteOrder.nativeOrder());
                }
                directBuf.clear();
                directBuf.put(data, 0, data.length);
                directBuf.position(0).limit(data.length);
                GL12.glTexSubImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0,                     //Mipmap number
                        0, 0, slot,                 //xoffset, yoffset, zoffset
                        this.tileSize, this.tileSize, 1,                 //width, height, depth
                        GL_RGBA,                //format
                        GL_UNSIGNED_BYTE,      //type
                        directBuf);                //pointer to data
                if (Main.GL_ERROR_CHECKS)
                    Engine.checkGLError("GL12.glTexSubImage3D");
                textures[blockId << 4 | i] = slot;
                slot++;
            }
        }
        glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
        glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT);
        GL30.glGenerateMipmap(GL30.GL_TEXTURE_2D_ARRAY);
        GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
    }

    public int getTextureIdx(int block, int texId) {
        return this.textures[block << 4 | texId];
    }

}
