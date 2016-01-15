/**
 * 
 */
package nidefawl.qubes.test;

import nidefawl.qubes.util.TripletLongHash;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class TestHashFunctions {

	public static void main(String[] args) {
//		int bufLen = 32;
//		int block = 16;
//		int steps = bufLen/block;
//		if (bufLen%block!=0){
//			steps++;
//		}
//		int alignedLen = steps*block;
//		System.out.println(steps+" steps");
//		System.out.println(bufLen+"/"+alignedLen);
//		System.out.println(alignedLen-bufLen);
		
		
		long l = 0x80F0F0F0F0F0F0F0L;
		System.out.println(Long.toBinaryString(l));
		p(Long.toBinaryString(l>>48));
		p(Long.toBinaryString(l>>>48));
		p(Long.toBinaryString((l>>48)& 0xFFFFL));
		p(Long.toBinaryString((l>>>48)& 0xFFFFL));
		
//        return (int) ((hash >> 48) & 0xFFFFL);
		{

			long x = TripletLongHash.getX(l);
			long y = TripletLongHash.getY(l);
			long z = TripletLongHash.getZ(l);
			long l2 = TripletLongHash.toHash(x, y, z);
			long x2 = TripletLongHash.getX(l2);
			long y2 = TripletLongHash.getY(l2);
			long z2 = TripletLongHash.getZ(l2);
			if (x2 != x || y2 != y || z2 != z) {
				System.err.println("long missmatch");
			}

		}
		{

			int x = TripletLongHash.getX(l);
			int y = TripletLongHash.getY(l);
			int z = TripletLongHash.getZ(l);
			long l2 = TripletLongHash.toHash(x, y, z);
			int x2 = TripletLongHash.getX(l2);
			int y2 = TripletLongHash.getY(l2);
			int z2 = TripletLongHash.getZ(l2);
			if (x2 != x || y2 != y || z2 != z) {
				System.err.println("int missmatch");
			}

		}
		
	}

	/**
	 * @param binaryString
	 */
	private static void p(String binaryString) {
		while (binaryString.length() < 64)
			binaryString = "0"+binaryString;
		System.out.println(binaryString);
		
	}
}
