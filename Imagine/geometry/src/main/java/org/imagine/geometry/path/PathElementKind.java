package org.imagine.geometry.path;

import com.mastfrog.function.DoubleBiConsumer;
import com.mastfrog.function.DoubleBiFunction;
import java.awt.geom.Path2D;
import static java.awt.geom.PathIterator.SEG_CLOSE;
import static java.awt.geom.PathIterator.SEG_CUBICTO;
import static java.awt.geom.PathIterator.SEG_LINETO;
import static java.awt.geom.PathIterator.SEG_MOVETO;
import static java.awt.geom.PathIterator.SEG_QUADTO;
import org.imagine.geometry.util.GeometryUtils;

/**
 *
 * @author Tim Boudreau
 */
public enum PathElementKind {

    MOVE,
    LINE,
    QUADRATIC,
    CUBIC,
    CLOSE;

    public PointKind pointKindFor(int pointIndex) {
        PointKind[] kinds = pointKinds();
        if (pointIndex < 0 || pointIndex >= this.pointCount()) {
            throw new IllegalArgumentException(this + " has " + pointCount()
                    + " points, but point kind " + pointIndex + " requested");
        }
        return kinds[pointIndex];
    }

    public boolean isCurve() {
        return this == CUBIC || this == QUADRATIC;
    }

    public PointKind[] pointKinds() {
        switch (this) {
            case CLOSE:
                return new PointKind[0];
            case LINE:
                return new PointKind[]{PointKind.LINE_DESTINATION_POINT};
            case MOVE:
                return new PointKind[]{PointKind.MOVE_DESTINATION_POINT};
            case CUBIC:
                return new PointKind[]{PointKind.CUBIC_FIRST_CONTROL_POINT,
                    PointKind.CUBIC_SECOND_CONTROL_POINT, PointKind.CUBIC_DESTINATION_POINT};
            case QUADRATIC:
                return new PointKind[]{PointKind.QUADRATIC_FIRST_CONTROL_POINT, PointKind.QUADRATIC_DESTINATION_POINT};
            default:
                throw new AssertionError(this);
        }
    }

    public void apply(double[] pointData, Path2D path) {
        if (this != CLOSE) {
            assert pointData != null;
            assert pointData.length >= arraySize() : "Wrong array size " + pointData.length + " for " + this;
        }
        switch (this) {
            case CLOSE:
                path.closePath();
                break;
            case MOVE:
                path.moveTo(pointData[0], pointData[1]);
                break;
            case LINE:
                path.lineTo(pointData[0], pointData[1]);
                break;
            case QUADRATIC:
                path.quadTo(pointData[0], pointData[1], pointData[2], pointData[3]);
                break;
            case CUBIC:
                path.curveTo(pointData[0], pointData[1], pointData[2], pointData[3], pointData[4], pointData[5]);
                break;
            default:
                throw new AssertionError(this);
        }
    }

    public boolean destinationPoint(double[] pointData, DoubleBiConsumer consumer) {
        if (this == CLOSE) {
            return false;
        }
        int off = destinationPointArrayOffset();
        consumer.accept(pointData[off], pointData[off + 1]);
        return true;
    }

    public <T> T destinationPoint(DoubleBiFunction<T> consumer, double[] pointData) {
        if (this == CLOSE) {
            return null;
        }
        int off = destinationPointArrayOffset();
        return consumer.apply(pointData[off], pointData[off + 1]);
    }

    public int destinationPointArrayOffset() {
        switch (this) {
            case MOVE:
            case LINE:
                return 0;
            case QUADRATIC:
                return 2;
            case CUBIC:
                return 4;
            case CLOSE:
                return -1;
            default:
                throw new AssertionError(this);
        }
    }

    public boolean isSubpathEnd() {
        return this == CLOSE;
    }

    public boolean isSubpathStart() {
        return this == MOVE;
    }

    public boolean hasCoordinates() {
        return this != CLOSE;
    }

    public int arraySize() {
        return GeometryUtils.arraySizeForType(intValue());
    }

    public int pointCount() {
        switch (this) {
            case MOVE:
                return 1;
            case LINE:
                return 1;
            case QUADRATIC:
                return 2;
            case CUBIC:
                return 3;
            case CLOSE:
                return 0;
            default:
                throw new AssertionError(this);
        }
    }

    public static PathElementKind of(int type) {
        switch (type) {
            case SEG_MOVETO:
                return MOVE;
            case SEG_LINETO:
                return LINE;
            case SEG_CUBICTO:
                return CUBIC;
            case SEG_QUADTO:
                return QUADRATIC;
            case SEG_CLOSE:
                return CLOSE;
            default:
                throw new AssertionError("Invalid segment type " + type);
        }
    }

    public int intValue() {
        return ordinal();
    }
}
