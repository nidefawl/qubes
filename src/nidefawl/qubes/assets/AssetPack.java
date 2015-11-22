/**
 * 
 */
package nidefawl.qubes.assets;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public abstract class AssetPack {

    /**
     * @param name
     * @throws IOException 
     */
    public abstract AssetInputStream getInputStream(String name) throws IOException;

}
