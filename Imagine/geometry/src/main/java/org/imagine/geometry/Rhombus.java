/*
 * The MIT License
 *
 * Copyright 2020 Mastfrog Technologies.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.imagine.geometry;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author Tim Boudreau
 */
public final class Rhombus implements Shape {

    private Circle circ;
    private double radX;
    private double radY;

    public Rhombus(double x, double y, double radX, double radY, double rotation) {
        circ = new Circle(x, y);
        circ.setRotation(rotation);
        this.radX = radX;
        this.radY = radY;
    }

    public Rhombus(Rectangle2D rect, double rotation) {
        circ = new Circle(rect.getCenterX(), rect.getCenterY());
        radX = rect.getWidth();
        radY = rect.getHeight();
        circ.setRotation(rotation);
    }

    public void setXRadius(double radX) {
        this.radX = Math.abs(radX);
    }

    public void setYRadius(double radY) {
        this.radY = Math.abs(radY);
    }

    public double getXRadius() {
        return radX;
    }

    public double getYRadius() {
        return radX;
    }

    public void setCenter(double x, double y) {
        circ.setCenter(x, y);
    }

    public void translate(double x, double y) {
        circ.translate(x, y);
    }

    public Point2D.Double top() {
        double[] d = circ.positionOf(0, radY);
        return new Point2D.Double(d[0], d[1]);
    }

    public Point2D.Double bottom() {
        double[] d = circ.positionOf(180, radY);
        return new Point2D.Double(d[0], d[1]);
    }

    public Point2D.Double left() {
        double[] d = circ.positionOf(270, radX);
        return new Point2D.Double(d[0], d[1]);
    }

    public Point2D.Double right() {
        double[] d = circ.positionOf(90, radX);
        return new Point2D.Double(d[0], d[1]);
    }

    private Point2D.Double[] points() {
        return new Point2D.Double[]{top(), left(), bottom(), right()};
    }

    @Override
    public Rectangle getBounds() {
        return getBounds2D().getBounds();
    }

    @Override
    public Rectangle2D getBounds2D() {
        double minX, minY, maxX, maxY;
        minX = minY = Double.MAX_VALUE;
        maxX = maxY = Double.MIN_VALUE;
        for (Point2D.Double d : points()) {
            minX = Math.min(d.x, minX);
            maxX = Math.max(d.x, minX);
            minY = Math.min(d.y, minY);
            maxY = Math.max(d.y, minY);
        }
        return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
    }

    private Path2D.Double toPath() {
        return new Path2D.Double(this);
    }

    @Override
    public boolean contains(double x, double y) {
        // XXX create lines for each point
        // test relativeCCW is inside for each

        return toPath().contains(x, y);
    }

    @Override
    public boolean contains(Point2D p) {
        return toPath().contains(p.getX(), p.getY());
    }

    @Override
    public boolean intersects(double x, double y, double w, double h) {
        return toPath().intersects(x, y, w, h);
    }

    @Override
    public boolean intersects(Rectangle2D r) {
        return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
        return toPath().contains(x, y, w, h);
    }

    @Override
    public boolean contains(Rectangle2D r) {
        return toPath().contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        return new PI(points(), at);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return getPathIterator(at);
    }

    static class PI implements PathIterator {

        private final Point2D.Double[] pts;
        private int cursor;

        public PI(Point2D.Double[] pts, AffineTransform xform) {
            this.pts = pts;
            if (xform != null) {
                for (int i = 0; i < pts.length; i++) {
                    xform.transform(pts[i], pts[i]);
                }
            }
        }

        @Override
        public int getWindingRule() {
            return PathIterator.WIND_NON_ZERO;
        }

        @Override
        public boolean isDone() {
            return cursor >= pts.length + 1;
        }

        @Override
        public void next() {
            cursor++;
        }

        @Override
        public int currentSegment(float[] coords) {
            if (cursor == pts.length) {
                return PathIterator.SEG_CLOSE;
            }
            coords[0] = (float) pts[cursor].x;
            coords[1] = (float) pts[cursor].y;
            return cursor == 0 ? PathIterator.SEG_MOVETO : PathIterator.SEG_LINETO;
        }

        @Override
        public int currentSegment(double[] coords) {
            if (cursor == pts.length) {
                return PathIterator.SEG_CLOSE;
            }
            coords[0] = pts[cursor].x;
            coords[1] = pts[cursor].y;
            return cursor == 0 ? PathIterator.SEG_MOVETO : PathIterator.SEG_LINETO;
        }
    }

}
