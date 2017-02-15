package nidefawl.qubes.texture;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.EXTTextureCompressionS3TC;
import org.lwjgl.opengl.GL11;

import nidefawl.qubes.texture.TextureCreateInfo.FilterType;
import nidefawl.qubes.util.GameError;

public class TextureCreateInfo {
    public static enum FilterType {
        LINEAR, NEAREST
    };
    public static enum UVCoordMode {
        CLAMP, REPEAT
    };
    public static enum TextureFormat {
        RGBA8(false), BC1(true), BC1_ALPHA(true), BC2(true), BC3(true);
        private final boolean isCompressed;
        public boolean isCompressed() {
            return this.isCompressed;
        }
        private TextureFormat(boolean isCompressed) {
            this.isCompressed = isCompressed;
        }
    };
    public static class TextureSub {
        /**
         * @param width
         * @param height
         * @param size
         */
        public TextureSub(int width, int height, int size) {
            this.width = width;
            this.height = height;
            this.size = size;
        }
        public int width;
        public int height;
        public int size;
        public int getWidth() {
            return width;
        }
        public int getHeight() {
            return height;
        }
        public int getSize() {
            return size;
        }
    }
    FilterType filterType = FilterType.LINEAR;
    TextureFormat textureFormat = TextureFormat.RGBA8;
    UVCoordMode uvMode = UVCoordMode.CLAMP;
    int numMipmaps;
    ByteBuffer nonDirectBufData = null;
    TextureSub[] levels = new TextureSub[0];
    int totalSize = 0;
    float anisotropicFilterLevel = 0;
    
    
    public TextureCreateInfo(DDSLoader dds) {
        ByteBuffer data = dds.getData().get(0);
        int totalSize = dds.getTotalSize();
        this.nonDirectBufData = ByteBuffer.allocate(totalSize);
        data.position(0).limit(totalSize);
        this.nonDirectBufData.put(data);
        this.nonDirectBufData.clear();
        this.numMipmaps = dds.getNumMipmaps();
        this.totalSize = totalSize;
        this.levels = new TextureSub[this.numMipmaps];
        for (int i = 0; i < this.numMipmaps; i++) {
            this.levels[i] = new TextureSub(dds.getWidth(i), dds.getHeight(i), dds.getSize(i));
        }
        switch (dds.getPixelFormat()) {
            case RGBA8:
                this.textureFormat = TextureFormat.RGBA8;
                break;
            case DXT1:
                this.textureFormat = TextureFormat.BC1;
                break;
            case DXT1A:
                this.textureFormat = TextureFormat.BC1_ALPHA;
                break;
            case DXT3:
                this.textureFormat = TextureFormat.BC2;
                break;
            case DXT5:
                this.textureFormat = TextureFormat.BC3;
                break;
            default:
                throw new GameError("Source pixelformat "+dds.getPixelFormat()+" not yet implemented");
        }
    }
    public TextureCreateInfo() {
    }
    public TextureCreateInfo(TextureFormat format, TextureSub[] levels, ByteBuffer data) {
        this.textureFormat = format;
        this.levels = levels;
        this.numMipmaps = levels.length;
        this.nonDirectBufData = data;
        this.totalSize = data.remaining();
    }
    public int getGLFormat() {
        switch (textureFormat) {
            case RGBA8:
                return GL11.GL_RGBA8;
            case BC1:
                return EXTTextureCompressionS3TC.GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
            case BC1_ALPHA:
                return EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
            case BC2:
                return EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
            case BC3:
                return EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
            default:
                throw new GameError("GL format for "+textureFormat+" not yet implemented");
        }
    }
    public boolean isCompresedFormat() {
        return textureFormat.isCompressed();
    }
    public FilterType getFilter() {
        return this.filterType;
    }
    public int getNumMips() {
        return this.numMipmaps;
    }
    public TextureSub getLevel(int i) {
        return levels[i];
    }
    public UVCoordMode getUVMode() {
        return this.uvMode;
    }
    public ByteBuffer getData() {
        return nonDirectBufData;
    }
    
    public int getTotalSize() {
        return this.totalSize;
    }
    public void setFilter(FilterType filterType) {
        this.filterType = filterType;
    }
    public void setUVMode(UVCoordMode uvMode) {
        this.uvMode = uvMode;
    }
    public float getAnisotropicFilterLevel() {
        return this.anisotropicFilterLevel;
    }
    public void setAnisotropicFilterLevel(float anisotropicFilterLevel) {
        this.anisotropicFilterLevel = anisotropicFilterLevel;
    }
}
