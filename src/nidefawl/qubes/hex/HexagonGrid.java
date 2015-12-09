/**
 * 
 */
package nidefawl.qubes.hex;

import nidefawl.qubes.util.GameMath;

/**
 * Hexagon math only class
 * 
 * @author Michael Hept 2015 
 * Copyright: Michael Hept
 * 
 */
public class HexagonGrid {
    public final double radius;
	public final double height;
	public final double width;
	public final double hwidth;
	public final double ttheight;
	final double[] sinCos = new double[12];
	private final static int[] offset = new int[] {
			+1, 0, +1, -1, 0, -1,
			-1, 0, -1, +1, 0, +1
	};

	public HexagonGrid(double radius) {
		this.radius = radius;
		this.height = radius * 3 / 2;
		this.width = Math.sqrt(3) * radius;
		this.hwidth = width / 2.0;
		this.ttheight = height * (2.0 / 3.0);
		for (int i = 0; i < 6; i++) {
			final double angle = 2.0 * Math.PI / 6.0 * (i + 0.5);
			sinCos[i * 2 + 0] = Math.cos(angle);
			sinCos[i * 2 + 1] = Math.sin(angle);
		}
	}

	public long blockToGrid(int x, int y) {
		return toHex(x + 0.5D, y + 0.5D);
	}

	public long toHex(double x, double y) {
		y += ttheight;
		x = x / width;
		double t1 = y / radius;
		double t2 = Math.floor(x + t1);
		double r = Math.floor((Math.floor(t1 - x) + t2) / 3.0);
		double q = Math.floor((Math.floor(2.0 * x + 1.0) + t2) / 3.0) - r;
		int row = (int) r;
		int col = (int) q;
		return GameMath.toLong(col, row);
	}

	public double getCenterX(int hexX, int hexY) {
		return hexX * this.width + hexY * hwidth;
	}

	public double getCenterY(int hexX, int hexY) {
		return hexY * this.height;
	}

	public final double getPointX(int hexX, int hexY, int n) {
		return getCenterX(hexX, hexY) + this.radius * sinCos[n * 2 + 0];
	}
	public final double getPointY(int hexX, int hexY, int n) {
		return getCenterY(hexX, hexY) + this.radius * sinCos[n * 2 + 1];
	}

	public final Point2F getPoint(int hexX, int hexY, int n) {
		final double centerX = hexX * this.width + hexY * hwidth;
		final double centerY = hexY * this.height;
		double x = centerX + this.radius * sinCos[n * 2 + 0];
		double y = centerY + this.radius * sinCos[n * 2 + 1];
		return new Point2F(x, y);
	}

	public final Point2F[] getPoints(int gridX, int gridZ) {
		final double centerX = gridX * this.width + gridZ * hwidth;
		final double centerY = gridZ * this.height;
		Point2F[] points = new Point2F[6];
		for (int i = 0; i < 6; i++) {
			double x = centerX + this.radius * sinCos[i * 2 + 0];
			double y = centerY + this.radius * sinCos[i * 2 + 1];
			points[i] = new Point2F(x, y);
		}
		return points;
	}

	public static long right(int hx, int hz) {
		return offset(hx, hz, 0);
	}
	public static long topRight(int hx, int hz) {
		return offset(hx, hz, 1);
	}
	public static long topLeft(int hx, int hz) {
		return offset(hx, hz, 2);
	}
	public static long left(int hx, int hz) {
		return offset(hx, hz, 3);
	}
	public static long bottomLeft(int hx, int hz) {
		return offset(hx, hz, 4);
	}
	public static long bottomRight(int hx, int hz) {
		return offset(hx, hz, 5);
	}

	public static long offset(int hx, int hz, int i) {
		return GameMath.toLong(offset[i*2]+hx, offset[i*2+1]+hz);
	}

}
