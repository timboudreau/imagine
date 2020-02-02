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
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.Fillable;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.Strokable;
import net.java.dev.imagine.api.vector.Vector;
import net.java.dev.imagine.api.vector.Volume;
import net.java.dev.imagine.api.vector.util.Pt;

/**
 *
 * @author Tim Boudreau
 */
public final class Arc implements Strokable, Fillable, Volume, Adjustable, Vector {

    private static long serialVersionUID = 2394L;
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

    public String toString() {
        return "Arc: " + x + ", " + y + ", "
                + width + ", " + height + ": "
                + startAngle + ", " + arcAngle
                + " fill:" + fill;
    }

    public Shape toShape() {
        return new Arc2D.Double(x, y, width, height, startAngle, arcAngle,
                fill ? Arc2D.OPEN : Arc2D.CHORD); //XXX PIE?
    }

    public boolean isFill() {
        return fill;
    }

    public boolean equals(Object o) {
        boolean result = o instanceof Arc;
        if (result) {
            Arc a = (Arc) o;
            result = a.arcAngle == arcAngle && a.startAngle
                    == startAngle && width == a.width && height == a.height
                    && x == a.x && y == a.y && width == a.width;
        }
        return result;
    }

    public int hashCode() {
        return (int) arcAngle + (int) x + (int) y
                + (int) height + (int) startAngle + (int) width;
    }

    public void paint(Graphics2D g) {
        if (fill) {
            fill(g);
        } else {
            draw(g);
        }
    }

    public void getControlPoints(double[] xy) {
        xy[0] = this.x;
        xy[1] = this.y;
        xy[2] = xy[0] + this.width;
        xy[3] = xy[1] + this.height;
    }

    public int getControlPointCount() {
        return 2;
    }

    public void draw(Graphics2D g) {
        g.draw(toShape());
    }

    public void fill(Graphics2D g) {
        g.fill(toShape());
    }

    public Primitive copy() {
        return new Arc(x, y, width, height, startAngle, arcAngle, fill);
    }

    public Pt getLocation() {
        return new Pt(x, y);
    }

    public void setLocation(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void clearLocation() {
        x = 0;
        y = 0;
    }

    public Vector copy(AffineTransform transform) {
        double[] pts = new double[]{
            x, y, x + width, y + height,};
        transform.transform(pts, 0, pts, 0, 2);
        return new Arc(pts[0], pts[1], pts[2] - pts[0], pts[3] - pts[1],
                startAngle, arcAngle, fill);
    }

    public int[] getVirtualControlPointIndices() {
        return EMPTY_INT;
    }

    public void getBounds(Rectangle2D.Double dest) {
        dest.x = x;
        dest.y = y;
        dest.width = width;
        dest.height = height;
    }

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
}
