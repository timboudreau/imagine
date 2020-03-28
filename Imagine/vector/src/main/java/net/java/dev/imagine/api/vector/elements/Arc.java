/*
 * Arc.java
 *
 * Created on September 27, 2006, 6:27 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.elements;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import static java.lang.Double.doubleToLongBits;
import java.util.Arrays;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.Fillable;
import net.java.dev.imagine.api.vector.Strokable;
import net.java.dev.imagine.api.vector.Volume;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import net.java.dev.imagine.api.vector.util.Pt;
import org.imagine.geometry.util.GeometryUtils;
import net.java.dev.imagine.api.vector.Vectors;

/**
 *
 * @author Tim Boudreau
 */
public final class Arc implements Strokable, Fillable, Volume, Adjustable, Vectors {

    private static final long serialVersionUID = 2_394L;
    public double x;
    public double y;
    public double width;
    public double height;
    public double startAngle;
    public double arcAngle;
    public boolean fill;

    /**
     * Creates a new instance of Arc
     */
    public Arc(double x, double y, double width, double height, double startAngle, double arcAngle, boolean fill) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.startAngle = startAngle;
        this.arcAngle = arcAngle;
        this.fill = fill;
    }

    public Runnable restorableSnapshot() {
        double ox = x;
        double oy = y;
        double ow = width;
        double oh = height;
        double sa = startAngle;
        double aa = arcAngle;
        return () -> {
            x = ox;
            y = oy;
            width = ow;
            height = oh;
            startAngle = sa;
            arcAngle = aa;
        };
    }

    @Override
    public double cumulativeLength() {
        return GeometryUtils.shapeLength(toShape());
    }

    @Override
    public void translate(double x, double y) {
        this.x += x;
        this.y += y;
    }

    @Override
    public void applyTransform(AffineTransform xform) {
        Point2D.Double a = new Point2D.Double(x, y);
        Point2D.Double b = new Point2D.Double(x + width, y + height);
        xform.transform(a, a);
        xform.transform(b, b);
        x = a.x;
        y = a.y;
        width = b.x - a.x;
        height = b.y - a.y;
    }

    public double getStartAngle() {
        return startAngle;
    }

    public double getArcAngle() {
        return arcAngle;
    }

    public void setArcAngle(double arcAngle) {
        this.arcAngle = arcAngle;
    }

    public void setStartAngle(double startAngle) {
        this.startAngle = startAngle;
    }

    @Override
    public String toString() {
        return "Arc: " + x + ", " + y + ", "
                + width + ", " + height + ": "
                + startAngle + ", " + arcAngle
                + " fill:" + fill;
    }

    @Override
    public Arc2D.Double toShape() {
        return new Arc2D.Double(x, y, width, height, startAngle, arcAngle,
                fill ? Arc2D.OPEN : Arc2D.CHORD); //XXX PIE?
    }

    @Override
    public boolean isFill() {
        return fill;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        }
        boolean result = o instanceof Arc;
        if (result) {
            Arc a = (Arc) o;
            result = a.arcAngle == arcAngle && a.startAngle
                    == startAngle && width == a.width && height == a.height
                    && x == a.x && y == a.y && width == a.width;
        }
        return result;
    }

    @Override
    public int hashCode() {
        long bits = 2_701 * ((doubleToLongBits(x) * 83)
                + (doubleToLongBits(y) * 431)
                + (doubleToLongBits(width) * 5)
                + (doubleToLongBits(height) * 971)
                + (doubleToLongBits(startAngle) * 5_843)
                + (doubleToLongBits(arcAngle) * 7_451));
        return (((int) bits) ^ ((int) (bits >> 32)));
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
    public void getControlPoints(double[] xy) {
        xy[0] = this.x;
        xy[1] = this.y;
        xy[2] = xy[0] + this.width;
        xy[3] = xy[1] + this.height;
    }

    @Override
    public int getControlPointCount() {
        return 2;
    }

    @Override
    public void draw(Graphics2D g) {
        g.draw(toShape());
    }

    @Override
    public void fill(Graphics2D g) {
        g.fill(toShape());
    }

    @Override
    public Arc copy() {
        return new Arc(x, y, width, height, startAngle, arcAngle, fill);
    }

    @Override
    public Pt getLocation() {
        return new Pt(x, y);
    }

    @Override
    public void setLocation(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void clearLocation() {
        x = 0;
        y = 0;
    }

    @Override
    public Arc copy(AffineTransform transform) {
        double[] pts = new double[]{
            x, y, x + width, y + height};
        transform.transform(pts, 0, pts, 0, 2);
        return new Arc(pts[0], pts[1], pts[2] - pts[0], pts[3] - pts[1],
                startAngle, arcAngle, fill);
    }

    @Override
    public int[] getVirtualControlPointIndices() {
        return EMPTY_INT;
    }

    @Override
    public void getBounds(Rectangle2D dest) {
        dest.setFrame(x, y, width, height);
    }

    @Override
    public ControlPointKind[] getControlPointKinds() {
        ControlPointKind[] kinds = new ControlPointKind[2];
        Arrays.fill(kinds, ControlPointKind.PHYSICAL_POINT);
        return kinds;
    }

    @Override
    public void setControlPointLocation(int pointIndex, Pt location) {
        switch (pointIndex) {
            case 0:
                double nx = width + x;
                double ny = height + y;
                x = location.x;
                y = location.y;
                width = nx - x;
                height = ny - y;
                break;
            case 1:
                double w = location.x - x;
                double h = location.y - y;
                if (w < 0) {
                    x = location.x;
                    w = -w;
                }
                if (h < 0) {
                    y = location.y;
                    h = -h;
                }
                width = w;
                height = h;
                break;
            default:
                throw new IllegalArgumentException("Illegal point index");
        }
    }

    @Override
    public java.awt.Rectangle getBounds() {
        Rectangle2D.Double bds = new Rectangle2D.Double();
        getBounds(bds);
        return bds.getBounds();
    }
}
