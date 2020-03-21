package org.imagine.geometry;

import java.awt.geom.Line2D;
import static java.lang.Math.atan2;
import static java.lang.Math.ceil;
import static java.lang.Math.cos;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sin;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;
import java.util.function.Consumer;
import org.imagine.geometry.util.GeometryUtils;

/**
 * Safe type for a normalized angle in degrees, assuming 0\u00b0 is 12 o'clock.
 *
 * @author Tim Boudreau
 */
public final class Angle implements Comparable<Angle> {

    private final double degrees;
    public static final Angle ZERO = new Angle(0);

    Angle(double degrees) {
        assert degrees >= 0 && degrees < 360;
        this.degrees = degrees;
    }

    public boolean isAxial() {
        return degrees == 0 || degrees == 180 || degrees == 90 || degrees == 270;
    }

    public boolean isAxial(double tolerance) {
        return axis(tolerance) != null;
    }

    public Axis axis(double tolerance) {
        if (GeometryUtils.isSameCoordinate(0, degrees, tolerance) || GeometryUtils.isSameCoordinate(180, degrees, tolerance)) {
            return Axis.VERTICAL;
        } else if (GeometryUtils.isSameCoordinate(90, degrees, tolerance) || GeometryUtils.isSameCoordinate(270, degrees, tolerance)) {
            return Axis.HORIZONTAL;
        }
        return null;
    }

    public Axis axis() {
        if (degrees == 0 || degrees == 180) {
            return Axis.VERTICAL;
        } else if (degrees == 90 || degrees == 270) {
            return Axis.HORIZONTAL;
        }
        return null;
    }

    public Angle translatedTo(Quadrant quadrant) {
        Quadrant curr = quadrant();
        if (quadrant == curr) {
            return this;
        }
        return ofDegrees(quadrant.translate(curr, degrees));
    }

    public static boolean isSameHemisphere(double a, double b) {
        return Hemisphere.forAngle(a) == Hemisphere.forAngle(b);
    }

    public boolean isSameHemisphere(Angle other) {
        return isSameHemisphere(this.degrees, other.degrees);
    }

    public boolean isSameHemisphere(double other) {
        return isSameHemisphere(degrees, other);
    }

    public static Angle maxValue() {
        return new Angle(359D + 0.999999999999971);
    }

    public boolean equals(double degrees, double tolerance) {
        return GeometryUtils.isSameCoordinate(degrees, this.degrees, tolerance);
    }

    public boolean equals(Angle other, double tolerance) {
        return other == this ? true : equals(other.degrees, tolerance);
    }

    public static Angle ofDegrees(double degrees) {
        if (!Double.isFinite(degrees)) {
            return ZERO;
        }
        if (degrees == 0.0 || degrees == -0.0 || degrees == 360.0 || degrees == -360.0) {
            return ZERO;
        }
        return new Angle(normalize(degrees));
    }

    public static Angle zero() {
        return ZERO;
    }

    public static double asFraction(double degrees) {
        return normalize(degrees) / 360;
    }

    public static boolean isSameHemisphere(double a, double b, Axis axis) {
        Quadrant qa = Quadrant.forAngle(a);
        Quadrant qb = Quadrant.forAngle(b);
        if (qa == qb) {
            return true;
        }
        if (qa == qb.opposite()) {
            return false;
        }
        return qa.trailingAxis() == axis ? qb.leadingAxis() == axis
                : qb.trailingAxis() == axis;
    }

    public static double normalize(double degrees) {
        if (degrees == 0 || degrees == -0.0) {
            return 0;
        }
        if (degrees == 360 || degrees == -360) {
            degrees = 0;
        }
        if (degrees < -360) {
            degrees = 360 - (degrees % -360);
        }
        if (degrees < 0) {
            degrees = 360 + degrees;
        }
        if (degrees > 360) {
            degrees = degrees % 360;
        }
        return degrees;
    }

    public static double opposite(double angle) {
        if (!Double.isFinite(angle)) {
            return angle;
        }
        if (angle < 0) {
            angle = 360D + angle;
        }
        if (angle > 360) {
            angle = 360D % angle;
        }
        if (angle == 0D || angle == 360D) {
            return 180D;
        } else if (angle == 180D) {
            return 0;
        } else if (angle == 90D) {
            return 270D;
        }
        if (angle >= 360 || angle < 0) {
            angle = normalize(angle);
        }
        if (angle > 180D) {
            return angle - 180D;
        } else {
            return angle + 180D;
        }
    }

    public static Angle forLine(Line2D line) {
        return forLine(line.getX1(), line.getY1(), line.getX2(), line.getY2());
    }

    public static Angle forLine(double x1, double y1, double x2, double y2) {
        return Angle.ofDegrees(ofLine(x1, y1, x2, y2));
    }

    public static double ofLine(double x1, double y1, double x2, double y2) {
        double[] db = GeometryUtils.equidistantPoint(x1, y1, x2, y2);
        double result = Math.round(new Circle(db[0], db[1]).angleOf(x2, y2) - 180);
        if (result < 0) {
            result += 360;
        }
        return result;
    }

    public static void angles(int count, Consumer<Angle> c) {
        double curr = 0;
        double step = 360 / count;
        while (curr < 360) {
            c.accept(new Angle(curr));
            curr += step;
        }
    }

    public static void angles(double start, int count, Consumer<Angle> c) {
        double curr = normalize(start);
        double step = 360 / count;
        while (curr < 360) {
            c.accept(ofDegrees(curr));
            curr += step;
        }
    }

    public Angle ceiling() {
        return ofDegrees(ceil(degrees));
    }

    public Angle floor() {
        return ofDegrees(Math.floor(degrees));
    }

    public Angle round() {
        return ofDegrees((int) Math.round(degrees));
    }

    public Angle times(double multiplier) {
        return ofDegrees(degrees * multiplier);
    }

    public Angle dividedBy(double divideBy) {
        return ofDegrees(degrees / divideBy);
    }

    public Angle degreesTo(Angle other) {
        double diff = other.degrees - degrees;
        if (diff == 0) {
            return ZERO;
        }
        if (diff < 0) {
            diff = opposite(-diff);
        }
        return new Angle(diff);
    }

    public Angle degreesFrom(Angle other) {
        double diff = degrees - other.degrees;
        if (diff == 0) {
            return ZERO;
        }
        if (diff < 0) {
            diff = opposite(-diff);
        }
        return new Angle(diff);
    }

    public Angle gapWith(Angle ang, boolean leading) {
        double a = min(degrees, ang.degrees);
        double b = max(degrees, ang.degrees);
        if (a == b) {
            return ZERO;
        }
        double result = a + ((b - a) / 2D);
        return ofDegrees(result);
    }

    public static double angleBetween(double angleA, double angleB) {
        if (angleA == angleB) {
            return angleA;
        }
        angleA = normalize(angleA);
        angleB = normalize(angleB);
        double a = min(angleA, angleB);
        double b = max(angleA, angleB);
        return a + ((b - a) / 2D);
    }

    public static double addAngles(double angleA, double angleB) {
        return normalize(angleA + angleB);
    }

    public static double subtractAngles(double from, double subtract) {
        return normalize(from - subtract);
    }

    public static double averageAngle(double a, double b) {
        double ra = toRadians(a);
        double rb = toRadians(b);
        double x = cos(ra) + cos(rb);
        double y = sin(ra) + sin(rb);
        double result = toDegrees(atan2(y, x));
        return result == 0 ? 0 : (result + 360D) % 360D;
    }

    public static double averageAngle(double a, double b, double c) {
        double ra = toRadians(a);
        double rb = toRadians(b);
        double rc = toRadians(c);
        double x = cos(ra) + cos(rb) + cos(rc);
        double y = sin(ra) + sin(rb) + sin(rc);
        double result = toDegrees(atan2(y, x));
        return result == 0 ? 0 : (result + 360D) % 360D;
    }

    public static double average(double... angles) {
        double x = 0;
        double y = 0;
        for (int i = 0; i < angles.length; i++) {
            double rad = toRadians(angles[i]);
            x += cos(rad);
            y += sin(rad);
        }
        double result = toDegrees(atan2(x, y));
        return result == 0 ? 0 : (result + 360D) % 360D;
    }

    public static Angle ofRadians(double radians) {
        return ofDegrees(toRadians(radians));
    }

    public static double perpendicularClockwise(double angle) {
        if (!Double.isFinite(angle)) {
            return angle;
        }
        if (angle < 0) {
            angle = 360D + angle;
        }
        if (angle > 360) {
            angle = 360D % angle;
        }
        if (angle + 90D > 360) {
            return (angle + 90D) - (angle % 360);
        } else {
            return angle + 90D;
        }
    }

    public static double perpendicularCounterclockwise(double angle) {
        if (!Double.isFinite(angle)) {
            return angle;
        }
        if (angle < 0) {
            angle = 360D + angle;
        }
        if (angle > 360) {
            angle = 360D % angle;
        }
        if (angle - 90D < 0) {
            return 360D + (angle - 90D);
        } else {
            return angle - 90D;
        }
    }

    public Angle minus(double degrees) {
        return new Angle(normalize(this.degrees - degrees));
    }

    public Angle plus(double degrees) {
        return new Angle(normalize(this.degrees + degrees));
    }

    public Angle plus(Angle other) {
        return new Angle(normalize(degrees + other.degrees));
    }

    public double degrees() {
        return degrees;
    }

    public double radians() {
        return toRadians(degrees);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof Angle) {
            Angle a = (Angle) o;
            return degrees == a.degrees
                    || degrees + 0.0 == a.degrees + 0.0;
        }
        return false;
    }

    @Override
    public int hashCode() {
        long hash = Double.doubleToLongBits(degrees + 0.0);
        return (int) (hash ^ hash >> 32);
    }

    @Override
    public String toString() {
        return GeometryUtils.toString(degrees) + "\u00B0";
    }

    @Override
    public int compareTo(Angle o) {
        return Double.compare(degrees, o.degrees);
    }

    public Quadrant quadrant() {
        return Quadrant.forAngle(degrees);
    }
}
