package org.imagine.geometry;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * A semicirclular sector aligned on the 0-180 or 90-270 degree axis of a
 * circle.
 *
 * @author Tim Boudreau
 */
public enum Hemisphere implements Sector {
    /**
     * The hemisphere starting at 270\u00B0 and ending at 90\u00B0.
     */
    NORTH(Quadrant.NORTHWEST, Quadrant.NORTHEAST),
    /**
     * The hemisphere starting at 0\u00B0 and ending at 180\u00B0.
     */
    EAST(Quadrant.NORTHEAST, Quadrant.SOUTHEAST),
    /**
     * The hemisphere starting at 90\u00B0 and ending at 270\u00B0.
     */
    SOUTH(Quadrant.SOUTHEAST, Quadrant.SOUTHWEST),
    /**
     * The hemisphere starting at 180\u00B0 and ending at 360\u00B0 / 0\u00B0.
     */
    WEST(Quadrant.SOUTHWEST, Quadrant.NORTHWEST);

    private final Quadrant leadingQuadrant;
    private final Quadrant trailingQuadrant;

    private Hemisphere(Quadrant leadingQuadrant, Quadrant trailingQuadrant) {
        this.leadingQuadrant = leadingQuadrant;
        this.trailingQuadrant = trailingQuadrant;
        assert leadingQuadrant != trailingQuadrant;
        assert leadingQuadrant.isAdjacentTo(trailingQuadrant);
    }

    public static Hemisphere startingWith(Quadrant quadrant) {
        switch (quadrant) {
            case NORTHEAST:
                return EAST;
            case NORTHWEST:
                return NORTH;
            case SOUTHEAST:
                return SOUTH;
            case SOUTHWEST:
                return WEST;
            default:
                throw new AssertionError(quadrant);
        }
    }

    public Quadrant[] quadrants() {
        return new Quadrant[]{leadingQuadrant, trailingQuadrant};
    }

    public boolean contains(Quadrant q) {
        return q == leadingQuadrant || q == trailingQuadrant;
    }

    public static Hemisphere forAngle(double ang, Axis axis) {
        ang = Angle.normalize(ang);
        switch (axis) {
            case HORIZONTAL:
                if (NORTH.contains(ang)) {
                    return NORTH;
                } else if (SOUTH.contains(ang)) {
                    return SOUTH;
                } else {
                    throw new AssertionError("No hemisphere for " + axis + " contains " + ang);
                }
            case VERTICAL:
                if (EAST.contains(ang)) {
                    return EAST;
                } else if (WEST.contains(ang)) {
                    return WEST;
                } else {
                    throw new AssertionError("No hemisphere for " + axis + " contains " + ang);
                }
            default:
                throw new AssertionError("Unknown axis " + axis);
        }
    }

    public Axis axis() {
        switch (this) {
            case NORTH:
            case SOUTH:
                return Axis.HORIZONTAL;
            default:
                return Axis.VERTICAL;
        }
    }

    public Hemisphere preceding() {
        switch (this) {
            case NORTH:
                return WEST;
            case WEST:
                return SOUTH;
            case SOUTH:
                return EAST;
            case EAST:
                return NORTH;
            default:
                throw new AssertionError(this);
        }
    }

    public Hemisphere next() {
        switch (this) {
            case NORTH:
                return EAST;
            case EAST:
                return SOUTH;
            case SOUTH:
                return WEST;
            case WEST:
                return NORTH;
            default:
                throw new AssertionError(this);
        }
    }

    /**
     * Returns the hemisphere that best contains this angle - that which the
     * angle is nearest to the midpoint of.
     *
     * @param ang An angle
     * @return A hemisphere
     */
    public static Hemisphere forAngle(double ang) {
        double a = Angle.normalize(ang);
        List<Hemisphere> result = new LinkedList<>();
        for (Hemisphere h : values()) {
            if (h.contains(a)) {
                result.add(h);
            }
        }
        Collections.sort(result, (ha, hb) -> {
            double distA = Math.abs(a - ha.midAngle());
            double distB = Math.abs(a - hb.midAngle());
            return Double.compare(distA, distB);
        });
        return result.iterator().next();
    }

    @Override
    public double start() {
        return leadingQuadrant.start();
    }

    @Override
    public double extent() {
        return 180;
    }

    @Override
    public boolean contains(double degrees) {
        degrees = Angle.normalize(degrees);
        return leadingQuadrant.contains(degrees)
                || trailingQuadrant.contains(degrees);
    }

    public boolean isAdjacent(Hemisphere hemi) {
        return hemi != this && axis() == hemi.axis();
    }

    @Override
    public Sector intersection(Sector other) {
        if (other == this) {
            return this;
        } else if (other == leadingQuadrant || other == trailingQuadrant) {
            return other;
        } else if (other instanceof Circle && ((Circle) other).factor == 1) {
            return this;
        }
        return Sector.super.intersection(other);
    }

    public Quadrant leadingQuadrant() {
        return leadingQuadrant;
    }

    public Quadrant trailingQuadrant() {
        return trailingQuadrant;
    }

    public Set<Quadrant> overlapWith(Hemisphere hemi) {
        if (hemi == this) {
            return EnumSet.of(leadingQuadrant, trailingQuadrant);
        } else if (isAdjacent(hemi)) {
            return EnumSet.noneOf(Quadrant.class);
        } else {
            EnumSet<Quadrant> result = EnumSet.noneOf(Quadrant.class);
            if (leadingQuadrant == hemi.trailingQuadrant) {
                result.add(leadingQuadrant);
            } else {
                result.add(trailingQuadrant);
            }
            return result;
        }
    }

    @Override
    public String toString() {
        switch (this) {
            case EAST:
                return "E";
            case NORTH:
                return "N";
            case SOUTH:
                return "S";
            case WEST:
                return "W";
            default:
                throw new AssertionError(this);
        }
    }

    @Override
    public Hemisphere opposite() {
        switch (this) {
            case EAST:
                return WEST;
            case NORTH:
                return SOUTH;
            case SOUTH:
                return NORTH;
            case WEST:
                return EAST;
            default:
                throw new AssertionError(this);
        }
    }
}
