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
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import org.imagine.geometry.util.GeometryStrings;
import org.imagine.geometry.util.GeometryUtils;

/**
 * A Line2D.Double implementation which implements equals and hash code
 * reasonably, and provides utility methods for angle, length, midpoint and
 * adjustments thereof.
 *
 * @author Tim Boudreau
 */
public final class EqLine extends Line2D.Double implements Intersectable {

    public EqLine() {
    }

    public EqLine(double x1, double y1, double x2, double y2) {
        this.x1 = x1 == -0.0 ? 0 : x1;
        this.y1 = y1 == -0.0 ? 0 : y1;
        this.x2 = x2 == -0.0 ? 0 : x2;
        this.y2 = y2 == -0.0 ? 0 : y2;
    }

    public EqLine(Line2D line) {
        this(line.getX1(), line.getY1(), line.getX2(), line.getY2());
    }

    public EqLine(Point2D pd, Point2D pd1) {
        this(pd.getX(), pd.getY(), pd1.getX(), pd1.getY());
    }

    public static EqLine forAngleAndLength(double x, double y, double angle, double length) {
        EqLine ln = new EqLine(x, y, 1, 1);
        ln.setAngleAndLength(angle, length);
        return ln;
    }

    public static EqLine of(Line2D line) {
        if (line == null) {
            return null;
        }
        if (line instanceof EqLine) {
            return (EqLine) line;
        }
        return new EqLine(line);
    }

    public void shiftPerpendicular(double by) {
        if (by == 0) {
            return;
        }
        double ang = angle();
        double perp = by > 0 ? Angle.perpendicularClockwise(ang)
                : Angle.perpendicularCounterclockwise(ang);
        Circle.positionOf(perp, 0, 0, Math.abs(by), this::translate);
    }

    /**
     * Apply the passed Affine Transform to the points of this line.
     *
     * @param xform A transform
     */
    public void applyTransform(AffineTransform xform) {
        double[] pts = new double[]{x1, y1, x2, y2};
        xform.transform(pts, 0, pts, 0, 2);
        x1 = pts[0];
        y1 = pts[1];
        x2 = pts[2];
        y2 = pts[3];
    }

    /**
     * Shift this line's x and y coordinates by the passed values.
     *
     * @param deltaX The difference X coordinate
     * @param deltaY The difference Y coordinate
     */
    public void translate(double deltaX, double deltaY) {
        x1 += deltaX;
        y1 += deltaY;
        x2 += deltaX;
        y2 += deltaY;
    }

    /**
     * Create a copy of this line translated by the passed x and y offsets.
     *
     * @param deltaX The x axis offset
     * @param deltaY The y axis offset
     * @return A line
     */
    public EqLine translatedBy(double deltaX, double deltaY) {
        return new EqLine(x1 + deltaX, y1 + deltaY,
                x2 + deltaX, y2 + deltaY);
    }

    /**
     * Returns a copy of this line with point one moved to the passed
     * coordinates, and point two moved so it has the same relationship to point
     * one as in this line.
     *
     * @param newX The new x coordinate
     * @param newY The new y coordinate
     * @return A new line
     */
    public EqLine withPoint1(double newX, double newY) {
        double newX2 = x2 + (newX - x1);
        double newY2 = y2 + (newY - y1);
        return new EqLine(newX, newY, newX2, newY2);
    }

    /**
     * Returns a copy of this line with point two moved to the passed
     * coordinates, and point one moved so it has the same relationship to point
     * two as in this line.
     *
     * @param newX The new x coordinate
     * @param newY The new y coordinate
     * @return A new line
     */
    public EqLine withPoint2(double newX, double newY) {
        double newX1 = x1 + (newX - x2);
        double newY1 = y1 + (newY - y2);
        return new EqLine(newX1, newY1, newX, newY);
    }

    /**
     * Create a new line with the same length as this one, rotated by the passed
     * angle in degrees around its center point.
     *
     * @param angle
     * @return
     */
    public EqLine rotatedBy(double angle) {
        if (angle == 0D || angle == 360D || angle % 360D == 0) {
            return new EqLine(this);
        }
        EqPointDouble mid = midPoint();
        Circle circ = new Circle(mid.x, mid.y, length() / 2D);
        return circ.line(angle);
    }

    public EqLine setAngleAndLength(double angle, double length) {
        Circle.positionOf(angle, x1, y1, length, (x, y) -> {
            x2 = x;
            y2 = y;
        });
        return this;
    }

    public double distanceIn(Axis axis) {
        switch (axis) {
            case HORIZONTAL:
                return Math.abs(x2 - x1);
            case VERTICAL:
                return Math.abs(y2 - y1);
            default:
                throw new AssertionError(axis);
        }
    }

    public Axis nearestAxis() {
        double ang = Angle.canonicalize(angle());
        if (ang >= 45 && ang < 135) {
            return Axis.VERTICAL;
        } else {
            return Axis.HORIZONTAL;
        }
    }

    /**
     * Set of this line, changing the coordinates of the second point.
     *
     * @param newLength The new length
     */
    public void setLength(double newLength) {
        setLength(newLength, false);
    }

    /**
     * Set the length of this line.
     *
     * @param newLength The new length
     * @param firstPoint Whether to change the first or second point
     */
    public void setLength(double newLength, boolean firstPoint) {
        if (newLength == 0) {
            if (firstPoint) {
                x1 = x2;
                y1 = y2;
            } else {
                x2 = x1;
                y2 = y1;
            }
        } else {
            double ang = angle(!firstPoint);
            if (newLength < 0) {
                ang = Angle.opposite(ang);
                newLength = -newLength;
            }
            Circle circle = new Circle(firstPoint ? x1 : x2, firstPoint ? y1 : y2, newLength);
            double[] newPos = circle.positionOf(ang);
            if (firstPoint) {
                x1 = newPos[0];
                y1 = newPos[1];
            } else {
                x2 = newPos[0];
                y2 = newPos[1];
            }
        }
    }

    /**
     * Increase (or decrease if negative) the length of this point by the passed
     * amount, altering the second point.
     *
     * @param addedLength The additional length
     */
    public void extend(double addedLength) {
        extend(addedLength, false);
    }

    /**
     * Increase (or decrease) the length of this line.
     *
     * @param addedLength The additional length
     * @param firstPoint Whether to alter the position of the first or second
     * point
     */
    public void extend(double addedLength, boolean firstPoint) {
        if (addedLength == 0) {
            return;
        }
        Circle circle = new Circle(firstPoint ? x1 : x2, firstPoint ? y1 : y2, 0);
        double ang = angle();
        if (addedLength < 0) {
            addedLength = -addedLength;
            ang = Angle.opposite(ang);
        }
        double[] nue = circle.positionOf(ang, addedLength);
        if (firstPoint) {
            x1 = nue[0];
            y1 = nue[1];
        } else {
            x2 = nue[0];
            y2 = nue[1];
        }
    }

    public EqLine translated(double x, double y) {
        return new EqLine(x1 + x, y1 + y, x2 + x, y2 + y);
    }

    /**
     * Determine if this line is empty within a tolerance.
     *
     * @param tolerance the minimum difference between coordinates to consider
     * @return true if it is empty
     */
    public boolean isEmpty(double tolerance) {
        return GeometryUtils.isSamePoint(x1, y1, x2, y2, tolerance);
    }

    /**
     * Determine if this line is horizontal within a tolerance.
     *
     * @param tolerance the minimum difference between coordinates to consider
     * @return true if it is horizontal
     */
    public boolean isVertical(double tolerance) {
        return GeometryUtils.isSameCoordinate(x1, x2, tolerance);
    }

    /**
     * Determine if this line is vertical within a tolerance.
     *
     * @param tolerance the minimum difference between coordinates to consider
     * @return true if it is vertical
     */
    public boolean isHorizontal(double tolerance) {
        return GeometryUtils.isSameCoordinate(y1, y2, tolerance);
    }

    /**
     * Determine if this line is empty.
     *
     * @param tolerance the minimum difference between coordinates to consider
     * @return true if it is empty
     */
    public boolean isEmpty() {
        return GeometryUtils.isSamePoint(x1, y1, x2, y2);
    }

    /**
     * Determine if this line is horizontal.
     *
     * @return true if it is horizontal
     */
    public boolean isVertical() {
        return GeometryUtils.isSameCoordinate(x1, x2);
    }

    /**
     * Determine if this line is vertical.
     *
     * @return true if it is vertical
     */
    public boolean isHorizontal() {
        return GeometryUtils.isSameCoordinate(y1, y2);
    }

    /**
     * Get the axis of this line, if it is vertical or horizontal, or null.
     *
     * @return An axis or null
     */
    public Axis axis() {
        return isVertical()
                ? Axis.VERTICAL
                : isHorizontal()
                        ? Axis.HORIZONTAL
                        : null;
    }

    /**
     * Determine if the points are ordered such that the first point has a y
     * and/or x coordinate which is less than the second point.
     *
     * @return True if this line is normalized
     */
    public boolean isNormalized() {
        return y1 < y2
                ? true
                : GeometryUtils.isSameCoordinate(y1, y2)
                ? x1 < x2 : false;
    }

    /**
     * Get the angle of this line in degrees, in a coordinate space where 0° is
     * 12:00, using whichever point is top-leftmost as the apex.
     *
     * @return An angle in degrees, or Double.NaN if this line is empty (first
     * and second points are the same).
     */
    public double angleNormalized() {
        if (isEmpty()) {
            return java.lang.Double.NaN;
        }
        return Angle.canonicalize(angle());
    }

    public void setAngle(double angle) {
        setAngle(angle, false);
    }

    public void setAngle(double angle, boolean fromSecondPoint) {
        angle = Angle.normalize(angle);
        double len = length();
        if (len == 0) {
            return;
        }
        if (fromSecondPoint) {
            Circle.positionOf(angle, x2, y2, len, this::setPoint1);
        } else {
            Circle.positionOf(angle, x1, y1, len, this::setPoint2);
        }
    }

    public void setPoint1(double x, double y) {
        x1 = x == -0.0 ? 0 : x;
        y1 = y == -0.0 ? 0 : y;
    }

    public void setPoint2(double x, double y) {
        x2 = x == -0.0 ? 0 : x;
        y2 = y == -0.0 ? 0 : y;
    }

    public void setPoint1(Point2D pt) {
        setPoint1(pt.getX(), pt.getY());
    }

    public void setPoint2(Point2D pt) {
        setPoint2(pt.getX(), pt.getY());
    }

    /**
     * Get the angle of this line in degrees, in a coordinate space where 0° is
     * 12:00, using point 1 as the apex.
     *
     * @return An angle in degrees
     */
    public double angle() {
        return angle(false);
    }

    public double canonicalAngle() {
        return Angle.canonicalize(angle());
    }

    /**
     * Get the angle of this line, in a coordinate space where 0° is 12:00.
     *
     * @param fromSecondPoint If true, the angle using x2,y2 as the apex
     * @return An angle in degrees &gt;= 0\u00B0 and &lt; 360\u00B0
     */
    public double angle(boolean fromSecondPoint) {
        if (fromSecondPoint) {
            return Angle.ofLine(x2, y2, x1, y1);
        } else {
            return Angle.ofLine(x1, y1, x2, y2);
        }
    }

    /**
     * Swap the x1,y1 and x2,y2 points in-place.
     */
    public void swap() {
        double hx = x1;
        double hy = y1;
        x1 = x2;
        y1 = y2;
        x2 = hx;
        y2 = hy;
    }

    /**
     * Normalize this line in-place, so the top-leftmost coordinate pair is the
     * first point.
     *
     * @return true if the coordinates were changed
     */
    public boolean normalize() {
        if (y1 > y2) {
            swap();
            return true;
        } else if (GeometryUtils.isSameCoordinate(y1, y2)) {
            if (x1 > x2) {
                swap();
                return true;
            }
        }
        return false;
    }

    @Override
    public EqPointDouble getP2() {
        return new EqPointDouble(x2, y2);
    }

    @Override
    public EqPointDouble getP1() {
        return new EqPointDouble(x1, y1);
    }

    /**
     * Get whichever point is top-leftmost.
     *
     * @return A point
     */
    public EqPointDouble leastPoint() {
        return isNormalized() ? getP1() : getP2();
    }

    /**
     * Get whichever point is bottom-rightmost.
     *
     * @return A point
     */
    public EqPointDouble greatestPoint() {
        return isNormalized() ? getP2() : getP1();
    }

    /**
     * Get the slope of this line.
     *
     * @return The slope
     */
    public double slope() {
        return (y2 - y1) / (x2 - x1);
    }

    /**
     * Get the slope of this line, using the top-leftmost point as the second
     * point.
     *
     * @return The slope
     */
    public double slopeNormalized() {
        if (isNormalized()) {
            return slope();
        } else {
            return (y2 - y1) / (x2 - x1);
        }
    }

    /**
     * Determine if these lines are parallel.
     *
     * @param other another line
     * @return
     */
    public boolean isParallel(Line2D other) {
        if (other == this) {
            return true;
        }
        if (isEmpty()) {
            return false;
        }
        EqLine o = EqLine.of(other);
        if (o.isEmpty()) {
            return false;
        }
        return GeometryUtils.isSameCoordinate(angleNormalized(), o.angleNormalized());
    }

    /**
     * Get the intersection of this line and another, if they intersect within
     * their bounds.
     *
     * @param other another line
     * @return A point or null if they do not intersect, if either line is
     * empty, or if the lines are homomorphic
     */
    public EqPointDouble intersection(Line2D other) {
        EqPointDouble result = intersectionPoint(other);
        if (result != null) {
            double minX = Math.min(x1, x2);
            double maxX = Math.max(x1, x2);
            double minY = Math.min(y1, y2);
            double maxY = Math.max(y1, y2);
            if (result.x >= minX && result.x <= maxX && result.y >= minY && result.y <= maxY) {
                return result;
            }
        }
        return result;
    }

    /**
     * Get the intersection of this line and another at any point in or outside
     * the bounds of each, where they would intersect if extended.
     *
     * @param other Another line
     * @return A point or null if they do not intersect, if either line is
     * empty, or if the lines are homomorphic
     */
    public EqPointDouble intersectionPoint(Line2D other) {
        if (other == this || equalsNormalized(other) || isHomomorphic(other)) {
            return null;
        } else if (other.getX1() == getX1() && other.getY1() == getY1()) {
            return getP1();
        } else if (other.getX1() == getX2() && other.getY1() == getY2()) {
            return getP2();
        } else if (other.getX2() == getX1() && other.getY2() == getY1()) {
            return getP1();
        } else if (other.getX2() == getX2() && other.getY2() == getY2()) {
            return getP2();
        }
        return GeometryUtils.intersection(x1, y1, x2, y2, other.getX1(),
                other.getY1(), other.getX2(), other.getY2());
    }

    /**
     * Get the mid point of a line.
     *
     * @return The mid point
     */
    public EqPointDouble midPoint() {
        double xm = x1 + ((x2 - x1) / 2);
        double ym = y1 + ((y2 - y1) / 2);
        return new EqPointDouble(xm, ym);
    }

    /**
     * Determine if this line contains the same points as another, in any order.
     *
     * @param other Another line
     * @return true if they are homomorphic
     */
    public boolean isHomomorphic(Line2D other) {
        EqLine o = of(other);
        return length() == o.length() && angleNormalized() == o.angleNormalized();
    }

    /**
     * Determine if this line contains the same points as another, in any order,
     * within the passed tolerance.
     *
     * @param other Another line
     * @return true if they are homomorphic
     */
    public boolean isHomomorphic(Line2D other, double tolerance) {
        EqLine o = of(other);
        double len1 = length();
        double ang1 = angleNormalized();
        double len2 = o.length();
        double ang2 = o.angleNormalized();
        return GeometryUtils.isSameCoordinate(len1, len2, tolerance)
                && GeometryUtils.isSameCoordinate(ang1, ang2);
    }

    @Override
    public boolean intersectsLine(Line2D l) {
        return GeometryUtils.linesIntersect(this, l);
    }

    @Override
    public boolean intersectsLine(double x1, double y1, double x2, double y2) {
        return GeometryUtils.linesIntersect(this, x1, y1, x2, y2);
    }

    /**
     * Determine if this and another line are equal to each other within the
     * passed tolerance.
     *
     * @param other Another line
     * @param tolerance The minimum difference between coordinates that should
     * be recognized as a difference
     * @return true if they are equal within the tolerance
     */
    public boolean equals(Line2D other, double tolerance) {
        if (equals(other)) {
            return true;
        }
        return GeometryUtils.isSamePoint(x1, y1, other.getX1(), other.getY1(), tolerance)
                && GeometryUtils.isSamePoint(x2, y2, other.getX2(), other.getY2(), tolerance);
    }

    /**
     * Determine the length of this line.
     *
     * @return A length
     */
    public double length() {
        return Point2D.distance(x1, y1, x2, y2);
    }

    @Override
    public String toString() {
        return GeometryStrings.lineToString(x1, y1, x2, y2);
    }

    public boolean equalsNormalized(Line2D other) {
        return (GeometryUtils.isSamePoint(x1, y1, other.getX1(), other.getY1())
                && GeometryUtils.isSamePoint(x2, y2, other.getX2(), other.getY2()))
                || (GeometryUtils.isSamePoint(x2, y2, other.getX1(), other.getY1())
                && GeometryUtils.isSamePoint(x1, y1, other.getX2(), other.getY2()));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof EqLine) {
            EqLine other = (EqLine) o;
            return GeometryUtils.isSamePoint(x1, y1, other.x1, other.y1)
                    && GeometryUtils.isSamePoint(x2, y2, other.x2, other.y2);
        } else if (o instanceof Line2D) {
            Line2D other = (Line2D) o;
            return GeometryUtils.isSamePoint(x1, y1, other.getX1(), other.getY1())
                    && GeometryUtils.isSamePoint(x2, y2, other.getX2(), other.getY2());
        }
        return false;
    }

    @Override
    public int hashCode() {
        // avoid -0.0
        long l1 = java.lang.Double.doubleToLongBits(x1 + 0.0);
        long l2 = java.lang.Double.doubleToLongBits(y1 + 0.0);
        long l3 = java.lang.Double.doubleToLongBits(x2 + 0.0);
        long l4 = java.lang.Double.doubleToLongBits(y2 + 0.0);
        int hash = 5;
        hash += l1 * 37;
        hash += l2 * 91;
        hash += l3 * 51;
        hash += l4 * 3;
        return hash;
    }

    @Override
    public void visitLines(DoubleQuadConsumer consumer, boolean includeClose) {
        consumer.accept(x1, y1, x2, y2);
    }
}
