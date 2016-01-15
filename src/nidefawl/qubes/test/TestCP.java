/**
 * 
 */
package nidefawl.qubes.test;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class TestCP {
    
    public static void main(String[] args) {
        for (int i = -520; i < -510; i++) {
            System.err.println(i%513);    
        }
        for (int i = -2; i < 4; i++) {
            System.err.println(i%513);    
        }
        
        System.out.println(Integer.toHexString(Integer.reverseBytes(0x00112233)));
//        try {
//            PNGDecoder dec = new PNGDecoder(new FileInputStream(new File("res/textures/colorpalette.png")));
//            int w = dec.getWidth();
//            int h = dec.getHeight();
//            ByteBuffer buffer = ByteBuffer.allocate(w*h*4); 
//            dec.decode(buffer, w*4, PNGDecoder.Format.RGBA);
//            byte[] mainChunk = buffer.array();
//          String defTable = "";
//            for (int i = 0;i < mainChunk.length;) {
//                int rgba = 0;
//                rgba = mainChunk[i+3] & 0xFF;
//                rgba <<= 8;
//                rgba |= mainChunk[i+2] & 0xFF;
//                rgba <<= 8;
//                rgba |= mainChunk[i+1] & 0xFF;
//                rgba <<= 8;
//                rgba |= mainChunk[i+0] & 0xFF;
//                defTable+="table["+(i>>2)+"] = 0x"+Integer.toHexString(rgba)+";\n";
//                i+=4;
////                i++;
//            }
//            System.out.println(defTable);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

}
