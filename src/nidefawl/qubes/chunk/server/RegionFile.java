package nidefawl.qubes.chunk.server;

import java.io.*;
import java.util.ArrayList;

public class RegionFile {

	static class RegionFileHeader {
		int version;
		final DataChunkMeta[] chunks = new DataChunkMeta[NUM_CHUNKS];
		ArrayList<Boolean> usedSectors = new ArrayList<>(); // crappy

		RegionFileHeader() {
			for (int a = 0; a < chunks.length; a++) {
				chunks[a] = new DataChunkMeta(FILE_META_SIZE + a * CHUNK_META_SIZE);
			}
		}

		void read(RandomAccessFile f) throws IOException {
			this.version = f.readInt();
			for (int a = 0; a < chunks.length; a++) {
				f.seek(FILE_META_SIZE + a * CHUNK_META_SIZE);
				chunks[a].read(f);
				if (chunks[a].offset >= 0) {
					while (usedSectors.size() < chunks[a].size + chunks[a].offset) {
						usedSectors.add(false);
					}
					for (int o = 0; o < chunks[a].size; o++) {
						usedSectors.set(chunks[a].offset + o, true);
					}
				}
			}
		}

		void write(RandomAccessFile f) throws IOException {
			f.writeInt(this.version);
			for (int a = 0; a < chunks.length; a++) {
				f.seek(FILE_META_SIZE + a * CHUNK_META_SIZE);
				chunks[a].write(f);
			}
		}
	}

	static class DataChunkMeta {
		public final long byteOffset;
		int offset = -1;
		int size;
		int writetime;
		DataChunkMeta(long byteOffset) {
			this.byteOffset = byteOffset;
		}

		void read(RandomAccessFile f) throws IOException {
			this.offset = f.readInt();
			this.size = f.readInt();
			this.writetime = f.readInt();
		}

		void write(RandomAccessFile f) throws IOException {
			f.writeInt(this.offset);
			f.writeInt(this.size);
			f.writeInt(this.writetime);
		}
	}

	public final static int SECTOR_SIZE = 1024 * 4;

	public final static int REGION_SIZE_BITS = 5;
	public final static int REGION_SIZE = 1 << REGION_SIZE_BITS;
	public final static int CHUNK_MASK = REGION_SIZE - 1;
	public final static int NUM_CHUNKS = 1 << (REGION_SIZE_BITS * 2);
	public final static int FILE_META_SIZE = 32;
	public final static int CHUNK_META_SIZE = 32;
	public final static int HEADER_SIZE = FILE_META_SIZE + NUM_CHUNKS * CHUNK_META_SIZE;
	public final static int DATA_HEADER_SIZE = 4;
	private final RandomAccessFile randomAccess;
	private String name;
	final RegionFileHeader header = new RegionFileHeader();

	public RegionFile(File file, int regionX, int regionZ) throws FileNotFoundException {
		this.name = file.getAbsolutePath();
		this.randomAccess = new RandomAccessFile(file, "rw");
		// this.randomAccess.getChannel();
		boolean validHeader = false;
		try {

			validHeader = this.readHeader();
		} catch (Exception e) {
			// TODO: report + handle;
			e.printStackTrace();
		}
		try {

			if (!validHeader) {
				writeHeader();
			}
		} catch (Exception e) {
			// TODO: report + handle;
			e.printStackTrace();
		}
		int h = getLastUsedSec();
//		System.out.println("opened region file, highest used sector: " + h);
	}
    public synchronized int deleteChunks() {
        int n = 0;
        for (int a = 0; a < this.header.chunks.length; a++) {
            DataChunkMeta m = this.header.chunks[a];
            if (m.offset > -1)
                n++;
            this.header.chunks[a] = new DataChunkMeta(FILE_META_SIZE + a * CHUNK_META_SIZE);
        }
        this.header.usedSectors.clear();
        try {
            this.randomAccess.seek(0);
            this.header.write(this.randomAccess);
        } catch (Exception e) {
            // TODO: report + handle;
            e.printStackTrace();
        }
        return n;
    }

	public synchronized byte[] readChunk(int x, int z) throws IOException {
		x &= CHUNK_MASK;
		z &= CHUNK_MASK;
		int idx = z * REGION_SIZE + x;
		DataChunkMeta meta = getMeta(idx);
		if (meta.offset < 0) {
			return new byte[0];
		}
		int offset = toBytes(meta.offset);
		this.randomAccess.seek(offset + HEADER_SIZE);
		int chunkSize = this.randomAccess.readInt();
		byte[] data = new byte[chunkSize];
		this.randomAccess.readFully(data);
		return data;
	}

	public static long timeWrite = 0L;
	public static long timeSeek = 0L;
	public static long timeSetSec = 0L;
	public static long timeFindSec = 0L;
	public static long timeWriteHeader = 0L;

	public synchronized void writeChunk(int x, int z, byte[] data) throws IOException {
		x &= CHUNK_MASK;
		z &= CHUNK_MASK;
		int idx = z * REGION_SIZE + x;
		DataChunkMeta meta = getMeta(idx);
		ArrayList<Boolean> used = this.header.usedSectors;
		setSectors(meta.offset, meta.size, false);
		int secs = toSectors(data.length + 4);
		if (meta.offset >= 0 && meta.size >= secs) {
//			System.out.println("writing " + data.length + " bytes (" + secs + " sectors) to prev. pos");
			meta.size = secs;
			writeAtOffset(meta, data);
			return;
		} else {
//			System.out.println("searching new position for " + data.length + " bytes (" + secs + " sectors)");
			int start = -1;
			int len = 0;
			for (int o = 0; o < used.size(); o++) {
				if (used.get(o)) {
					start = -1;
					len = 0;
				} else if (start < 0) {
					start = o;
					len = 1;
				} else {
					len++;
				}
				if (len >= secs) {
					break;
				}
			}
			if (start >= 0 && len >= secs) {
//				System.out.println("found empty position for " + data.length + " bytes (" + secs
//						+ " sectors) in allocated sectors at offset " + start);
				meta.offset = start;
				meta.size = secs;
				writeAtOffset(meta, data);
			} else {
//				System.out.println("no empty position for " + data.length + " bytes (" + secs
//						+ " sectors) in allocated sectors found, adding new sectors to end of file (file ends at "
//						+ used.size() + " sectors");
				meta.offset = getLastFreeSec();
				meta.size = secs;
				while (used.size() <= meta.offset + meta.size) {
					used.add(true);
				}
				writeAtOffset(meta, data);
			}
		}

	}

	private void writeAtOffset(DataChunkMeta meta, byte[] data) throws IOException {
		setSectors(meta.offset, meta.size, true);
		long l = System.nanoTime();
		this.randomAccess.seek(toBytes(meta.offset) + HEADER_SIZE);
		timeSeek += System.nanoTime() - l;
		this.randomAccess.writeInt(data.length);
		l = System.nanoTime();
		this.randomAccess.write(data);
		timeWrite += System.nanoTime() - l;
		l = System.nanoTime();
		this.randomAccess.seek(meta.byteOffset);
		meta.write(this.randomAccess);
		timeWriteHeader += System.nanoTime() - l;
	}

	private void setSectors(int offset, int size, boolean b) {
		long l = System.nanoTime();
		for (int o = 0; o < size; o++) {
			this.header.usedSectors.set(offset + o, b);
		}
		timeSetSec += System.nanoTime() - l;
	}

	private int toBytes(int sectors) {
		return SECTOR_SIZE * sectors;
	}

	private int toSectors(int bytes) {
		return bytes / SECTOR_SIZE + 1;
	}

	private DataChunkMeta getMeta(int idx) {
		return this.header.chunks[idx];
	}

	private void writeHeader() throws IOException {
		this.randomAccess.seek(0);
		this.header.write(this.randomAccess);
	}

	private boolean readHeader() throws IOException {
		this.randomAccess.seek(0);
		long len = this.randomAccess.length();
		if (len >= HEADER_SIZE) {
			header.read(randomAccess);
			return true;
		}
		return false;
	}

	public static String getName(int regionX, int regionZ) {
		return "region." + regionX + "." + regionZ + ".dat";
	}

	public synchronized void close() throws IOException {
		int h = getLastUsedSec();
		System.out.println("closing region file, highest used sector: " + h);
		this.randomAccess.getChannel().force(true);
		this.randomAccess.close();
	}

	private int getLastUsedSec() {
		for (int a = this.header.usedSectors.size() - 1; a >= 0; a--) {
			if (this.header.usedSectors.get(a)) {
				return a;
			}
		}
		return -1;
	}

	private int getLastFreeSec() {
		for (int a = this.header.usedSectors.size() - 1; a >= 0; a--) {
			if (this.header.usedSectors.get(a)) {
				return a + 1;
			}
		}
		return 0;
	}

	public String getFileName() {
		return this.name;
	}

}
