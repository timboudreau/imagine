/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.elements;

import com.mastfrog.util.collections.IntSet;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.Fillable;
import net.java.dev.imagine.api.vector.Strokable;
import net.java.dev.imagine.api.vector.Vectors;
import net.java.dev.imagine.api.vector.Versioned;
import net.java.dev.imagine.api.vector.Volume;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import net.java.dev.imagine.api.vector.util.Pt;
import com.mastfrog.geometry.EqLine;
import com.mastfrog.geometry.EqPointDouble;
import com.mastfrog.geometry.Rhombus;
import com.mastfrog.geometry.util.GeometryUtils;

/**
 *
 * @author Tim Boudreau
 */
public final class RhombusWrapper implements Strokable, Fillable,
        Volume, Adjustable, Vectors, Versioned {

    private Rhombus rhombus;
    private int rev = 0;
    private boolean fill;

    public RhombusWrapper(Rhombus rhombus, boolean fill) {
        this.rhombus = rhombus;
        this.fill = fill;
    }

    public RhombusWrapper(Rhombus rhombus) {
        this.rhombus = rhombus;
    }

    public RhombusWrapper(double cx, double cy, double rx, double ry, double rot) {
        this(new Rhombus(cx, cy, rx, ry, rot));
    }

    private void change() {
        rev++;
    }

    public RhombusWrapper copy() {
        RhombusWrapper result = new RhombusWrapper(new Rhombus(rhombus), fill);
        result.rev = rev;
        return result;
    }

    @Override
    public void draw(Graphics2D g) {
        g.draw(rhombus);
    }

    @Override
    public Shape toShape() {
        return rhombus;
    }

    @Override
    public Pt getLocation() {
        return new Pt(rhombus.centerX(), rhombus.centerY());
    }

    @Override
    public void setLocation(double x, double y) {
        rhombus.setCenter(x, y);
    }

    @Override
    public void clearLocation() {
        rhombus.setCenter(0, 0);
    }

    @Override
    public double centerX() {
        return rhombus.centerX();
    }

    @Override
    public double centerY() {
        return rhombus.centerY();
    }

    public double radiusX() {
        return rhombus.getXRadius();
    }

    public double radiusY() {
        return rhombus.getYRadius();
    }

    public double rotation() {
        return rhombus.rotation();
    }

    private Rhombus transform(Rhombus r, AffineTransform xform) {
        EqPointDouble center = new EqPointDouble(r.centerX(), r.centerY());
        EqPointDouble top = r.top();
        EqPointDouble right = r.right();
        double[] pts = new double[]{
            center.x,
            center.y,
            top.x,
            top.y,
            right.x,
            right.y
        };
        xform.transform(pts, 0, pts, 0, 3);
        double oldRot = new EqLine(center, top).angle();
        EqLine xLine = new EqLine(pts[0], pts[1], pts[2], pts[3]);
        double newRot = xLine.angle();
        double newYRadius = Point2D.distance(pts[0], pts[1], pts[2], pts[3]);
        double newXRadius = Point2D.distance(pts[0], pts[1], pts[4], pts[5]);
        double rot = rhombus.rotation() + (newRot - oldRot);
        return new Rhombus(pts[0], pts[1], newXRadius, newYRadius, rot);
    }

    @Override
    public RhombusWrapper copy(AffineTransform transform) {
        Rhombus rhom = transform(rhombus, transform);
        RhombusWrapper result = new RhombusWrapper(rhom, fill);
        result.rev = rev + 1;
        return result;
    }

    @Override
    public void applyTransform(AffineTransform xform) {
        if (xform.isIdentity()) {
            return;
        }
        rhombus = transform(rhombus, xform);
        change();
    }

    @Override
    public boolean canApplyTransform(AffineTransform xform) {
        if (xform.getType() == AffineTransform.TYPE_FLIP) {
            return false;
        }
        return true;
    }

    @Override
    public Rectangle getBounds() {
        return rhombus.getBounds();
    }

    @Override
    public void paint(Graphics2D g) {
        g.fill(rhombus);
    }

    @Override
    public void applyScale(double scale) {
        rhombus.setXRadius(rhombus.getXRadius() * scale);
        rhombus.setYRadius(rhombus.getYRadius() * scale);
    }

    @Override
    public Runnable restorableSnapshot() {
        Rhombus rhom = new Rhombus(rhombus);
        int oldRev = rev;
        return () -> {
            rev = oldRev;
            rhombus = rhom;
        };
    }

    @Override
    public void fill(Graphics2D g) {
        g.fill(rhombus);
    }

    @Override
    public boolean isFill() {
        return fill;
    }

    @Override
    public int getControlPointCount() {
        return 5;
    }

    @Override
    public void getControlPoints(double[] xy) {
        xy[0] = rhombus.centerX();
        xy[1] = rhombus.centerY();
        List<? extends EqPointDouble> pts = rhombus.points();
        for (int i = 0; i < pts.size(); i++) {
            EqPointDouble pt = pts.get(i);
            xy[(i * 2) + 2] = pt.x;
            xy[(i * 2) + 3] = pt.y;
        }
    }

    @Override
    public int[] getVirtualControlPointIndices() {
        return new int[]{1, 2, 3, 4};
    }

    @Override
    public IntSet virtualControlPointIndices() {
        return IntSet.of(1, 2, 3, 4);
    }

    @Override
    public boolean isVirtualControlPoint(int index) {
        return index > 0;
    }

    @Override
    public void setControlPointLocation(int pointIndex, Pt location) {
        Point2D p = location.toPoint2D();
        switch (pointIndex) {
            case 0:
                if (location.x != rhombus.centerX() || location.y != rhombus.centerY()) {
                    rhombus.setCenter(p);
                    change();
                }
                break;
            case 1:
            case 3:
                double dist2 = p.distance(rhombus.centerX(), rhombus.centerY());
                if (dist2 != rhombus.getYRadius()) {
                    rhombus.setYRadius(dist2);
                    change();
                }
                break;
            case 2:
            case 4:
                double dist = p.distance(rhombus.centerX(), rhombus.centerY());
                if (dist != rhombus.getYRadius()) {
                    rhombus.setXRadius(dist);
                    change();
                }
                break;
            default:
                throw new IndexOutOfBoundsException("Point index > 5: " + pointIndex);
        }
    }

    @Override
    public ControlPointKind[] getControlPointKinds() {
        return new ControlPointKind[]{ControlPointKind.PHYSICAL_POINT,
            ControlPointKind.RADIUS, ControlPointKind.RADIUS,
            ControlPointKind.RADIUS, ControlPointKind.RADIUS};
    }

    @Override
    public int rev() {
        return rev;
    }

    @Override
    public double cumulativeLength() {
        double result = 0;
        for (EqLine ln : rhombus.lines()) {
            result += ln.length();
        }
        return result;
    }

    @Override
    public void addToBounds(Rectangle2D bds) {
        rhombus.addToBounds(bds);
    }

    @Override
    public Set<ControlPointKind> availablePointKinds(int point) {
        return EnumSet.of(ControlPointKind.PHYSICAL_POINT, ControlPointKind.RADIUS);
    }

    @Override
    public String toString() {
        return rhombus.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof RhombusWrapper) {
            RhombusWrapper rho = (RhombusWrapper) o;
            return GeometryUtils.isSamePoint(rho.centerX(), rho.centerY(), centerX(), centerY())
                    && GeometryUtils.isSameCoordinate(rho.radiusX(), radiusX())
                    && GeometryUtils.isSameCoordinate(rho.radiusY(), radiusY())
                    && GeometryUtils.isSameCoordinate(rho.rotation(), rotation())
                    ;
        }
        return false;
    }

    @Override
    public int hashCode() {
        long hash = 7;
        hash = hash + (7 * Double.doubleToLongBits(centerX()));
        hash = hash + (7 * Double.doubleToLongBits(centerY()));
        hash = hash + (7 * Double.doubleToLongBits(radiusX()));
        hash = hash + (7 * Double.doubleToLongBits(radiusY()));
        hash = hash + (7 * Double.doubleToLongBits(rotation()));
        return (int) (hash ^ (hash << 32));
    }

}
