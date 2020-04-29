package org.imagine.geometry;

import com.mastfrog.function.DoubleBiPredicate;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import static java.lang.Math.max;
import static java.lang.Math.min;
import org.imagine.geometry.util.GeometryUtils;
import org.imagine.geometry.util.PooledTransform;

/**
 * A pair of lines connected by a shared point which form a vector which has an
 * angle.
 *
 * @author Tim Boudreau
 */
public interface LineVector extends AngleVector, Intersectable {

    /**
     * Create a LineVector from three points.
     *
     * @param a The trailing point
     * @param apex The apex point
     * @param b The leading point
     * @return A vector
     */
    public static LineVector of(Point2D a, Point2D apex, Point2D b) {
        return of(a.getX(), a.getY(), apex.getX(), apex.getY(), b.getX(), b.getY());
    }

    /**
     * Create a LineVector from three points.
     *
     * @param ax The trailing x coordinate
     * @param ay The trailing y coordinate
     * @param sharedX The apex x coordinate
     * @param sharedY The apex y coordinate
     * @param bx The leading x coordinate
     * @param by The trailing y coordinate
     * @return A vector
     */
    public static LineVector of(double ax, double ay, double sharedX, double sharedY, double bx, double by) {
        return new LineVectorImpl(ax, ay, sharedX, sharedY, bx, by);
    }

    /**
     * Returns a circle in which any point within one hemisphere of it results
     * in an angle with the same extent as this LineVector's corner angle.
     *
     * @return A circle
     */
    Circle extentCircle();

    /**
     * Returns a circle where which any point within one hemisphere of results
     * in an angle with the passed extent.
     *
     * @return A circle
     */
    Circle extentCircle(double ext);

    /**
     * Determine if the passed line intersects the trailing line of this vector.
     *
     * @param x1 the line x1
     * @param y1 The line y1
     * @param x2 The line x2
     * @param y2 The line y2
     * @return True if they intersect
     */
    default boolean intersectsTrailingLine(double x1, double y1, double x2, double y2) {
        return GeometryUtils.linesIntersect(x1, y1, x2, y2, trailingX(), trailingY(), apexX(), apexY());
    }

    /**
     * Determine if the passed line intersects the leading line of this vector.
     *
     * @param x1 the line x1
     * @param y1 The line y1
     * @param x2 The line x2
     * @param y2 The line y2
     * @return True if they intersect
     */
    default boolean intersectsLeadingLine(double x1, double y1, double x2, double y2) {
        return GeometryUtils.linesIntersect(x1, y1, x2, y2, apexX(), apexY(), leadingX(), leadingY(), false);
    }

    /**
     * Determine if the passed line intersects the trailing line of this vector.
     *
     * @param line A line
     * @return True if they intersect
     */
    default boolean intersectsTrailingLine(Line2D line) {
        return GeometryUtils.linesIntersect(line.getX1(), line.getY1(),
                line.getX2(), line.getY2(), trailingX(), trailingY(), apexX(), apexY());
    }

    /**
     * Determine if the passed line intersects the leading line of this vector.
     *
     * @param line A line
     * @return True if they intersect
     */
    default boolean intersectsLeadingLine(Line2D line) {
        return GeometryUtils.linesIntersect(line.getX1(), line.getY1(),
                line.getX2(), line.getY2(), apexX(), apexY(), leadingX(),
                leadingY(), false);
    }

    /**
     * Create a copy of this vector translated such that the apex point is the
     * passed one.
     *
     * @param newApexX The new apex x coordinate
     * @param newApexY The new apex y coordinate
     * @return A new line vector with the same angles as this one, translated so
     * that the apex point is based on the passed value
     */
    default LineVector withApex(double newApexX, double newApexY) {
        double ax = apexX();
        double ay = apexY();
        if (ax == newApexX && ay == newApexY) {
            return this;
        }
        double xDiff = newApexX - ax;
        double yDiff = newApexY - ay;
        return transformedBy(AffineTransform.getTranslateInstance(xDiff, yDiff));
    }

    /**
     * Create a copy of this vector translated such that the trailing point is
     * the passed one.
     *
     * @param newApexX The new trailing x coordinate
     * @param newApexY The new trailing y coordinate
     * @return A new line vector with the same angles as this one, translated so
     * that the trailing point is based on the passed value
     */
    default LineVector withTrailing(double newTrailingX, double newTrailingY) {
        double tx = trailingX();
        double ty = trailingY();
        if (tx == newTrailingX && ty == newTrailingY) {
            return this;
        }
        double xDiff = tx - newTrailingX;
        double yDiff = ty - newTrailingY;
        return transformedBy(AffineTransform.getTranslateInstance(xDiff, yDiff));
    }

    /**
     * Create a copy of this vector translated such that the apex point is the
     * passed one.
     *
     * @param newApexX The new leading x coordinate
     * @param newApexY The new leading y coordinate
     * @return A new line vector with the same angles as this one, translated so
     * that the leading point is based on the passed value
     */
    default LineVector withLeading(double newLeadingX, double newLeadingY) {
        double ly = leadingY();
        double lx = leadingX();
        if (lx == newLeadingX && ly == newLeadingY) {
            return this;
        }
        double xDiff = lx - newLeadingX;
        double yDiff = ly - newLeadingY;
        return transformedBy(AffineTransform.getTranslateInstance(xDiff, yDiff));
    }

    /**
     * Create a new LineVector based on this one, processed by the passed
     * AffineTransform.
     *
     * @param xform A transform
     * @return A new line vector
     */
    default LineVector transformedBy(AffineTransform xform) {
        double[] pts = new double[]{trailingX(), trailingY(),
            apexX(), apexY(), leadingX(), leadingY()};
        xform.transform(pts, 0, pts, 0, 3);
        return new LineVectorImpl(pts[0], pts[1], pts[2], pts[3], pts[4],
                pts[5]);
    }

    /**
     * Create a new LineVector based on this one, with its points translated by
     * the passed offsets.
     *
     * @param dx The xOffset
     * @param dy The y offset
     * @return A new line vector
     */
    default LineVector translatedBy(double dx, double dy) {
        AffineTransform xform = PooledTransform.getTranslateInstance(dx, dy, null);
        try {
            return transformedBy(xform);
        } finally {
            PooledTransform.returnToPool(xform);
        }
    }

    /**
     * Create a new LineVector based on this one, with a different length for
     * the trailing line, but the same angles (modulo rounding errors).
     *
     * @param newLength The new length
     * @return A new line vector
     */
    default LineVector withTrailingLineLength(double newLength) {
        EqLine ln = trailingLine();
        ln.setLength(Math.abs(newLength), true);
        return new LineVectorImpl(ln.getX1(), ln.getY1(),
                apexX(), apexY(), leadingX(), leadingY());
    }

    /**
     * Create a new LineVector based on this one, with a different length for
     * the leading line, but the same angles (modulo rounding errors).
     *
     * @param newLength The new length
     * @return A new line vector
     */
    default LineVector withLeadingLineLength(double newLength) {
        EqLine ln = leadingLine();
        ln.setLength(Math.abs(newLength), false);
        return new LineVectorImpl(trailingX(), trailingY(),
                apexX(), apexY(), ln.getX2(), ln.getY2());
    }

    /**
     * Create a new LineVector based on this one, with a different length for
     * the leading line, but the same angles (modulo rounding errors).
     *
     * @param newLeading The new length
     * @return A new line vector
     */
    default LineVector withLineLengths(double newTrailing, double newLeading) {
        EqLine trail = trailingLine();
        trail.setLength(Math.abs(newTrailing), true);
        EqLine lead = leadingLine();
        lead.setLength(Math.abs(newLeading), false);
        return new LineVectorImpl(trail.getX1(), trail.getY1(),
                apexX(), apexY(), lead.getX2(), lead.getY2());
    }

    /**
     * Get the inverse of this LineVector, swapping the start and end points.
     *
     * @return An inverted line vector
     */
    LineVector inverse();

    /**
     * Get a line vector to a following point, using the shared point as the
     * first point of the return value, and the second point as the shared
     * point.
     *
     * @param nx An x coordinate
     * @param ny A y coordinate
     * @return Another line vector
     */
    LineVector next(double nx, double ny);

    /**
     * Get a line vector to a preceding point, using the start point as the
     * shared point of the return value, and the passed point as the start point
     * of it.
     *
     * @param px An x coordinate
     * @param py A y coordinate
     * @return Another line vector
     */
    LineVector previous(double px, double py);

    /**
     * Get a corner angle for this line vector, inverting it if the direction of
     * this vector's corner is the opposite of the passed one.
     *
     * @param dir The direction
     * @return An angle
     */
    CornerAngle corner(RotationDirection dir);

    /**
     * Get the corner angle for this line vector, inverted.
     *
     * @return A corner angle
     */
    CornerAngle inverseCorner();

    /**
     * Get the direction as returned by Line2D.relativeCCW() of the second line
     * with respect to the first line, as a RotationDirection.
     *
     * @return A direction
     */
    RotationDirection ccw();

    /**
     * Get the length of whichever line is shorter.
     *
     * @return A length
     */
    default double minLength() {
        return min(trailingLineLength(), leadingLineLength());
    }

    /**
     * Get the length of whichever line is longer.
     *
     * @return A length
     */
    default double maxLength() {
        return max(trailingLineLength(), leadingLineLength());
    }

    /**
     * Get a view of this angle as a sector of a circle.
     *
     * @return A sector
     */
    default Sector toSector() {
        double ang1 = Circle.angleOf(apexX(), apexY(), trailingX(), trailingY());
        double ang2 = Circle.angleOf(apexX(), apexY(), leadingX(), leadingY());
        double ext = ang2 - ang1;
        if (ext < 0) {
            return new SectorImpl(ang2, 360 + ext);
        } else {
            return new SectorImpl(ang2, ext);
        }
    }

    /**
     * Sample the coordinates of this vector's corner angle's quarter, mid and
     * three-quarters angles, at a default distance of 1.5, passing those
     * coordinates to the passed predicate, and returning the number of times
     * the passed predicate returned true, out of a maximum possible value of 3.
     *
     * @param test A test which takes x and y coordinates
     * @return The number of times the test passed
     */
    default int sample(DoubleBiPredicate test) {
        return corner().sample(apexX(), apexY(), test);
    }

    /**
     * Sample the coordinates of this vector's corner angle's quarter, mid and
     * three-quarters angles, at a default distance of 1.5, passing those
     * coordinates to the passed predicate, and returning the number of times
     * the passed predicate returned true, out of a maximum possible value of 3.
     *
     * @param atDistance The radius at which to take the coordinates of the
     * angles
     * @param test A test which takes x and y coordinates
     * @return The number of times the test passed
     */
    default int sample(double atDistance, DoubleBiPredicate test) {
        return corner().sample(apexX(), apexY(), atDistance, test);
    }

    /**
     * Get the trailing line - the line between the first and apex points passed
     * to the constructor.
     *
     * @return A line
     */
    default EqLine trailingLine() {
        return new EqLine(trailingX(), trailingY(), apexX(), apexY());
    }

    /**
     * Get the shared point as a Point2D.
     *
     * @return A point
     */
    default EqPointDouble apex() {
        return new EqPointDouble(apexX(), apexY());
    }

    /**
     * Get the leading line - the line between the apex and third point passed
     * to the constructor.
     *
     * @return A line
     */
    default EqLine leadingLine() {
        return new EqLine(apexX(), apexY(), leadingX(), leadingY());
    }

    /**
     * Get the first point passed to the constructor, forming the initial point
     * of the initial line.
     *
     * @return The trailing point
     */
    default EqPointDouble trailingPoint() {
        return new EqPointDouble(trailingX(), trailingY());
    }

    /**
     * Get the last point passed to the costructor, forming the final point of
     * this vector.
     *
     * @return A point
     */
    default EqPointDouble leadingPoint() {
        return new EqPointDouble(leadingX(), leadingY());
    }

    /**
     * Get the line that conects the leading and trailing points.
     *
     * @return A line
     */
    default EqLine throughLine() {
        return new EqLine(trailingX(), trailingY(), leadingX(), leadingY());
    }

    /**
     * Create a triangle from the three points represented by this vector.
     *
     * @return Triangle
     */
    default Triangle2D toTriangle() {
        return new Triangle2D(trailingX(), trailingY(), apexX(), apexY(), leadingX(), leadingY());
    }

    /**
     * Get the trailing x coordinate.
     *
     * @return A coordinate
     */
    double trailingX();

    /**
     * Get the trailing y coordinate.
     *
     * @return A coordinate
     */
    double trailingY();

    /**
     * Get the leading x coordinate.
     *
     * @return A coordinate
     */
    double leadingX();

    /**
     * Get the trailing y coordinate.
     *
     * @return A coordinate
     */
    double leadingY();

    /**
     * Get the apex x coordinate.
     *
     * @return A coordinate
     */
    double apexX();

    /**
     * Get the trailing y coordinate.
     *
     * @return A coordinate
     */
    double apexY();

    /**
     * Equality test with rounding-error tolerance.
     *
     * @param other Another vector
     * @param tolerance The tolerance
     * @return True if all coordinates match within the tolerance
     */
    boolean equals(LineVector other, double tolerance);
}
