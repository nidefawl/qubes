package nidefawl.qubes.test;

public class TestC {
    
    public static void main(String[] args) {
        short[] data = new short[] { (short) 0b1010101010101010, (short)0b1100110011001100 };
        int idata = data[0] | data[1] << 16;
        System.out.println(Integer.toBinaryString(idata));
    }

}
