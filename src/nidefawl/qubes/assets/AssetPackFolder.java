/**
 * 
 */
package nidefawl.qubes.assets;

import java.io.*;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class AssetPackFolder extends AssetPack {

    private final File directory;

    /**
     * @param directory
     */
    public AssetPackFolder(File directory) {
        this.directory = directory;
    }

    /* (non-Javadoc)
     * @see nidefawl.qubes.assets.AssetPack#getInputStream(java.lang.String)
     */
    @Override
    public InputStream getInputStream(String name) throws IOException {
        File f = new File(this.directory, name);
        if (f.exists()) {
            FileInputStream fis = new FileInputStream(f);
            BufferedInputStream bif = new BufferedInputStream(fis);
            return bif;
        }
        return null;
    }

    @Override
    public String toString() {
        return "Asset Pack "+this.directory;
    }
}
