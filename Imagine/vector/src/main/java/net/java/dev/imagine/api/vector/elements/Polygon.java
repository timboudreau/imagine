/*
 * Polygon.java
 *
 * Created on September 27, 2006, 6:17 PM
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
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.Fillable;
import net.java.dev.imagine.api.vector.Mutable;
import net.java.dev.imagine.api.vector.Strokable;
import net.java.dev.imagine.api.vector.Versioned;
import net.java.dev.imagine.api.vector.Volume;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import net.java.dev.imagine.api.vector.util.Pt;
import org.imagine.geometry.EnhancedShape;
import org.imagine.geometry.Polygon2D;
import net.java.dev.imagine.api.vector.Vectors;

/**
 *
 * @author Tim Boudreau
 */
public class Polygon implements Strokable, Fillable, Volume, Adjustable, Vectors, Mutable, Versioned {

    private static final long serialVersionUID = 1L;
    // XXX if this class winds up heavily used, rewrite it to
    // keep a single array of points - the only reason it is
    // like this is that it started out as a wrapper for int-based
    // java.awt.Polygon
    private double[] xpoints;
    private double[] ypoints;
    private boolean fill;
    private transient int rev;

    public Polygon(double[] xpoints, double[] ypoints, boolean fill) {
        if (xpoints.length != ypoints.length) {
            throw new IllegalArgumentException("Array sizes do not match: "
                    + xpoints.length + ", " + ypoints.length);
        }
        this.xpoints = xpoints;
        this.ypoints = ypoints;
        this.fill = fill;
    }

    public Polygon(int[] xpoints, int[] ypoints, int npoints, boolean fill) {
        this.xpoints = toDoubleArray(xpoints, npoints);
        this.ypoints = toDoubleArray(ypoints, npoints);
        assert npoints <= xpoints.length;
        assert ypoints.length >= npoints;
        assert npoints >= 0;
        this.fill = fill;
    }

    public Polygon(Polygon2D poly) {
        this(poly, true);
    }

    public Polygon(Polygon2D poly, boolean fill) {
        xpoints = new double[poly.pointCount()];
        ypoints = new double[poly.pointCount()];
        double[] data = poly.pointsArray();
        for (int i = 0; i < data.length; i += 2) {
            int ptix = i / 2;
            xpoints[ptix] = data[i];
            ypoints[ptix] = data[i + 1];
        }
    }

    public Polygon(java.awt.Polygon poly, boolean fill) {
        this(poly.xpoints, poly.ypoints, poly.npoints, fill);
    }

    private void change() {
        rev++;
    }

    public double[] xpoints() {
        return xpoints;
    }

    public double[] ypoints() {
        return ypoints;
    }

    private static double[] toDoubleArray(int[] ints, int count) {
        double[] result = new double[count];
        for (int i = 0; i < count; i++) {
            result[i] = ints[i];
        }
        return result;
    }

    @Override
    public synchronized Runnable restorableSnapshot() {
        double[] xp = Arrays.copyOf(xpoints, xpoints.length);
        double[] yp = Arrays.copyOf(ypoints, ypoints.length);
        int oldRev = rev;
        return () -> {
            if (xpoints.length != xp.length) {
                xpoints = xp;
                ypoints = yp;
            } else {
                System.arraycopy(xp, 0, xpoints, 0, xp.length);
                System.arraycopy(yp, 0, ypoints, 0, yp.length);
            }
            rev = oldRev;
        };
    }

    @Override
    public void translate(double x, double y) {
        int xx = (int) x;
        int yy = (int) y;
        if (xx == 0 && yy == 0) {
            return;
        }
        for (int i = 0; i < xpoints.length; i++) {
            xpoints[i] += xx;
            ypoints[i] += yy;
        }
        change();
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("Polygon ");
        for (int i = 0; i < xpoints.length; i++) {
            b.append('[');
            b.append(xpoints[i]);
            b.append(", ");
            b.append(ypoints[i]);
            b.append(']');
        }
        return b.toString();
    }

    @Override
    public Shape toShape() {
        return new Polygon2D(xpoints, ypoints);
    }

    @Override
    public <T> T as(Class<T> type) {
        if (EnhancedShape.class == type) {
            return type.cast(new Polygon2D(xpoints, ypoints));
        }
        return Strokable.super.as(type);
    }

    @Override
    public boolean is(Class<?> type) {
        if (EnhancedShape.class == type) {
            return true;
        }
        return Strokable.super.is(type);
    }

    @Override
    public boolean isFill() {
        return fill;
    }

    @Override
    public double cumulativeLength() {
        double result = 0;
        for (int i = 1; i < xpoints.length; i++) {
            double xp = xpoints[i - 1];
            double yp = ypoints[i - 1];
            double x = xpoints[i];
            double y = ypoints[i];
            result += Point2D.distance(xp, yp, x, y);
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (o == this) {
            return true;
        }
        boolean result = o instanceof Polygon;
        if (result) {
            Polygon p = (Polygon) o;
            result = p.xpoints.length == xpoints.length;
            if (result) {
                result &= Arrays.equals(p.xpoints, xpoints);
                result &= Arrays.equals(p.ypoints, ypoints);
            }
        }
        return result;
    }

    @Override
    public int hashCode() {
        int a = Arrays.hashCode(xpoints);
        int b = Arrays.hashCode(ypoints);
        return (a + 1) * (b + 1) + ((xpoints.length + 1) * 36_229);
    }

    @Override
    public void paint(Graphics2D g) {
        if (fill) {
            g.draw(toShape());
        } else {
            g.fill(toShape());
        }
    }

    @Override
    public int getControlPointCount() {
        return xpoints.length;
    }

    public Strokable create(int[] x, int[] y) {
        return new Polygon(x, y, x.length, fill);
    }

    @Override
    public void addToBounds(Rectangle2D bds) {
        if (xpoints.length == 0) {
            return;
        }
        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;
        for (int i = 0; i < xpoints.length; i++) {
            minX = Math.min(minX, xpoints[i]);
            minY = Math.min(minY, ypoints[i]);
            maxX = Math.max(maxX, xpoints[i]);
            maxY = Math.max(maxY, ypoints[i]);
        }
        if (bds.isEmpty()) {
            bds.setFrameFromDiagonal(minX, minY, maxX, maxY);
        } else {
            bds.add(minX, minY);
            bds.add(maxX, maxY);
        }
    }

    @Override
    public void getBounds(Rectangle2D r) {
        if (xpoints.length == 0) {
            return;
        }
        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;
        for (int i = 0; i < xpoints.length; i++) {
            minX = Math.min(minX, xpoints[i]);
            minY = Math.min(minY, ypoints[i]);
            maxX = Math.max(maxX, xpoints[i]);
            maxY = Math.max(maxY, ypoints[i]);
        }
        double width = maxX - minX;
        double height = maxY - minY;
        double x = minX;
        double y = minY;
        r.setRect(x, y, width, height);
    }

    public Strokable createInverseFilledInstance() {
        return new Polygon(xpoints, ypoints, !fill);
    }

    @Override
    public void draw(Graphics2D g) {
        g.draw(toShape());
    }

    @Override
    public Polygon copy() {
        return new Polygon(Arrays.copyOf(xpoints, xpoints.length),
                Arrays.copyOf(ypoints, ypoints.length), fill);
    }

    @Override
    public ControlPointKind[] getControlPointKinds() {
        ControlPointKind[] kinds = new ControlPointKind[xpoints.length];
        Arrays.fill(kinds, ControlPointKind.PHYSICAL_POINT);
        return kinds;
    }

    @Override
    public void applyTransform(AffineTransform xform) {
        double[] scratch = new double[2];
        for (int i = 0; i < xpoints.length; i++) {
            scratch[0] = xpoints[i];
            scratch[1] = ypoints[i];
            xform.transform(scratch, 0, scratch, 0, 1);
            xpoints[i] = scratch[0];
            ypoints[i] = scratch[1];
        }
        change();
    }

    @Override
    public void fill(Graphics2D g) {
        g.fill(toShape());
    }

    @Override
    public void getControlPoints(double[] xy) {
        assert xy.length <= xpoints.length * 2;
        for (int i = 0; i < xpoints.length; i += 2) {
            int pos = i / 2;
            xy[pos] = xpoints[i];
            xy[i + 1] = ypoints[i];
        }
    }

    @Override
    public Pt getLocation() {
        double minx = Double.MAX_VALUE;
        double miny = Double.MAX_VALUE;
        for (int i = 0; i < xpoints.length; i++) {
            minx = Math.min(minx, xpoints[i]);
            miny = Math.min(miny, ypoints[i]);
        }
        minx = minx == Double.MAX_VALUE ? 0 : minx;
        miny = miny == Double.MAX_VALUE ? 0 : miny;
        return new Pt(minx, miny);
    }

    @Override
    public void setLocation(double xx, double yy) {
        //XXX do all this double precision?
        int x = (int) xx;
        int y = (int) yy;
        double minY = Double.MAX_VALUE;
        double minX = Double.MAX_VALUE;
        for (int i = 0; i < xpoints.length; i++) {
            double ix = xpoints[i];
            double iy = ypoints[i];
            minX = Math.min(minX, ix);
            minY = Math.min(minY, iy);
        }
        double offx = x - minX;
        double offy = y - minY;
        if (offx != 0 || offy != 0) {
            for (int i = 0; i < xpoints.length; i++) {
                xpoints[i] += offx;
                ypoints[i] += offy;
            }
            change();
        }
    }

    @Override
    public void clearLocation() {
        setLocation(0, 0);
    }

    @Override
    public Vectors copy(AffineTransform transform) {
        Shape s = toShape();
        //XXX not really returning the right type here
        return new PathIteratorWrapper(
                s.getPathIterator(transform), fill);
    }

    @Override
    public boolean delete(int pointIndex) {
        if (xpoints.length <= 2) {
            return false;
        }
        double[] newX = new double[xpoints.length - 1];
        double[] newY = new double[xpoints.length - 1];

        System.arraycopy(xpoints, 0, newX, 0, pointIndex);
        System.arraycopy(xpoints, pointIndex + 1, newX, pointIndex, xpoints.length - pointIndex);

        System.arraycopy(ypoints, 0, newY, 0, pointIndex);
        System.arraycopy(ypoints, pointIndex + 1, newY, pointIndex, ypoints.length - pointIndex);
        change();
        return true;
    }

    @Override
    public boolean insert(double x, double y, int index, int kind) {
        if (index == xpoints.length - 1) {
            xpoints = Arrays.copyOf(xpoints, xpoints.length + 1);
            ypoints = Arrays.copyOf(xpoints, ypoints.length + 1);
            xpoints[index] = x;
            ypoints[index] = y;
            return true;
        } else if (index == 0) {
            double[] xp = new double[xpoints.length + 1];
            double[] yp = new double[ypoints.length + 1];
            xp[0] = x;
            yp[0] = y;
            System.arraycopy(xpoints, 0, xp, 1, xpoints.length);
            System.arraycopy(ypoints, 0, xp, 1, ypoints.length);
            xpoints = xp;
            ypoints = yp;
            return true;
        }
        double[] xp = new double[xpoints.length + 1];
        double[] yp = new double[xpoints.length + 1];
        System.arraycopy(xpoints, 0, xp, index, xpoints.length - index);
        System.arraycopy(xpoints, index, xp, index + 1, xpoints.length - index);

        System.arraycopy(ypoints, 0, yp, index, ypoints.length - index);
        System.arraycopy(ypoints, index, yp, index + 1, ypoints.length - index);

        xp[index] = x;
        yp[index] = y;
        xpoints = xp;
        ypoints = yp;
        change();
        return true;
    }

    @Override
    public int getPointIndexNearest(double x, double y) {
        Point2D.Double curr = new Point2D.Double(0, 0);
        double bestDistance = Double.MAX_VALUE;
        int bestIndex = -1;
        for (int i = 0; i < xpoints.length; i++) {
            curr.setLocation(xpoints[i], ypoints[i]);
            double dist = curr.distance(x, y);
            if (dist < bestDistance) {
                bestDistance = dist;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    @Override
    public int[] getVirtualControlPointIndices() {
        return EMPTY_INT;
    }

    @Override
    public void setControlPointLocation(int pointIndex, Pt pt) {
        xpoints[pointIndex] = (int) pt.x;
        ypoints[pointIndex] = (int) pt.y;
        change();
    }

    @Override
    public java.awt.Rectangle getBounds() {
        Rectangle2D.Double bds = new Rectangle2D.Double();
        getBounds(bds);
        return bds.getBounds();
    }

    public int rev() {
        return rev;
    }
}
