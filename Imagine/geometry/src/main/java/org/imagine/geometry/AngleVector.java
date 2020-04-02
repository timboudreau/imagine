package org.imagine.geometry;

import com.mastfrog.function.DoubleBiConsumer;
import com.mastfrog.function.DoubleQuadConsumer;
import java.util.function.BiConsumer;

/**
 * Represents two angles, as in a CornerAngle, with lengths.
 *
 * @author Tim Boudreau
 */
public interface AngleVector {

    CornerAngle corner();

    double firstLineLength();

    double secondLineLength();

    default double firstLineAngle() {
        return corner().aDegrees();
    }

    default double secondLineAngle() {
        return corner().aDegrees();
    }

    default void firstPositionAt(double sharedX, double sharedY, DoubleBiConsumer c) {
        new Circle(sharedX, sharedY, firstLineLength()).positionOf(corner().aDegrees(), c);
    }

    default void secondPositionAt(double sharedX, double sharedY, DoubleBiConsumer c) {
        new Circle(sharedX, sharedY, secondLineLength()).positionOf(corner().bDegrees(), c);
    }

    default void linesAt(double x, double y, BiConsumer<? super EqLine, ? super EqLine> c) {
        positionsAt(x, y, (x1, y1, x2, y2) -> {
            c.accept(new EqLine(x, y, x1, y1), new EqLine(x, y, x2, y2));
        });
    }

    default void positionsAt(double x, double y, DoubleQuadConsumer c) {
        Circle circ = new Circle(x, y);
        CornerAngle corner = corner();
        circ.positionOf(corner.aDegrees(), firstLineLength(), (x1, y1) -> {
            circ.positionOf(corner.bDegrees(), secondLineLength(), (x2, y2) -> {
                c.accept(x1, y1, x2, y2);
            });
        });
    }

    default Triangle2D toTriangleAt(double x, double y) {
        Triangle2D result = new Triangle2D();
        positionsAt(x, y, (x1, y1, x2, y2) -> {
            result.setPoints(x, y, x1, y1, x2, y2);
        });
        return result;
    }
}
