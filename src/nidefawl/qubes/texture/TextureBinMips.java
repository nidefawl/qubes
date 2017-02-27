package nidefawl.qubes.texture;

import java.io.*;

import nidefawl.qubes.assets.AssetBinary;

public class TextureBinMips {
    public byte[] data;
	public int totalSize;
	public int mips;
	public int[] sizes;
	public int[] w;
	public int[] h;

    public TextureBinMips(byte[] data, int w, int h) {
        this.data = data;
        this.mips = 1;
        this.totalSize = data.length;
        this.w = new int[] {w};
        this.h = new int[] {h};
        this.sizes = new int[] {data.length};
    }
	public TextureBinMips(AssetBinary bin) {
		ByteArrayInputStream bis = new ByteArrayInputStream(bin.getData());
		DataInputStream dis = new DataInputStream(bis);
		try {
			totalSize = Integer.reverseBytes(dis.readInt());
//			System.out.println("size "+totalSize);
			mips = Integer.reverseBytes(dis.readInt());
//			System.out.println("mipmaps "+mips);
			sizes = new int[mips];
			w = new int[mips];
			h = new int[mips];
			for (int i = 0; i < mips; i++) {
				w[i] = Integer.reverseBytes(dis.readInt());
				h[i] = Integer.reverseBytes(dis.readInt());
				sizes[i] = Integer.reverseBytes(dis.readInt());
//				System.out.println("mipmaps["+i+"] = "+w[i]+","+h[i]+","+sizes[i]);
			}
			data = new byte[totalSize];
			dis.readFully(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}