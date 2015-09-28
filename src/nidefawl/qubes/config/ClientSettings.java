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
    
    @Override
    public void setDefaults() {
        this.chunkLoadDistance = 12; 
        this.shadowDrawMode = 0;
        this.ssr = 0;
    }

    @Override
    public void load() {
        chunkLoadDistance = getInt("chunkLoadDistance", chunkLoadDistance);
        shadowDrawMode = getInt("shadowDrawMode", shadowDrawMode);
        ssr = getInt("ssr", ssr);
    }

    @Override
    public void save() {
        setInt("chunkLoadDistance", chunkLoadDistance);
        setInt("shadowDrawMode", shadowDrawMode);
        setInt("ssr", ssr);
    }

}
