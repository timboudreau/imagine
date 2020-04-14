package org.imagine.geometry;

import com.mastfrog.function.DoubleBiPredicate;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import org.imagine.geometry.util.DoubleList;
import org.imagine.geometry.util.GeometryStrings;
import org.imagine.geometry.util.GeometryUtils;
import com.mastfrog.function.state.Int;

/**
 * An angle between two vectors; unlike the generic Sector, which has a start
 * and an extent, the salient feature of a CornerAngle is that it represents two
 * angles, which may be clockwise or counter-clockwise from each other.
 * <p>
 * Unlike other Sector implementations, the angles of CornerAngle have an order,
 * and the extent may be negative, so CornerAngles have a direction. Angle pairs
 * which span zero (i.e. the second angle is less than the first) are only
 * expressible as negative extents with a CornerAngle (the price of preserving
 * the order of angles); use toSector() to get an equivalent with positive
 * extents when needed.
 * </p>
 * <p>
 * <b>Constraints:</b>
 * <ul>
 * <li>A CornerAngle shall never return an angle &gt;=360 or &lt;0</li>
 * <li>Angles outside the range 0-360 will be normalized on input
 * <ul>
 * <li>If <code>(unnormalizedAngleA > unnormalizedAngleB) !=
 *      (normalizedAngleA > normalizedAngleB)</code>, then the normalized angles are
 * applied in reverse order (e.g. if you pass in <code>350, 365</code>, that is
 * the same as passing <code>5, 350</code>, and if you pass in
 * <code>365, 350</code>, that is the same as passing <code>350, 5</code>)
 * </ul>
 * </li>
 * <li>Extents may be negative (for traversing zero, i.e. to span from 15\u00B0
 * degrees around to 5\u00B0, you would call the constructor with
 * <code>15,5</code>),
 * <i>but extents shall always be <code>&lt; 360 &gt; -360</code>
 * </li>
 * <li>The constructor that takes a 3-point vector applies the angles of the
 * lines in reverse order (it's intuitive once you grok it - the initial line
 * establishes the angle <i>toward</i> the shared point, and the second point
 * based on the shared line establishes the angle <i>away from</i>
 * that same point)</li>
 * </ul>
 *
 * @author Tim Boudreau
 */
public final strictfp class CornerAngle implements Sector, Comparable<CornerAngle> {

    /**
     * The multiplier applied to the trailing angle when encoding as a single
     * double, for use when determining the range to binary search in a
     * collection of angles to match.
     */
    public static final int ENCODING_MULTIPLIER = 10000000;
    private final double a;
    private final double b;

    /**
     * Create a new corner angle.
     *
     * @param trailing The trailing angle
     * @param leading The leading angle
     */
    public CornerAngle(double trailing, double leading) {
        double aa = Angle.normalize(trailing);
        double bb = Angle.normalize(leading);
        // passed an angle > 360 or < 0
        if ((trailing > leading) != (aa > bb)) {
            this.b = aa;
            this.a = bb;
        } else {
            this.a = aa;
            this.b = bb;
        }
    }

    public double distance(CornerAngle other) {
        if (other == this || (other.a == a && other.b == b)) {
            return 0;
        }
        double aDist = Math.abs(a - other.a);
        double bDist = Math.abs(b - other.b);
        return (aDist + bDist) / 2;
    }

    /**
     * Create a new CornerAngle from lines connected by a shared apex point.
     *
     * @param start The first line's start point
     * @param apex The shared apex point
     * @param end The second line's end point
     */
    public CornerAngle(Point2D start, Point2D apex, Point2D end) {
        this(start.getX(), start.getY(), apex.getX(), apex.getY(), end.getX(), end.getY());
    }

    /**
     * Create a new CornerAngle from two extent points and a central point.
     *
     * @param ax The first line's start x coordinate
     * @param ay The first line's start y coordinate
     * @param sx The first line's end x coordinate and the second line's start x
     * coordinate
     * @param sy The first line's end y coordinate and the second line's start y
     * coordinate
     * @param bx The second line's end x coordinate
     * @param by The second line's end y coordinate
     */
    public CornerAngle(double ax, double ay, double sx, double sy, double bx, double by) {
        // okay, say we have a point array
        // 0, 0 /  10, 0  / 10, 10 - the start of tracing a rectangle
        // clockwise from the upper left, - across 10 and down 10
        //
        // What we have for an angle can be phrased as
        // "An angle with 90 degrees extent starting at 180 degrees", or
        // "An angle from 90 degrees to 270 degrees.
        //
        // So, get our angles relative to the shared point
        this.b = Circle.angleOf(sx, sy, ax, ay);
        this.a = Circle.angleOf(sx, sy, bx, by);
    }

    /**
     * Create a corner angle from two lines, based on their angles, whether or
     * not the intersection point is within the bounds of either line segment.
     * If the lines are parallel, will result in an empty CornerAngle.
     *
     * @param a The first line
     * @param b The second line
     */
    public CornerAngle(Line2D a, Line2D b) {
        this.a = EqLine.of(a).angle();
        this.b = EqLine.of(b).angle();
    }

    /**
     * Create a corner angle from two angle objects.
     *
     * @param a The trailing angle
     * @param b The leading angle
     */
    public CornerAngle(Angle a, Angle b) {
        this(a.degrees(), b.degrees());
    }

    /**
     * Create a LineVector for the two passed points whose angles will (modulo
     * rounding errors) match this one's, computing the apex based on the angles
     * of this corner.
     *
     * @param trailing The trailing point
     * @param leading The leading point
     * @return A line vector
     */
    public LineVector toLineVector(EqPointDouble trailing, EqPointDouble leading) {
        if (trailing.equals(leading)) {
            return LineVector.of(trailing, trailing, trailing);
        }
        EqLine firstLine = EqLine.forAngleAndLength(trailing.x, trailing.y,
                leadingAngle(), 100000);
        EqLine secondLine = EqLine.forAngleAndLength(leading.x, leading.y,
                trailingAngle(), 100000);
        secondLine.swap();

        EqPointDouble apex = firstLine.intersectionPoint(secondLine);
        if (apex == null) {
            EqLine ln = new EqLine(trailing, leading);
            return LineVector.of(trailing, ln.midPoint(), leading);
        }
        return LineVector.of(firstLine.getP1(), apex, secondLine.getP2());
    }

    /**
     * Get a string representation of this angle with values rounded to two
     * decimal places.
     *
     * @return A string
     */
    @Override
    public String toShortString() {
        return GeometryStrings.toDegreesStringShort(a) + "/"
                + GeometryStrings.toDegreesStringShort(b)
                + " (" + GeometryStrings.toDegreesStringShort(b - a) + ")";
    }

    /**
     * Determine if this CornerAngle is the same as the passed one with the
     * leading and trailing angles reversed.
     *
     * @param other
     * @return
     */
    public boolean isHomomorphic(CornerAngle other) {
        return (other.a == a && other.b == b)
                || (other.a == b && other.b == a);
    }

    /**
     * Get the first angle passed to the constructor, which may be greater than
     * the first.
     *
     * @return The first angle in degrees
     */
    public double trailingAngle() {
        return a;
    }

    /**
     * Get the second angle passed to the constructor, which may be less than
     * the first.
     *
     * @return The second angle in degrees
     */
    public double leadingAngle() {
        return b;
    }

    public double throughAngle() {
        return 180 - a - b;
    }

    /**
     * Get the first angle.
     *
     * @return The first angle
     */
    public Angle leading() {
        return Angle.ofDegrees(a);
    }

    /**
     * Get the second angle.
     *
     * @return The second angle
     */
    public Angle trailing() {
        return Angle.ofDegrees(b);
    }

    /**
     * Get the direction of rotation of this CornerAngle - if the leading angle
     * is greater than the trailing angle, it will be CLOCKWISE; if the angles
     * are the same, it will be NONE.
     *
     * @return A direction
     */
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
    public double start() {
        return a;
    }

    /**
     * The extent of this CornerAngle; unlike other implementations of Sector,
     * with a CornerAngle, which has a direction, the extent may be negative
     * (indicating that the actual extent as a positive number of degrees is 360
     * + extent() starting at the <i>leading</i>
     * rather than the trailing angle).
     *
     * @return An extent between -360 and 360
     */
    @Override
    public double extent() {
        if (b == a) {
            return 0;
        }
        if (Math.abs(b - a) == 360) {
            return 0;
        }
        return b - a;
    }

    /**
     * Convert this CornerAngle to a Sector with a positive extent and whichever
     * of the angles is appropriate for the start angle.
     *
     * @return A sector
     */
    public Sector toSector() {
        if (b < a) {
            return new SectorImpl(a, 360 - (a - b));
        }
        return new SectorImpl(a, extent());
    }

    /**
     * Overridden to return this.
     *
     * @return this
     */
    @Override
    public CornerAngle toCornerAngle() {
        // already is one
        return this;
    }

    /**
     * Will return true if the extent is (effectively) zero degrees.
     *
     * @return True if the extent is 0
     */
    @Override
    public boolean isEmpty() {
        return a == b || Math.abs(extent() % 360) == 360;
    }

    /**
     * Create leading normalized version of this CornerAngle which is consistent
     * with Sector, where the least angle is the leading angle and the extent is
     * positive. This can have the effect of returning the inverse of this
     * angle, if its trailing angle is greater than its leading one.
     *
     * @return This angle or its inverse
     */
    @Override
    public CornerAngle normalized() {
        if (direction() == RotationDirection.COUNTER_CLOCKWISE) {
            return inverse();
        }
        return this;
    }

    /**
     * Get the physical extent, in degrees, of this angle - if the extent is
     * negative, adds 360 to it.
     *
     * @return The physical number of degrees out of 360 that this angle takes
     * up
     */
    public double physicalExtent() {
        double result = extent();
        if (result < 0) {
            result = 360 + result;
        }
        return result;
    }

    /**
     * The sector opposite this one, 180 degrees rotated, with the same physical
     * extent. In the case of a sector which <i>is</i> leading circle, returns
     * itself.
     *
     * @return A sector
     */
    @Override
    public CornerAngle opposite() {
        double ext = extent();
        if (ext == 0) {
            return this;
        }
        double oppA = Angle.opposite(a);
        double oppB = Angle.opposite(b);
        if (ext > 0) {
            // the angle straddles 180, such as 175-185 would
            // result in 355-5 spanning 350 degrees the wrong direction
            if (a < 180 && b > 180) {
                return new CornerAngle(oppA, oppB);
            }
            return new CornerAngle(oppB, oppA);
        } else {
            // the angle straddles 180 reversed, such as 185-175
            // would result in 355-5, going from spanning 10 degrees
            // to spanning 350
            if (a > 180 && b < 180) {
                return new CornerAngle(oppA, oppB);
            }
            return new CornerAngle(oppB, oppA);
        }
    }

    private Shape buildShape(double ax, double ay, double from, double to, Circle circ) {
        double ext = to < from ? 360 - (from - to) : to - from;
        int subdivisions = 12;
        double interval = ext / subdivisions;
        DoubleList dl = new DoubleList();
        dl.add(ax);
        dl.add(ay);
        for (int i = 0; i < subdivisions + 1; i++) {
            double ang = Angle.normalize(from + (interval * i));
            circ.positionOf(ang, (bx, by) -> {
                dl.add(bx);
                dl.add(by);
            });
        }
        return new Polygon2D(dl.toDoubleArray());
    }

    /**
     * Get a shape for use in tests of point-sampling, which includes all
     * coordinates that should be reachable from this corner angle out to some
     * radius from a base point.
     *
     * @param ax The base point x
     * @param ay The base point y
     * @param radius The radius
     * @return A shape
     */
    Shape boundingShape(double ax, double ay, double radius) {
        Circle circ = new Circle(ax, ay, radius);
        if (isNormalized()) {
            return buildShape(ax, ay, trailingAngle(), leadingAngle(), circ);
        } else {
            return buildShape(ax, ay, trailingAngle(), leadingAngle(), circ);
        }
    }

    /**
     * Sample the quarter angle, mid angle and three-quarters angle of this
     * CornerAngle, passing the coordinates of those angles at the passed
     * distance relative to the passed location, and return the number of points
     * which passed the passed test.
     *
     * @param sharedX The x coordinate
     * @param sharedY The y coordinate
     * @param atDistance The radius
     * @param test A test which takes a pair of x/y coordinates
     * @return The number of angles which passed the test - a return value of 3
     * means all tests passed, 0 means none did. Returns a number from zero
     * through 3.
     */
    public int sample(double sharedX, double sharedY, double atDistance, DoubleBiPredicate test) {
        Int res = Int.create();
        assert contains(midAngle()) : "Uh oh " + midAngle() + " not in " + this;
        assert contains(threeQuarterAngle()) : "Uh oh " + threeQuarterAngle() + " not in " + this;
        assert contains(quarterAngle()) : "Uh oh " + quarterAngle() + " not in " + this;
        Circle.positionOf(quarterAngle(), sharedX, sharedY, atDistance, (x1, y1) -> {
            if (test.test(x1, y1)) {
                res.increment();
            }
            Circle.positionOf(midAngle(), sharedX, sharedY, atDistance, (x2, y2) -> {
                if (test.test(x2, y2)) {
                    res.increment();
                }
                Circle.positionOf(threeQuarterAngle(), sharedX, sharedY, atDistance, (x3, y3) -> {
                    if (test.test(x3, y3)) {
                        res.increment();
                    }
                });
            });
        });
        return res.getAsInt();
    }

    /**
     * Returns leading sector comprising the degrees of a circle
     * <i>not</i> contained within this one.
     *
     * @return
     */
    @Override
    public CornerAngle inverse() {
        return new CornerAngle(b, a);
    }

    /**
     * Create a paintable shape (currently PieWedge) which can visualize this
     * CornerAngle.
     *
     * @param x The apex x coordinate
     * @param y The apex y coordinate
     * @param r The radius
     * @return A shape
     */
    @Override
    public Shape toShape(double x, double y, double r) {
        return new PieWedge(x, y, r, a, extent());
    }

    /**
     * Create a new corner angle rotated by the passed number of degrees.
     *
     * @param degrees A number of degrees
     * @return A corner angle
     */
    @Override
    public CornerAngle rotatedBy(double degrees) {
        if (degrees == 0) {
            return this;
        }
        double aa = Angle.addAngles(a, degrees);
        double bb = Angle.addAngles(b, degrees);
        if ((aa < bb) != (a < b)) {
            return new CornerAngle(bb, aa);
        }
        return new CornerAngle(aa, bb);
    }

    @Override
    public String toString() {
        return GeometryStrings.toDegreesString(a) + " / "
                + GeometryStrings.toDegreesString(b)
                + " (" + GeometryStrings.toString(extent()) + "\u00B0)";
    }

    public boolean equals(CornerAngle other, double tolerance) {
        return GeometryUtils.isSameCoordinate(leadingAngle(), other.leadingAngle(), tolerance)
                && GeometryUtils.isSameCoordinate(trailingAngle(), other.trailingAngle(), tolerance);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof CornerAngle) {
            CornerAngle ca = (CornerAngle) o;
            return GeometryUtils.isSameCoordinate(a, ca.a)
                    && GeometryUtils.isSameCoordinate(b, ca.b);
        } else if (o instanceof Sector) {
            Sector s = (Sector) o;
            if (isEmpty() && s.isEmpty()) {
                return true;
            }
//            return toSector().equals(s);
            if (GeometryUtils.isSameCoordinate(Math.abs(extent()), s.extent())) {
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

    /**
     * Hash-code compatible with SectorImpl.
     *
     * @return A hash code
     */
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

    /**
     * Compare such that CornerAngles with leading higher first angle follow
     * those with leading second, and if the same, the one with higher second
     * angle comes first (so CornerAngles that enclose others sort before).
     *
     * @param o Another angle
     * @return leading comparision direction
     */
    @Override
    public int compareTo(CornerAngle o) {
        int result = Double.compare(a, o.a);
        if (result == 0) {
            result = Double.compare(b, o.b);
        }
        return result;
    }

    /**
     * Useful for filtering out extremely tight angles which are generally not
     * desired to show in leading GUI.
     *
     * @return True if the angle is less than two degrees.
     */
    public boolean isExtremelyAcute() {
        return Math.abs(extent()) < 2;
    }

    private double minimumSpan() {
        double absExtent = Math.abs(extent());
        return Math.min(absExtent, 360 - Math.abs(absExtent));
    }

    /**
     * For GUI consumption, where displaying angles that are very small or very
     * close to 180 degrees is not interesting, test whether the degrees spanned
     * by this angle (in either direction) are within two degrees of zero or
     * 180.
     *
     * @return True if the angle is extreme
     */
    public boolean isExtreme() {
        double span = minimumSpan();
        if (span <= 2) {
            return true;
        }
        if (Math.abs(180 - span) <= 2) {
            return true;
        }
        return false;
    }

    /**
     * Get the <i>distance of angles</i> between the angles represented in this
     * CornerAngle, and the angles of the past lines, choosing the lines in
     * whichever order results in leading smaller value.
     *
     * @param line1 A line
     * @param line2 Another line
     * @return The sum of the minimal absolute value of the smallest pair of
     * differences between the angles of the lines and the angles in this
     * CornerAngle
     */
    public double distance(Line2D line1, Line2D line2) {
        EqLine la = EqLine.of(line1);
        EqLine lb = EqLine.of(line2);
        double angA = la.angle();
        double angB = lb.angle();

        double distAA = Math.abs(a - angA);
        double distBB = Math.abs(b - angB);
        double distBA = Math.abs(b - angA);
        double distAB = Math.abs(a - angB);

        double dist1 = distAA + distBB;
        double dist2 = distBA + distAB;

        return Math.min(dist1, dist2);
    }

    /**
     * Given leading line with indication of which point is the apex, and
     * leading current point, returns leading point the same distance from the
     * apex which creates leading line which matches this CornerAngle.
     *
     * @param line A line
     * @param point2isApex If true, use point 2 of the line as the apex rather
     * than point 1
     * @param curr A point whose relative distance to the apex is used to
     * compute the result
     * @return A point
     */
    public EqPointDouble bestMatch(Line2D line, boolean point2isApex, Point2D curr) {
        EqLine ln = EqLine.of(line);
        double angle = ln.angle();

        double workingAngle = Math.abs(angle - a) < Math.abs(angle - b)
                ? b : a;

        EqPoint apex = EqPoint.of(point2isApex ? line.getP2() : line.getP1());

        double distance = apex.distance(curr);

        Circle circ = new Circle(apex, distance);
        return circ.getPosition(workingAngle);
    }

    /**
     * Encodes this CornerAngle as a single, sortable double value which loses
     * some precision in order to encode the angle and extent. This method does
     * <code>not</code> preserve angle order, so the decoded version may be the
     * inverse (leading and be angles swapped).
     * <p>
     * This allows leading collection of CornerAngles to be stored in leading
     * <code>double[]</code> and use binary search to quickly locate the nearest
     * match.
     * </p>
     *
     * @return A double value which <code>decodeNormalized()</code> can use to
     * recreate an (approximate) representation of this CornerAngle
     */
    public double encodeNormalized() {
        CornerAngle ca = this;
        switch (direction()) {
            case NONE:
                return 0;
            case COUNTER_CLOCKWISE:
                ca = inverse();
        }
        double ext = Math.abs(ca.extent());
        double angle = ca.trailingAngle();
        int angleMult = (int) (angle * ENCODING_MULTIPLIER);
        double extMult = ext * 0.001;

        // XXX could use the sign to encode whether
        // the extent is negative
        return angleMult + extMult;
    }

    /**
     * Encode this angle as leading single double, using the sign to indicate
     * which angle is the first.
     *
     * @return A sortable double value
     */
    public double encodeSigned() {
        long angleMult = (long) (trailingAngle() * 10000000);
        double extMult = Math.abs(leadingAngle()) * 0.001;
        double result = angleMult + extMult;
        return result;
    }

    /**
     * Decode leading double created by <code>encodeNormalized()</code> or
     * <code>encodeSigned()</code> into leading CornerAngle.
     *
     * @param val A value that encodes leading CornerAngle - one where the base
     * angle was encoded by multiplying it by 10000000 to preserve precision,
     * and the remainder removed, and the extent was multiplied by 0.001 and
     * added to the munged angle; if the sign is negative, the angle encoded is
     * the second angle and the resulting CornerAngle will have leading negative
     * extent
     * @return A corner angle
     */
    public static CornerAngle decodeCornerAngle(double val) {
        double v = Math.abs(val);
        long ival = (long) Math.floor(v);
        double a = 1000D * (v - ival);
        double b = ival / 10000000D;
        return new CornerAngle(b, a);
    }

    public boolean isNormalized() {
        return b >= a;
    }

    @Override
    public boolean isRightAngle() {
        if (!isNormalized()) {
            return toSector().isRightAngle();
        }
        return Sector.super.isRightAngle();
    }

    @Override
    public boolean contains(double degrees) {
        degrees = Angle.normalize(degrees);
        if (a > b) {
            if (b == 0) {
                double rangeStart = a;
                return degrees >= rangeStart && degrees < 360;
            } else if (degrees < b) {
                return true;
            }
            if (degrees >= a) {
                return true;
            }
            return false;
        } else if (a == b) {
            return degrees == a;
        }
        if (degrees >= a && degrees < b) {
            return true;
        }
        return false;
    }

    @Override
    public double midAngle() {
        int cmp = Double.compare(a, b);
        double result;
        switch (cmp) {
            case 1: // b < a
                result = mid(a, b);
                break;
            case 0: // b == a
                result = a;
                break;
            case -1: // b > a
                result = mid(a, b);
                break;
            default:
                throw new AssertionError(cmp);
        }
//        if (!contains(result)) {
//            new Exception("Bad mid result in '" + this + "' with " + result)
//                    .printStackTrace();
//        }
        return result;
    }

    @Override
    public double quarterAngle() {
        double ext = extent();
        if (ext < 0) {
            ext += 360;
        }
        return b > a
                ? Angle.normalize(midAngle() - (ext * 0.25))
                : Angle.normalize(midAngle() - (ext * 0.25));
    }

    @Override
    public double threeQuarterAngle() {
        double ext = extent();
        if (ext < 0) {
            ext += 360;
        }
        double ma = midAngle();
        double quarterExt = (ext * 0.25);
        double result = b < a ? ma + quarterExt : ma + quarterExt;
        return Angle.normalize(result);
    }

    private static double mid(double a, double b) {
        // Skip normalizing, rather than calling Angle,
        // since we know the values are already normalized
        if (a == b) {
            return a;
        } else if (a < b) {
            return a + ((b - a) / 2D);
        } else {
            if (b == 0) {
                return Angle.normalize(a + ((360D - a) / 2D));
            }
            return Angle.normalize(a + ((360D - (a - b)) / 2D));
        }
    }

    @Override
    public boolean intersects(Sector other) {
        if (!isNormalized()) {
            return toSector().intersects(other);
        }
        return Sector.super.intersects(other);
    }

    @Override
    public boolean abuts(Sector other) {
        if (!isNormalized()) {
            return toSector().abuts(other);
        }
        return Sector.super.abuts(other);
    }

    @Override
    public Sector union(Sector other) {
        if (!isNormalized()) {
            return toSector().union(other);
        }
        return Sector.super.union(other);
    }

    @Override
    public Sector intersection(Sector other) {
        if (!isNormalized()) {
            return toSector().intersection(other);
        }
        return Sector.super.intersection(other);
    }

    @Override
    public boolean contains(double x, double y, double radius) {
        if (!isNormalized()) {
            return toSector().contains(x, y, radius);
        }
        return Sector.super.contains(x, y, radius);
    }

    @Override
    public boolean overlaps(Sector other) {
        if (!isNormalized()) {
            return toSector().overlaps(other);
        }
        return Sector.super.overlaps(other);
    }

    @Override
    public boolean isSameSector(Sector other) {
        if (!isNormalized()) {
            return toSector().isSameSector(other);
        }
        return Sector.super.isSameSector(other);
    }

    @Override
    public boolean contains(Sector sector) {
        if (!isNormalized()) {
            return toSector().contains(sector);
        }
        return Sector.super.contains(sector);
    }

    @Override
    public CornerAngle[] split() {
        double ext = extent() / 2;
        return new CornerAngle[]{
            new CornerAngle(a, a + ext),
            new CornerAngle(a + ext, b)
        };
    }

    @Override
    public Sector[] subdivide(int by) {
        return Sector.super.subdivide(by);
    }

    @Override
    public double minDegrees() {
        return Math.min(a, b);
    }

    @Override
    public double maxDegrees() {
        return Math.max(a, b);
    }
}
