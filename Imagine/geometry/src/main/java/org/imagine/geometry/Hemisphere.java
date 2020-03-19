/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.geometry;

import java.util.EnumSet;
import java.util.Set;

/**
 *
 * @author Tim Boudreau
 */
public enum Hemisphere implements Sector {
    NORTH(Quadrant.NORTHWEST, Quadrant.NORTHEAST),
    EAST(Quadrant.NORTHEAST, Quadrant.SOUTHEAST),
    SOUTH(Quadrant.SOUTHEAST, Quadrant.SOUTHWEST),
    WEST(Quadrant.SOUTHWEST, Quadrant.NORTHWEST);

    private final Quadrant leadingQuadrant;
    private final Quadrant trailingQuadrant;

    private Hemisphere(Quadrant leadingQuadrant, Quadrant trailingQuadrant) {
        this.leadingQuadrant = leadingQuadrant;
        this.trailingQuadrant = trailingQuadrant;
        assert leadingQuadrant != trailingQuadrant;
        assert leadingQuadrant.isAdjacentTo(trailingQuadrant);
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

    public static Hemisphere forAngle(double ang) {
        for (Hemisphere h : values()) {
            if (h.contains(ang)) {
                return h;
            }
        }
        throw new AssertionError("Angle " + ang + " not in a hemisphere");
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
