/**
 * 
 */
package nidefawl.qubes.hex;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class Point2F {

	public float x;
	public float y;

	/**
	 * 
	 */
	public Point2F(float x, float y) {
		this.x = x;
		this.y = y;
	}
	/**
	 * 
	 */
	public Point2F(double x, double y) {
		this.x = (float) x;
		this.y = (float) y;
	}
}
