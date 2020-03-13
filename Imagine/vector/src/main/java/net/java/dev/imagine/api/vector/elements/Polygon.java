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
import net.java.dev.imagine.api.vector.Vector;
import net.java.dev.imagine.api.vector.Volume;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import net.java.dev.imagine.api.vector.util.Pt;

/**
 *
 * @author Tim Boudreau
 */
public class Polygon implements Strokable, Fillable, Volume, Adjustable, Vector, Mutable {

    public long serialVersionUID = 12_342_394L;
    public int[] xpoints;
    public int[] ypoints;
    public int npoints;
    public boolean fill;

    public Polygon(int[] xpoints, int[] ypoints, int npoints, boolean fill) {
        this.xpoints = xpoints;
        this.ypoints = ypoints;
        this.npoints = npoints;
        assert npoints <= xpoints.length;
        assert ypoints.length >= npoints;
        assert npoints >= 0;
        this.fill = fill;
    }

    @Override
    public synchronized Runnable restorableSnapshot() {
        int[] xp = Arrays.copyOf(xpoints, xpoints.length);
        int[] yp = Arrays.copyOf(ypoints, ypoints.length);
        int np = npoints;
        return () -> {
            if (np != npoints) {
                npoints = np;
                xpoints = xp;
                ypoints = yp;
            } else {
                System.arraycopy(xp, 0, xpoints, 0, xp.length);
                System.arraycopy(yp, 0, ypoints, 0, yp.length);
            }
        };
    }

    @Override
    public void translate(double x, double y) {
        int xx = (int) x;
        int yy = (int) y;
        if (xx == 0 && yy == 0) {
            return;
        }
        for (int i = 0; i < npoints; i++) {
            xpoints[i] += xx;
            ypoints[i] += yy;
        }
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("Polygon ");
        for (int i = 0; i < npoints; i++) {
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
        java.awt.Polygon result = new java.awt.Polygon(xpoints, ypoints, npoints);
        return result;
    }

    @Override
    public boolean isFill() {
        return fill;
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
            result = p.npoints == npoints;
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
        return (a + 1) * (b + 1) + ((npoints + 1) * 36_229);
    }

    @Override
    public void paint(Graphics2D g) {
        if (fill) {
            g.drawPolygon(xpoints, ypoints, npoints);
        } else {
            g.fillPolygon(xpoints, ypoints, npoints);
        }
    }

    @Override
    public int getControlPointCount() {
        return npoints;
    }

    public Strokable create(int[] x, int[] y) {
        return new Polygon(x, y, npoints, fill);
    }

    @Override
    public void addToBounds(Rectangle2D bds) {
        if (npoints == 0) {
            return;
        }
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int i = 0; i < npoints; i++) {
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
        if (npoints == 0) {
            return;
        }
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int i = 0; i < npoints; i++) {
            minX = Math.min(minX, xpoints[i]);
            minY = Math.min(minY, ypoints[i]);
            maxX = Math.max(maxX, xpoints[i]);
            maxY = Math.max(maxY, ypoints[i]);
        }
        int width = maxX - minX;
        int height = maxY - minY;
        int x = minX;
        int y = minY;
        r.setRect(x, y, width, height);
    }

    public Strokable createInverseFilledInstance() {
        return new Polygon(xpoints, ypoints, npoints, !fill);
    }

    @Override
    public void draw(Graphics2D g) {
        g.drawPolygon(xpoints, ypoints, npoints);
    }

    @Override
    public Polygon copy() {
        return new Polygon(Arrays.copyOf(xpoints, xpoints.length),
                Arrays.copyOf(ypoints, ypoints.length), npoints, fill);
    }

    @Override
    public ControlPointKind[] getControlPointKinds() {
        ControlPointKind[] kinds = new ControlPointKind[npoints];
        Arrays.fill(kinds, ControlPointKind.PHYSICAL_POINT);
        return kinds;
    }

    @Override
    public void applyTransform(AffineTransform xform) {
        Point2D.Double scratch = new Point2D.Double();
        for (int i = 0; i < npoints; i++) {
            scratch.x = xpoints[i];
            scratch.y = ypoints[i];
            xform.transform(scratch, scratch);
            xpoints[i] = (int) Math.round(scratch.x);
            ypoints[i] = (int) Math.round(scratch.x);
        }
    }

    @Override
    public void fill(Graphics2D g) {
        g.fillPolygon(xpoints, ypoints, npoints);
    }

    @Override
    public void getControlPoints(double[] xy) {
        assert xy.length <= npoints * 2;
        for (int i = 0; i < npoints; i += 2) {
            int pos = i / 2;
            xy[pos] = xpoints[i];
            xy[i + 1] = ypoints[i];
        }
    }

    @Override
    public Pt getLocation() {
        int minx = Integer.MAX_VALUE;
        int miny = Integer.MAX_VALUE;
        for (int i = 0; i < npoints; i++) {
            minx = Math.min(minx, xpoints[i]);
            miny = Math.min(miny, ypoints[i]);
        }
        minx = minx == Integer.MAX_VALUE ? 0 : minx;
        miny = miny == Integer.MAX_VALUE ? 0 : miny;
        return new Pt(minx, miny);
    }

    @Override
    public void setLocation(double xx, double yy) {
        //XXX do all this double precision?
        int x = (int) xx;
        int y = (int) yy;
        int minY = Integer.MAX_VALUE;
        int minX = Integer.MAX_VALUE;
        for (int i = 0; i < npoints; i++) {
            int ix = xpoints[i];
            int iy = ypoints[i];
            minX = Math.min(minX, ix);
            minY = Math.min(minY, iy);
        }
        int offx = x - minX;
        int offy = y - minY;
        if (offx != 0 || offy != 0) {
            for (int i = 0; i < npoints; i++) {
                xpoints[i] += offx;
                ypoints[i] += offy;
            }
        }
    }

    @Override
    public void clearLocation() {
        setLocation(0, 0);
    }

    @Override
    public Vector copy(AffineTransform transform) {
        Shape s = toShape();
        //XXX not really returning the right type here
        return new PathIteratorWrapper(
                s.getPathIterator(transform), fill);
    }

    @Override
    public boolean delete(int pointIndex) {
        if (npoints <= 2) {
            return false;
        }
        int ix = 0;
        for (int i = 0; i < npoints; i++) {
            if (i == pointIndex) {
                ix++;
            }
            if (i >= pointIndex && ix < npoints) {
                xpoints[i] = xpoints[ix];
                ypoints[i] = ypoints[ix];
            }
            ix++;
        }
        npoints--;
        return true;
    }

    @Override
    public boolean insert(double x, double y, int index, int kind) {
        if (xpoints.length < npoints + 1) {
            int[] xp = new int[xpoints.length + 2];
            int[] yp = new int[ypoints.length + 2];
            System.arraycopy(xpoints, 0, xp, 0, npoints);
            System.arraycopy(ypoints, 0, yp, 0, npoints);
            xpoints = xp;
            ypoints = yp;
        }

        int ix = npoints;
        for (int i = npoints; i >= 0; i--) {
            if (i == index) {
                xpoints[i] = (int) x;
                ypoints[i] = (int) y;
            } else if (i > index) {
                xpoints[i] = xpoints[i - 1];
                ypoints[i] = ypoints[i - 1];
            } else {
                break;
            }
        }
        npoints++;
        return true;
    }

    @Override
    public int getPointIndexNearest(double x, double y) {
        Point2D.Double curr = new Point2D.Double(0, 0);
        double bestDistance = Double.MAX_VALUE;
        int bestIndex = -1;
        for (int i = 0; i < npoints; i++) {
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
    }

    @Override
    public java.awt.Rectangle getBounds() {
        Rectangle2D.Double bds = new Rectangle2D.Double();
        getBounds(bds);
        return bds.getBounds();
    }
}
