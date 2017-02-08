package nidefawl.qubes.texture;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.lwjgl.opengl.EXTTextureCompressionS3TC;
import org.lwjgl.stb.STBDXT;

public class DXTCompressor {
    
    
    public static void stbgl__compress(ByteBuffer p, ByteBuffer rgba, int w, int h, int output_desc, long end)
    {
       int i,j,y,y2;
       ByteBuffer block = ByteBuffer.allocateDirect(16*4).order(ByteOrder.nativeOrder());
       boolean alpha = (output_desc == EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT);
       int output_pos = 0;
        for (j = 0; j < w; j += 4) {
            int x = 4;
            for (i = 0; i < h; i += 4) {
                block.clear();
                if (i + 3 >= w)
                    x = w - i;
                for (y = 0; y < 4; ++y) {
                    if (j + y >= h)
                        break;
                    memcpy(block, (y * 16), rgba, (w * 4 * (j + y) + i * 4), x * 4);
                }
                if (x < 4) {
                    switch (x) {
                        case 0:
                            assert (false);
                        case 1:
                            for (y2 = 0; y2 < y; ++y2) {
                                memcpy(block, y2 * 16 + 1 * 4, block, y2 * 16 + 0 * 4, 4);
                                memcpy(block, y2 * 16 + 2 * 4, block, y2 * 16 + 0 * 4, 8);
                            }
                            break;
                        case 2:
                            for (y2 = 0; y2 < y; ++y2)
                                memcpy(block, y2 * 16 + 2 * 4, block, y2 * 16 + 0 * 4, 8);
                            break;
                        case 3:
                            for (y2 = 0; y2 < y; ++y2)
                                memcpy(block, y2 * 16 + 3 * 4, block, y2 * 16 + 1 * 4, 4);
                            break;
                    }
                }
                y2 = 0;
                for (; y < 4; ++y, ++y2)
                    memcpy(block, y * 16, block, y2 * 16, 4 * 4);
                p.clear();
                block.clear();
                p.position(output_pos);
//                System.out.println("call "+p+","+block);
                STBDXT.stb_compress_dxt_block(p, block, alpha, 0);
                output_pos += alpha ? 16 : 8;
            }
        }
        p.position(0).limit(output_pos);
//        p.flip();
       assert(p.position() <= end);
    }

    private static void memcpy(Buffer dst, int dstPos, Buffer src, int srcPos, int i) {
        for (int j = 0; j < i; j++) {
            ((ByteBuffer)dst).put(dstPos+j, ((ByteBuffer)src).get(srcPos+j));
        }
    }
}
