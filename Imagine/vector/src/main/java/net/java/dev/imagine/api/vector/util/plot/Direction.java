/*
 * The MIT License
 *
 * Copyright 2020 Mastfrog Technologies.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.java.dev.imagine.api.vector.util.plot;

import com.mastfrog.function.IntBiConsumer;
import com.mastfrog.function.IntBiPredicate;
import java.awt.geom.Rectangle2D;
import java.util.Set;
import java.util.function.BiConsumer;
import org.imagine.geometry.Quadrant;

/**
 * Pixel-relative directions.
 *
 * @author Tim Boudreau
 */
public enum Direction {
    NONE(0, 0), NORTH(0, -1), SOUTH(0, 1), EAST(1, 0), WEST(-1, 0),
    NORTHEAST(1, -1), SOUTHEAST(1, 1), SOUTHWEST(-1, 1), NORTHWEST(-1, -1);
    private final int xoff;
    private final int yoff;

    Direction(int xoff, int yoff) {
        this.xoff = xoff;
        this.yoff = yoff;
    }

    public static Direction ofAngle(double angle) {
        if (angle == 0 || angle == 360) {
            return SOUTH;
        } else if (angle == 180) {
            return NORTH;
        } else if (angle == 90 || angle == -180) {
            return EAST;
        } else if (angle == 270 || angle == -90) {
            return WEST;
        } else {
            Quadrant quad = Quadrant.forAngle(angle);
            switch (quad) {
                case NORTHEAST:
                    return NORTHEAST;
                case NORTHWEST:
                    return NORTHWEST;
                case SOUTHEAST:
                    return SOUTHEAST;
                case SOUTHWEST:
                    return SOUTHWEST;
                default:
                    throw new AssertionError(quad);
            }
        }
    }

    public int navigate(int x, int y, IntBiPredicate pred) {
        int count = 0;
        while (pred.test(x, y)) {
            x += xoff;
            y += yoff;
            count++;
        }
        return count;
    }

    public static Direction[] outwardDirections() {
        return new Direction[]{NORTH, SOUTH, EAST, WEST, NORTHEAST, SOUTHEAST, NORTHWEST, SOUTHWEST};
    }

    public String toString() {
        switch (this) {
            case NONE:
                return "(none)";
            case NORTH:
                return "N";
            case SOUTH:
                return "S";
            case EAST:
                return "E";
            case WEST:
                return "W";
            case NORTHEAST:
                return "NE";
            case SOUTHEAST:
                return "SE";
            case SOUTHWEST:
                return "SW";
            case NORTHWEST:
                return "NW";
            default:
                throw new AssertionError();
        }
    }

    public int xOffset() {
        return xoff;
    }

    public int yOffset() {
        return yoff;
    }

    public static int neighborPercentages(double dblX, double dblY, BiConsumer<Direction, Double> c) {
        double absX = Math.floor(dblX);
        double absY = Math.floor(dblY);
        if (absX == dblX && absY == dblY) {
            return 0;
        }
        int count = 0;
        for (Direction d : new Direction[]{SOUTH, SOUTHEAST, EAST, NONE}) {
            double pct = d.percentage(dblX, dblY);
            if (pct != 0) {
                c.accept(d, pct);
                count++;
            }
        }
        return count;
    }

    public static int centeredNeighborPercentages(double dblX, double dblY, BiConsumer<Direction, Double> c) {
        double absX = Math.floor(dblX);
        double absY = Math.floor(dblY);
        if (absX == dblX && absY == dblY) {
            return 0;
        }
        int count = 0;
        for (Direction d : Direction.values()) {
            double pct = d.percentage(dblX, dblY);
            if (pct != 0) {
                c.accept(d, pct);
                count++;
            }
        }
        return count;
    }

    public double centeredPercentage(double dblX, double dblY) {
        double xFloor = Math.floor(dblX);
        double yFloor = Math.floor(dblY);
        double xCenter = xFloor + 0.5D;
        double yCenter = yFloor + 0.5D;

        if (xCenter == dblX && yCenter == dblY) {
            return this == NONE ? 1 : 0;
        }

        double xOff = dblX - xCenter;
        double yOff = dblY - yCenter;

        Rectangle2D.Double r = new Rectangle2D.Double(xFloor, yFloor, 1, 1);
        Rectangle2D.Double test = new Rectangle2D.Double(r.x, r.y, r.width, r.height);
        test.x += xOff * 2D;
        test.y += yOff * 2D;
        r.x += xOffset();
        r.y += xOffset();
        Rectangle2D isect = r.createIntersection(test);
        return isect.getWidth() * isect.getHeight();
    }

    /**
     * Given two floating-point coordinates which may not be exactly centered on
     * a cell, give the percentage this neighbor should contribute for a value
     * for that cell. Will always return 0 for NORTH, NORTHEAST, WEST and
     * SOUTHWEST; NORTHWEST is used for the cell in question itself.
     *
     * @param dblX An X coordinate
     * @param dblY A Y coordinate
     * @return A factor to multiply the contribution of this neighbor by when
     * computing a value for a cell not perfectly positioned on an exact
     * coordinate (as happens in line calculations)
     */
    public double percentage(double dblX, double dblY) {
        switch (this) {
            case NORTH:
            case NORTHEAST:
            case WEST:
            case SOUTHWEST:
            case NORTHWEST:
                return 0;
            case SOUTH:
            case EAST:
            case SOUTHEAST:
            case NONE:
                double absX = Math.floor(dblX);
                double absY = Math.floor(dblY);
                Rectangle2D.Double coordCell = new Rectangle2D.Double(dblX, dblY, 1, 1);
                Rectangle2D target;
                switch (this) {
                    case NONE:
                        target = coordCell.createIntersection(new Rectangle2D.Double(absX, absY, 1, 1));
                        break;
                    case SOUTH:
                        target = coordCell.createIntersection(new Rectangle2D.Double(absX, absY + 1, 1, 1));
                        break;
                    case SOUTHEAST:
                        target = coordCell.createIntersection(new Rectangle2D.Double(absX + 1, absY + 1, 1, 1));
                        break;
                    case EAST:
                        target = coordCell.createIntersection(new Rectangle2D.Double(absX + 1, absY, 1, 1));
                        break;
                    default:
                        throw new AssertionError(this);
                }
                return target.getWidth() * target.getHeight();
            default:
                throw new AssertionError(this);
        }
    }

    public void visit(int x, int y, IntBiConsumer c) {
        c.accept(x + xoff, y + yoff);
    }

    public static void visitAll(int x, int y, IntBiConsumer c) {
        visitDirections(x, y, c, values());
    }

    public static void visitDirections(int x, int y, IntBiConsumer bi, Set<Direction> directions) {
        for (Direction d : directions) {
            d.visit(x, y, bi);
        }
    }

    public static void visitDirections(int x, int y, IntBiConsumer bi, Direction... directions) {
        for (Direction d : directions) {
            d.visit(x, y, bi);
        }
    }
}
