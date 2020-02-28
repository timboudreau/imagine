/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.elements;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.Fillable;
import net.java.dev.imagine.api.vector.Strokable;
import net.java.dev.imagine.api.vector.Vector;
import net.java.dev.imagine.api.vector.Volume;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import net.java.dev.imagine.api.vector.util.Pt;
import org.imagine.geometry.Triangle;

/**
 *
 * @author Tim Boudreau
 */
public class TriangleWrapper implements Strokable, Fillable, Volume, Adjustable, Vector {

    public double ax, ay, bx, by, cx, cy;
    public boolean fill;

    public TriangleWrapper(double ax, double ay, double bx, double by, double cx, double cy) {
        this(ax, ay, bx, by, cx, cy, true);
    }

    public TriangleWrapper(double ax, double ay, double bx, double by, double cx, double cy, boolean fill) {
        this.ax = ax;
        this.ay = ay;
        this.bx = bx;
        this.by = by;
        this.cx = cx;
        this.cy = cy;
        this.fill = fill;
    }

    public TriangleWrapper(Triangle t) {
        this(t, true);
    }

    public TriangleWrapper(Triangle t, boolean fill) {
        ax = t.ax();
        ay = t.ay();
        bx = t.bx();
        by = t.by();
        cx = t.cx();
        cy = t.cy();
        this.fill = fill;
    }

    public Runnable restorableSnapshot() {
        double oax = ax;
        double oay = ay;
        double obx = bx;
        double oby = by;
        double ocx = cx;
        double ocy = cy;
        return () -> {
            ax = oax;
            ay = oay;
            bx = obx;
            by = oby;
            cx = ocx;
            cy = ocy;
        };
    }

    public double ax() {
        return ax;
    }

    public double ay() {
        return ay;
    }

    public double bx() {
        return bx;
    }

    public double by() {
        return by;
    }

    public double cx() {
        return cx;
    }

    public double cy() {
        return cy;
    }

    public void setAx(double val) {
        ax = val;
    }

    public void setAy(double val) {
        ay = val;
    }

    public void setBx(double val) {
        bx = val;
    }

    public void setBy(double val) {
        by = val;
    }

    public void setCx(double val) {
        cx = val;
    }

    public void setCy(double val) {
        cy = val;
    }

    @Override
    public void translate(double x, double y) {
        this.ax += x;
        this.ay += y;
        this.bx += x;
        this.by += y;
        this.cx += x;
        this.cy += y;
    }

    public TriangleWrapper[] tesselate() {
        Triangle[] result = toShape().tesselate();
        TriangleWrapper[] res = new TriangleWrapper[result.length];
        for (int i = 0; i < result.length; i++) {
            res[i] = new TriangleWrapper(result[i], fill);
        }
        return res;
    }

    @Override
    public ControlPointKind[] getControlPointKinds() {
        ControlPointKind[] kinds = new ControlPointKind[3];
        Arrays.fill(kinds, ControlPointKind.PHYSICAL_POINT);
        return kinds;
    }

    @Override
    public TriangleWrapper copy() {
        return new TriangleWrapper(ax, ay, bx, by, cx, cy, fill);
    }

    @Override
    public void draw(Graphics2D g) {
        g.draw(toShape());
    }

    @Override
    public Triangle toShape() {
        return new Triangle(ax, ay, bx, by, cx, cy);
    }

    @Override
    public Pt getLocation() {
        return new Pt(ax, ay);
    }

    @Override
    public void setLocation(double x, double y) {
        if (ax != x && ay != y) {
            double dx = x - ax;
            double dy = y - ay;
            ax = x;
            ay = y;
            bx += dx;
            by += dy;
            cx += dx;
            cy += dy;
        }
    }

    @Override
    public void clearLocation() {
        setLocation(0, 0);
    }

    @Override
    public Vector copy(AffineTransform transform) {
        double[] pts = new double[]{ax, ay, bx, by, cx, cy};
        transform.transform(pts, 0, pts, 0, 3);
        return new TriangleWrapper(pts[0], pts[1],
                pts[2], pts[3], pts[4], pts[5]);
    }

    @Override
    public void addToBounds(Rectangle2D bds) {
        double minX = Math.min(ax, Math.min(bx, cx));
        double minY = Math.min(ay, Math.min(by, cy));
        double maxX = Math.max(ax, Math.max(bx, cx));
        double maxY = Math.max(ay, Math.max(by, cy));
        bds.add(minX, minY);
        bds.add(maxX, maxY);
    }

    @Override
    public void getBounds(Rectangle2D dest) {
        double minX = Math.min(ax, Math.min(bx, cx));
        double minY = Math.min(ay, Math.min(by, cy));
        double maxX = Math.max(ax, Math.max(bx, cx));
        double maxY = Math.max(ay, Math.max(by, cy));
        double width = maxX - minX;
        double height = maxY - minY;
        dest.setFrame(minX, minY, width, height);
    }

    @Override
    public Rectangle getBounds() {
        double x = Math.floor(Math.min(ax, Math.min(bx, cx)));
        double y = Math.floor(Math.min(ay, Math.min(by, cy)));
        double mx = Math.ceil(Math.max(ax, Math.max(bx, cx)));
        double my = Math.ceil(Math.max(ay, Math.max(by, cy)));
        return new Rectangle((int) x, (int) y, (int) (mx - x), (int) (my - y));
    }

    @Override
    public void paint(Graphics2D g) {
        if (fill) {
            fill(g);
        } else {
            draw(g);
        }
    }

    @Override
    public void applyTransform(AffineTransform xform) {
        double[] pts = new double[]{ax, ay, bx, by, cx, cy};
        xform.transform(pts, 0, pts, 0, 3);
        ax = pts[0];
        ay = pts[1];
        bx = pts[2];
        by = pts[3];
        cx = pts[4];
        cy = pts[5];
    }

    @Override
    public void fill(Graphics2D g) {
        g.fill(toShape());
    }

    @Override
    public boolean isFill() {
        return fill;
    }

    @Override
    public int getControlPointCount() {
        return 3;
    }

    @Override
    public void getControlPoints(double[] xy) {
        xy[0] = ax;
        xy[1] = ay;
        xy[2] = bx;
        xy[3] = by;
        xy[4] = cx;
        xy[5] = cy;
    }

    @Override
    public int[] getVirtualControlPointIndices() {
        return new int[0];
    }

    @Override
    public void setControlPointLocation(int pointIndex, Pt location) {
        switch (pointIndex) {
            case 0:
                ax = location.x;
                ay = location.y;
                break;
            case 1:
                bx = location.x;
                by = location.y;
                break;
            case 2:
                cx = location.x;
                cy = location.y;
                break;
            default:
                throw new IllegalArgumentException("Index out of range " + pointIndex);
        }
    }

    @Override
    public String toString() {
        return "Triangle(" + ax + ", " + ay + ", "
                + bx + ", " + by + ", "
                + cx + ", " + cy + ")";
    }

    static int[] PRIMES = new int[]{
        5, 433, 691, 3_931, 8_101, 11
    };

    @Override
    public int hashCode() {
        long bits = 0;
        for (int i = 0; i < 6; i++) {
            double val;
            switch (i) {
                case 0:
                    val = ax * 5;
                    break;
                case 1:
                    val = ay * 433;
                    break;
                case 2:
                    val = bx * 691;
                    break;
                case 3:
                    val = by * 3_931;
                    break;
                case 4:
                    val = cx * 8_101;
                    break;
                case 5:
                    val = cy * 11;
                    break;
                default:
                    throw new AssertionError(i);
            }
            bits += val;
        }
        return (int) (Double.doubleToLongBits(bits)
                ^ (Double.doubleToLongBits(bits) >>> 32));
    }

    public boolean isIsomorphic(TriangleWrapper other) {
        if (other == null) {
            return false;
        } else if (other == this) {
            return true;
        }
        return Arrays.equals(pointsSorted(), other.pointsSorted());
    }

    public double[] toDoubleArray() {
        return pointsSorted();
    }

    public void normalize() {
        double[] d = pointsSorted();
        ax = d[0];
        ay = d[1];
        bx = d[2];
        by = d[3];
        cx = d[4];
        cy = d[5];
    }

    private double[] pointsSorted() {
        double[][] d = new double[3][2];
        d[0][0] = ax;
        d[0][1] = ay;
        d[1][0] = bx;
        d[1][1] = by;
        d[2][0] = cx;
        d[2][1] = cy;
        Arrays.sort(d, (a, b) -> {
            int result = Double.compare(a[0], b[0]);
            if (result == 0) {
                result = Double.compare(a[1], b[1]);
            }
            return result;
        });
        double[] result = new double[]{
            d[0][0], d[0][1], d[1][0], d[1][1], d[2][0], d[2][1]
        };
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TriangleWrapper other = (TriangleWrapper) obj;
        return doublesEqual(this.ax, other.ax)
                && doublesEqual(this.ay, other.ay)
                && doublesEqual(this.bx, other.bx)
                && doublesEqual(this.by, other.by)
                && doublesEqual(this.cx, other.cx)
                && doublesEqual(this.cy, other.cy);
    }

    private static boolean doublesEqual(double ax, double bx) {
        return Double.doubleToLongBits(ax) == Double.doubleToLongBits(bx);
    }
}
