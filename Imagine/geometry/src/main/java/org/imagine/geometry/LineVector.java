package org.imagine.geometry;

import com.mastfrog.function.DoubleBiPredicate;
import org.imagine.geometry.util.GeometryUtils;

/**
 *
 * @author Tim Boudreau
 */
public interface LineVector extends AngleVector, Intersectable {

    public static LineVector of(double ax, double ay, double sharedX, double sharedY, double bx, double by) {
        return new LineVectorImpl(ax, ay, sharedX, sharedY, bx, by);
    }

    default boolean intersectsFirst(double x1, double y1, double x2, double y2) {
        return GeometryUtils.linesIntersect(x1, y1, x2, y2, firstX(), firstY(), sharedX(), sharedY());
    }

    default boolean intersectsSecond(double x1, double y1, double x2, double y2) {
        return GeometryUtils.linesIntersect(x1, y1, x2, y2, sharedX(), sharedY(), secondX(), secondY(), false);
    }

    LineVector next(double nx, double ny);

    LineVector previous(double px, double py);

    CornerAngle corner(RotationDirection dir);

    CornerAngle inverseCorner();

    RotationDirection ccw();

    default Sector toSector() {
        double ang1 = Circle.angleOf(sharedX(), sharedY(), firstX(), firstY());
        double ang2 = Circle.angleOf(sharedX(), sharedY(), secondX(), secondY());
        double ext = ang2 - ang1;
        if (ext < 0) {
            System.out.println("A " + toString());
            return new SectorImpl(ang2, 360 + ext);
        } else {
            System.out.println("B " + toString());
            return new SectorImpl(ang2, ext);
        }
    }

    default int sample(DoubleBiPredicate test) {
        return toSector().sample(sharedX(), sharedY(), test);
    }

    default int sample(double atDistance, DoubleBiPredicate test) {
        return toSector().sample(sharedX(), sharedY(), atDistance, test);
    }

    default EqLine firstLine() {
        return new EqLine(firstX(), firstY(), sharedX(), sharedY());
    }

    default EqPointDouble location() {
        return new EqPointDouble(sharedX(), sharedY());
    }

    default EqLine secondLine() {
        return new EqLine(sharedX(), sharedY(), secondX(), secondY());
    }

    default EqPointDouble firstPoint() {
        return new EqPointDouble(firstX(), firstY());
    }

    default EqPointDouble secondPoint() {
        return new EqPointDouble(secondX(), secondY());
    }

    default Triangle2D toTriangle() {
        return new Triangle2D(firstX(), firstY(), sharedX(), sharedY(), secondX(), secondY());
    }

    double firstX();

    double firstY();

    double secondX();

    double secondY();

    double sharedX();

    double sharedY();

}
