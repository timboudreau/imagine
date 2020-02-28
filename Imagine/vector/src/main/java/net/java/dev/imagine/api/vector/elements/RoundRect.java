/*
 * RoundRect.java
 *
 * Created on September 27, 2006, 6:36 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.elements;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import static java.lang.Double.doubleToLongBits;
import java.util.Arrays;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.Fillable;
import net.java.dev.imagine.api.vector.Strokable;
import net.java.dev.imagine.api.vector.Vector;
import net.java.dev.imagine.api.vector.Volume;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import net.java.dev.imagine.api.vector.util.Pt;

/**
 *
 * @author Tim Boudreau
 */
public class RoundRect implements Vector, Volume, Adjustable, Fillable, Strokable {

    public long serialVersionUID = 39_201L;
    public double aw;
    public double ah;
    public double x;
    public double y;
    public double w;
    public double h;
    public boolean fill;

    public RoundRect(double x, double y, double w, double h, double aw, double ah, boolean fill) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.aw = aw;
        this.ah = ah;
        this.fill = fill;
    }

    public Runnable restorableSnapshot() {
        double ox = x;
        double oy = y;
        double ow = w;
        double oh = h;
        double oaw = aw;
        double oah = ah;
        return () -> {
            x = ox;
            y = oy;
            h = oh;
            w = ow;
            aw = oaw;
            ah = oah;
        };
    }

    @Override
    public void translate(double x, double y) {
        this.x += x;
        this.y += y;
    }

    public double getArcHeight() {
        return ah;
    }

    public double getArcWidth() {
        return aw;
    }

    public void setArcHeight(double arcHeight) {
        this.ah = Math.abs(arcHeight);
    }

    public void setArcWidth(double val) {
        this.aw = Math.abs(val);
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double width() {
        return w;
    }

    public double height() {
        return h;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setWidth(double w) {
        this.w = w;
    }

    public void setHeight(double h) {
        this.h = h;
    }

    @Override
    public String toString() {
        return "RoundRect " + x + ", " + y + ", " + w
                + ", " + h + " fill: " + fill;
    }

    @Override
    public Shape toShape() {
        return new RoundRectangle2D.Double(x, y, w, h, aw, ah);
    }

    @Override
    public void paint(Graphics2D g) {
        if (fill) {
            fill(g);
        } else {
            draw(g);
        }
    }

//    public void getControlPoints(double[] xy) {
//        double halfh = h / 2;
//        double halfw = w / 2;
//        xy[0] = x + halfw;
//        xy[1] = y;
//        xy[2] = x + w;
//        xy[3] = y + halfh;
//        xy[4] = x + halfw;
//        xy[5] = y + h;
//        xy[6] = x;
//        xy[7] = y + halfw;
//    }
//
    @Override
    public RoundRect copy() {
        return new RoundRect(x, y, w, h, aw, ah, fill);
    }

    @Override
    public void addToBounds(Rectangle2D bds) {
        bds.add(x, y);
        bds.add(x + w, y + h);
    }

    @Override
    public void getBounds(Rectangle2D r) {
        double wid = w;
        double hi = h;
        double xx, yy, ww, hh;
        if (wid < 0) {
            wid = -wid;
            xx = x + w;
        } else {
            xx = x;
        }
        ww = wid;
        if (hi < 0) {
            hi = -hi;
            yy = y + h;
        } else {
            yy = y;
        }
        hh = hi;
        r.setRect(xx, yy, ww, hh);
    }

    @Override
    public int getControlPointCount() {
        return 4;
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
    public void draw(Graphics2D g) {
        g.draw(toShape());
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
        setLocation(0D, 0D);
    }

    @Override
    public Vector copy(AffineTransform transform) {
        double[] pts = new double[]{
            x, y, x + w, y + h,};
        transform.transform(pts, 0, pts, 0, 2);
        return new RoundRect(pts[0], pts[1], pts[2] - pts[0],
                pts[3] - pts[1], aw, ah, fill);
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
        boolean result = o instanceof RoundRect;
        if (result) {
            RoundRect r = (RoundRect) o;
            result = r.h == h && r.w == w && r.x == x && r.y == y
                    && r.ah == ah && r.aw == aw;
        }
        return result;
    }

    @Override
    public int hashCode() {
        long bits = 2_701 * ((doubleToLongBits(x) * 83)
                + (doubleToLongBits(y) * 431)
                + (doubleToLongBits(w) * 5)
                + (doubleToLongBits(h) * 971)
                + (doubleToLongBits(aw) * 5_843)
                + (doubleToLongBits(ah) * 7_451));
        return (((int) bits) ^ ((int) (bits >> 32)));
    }

    @Override
    public void getControlPoints(double[] xy) {
        xy[0] = x;
        xy[1] = y;

        xy[2] = x;
        xy[3] = y + h;

        xy[4] = x + w;
        xy[5] = y + h;

        xy[6] = x + w;
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
                h += x - pt.x;
                w += y - pt.y;
                x = pt.x;
                y = pt.y;
                break;
            case 1:
                w += x - pt.x;
                x = pt.x;
                h = pt.y - y;
                break;
            case 2:
                w = pt.x - x;
                h = pt.y - y;
                break;
            case 3:
                w = pt.x - x;
                h += y - pt.y;
                y = pt.y;
                break;
            default:
                throw new IllegalArgumentException(Integer.toString(pointIndex));
        }
        renormalize();
    }

    private void renormalize() {
        if (w < 0) {
            double ww = w;
            x += w;
            w *= -1;
        }
        if (h < 0) {
            y += h;
            h *= -1;
        }
    }

    @Override
    public void applyTransform(AffineTransform xform) {
        Point2D.Double a = new Point2D.Double(x, y);
        Point2D.Double b = new Point2D.Double(x + w, y + h);
        xform.transform(a, a);
        xform.transform(b, b);
        x = a.x;
        y = a.y;
        w = b.x - a.x;
        h = b.y - a.y;
    }

    @Override
    public java.awt.Rectangle getBounds() {
        Rectangle2D.Double bds = new Rectangle2D.Double();
        getBounds(bds);
        return bds.getBounds();
    }
}
