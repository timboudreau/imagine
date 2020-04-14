/*
 * Copyright (c) 2015, tim
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.imagine.geometry;

import com.mastfrog.function.DoubleBiConsumer;
import com.mastfrog.function.DoubleBiPredicate;
import com.mastfrog.function.DoubleQuadConsumer;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.imagine.geometry.util.GeometryStrings;
import static org.imagine.geometry.util.GeometryUtils.toInt;

/**
 *
 * @author Tim Boudreau
 */
public final strictfp class Circle implements Shape, Sector {

    double centerX;
    double centerY;
    double radius = 10D;
    double factor = 1D;
    private double rotation;

    public Circle() {
        this(0, 0);
    }

    public Circle(Point2D center) {
        centerX = center.getX();
        centerY = center.getY();
    }

    public Circle(Point2D center, double radius) {
        centerX = center.getX();
        centerY = center.getY();
        this.radius = radius;
    }

    public Circle(Rectangle2D rect) {
        this(rect.getCenterX(), rect.getCenterY(), Math.min(rect.getWidth(), rect.getHeight()) / 2D);
    }

    public Circle(double centerX, double centerY) {
        this.centerX = centerX;
        this.centerY = centerY;
    }

    public Circle(double centerX, double centerY, double radius) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = Math.abs(radius);
    }

    @Override
    public Circle toShape(double x, double y, double radius) {
        if (x == centerX && y == centerY && radius == this.radius) {
            return this;
        }
        Circle circ = new Circle(x, y, radius);
        circ.factor = factor;
        circ.rotation = rotation;
        return circ;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + (int) (Double.doubleToLongBits(this.centerX) ^ (Double.doubleToLongBits(this.centerX) >>> 32));
        hash = 29 * hash + (int) (Double.doubleToLongBits(this.centerY) ^ (Double.doubleToLongBits(this.centerY) >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Circle other = (Circle) obj;
        if (Double.doubleToLongBits(this.centerX) != Double.doubleToLongBits(other.centerX)) {
            return false;
        }
        if (Double.doubleToLongBits(this.centerY) != Double.doubleToLongBits(other.centerY)) {
            return false;
        }
        return true;
    }

    public void setCenterAndRadius(double centerX, double centerY, double radius) {
        this.centerX = centerX + 0.0; // avoid -0.0
        this.centerY = centerY + 0.0;
        this.radius = Math.abs(radius);
    }

    @Override
    public double start() {
        return rotation;
    }

    @Override
    public double extent() {
        return factor == 1 ? 360 : 360 * factor;
    }

    @Override
    public boolean isSameSector(Sector other) {
        return other.extent() == 360;
    }

    @Override
    public boolean contains(Sector sector) {
        return true;
    }

    @Override
    public boolean contains(double degrees) {
        if (factor == 1) {
            return true;
        }
        return Sector.super.contains(degrees);
    }

    @Override
    public Circle rotatedBy(double degrees) {
        if (degrees == 0.0 || degrees == -0.0) {
            return this;
        }
        Circle nue = new Circle(centerX, centerY, radius);
        nue.factor = this.factor;
        nue.rotation = Angle.normalize(rotation + degrees);
        return nue;
    }

    @Override
    public PieWedge[] subdivide(int by) {
        Sector[] sects = Sector.super.subdivide(by);
        PieWedge[] w = new PieWedge[sects.length];
        for (int i = 0; i < sects.length; i++) {
            w[i] = new PieWedge(centerX, centerY, radius, sects[i].start(), sects[i].extent(), true);
        }
        return w;
    }

    public void applyTransform(AffineTransform xform) {
        switch (xform.getType()) {
            case AffineTransform.TYPE_QUADRANT_ROTATION:
            case AffineTransform.TYPE_GENERAL_ROTATION:
                return;
            default:
                double[] pts = new double[]{centerX, centerY, 0, 0};
                positionOf(0, radius, 2, pts);
                xform.transform(pts, 0, pts, 0, pts.length / 2);
                double newRadius = Point2D.distance(pts[0], pts[1], pts[2], pts[3]);
                centerX = pts[0];
                centerY = pts[1];
                radius = newRadius;
        }
    }

    /**
     * Get the rotation of this angle (skews results of positionOf, etc.).
     *
     * @return The rotation
     */
    public double rotation() {
        return rotation;
    }

    public Angle getRotation() {
        return Angle.ofDegrees(rotation);
    }

    /**
     * Create a circle which entirely contains the passed rectangle.
     *
     * @param rect
     * @return
     */
    public static Circle containing(Rectangle2D rect) {
        double dist = Point2D.distance(0, 0, rect.getWidth(), rect.getHeight());
        return new Circle(rect.getCenterX(), rect.getCenterY(), dist / 2D);
    }

    /**
     * Determine if this circle overlaps another.
     *
     * @param other Another circle
     * @return True if they overlap
     */
    public boolean overlaps(Circle other) {
        if (other == this || other.centerX == centerX && other.centerY == centerY) {
            return true;
        }
        double dist = Point2D.distance(centerX, centerY, other.centerX, other.centerY);
        return dist < radius + other.radius;
    }

    /*
    public double areaOfOverlap(Circle other) {
    // Some optimizations - overlap with self is always area
    if (other == this || other.equals(this)) {
    return area();
    }
    // If either is fully contained in the other, return the smaller
    // of them's radius
    if (contains(other.centerX, other.centerY)) {
    // This one fully contains that one
    if (distanceToRadius(other.centerX, other.centerY) > other.radius) {
    return other.area();
    }
    } else if (other.contains(centerX, centerY)) {
    // That one fully contains this one
    if (other.distanceToRadius(centerX, centerY) > radius) {
    return area();
    }
    // If the bounding boxes do not overlap, the circles cannot either
    } else if (!this.getBounds2D().intersects(other.getBounds2D())) {
    return 0;
    }
    double d = Point2D.distance(centerX, centerY, other.centerX, other.centerY);
    //        double a1 = Math.acos((d2 + r12 - r22) / (2 * d * r1));
    //        double a2 = Math.acos((d2 + r22 - r12) / (2 * d * r2));
    double ang1 = Math.acos((Math.pow(d, 2D) + Math.pow(radius, 2D) - Math.pow(other.radius, 2) / (2D * d * radius)));
    double ang2 = Math.acos((Math.pow(d, 2D) + Math.pow(other.radius, 2D) - Math.pow(radius, 2) / (2D * d * other.radius)));
    // FINISHME:
    // http://www.ambrsoft.com/TrigoCalc/Circles2/circle2intersection/CircleCircleIntersection.htm
    }
     */
    /**
     * Get a circle centered on the center of one quadrant of this one.
     *
     * @param quadrant The quadrant
     * @return
     */
    public Circle quadrantCircle(Quadrant quadrant) {
        double halfRadius = radius / 2D;
        double[] center = positionOf(quadrant.center(), halfRadius);
        return new Circle(center[0], center[1], halfRadius);
    }

    @Override
    public boolean contains(double x, double y) {
        boolean result = distanceToCenter(x, y) <= radius;
        if (factor != 1D) {
            double usableDegrees = factor * 360;
            if (angleOf(x, y) > usableDegrees) {
                return false;
            }
        }
        return result;
    }

    /**
     * Create a circle in which the three passed points are all exactly on the
     * circumference.
     *
     * @param x1 The first x coordinate
     * @param y1 The first y coordinate
     * @param x2 The second x coordinate
     * @param y2 The second y coordinate
     * @param x3 The third x coordinate
     * @param y3 The third y coordinate
     * @return A circle
     */
    public static Circle fromPoints(double x1, double y1, double x2, double y2, double x3, double y3) {
        double xDiff1to2 = x1 - x2;
        double xDiff1to3 = x1 - x3;

        double yDiff1to2 = y1 - y2;
        double yDiff1to3 = y1 - y3;

        double yDiff3to1 = y3 - y1;
        double yDiff2to1 = y2 - y1;

        double xDiff3to1 = x3 - x1;
        double xDiff2to1 = x2 - x1;

        double x1squared = x1 * x1;
        double y1squared = y1 * y1;

        double xSquared1to3 = (x1squared
                - Math.pow(x3, 2));

        double ySquard1to3 = (y1squared
                - Math.pow(y3, 2));
        double xSquared2to1 = (Math.pow(x2, 2)
                - x1squared);
        double ySquared2to1 = (Math.pow(y2, 2)
                - Math.pow(y1, 2));
        double vx = (xSquared1to3 * xDiff1to2
                + ySquard1to3 * xDiff1to2
                + xSquared2to1 * xDiff1to3
                + ySquared2to1 * xDiff1to3)
                / (2 * (yDiff3to1 * xDiff1to2 - yDiff2to1 * xDiff1to3));
        double vy = (xSquared1to3 * yDiff1to2
                + ySquard1to3 * yDiff1to2
                + xSquared2to1 * yDiff1to3
                + ySquared2to1 * yDiff1to3)
                / (2 * (xDiff3to1 * yDiff1to2 - xDiff2to1 * yDiff1to3));

        double c = -x1squared - y1squared
                - 2 * vy * x1 - 2 * vx * y1;

        // eqn of circle be x^2 + y^2 + 2*g*x + 2*f*y + c = 0
        // where centre is (h = -g, k = -f) and radius r
        // as r^2 = h^2 + k^2 - c
        double cx = -vy;
        double cy = -vx;
        double rSquared = cx * cx + cy * cy - c;

        // r is the radius
        double r = Math.sqrt(rSquared);
        return new Circle(cx, cy, r);

    }

    public void nearestPoint(double toX, double toY, DoubleBiConsumer c) {
        double ang = angleOf(toX, toY);
        positionOf(ang, c);
    }

    public EqPointDouble nearestPointTo(double toX, double toY) {
        EqPointDouble result = new EqPointDouble();
        nearestPoint(toX, toY, result::setLocation);
        return result;
    }

    /**
     * Get the distance of a point to the edge of this circle (will be negative
     * for points outside the circle).
     *
     * @param x x coord
     * @param y y coord
     * @return the distance
     */
    public double distanceToRadius(double x, double y) {
        double toCenter = distanceToCenter(x, y);
        return radius - toCenter;
    }

    public Circle scale(double by) {
        return new Circle(centerX, centerY, radius * by);
    }

    @Override
    public String toString() {
        return "Circle(" + radius + " @ " + centerX + ", " + centerY + ")";
    }

    public double centerX() {
        return centerX;
    }

    public double centerY() {
        return centerY;
    }

    public double radius() {
        return radius;
    }

    public double area() {
        return area(radius);
    }

    public static double area(double radius) {
        return Math.PI * (radius * radius);
    }

    public Circle setCenter(Point2D p) {
        return setCenter(p.getX(), p.getY());
    }

    public Circle setCenter(double x, double y) {
        this.centerX = x + 0.0; // avoid -0.0
        this.centerY = y + 0.0;
        return this;
    }

    public Circle setRadius(double radius) {
        this.radius = Math.abs(radius);
        return this;
    }

    public Circle setRotation(double angle) {
        this.rotation = Angle.normalize(angle);
        return this;
    }

    public Circle setRotation(Angle angle) {
        return setRotation(angle.degrees());
    }

    public Circle rotateTo(double x, double y) {
        return setRotation(angleOf(x, y));
    }

    public Angle getAngle(double x, double y) {
        return Angle.ofDegrees(angleOf(x, y));
    }

    public Angle getAngle(Point2D p) {
        return getAngle(p.getX(), p.getY());
    }

    public double angleOf(Point2D p) {
        return angleOf(p.getX(), p.getY());
    }

    public double angleOf(double x, double y) {
        double angle = rotation + ((Math.toDegrees(Math.atan2(x - centerX, centerY - y)) + 360.0) % 360.0);
        return angle;
    }

    /**
     * Get the angle in degrees of the second point on a circle centered on the
     * first point.
     *
     * @param cx The center X coordinate
     * @param cy The center Y coordinate
     * @param tx The test X coordinate
     * @param ty The test Y coordinate
     * @return An angle in degrees, or 0 if both points are the same
     */
    public static double angleOf(double cx, double cy, double tx, double ty) {
        if (cx == tx && cy == ty) {
            return 0;
        }
        return ((Math.toDegrees(Math.atan2(tx - cx, cy - ty)) + 360.0) % 360.0);
    }

    public Quadrant quadrantOf(double x, double y) {
        return Quadrant.forAngle(angleOf(x, y));
    }

    public double distanceToCenter(double x, double y) {
        double distX = x - centerX;
        double distY = y - centerY;
        double len = Math.sqrt((distX * distX) + (distY * distY));
        return len;
    }

    void setUsablePercentage(double factor) {
        this.factor = Math.max(0.001D, Math.min(1D, factor));
    }

    public double[] positionOf(double angle) {
        return positionOf(angle, this.radius, new double[2]);
    }

    public double[] positionOf(double angle, double radius) {
        double[] result = new double[2];
        positionOf(angle, radius, result);
        return result;
    }

    /**
     * Get the position of an angle (in degrees) on the radius of this circle.
     *
     * @param angle The angle, in degrees
     * @param c A consumer
     */
    public void positionOf(double angle, DoubleBiConsumer c) {
        positionOf(angle, this.radius, c);
    }

    /**
     * Fetch the position of an angle (in degrees) on the radius of this circle
     * into the passed array at the requested offset.
     *
     * @param angle The angle, in degrees
     * @param radius The radius
     * @param offset The offset into the array
     * @param into The array
     * @return the passed array
     */
    public double[] positionOf(double angle, double radius, int offset, double[] into) {
        angle -= 90D;
        angle += rotation;
        angle = Math.toRadians(angle);
        into[offset] = radius * cos(angle) + centerX;
        into[offset + 1] = radius * sin(angle) + centerY;
        return into;
    }

    /**
     * Get the position of an angle given a center point and radius.
     *
     * @param angle An angle in degrees.
     * @param cx The center x coordinate
     * @param cy The center y coordinate
     * @param radius The radius
     * @param c A consumer for the result
     */
    public static void positionOf(double angle, double cx, double cy, double radius, DoubleBiConsumer c) {
        angle -= 90D;
        angle = Math.toRadians(angle);
        c.accept(radius * cos(angle) + cx, radius * sin(angle) + cy);
    }

    public static void positionOf(double angle, double cx, double cy, double radius, double[] into, int offset) {
        positionOf(angle, cx, cy, radius, (x, y) -> {
            into[offset] = x;
            into[offset + 1] = y;
        });
    }

    /**
     * Get the position of an angle (in degrees) on the radius of this circle.
     *
     * @param angle The angl3, in degrees
     * @return A point
     */
    public EqPointDouble getPosition(double angle) {
        double[] d = positionOf(angle);
        return new EqPointDouble(d[0], d[1]);
    }

    public double[] positionOf(double angle, double radius, double[] into) {
        angle -= 90D;
        angle += rotation;
        angle = Math.toRadians(angle);
        into[0] = radius * cos(angle) + centerX;
        into[1] = radius * sin(angle) + centerY;
        return into;
    }

    public void positionOf(double angle, double radius, DoubleBiConsumer into) {
        angle -= 90D;
        angle += rotation;
        angle = Math.toRadians(angle);
        double x = radius * cos(angle) + centerX;
        double y = radius * sin(angle) + centerY;
        into.accept(x, y);
    }

    /**
     * Pass the passed consumer a series of points along the radius of this
     * circle.
     *
     * @param count The number of times to subdivide the circumference and call
     * the consumer
     * @param c A consumer
     */
    public void positions(int count, DoubleBiConsumer c) {
        if (count == 0) {
            return;
        }
        final double stepSize = (360D / count) * factor;
        c.accept(centerX, centerY - radius);
        for (int i = 1; i < count; i++) {
            double ang = ((((double) i) * stepSize) - rotation) % 360;
            c.accept(centerX + radius * Math.cos(ang), centerY + radius * Math.sin(ang));
        }
    }

    /**
     * Pass the passed predicate a series of points along the radius of this
     * circle, stopping when it returns false.
     *
     * @param count The number of times to subdivide the circumference and call
     * the consumer
     * @param tester A predicate
     */
    public boolean testPositions(int count, DoubleBiPredicate tester) {
        if (count == 0) {
            return true;
        }
        final double stepSize = (360D / count) * factor;
        if (tester.test(centerX, centerY - radius)) {
            for (int i = 1; i < count; i++) {
                double ang = ((((double) i) * stepSize) - rotation) % 360;
                if (!tester.test(centerX + radius * Math.cos(ang), centerY + radius * Math.sin(ang))) {
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    /**
     * Get an iterator of positions along the circumference, one for each
     * integer coordinate pair along it.
     *
     * @return An iterator
     */
    public Iterator<double[]> positions() {
        return positions((int) Math.floor(circumference()));
    }

    /**
     * Get an iterator of positions along the circumference.
     *
     * @return The number of times to subdivide the circumference
     * @return An iterator
     */
    public Iterator<double[]> positions(final int count) {
        return new It(count);
    }

    /**
     * Shift this circle by the passed delta coordinates.
     *
     * @param deltaX The x delta
     * @param deltaY The y delta
     */
    public void translate(double deltaX, double deltaY) {
        centerX += deltaX;
        centerY += deltaY;
    }

    class It implements Iterator<double[]> {

        private final double step;
        private double deg = 0;

        It(double count) {
            step = (360D / count) * factor;
        }

        @Override
        public boolean hasNext() {
            return deg < 360;
        }

        @Override
        public double[] next() {
            double[] result = positionOf(deg + rotation);
            deg += step;
            return result;
        }
    }

    public Iterable<double[]> iterable(final int count) {
        return () -> positions(count);
    }

    public void getBounds(Rectangle into) {
        int x = toInt(centerX - radius, false);
        int y = toInt(centerY - radius, false);
        int x2 = toInt(centerX + radius, true);
        int y2 = toInt(centerY + radius, true);
        into.setBounds(x, y, x2 - x, y2 - y);
    }

    @Override
    public Rectangle getBounds() {
        int x = toInt(centerX - radius, false);
        int y = toInt(centerY - radius, false);
        int x2 = toInt(centerX + radius, true);
        int y2 = toInt(centerY + radius, true);
        return new Rectangle(x, y, x2 - x, y2 - y);
    }

    @Override
    public Rectangle2D getBounds2D() {
        return new Rectangle2D.Double(centerX - radius, centerY - radius, radius * 2, radius * 2);
    }

    @Override
    public boolean contains(Point2D p) {
        return Math.pow(p.getX() - centerX, 2)
                + Math.pow(p.getY() - centerY, 2) < Math.pow(radius, 2);
    }

    @Override
    public boolean intersects(double x, double y, double w, double h) {
        double dx = centerX - Math.max(x, Math.min(centerX, x + w));
        double dy = centerY - Math.max(y, Math.min(centerY, y + h));
        return (dx * dx + dy * dy) < Math.pow(radius, 2);
    }

    @Override
    public boolean intersects(Rectangle2D r) {
        return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
        return contains(x, y)
                && contains(x + w - 1, y + h - 1)
                && contains(x, y + h - 1)
                && contains(x + w - 1, y);
    }

    @Override
    public boolean contains(Rectangle2D r) {
        return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        if (radius <= 0) {
            return new EmptyPath(centerX, centerY);
        }
        return new PointsPI(centerX, centerY, at, radius);
    }

    public double circumference() {
        return 2D * Math.PI * radius;
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        if (radius == 0 || !Double.isFinite(radius)) {
            return new EmptyPath(centerX, centerY);
        }
        flatness = Math.min(flatness, radius);
        double circ = circumference();
        int count = (int) (Math.round(circ / flatness));
        return new FlatteningPI(at, positions(count));
    }

    private static class FlatteningPI implements PathIterator {

        private final Iterator<double[]> positions;
        private double[] curr;
        private int state;
        private final AffineTransform affine;

        public FlatteningPI(AffineTransform affine, Iterator<double[]> positions) {
            this.positions = positions;
            this.affine = affine;
            if (positions.hasNext()) {
                curr = positions.next();
            }
        }

        @Override
        public int getWindingRule() {
            return WIND_EVEN_ODD;
        }

        @Override
        public boolean isDone() {
            return state > 2;
        }

        @Override
        public void next() {
            switch (state) {
                case 0:
                case 1:
                    if (state == 0) {
                        state++;
                    }
                    if (positions.hasNext()) {
                        curr = positions.next();
                    } else {
                        state = 2;
                        curr = null;
                    }
                    break;
                case 2:
                    state++;
                    curr = null;
                    break;
            }
        }

        @Override
        public int currentSegment(double[] coords) {
            switch (state) {
                case 0:
                case 1:
                    System.arraycopy(curr, 0, coords, 0, 2);
                    if (affine != null) {
                        affine.transform(coords, 0, coords, 0, 1);
                    }
                case 2:
                    break;
                default:
                    throw new NoSuchElementException("completed");
            }
            switch (state) {
                case 0:
                    return PathIterator.SEG_MOVETO;
                case 1:
                    return PathIterator.SEG_LINETO;
                case 2:
                    return PathIterator.SEG_CLOSE;
                default:
                    throw new NoSuchElementException("completed");
            }
        }

        @Override
        public int currentSegment(float[] coords) {
            switch (state) {
                case 0:
                case 1:
                    coords[0] = (float) curr[0];
                    coords[1] = (float) curr[1];
                    if (affine != null) {
                        affine.transform(coords, 0, coords, 0, 1);
                    }
                    break;
                case 2:
                    break;
                default:
            }
            switch (state) {
                case 0:
                    state++;
                    return PathIterator.SEG_MOVETO;
                case 1:
                    return PathIterator.SEG_LINETO;
                case 2:
                    state++;
                    return PathIterator.SEG_CLOSE;
                default:
                    throw new NoSuchElementException("completed");
            }
        }
    }

    private static final class EmptyPath implements PathIterator {

        private final double centerX, centerY;
        private int ix = 0;

        public EmptyPath(double centerX, double centerY) {
            this.centerX = centerX;
            this.centerY = centerY;
        }

        @Override
        public int getWindingRule() {
            return WIND_EVEN_ODD;
        }

        @Override
        public boolean isDone() {
            return ix >= 2;
        }

        @Override
        public void next() {
            ix++;
        }

        @Override
        public int currentSegment(float[] coords) {
            if (isDone()) {
                throw new NoSuchElementException("completed");
            }
            switch (ix) {
                case 0:
                    coords[0] = (float) centerX;
                    coords[1] = (float) centerY;
                    return PathIterator.SEG_MOVETO;
                case 1:
                    return PathIterator.SEG_CLOSE;
                default:
                    throw new AssertionError();
            }
        }

        @Override
        public int currentSegment(double[] coords) {
            if (isDone()) {
                throw new NoSuchElementException("completed");
            }
            switch (ix) {
                case 0:
                    coords[0] = centerX;
                    coords[1] = centerY;
                    return PathIterator.SEG_MOVETO;
                case 1:
                    return PathIterator.SEG_CLOSE;
                default:
                    throw new AssertionError();
            }
        }
    }

    static class PointsPI implements PathIterator {

        private final double centerX;
        private final double centerY;
        private final AffineTransform affine;
        private int index;
        private final double radius;

        public PointsPI(double centerX, double centerY, AffineTransform xform, double radius) {
            this.centerX = centerX - radius;
            this.centerY = centerY - radius;
            this.affine = xform;
            this.radius = radius * 2D;
        }

        public static final double CONTROL = 0.5522847498307933;
        private static final double PCV = 0.5 + CONTROL * 0.5;
        private static final double NCV = 0.5 - CONTROL * 0.5;
        private static double CTRLPTS[][] = {
            {1.0, PCV, PCV, 1.0, 0.5, 1.0},
            {NCV, 1.0, 0.0, PCV, 0.0, 0.5},
            {0.0, NCV, NCV, 0.0, 0.5, 0.0},
            {PCV, 0.0, 1.0, NCV, 1.0, 0.5}
        };

        @Override
        public int getWindingRule() {
            return WIND_NON_ZERO;
        }

        @Override
        public boolean isDone() {
            return index > 5;
        }

        @Override
        public void next() {
            index++;
        }

        @Override
        public int currentSegment(float[] coords) {
            if (isDone()) {
                throw new NoSuchElementException("ellipse iterator out of bounds");
            }
            double x = centerX;
            double y = centerY;
            if (index == 5) {
                return SEG_CLOSE;
            }
            if (index == 0) {
                double ctrls[] = CTRLPTS[3];
                coords[0] = (float) (x + ctrls[4] * radius);
                coords[1] = (float) (y + ctrls[5] * radius);
                if (affine != null) {
                    affine.transform(coords, 0, coords, 0, 1);
                }
                return SEG_MOVETO;
            }
            double ctrls[] = CTRLPTS[index - 1];
            coords[0] = (float) (x + ctrls[0] * radius);
            coords[1] = (float) (y + ctrls[1] * radius);
            coords[2] = (float) (x + ctrls[2] * radius);
            coords[3] = (float) (y + ctrls[3] * radius);
            coords[4] = (float) (x + ctrls[4] * radius);
            coords[5] = (float) (y + ctrls[5] * radius);
            if (affine != null) {
                affine.transform(coords, 0, coords, 0, 3);
            }
            return SEG_CUBICTO;
        }

        @Override
        public int currentSegment(double[] coords) {
            if (isDone()) {
                throw new NoSuchElementException("ellipse iterator out of bounds");
            }
            double x = centerX;
            double y = centerY;
            if (index == 5) {
                return SEG_CLOSE;
            }
            if (index == 0) {
                double ctrls[] = CTRLPTS[3];
                coords[0] = (x + ctrls[4] * radius);
                coords[1] = (y + ctrls[5] * radius);
                if (affine != null) {
                    affine.transform(coords, 0, coords, 0, 1);
                }
                return SEG_MOVETO;
            }
            double ctrls[] = CTRLPTS[index - 1];
            coords[0] = (x + ctrls[0] * radius);
            coords[1] = (y + ctrls[1] * radius);
            coords[2] = (x + ctrls[2] * radius);
            coords[3] = (y + ctrls[3] * radius);
            coords[4] = (x + ctrls[4] * radius);
            coords[5] = (y + ctrls[5] * radius);
            if (affine != null) {
                affine.transform(coords, 0, coords, 0, 3);
            }
            return SEG_CUBICTO;
        }
    }

    public EqLine line(double angle) {
        double[] xy1 = positionOf(angle);
        double[] xy2 = positionOf(angle + 180);
        return new EqLine(xy1[0], xy1[1], xy2[0], xy2[1]);
    }

    public EqLine halfLine(double angle) {
        double[] xy1 = positionOf(angle);
        return new EqLine(centerX, centerY, xy1[0], xy1[1]);
    }

    public EqLine line(double angle, double radius) {
        double[] xy1 = positionOf(angle, radius);
        double[] xy2 = positionOf(angle + 180);
        return new EqLine(xy1[0], xy1[1], xy2[0], xy2[1]);
    }

    public EqLine halfLine(double angle, double radius) {
        double[] xy1 = positionOf(angle, radius);
        return new EqLine(centerX, centerY, xy1[0], xy1[1]);
    }

    public Rectangle2D halfBounds(Quadrant quad, Axis axis, boolean interior) {
        return halfBounds(quad, axis == Axis.VERTICAL, interior);
    }

    public Rectangle2D halfBounds(Quadrant quad, boolean eastWestAxis, boolean interior) {
        Rectangle2D.Double result;
        Quadrant other;
        switch (quad) {
            case NORTHEAST:
                other = eastWestAxis ? Quadrant.SOUTHEAST : Quadrant.NORTHWEST;
                break;
            case NORTHWEST:
                other = eastWestAxis ? Quadrant.SOUTHWEST : Quadrant.NORTHEAST;
                break;
            case SOUTHEAST:
                other = eastWestAxis ? Quadrant.NORTHEAST : Quadrant.SOUTHWEST;
                break;
            case SOUTHWEST:
                other = eastWestAxis ? Quadrant.NORTHWEST : Quadrant.SOUTHEAST;
                break;
            default:
                throw new AssertionError(quad);
        }
        result = quadrantBounds(quad, interior);
        result.add(quadrantBounds(other, interior));
        return result;
    }

    public Point2D.Double corner(Quadrant quadrant) {
        switch (quadrant) {
            case NORTHEAST:
                return new Point2D.Double(centerX + radius, centerY - radius);
            case NORTHWEST:
                return new Point2D.Double(centerX - radius, centerY - radius);
            case SOUTHEAST:
                return new Point2D.Double(centerX + radius, centerY + radius);
            case SOUTHWEST:
                return new Point2D.Double(centerX - radius, centerY + radius);
            default:
                throw new AssertionError(quadrant);
        }
    }

    public Rectangle2D outBounds(Quadrant quad) {
        double center = quad.center();
        double[] pos = positionOf(center);
        Rectangle2D.Double result = new Rectangle2D.Double();
        Point2D.Double corner = corner(quad);
        result.setFrameFromDiagonal(corner, new Point2D.Double(pos[0], pos[1]));
        return result;
    }

    public Rectangle2D.Double quadrantBounds(Quadrant quad, boolean interior) {
        Rectangle2D.Double result;
        if (!interior) {
            result = new Rectangle2D.Double(centerX - radius, centerX - radius, centerX, centerY);
            switch (quad) {
                case NORTHWEST:
                    break;
                case NORTHEAST:
                    result.x += radius;
                    break;
                case SOUTHEAST:
                    result.x += radius;
                    result.y += radius;
                    break;
                case SOUTHWEST:
                    result.y += radius;
                    break;
                default:
                    throw new AssertionError(quad);
            }
        } else {
            result = new Rectangle2D.Double();
            double center = quad.center();
            double[] xy = positionOf(center);
            result.setFrameFromDiagonal(centerX, centerY, xy[0], xy[1]);
        }
        return result;
    }

    /**
     * Get the bounding box of a line at some radius across the circumference of
     * the circle; note that the returned bounding box will be larger than the
     * circle in at least one dimension, so it includes all points within the
     * sweep. Returns the rectangle that would completely contain such a line.
     *
     * @param angle
     * @return
     */
    public Rectangle2D angleBounds(double angle) {
        double[] xy1 = positionOf(angle);
        double[] xy2 = positionOf(angle + 180);
        Rectangle2D result = new Rectangle2D.Double();
        result.setFrameFromDiagonal(xy1[0], xy1[1], xy2[0], xy2[1]);
        if (result.getWidth() < result.getHeight()) {
            result.setRect(result.getX(), centerY - radius, result.getWidth(), radius * 2);
        } else if (result.getHeight() < result.getWidth()) {
            result.setRect(centerX - radius, result.getY(), radius * 2, result.getHeight());
        } else {
            result.setFrame(getBounds2D());
        }
        return result;
    }

    public EqLine tangent(double angle) {
        double[] tangentCenter = positionOf(angle);
        Circle c = new Circle(tangentCenter[0], tangentCenter[1], radius);
        return c.line(angle - 90);
    }

    public EqLine tangent(double angle, double length) {
        double[] tangentCenter = positionOf(angle);
        Circle c = new Circle(tangentCenter[0], tangentCenter[1], length);
        return c.line(angle - 90, length);
    }

    /**
     * Scan all lines which intersect this circle at the passed angle, with step
     * positions between them, starting at the tangent line to the passed angle
     * plus the passed step.
     *
     *
     * @param angle An angle
     * @param step The spacing between lines to scan
     * @param c A consumer which will be passed the x1, y1, x2, y2 coordinates
     * of the line to test
     * @return The number of lines scanned
     */
    public int scan(double angle, double step, DoubleQuadConsumer c) {
        if (radius == 0 || step <= 0) {
            return 0;
        }
        // Get a line which is at a tangent to the angle
        Line2D.Double line = tangent(angle);
        // Get its quadrant
        Quadrant angleQuadrant = Quadrant.forAngle(angle);

        // With a circle of radius 1, we can get x/y multipliers
        // for the step, so we produce evenly-spaced lines at all
        // angles
        Circle c1 = new Circle(0, 0, 1);
        double[] dist = c1.positionOf(angle);

        // Take our multipliers and shift them in the direction of
        // the angle
        dist = angleQuadrant.opposite().direct(dist[0], dist[1]);

        // Create the step amounts
        double xStep = dist[0] * step;
        double yStep = dist[1] * step;

        // Increment each coordinate once so we move our tangent line one
        // step toward the center, so we will get a non-null intersecting
        // line
        line.x1 += xStep;
        line.y1 += yStep;
        line.x2 += xStep;
        line.y2 += yStep;

        assert line.getP1().distance(line.getP2()) < (radius * 2 + 1) : "Bogus line " + GeometryStrings.lineToString(line.x1, line.y1, line.x2, line.y2);
        //Line2D.Double lastIsect = null;
        Line2D.Double lisect;
        int count = 0;
        do {
            lisect = intersection(line);
            if (lisect != null) {
                // Pass the coordinates to the calllback
                c.accept(lisect.x1, lisect.y1, lisect.x2, lisect.y2);
                // Increment the line position
                line.x1 += xStep;
                line.y1 += yStep;
                line.x2 += xStep;
                line.y2 += yStep;
                count++;
            }
        } while (lisect != null);
        return count;
    }

    static String l2s(Line2D.Double ln) {
        if (ln == null) {
            return "<null>";
        }
        return ln.x1 + ", " + ln.y1 + " <--> " + ln.x2 + ", " + ln.y2;
    }

    public EqLine intersection(Line2D orig) {

        double bx = orig.getX2();
        double ax = orig.getX1();
        double by = orig.getY2();
        double ay = orig.getY1();
        double cx = centerX;
        double cy = centerY;
        double r = radius;
        // compute the euclidean distance between A and B
        double LAB = Math.sqrt(Math.pow(bx - ax, 2) + Math.pow(by - ay, 2));

        // compute the direction vector D from A to B
        double Dx = (bx - ax) / LAB;
        double Dy = (by - ay) / LAB;

        // the equation of the line AB is x = Dx*t + Ax, y = Dy*t + Ay with 0 <= t <= LAB.
        // compute the distance between the points A and E, where
        // E is the point of AB closest the circle center (Cx, Cy)
        double t = Dx * (cx - ax) + Dy * (cy - ay);

        // compute the coordinates of the point E
        double Ex = t * Dx + ax;
        double Ey = t * Dy + ay;

        // compute the euclidean distance between E and C
        double LEC = Math.sqrt(Math.pow(Ex - cx, 2) + Math.pow(Ey - cy, 2));

        // test if the line intersects the circle
        if (LEC < r) {
            // compute distance from t to circle intersection point
            double dt = Math.sqrt(Math.pow(r, 2) - Math.pow(LEC, 2));

            // compute first intersection point
            double Fx = (t - dt) * Dx + ax;
            double Fy = (t - dt) * Dy + ay;

            // compute second intersection point
            double Gx = (t + dt) * Dx + ax;
            double Gy = (t + dt) * Dy + ay;
            return new EqLine(Fx, Fy, Gx, Gy);
        } else if (LEC == r) {
            // else test if the line is tangent to circle
            // tangent point to circle is E
        } else {
            // line doesn't touch circle
        }
        return null;
    }
}
