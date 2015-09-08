package nidefawl.qubes.chunk.server;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;

public class RegionFileCache {
	private final File dir;

	public RegionFileCache(File dir) {
		this.dir = dir;
		this.dir.mkdirs();
		System.out.println("RegionFileCache at "+dir.getAbsolutePath());
	}

	public static int lhToZ(long l) {
		return (int) (l & 0xFFFFFFFF) + Integer.MIN_VALUE;
	}

	public static int lhToX(long l) {
		return (int) (l >> 32);
	}

	public static long toLong(int x, int z) {
		return ((long) x << 32) | ((long) z - Integer.MIN_VALUE);
	}

	final HashMap<Long, RegionFile> map = new HashMap<>();

	public RegionFile getRegionFileChunk(int chunkX, int chunkZ) {
		return getRegionFile(chunkX >> RegionFile.REGION_SIZE_BITS, chunkZ >> RegionFile.REGION_SIZE_BITS);
	}

	public RegionFile getRegionFile(int regionX, int regionZ) {
		long hash = toLong(regionX, regionZ);
		RegionFile f = map.get(hash);
		if (f == null) {
			synchronized (map) {
				f = map.get(hash);
				if (f == null) {
					try {
						f = new RegionFile(getFile(dir, regionX, regionZ), regionX, regionZ);
					} catch (Exception e) {
						e.printStackTrace();
						//TODO: handle, report to main loop and let it shutdown
					}
					map.put(hash, f);
				}
			}
		}
		return f;
	}

	private File getFile(File dir, int regionX, int regionZ) {
		String name = RegionFile.getName(regionX, regionZ);
		return new File(dir, name);
	}
	
	public synchronized void closeAll() {
		Iterator<RegionFile> it = this.map.values().iterator();
		while (it.hasNext()) {
			RegionFile file = it.next();
			try {
				file.close();
			} catch (Exception e) {
				System.err.println("While closing "+file.getFileName()+": "+e.getMessage());
				e.printStackTrace();
			}
		}
		this.map.clear();
	}
}
