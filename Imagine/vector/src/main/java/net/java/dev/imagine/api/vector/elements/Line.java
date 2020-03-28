/*
 * Line.java
 *
 * Created on September 27, 2006, 6:41 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.elements;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import static java.lang.Double.doubleToLongBits;
import java.util.Arrays;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.Strokable;
import net.java.dev.imagine.api.vector.Versioned;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import net.java.dev.imagine.api.vector.util.Pt;
import net.java.dev.imagine.api.vector.Vectors;

/**
 *
 * @author Tim Boudreau
 */
public class Line implements Strokable, Adjustable, Vectors, Versioned {

    private double x1;
    private double x2;
    private double y1;
    private double y2;
    private static final long serialVersionUID = 23_923_214L;
    private int rev;

    public Line(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public Runnable restorableSnapshot() {
        double ox1 = x1;
        double oy1 = y1;
        double ox2 = x2;
        double oy2 = y2;
        int oldRev = rev;
        return () -> {
            rev = oldRev;
            x1 = ox1;
            y1 = oy1;
            x2 = ox2;
            y2 = oy2;
        };
    }

    public int rev() {
        return rev;
    }

    private void change() {
        rev++;
    }

    @Override
    public void translate(double x, double y) {
        if (x != 0 || y != 0) {
            this.x1 += x;
            this.y1 += y;
            this.x2 += x;
            this.y2 += y;
            change();
        }
    }

    public double x1() {
        return x1;
    }

    public double y1() {
        return y1;
    }

    public double x2() {
        return x2;
    }

    public double y2() {
        return y2;
    }

    public void setX1(double x1) {
        if (x1 != this.x1) {
            this.x1 = x1;
            change();
        }
    }

    public void setX2(double x2) {
        if (x2 != this.x2) {
            this.x2 = x2;
            change();
        }
    }

    public void setY1(double y1) {
        if (y1 != this.y1) {
            this.y1 = y1;
            change();
        }
    }

    public void setY2(double y2) {
        if (y2 != this.y2) {
            this.y2 = y2;
            change();
        }
    }

    public double length() {
        return Point2D.distance(x1, y1, x2, y2);
    }

    @Override
    public double cumulativeLength() {
        return length();
    }

    @Override
    public String toString() {
        return "Line " + x1 + ", " + y1
                + "->" + x2 + ", " + y2;
    }

    @Override
    public Line2D.Double toShape() {
        return new Line2D.Double(x1, y1, x2, y2);
    }

    @Override
    public void applyTransform(AffineTransform xform) {
        if (xform == null || xform.isIdentity()) {
            return;
        }
        double[] pts = new double[4];
        getControlPoints(pts);
        xform.transform(pts, 0, pts, 0, 2);
        x1 = pts[0];
        x2 = pts[1];
        y1 = pts[2];
        y2 = pts[3];
        change();
    }

    @Override
    public void paint(Graphics2D g) {
//        g.drawLine (x1, y1, x2, y2);
        g.draw(toShape());
    }

    @Override
    public int getControlPointCount() {
        return 2;
    }

    @Override
    public void getControlPoints(double[] xy) {
        xy[0] = this.x1;
        xy[1] = this.y1;
        xy[2] = this.x2;
        xy[3] = this.y2;
    }

    @Override
    public ControlPointKind[] getControlPointKinds() {
        ControlPointKind[] kinds = new ControlPointKind[2];
        Arrays.fill(kinds, ControlPointKind.PHYSICAL_POINT);
        return kinds;
    }

    public Strokable create(int[] xp, int[] yp) {
        return new Line(xp[0], yp[0], xp[1], yp[1]);
    }

    public void getBounds(Rectangle r) {
        r.setFrameFromDiagonal(x1, y1, x2, y2);
    }

    @Override
    public void getBounds(Rectangle2D r) {
        r.setFrameFromDiagonal(x1, y1, x2, y2);
    }

    @Override
    public void addToBounds(Rectangle2D bds) {
        if (bds.isEmpty()) {
            bds.setFrameFromDiagonal(x1, y1, x2, y2);
        } else {
            bds.add(x1, y1);
            bds.add(x2, y2);
        }
    }

    @Override
    public void draw(Graphics2D g) {
        paint(g);
    }

    @Override
    public Line copy() {
        Line result = new Line(x1, y1, x2, y2);
        result.rev = rev;
        return result;
    }

    @Override
    public Pt getLocation() {
        return new Pt(x1, y1);
    }

    @Override
    public void setLocation(double x, double y) {
        if (x != 0 || y != 0) {
            double dx = x2 - x1;
            double dy = y2 - y1;
            x1 = x;
            y1 = y;
            x2 = x1 + dx;
            y2 = y1 + dy;
            change();
        }
    }

    @Override
    public void clearLocation() {
        if (x1 != 0 || y1 != 0) {
            double offx = x2 - x1;
            double offy = y2 - y1;
            x1 = 0;
            y1 = 0;
            x2 = offx;
            y2 = offy;
            if (x2 < 0 || y2 < 0) {
                x2 = 0;
                y2 = 0;
                x1 = -offx;
                y1 = -offy;
            }
            change();
        }
    }

    @Override
    public Vectors copy(AffineTransform xform) {
        double[] pts = new double[]{
            x1, y1, x2, y2
        };
        xform.transform(pts, 0, pts, 0, 2);
        x1 = pts[0];
        y1 = pts[1];
        x2 = pts[2];
        y2 = pts[3];
        Line result = new Line(x1, y1, x2, y2);
        result.rev = rev + 1;
        return result;
    }

    @Override
    public int[] getVirtualControlPointIndices() {
        return EMPTY_INT;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        }
        boolean result = o instanceof Line;
        if (result) {
            Line l = (Line) o;
            result = l.x1 == x1 && l.x2 == x2 && l.y1 == y1 && l.y2 == y2;
        }
        return result;
    }

    @Override
    public int hashCode() {
        long bits = doubleToLongBits(x1)
                + (983 * doubleToLongBits(y1))
                + (4_003 * doubleToLongBits(x2)
                + (39 * doubleToLongBits(y2)));
        return ((int) bits) ^ ((int) (bits >> 32));
    }

    @Override
    public void setControlPointLocation(int pointIndex, Pt location) {
        switch (pointIndex) {
            case 0:
                if (x1 != location.x || y1 != location.y) {
                    x1 = location.x;
                    y1 = location.y;
                    change();
                }
                break;
            case 1:
                if (x2 != location.x || y2 != location.y) {
                    x2 = location.x;
                    y2 = location.y;
                    change();
                }
                break;
            default:
                throw new IllegalArgumentException(Integer.toString(pointIndex));
        }
    }

    @Override
    public Rectangle getBounds() {
        Rectangle r = new Rectangle();
        r.setFrameFromDiagonal(x1, y1, x2, y2);
        return r;
    }
}
