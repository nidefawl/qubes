package nidefawl.qubes.noise;

import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.assets.AssetTexture;
import nidefawl.qubes.util.GameError;

public class NoiseMap2D extends AbstractNoiseGen {

    private String tex;
    private short[] data;
    private int height;
    private int width;

    public NoiseMap2D(String tex) {
        this.tex = tex;
        AssetTexture asset = AssetManager.getInstance().loadPNGAsset(this.tex);
        if (asset.getComponents() != 1) {
            throw new GameError("Invalid tex format, need 1 comp fmt");
        }
        this.width = asset.getWidth();
        this.height = asset.getHeight();
        if (asset.getBits() == 8) {
            this.data = new short[this.width*this.height];
            byte[] bytes = asset.getData();
            for (int i = 0; i < this.data.length; i++) {
                this.data[i] = bytes[i];
            }
        } else {
            this.data = asset.getUShortData();
        }
    }
    public double evalI(int n, int m) {
        if (n < 0 || n >= this.width) {
            return 0;
        }
        if (m < 0 || m >= this.width) {
            return 0;
        }
        int idx = m*this.width+n;
        int s = this.data[idx]&0xFFFF;
        double d = s/65536.0;
        if (d > 1)
            System.out.println("> 1 "+s);
        return d;
        
    }
    public double eval(double x, double y) {
        int n = (int) (x*this.width);
        int m = (int) (y*this.height);
        n %= this.width;
        if (n < 0)
            n += this.width;
        m %= this.height;
        if (m < 0)
            m += this.height;
        return evalI(n, m);
    }
    public int getWidth() {
        return this.width;
    }
    public int getHeight() {
        return this.height;
    }
}
