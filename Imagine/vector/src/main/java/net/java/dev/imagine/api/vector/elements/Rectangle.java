/*
 * Rectangle.java
 *
 * Created on September 27, 2006, 6:22 PM
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
public final class Rectangle implements Strokable, Fillable, Volume, Adjustable {

    public long serialVersionUID = 2_354_354L;
    public double h;
    public double x;
    public double y;
    public double w;
    public boolean fill;

    public Rectangle(double x, double y, double w, double h, boolean fill) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.fill = fill;
    }

    public Runnable restorableSnapshot() {
        double ox = x;
        double oy = y;
        double ow = w;
        double oh = h;
        return () -> {
            x = ox;
            y = oy;
            h = oh;
            w = ow;
        };
    }

    @Override
    public void translate(double x, double y) {
        this.x += x;
        this.y += y;
    }

    @Override
    public String toString() {
        return "Rectangle " + x + ", " + y + ", " + w
                + ", " + h + " fill: " + fill;
    }

    @Override
    public Shape toShape() {
        return new Rectangle2D.Double(x, y, w, h);
    }

    public String toSvgFragment(Map<String, String> otherAttributes) {
        //PENDING:  Do this for other primitives
        if (!otherAttributes.keySet().containsAll(requiredAttributes())) {
            HashSet<String> set = new HashSet<>(otherAttributes.keySet());
            set.removeAll(requiredAttributes());
            throw new IllegalArgumentException("Missing attributes " + set);
        }
        StringBuilder bld = new StringBuilder("<");
        bld.append(getSvgName()).append(" x=\"");
        bld.append(x);
        bld.append("\" y=\"");
        bld.append(y);
        bld.append("\" width=\"");
        bld.append(w);
        bld.append("\" height=\"");
        bld.append(h);
        bld.append("\" fill=\"");
        bld.append(otherAttributes.get("fill"));
        bld.append("\" stroke=\"");
        bld.append(otherAttributes.get("stroke"));
        bld.append("\"/>");
        return bld.toString();
    }

    String getSvgName() {
        return "rect";
    }

    public Set<String> requiredAttributes() {
        return new HashSet<>(Arrays.asList("fill", "stroke"));
    }

    @Override
    public boolean isFill() {
        return fill;
    }

    @Override
    public boolean equals(Object o) {
        boolean result = o instanceof Rectangle;
        if (result) {
            Rectangle r = (Rectangle) o;
            result = r.h == h && r.w == w && r.x == x && r.y == y;
        }
        return result;
    }

    @Override
    public int hashCode() {
        long bits = java.lang.Double.doubleToLongBits(x)
                * 37;
        bits += java.lang.Double.doubleToLongBits(y) * 431;
        bits += java.lang.Double.doubleToLongBits(w) * 5;
        bits += java.lang.Double.doubleToLongBits(h) * 971;
        return (((int) bits) ^ ((int) (bits >> 32)));
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
    public void paint(Graphics2D g) {
        if (fill) {
            fill(g);
        } else {
            draw(g);
        }
    }

    @Override
    public int getControlPointCount() {
        return 4;
    }

    @Override
    public void addToBounds(Rectangle2D bds) {
        if (bds.isEmpty()) {
            bds.setFrame(x, y, w, h);
        } else {
            bds.add(x, y);
            bds.add(x + w, y + h);
        }
    }

    @Override
    public void getBounds(Rectangle2D r) {
        r.setRect(x, y, w, h);
    }

    public Strokable createInverseFilledInstance() {
        return new Rectangle(x, y, w, h, !fill);
    }

    @Override
    public void draw(Graphics2D g) {
        g.draw(toShape());
    }

    @Override
    public Rectangle copy() {
        return new Rectangle(x, y, w, h, fill);
    }

    @Override
    public void fill(Graphics2D g) {
        g.fill(toShape());
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
        setLocation(0, 0);
    }

    @Override
    public Vector copy(AffineTransform transform) {
        double[] pts = new double[]{
            x, y, x + w, y + h,};
        transform.transform(pts, 0, pts, 0, 2);
        return new Rectangle(pts[0], pts[1],
                pts[2] - pts[0], pts[3] - pts[1], fill);
    }

    @Override
    public int[] getVirtualControlPointIndices() {
        return EMPTY_INT;
    }

    @Override
    public void getControlPoints(double[] xy) {
        assert xy.length >= 8 : "Array too small";
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
                renormalize();
                break;
            case 3:
                w = pt.x - x;
                h += y - pt.y;
                y = pt.y;
                renormalize();
                break;
            default:
                throw new IllegalArgumentException(Integer.toString(pointIndex));
        }
        renormalize();
    }

    @Override
    public ControlPointKind[] getControlPointKinds() {
        ControlPointKind[] kinds = new ControlPointKind[4];
        Arrays.fill(kinds, ControlPointKind.PHYSICAL_POINT);
        return kinds;
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
    public java.awt.Rectangle getBounds() {
        Rectangle2D.Double bds = new Rectangle2D.Double();
        getBounds(bds);
        return bds.getBounds();
    }

    @Override
    public void collectSizings(SizingCollector c) {
        c.dimension(h, true, 1, 3);
        c.dimension(w, false, 0, 1);
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
}
