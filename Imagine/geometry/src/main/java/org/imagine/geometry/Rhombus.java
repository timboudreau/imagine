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

import com.mastfrog.function.DoubleQuadConsumer;
import com.mastfrog.function.DoubleSextaConsumer;
import com.mastfrog.util.collections.DoubleSet;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.imagine.geometry.util.GeometryStrings;
import org.imagine.geometry.util.GeometryUtils;
import static org.imagine.geometry.util.GeometryUtils.INTERSECTION_FUDGE_FACTOR;

/**
 * A rhombus implemented as a center point, two radii specifying the distances
 * between opposing corner, and a rotation angle.
 *
 * @author Tim Boudreau
 */
public final class Rhombus implements Shape, EnhancedShape {

    private final Circle circ;
    private double radX;
    private double radY;

    public String toString() {
        return "Rhombus(" + GeometryStrings.toString(circ.centerX, circ.centerY)
                + "  rX " + GeometryStrings.toString(radX)
                + "  rY " + GeometryStrings.toString(radY)
                + " rot " + GeometryStrings.toString(circ.rotation()) + "\u00B0 <"
                + top() + "> <" + right() + "> <" + bottom() + "> <" + left() + ">)";
    }

    public Rhombus(double x, double y, double radX, double radY, double rotation) {
        circ = new Circle(x, y);
        circ.setRotation(rotation);
        this.radX = radX;
        this.radY = radY;
    }

    public Rhombus(Rectangle2D rect, double rotation) {
        circ = new Circle(rect.getCenterX(), rect.getCenterY());
        radX = rect.getWidth() / 2;
        radY = rect.getHeight() / 2;
        circ.setRotation(rotation);
    }

    private static boolean isLesser(Point2D compareTo, Point2D test) {
        return EqPointDouble.of(compareTo).compareTo(test) > 0;
    }

    public static Rhombus fromPoints(Point2D a, Point2D b, Point2D c, Point2D d) {
        if (isLesser(a, c)) {
            Point2D hold = a;
            a = c;
            c = hold;
        }
        if (isLesser(b, d)) {
            Point2D hold = b;
            b = d;
            d = hold;
        }

        EqLine ln1 = new EqLine(a, c);
        EqLine ln2 = new EqLine(b, d);
        ln1.normalize();
        ln2.normalize();
        EqPointDouble mid1 = ln1.midPoint();
        EqPointDouble mid2 = ln2.midPoint();
        if (mid1.distance(mid2) > 2) { // XXX arbitrary
            return null;
        }
        double ang = ln2.angleNormalized();
        return new Rhombus(mid1.getX(), mid1.getY(), ln2.length(), ln1.length(), ang);
    }

    public static void main(String[] args) {
        Rhombus rhom = new Rhombus(100, 100, 10, 20, 0);
        Rhombus nue = fromPoints(rhom.top(), rhom.right(), rhom.bottom(), rhom.left());
    }

    public Angle getRotation() {
        return circ.getRotation();
    }

    public boolean isHomomorphic(Rhombus other) {
        List<? extends EqPointDouble> a = points();
        List<? extends EqPointDouble> b = other.points();
        Collections.sort(a);
        Collections.sort(b);
        DoubleSet ds = DoubleSet.create(4);
        for (int i = 0; i < 4; i++) {
            EqPointDouble ap = a.get(i);
            EqPointDouble bp = b.get(i);
            ds.add(ap.distance(bp));
        }
        if (ds.size() == 1) {
            return true;
        }
        for (int i = 1; i < ds.size(); i++) {
            double prev = ds.getAsDouble(i);
            double curr = ds.getAsDouble(i);
            if (!GeometryUtils.isSameCoordinate(prev, curr)) {
                return false;
            }
        }
        return true;
    }

    public void translate(double x, double y) {
        circ.translate(x, y);
    }

    public boolean isRegular() {
        return radX == radY || Math.abs(radX - radY) < 0.00000000000001;
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

    public EqPointDouble top() {
        double[] d = circ.positionOf(0, radY);
        return new EqPointDouble(d[0], d[1]);
    }

    public EqPointDouble bottom() {
        double[] d = circ.positionOf(180, radY);
        return new EqPointDouble(d[0], d[1]);
    }

    public EqPointDouble left() {
        double[] d = circ.positionOf(270, radX);
        return new EqPointDouble(d[0], d[1]);
    }

    public EqPointDouble right() {
        double[] d = circ.positionOf(90, radX);
        return new EqPointDouble(d[0], d[1]);
    }

    private Point2D.Double[] allPoints() {
        return new Point2D.Double[]{top(), left(), bottom(), right()};
    }

    @Override
    public Rectangle getBounds() {
        return getBounds2D().getBounds();
    }

    @Override
    public Rectangle2D getBounds2D() {
        Rectangle2D.Double result = new Rectangle2D.Double();
        EqPointDouble t = top();
        result.x = t.x;
        result.y = t.y;
        result.add(right());
        result.add(bottom());
        result.add(left());
        return result;
    }

    public <T extends Rectangle2D> T addToBounds(T bds) {
        EqPointDouble t = top();
        if (bds.isEmpty()) {
            bds.setFrame(t.x, t.y, 0, 0);
        } else {
            bds.add(top());
        }
        bds.add(left());
        bds.add(right());
        bds.add(bottom());
        return bds;
    }

    public double[] coordinates() {
        Point2D.Double top, right, bottom, left;
        top = top();
        right = right();
        bottom = bottom();
        left = left();
        return new double[]{
            top.x, top.y, right.x, right.y, bottom.x, bottom.y, left.x, left.y
        };
    }

    private Path2D.Double toPath() {
        return new Path2D.Double(this);
    }

    @Override
    public boolean contains(double tx, double ty) {
        double[] points = coordinates();
        double minX, minY, maxX, maxY;
        minX = minY = Double.MAX_VALUE;
        maxX = maxY = Double.MIN_VALUE;
        for (int i = 0; i < 8; i += 2) {
            minX = min(minX, points[i]);
            minY = min(minY, points[i + 1]);
            maxX = max(maxX, points[i]);
            maxY = max(maxY, points[i + 1]);
        }
        if ((tx < minX && ty < minY) || (tx > maxX && ty > maxY)) {
            return false;
        }
        double testY = ty < minY ? maxY + 1 : minY - 1;
        int count = 0;

        // Line2D.linesIntersect will give a false positive for
        // some points where the y coordinate EXACTLY MATCHES
        // one of the points on the polygon; so we tilt the tested
        // line VERY fractionally to avoid having a few stripes across
        // the polygon which test as not being part of it, and
        // a few out to the bounding box that do
        double workingTxFirst = tx + INTERSECTION_FUDGE_FACTOR;
        double workingTxSecond = tx - INTERSECTION_FUDGE_FACTOR;

        for (int i = 0; i < points.length; i += 2) {
            int next = i == points.length - 2 ? 0 : i + 2;
            double x = points[i];
            double y = points[i + 1];
            // compensate for -0.0 by adding 0.0
            if (tx + 0.0 == x + 0.0 && ty + 0.0 == y + 0.0) {
                return true;
            }
            double nx = points[next];
            double ny = points[next + 1];
            if (nx + 0.0 == tx + 0.0 && ny + 0.0 == tx + 0.0) {
                return true;
            }
            boolean isect = Line2D.linesIntersect(workingTxFirst, ty,
                    workingTxSecond, testY, x, y, nx, ny);
            if (isect) {
                count++;
            }
        }
        return count % 2 == 1;
    }

    public Triangle2D[] toTriangles(boolean longAxis) {
        Triangle2D[] result = new Triangle2D[2];
        if (getXRadius() > getYRadius() && longAxis) {
            result[0] = new Triangle2D(top(), bottom(), left());
            result[1] = new Triangle2D(top(), bottom(), right());
        } else {
            result[0] = new Triangle2D(top(), left(), right());
            result[1] = new Triangle2D(bottom(), left(), right());
        }
        return result;
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
        return new PI(allPoints(), at);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return getPathIterator(at);
    }

    @Override
    public Point2D point(int index) {
        switch (index) {
            case 0:
                return top();
            case 1:
                return right();
            case 2:
                return bottom();
            case 3:
                return left();
            default:
                throw new IndexOutOfBoundsException("" + index);
        }
    }

    @Override
    public int pointCount() {
        return 4;
    }

    @Override
    public void visitAdjoiningLines(DoubleSextaConsumer consumer) {
        Point2D.Double top, right, bottom, left;
        top = top();
        right = right();
        bottom = bottom();
        left = left();
        consumer.accept(top.x, top.y, right.x, right.y, bottom.x, bottom.y);
        consumer.accept(right.x, right.y, bottom.x, bottom.y, left.x, left.y);
        consumer.accept(bottom.x, bottom.y, left.x, left.y, top.x, top.y);
        consumer.accept(left.x, left.y, top.x, top.y, right.x, right.y);
    }

    @Override
    public void visitLines(DoubleQuadConsumer consumer) {
        Point2D.Double top, right, bottom, left;
        top = top();
        right = right();
        bottom = bottom();
        left = left();
        consumer.accept(top.x, top.y, right.x, right.y);
        consumer.accept(right.x, right.y, bottom.x, bottom.y);
        consumer.accept(bottom.x, bottom.y, left.x, left.y);
        consumer.accept(left.x, left.y, top.x, top.y);
    }

    @Override
    public List<? extends EqPointDouble> points() {
        return Arrays.asList(top(), right(), bottom(), left());
    }

    @Override
    public boolean selfIntersects() {
        return radX <= 0 || radY <= 0;
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
