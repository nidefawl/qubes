/**
 * 
 */
package nidefawl.qubes.assets;

import java.io.*;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Stack;

import com.google.common.collect.Sets;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class AssetPackFolder extends AssetPack {

    private final File directory;

    HashSet<File> files = Sets.newHashSet();
    /**
     * @param directory
     */
    public AssetPackFolder(File directory) {
        this.directory = directory;
        Stack<File> stack = new Stack<File>();
        stack.push(this.directory);
        while (!stack.isEmpty()) {
            File f = stack.pop();
            System.out.println("scan "+f);
            File[] fList = f.listFiles(new FileFilter() {
                
                @Override
                public boolean accept(File f) {
                    return f.isDirectory() || (f.isFile());
                }
            });

            for (int i = 0; fList != null && i < fList.length; i++) {
                if (fList[i].isDirectory() && !stack.contains(fList[i])) {
                    stack.push(fList[i]);
                } else if (fList[i].isFile()) {
                    files.add(fList[i]);
                }
            }
        }
    }


    @Override
    public AssetInputStream getInputStream(String name) throws IOException {
        File f = new File(this.directory, name);
        if (files.contains(f)) {
            FileInputStream fis = new FileInputStream(f);
            BufferedInputStream bif = new BufferedInputStream(fis);
            return new AssetInputStream(this, bif);
        }
        return null;
    }

    @Override
    public String toString() {
        return "Asset Pack "+this.directory;
    }

    @Override
    public void collectAssets(final String path, final String extension, LinkedHashSet<AssetPath> assets) {
        if (this.directory.isDirectory()) {
            Stack<File> stack = new Stack<File>();
            stack.push(this.directory);
            while (!stack.isEmpty()) {
                File f = stack.pop();
                File[] fList = f.listFiles(new FileFilter() {
                    
                    @Override
                    public boolean accept(File f) {
                        return f.isDirectory() || (f.isFile() && f.getName().startsWith(path) && f.getName().endsWith(extension));
                    }
                });

                for (int i = 0; fList != null && i < fList.length; i++) {
                    if (fList[i].isDirectory()) {
                        stack.push(fList[i]);
                    } else if (fList[i].isFile()) {
                        assets.add(new AssetPath(this, fList[i].getPath()));    
                    }
                }
            }
        }
    }
}
