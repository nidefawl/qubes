/**
 * 
 */
package nidefawl.qubes.test;

import java.util.Locale;
import java.util.Random;

import nidefawl.qubes.util.Half;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class HalfTest {

    /**
     * 
     */
    public HalfTest() {
        Locale.setDefault(Locale.US);
        
        Random rand = new Random(44);
        float[] vals = new float[32];
        int idx = 0;
        vals[idx++] = Float.NaN;
        vals[idx++] = Float.NEGATIVE_INFINITY;
        vals[idx++] = Float.POSITIVE_INFINITY;
        vals[idx++] = 10000.0f;
        vals[idx++] = 1 / 32.0f;
        vals[idx++] = 1 / 16.0f;
        vals[idx++] = 1 / 8.0f;
        vals[idx++] = 1 / 4.0f;
        vals[idx++] = 1 / 2.0f;
        vals[idx++] = 2.0f;
        vals[idx++] = 4.0f;
        vals[idx++] = 8.0f;
        vals[idx++] = 16.0f;
        vals[idx++] = 32.0f;
        vals[idx++] = 64.0f;
        vals[idx++] = 128.0f;
        
        for (int i = idx; i < vals.length; i++) {
            vals[i] = rand.nextFloat();
            
        }
        for (int a = 0; a < vals.length; a++) {
            float fr = vals[a];
            int n = Half.fromFloat(fr);
            float f = Half.toFloat(n);
            float err = fr-f;
            float percRound = Math.abs(err);
            percRound = percRound/f;
            percRound*=100;
            System.out.printf("%f => %d => %f (%.8f = %.4f%% error)\n", fr, n, f, err, percRound);
        }
        float maxErr = -1;
        String errVal = "";
        for (int a = 0; a <111022000; a++) {
            float fr = rand.nextFloat()*rand.nextInt(23945);
            int n = Half.fromFloat(fr);
            float f = Half.toFloat(n);
            float err = fr-f;
            if (Math.abs(err) > maxErr) {
                maxErr = Math.abs(err);
                double percRound = maxErr/f;
                percRound*=100;
                errVal = String.format("%f => %d => %f (%.8f = %.4f%% error)\n", fr, n, f, err, percRound);
            }
        }
        System.out.println(errVal);
    }
    public static void main(String[] args) {
        new HalfTest();
    }

}
