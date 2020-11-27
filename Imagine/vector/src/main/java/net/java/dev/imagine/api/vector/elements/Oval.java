/*
 * Oval.java
 *
 * Created on September 27, 2006, 6:33 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.elements;

import com.mastfrog.util.collections.IntSet;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.Fillable;
import net.java.dev.imagine.api.vector.Strokable;
import net.java.dev.imagine.api.vector.Versioned;
import net.java.dev.imagine.api.vector.Volume;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import net.java.dev.imagine.api.vector.util.Pt;
import com.mastfrog.geometry.Circle;
import com.mastfrog.geometry.util.GeometryUtils;
import net.java.dev.imagine.api.vector.Vectors;

/**
 *
 * @author Tim Boudreau
 */
public class Oval implements Strokable, Fillable, Adjustable, Volume, Vectors, Versioned {

    private static final long serialVersionUID = 232_354_194L;
    private double x;
    private double y;
    private double width;
    private double height;
    public boolean fill;
    private transient int rev;

    public Oval(double x, double y, double width, double height, boolean fill) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.fill = fill;
    }

    @Override
    public int rev() {
        return rev;
    }

    private void change() {
        rev++;
    }

    @Override
    public double cumulativeLength() {
        return GeometryUtils.shapeLength(toShape());
    }

    @Override
    public Runnable restorableSnapshot() {
        double ox = x;
        double oy = y;
        double ow = width;
        double oh = height;
        int oldRev = rev;
        return () -> {
            rev = oldRev;
            x = ox;
            y = oy;
            height = oh;
            width = ow;
        };
    }

    @Override
    public void collectSizings(SizingCollector c) {
        c.dimension(height, true, 1, 3);
        c.dimension(width, false, 0, 1);
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }

    public void setX(double x) {
        if (x != this.x) {
            this.x = x;
            change();
        }
    }

    public void setY(double y) {
        if (y != this.y) {
            this.y = y;
            change();
        }
    }

    public void setWidth(double w) {
        if (w != this.width) {
            width = w;
            change();
        }
    }

    public void setHeight(double h) {
        if (h != height) {
            height = h;
            change();
        }
    }

    @Override
    public void translate(double x, double y) {
        if (x != 0 || y != 0) {
            this.x += x;
            this.y += y;
            change();
        }
    }

    @Override
    public String toString() {
        return "Oval " + x + ", " + y + ", " + width
                + ", " + height + " fill: " + fill;
    }

    @Override
    public Shape toShape() {
        if (width == height) {
            double radius = width / 2;
            return new Circle(x + radius, y + radius, radius);
        }
        return new Ellipse2D.Double(x, y, width, height);
    }

    @Override
    public void applyTransform(AffineTransform xform) {
        if (xform == null || xform.isIdentity()) {
            return;
        }
        Point2D.Double a = new Point2D.Double(x, y);
        Point2D.Double b = new Point2D.Double(x + width, y + height);
        xform.transform(a, a);
        xform.transform(b, b);
        x = a.x;
        y = a.y;
        width = b.x - a.x;
        height = b.y - a.y;
        change();
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
    public void addToBounds(Rectangle2D bds) {
        if (bds.isEmpty()) {
            getBounds(bds);
        } else {
            bds.add(x, y);
            bds.add(x + width, y + height);
        }
    }

    @Override
    public void getBounds(Rectangle2D r) {
        double wid = width;
        double hi = height;
        double xx, yy, ww, hh;
        if (wid < 0) {
            wid = -wid;
            xx = x + width;
        } else {
            xx = x;
        }
        ww = wid;
        if (hi < 0) {
            hi = -hi;
            yy = y + height;
        } else {
            yy = y;
        }
        hh = hi;
        r.setRect(xx, yy, ww, hh);
    }

    @Override
    public boolean isFill() {
        return fill;
    }

    @Override
    public int getControlPointCount() {
        return 4;
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
    public Oval copy() {
        Oval result = new Oval(x, y, width, height, fill);
        result.rev = rev;
        return result;
    }

    @Override
    public Pt getLocation() {
        return new Pt(x, y);
    }

    @Override
    public void setLocation(double x, double y) {
        if (x != this.x || y != this.y) {
            this.x = x;
            this.y = y;
            change();
        }
    }

    @Override
    public void clearLocation() {
        setLocation(0, 0);
    }

    @Override
    public Oval copy(AffineTransform xform) {
        double[] pts = new double[]{
            x, y, x + width, y + height,};
        xform.transform(pts, 0, pts, 0, 2);
        Oval result = new Oval(pts[0], pts[1],
                pts[2] - pts[0], pts[3] - pts[1], fill);
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
        boolean result = o instanceof Oval;
        if (result) {
            Oval v = (Oval) o;
            result = v.x == x && v.y == y && v.width == width && v.height == height;
        }
        return result;
    }

    @Override
    public int hashCode() {
        long bits = java.lang.Double.doubleToLongBits(x)
                * 13;
        bits += java.lang.Double.doubleToLongBits(y) * 431;
        bits += java.lang.Double.doubleToLongBits(width) * 5;
        bits += java.lang.Double.doubleToLongBits(height) * 971;
        return (((int) bits) ^ ((int) (bits >> 32)));
    }

    @Override
    public void getControlPoints(double[] xy) {
        assert xy.length >= 8 : "Array too small";
        xy[0] = x;
        xy[1] = y;

        xy[2] = x;
        xy[3] = y + height;

        xy[4] = x + width;
        xy[5] = y + height;

        xy[6] = x + width;
        xy[7] = y;
    }

    @Override
    public ControlPointKind[] getControlPointKinds() {
        ControlPointKind[] kinds = new ControlPointKind[4];
        Arrays.fill(kinds, ControlPointKind.PHYSICAL_POINT);
        return kinds;
    }

    @Override
    public void setControlPointLocation(int pointIndex, Pt pt) {
        switch (pointIndex) {
            case 0:
                height += x - pt.x;
                width += y - pt.y;
                x = pt.x;
                y = pt.y;
                break;
            case 1:
                width += x - pt.x;
                x = pt.x;
                height = pt.y - y;
                break;
            case 2:
                width = pt.x - x;
                height = pt.y - y;
                break;
            case 3:
                width = pt.x - x;
                height += y - pt.y;
                y = pt.y;
                break;
            default:
                throw new IllegalArgumentException(Integer.toString(pointIndex));
        }
        change();
        renormalize();
    }

    private void renormalize() {
        if (width < 0) {
            x += width;
            width *= -1;
        }
        if (height < 0) {
            y += height;
            height *= -1;
        }
    }

    @Override
    public java.awt.Rectangle getBounds() {
        Rectangle2D.Double bds = new Rectangle2D.Double();
        getBounds(bds);
        return bds.getBounds();
    }

    @Override
    public IntSet virtualControlPointIndices() {
        return IntSet.EMPTY;
    }
}
