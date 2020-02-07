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
package org.netbeans.paint.geom;

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

/**
 *
 * @author Tim Boudreau
 */
public class Circle implements Shape {

    double centerX;
    double centerY;
    double radius = 10D;
    private double rotation;

    public Circle() {
        this(new Point2D.Double());
    }

    public Circle(Point2D center) {
        centerX = center.getX();
        centerY = center.getY();
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
        this.radius = radius;
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
    public static double angleBetween(double angleA, double angleB) {
        if (angleA == angleB) {
            return angleA;
        }
        angleA = Quadrant.normalize(angleA);
        angleB = Quadrant.normalize(angleB);
        double a = Math.min(angleA, angleB);
        double b = Math.max(angleA, angleB);
        return a + ((b - a) / 2D);
    }

    public Circle quadrantCircle(Quadrant quadrant) {
        double halfRadius = radius / 2D;
        double[] center = positionOf(quadrant.center(), halfRadius);
        return new Circle(center[0], center[1], halfRadius);
    }

    @Override
    public boolean contains(double x, double y) {
        boolean result = distanceToCenter(x, y) < radius;
        if (factor != 1D) {
            double usableDegrees = factor * 360;
            if (angleOf(x, y) > usableDegrees) {
                return false;
            }
        }
        return result;
    }

    public double distanceToRadius(double x, double y) {
        double toCenter = distanceToCenter(x, y);
        return radius - toCenter;
    }

    public static double perpendicularClockwise(double angle) {
        if (!Double.isFinite(angle)) {
            return angle;
        }
        if (angle < 0) {
            angle = 360D + angle;
        }
        if (angle > 360) {
            angle = 360D % angle;
        }
        if (angle + 90D > 360) {
            return (angle + 90D) - 360;
        } else {
            return angle + 90D;
        }
    }

    public static double perpendicularCounterclockwise(double angle) {
        if (!Double.isFinite(angle)) {
            return angle;
        }
        if (angle < 0) {
            angle = 360D + angle;
        }
        if (angle > 360) {
            angle = 360D % angle;
        }
        if (angle - 90D < 0) {
            return 360D + (angle - 90D);
        } else {
            return angle - 90D;
        }
    }

    public static double opposite(double angle) {
        if (!Double.isFinite(angle)) {
            return angle;
        }
        if (angle < 0) {
            angle = 360D + angle;
        }
        if (angle > 360) {
            angle = 360D % angle;
        }
        if (angle == 0D) {
            return 180D;
        } else if (angle == 180D) {
            return 0;
        } else if (angle == 90D) {
            return 270D;
        }
        if (angle > 180D) {
            return angle - 180D;
        } else {
            return angle + 180D;
        }
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

    public Circle setCenter(double x, double y) {
        this.centerX = x;
        this.centerY = y;
        return this;
    }

    public Circle setRadius(double radius) {
        this.radius = radius;
        return this;
    }

    public Circle setRotation(double angle) {
        this.rotation = angle;
        return this;
    }

    /**
     * THe angle of a line through the two passed points, if the center is an
     * equidistant point and we take the angle of the second point.
     *
     * @param x1 x1
     * @param y1 y1
     * @param x2 x2
     * @param y2 y2
     * @return an angle
     */
    public static double angle(double x1, double y1, double x2, double y2) {
        double[] db = equidistantPoint(x1, y1, x2, y2);
        double result = Math.round(new Circle(db[0], db[1]).angleOf(x2, y2) - 180);
        if (result < 0) {
            result += 360;
        }
        return result;
    }

    public static double[] equidistantPoint(double x1, double y1, double x2, double y2) {
        return new double[]{(x1 + x2) / 2D, (y1 + y2) / 2D};
    }

    public double angleOf(double x, double y) {
        double angle = rotation + ((Math.toDegrees(Math.atan2(x - centerX, centerY - y)) + 360.0) % 360.0);
        return angle;
    }

    public Quadrant quadrantOf(double angle) {
        return Quadrant.forAngle(angle);
    }

    public Quadrant quadrantOf(double x, double y) {
        double angle = angleOf(x, y);
        return quadrantOf(angle);
    }

    public double distanceToCenter(double x, double y) {
        double distX = x - centerX;
        double distY = y - centerY;
        double len = Math.sqrt((distX * distX) + (distY * distY));
        return len;
    }

    double factor = 1D;

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

    /*
    public void positionOf(double angle, DoubleBiConsumer c) {
        positionOf(angle, this.radius, c);
    }
     */
    public double[] positionOf(double angle, double radius, double[] into) {
        angle -= 90D;
        angle += rotation;
        angle = Math.toRadians(angle);
        into[0] = radius * cos(angle) + centerX;
        into[1] = radius * sin(angle) + centerY;
        return into;
    }

    /*
    public void positionOf(double angle, double radius, DoubleBiConsumer into) {
        angle -= 90D;
        angle += rotation;
        angle = Math.toRadians(angle);
        double x = radius * cos(angle) + centerX;
        double y = radius * sin(angle) + centerY;
        into.accept(x, y);
    }
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

    public boolean testPositions(int count, DoubleBiPredicate c) {
        if (count == 0) {
            return true;
        }
        final double stepSize = (360D / count) * factor;
        if (c.test(centerX, centerY - radius)) {
            for (int i = 1; i < count; i++) {
                double ang = ((((double) i) * stepSize) - rotation) % 360;
                if (!c.test(centerX + radius * Math.cos(ang), centerY + radius * Math.sin(ang))) {
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }
     */
    public Iterator<double[]> positions() {
        return positions((int) Math.floor(circumference()));
    }

    public Iterator<double[]> positions(final int count) {
        return new It(count);
    }

    void translate(double x, double y) {
        centerX += x;
        centerY += y;
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

    private static int toInt(double val, boolean biasUp) {
        return (int) (biasUp ? Math.ceil(val) : Math.floor(val));
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
        // (x - center_x)^2 + (y - center_y)^2 < radius^2
        return Math.pow(p.getX() - centerX, 2)
                + Math.pow(p.getY() - centerY, 2) < Math.pow(radius, 2);
//        return contains(p.getX(), p.getY());
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
                    coords[0] = Math.round(curr[0]);
                    coords[1] = Math.round(curr[1]);
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
        // ArcIterator.btan(Math.PI/2)

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

    public Line2D.Double line(double angle) {
        double[] xy1 = positionOf(angle);
        double[] xy2 = positionOf(angle + 180);
        return new Line2D.Double(xy1[0], xy1[1], xy2[0], xy2[1]);
    }

    public Line2D.Double halfLine(double angle) {
        double[] xy1 = positionOf(angle);
        return new Line2D.Double(centerX, centerY, xy1[0], xy1[1]);
    }

    public Line2D.Double line(double angle, double radius) {
        double[] xy1 = positionOf(angle, radius);
        double[] xy2 = positionOf(angle + 180);
        return new Line2D.Double(xy1[0], xy1[1], xy2[0], xy2[1]);
    }

    public Line2D.Double halfLine(double angle, double radius) {
        double[] xy1 = positionOf(angle, radius);
        return new Line2D.Double(centerX, centerY, xy1[0], xy1[1]);
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

    public Line2D.Double tangent(double angle) {
        double[] tangentCenter = positionOf(angle);
        Circle c = new Circle(tangentCenter[0], tangentCenter[1], radius);
        return c.line(angle - 90);
    }

    /*
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
    public int scan(double angle, double step, DoubleQuadConsumer c) {
        if (radius == 0 || step <= 0) {
            return 0;
        }
        // Get a line which is at a tangent to the angle
        Line2D.Double line = tangent(angle);
        // Get its quadrant
        Quadrant angleQuadrant = quadrantOf(angle);

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

        assert line.getP1().distance(line.getP2()) < (radius * 2 + 1) : "Bogus line " + l2s(line);
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
     */
    static String l2s(Line2D.Double ln) {
        if (ln == null) {
            return "<null>";
        }
        return ln.x1 + ", " + ln.y1 + " <--> " + ln.x2 + ", " + ln.y2;
    }

    public Line2D.Double intersection(Line2D.Double orig) {

        double bx = orig.x2;
        double ax = orig.x1;
        double by = orig.y2;
        double ay = orig.y1;
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
            return new Line2D.Double(Fx, Fy, Gx, Gy);
        } else if (LEC == r) {
            // else test if the line is tangent to circle
            // tangent point to circle is E
        } else {
            // line doesn't touch circle
        }
        return null;
    }
}
