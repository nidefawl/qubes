package nidefawl.qubes.assets;

import java.io.DataInputStream;
import java.util.ArrayList;

import com.google.common.collect.Lists;

public class AssetBinary extends Asset {
    private byte[] data;
    private String name;
    
    public AssetBinary() {
        this.name = "";
    }
    
    public AssetBinary(String name) {
        this.name = name;
    }

    public void load(AssetInputStream is) throws Exception {
        setPack(is.source);
        DataInputStream stream = new DataInputStream(is.inputStream);
        ArrayList<Byte> data = Lists.newArrayList();
        int n;
        while ((n = stream.available())>0) { // VERY inefficient, Do not use for large amounts of bin data
            for (int j = 0; j < n; j++) {
                data.add(stream.readByte());
            }
        }
        this.data = new byte[data.size()];
        for (int j = 0; j < this.data.length; j++) {
            this.data[j] = data.get(j);
        }
    }
    public byte[] getData() {
        return data;
    }

    /**
     * @return the texture path
     */
    public String getName() {
        return this.name;
    }
}
