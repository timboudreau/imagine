package org.imagine.editor.api.snap;

import java.awt.geom.Point2D;
import java.util.Objects;

/**
 *
 * @author Tim Boudreau
 */
public class SnapPoint<T> implements Comparable<SnapPoint<?>> {

    final SnapAxis axis;
    final double coordinate;
    private final SnapKind kind;
    private final T obj;

    public SnapPoint(SnapAxis axis, double coordinate, SnapKind kind, T obj) {
        this.axis = axis;
        this.coordinate = coordinate;
        this.kind = kind;
        this.obj = obj;
    }

    public T value() {
        return obj;
    }

    public SnapKind kind() {
        return kind;
    }

    public double coordinate() {
        return coordinate;
    }

    public SnapAxis axis() {
        return axis;
    }

    public double distance(double x, double y) {
        double c = axis == SnapAxis.X ? x : y;
        return Math.abs(c - coordinate);
    }

    public double distance(Point2D p) {
        double c = axis.value(p);
        return Math.abs(c - coordinate);
    }

    @Override
    public String toString() {
        return axis + ":" + coordinate + "-" + kind + " (" + obj + ")";
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Objects.hashCode(this.axis);
        hash = 83 * hash + (int) (Double.doubleToLongBits(Math.floor(this.coordinate) + 0.5) ^ (Double.doubleToLongBits(this.coordinate) >>> 32));
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
        final SnapPoint other = (SnapPoint) obj;
        if (Math.abs(this.coordinate - other.coordinate) < 0.5) {
            return true;
        }
        return this.axis == other.axis;
    }

    @Override
    public int compareTo(SnapPoint<?> o) {
        int result = axis.compareTo(o.axis);
        if (result == 0) {
            result = Double.compare(coordinate, o.coordinate);
        }
        return result;
    }
}
