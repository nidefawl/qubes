/**
 * 
 */
package nidefawl.qubes.test;

import nidefawl.qubes.vec.Vector3f;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class TestNormalCalc {

    public static void main(String[] args) {
        float x = 1;
        float z =  -1;
        float y =  2;
        Vector3f v = new Vector3f(x,y,z);
        v.normalise();
        v.scale(0.5f);
        v.translate(0.5f, 0.5f, 0.5f);
        System.out.println(v);
        int iX = (int) (v.x * 255);
        int iY = (int) (v.y * 255);
        int iZ = (int) (v.z * 255);
        if (iX < 0 || iX > 255 || iZ < 0 || iZ > 255 || iY < 0 || iY > 255)
            throw new RuntimeException("oob");
        int normalRGB = iX<<16|iZ<<8|iY;
        String rgbStr = Integer.toHexString(normalRGB);
        while (rgbStr.length()<6)
            rgbStr = "0"+rgbStr;
        System.out.println(rgbStr);
        v.set(-50, -100, -50);
        v.normalise();
        System.out.println(v);;
    }
}
