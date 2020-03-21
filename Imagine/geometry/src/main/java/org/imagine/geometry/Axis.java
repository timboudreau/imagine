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
package org.imagine.geometry;

import java.awt.Point;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.EnumSet;
import java.util.Set;

/**
 *
 * @author Tim Boudreau
 */
public enum Axis {

    HORIZONTAL,
    VERTICAL;

    public static Axis nearestForLine(Line2D line) {
        return _forAngle(EqLine.of(line).angle());
    }

    private static Axis _forAngle(double ang) {
        if (ang <= 45 || ang > 325 || (ang > 135 && ang <= 215)) {
            return VERTICAL;
        } else {
            return HORIZONTAL;
        }
    }

    public static Axis nearestForAngle(double angle) {
        return _forAngle(Angle.normalize(angle));
    }

    public double value(Point2D point) {
        return this == HORIZONTAL ? point.getX() : point.getY();
    }

    public int intValue(Point point) {
        return this == HORIZONTAL ? point.x : point.y;
    }

    public EqLine line(double otherCoord, double start, double end) {
        switch (this) {
            case HORIZONTAL:
                return new EqLine(start, otherCoord, end, otherCoord);
            case VERTICAL:
                return new EqLine(otherCoord, start, otherCoord, end);
            default:
                throw new AssertionError();
        }
    }

    public Axis of(double x, double y, double x2, double y2) {
        if (x == x2 && y == y2) {
            return null;
        }
        if (x == x2) {
            return HORIZONTAL;
        } else if (y == y2) {
            return VERTICAL;
        }
        return null;
    }

    public Set<Axis> common(Point a, Point b, int tolerance) {
        EnumSet<Axis> result = EnumSet.noneOf(Axis.class);
        int dx = Math.abs(a.x - b.x);
        int dy = Math.abs(a.y - b.y);
        if (dx == 0D) {
            result.add(HORIZONTAL);
        }
        if (dy == 0D) {
            result.add(VERTICAL);
        }
        return result;
    }

    public Set<Axis> common(Point a, Point b) {
        EnumSet<Axis> result = EnumSet.noneOf(Axis.class);
        if (a.x == b.x) {
            result.add(HORIZONTAL);
        }
        if (a.y == b.y) {
            result.add(VERTICAL);
        }
        return result;
    }

    public Set<Axis> common(Point2D a, Point2D b) {
        EnumSet<Axis> result = EnumSet.noneOf(Axis.class);
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        if (dx == 0D) {
            result.add(HORIZONTAL);
        }
        if (dy == 0D) {
            result.add(VERTICAL);
        }
        return result;
    }

    public Set<Axis> common(Point2D a, Point2D b, double tolerance) {
        EnumSet<Axis> result = EnumSet.noneOf(Axis.class);
        double dx = Math.abs(a.getX() - b.getX());
        double dy = Math.abs(a.getY() - b.getY());
        if (dx <= tolerance) {
            result.add(HORIZONTAL);
        }
        if (dy <= tolerance) {
            result.add(VERTICAL);
        }
        return result;
    }

    public void clamp(Point2D guide, Point2D target) {
        switch (this) {
            case HORIZONTAL:
                target.setLocation(guide.getX(), target.getY());
                break;
            case VERTICAL:
                target.setLocation(target.getX(), guide.getY());
                break;
            default:
                throw new AssertionError(this);
        }
    }

    public Axis opposite() {
        return this == HORIZONTAL ? VERTICAL : HORIZONTAL;
    }
}
