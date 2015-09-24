/**
 * 
 */
package nidefawl.qubes.assets;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import nidefawl.qubes.NativeClassLoader;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class AssetPackClassPath extends AssetPack {

    public AssetPackClassPath() {
    }

    @Override
    public InputStream getInputStream(String name) {
        byte[] data = NativeClassLoader.loadGameResource("/res/"+name);
        if (data != null)
            return new ByteArrayInputStream(data);
        return getClass().getResourceAsStream("/res/"+name);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Core Assets";
    }
}
