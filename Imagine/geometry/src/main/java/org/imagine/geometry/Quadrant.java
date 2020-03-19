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

/**
 * Quadrants of a 360 degree circular coordinate space.
 *
 * @author Tim Boudreau
 */
public enum Quadrant implements Sector {

    NORTHEAST,
    SOUTHEAST,
    SOUTHWEST,
    NORTHWEST;

    public Axis leadingAxis() {
        switch (this) {
            case NORTHWEST:
                return Axis.VERTICAL;
            case NORTHEAST:
                return Axis.HORIZONTAL;
            case SOUTHEAST:
                return Axis.VERTICAL;
            case SOUTHWEST:
                return Axis.HORIZONTAL;
            default:
                throw new AssertionError(this);
        }
    }

    @Override
    public Sector intersection(Sector other) {
        if (other == this) {
            return this;
        } else if (other instanceof Quadrant) {
            return null;
        } else if (other instanceof Circle && ((Circle) other).factor == 1) {
            return this;
        }
        if (other instanceof Hemisphere) {
            Hemisphere hem = (Hemisphere) other;
            if (hem.contains(this)) {
                return this;
            } else {
                return null;
            }
        }
        return Sector.super.intersection(other);
    }

    @Override
    public double midPoint() {
        return start() + 45;
    }

    @Override
    public double extent() {
        return 90;
    }

    @Override
    public boolean contains(double angle) {
        double s = start();
        boolean result = angle >= s && angle < s + 90;
//        System.out.println(this + " contains " + angle
//                + " >= " + start() + "? " + (angle >= start())
//                + " <= end " + end() + "? " + (angle < end()));
        return result;
    }

    public int subdivision(double angle, int subs) {
        double divisionSize = 90D / subs;
        double result = (angle - start()) / divisionSize;
        return (int) result;
    }

    public int quarter(double angle) {
        return subdivision(angle, 4);
    }

    public Axis trailingAxis() {
        return leadingAxis().opposite();
    }

    public double translate(Quadrant from, double ang) {
        ang = Angle.normalize(ang);
        double relative = ang - from.start();
        if (relative == 0) {
            return start();
        }
        return start() + relative;
    }

    public double center() {
        return start() + 45;
    }

    public Quadrant opposite() {
        switch (this) {
            case NORTHEAST:
                return SOUTHWEST;
            case NORTHWEST:
                return SOUTHEAST;
            case SOUTHEAST:
                return NORTHWEST;
            case SOUTHWEST:
                return NORTHEAST;
            default:
                throw new AssertionError();
        }
    }

    public static boolean isLeading(double angle) {
        return forAngle(angle).center() <= Angle.normalize(angle);
    }

    public Angle startingAngle() {
        return Angle.ofDegrees(start());
    }

    public Angle endingAngle() {
        return Angle.ofDegrees(end());
    }

    public double start() {
        switch (this) {
            case NORTHEAST:
                return 0;
            case SOUTHEAST:
                return 90;
            case SOUTHWEST:
                return 180;
            case NORTHWEST:
                return 270;
            default:
                throw new AssertionError(this);
        }
    }

    public double end() {
        return next().start();
    }

    public boolean isNorth() {
        switch (this) {
            case NORTHEAST:
            case NORTHWEST:
                return true;
            case SOUTHEAST:
            case SOUTHWEST:
                return false;
            default:
                throw new AssertionError(this);
        }
    }

    public boolean isSouth() {
        return !isNorth();
    }

    public boolean isEast() {
        switch (this) {
            case NORTHEAST:
            case SOUTHEAST:
                return true;
            case NORTHWEST:
            case SOUTHWEST:
                return false;
            default:
                throw new AssertionError(this);
        }
    }

    public boolean isWest() {
        return !isEast();
    }

    public Quadrant prev() {
        switch (this) {
            case NORTHEAST:
                return NORTHWEST;
            case SOUTHEAST:
                return NORTHEAST;
            case SOUTHWEST:
                return SOUTHEAST;
            case NORTHWEST:
                return SOUTHWEST;
            default:
                throw new AssertionError(this);
        }
    }

    public Quadrant next() {
        switch (this) {
            case NORTHEAST:
                return SOUTHEAST;
            case SOUTHEAST:
                return SOUTHWEST;
            case SOUTHWEST:
                return NORTHWEST;
            case NORTHWEST:
                return NORTHEAST;
            default:
                throw new AssertionError(this);
        }
    }

    public double xDirection() {
        return directionVector()[0];
    }

    public double yDirection() {
        return directionVector()[0];
    }

    public double[] direct(double xOff, double yOff) {
        double[] vect = directionVector();
        vect[0] *= Math.abs(xOff);
        vect[1] *= Math.abs(yOff);
        return vect;
    }

    public double[] directionVector() {
        switch (this) {
            case NORTHEAST:
                return new double[]{1, -1};
            case NORTHWEST:
                return new double[]{-1, -1};
            case SOUTHEAST:
                return new double[]{1, 1};
            case SOUTHWEST:
                return new double[]{-1, 1};
            default:
                throw new AssertionError(this);
        }
    }

    public int bias(double angle) {
        double c = center();
        return angle == c ? 0 : angle < c ? -1 : 1;
    }

    public static Quadrant forAngle(double ang) {
        if (!Double.isFinite(ang)) {
            throw new IllegalArgumentException("Not an angle: " + ang);
        }
        ang = Angle.normalize(ang);
        if ((ang >= 0 && ang < 90) || ang == 360) {
            return NORTHEAST;
        } else if (ang >= 90 && ang < 180) {
            return SOUTHEAST;
        } else if (ang >= 180 && ang < 270) {
            return SOUTHWEST;
        } else if (ang >= 270 && ang < 360) {
            return NORTHWEST;
        }
        throw new IllegalArgumentException("Not an angle: " + ang);
    }

    public boolean isAdjacentTo(Quadrant q) {
        switch (this) {
            case NORTHEAST:
                switch (q) {
                    case NORTHWEST:
                    case SOUTHEAST:
                        return true;
                    default:
                        return false;
                }
            case NORTHWEST:
                switch (q) {
                    case NORTHEAST:
                    case SOUTHWEST:
                        return true;
                    default:
                        return false;
                }
            case SOUTHEAST:
                switch (q) {
                    case SOUTHWEST:
                    case NORTHEAST:
                        return true;
                    default:
                        return false;
                }
            case SOUTHWEST:
                switch (q) {
                    case NORTHWEST:
                    case SOUTHEAST:
                        return true;
                    default:
                        return false;
                }
            default:
                throw new AssertionError(this);
        }
    }
}
