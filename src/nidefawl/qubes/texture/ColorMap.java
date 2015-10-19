/**
 * 
 */
package nidefawl.qubes.texture;

import java.awt.Color;

import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.GameMath;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class ColorMap {
    public final static ColorMap grass = new ColorMap(); 
    public final static ColorMap foliage = new ColorMap();
    private int[] data;
    private int w; 

    /**
     * 
     */
    public ColorMap() {
    }

    /**
     * @param tex
     */
    public void set(AssetTexture tex) {
        this.w = tex.getWidth();
        if (this.w != tex.getHeight()) {
            throw new GameError("Invalid Block Color Map "+tex.getName());
        }
        this.data = TextureUtil.toIntRGBA(tex.getData());
        float[] vals = new float[3];
        for (int i = 0; i < this.data.length; i++) {
            Color.RGBtoHSB(((this.data[i]>>16)&0xFF),
                    ((this.data[i]>>8)&0xFF),
                    ((this.data[i]>>0)&0xFF), vals);
            int rgb = Color.HSBtoRGB(vals[0]-0.04f, Math.min(vals[1]*1.8f, 1), vals[2]*0.6f);
            this.data[i] = rgb;
        }
    }

    public int get(double x, double y) {
        y = 0.6;
        x = 0.9;
        y*=x;
        
        int ix = this.w-1-GameMath.floor(x*this.w);
        int iy = this.w-1-GameMath.floor(y*this.w);
        return this.data[iy*this.w+ix];
    }

}
