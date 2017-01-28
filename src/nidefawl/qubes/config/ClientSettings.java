/**
 * 
 */
package nidefawl.qubes.config;

import nidefawl.qubes.gl.Engine;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class ClientSettings extends AbstractYMLConfig {
    public ClientSettings() {
        super(true);
    }

    public final RenderSettings renderSettings = Engine.RENDER_SETTINGS;
    public int chunkLoadDistance;
    public float thirdpersonDistance;
    public boolean dirty;
    private int saveTicks;
    public String lastserver;
    public boolean gui3d;
    
    @Override
    public void setDefaults() {
        this.chunkLoadDistance = 12; 
        this.thirdpersonDistance = 4.0f;
        this.gui3d = false;
        if (this.renderSettings != null) // null from constructor -> already called from own cstr
        this.renderSettings.setDefaults();
    }

    @Override
    public void load() {
        lastserver = getString("lastserver", "nide.ddns.net:21087");
        thirdpersonDistance = getFloat("thirdpersonDistance", thirdpersonDistance);
        chunkLoadDistance = getInt("chunkLoadDistance", chunkLoadDistance);
        this.renderSettings.shadowDrawMode = getInt("shadowDrawMode", this.renderSettings.shadowDrawMode);
        this.renderSettings.anisotropicFiltering = getInt("anisotropicFiltering", this.renderSettings.anisotropicFiltering);
        this.renderSettings.normalMapping = getInt("normalMapping", this.renderSettings.normalMapping);
        gui3d = getBoolean("gui3d", gui3d);
        this.renderSettings.ssr = getInt("ssr", this.renderSettings.ssr);
        this.renderSettings.ao = getInt("ao", this.renderSettings.ao);
        this.renderSettings.aa = getInt("aa", this.renderSettings.aa);
        this.renderSettings.smaaPredication = getBoolean("smaaPredication", this.renderSettings.smaaPredication);
        this.renderSettings.smaaQuality = getInt("smaaQuality", this.renderSettings.smaaQuality);
    }

    @Override
    public void save() {
        setString("lastserver", lastserver);
        setInt("chunkLoadDistance", chunkLoadDistance);
        setInt("shadowDrawMode", this.renderSettings.shadowDrawMode);
        setInt("ssr", this.renderSettings.ssr);
        setInt("ao", this.renderSettings.ao);
        setFloat("thirdpersonDistance", thirdpersonDistance);
        setInt("anisotropicFiltering", this.renderSettings.anisotropicFiltering);
        setInt("normalMapping", this.renderSettings.normalMapping);
        setBoolean("gui3d", gui3d);
        setInt("aa", this.renderSettings.aa);
        setInt("smaaQuality", this.renderSettings.smaaQuality);
        setBoolean("smaaPredication", this.renderSettings.smaaPredication);
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
