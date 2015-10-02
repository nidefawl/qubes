/**
 * 
 */
package nidefawl.qubes.noise;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public abstract class OpenSimplexNoise {

    /**
     * 
     */
    public OpenSimplexNoise() {
        super();
    }

    public abstract double eval(double x, double y, double z);
    public abstract double eval(double x, double y, double z, double w);
    public abstract double eval(double x, double y);

}