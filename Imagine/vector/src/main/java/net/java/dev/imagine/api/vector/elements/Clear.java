/*
 * Clear.java
 *
 * Created on September 27, 2006, 8:07 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.elements;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.Transformable;
import net.java.dev.imagine.api.vector.Volume;

/**
 *
 * @author Tim Boudreau
 */
public final class Clear implements Volume, Primitive, Transformable {

    private static final long serialVersionUID = 101_034L;
    public int x;
    public int y;
    public int width;
    public int height;

    public Clear(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;
    }

    @Override
    public double cumulativeLength() {
        return (width * 2) + (height * 2);
    }

    @Override
    public void collectSizings(SizingCollector c) {
        c.dimension(height, true, 1, 3);
        c.dimension(width, false, 0, 1);
    }

    public Runnable restorableSnapshot() {
        int ox = x;
        int oy = y;
        int ow = width;
        int oh = height;
        return () -> {
            x = ox;
            y = oy;
            width = ow;
            height = oh;
        };
    }

    @Override
    public void translate(double x, double y) {
        this.x += (int) x;
        this.y += (int) y;
    }

    @Override
    public String toString() {
        return "Clear " + x + ',' + y + ',' + width + ',' + height;
    }

    @Override
    public void paint(Graphics2D g) {
        g.clearRect(x, y, width, height);
    }

    @Override
    public void getBounds(Rectangle2D r) {
        r.setRect(x, y, width, height);
    }

    @Override
    public void addToBounds(Rectangle2D bds) {
        if (bds.isEmpty()) {
            bds.setFrame(x, y, width, height);
        } else {
            bds.add(x, y);
            bds.add(x + width, y + height);
        }
    }

    @Override
    public Clear copy() {
        return new Clear(x, y, width, height);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (o == this) {
            return true;
        }
        boolean result = o instanceof Clear;
        if (result) {
            Clear c = (Clear) o;
            result = c.x == x && c.y == y && c.width == width
                    && c.height == height;
        }
        return result;
    }

    @Override
    public int hashCode() {
        return ((x * y) + (width * height)) ^ 17;
    }

    public void setLocation(double x, double y) {
        this.x = (int) x;
        this.y = (int) y;
    }

    private void setFrame(double x, double y, double w, double h) {
        this.x = (int) Math.floor(x);
        this.y = (int) Math.floor(y);
        double x1 = Math.ceil(x + w);
        double y1 = Math.ceil(y + h);
        this.width = (int) (x1 - this.x);
        this.height = (int) (y1 - this.y);
    }

    @Override
    public void applyTransform(AffineTransform xform) {
        if (xform.isIdentity()) {
            return;
        }
        double[] pts = new double[]{
            x, y, x + width, y + height};
        xform.transform(pts, 0, pts, 0, 2);
        setFrame(pts[0], pts[1], pts[2] - pts[0],
                pts[3] - pts[1]);
    }

    @Override
    public Clear copy(AffineTransform transform) {
        Clear result = copy();
        result.applyTransform(transform);
        return result;
    }
}
