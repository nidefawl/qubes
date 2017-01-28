package nidefawl.qubes.config;

public class RenderSettings {
    public RenderSettings() {
        setDefaults();
    }
    public int shadowDrawMode;
    public int ssr;
    public int aa;
    public int smaaQuality;
    public boolean smaaPredication;
    public int ao;
    public int anisotropicFiltering;
    public int normalMapping;
    
    public void setDefaults() {
        this.shadowDrawMode = 0;
        this.ssr = 2;
        this.aa = 1;
        this.ao = 1;
        this.smaaQuality = 1;
        this.smaaPredication = true;
        this.normalMapping = 1;
        this.anisotropicFiltering = 0;
    }
}
