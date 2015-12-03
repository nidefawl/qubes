/**
 * 
 */
package nidefawl.qubes.assets;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class AssetPath extends Asset {

    private String path;

    /**
     * 
     */
    public AssetPath(AssetPack pack, String path) {
        this.path = path;
        setPack(pack);
    }

    /**
     * @return the path
     */
    public String getPath() {
        return this.path;
    }
}
