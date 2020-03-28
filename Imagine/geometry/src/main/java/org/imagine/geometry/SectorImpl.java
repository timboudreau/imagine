package org.imagine.geometry;

import org.imagine.geometry.util.GeometryUtils;

/**
 *
 * @author Tim Boudreau
 */
final class SectorImpl implements Sector {

    private final double startingAngle;
    private final double extent;

    SectorImpl(double startingAngle, double extent) {
        this.startingAngle = Angle.normalize(startingAngle);
        this.extent = Math.min(360, Math.abs(extent));
    }

    @Override
    public double start() {
        return startingAngle;
    }

    @Override
    public double extent() {
        return extent;
    }

    @Override
    public String toString() {
        return "Sector(" + extent + "\u00B0 from " + startingAngle
                + "\u00B0)";
    }

    public PieWedge toShape(double x, double y, double radius) {
        return new PieWedge(x, y, radius, start(), extent());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof Sector) {
            Sector s = (Sector) o;
            return GeometryUtils.isSameCoordinate(startingAngle, s.start())
                    && GeometryUtils.isSameCoordinate(extent, s.extent());
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (isEmpty()) {
            return 0;
        }
        long hash = 51 * Double.doubleToLongBits(startingAngle + 0.0);
        hash = 51 * hash
                + Double.doubleToLongBits(extent + 0.0);
        return (int) (hash ^ (hash >> 32));
    }
}
