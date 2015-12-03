/**
 * 
 */
package nidefawl.qubes.assets;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashSet;

import nidefawl.qubes.NativeClassLoader;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class AssetPackClassPath extends AssetPack {

    public AssetPackClassPath() {
    }

    @Override
    public AssetInputStream getInputStream(String name) {
        byte[] data = NativeClassLoader.loadGameResource("/res/"+name);
        if (data != null)
            return new AssetInputStream(this, new ByteArrayInputStream(data));
        return new AssetInputStream(this, getClass().getResourceAsStream("/res/"+name));
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Core Assets";
    }

    @Override
    public void collectAssets(String path, String extension, LinkedHashSet<AssetPath> assets) {
    }
}
