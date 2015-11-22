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
public class AssetInputStream {
    
    /**
     * @param assetPackZip
     * @param inputStream
     */
    public AssetInputStream(AssetPack source, InputStream inputStream) {
        this.source = source;
        this.inputStream = inputStream;
    }
    public AssetPack source;
    public InputStream inputStream;
    /**
     * @throws IOException 
     * 
     */
    public void close() throws IOException {
        if (this.inputStream != null)
            this.inputStream.close();
    }

}
