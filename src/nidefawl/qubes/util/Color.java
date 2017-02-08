package nidefawl.qubes.util;

import nidefawl.qubes.vec.Vector3f;

public class Color {
    public static int HSBtoRGB(float hue, float saturation, float brightness) {
        int r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else {
            float h = (hue - (float)Math.floor(hue)) * 6.0f;
            float f = h - (float)java.lang.Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
            case 0:
                r = (int) (brightness * 255.0f + 0.5f);
                g = (int) (t * 255.0f + 0.5f);
                b = (int) (p * 255.0f + 0.5f);
                break;
            case 1:
                r = (int) (q * 255.0f + 0.5f);
                g = (int) (brightness * 255.0f + 0.5f);
                b = (int) (p * 255.0f + 0.5f);
                break;
            case 2:
                r = (int) (p * 255.0f + 0.5f);
                g = (int) (brightness * 255.0f + 0.5f);
                b = (int) (t * 255.0f + 0.5f);
                break;
            case 3:
                r = (int) (p * 255.0f + 0.5f);
                g = (int) (q * 255.0f + 0.5f);
                b = (int) (brightness * 255.0f + 0.5f);
                break;
            case 4:
                r = (int) (t * 255.0f + 0.5f);
                g = (int) (p * 255.0f + 0.5f);
                b = (int) (brightness * 255.0f + 0.5f);
                break;
            case 5:
                r = (int) (brightness * 255.0f + 0.5f);
                g = (int) (p * 255.0f + 0.5f);
                b = (int) (q * 255.0f + 0.5f);
                break;
            }
        }
        return 0xff000000 | (r << 16) | (g << 8) | (b << 0);
    }
    public static float[] RGBtoHSB(int r, int g, int b, float[] hsbvals) {
        float hue, saturation, brightness;
        if (hsbvals == null) {
            hsbvals = new float[3];
        }
        int cmax = (r > g) ? r : g;
        if (b > cmax) cmax = b;
        int cmin = (r < g) ? r : g;
        if (b < cmin) cmin = b;

        brightness = ((float) cmax) / 255.0f;
        if (cmax != 0)
            saturation = ((float) (cmax - cmin)) / ((float) cmax);
        else
            saturation = 0;
        if (saturation == 0)
            hue = 0;
        else {
            float redc = ((float) (cmax - r)) / ((float) (cmax - cmin));
            float greenc = ((float) (cmax - g)) / ((float) (cmax - cmin));
            float bluec = ((float) (cmax - b)) / ((float) (cmax - cmin));
            if (r == cmax)
                hue = bluec - greenc;
            else if (g == cmax)
                hue = 2.0f + redc - bluec;
            else
                hue = 4.0f + greenc - redc;
            hue = hue / 6.0f;
            if (hue < 0)
                hue = hue + 1.0f;
        }
        hsbvals[0] = hue;
        hsbvals[1] = saturation;
        hsbvals[2] = brightness;
        return hsbvals;
    }

    public static void setColorVec(int uint32RGB, Vector3f vec) {
        vec.z = (uint32RGB&0xFF)/255.0f;
        uint32RGB>>=8;
        vec.y = (uint32RGB&0xFF)/255.0f;
        uint32RGB>>=8;
        vec.x = (uint32RGB&0xFF)/255.0f;
    }
    
    public static int toRGBAInt32(Vector3f vec) {
        int r = (int) (vec.x < 0 ? 0 : vec.x > 1 ? 255 : vec.x*255f);
        int g = (int) (vec.y < 0 ? 0 : vec.y > 1 ? 255 : vec.y*255f);
        int b = (int) (vec.z < 0 ? 0 : vec.z > 1 ? 255 : vec.z*255f);
        return r<<16|g<<8|b;
    }
    public static int SRGBtoLin(int rgb) {
        Vector3f tmp = Vector3f.pool();
        setColorVec(rgb, tmp);
        tmp.x=linearize(tmp.x);
        tmp.y=linearize(tmp.y);
        tmp.z=linearize(tmp.z);
        return toRGBAInt32(tmp);
    }
    public static int LinToSRGB(int rgb) {
        Vector3f tmp = Vector3f.pool();
        setColorVec(rgb, tmp);
        tmp.x=srgb(tmp.x);
        tmp.y=srgb(tmp.y);
        tmp.z=srgb(tmp.z);
        return toRGBAInt32(tmp);
    }

    private static float srgb(float v)
    {
        v = GameMath.clamp(v, 0.0f, 1.0f);
        float K0 = 0.03928f;
        float a = 0.055f;
        float phi = 12.92f;
        float gamma = 2.4f;
        v = v <= K0 / phi ? v * phi : (1.0f + a) * GameMath.powf(v, 1.0f / gamma) - a;
        return v;
    }
    private static float linearize(float v) {
        v = GameMath.clamp(v, 0.0f, 1.0f);
        float K0 = 0.03928f;
        float a = 0.055f;
        float phi = 12.92f;
        float gamma = 2.4f;
        v = v <= K0 ? v / phi :GameMath.powf((v + a) / (1.0f + a), gamma);
        return v;
    }
}
