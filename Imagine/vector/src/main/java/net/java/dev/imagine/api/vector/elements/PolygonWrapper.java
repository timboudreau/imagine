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
import static java.awt.geom.PathIterator.SEG_LINETO;
import static java.awt.geom.PathIterator.SEG_MOVETO;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.Fillable;
import net.java.dev.imagine.api.vector.Mutable;
import net.java.dev.imagine.api.vector.Normalizable;
import net.java.dev.imagine.api.vector.Strokable;
import net.java.dev.imagine.api.vector.Vectors;
import net.java.dev.imagine.api.vector.Versioned;
import net.java.dev.imagine.api.vector.Volume;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import net.java.dev.imagine.api.vector.util.Pt;
import com.mastfrog.geometry.EnhancedShape;
import com.mastfrog.geometry.EqPointDouble;
import com.mastfrog.geometry.Polygon2D;
import com.mastfrog.geometry.path.PathElementKind;
import com.mastfrog.geometry.util.GeometryStrings;
import com.mastfrog.geometry.util.PooledTransform;

/**
 *
 * @author Tim Boudreau
 */
public class PolygonWrapper implements Strokable, Fillable,
        Volume, Adjustable, Vectors, Mutable, Versioned, Normalizable {

    private Polygon2D poly;
    private int rev;
    private boolean fill;

    public PolygonWrapper(PolygonWrapper other) {
        this.poly = new Polygon2D(other.poly);
        this.rev = other.rev;
        this.fill = other.fill;
    }

    public PolygonWrapper(double[] pointsArray) {
        this(new Polygon2D(pointsArray));
    }

    public PolygonWrapper(int[] xpoints, int[] ypoints, int npoints) {
        this(xpoints, ypoints, npoints, false);
    }

    public PolygonWrapper(int[] xpoints, int[] ypoints, int npoints, boolean fill) {
        this(new Polygon2D(xpoints, ypoints, npoints), fill);
    }

    public PolygonWrapper(java.awt.Polygon poly) {
        this(new Polygon2D(poly), false);
    }

    public PolygonWrapper(java.awt.Polygon poly, boolean fill) {
        this(new Polygon2D(poly), fill);
    }

    public PolygonWrapper(Polygon2D poly) {
        this(poly, false);
    }

    public PolygonWrapper(Polygon2D poly, boolean fill) {
        this.poly = poly;
        this.fill = fill;
    }

    private void change() {
        rev++;
    }

    @Override
    public void draw(Graphics2D g) {
        g.draw(poly);
    }

    @Override
    public Shape toShape() {
        return poly;
    }

    @Override
    public Pt getLocation() {
        Rectangle2D pt = poly.getBounds2D();
        return new Pt(pt.getX(), pt.getY());
    }

    @Override
    public void setLocation(double x, double y) {
        Pt old = getLocation();
        double offX = old.x - x;
        double offY = old.y - y;
        if (offX != 0 || offY != 0) {
            translate(offX, offY);
            change();
        }
    }

    @Override
    public void clearLocation() {
        setLocation(0, 0);
    }

    @Override
    public PolygonWrapper copy() {
        return new PolygonWrapper(this);
    }

    @Override
    public PolygonWrapper copy(AffineTransform transform) {
        PolygonWrapper result = new PolygonWrapper(this);
        result.applyTransform(transform);
        return result;
    }

    @Override
    public Rectangle getBounds() {
        return poly.getBounds();
    }

    @Override
    public void paint(Graphics2D g) {
        g.fill(poly);
    }

    @Override
    public void translate(double x, double y) {
        if (x != 0 || y != 0) {
            PooledTransform.withTranslateInstance(x, y, this::applyTransform);
        }
    }

    @Override
    public boolean is(Class<?> type) {
        if (type == EnhancedShape.class) {
            return true;
        }
        return Strokable.super.is(type);
    }

    @Override
    public <T> T as(Class<T> type) {
        if (type == EnhancedShape.class) {
            return type.cast(poly);
        }
        return Strokable.super.as(type);
    }

    @Override
    public void getBounds(Rectangle2D dest) {
        dest.setFrame(0, 0, 0, 0);
        poly.addToBounds(dest);
    }

    @Override
    public void addToBounds(Rectangle2D bds) {
        poly.addToBounds(bds);
    }

    @Override
    public void applyTransform(AffineTransform xform) {
        if (xform != null && !xform.isIdentity()) {
            poly.applyTransform(xform);
            change();
        }
    }

    @Override
    public Runnable restorableSnapshot() {
        Polygon2D copy = new Polygon2D(poly);
        int oldRev = rev;
        return () -> {
            poly = copy;
            rev = oldRev;
        };
    }

    @Override
    public void fill(Graphics2D g) {
        g.fill(poly);
    }

    @Override
    public boolean isFill() {
        return fill;
    }

    @Override
    public int getControlPointCount() {
        return poly.pointCount();
    }

    @Override
    public void getControlPoints(double[] xy) {
        double[] pts = poly.pointsArray();
        System.arraycopy(pts, 0, xy, 0, pts.length);
    }

    @Override
    public int[] getVirtualControlPointIndices() {
        return new int[0];
    }

    @Override
    public void setControlPointLocation(int pointIndex, Pt location) {
        if (poly.setPoint(pointIndex, location.x, location.y)) {
            change();
        }
    }

    @Override
    public ControlPointKind[] getControlPointKinds() {
        ControlPointKind[] kinds = new ControlPointKind[poly.pointCount()];
        Arrays.fill(kinds, ControlPointKind.LINE_TO_DESTINATION);
        return kinds;
    }

    @Override
    public boolean delete(int pointIndex) {
        poly.deletePoint(pointIndex);
        change();
        return true;
    }

    @Override
    public boolean insert(double x, double y, int index, int kind) {
        switch (kind) {
            case SEG_MOVETO:
            case SEG_LINETO:
                break;
            default:
                throw new IllegalArgumentException("Invalid kind " + PathElementKind.of(kind));
        }
        poly.insertPoint(index, x, y);
        change();
        return true;
    }

    @Override
    public int getPointIndexNearest(double x, double y) {
        double[] pts = poly.pointsArray();
        double bestDist = Double.MAX_VALUE;
        int bestIndex = -1;
        for (int i = 0; i < pts.length; i += 2) {
            double xx = pts[i];
            double yy = pts[i + 1];
            if (xx == x && yy == y) {
                return i / 2;
            }
            double dist = Point2D.distance(x, y, xx, yy);
            if (dist < bestDist) {
                bestIndex = i / 2;
                bestDist = dist;
            }
        }
        return bestIndex;
    }

    @Override
    public int rev() {
        return rev;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(9 + poly.pointCount() * 10)
                .append("Polygon(");
        double[] pts = poly.pointsArray();
        for (int i = 0; i < pts.length; i += 2) {
            sb.append(GeometryStrings.toString(pts[i], pts[i + 1]));
            if (i != pts.length - 2) {
                sb.append(" / ");
            }
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(poly.pointsArray());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof PolygonWrapper) {
            PolygonWrapper p = (PolygonWrapper) o;
            return Arrays.equals(pointsArray(), p.pointsArray());
        }
        return false;
    }

    public double[] pointsArray() {
        return poly.pointsArray();
    }

    @Override
    public boolean isNormalized() {
        return poly.isNormalized();
    }

    @Override
    public void normalize() {
        if (!isNormalized()) {
            poly.normalize();
            change();
        }
    }

    @Override
    public IntSet virtualControlPointIndices() {
        return IntSet.EMPTY;
    }

    public double centerX() {
        double[] d = poly.pointsArray();
        double result = 0;
        for (int i = 0; i < d.length; i += 2) {
            result += d[i];
        }
        return result / (d.length / 2);
    }

    public double centerY() {
        double[] d = poly.pointsArray();
        double result = 0;
        for (int i = 0; i < d.length; i += 2) {
            result += d[i + 1];
        }
        return result / (d.length / 2);
    }

    public EqPointDouble center() {
        double[] d = poly.pointsArray();
        double resultX = 0;
        double resultY = 0;
        for (int i = 0; i < d.length; i += 2) {
            resultX += d[i];
            resultY += d[i + 1];
        }
        int pointCount = d.length / 2;
        return new EqPointDouble(resultX / pointCount, resultY / pointCount);
    }

}
