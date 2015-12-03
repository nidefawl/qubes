/**
 * 
 */
package nidefawl.qubes.assets;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.common.collect.Maps;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class AssetPackZip extends AssetPack {

    private final File zipFile;
    Map<String, ZipEntry> map = Maps.newConcurrentMap();
    ZipFile zip = null;
    /**
     * @param directory
     * @throws IOException 
     */
    public AssetPackZip(File directory) throws IOException {
        this.zipFile = directory;
        readZip();
    }

    /* (non-Javadoc)
     * @see nidefawl.qubes.assets.AssetPack#getInputStream(java.lang.String)
     */
    @Override
    public AssetInputStream getInputStream(String name) throws IOException {
        ZipEntry e = map.get(name);
        if (e != null) {
            return new AssetInputStream(this, this.zip.getInputStream(e));
        }
        if (name.startsWith("textures/")) {
            name = "assets/minecraft/"+name;
        }
        e = map.get(name);
        if (e != null) {
            return new AssetInputStream(this, this.zip.getInputStream(e));
        }
        return null;
    }
    

    private void readZip() throws IOException {
        zip = new ZipFile(this.zipFile);
        this.map.clear();
        Enumeration<? extends ZipEntry> enumeration = zip.entries();
        while (enumeration.hasMoreElements()) {
            ZipEntry e = enumeration.nextElement();
            this.map.put(e.getName(), e);
        }
    }

    @Override
    public String toString() {
        return "Asset Pack "+this.zipFile;
    }

    @Override
    public void collectAssets(String path, String extension, LinkedHashSet<AssetPath> assets) {
        for (String s : this.map.keySet()) {
            if (s.startsWith(path) && s.endsWith(extension)) {
                assets.add(new AssetPath(this, s));
            }
        }
    }
}
