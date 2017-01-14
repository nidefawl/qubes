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
    public float thirdpersonDistance;
    public boolean dirty;
    private int saveTicks;
    public int ao;
    public String lastserver;
    public int anisotropicFiltering;
    
    @Override
    public void setDefaults() {
        this.chunkLoadDistance = 12; 
        this.shadowDrawMode = 0;
        this.ssr = 2;
        this.aa = 1;
        this.ao = 1;
        this.smaaQuality = 1;
        this.anisotropicFiltering = 0;
        this.thirdpersonDistance = 4.0f;
    }

    @Override
    public void load() {
        lastserver = getString("lastserver", "nide.ddns.net:21087");
        thirdpersonDistance = getFloat("thirdpersonDistance", thirdpersonDistance);
        chunkLoadDistance = getInt("chunkLoadDistance", chunkLoadDistance);
        shadowDrawMode = getInt("shadowDrawMode", shadowDrawMode);
        smaaQuality = getInt("smaaQuality", smaaQuality);
        anisotropicFiltering = getInt("anisotropicFiltering", anisotropicFiltering);
        ssr = getInt("ssr", ssr);
        aa = getInt("aa", aa);
        ao = getInt("ao", ao);
    }

    @Override
    public void save() {
        setString("lastserver", lastserver);
        setInt("chunkLoadDistance", chunkLoadDistance);
        setInt("shadowDrawMode", shadowDrawMode);
        setInt("ssr", ssr);
        setInt("aa", aa);
        setInt("ao", ao);
        setInt("smaaQuality", smaaQuality);
        setFloat("thirdpersonDistance", thirdpersonDistance);
        setInt("anisotropicFiltering", anisotropicFiltering);
    }

    /**
     * @return 
     * 
     */
    public boolean lazySave() {
        if (this.dirty && this.saveTicks--<=0) {
            this.saveTicks = 20;
            this.dirty = false;
            save();
            return true;
        }
        return false;
    }

}
