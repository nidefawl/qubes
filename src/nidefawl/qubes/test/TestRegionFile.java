package nidefawl.qubes.test;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import nidefawl.qubes.chunk.server.RegionFile;
import nidefawl.qubes.chunk.server.RegionFileCache;
import nidefawl.qubes.nbt.Tag;
import nidefawl.qubes.nbt.Tag.Compound;
import nidefawl.qubes.nbt.Tag.TagList;
import nidefawl.qubes.nbt.TagReader;


public class TestRegionFile {
	static RegionFileCache cache;
	private static File regionDir;

	public static void closeCache() {
		cache.closeAll();
	}

	public static void openCache() {
		cache = new RegionFileCache(regionDir);
	}
	private static RegionFile getRegionFile() {
		return cache.getRegionFile(0, 0);
	}

	public static void wipe() {
		regionDir.mkdirs();
		File[] list = regionDir.listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				return pathname.isFile() && pathname.getName().endsWith(".dat");
			}
		});
		for (int i = 0; list != null && i < list.length; i++) {
			list[i].delete();
		}
	}
	public static void main(String[] args) {
        regionDir = new File("regions");
		openCache();
//		testStringTag();
//		testDoubleTag();
//		testFloatTag();
//		testIntTag();
//		testShortTag();
//		testLongTag();
//		testByteTag();
//		testByteArrTag();
//		testTagList();
//		testCompound();
//		testDeepNesting();
        testCompressed();
	}
	public static void testDeepNesting() {
		
		try {
			int chunkX = 0;//rand.nextInt(RegionFile.REGION_SIZE);
			int chunkZ = 0;//rand.nextInt(RegionFile.REGION_SIZE);
			Tag.Compound tag = new Tag.Compound();
			Tag.Compound tag1 = tag;
			for (int i = 0; i < 10; i++) {
				Tag.Compound next = new Tag.Compound();
				tag.set("entry"+i, next);
				tag = next;
			}
			Map<String, Tag> map = tag1.getMap();
			System.out.println("'"+tag1.getName()+"': '"+tag1.getValue()+"'");
			byte[] bytes = TagReader.writeTagToBytes(tag1);
			getRegionFile().writeChunk(chunkX, chunkZ, bytes);
			byte[] dataIn = getRegionFile().readChunk(chunkX, chunkZ);
			Tag t = TagReader.readTagFromBytesLimited(dataIn);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	public static void testCompound() {
		try {
			int chunkX = 0;//rand.nextInt(RegionFile.REGION_SIZE);
			int chunkZ = 0;//rand.nextInt(RegionFile.REGION_SIZE);
			final Tag.Compound tag = new Tag.Compound();
			{
				tag.setName("myname");
				tag.set("doubleVal", new Tag.Double(4/23.0));
				tag.set("stringval", new Tag.StringTag("test"));
				Tag.TagList tagList = new Tag.TagList();
				for (int a = 0; a < 23; a++) 
				{
					tagList.add(new Tag.Double(a/23.0));
				}
				tag.set("tagList", tagList);
				Tag.Compound nestedCompound = new Tag.Compound();
				nestedCompound.set("doubleVal", new Tag.Double(4/23.0));
				nestedCompound.set("stringval", new Tag.StringTag("test"));
				tag.set("nestedCompound", nestedCompound);
			}
			
			
			byte[] bytes = TagReader.writeTagToBytes(tag);
			getRegionFile().writeChunk(chunkX, chunkZ, bytes);
			byte[] dataIn = getRegionFile().readChunk(chunkX, chunkZ);
			Tag t = TagReader.readTagFromBytes(dataIn);
			if (!(t instanceof Tag.Compound)) {
				throw new RuntimeException("Expected Tag.TagList, got instead: "+t);	
			}
			Tag.Compound readBack = (Tag.Compound) t;
			if (!tag.getName().equals(readBack.getName())) {
				throw new RuntimeException("Value or name not equal for "+tag.getType());
			}
			
			Map<String, Tag> map = tag.getMap();
			Map<String, Tag> map2 = readBack.getMap();
			if (map.size() != map2.size()) {
				throw new RuntimeException("Expected length to be equal, list1: "+map.size()+", list2: "+map2.size());
			}
			Iterator<Map.Entry<String, Tag>> it = map.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, Tag> entry = it.next();
				if (!map2.containsKey(entry.getKey())) {
					throw new RuntimeException("Missing tag "+entry.getKey()+" in readback map");
				}
				Tag tagEntry = entry.getValue();
				Tag tagEntry2 = map2.get(entry.getKey());
				if (tagEntry.getType() != tagEntry2.getType()) {
					throw new RuntimeException("Expected equal types for tag with name "+entry.getKey());
				}
				if (!tag.getName().equals(readBack.getName())) {
					throw new RuntimeException("name not equal for "+tag.getType());
				}
				if (tagEntry instanceof TagList) {
					List<Tag> list1 = ((TagList) tagEntry).getList();
					List<Tag> list2 = ((TagList) tagEntry2).getList();
					if (list1.size() != list2.size()) {
						throw new RuntimeException("Expected length to be equal, list1: "+list1.size()+", list2: "+list2.size());
					}
					for (int a = 0; a < list1.size(); a++) {
						Tag.Double doubleTag1 = (Tag.Double) list1.get(a);
						Tag.Double doubleTag2 = (Tag.Double) list2.get(a);
						if (doubleTag1.getDouble() != doubleTag2.getDouble()) {
							throw new RuntimeException("Expected data to be equal");	
						}
					}
				}
				else if (tagEntry instanceof Compound) {
					Map<String, Tag> nestedMap1 = ((Compound) tagEntry).getMap();
					Map<String, Tag> nestedMap2 = ((Compound) tagEntry2).getMap();
					if (nestedMap1.size() != nestedMap2.size()) {
						throw new RuntimeException("Expected nested maps to be equal in size: "+nestedMap2.size()+" - "+nestedMap1.size());
					}
				} else {
					if (!tagEntry.getValue().equals(tagEntry2.getValue())) {
						throw new RuntimeException("Value or name not equal for "+tagEntry2.getType());
					}
				}
			}
			System.out.println("'"+readBack.getName()+"': '"+readBack.getValue()+"'");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	public static void testTagList() {
		try {
			int chunkX = 0;//rand.nextInt(RegionFile.REGION_SIZE);
			int chunkZ = 0;//rand.nextInt(RegionFile.REGION_SIZE);
			Tag.TagList tag = new Tag.TagList();
			tag.setName("myname");
			for (int a = 0; a < 23; a++) 
			{
				tag.add(new Tag.Double(a/23.0));
			}
			byte[] bytes = TagReader.writeTagToBytes(tag);
			getRegionFile().writeChunk(chunkX, chunkZ, bytes);
			byte[] dataIn = getRegionFile().readChunk(chunkX, chunkZ);
			Tag t = TagReader.readTagFromBytes(dataIn);
			if (!(t instanceof Tag.TagList)) {
				throw new RuntimeException("Expected Tag.TagList, got instead: "+t);	
			}
			Tag.TagList readBack = (Tag.TagList) t;
			if (!tag.getName().equals(readBack.getName())) {
				throw new RuntimeException("Value or name not equal for "+tag.getType());
			}
			if (readBack.getListTagType() != tag.getListTagType()) {
				throw new RuntimeException("Expected ListTagType to be equal");
			}
			List<Tag> list1 = tag.getList();
			List<Tag> list2 = readBack.getList();
			if (list1.size() != list2.size()) {
				throw new RuntimeException("Expected length to be equal, list1: "+list1.size()+", list2: "+list2.size());
			}
			for (int a = 0; a < list1.size(); a++) {
				Tag.Double doubleTag1 = (Tag.Double) list1.get(a);
				Tag.Double doubleTag2 = (Tag.Double) list2.get(a);
				if (doubleTag1.getDouble() != doubleTag2.getDouble()) {
					throw new RuntimeException("Expected data to be equal");	
				}
			}
			System.out.println("'"+readBack.getName()+"': '"+readBack.getValue()+"'");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
    public static void testCompressed() {
        try {
            int chunkX = 0;//rand.nextInt(RegionFile.REGION_SIZE);
            int chunkZ = 0;//rand.nextInt(RegionFile.REGION_SIZE);
            Tag.ByteArray tag = new Tag.ByteArray();
            tag.setName("myname");
            byte[] data = new byte[1024*1024*1];
            new Random(0x102).nextBytes(data);
            Arrays.fill(data, data.length/2, data.length-1, (byte)0);
            tag.setArray(data);
            byte[] bytes = TagReader.writeTagToCompresedBytes(tag);
            byte[] bytes2 = TagReader.writeTagToBytes(tag);
            System.out.println("compressed len: "+bytes.length);
            System.out.println("uncompressed len: "+bytes2.length);
            getRegionFile().writeChunk(chunkX, chunkZ, bytes);
            byte[] dataIn = getRegionFile().readChunk(chunkX, chunkZ);
            System.out.println("read back len: "+dataIn.length);
            Tag t = TagReader.readTagFromCompressedBytes(dataIn);
            if (!(t instanceof Tag.ByteArray)) {
                throw new RuntimeException("Expected Tag.ByteArray, got instead: "+t);  
            }
            Tag.ByteArray readBack = (Tag.ByteArray) t;
            if (!tag.getName().equals(readBack.getName())) {
                throw new RuntimeException("name not equal for "+tag.getType());
            }
            byte[] readBytes = readBack.getArray();
            if (readBytes.length != data.length) {
                throw new RuntimeException("Expected length to be equal");
            }
            for (int a = 0; a < data.length; a++) {
                if (data[a] != readBytes[a]) {
                    throw new RuntimeException("Expected data to be equal");    
                }
            }
            System.out.println("'"+readBack.getName()+"': '"+readBack.getValue()+"'");
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    public static void testStringTag() {
        try {
            int chunkX = 0;//rand.nextInt(RegionFile.REGION_SIZE);
            int chunkZ = 0;//rand.nextInt(RegionFile.REGION_SIZE);
            Tag.StringTag tag = new Tag.StringTag();
            tag.setName("myname");
            tag.setString("unknown");
            byte[] bytes = TagReader.writeTagToBytes(tag);
            getRegionFile().writeChunk(chunkX, chunkZ, bytes);
            byte[] dataIn = getRegionFile().readChunk(chunkX, chunkZ);
            Tag t = TagReader.readTagFromBytes(dataIn);
            if (!(t instanceof Tag.StringTag)) {
                throw new RuntimeException("Expected Tag.StringTag, got instead: "+t);  
            }
            Tag.StringTag readBack = (Tag.StringTag) t;
            if (!tag.getName().equals(readBack.getName()) || !tag.getValue().equals(readBack.getValue())) {
                throw new RuntimeException("Value or name not equal for "+tag.getType());
            }
            System.out.println("'"+readBack.getName()+"': '"+readBack.getValue()+"'");
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
	public static void testDoubleTag() {
		try {
			int chunkX = 0;//rand.nextInt(RegionFile.REGION_SIZE);
			int chunkZ = 0;//rand.nextInt(RegionFile.REGION_SIZE);
			Tag.Double tag = new Tag.Double();
			tag.setName("myval");
			tag.setDouble(0.234);
			byte[] bytes = TagReader.writeTagToBytes(tag);
			getRegionFile().writeChunk(chunkX, chunkZ, bytes);	
			byte[] dataIn = getRegionFile().readChunk(chunkX, chunkZ);
			Tag t = TagReader.readTagFromBytes(dataIn);
			if (!(t instanceof Tag.Double)) {
				throw new RuntimeException("Expected Tag.Double, got instead: "+t);	
			}
			Tag.Double readBack = (Tag.Double) t;
			if (!tag.getName().equals(readBack.getName()) || !tag.getValue().equals(readBack.getValue())) {
				throw new RuntimeException("Value or name not equal for "+tag.getType());
			}
			System.out.println("'"+readBack.getName()+"': '"+readBack.getValue()+"'");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	public static void testFloatTag() {
		try {
			int chunkX = 0;//rand.nextInt(RegionFile.REGION_SIZE);
			int chunkZ = 0;//rand.nextInt(RegionFile.REGION_SIZE);
			Tag.Float tag = new Tag.Float();
			tag.setName("myval");
			tag.setFloat(0.234F);
			byte[] bytes = TagReader.writeTagToBytes(tag);
			getRegionFile().writeChunk(chunkX, chunkZ, bytes);	
			byte[] dataIn = getRegionFile().readChunk(chunkX, chunkZ);
			Tag t = TagReader.readTagFromBytes(dataIn);
			if (!(t instanceof Tag.Float)) {
				throw new RuntimeException("Expected Tag.Float, got instead: "+t);	
			}
			Tag.Float readBack = (Tag.Float) t;
			if (!tag.getName().equals(readBack.getName()) || !tag.getValue().equals(readBack.getValue())) {
				throw new RuntimeException("Value or name not equal for "+tag.getType());
			}
			System.out.println("'"+readBack.getName()+"': '"+readBack.getValue()+"'");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	public static void testIntTag() {
		try {
			int chunkX = 0;//rand.nextInt(RegionFile.REGION_SIZE);
			int chunkZ = 0;//rand.nextInt(RegionFile.REGION_SIZE);
			Tag.Int tag = new Tag.Int();
			tag.setName("myval");
			tag.setInt(2);
			byte[] bytes = TagReader.writeTagToBytes(tag);
			getRegionFile().writeChunk(chunkX, chunkZ, bytes);	
			byte[] dataIn = getRegionFile().readChunk(chunkX, chunkZ);
			Tag t = TagReader.readTagFromBytes(dataIn);
			if (!(t instanceof Tag.Int)) {
				throw new RuntimeException("Expected Tag.Int, got instead: "+t);	
			}
			Tag.Int readBack = (Tag.Int) t;
			if (!tag.getName().equals(readBack.getName()) || !tag.getValue().equals(readBack.getValue())) {
				throw new RuntimeException("Value or name not equal for "+tag.getType());
			}
			System.out.println("'"+readBack.getName()+"': '"+readBack.getValue()+"'");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	public static void testShortTag() {
		try {
			int chunkX = 0;//rand.nextShort(RegionFile.REGION_SIZE);
			int chunkZ = 0;//rand.nextShort(RegionFile.REGION_SIZE);
			Tag.Short tag = new Tag.Short();
			tag.setName("myval");
			tag.setShort((short) (Short.MAX_VALUE-4));
			byte[] bytes = TagReader.writeTagToBytes(tag);
			getRegionFile().writeChunk(chunkX, chunkZ, bytes);	
			byte[] dataIn = getRegionFile().readChunk(chunkX, chunkZ);
			Tag t = TagReader.readTagFromBytes(dataIn);
			if (!(t instanceof Tag.Short)) {
				throw new RuntimeException("Expected Tag.Short, got instead: "+t);	
			}
			Tag.Short readBack = (Tag.Short) t;
			if (!tag.getName().equals(readBack.getName()) || !tag.getValue().equals(readBack.getValue())) {
				throw new RuntimeException("Value or name not equal for "+tag.getType());
			}
			System.out.println("'"+readBack.getName()+"': '"+readBack.getValue()+"'");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	public static void testLongTag() {
		try {
			int chunkX = 0;//rand.nextShort(RegionFile.REGION_SIZE);
			int chunkZ = 0;//rand.nextShort(RegionFile.REGION_SIZE);
			Tag.Long tag = new Tag.Long();
			tag.setName("myval");
			tag.setLong(0xABE17492);
			byte[] bytes = TagReader.writeTagToBytes(tag);
			getRegionFile().writeChunk(chunkX, chunkZ, bytes);	
			byte[] dataIn = getRegionFile().readChunk(chunkX, chunkZ);
			Tag t = TagReader.readTagFromBytes(dataIn);
			if (!(t instanceof Tag.Long)) {
				throw new RuntimeException("Expected Tag.Long, got instead: "+t);	
			}
			Tag.Long readBack = (Tag.Long) t;
			if (!tag.getName().equals(readBack.getName()) || !tag.getValue().equals(readBack.getValue())) {
				throw new RuntimeException("Value or name not equal for "+tag.getType());
			}
			System.out.println("'"+readBack.getName()+"': '"+readBack.getValue()+"'");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public static void testByteTag() {
		try {
			int chunkX = 0;//rand.nextShort(RegionFile.REGION_SIZE);
			int chunkZ = 0;//rand.nextShort(RegionFile.REGION_SIZE);
			Tag.Byte tag = new Tag.Byte();
			tag.setName("myval");
			tag.setByte((byte) 23);
			byte[] bytes = TagReader.writeTagToBytes(tag);
			getRegionFile().writeChunk(chunkX, chunkZ, bytes);	
			byte[] dataIn = getRegionFile().readChunk(chunkX, chunkZ);
			Tag t = TagReader.readTagFromBytes(dataIn);
			if (!(t instanceof Tag.Byte)) {
				throw new RuntimeException("Expected Tag.Byte, got instead: "+t);	
			}
			Tag.Byte readBack = (Tag.Byte) t;
			if (!tag.getName().equals(readBack.getName()) || !tag.getValue().equals(readBack.getValue())) {
				throw new RuntimeException("Value or name not equal for "+tag.getType());
			}
			System.out.println("'"+readBack.getName()+"': '"+readBack.getValue()+"'");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	public static void testByteArrTag() {
		try {
			int chunkX = 0;//rand.nextShort(RegionFile.REGION_SIZE);
			int chunkZ = 0;//rand.nextShort(RegionFile.REGION_SIZE);
			Tag.ByteArray tag = new Tag.ByteArray();
			tag.setName("myval");
			byte[] data = new byte[1024*4];
			new Random(0x101).nextBytes(data);
			tag.setArray(data);
			byte[] bytes = TagReader.writeTagToBytes(tag);
			getRegionFile().writeChunk(chunkX, chunkZ, bytes);	
			byte[] dataIn = getRegionFile().readChunk(chunkX, chunkZ);
			Tag t = TagReader.readTagFromBytes(dataIn);
			if (!(t instanceof Tag.ByteArray)) {
				throw new RuntimeException("Expected Tag.Byte, got instead: "+t);	
			}
			Tag.ByteArray readBack = (Tag.ByteArray) t;
			if (!tag.getName().equals(readBack.getName())) {
				throw new RuntimeException("name not equal for "+tag.getType());
			}
			byte[] readBytes = readBack.getArray();
			if (readBytes.length != data.length) {
				throw new RuntimeException("Expected length to be equal");
			}
			for (int a = 0; a < data.length; a++) {
				if (data[a] != readBytes[a]) {
					throw new RuntimeException("Expected data to be equal");	
				}
			}
			System.out.println("'"+readBack.getName()+"': '"+readBack.getValue()+"'");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public static void realloctest() {
//		wipe();
		try {
			openCache();
//			byte[] data = getRegionFile().readChunk(0, 0);
//			if (data.length != 0) {
//				throw new RuntimeException("Expected length == 0");
//			}
//			getRegionFile().writeChunk(0, 0, new byte[] { 0, 1, 2, 3 });
//			data = getRegionFile().readChunk(0, 0);
//			if (data.length != 4) {
//				throw new RuntimeException("Expected length == 4");
//			}
//			closeCache();
//			openCache();
//			data = getRegionFile().readChunk(0, 0);
//			if (data.length != 4) {
//				throw new RuntimeException("Expected length == 4");
//
//			}
			Random rand = new Random(0xdeadbeef);
			long timeRand = 0L;
			long timeWrite = 0L;
			long timeRead = 0L;
			for (int i = 0; i < 12; i++) {
				int chunkX = 0;//rand.nextInt(RegionFile.REGION_SIZE);
				int chunkZ = 0;//rand.nextInt(RegionFile.REGION_SIZE);
				int nBits = i < 3 ? 0 : i;
				int size = 1 << (11+nBits);
//				if (i == 11) {
//					size = 4000;
//				}
				byte[] dataOut = new byte[size];
				long l = System.nanoTime();
				rand.nextBytes(dataOut);
				timeRand+=System.nanoTime()-l;
				
//				openCache();
				System.out.println("writing "+size+" bytes");
				l = System.nanoTime();
				getRegionFile().writeChunk(chunkX, chunkZ, dataOut);
				timeWrite+=System.nanoTime()-l;
//				closeCache();
//				openCache();
				l = System.nanoTime();
				byte[] dataIn = getRegionFile().readChunk(chunkX, chunkZ);
				timeRead+=System.nanoTime()-l;
				if (dataIn.length != dataOut.length) {
					throw new RuntimeException("Expected length to be equal");
				}
				for (int a = 0; a < dataIn.length; a++) {
					if (dataIn[a] != dataOut[a]) {
						throw new RuntimeException("Expected data to be equal");	
					}
				}
//				closeCache();
			}
			/*
			 * 	public static long timeWrite = 0L;
	public static long timeSeek = 0L;
	public static long timeSetSec = 0L;
	public static long timeFindSec = 0L;
	public static long timeWriteHeader = 0L;
			 */
			System.out.printf("time rand: %.2f\n", timeRand/1000000.0);
			System.out.printf("time read: %.2f\n", timeRead/1000000.0);
			System.out.printf("time write: %.2f\n", timeWrite/1000000.0);
			System.out.printf("time write byte[]: %.2f\n", RegionFile.timeWrite/1000000.0);
			System.out.printf("timeSetSec: %.2f\n", RegionFile.timeSetSec/1000000.0);
			System.out.printf("timeSeek: %.2f\n", RegionFile.timeSeek/1000000.0);
			System.out.printf("timeFindSec: %.2f\n", RegionFile.timeFindSec/1000000.0);
			System.out.printf("timeWriteHeader: %.2f\n", RegionFile.timeWriteHeader/1000000.0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
