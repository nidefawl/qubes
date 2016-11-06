/**
 * 
 */
package nidefawl.qubes.test;

import static nidefawl.qubes.util.GameMath.*;
import nidefawl.qubes.util.TripletLongHash;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class TestIntHashFunctions {

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

        for (int x = Short.MIN_VALUE; x <= Short.MAX_VALUE; x++) {
            for (int z = Short.MIN_VALUE; z <= Short.MAX_VALUE; z++) {

                int l2 = toInt(x, z);
                int x2 = ihToX(l2);
                int z2 = ihToZ(l2);
                if (x2 != x || z2 != z) {
                    System.out.println("int missmatch");
                    System.out.println(x+","+z);
                    System.out.println(f(l2)+" -> "+x2+","+z2);
                    return;
                }
            }
        }
//		System.out.println(f(l));
//		p(f(l>>16));
//		p(f(l>>>16));
//		p(f((l>>16)& 0xFFFF));
//		p(f((l>>>16)& 0xFFFF));
		
//        return (int) ((hash >> 48) & 0xFFFFL);
		{
		}
		
	}

	/**
	 * @param binaryString
	 */
    private static void p(String binaryString) {
        while (binaryString.length() < 32)
            binaryString = "0"+binaryString;
        System.out.println(binaryString);
        
    }
    private static String f(int n) {
        String binaryString = Integer.toBinaryString(n);
        while (binaryString.length() < 32)
            binaryString = "0"+binaryString;
        return binaryString;
    }
}
