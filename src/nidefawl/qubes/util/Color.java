package nidefawl.qubes.util;

import nidefawl.qubes.vec.Vector3f;

public class Color {
    public static int HSLtoRGB(double h,
                                    double s,
                                    double l) {
        // Adapted from http://stackoverflow.com/a/9493060
        double r;
        double g;
        double b;

        if (s == 0) {
            r = l;
            g = l;
            b = l;
        } else {
            double q = l < 0.5 ? l * (1 + s) : l + s - l * s;
            double p = 2 * l - q;
            r = hue2Rgb(p, q, h + 1.0 / 3);
            g = hue2Rgb(p, q, h);
            b = hue2Rgb(p, q, h - 1.0 / 3);
        }
        return 0xff000000 | ((int)GameMath.clamp((float)r*255.0f, 0, 255) << 16) | ((int)GameMath.clamp((float)g*255.0f, 0, 255) << 8) | ((int)GameMath.clamp((float)b*255.0f, 0, 255) << 0);


    }

    private static double hue2Rgb(double p,
                                  double q,
                                  double t) {
        if (t < 0)
            t += 1;
        if (t > 1)
            t -= 1;
        if (t < 1.0 / 6) {
            return p + (q - p) * 6 * t;
        }
        if (t < 1.0 / 2) {
            return q;
        }
        if (t < 2.0 / 3) {
            return p + (q - p) * (2.0 / 3 - t) * 6;
        }
        return p;
    }


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
    public static float[] HSVtoHSL(float[] hsv_vals) {
        // both hsv and hsl values are in [0, 1]
        float h = hsv_vals[0];
        float s = hsv_vals[1];
        float v = hsv_vals[2];
        float l = (2 - s) * v / 2;

        if (l != 0) {
            if (l == 1) {
                s = 0;
            } else if (l < 0.5) {
                s = s * v / (l * 2);
            } else {
                s = s * v / (2 - l * 2);
            }
        }
        return new float[]{h,s,l};
    }
    public static float[] RGBtoHSL(int r, int g, int b, float[] hsbvals) {
        float[] HSB = RGBtoHSB(r, g, b, hsbvals);
        return HSVtoHSL(HSB);
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
