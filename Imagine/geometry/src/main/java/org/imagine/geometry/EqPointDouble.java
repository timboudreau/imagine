package org.imagine.geometry;

import com.mastfrog.function.DoubleBiConsumer;
import java.awt.Point;
import java.awt.geom.Point2D;
import org.imagine.geometry.util.GeometryStrings;
import org.imagine.geometry.util.GeometryUtils;

/**
 * A Point2D.Double which provides a reasonable (tolerance based) implementation
 * of equals() and hashCode().
 *
 * @author Tim Boudreau
 */
public final class EqPointDouble extends Point2D.Double implements Comparable<Point2D>, DoubleBiConsumer {

    public EqPointDouble() {
    }

    public EqPointDouble(double[] coords) {
        this(coords[0], coords[1]);
    }

    public EqPointDouble(int offset, double[] coords) {
        this(coords[offset], coords[offset + 1]);
    }

    public EqPointDouble(double x, double y) {
        super(x, y);
    }

    public EqPointDouble(Point2D p) {
        this(p.getX(), p.getY());
    }

    public EqPoint toFloat() {
        return new EqPoint(getX(), getY());
    }

    public EqPointDouble copy() {
        return new EqPointDouble(this);
    }

    public static EqPointDouble of(Point2D p) {
        if (p == null) {
            return null;
        }
        if (p instanceof EqPointDouble) {
            return (EqPointDouble) p;
        }
        return new EqPointDouble(p);
    }

    public void copyInto(double[] pts, int at) {
        pts[at] = x;
        pts[at + 1] = y;
    }

    /**
     * Exact equality test, with no within-tolerance skew.
     *
     * @param other Another point
     * @return true if x == other.x && y == other.y
     */
    public boolean exactlyEqual(Point2D other) {
        double ox = other.getX();
        double oy = other.getY();
        return ox == x && oy == y;
    }

    public void translate(double dx, double dy) {
        x += dx;
        y += dy;
    }

    public Point toPoint() {
        return new Point((int) Math.round(x), (int) Math.round(y));
    }

    public boolean equals(Point2D pt, double tolerance) {
        if (pt == this) {
            return true;
        }
        return GeometryUtils.isSamePoint(x, y, pt.getX(), pt.getY());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (o == this) {
            return true;
        } else if (o instanceof Point2D) {
            Point2D p = (Point2D) o;
            return GeometryUtils.isSamePoint(p, this);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return GeometryUtils.pointHashCode(x, y);
    }

    @Override
    public String toString() {
        return GeometryStrings.toString(x, y);
    }

    @Override
    public int compareTo(Point2D o) {
        int result = java.lang.Double.compare(y, o.getY());
        if (result == 0) {
            result = java.lang.Double.compare(x, o.getX());
        }
        return result;
    }

    @Override
    public void accept(double a, double b) {
        setLocation(a, b);
    }
}
