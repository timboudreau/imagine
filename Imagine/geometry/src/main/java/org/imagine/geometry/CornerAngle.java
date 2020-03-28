package org.imagine.geometry;

import java.awt.geom.Line2D;
import org.imagine.geometry.util.GeometryUtils;

/**
 * An angle between two vectors; unlike the generic Sector, which has a start
 * and an extent, the salient feature of a CornerAngle is that it represents two
 * angles, which may be clockwise or counter-clockwise from each other.
 *
 * @author Tim Boudreau
 */
public final class CornerAngle implements Sector, Comparable<CornerAngle> {

    private final double a;
    private final double b;

    public CornerAngle(double a, double b) {
        this.a = Angle.normalize(a);
        this.b = Angle.normalize(b);
    }

    public CornerAngle(double ax, double ay, double sx, double sy, double bx, double by) {
        this.a = Angle.ofLine(sx, sy, ax, ay);
        this.b = Angle.ofLine(sx, sy, bx, by);
    }

    public CornerAngle(Line2D a, Line2D b) {
        this.a = EqLine.of(a).angle();
        this.b = EqLine.of(b).angle();
    }

    public CornerAngle(Angle a, Angle b) {
        this(a.degrees(), b.degrees());
    }

    public double aDegrees() {
        return a;
    }

    public double bDegrees() {
        return b;
    }

    public Angle a() {
        return Angle.ofDegrees(a);
    }

    public Angle b() {
        return Angle.ofDegrees(b);
    }

    public RotationDirection direction() {
        if (a > b) {
            return RotationDirection.COUNTER_CLOCKWISE;
        } else if (a < b) {
            return RotationDirection.CLOCKWISE;
        } else {
            return RotationDirection.NONE;
        }
    }

    @Override
    public CornerAngle inverse() {
        return new CornerAngle(b, a);
    }

    @Override
    public double extent() {
        return Angle.angleBetween(a, b);
    }

    @Override
    public boolean isEmpty() {
        return a == b;
    }

    @Override
    public double start() {
        return aDegrees();
    }

    @Override
    public CornerAngle opposite() {
        if (extent() == 360) {
            return this;
        }
        return new CornerAngle(Angle.opposite(a), Angle.opposite(b));
    }

    @Override
    public CornerAngle rotatedBy(double degrees) {
        if (degrees == 0) {
            return this;
        }
        double aa = Angle.addAngles(a, degrees);
        double bb = Angle.addAngles(b, degrees);
        return new CornerAngle(aa, bb);
    }

    @Override
    public String toString() {
        return GeometryUtils.toString(a) + "\u00B0 - "
                + GeometryUtils.toString(b) + "\u00B0";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof CornerAngle) {
            CornerAngle ca = (CornerAngle) this;
            return GeometryUtils.isSameCoordinate(a, ca.a)
                    && GeometryUtils.isSameCoordinate(b, ca.b);
        } else if (o instanceof Sector) {
            Sector s = (Sector) o;
            if (GeometryUtils.isSameCoordinate(extent(), s.extent())) {
                switch (direction()) {
                    case NONE:
                        return s.isEmpty();
                    case CLOCKWISE:
                        return GeometryUtils.isSameCoordinate(a, s.start());
                    case COUNTER_CLOCKWISE:
                        return GeometryUtils.isSameCoordinate(b, s.start());
                    default:
                        throw new AssertionError(direction());
                }
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (isEmpty()) {
            return 0;
        }
        long hash = 51 * Double.doubleToLongBits(a + 0.0);
        hash = 51 * hash
                + Double.doubleToLongBits(extent() + 0.0);
        return (int) (hash ^ (hash >> 32));
    }

    @Override
    public int compareTo(CornerAngle o) {
        int result = Double.compare(a, o.a);
        if (result == 0) {
            result = Double.compare(b, o.b);
        }
        return result;
    }
}
