package org.imagine.geometry.path;

import java.util.Arrays;
import org.imagine.geometry.util.GeometryStrings;
import org.imagine.geometry.util.GeometryUtils;

/**
 *
 * @author Tim Boudreau
 */
final class SimplePathElement implements PathElement {

    private final int type;
    private final double[] points;

    public SimplePathElement(int type, double[] points) {
        this.type = type;
        this.points = points;
    }

    public SimplePathElement(int type) {
        this(type, null);
    }

    @Override
    public int type() {
        return type;
    }

    @Override
    public double[] points() {
        return points == null ? new double[0] : points;
    }

    @Override
    public String toString() {
        String typeName = kind().toString();
        StringBuilder sb = new StringBuilder(typeName.length() + 5 * pointCount());
        int count = pointCount();
        for (int i = 0; i < count; i++) {
            int ix = i;
            point(i, (x, y) -> {
                GeometryStrings.toString(sb, x, y);
                if (ix != count - 1) {
                    sb.append(",");
                }
            });
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + this.type;
        hash = 59 * hash + Arrays.hashCode(this.pointData());
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
        final PathElement other = (PathElement) obj;
        if (this.type != other.type()) {
            return false;
        }
        if (!Arrays.equals(this.pointData(), other.pointData())) {
            return false;
        }
        return true;
    }
}
