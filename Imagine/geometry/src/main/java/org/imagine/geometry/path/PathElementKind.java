package org.imagine.geometry.path;

import com.mastfrog.function.DoubleBiConsumer;
import com.mastfrog.function.DoubleBiFunction;
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
