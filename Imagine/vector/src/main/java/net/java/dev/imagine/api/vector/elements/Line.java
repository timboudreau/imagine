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
import net.java.dev.imagine.api.vector.Vector;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import net.java.dev.imagine.api.vector.util.Pt;

/**
 *
 * @author Tim Boudreau
 */
public class Line implements Strokable, Adjustable, Vector {

    public double x1;
    public double x2;
    public double y1;
    public double y2;
    public long serialVersionUID = 23_923_214L;

    public Line(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    @Override
    public void translate(double x, double y) {
        this.x1 += x;
        this.y1 += y;
        this.x2 += x;
        this.y2 += y;
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
        this.x1 = x1;
    }

    public void setX2(double x2) {
        this.x2 = x2;
    }

    public void setY1(double y1) {
        this.y1 = y1;
    }

    public void setY2(double y2) {
        this.y2 = y2;
    }

    public double length() {
        return Point2D.distance(x1, y1, x2, y2);
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
        Point2D.Double a = new Point2D.Double(x1, y1);
        Point2D.Double b = new Point2D.Double(x2, y2);
        xform.transform(a, a);
        xform.transform(b, b);
        x1 = a.x;
        x2 = b.x;
        y1 = a.y;
        y2 = b.y;
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
        r.x = (int) Math.floor(x1);
        r.y = (int) Math.floor(y1);
        r.width = (int) Math.ceil(x2 - x1);
        r.height = (int) Math.ceil(y2 - y1);
    }

    @Override
    public void getBounds(Rectangle2D.Double r) {
        double wid = x2 - x1;
        double hi = y2 - y1;
        double x, y, w, h;
        if (wid < 0) {
            wid = -wid;
            x = x2;
        } else {
            x = x1;
        }
        w = wid;
        if (hi < 0) {
            hi = -hi;
            y = y2;
        } else {
            y = y1;
        }
        h = hi;
        r.setRect(x, y, w, h);
    }

    @Override
    public void draw(Graphics2D g) {
        paint(g);
    }

    @Override
    public Line copy() {
        return new Line(x1, y1, x2, y2);
    }

    @Override
    public Pt getLocation() {
        return new Pt(x1, y1);
    }

    @Override
    public void setLocation(double x, double y) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        x1 = x;
        y1 = y;
        x2 = x1 + dx;
        y2 = y1 + dy;
    }

    @Override
    public void clearLocation() {
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
    }

    @Override
    public Vector copy(AffineTransform xform) {
        double[] pts = new double[]{
            x1, y1, x2, y2
        };
        xform.transform(pts, 0, pts, 0, 2);
        x1 = pts[0];
        y1 = pts[1];
        x2 = pts[2];
        y2 = pts[3];
        return new Line(x1, y1, x2, y2);
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
                x1 = location.x;
                y1 = location.y;
                break;
            case 1:
                x2 = location.x;
                y2 = location.y;
                break;
            default:
                throw new IllegalArgumentException(Integer.toString(pointIndex));
        }
    }

    @Override
    public Rectangle getBounds() {
        Rectangle2D.Double bds = new Rectangle2D.Double();
        getBounds(bds);
        return bds.getBounds();
    }
}
