package org.imagine.geometry;

import com.mastfrog.function.DoubleBiPredicate;
import java.awt.geom.Point2D;
import static java.lang.Math.min;
import org.imagine.geometry.util.GeometryUtils;

/**
 * A pair of lines connected by a shared point which form a vector which has an
 * angle.
 *
 * @author Tim Boudreau
 */
public interface LineVector extends AngleVector, Intersectable {

    public static LineVector of(Point2D a, Point2D apex, Point2D b) {
        return of(a.getX(), a.getY(), apex.getX(), apex.getY(), b.getX(), b.getY());
    }

    public static LineVector of(double ax, double ay, double sharedX, double sharedY, double bx, double by) {
        return new LineVectorImpl(ax, ay, sharedX, sharedY, bx, by);
    }

    default boolean intersectsFirst(double x1, double y1, double x2, double y2) {
        return GeometryUtils.linesIntersect(x1, y1, x2, y2, firstX(), firstY(), apexX(), apexY());
    }

    default boolean intersectsSecond(double x1, double y1, double x2, double y2) {
        return GeometryUtils.linesIntersect(x1, y1, x2, y2, apexX(), apexY(), secondX(), secondY(), false);
    }

    LineVector withAlternatePoints(double newAx, double newAy, double newBx, double newBy);

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
        return min(firstLineLength(), secondLineLength());
    }

    /**
     * Get the length of whichever line is longer.
     *
     * @return A length
     */
    default double maxLength() {
        return min(firstLineLength(), secondLineLength());
    }

    /**
     * Get a view of this angle as a sector of a circle.
     *
     * @return A sector
     */
    default Sector toSector() {
        double ang1 = Circle.angleOf(apexX(), apexY(), firstX(), firstY());
        double ang2 = Circle.angleOf(apexX(), apexY(), secondX(), secondY());
        double ext = ang2 - ang1;
        if (ext < 0) {
            System.out.println("A " + toString());
            return new SectorImpl(ang2, 360 + ext);
        } else {
            System.out.println("B " + toString());
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

    default EqLine firstLine() {
        return new EqLine(firstX(), firstY(), apexX(), apexY());
    }

    /**
     * Get the shared point as a Point2D.
     *
     * @return A point
     */
    default EqPointDouble location() {
        return new EqPointDouble(apexX(), apexY());
    }

    default EqLine secondLine() {
        return new EqLine(apexX(), apexY(), secondX(), secondY());
    }

    default EqPointDouble firstPoint() {
        return new EqPointDouble(firstX(), firstY());
    }

    default EqPointDouble secondPoint() {
        return new EqPointDouble(secondX(), secondY());
    }

    default EqLine throughLine() {
        return new EqLine(firstX(), firstY(), secondX(), secondY());
    }

    /**
     * Create a triangle from the three points represented by this vector.
     *
     * @return Triangle
     */
    default Triangle2D toTriangle() {
        return new Triangle2D(firstX(), firstY(), apexX(), apexY(), secondX(), secondY());
    }

    double firstX();

    double firstY();

    double secondX();

    double secondY();

    double apexX();

    double apexY();

}
