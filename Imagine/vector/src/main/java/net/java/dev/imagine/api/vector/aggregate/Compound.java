/*
 * Compound.java
 *
 * Created on October 31, 2006, 1:45 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.aggregate;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.Aggregate;
import net.java.dev.imagine.api.vector.Fillable;
import net.java.dev.imagine.api.vector.Mutable;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.Shaped;
import net.java.dev.imagine.api.vector.Strokable;
import net.java.dev.imagine.api.vector.Transformable;
import net.java.dev.imagine.api.vector.Vector;
import net.java.dev.imagine.api.vector.Volume;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import net.java.dev.imagine.api.vector.util.Pt;

/**
 * Wrapper for a stack of primitives which are treated as a unit
 *
 * @author Tim Boudreau
 */
public class Compound implements Primitive, Strokable, Vector, Volume, Adjustable, Fillable, Mutable, Aggregate {

    private final List<Primitive> contents = new ArrayList<>(10);
    public double x;
    public double y;

    public Compound(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public Runnable restorableSnapshot() {
        List<Runnable> all = new ArrayList<>(contents.size());
        for (Primitive p : contents) {
            if (p instanceof Shaped) {
                all.add(((Shaped) p).restorableSnapshot());
            }
        }
        return () -> {
            for (Runnable r : all) {
                r.run();
            }
        };
    }

    public void add(Primitive primitive) {
        contents.add(primitive);
    }

    public void add(int ix, Primitive primitive) {
        contents.add(ix, primitive);
    }

    public void remove(Primitive primitive) {
        contents.remove(primitive);
    }

    public void remove(int ix) {
        contents.remove(ix);
    }

    public void moveUp(int ix) {
        moveUp(ix, contents.get(ix));
    }

    private void moveUp(int ix, Primitive primitive) {
        if (ix > 0) {
            contents.remove(ix);
            contents.add(ix - 1, primitive);
        }
    }

    public void moveDown(int ix) {
        moveDown(ix, contents.get(ix));
    }

    private void moveDown(int ix, Primitive primitive) {
        if (ix < contents.size() - 1) {
            contents.remove(ix);
            contents.add(ix - 1, primitive);
        }
    }

    public void moveUp(Primitive primitive) {
        int ix = indexOf(primitive);
        moveUp(ix, primitive);
    }

    public void moveDown(Primitive primitive) {
        int ix = indexOf(primitive);
        moveDown(ix, primitive);
    }

    public int indexOf(Primitive p) {
        return contents.indexOf(p);
    }

    @Override
    public void applyTransform(AffineTransform xform) {
        for (Primitive p : contents) {
            if (p instanceof Transformable) {
                ((Transformable) p).applyTransform(xform);
            }
        }
    }

    @Override
    public java.awt.Rectangle getBounds() {
        java.awt.Rectangle result = null;
        Rectangle2D.Double scratch = new Rectangle2D.Double(0, 0, 0, 0);
        for (Primitive p : contents) {
            if (p instanceof Volume) {
                Volume vp = (Volume) p;
                vp.getBounds(scratch);
                if (result == null) {
                    result = new java.awt.Rectangle(scratch.getBounds());
                } else {
                    java.awt.Rectangle.union(scratch, result, result);
                }
//                System.err.println("Union with " + vp + " gets " + result);
            }
        }
        if (result == null) {
            Pt pt = getLocation();
            result = new java.awt.Rectangle((int) pt.x, (int) pt.y, 0, 0);
        } else {
            result.translate((int) x, (int) y);
        }
        return result;
    }

    @Override
    public void paint(Graphics2D g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.getTransform().concatenate(AffineTransform.getTranslateInstance(x, y));
        for (Primitive p : contents) {
            p.paint(g2);
        }
        g2.dispose();
    }

    @Override
    public Shape toShape() {
        Area a = new Area();
        for (Primitive p : contents) {
            if (p instanceof Vector) {
                Area b = new Area(((Vector) p).toShape());
                a.add(b);
            }
        }
        a.transform(AffineTransform.getTranslateInstance(x, y));
        return a;
    }

    @Override
    public void getBounds(Rectangle2D r) {
        Rectangle2D.Double scratch = null;
        for (Primitive p : contents) {
            if (p instanceof Volume) {
                if (scratch == null) {
                    scratch = new Rectangle2D.Double();
                    ((Volume) p).getBounds(r);
                } else {
                    ((Volume) p).getBounds(scratch);
                    Rectangle2D.Double.union(scratch, r, r);
                }
            }
        }
        if (scratch == null) {
            r.setFrame(x, y, 0,0);
        } else {
            double xx1 = r.getX() + r.getWidth();
            double yy1 = r.getHeight() + r.getY();
            double xx = r.getX() + x;
            double yy = r.getY() + y;
            r.setFrame(xx, yy, xx1-xx, yy1-yy);
        }
    }

    @Override
    public Compound copy() {
        Compound nue = new Compound(x, y);
        List<Primitive> l = new ArrayList<>(contents.size());
        for (Primitive p : contents) {
            l.add(p.copy());
        }
        nue.contents.addAll(l);
        return nue;
    }

    @Override
    public void draw(Graphics2D g) {
        g.translate(x, y);
        for (Primitive p : contents) {
            if (p instanceof Strokable) {
                ((Strokable) p).draw(g);
            }
        }
        g.translate(-x, -y);
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
        double[] pts = new double[]{x, y};
        transform.transform(pts, 0, pts, 0, 1);
        Compound nue = new Compound(pts[0], pts[1]);
        nue.contents.addAll(contents);
        return nue;
    }

    @Override
    public void fill(Graphics2D g) {
        AffineTransform oldXform = g.getTransform();
        g.setTransform(AffineTransform.getTranslateInstance(x, y));
        for (Primitive p : contents) {
            if (p instanceof Fillable) {
                ((Fillable) p).fill(g);
            }
        }
        g.setTransform(oldXform);
    }

    @Override
    public boolean isFill() {
        return true; //XXX check primitives?
    }

    @Override
    public boolean delete(int pointIndex) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean insert(double x, double y, int index, int kind) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getPointIndexNearest(double x, double y) {
        Rectangle2D.Double r = new Rectangle2D.Double();
        getBounds(r);
        Point2D.Double[] d = points(r);
        double bestDistance = Double.MAX_VALUE;
        int bestIndex = 0;
        for (int i = 0; i < d.length; i++) {
            Point2D.Double p = d[i];
            double dist = p.distance(x, y);
            if (dist < bestDistance) {
                bestDistance = dist;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private Point2D.Double[] points(Rectangle2D.Double r) {
        return new Point2D.Double[]{
            new Point2D.Double(r.x, r.y),
            new Point2D.Double(r.x + r.width, r.y),
            new Point2D.Double(r.x + r.width, r.y + r.height),
            new Point2D.Double(r.x, r.y + r.height),};
    }

    @Override
    public void setControlPointLocation(int pointIndex, Pt location) {
        int total = 0;
        location = new Pt(location.x - x, location.y - y);
        for (Primitive p : contents) {
            if (p instanceof Adjustable) {
                Adjustable a = (Adjustable) p;
                int count = a.getControlPointCount();
                if (total <= pointIndex && total + count > pointIndex) {
                    a.setControlPointLocation(pointIndex - total, location);
                }
                total += count;
            }
        }
    }

    @Override
    public ControlPointKind[] getControlPointKinds() {
        List<ControlPointKind> result = new ArrayList<>(contents.size() * 4);
        for (Primitive p : contents) {
            if (p instanceof Adjustable) {
                Adjustable a = (Adjustable) p;
                result.addAll(Arrays.asList(a.getControlPointKinds()));
            }
        }
        return result.toArray(new ControlPointKind[result.size()]);
    }

    @Override
    public void getControlPoints(double[] xy) {
        int total = 0;
        List<double[]> l = new ArrayList<>(contents.size());
        List<int[]> v = new ArrayList<>(contents.size());
        for (Primitive p : contents) {
            if (p instanceof Adjustable) {
                Adjustable a = (Adjustable) p;
                int count = a.getControlPointCount();
                double[] d = new double[count * 2];
                a.getControlPoints(d);
                int[] vcp = a.getVirtualControlPointIndices();
                for (int i = 0; i < vcp.length; i++) {
                    vcp[i] += total;
                }
                v.add(vcp);
                l.add(d);
                total += count;
            }
        }
        int ix = 0;
        for (double[] d : l) {
            int len = d.length;
            System.arraycopy(d, 0, xy, ix, d.length);
            ix += len;
        }
    }

    @Override
    public int getControlPointCount() {
        int total = 0;
        List<double[]> l = new ArrayList<>(contents.size());
        for (Primitive p : contents) {
            if (p instanceof Adjustable) {
                Adjustable a = (Adjustable) p;
                int count = a.getControlPointCount();
                total += count;
            }
        }
        return total;
    }

    @Override
    public int[] getVirtualControlPointIndices() {
        int total = 0;
        List<int[]> v = new ArrayList<>(contents.size());
        for (Primitive p : contents) {
            if (p instanceof Adjustable) {
                Adjustable a = (Adjustable) p;
                int count = a.getControlPointCount();
                double[] d = new double[count * 2];
                a.getControlPoints(d);
                int[] vcp = a.getVirtualControlPointIndices();
                for (int i = 0; i < vcp.length; i++) {
                    vcp[i] += total;
                }
                v.add(vcp);
                total += vcp.length;
            }
        }
        int ix = 0;
        int[] result = new int[total];
        for (int[] i : v) {
            int len = i.length;
            System.arraycopy(i, 0, result, ix, i.length);
            ix += len;
        }
        return result;
    }

    @Override
    public int getPrimitiveCount() {
        return contents.size();
    }

    @Override
    public Primitive getPrimitive(int i) {
        return contents.get(i);
    }

    @Override
    public int getVisualPrimitiveCount() {
        int result = 0;
        int max = getPrimitiveCount();
        for (int i = 0; i < max; i++) {
            Primitive p = getPrimitive(i);
            if (p instanceof Vector || p instanceof Strokable || p instanceof Volume) {
                result++;
            }
        }
        return result;
    }

    @Override
    public Primitive getVisualPrimitive(int ix) {
        int result = 0;
        int max = getPrimitiveCount();
        for (int i = 0; i < max; i++) {
            Primitive p = getPrimitive(i);
            if (p instanceof Vector || p instanceof Strokable || p instanceof Volume) {
                result++;
                if (result == ix) {
                    return p;
                }
            }
        }
        throw new IndexOutOfBoundsException("Only " + max + " present but "
                + "requested " + ix);
    }
}
