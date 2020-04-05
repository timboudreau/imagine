package org.imagine.geometry.path;

import java.awt.geom.PathIterator;
import java.util.Arrays;
import org.imagine.geometry.util.GeometryStrings;
import org.imagine.geometry.util.GeometryUtils;

/**
 * A flyweight implementation of PathElement
 *
 * @author Tim Boudreau
 */
public final class FlyweightPathElement implements PathElement {

    private int type = -1;
    private double[] data = new double[6];

    FlyweightPathElement() {

    }

    FlyweightPathElement(int type, double[] data) {
        this.data = Arrays.copyOf(data, 6);
        this.type = type;
    }

    @Override
    public PathElementKind kind() {
        if (type < 0) {
            return null;
        }
        return PathElement.super.kind();
    }

    @Override
    public int type() {
        return type;
    }

    @Override
    public double[] points() {
        return data;
    }

    public boolean update(PathIterator iter) {
        if (iter.isDone()) {
            return false;
        }
        type = iter.currentSegment(data);
        return true;
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
        int type = type();
        if (this.type != other.type()) {
            return false;
        }
        return Arrays.equals(this.pointData(), other.pointData());
    }

}
