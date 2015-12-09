/**
 * 
 */
package nidefawl.qubes.vec;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class Vector2f {

    public float x; float y;
    /**
     * 
     */
    public Vector2f() {
    }
    public Vector2f(float x, float y) {
        this.x=x;
        this.y=y;
    }
    
    /**
     * @param d
     * @param e
     */
    public Vector2f(double d, double e) {
        this((float)d, (float)e);
    }
    public void normalize() {
        float len = getLength();
        if (len > 0) {
            x /= len;
            y /= len;
        }
    }
    /**
     * @return
     */
    public float getLength() {
        float lenSq = x*x+y*y;
        return lenSq<1.0E-7F?0.0f:(float)Math.sqrt(lenSq);
    }
    /**
     * @param v2
     * @return
     */
    public double dotProduct(Vector2f v2) {
        return this.x*v2.x+this.y*v2.y;
    }
    /**
     * @param d
     * @param e
     */
    public void set(double x, double y) {
        this.x = (float)x;
        this.y = (float)y;
    }
    /**
     * @param v2
     * @return
     */
    public double crossProduct(Vector2f v2) {
        return this.x*v2.y-this.y*v2.x;
    }
}
