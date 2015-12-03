/**
 * 
 */
package nidefawl.qubes.assets;

import java.io.IOException;
import java.util.LinkedHashSet;

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

    /**
     * @param path
     * @param extension
     * @param assets 
     */
    public abstract void collectAssets(String path, String extension, LinkedHashSet<AssetPath> assets);

}
