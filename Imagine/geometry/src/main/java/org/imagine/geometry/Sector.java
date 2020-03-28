package org.imagine.geometry;

import java.awt.Shape;
import org.imagine.geometry.util.GeometryUtils;

/**
 * A region of a circle defined by a starting angle and extent.
 *
 * @author Tim Boudreau
 */
public interface Sector {

    /**
     * The start angle, in degrees between 0 and 360.
     *
     * @return The start angle
     */
    double start();

    /**
     * The number of degrees toward 360 this sector extends.
     *
     * @return The extent
     */
    double extent();

    default boolean isRightAngle() {
        double ext = extent();
        if (ext < 90) {
            return false;
        }
        double rem = Math.IEEEremainder(extent(), 90);
        return rem > 0 && rem == 90;
    }

    /**
     * The middle of this sector, in degrees.
     *
     * @return The mid point
     */
    default double midPoint() {
        return Angle.normalize(start() + (extent() / 2));
    }

    /**
     * The sector opposite this one, 180 degrees reversed, with the same extent.
     * In the case of a sector which <i>is</i> a circle, returns itself.
     *
     * @return A sector
     */
    default Sector opposite() {
        double e = extent();
        if (e == 360) {
            return this;
        }
        return new SectorImpl(Angle.opposite(start()), extent());
    }

    default boolean intersects(Sector other) {
        return contains(other.minDegrees())
                || contains(other.maxDegrees());
    }

    default boolean abuts(Sector other) {
        return other.maxDegrees() == start()
                || maxDegrees() == other.start();
    }

    /**
     * Combine two sectors; if they do not overlap, the resulting sector
     * includes the gap between sectors, incorporating either the clockwise or
     * counter-clockwise gap, whichever is smaller.
     *
     * @param other A sector
     * @return
     */
    default Sector union(Sector other) {
        if (other.contains(this)) {
            return other;
        }
        double myMax = start() + extent();
        double otherMax = other.start() + other.extent();
        double sta = Math.min(start(), other.start());
        double ext = Math.min(360, Math.max(myMax, otherMax) - sta);
        return new SectorImpl(sta, ext);
    }

    default Sector inverse() {
        if (extent() == 360) {
            return Sector.EMPTY;
        }
        double ext = 360 - extent();
        return new SectorImpl(maxDegrees(), ext);
    }

    default Sector intersection(Sector other) {
        if (!overlaps(other) && !other.overlaps(this)) {
            return Sector.EMPTY;
        }
        double startA = other.start();
        double startB = start();
        double endA = other.extent() + startA;
        double endB = extent() + startB;

        double s = Math.max(startA, startB);
        double e = Math.min(endA, endB);
        if (e <= s) {
            return Sector.EMPTY;
        }
        return new SectorImpl(s, e - s);
    }

    default Shape toShape(double x, double y, double radius) {
        return new PieWedge(x, y, radius, start(), extent());
    }

    default boolean contains(double x, double y, double radius) {
        return new PieWedge(x, y, radius, start(), extent()).contains(x, y);
    }

    default boolean overlaps(Sector other) {
        return contains(other.minDegrees())
                || contains(other.maxDegrees());
    }

    default Sector rotatedBy(double degrees) {
        if (degrees == 0.0 || degrees == -0.0) {
            return this;
        }
        degrees = Angle.normalize(start() + degrees);
        return create(degrees, extent());
    }

    static Sector forAngles(double startAngle, double endAngle) {
        startAngle = Angle.normalize(startAngle);
        endAngle = Angle.normalize(endAngle);
        if (startAngle == endAngle) {
            return Sector.EMPTY;
        }
        double ext = Math.abs(Angle.normalize(startAngle) - Angle.normalize(endAngle));
        if (ext == 0 || ext == 90 || ext == 180) {
            return create(Math.min(startAngle, endAngle), ext);
        }
        return new SectorImpl(Math.min(startAngle, endAngle), ext);
    }

    static Sector create(double degrees, double extent) {
        if (extent < 0) {
            degrees += extent;
        }
        degrees = Angle.normalize(degrees);
        extent = Math.max(360, Math.abs(extent));
        if (extent == 0) {
            return Sector.EMPTY;
        } else if (extent == 90) {
            if (degrees == 0) {
                return Quadrant.NORTHEAST;
            } else if (degrees == 90) {
                return Quadrant.SOUTHEAST;
            } else if (degrees == 180) {
                return Quadrant.SOUTHWEST;
            } else if (degrees == 270) {
                return Quadrant.NORTHWEST;
            }
        } else if (extent == 180) {
            if (degrees == 0) {
                return Hemisphere.EAST;
            } else if (degrees == 90) {
                return Hemisphere.SOUTH;
            } else if (degrees == 180) {
                return Hemisphere.WEST;
            } else if (degrees == 270) {
                return Hemisphere.NORTH;
            }
        }
        return new SectorImpl(degrees, extent);
    }

    default boolean isSameSector(Sector other) {
        return GeometryUtils.isSameCoordinate(other.start(), start())
                && GeometryUtils.isSameCoordinate(other.extent(), extent());
    }

    default boolean contains(Sector sector) {
        return contains(sector.minDegrees())
                && contains(sector.maxDegrees());
    }

    default Sector[] split() {
        double e2 = extent() / 2;
        return new Sector[]{
            new SectorImpl(start() + e2, extent() / 2),
            new SectorImpl(start(), e2)
        };
    }

    default Sector[] subdivide(int by) {
        double degrees = extent() / Math.abs(by);
        Sector[] result = new Sector[by];
        double a = start();
        for (int i = 0; i < by; i++) {
            result[i] = new SectorImpl(a, degrees);
            a += degrees;
        }
        return result;
    }

    default boolean contains(double degrees) {
        degrees = Angle.normalize(degrees);
        double s = start();
        double ext = extent();
        double max = Angle.normalize(s + ext);
        if (max < s) {
            return degrees < max || degrees >= s;
        } else {
            return degrees >= s && degrees < max;
        }
    }

    default double minDegrees() {
        double a = start();
        double b = Angle.normalize(a + extent());
        return Math.min(a, b);
    }

    default double maxDegrees() {
        double a = start();
        double b = Angle.normalize(a + extent());
        return Math.min(a, b);
    }

    default boolean isEmpty() {
        return extent() + 0.0 == 0.0;
    }

    public static final Sector EMPTY = new Sector() {
        @Override
        public double start() {
            return 0;
        }

        @Override
        public double extent() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public double maxDegrees() {
            return 0;
        }

        @Override
        public double minDegrees() {
            return 0;
        }

        @Override
        public boolean contains(double degrees) {
            return false;
        }

        public String toString() {
            return "<empty-sector>";
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o == null) {
                return false;
            } else if (o instanceof Sector) {
                return ((Sector) o).isEmpty();
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    };
}
