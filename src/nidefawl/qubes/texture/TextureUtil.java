package nidefawl.qubes.texture;

import java.awt.image.BufferedImage;

import nidefawl.qubes.util.GameMath;

public class TextureUtil {

    public static byte[] genNoise(int w) {
        byte[] data = new byte[w * w * 3];
        for (int x = 0; x < w; x++)
            for (int z = 0; z < w; z++)
                for (int y = 0; y < 3; y++) {
                    int seed = (GameMath.randomI(x * 5) - 79 + GameMath.randomI(y * 37)) * 1 + GameMath.randomI((z - 2) * 73);
                    data[(x * 64 + z) * 3 + y] = (byte) (GameMath.randomI(seed) % 128);
                }
        return data;
    }
    public static byte[] genNoise2(int w) {
        byte[] data = new byte[w * w * 3];
        for (int y = 0; y < w; y++)
            for (int x = 0; x < w; x++)
                for (int z = 1; z < 4; z++) {
                    int seed = (GameMath.randomI(x) + GameMath.randomI(y * 19)) * GameMath.randomI(z*23)-z;
                    
                    data[(y * w + x) * 3 + (z-1)] = (byte) (GameMath.randomI(seed) % 128);
                }
        return data;
    }
//
//    public static byte[] genNoise2(int w, int h) {
//        int noct = 8;
//        long seed = 0xdeadbeefL;
//        seed--;
//        OpenSimplexNoise n4d2 = NoiseLib.makeGenerator(42);
//        OpenSimplexNoise n4d = NoiseLib.makeGenerator(5);
//        OpenSimplexNoise n4d3 = NoiseLib.makeGenerator(6);
//        OpenSimplexNoise n4d5 = NoiseLib.makeGenerator(7);
//        byte[] data = new byte[w * h * 3];
//        TimingHelper.startSilent(123);
////        float f1 = Client.ticksran + Main.instance.partialTick;
//        int iW = 0;
//        double scale = 6;
//        double scale2 = scale/1.2;
//        double sclae3 = 44;
//        double x1,x2,y1,y2;
//        double dx,dy;
//        x1 = -1; y1= -1;
//        x2 = 1; y2 = 1;
//        dx = x2-x1; dy = y2-y1;
//        float PI = (float) Math.PI;
//        double[] dNoise = new double[(w + iW * 2) * (h + iW * 2)];
//        float fb=0.05f;
//        double[] rgb = new double[3];
//        for (int iX = -iW; iX < w + iW; iX++)
//            for (int iZ = -iW; iZ < h + iW; iZ++) {
//                float fx = (iX/(float)w);
//                float fy = (iZ/(float)h);
//                double xI1=fx>fb?1:(fx)/fb;
//                double xI2=fx<1-fb?1:1-(fx-(1-fb))/fb;
//                double nx = x1+GameMath.cos(fx*2*PI)*dx/(2*PI);
//                double ny = y1+GameMath.cos(fy*2*PI)*dy/(2*PI);
//                double nz = x1+GameMath.sin(fx*2*PI)*dx/(2*PI);
//                double nw = y1+GameMath.sin(fy*2*PI)*dy/(2*PI);
//                double d3 = 0.6+0.2*n4d3.eval(nx*sclae3,ny*sclae3,nz*sclae3,nw*sclae3);
//                ny*=scale2;
//                nz*=scale;
//                nw*=scale2;
//                nx*=scale;
//                double d1 = n4d2.eval(nx,ny*0.1,nz,nw*0.1);
//                double d4 = n4d5.eval(nx,ny*0.4,nz,nw*0.4);
//                double oc = 12;
//                double d2 = (oc)*(n4d.eval(nx*(1+d3*0.1),ny*d1,nz*(1+d3*0.1),nw*d1)*0.5+0.5);
//                d2 *=(0.9+d3*0.3);
//                int a = GameMath.floor(d2);
//                d2 = (d2-a);
//                d2 = d2*0.25;
////                if (d2 < 0.47) d2 = 0;
//                double d = d2;
//                d = Math.min(1, d);
//                d = Math.max(0, d);
//                d = 1 - d;
//                d = Math.pow(d, 4)-1.23;
//                d = 1 - d;
//                d = Math.min(1, d);
//                d = Math.max(0, d);
//                int lu = 12;
//                d3 = Math.min(0.2, d3-0.7);
//                d4*=(0.4+d3);
//                d*=1.5;
//                rgb[0] = d * 69 + 10 * d1 * 0.3 + d3 * lu + (d4*34.2);
//                rgb[1] = d * 45 + 10 * d1 * 0.3 + d3 * lu + (d4*24.2);
//                rgb[2] = d * 7  + 10 + d3 * lu + (d4*14.2);
//                
//                oc = Math.abs(a)/oc;
//                rgb[0] += 3+oc*44;
//                rgb[1] += -4+oc*44;
//                rgb[2] += 0+oc*12;
//                for (int i = 0; i < 3; i++) {
//                    
//                    d = rgb[i];
//                    d = Math.min(255, d);
//                    d = Math.max(0, d);
////                    d = 1+(d-1)*xI1;
////                    d = 1+(d-1)*xI2;
//                    data[(iZ * w + iX) * 3 + i] = (byte)(int)d;
//                }
//
//            }
//        if (iW == 0) {
//        } else {
//            for (int ix = 0; ix < w; ix++)
//                for (int iz = 0; iz < h; iz++) {
//                    double d = getBlur(dNoise, ix, iz, iW, iW, w);
//                    d = 1 - d;
//                    d = Math.pow(d, 4);
//                    //              d-=1D;
//                    d = Math.min(1, d);
//                    d = Math.max(0, d);
//                    d = 1 - d;
//                    int lum = (int) (d * 255);
//                    //              int seed = (GameMath.randomI(x*5)-79 + GameMath.randomI(y * 37)) * 1+GameMath.randomI((z-2) * 73);
//                    data[(iz * w + ix) * 3 + 0] = (byte) lum;
//                    data[(iz * w + ix) * 3 + 1] = (byte) lum;
//                    data[(iz * w + ix) * 3 + 2] = (byte) lum;
//                    //                break;
//                }
//        }
//        long l = TimingHelper.stopSilent(123);
//        System.out.println(l);
//        return data;
//    }

    private static double getBlur(double[] dNoise, int x, int z, int iW, int i, int w) {
        if (i == 0) {
            return dNoise[((z + iW) * (w + iW * 2)) + (x + iW)];
        }
        double d = 0;
        double[] dWeights = new double[i * i * 2 * 2];
        double dmax = 0D;
        for (int x1 = -i; x1 < i; x1++) {
            for (int z1 = -i; z1 < i; z1++) {
                double dD = GameMath.dist2d(0, 0, x1, z1);
                if (dD > dmax)
                    dmax = dD;
                dWeights[((z1 + i) * (i * 2)) + (x1 + i)] = dD;
            }
        }
        for (int j = 0; j < dWeights.length; j++) {
            dWeights[j] = 1.0D - dWeights[j] / dmax;
        }
        double weight = 0;
        for (int x1 = -i; x1 < i; x1++) {
            for (int z1 = -i; z1 < i; z1++) {
                int xx = x1 + x;
                int zz = z1 + z;
                double dWeight = dWeights[((z1 + i) * (i * 2)) + (x1 + i)];
                weight += dWeight;
                //                if (x1==-i&&z1==-i)
                //                System.out.printf("%d, %d = %.2f\n", x1, z1, dWeight);
                d += dNoise[((zz + iW) * (w + iW * 2)) + (xx + iW)] * dWeight;
            }
        }
        return d / weight;
    }

    /**
     * @param data
     * @param tileSize
     * @param tileSize2
     */
    public static int getAverageColor(byte[] data, int w, int h) {
        int r=0,g=0,b=0;
        int pixelsContributing = 0;
        for (int x = 0; x < w; x++)
            for (int y = 0; y < h; y++) {
                int idx = (x+y*w)*4;
                int alpha = data[idx+3]&0xFF;
                if (alpha != 0) {
                    r+=data[idx+2]&0xFF;
                    g+=data[idx+1]&0xFF;
                    b+=data[idx+0]&0xFF;
                    pixelsContributing++;
                }
            }
        if (pixelsContributing > 0) {
            r = clampRGB(r / pixelsContributing);
            g = clampRGB(g / pixelsContributing);
            b = clampRGB(b / pixelsContributing);
        }
        return r<<16|g<<8|b;
    }

    /**
     * @param r
     * @return
     */
    private static int clampRGB(int r) {
        return r < 0 ? 0 : r > 255 ? 255 : r;
    }

    /**
     * @param data
     * @param avg
     * @return 
     */
    public static byte[] makeMipMap(byte[] data, int w, int h, int avg) {
        int w2 = w;
        int h2 = h;
        w*=2;
        h*=2;
        byte[] down = new byte[w2*h2*4];
        for (int x = 0; x < w2; x++) {
            for (int y = 0; y < h2; y++) {
                int idx = (y*w2+x)*4;
                int r=0,g=0,b=0;
                int alphaMixed = 0;
                for (int x2 = 0; x2 < 2; x2++) {
                    for (int y2 = 0; y2 < 2; y2++) {
                        int idx2 = ((x*2+x2)+(y*2+y2)*w)*4;
                        int alpha = data[idx2+3]&0xFF;
                        if (alpha != 0) {
                            r += data[idx2+2]&0xFF;
                            g += data[idx2+1]&0xFF;
                            b += data[idx2+0]&0xFF;
                        } else {
                            r += (avg>>16)&0xFF;
                            g += (avg>>8)&0xFF;
                            b += (avg>>0)&0xFF;
                        }
                        alphaMixed+=alpha;
                    }
                }
                r = clampRGB(r / 4);
                g = clampRGB(g / 4);
                b = clampRGB(b / 4);
                alphaMixed = clampRGB(alphaMixed);
                if (alphaMixed > 120) {
                    alphaMixed = 255;
                } else {
                    alphaMixed = 0;
                }
                down[idx+3] = (byte) alphaMixed;
                down[idx+2] = (byte) r;
                down[idx+1] = (byte) g;
                down[idx+0] = (byte) b;
            }
        }
        return down;
    }

    /**
     * @param data
     * @param tileSize
     * @param tileSize2
     */
    public static void clampAlpha(byte[] data, int w, int h) {
        for (int x = 0; x < w; x++)
            for (int y = 0; y < h; y++) {
                int idx = (x+y*w)*4;
                int alpha = data[idx+3]&0xFF;
                if (alpha > 30) {
                    data[idx+3] = (byte)255;
                } else {
                    data[idx+3] = 0;
                }
            }
    }

    public static byte[] toBytesRGBA(int[] irgba) {
        byte textureData[] = new byte[irgba.length * 4];
        for (int k1 = 0; k1 < irgba.length; k1++) {
            if (irgba[k1] >> 24 == 0) {
                for (int a = 0; a < 4; a++) 
                    textureData[k1 * 4 + a] = 0;
            } else {
                textureData[k1 * 4 + 0] = (byte)(irgba[k1] >> 16 & 0xff);
                textureData[k1 * 4 + 1] = (byte)(irgba[k1] >> 8 & 0xff);
                textureData[k1 * 4 + 2] = (byte)(irgba[k1] >> 0 & 0xff);
                textureData[k1 * 4 + 3] = (byte)(irgba[k1] >> 24 & 0xff);
            }
        }
        return textureData;
    }

    public static int[] toIntRGBA(byte[] rgba) {
        int textureData[] = new int[rgba.length / 4];
        for (int k1 = 0; k1 < rgba.length; k1+=4) {
            int irgba = 0;
            irgba |= (rgba[k1+0]&0xFF)<<16;
            irgba |= (rgba[k1+1]&0xFF)<<8;
            irgba |= (rgba[k1+0]&0xFF)<<0;
            irgba |= (rgba[k1+3]&0xFF)<<24;
            textureData[k1/4] = irgba;
        }
        return textureData;
    }

    public static byte[] toBytesRGBA(BufferedImage bufferedImage) {
        int j = bufferedImage.getWidth();
        int l = bufferedImage.getHeight();
        int rgba[] = new int[j * l];
        bufferedImage.getRGB(0, 0, j, l, rgba, 0, j);
        byte textureData[] = new byte[rgba.length * 4];
        for (int k1 = 0; k1 < rgba.length; k1++) {
            if (rgba[k1] >> 24 == 0) {
                for (int a = 0; a < 4; a++) 
                    textureData[k1 * 4 + a] = 0;
            } else {
                textureData[k1 * 4 + 0] = (byte)(rgba[k1] >> 16 & 0xff);
                textureData[k1 * 4 + 1] = (byte)(rgba[k1] >> 8 & 0xff);
                textureData[k1 * 4 + 2] = (byte)(rgba[k1] >> 0 & 0xff);
                textureData[k1 * 4 + 3] = (byte)(rgba[k1] >> 24 & 0xff);
            }
        }
        return textureData;
    }

    /**
     * @param rgba
     * @return
     */
    public static float getA(int rgba) {
        return ((rgba>>24)&0xFF)/255.0f;
    }
    public static float getR(int rgba) {
        return ((rgba>>16)&0xFF)/255.0f;
    }
    public static float getG(int rgba) {
        return ((rgba>>8)&0xFF)/255.0f;
    }
    public static float getB(int rgba) {
        return (rgba&0xFF)/255.0f;
    }

    public static int mixRGB(int rgb1, int rgb2, float f) {
        int r=0,g=0,b=0;
        float f1=1-f;
        b += (rgb1&0xff)*f1;
        b += (rgb2&0xff)*f;
        rgb1>>=8; rgb2>>=8;
        g += (rgb1&0xff)*f1;
        g += (rgb2&0xff)*f;
        rgb1>>=8; rgb2>>=8;
        r += (rgb1&0xff)*f1;
        r += (rgb2&0xff)*f;
        r = r>255?255:r<0?0:r;
        g = g>255?255:g<0?0:g;
        b = b>255?255:b<0?0:b;
        return r<<16|g<<8|b;
    }
    public static int mix3RGB(int rgb1, int rgb2, int rgb3) {
        int r,g,b;
        b = rgb1&0xff;
        b += rgb2&0xff;
        b += rgb3&0xff;
        rgb1>>=8; rgb2>>=8; rgb3>>=8;
        g = rgb1&0xff;
        g += rgb2&0xff;
        g += rgb3&0xff;
        rgb1>>=8; rgb2>>=8; rgb3>>=8;
        r = rgb1&0xff;
        r += rgb2&0xff;
        r += rgb3&0xff;
        r/=3;
        g/=3;
        b/=3;
        r = r>255?255:r<0?0:r;
        g = g>255?255:g<0?0:g;
        b = b>255?255:b<0?0:b;
        return r<<16|g<<8|b;
    }

}
