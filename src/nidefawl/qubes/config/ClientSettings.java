/**
 * 
 */
package nidefawl.qubes.config;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class ClientSettings extends AbstractYMLConfig {
    public ClientSettings() {
        super(true);
    }

    public int chunkLoadDistance;
    public int shadowDrawMode;
    public int ssr;
    public int aa;
    public int smaaQuality;
    
    @Override
    public void setDefaults() {
        this.chunkLoadDistance = 12; 
        this.shadowDrawMode = 0;
        this.ssr = 0;
        this.aa = 1;
        this.smaaQuality = 1;
    }

    @Override
    public void load() {
        chunkLoadDistance = getInt("chunkLoadDistance", chunkLoadDistance);
        shadowDrawMode = getInt("shadowDrawMode", shadowDrawMode);
        smaaQuality = getInt("smaaQuality", smaaQuality);
        ssr = getInt("ssr", ssr);
        aa = getInt("aa", aa);
    }

    @Override
    public void save() {
        setInt("chunkLoadDistance", chunkLoadDistance);
        setInt("shadowDrawMode", shadowDrawMode);
        setInt("ssr", ssr);
        setInt("aa", aa);
        setInt("smaaQuality", smaaQuality);
    }

}
