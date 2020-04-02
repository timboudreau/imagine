package org.imagine.geometry;

import com.mastfrog.function.DoubleBiFunction;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import static java.awt.geom.PathIterator.SEG_CLOSE;
import static java.awt.geom.PathIterator.SEG_CUBICTO;
import static java.awt.geom.PathIterator.SEG_LINETO;
import static java.awt.geom.PathIterator.SEG_MOVETO;
import static java.awt.geom.PathIterator.SEG_QUADTO;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import org.imagine.geometry.util.GeometryUtils;
import org.imagine.geometry.util.function.Bool;
import org.imagine.geometry.util.function.Int;
import org.imagine.geometry.util.function.IntWithChildren;

/**
 * An angle between two vectors; unlike the generic Sector, which has a start
 * and an extent, the salient feature of a CornerAngle is that it represents two
 * angles, which may be clockwise or counter-clockwise from each other.
 * <p>
 * Unlike other Sector implementations, the angles of a CornerAngle have an
 * order, and the extent may be negative.
 * </p>
 *
 * @author Tim Boudreau
 */
public final strictfp class CornerAngle implements Sector, Comparable<CornerAngle> {

    private final double a;
    private final double b;

    public CornerAngle(double a, double b) {
        this.a = Angle.normalize(a);
        this.b = Angle.normalize(b);
    }

    public CornerAngle(Point2D start, Point2D apex, Point2D end) {
        this(start.getX(), start.getY(), apex.getX(), apex.getY(), end.getX(), end.getY());
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

    public String toShortString() {
        return GeometryUtils.toDegreesStringShort(a) + "-"
                + GeometryUtils.toDegreesStringShort(b)
                + "(" + GeometryUtils.toDegreesStringShort(b - a) + ")";
    }

    public static Emitter emitter(Point2D initialPoint) {
        return new Emitter(initialPoint);
    }

    public static Emitter emitter(double initialX, double initialY) {
        return new Emitter(initialX, initialY);
    }

    public static Emitter emitter() {
        return new Emitter();
    }

    /**
     * Consumer which can collect corner angles within a shape.
     */
    public interface CornerAngleConsumer {

        /**
         * Called with one corner angle within the shape. The apex point value
         * is the index of the <i>logical point</i> within the shape (and will
         * match control point indices from the vector library) - i.e. it is
         * incremented once for <i>every x/y coordinate pair</i>
         * within the shape - so, a SEG_MOVETO starting a shape, followed by a
         * SEG_CUBICTO, means the index of a subsequent SEG_LINETO will be
         * <i>4</i> (0 is the initial SEG_MOVETO, and 1, 2 and 3 are the two
         * control points and destination point of the cubic curve).
         * <p>
         * Generally, points should be emitted for consecutive stright-line
         * segments, not emitted following a SEG_MOVETO or SEG_CLOSE until 3
         * more straight-line segment points have been collected, and similarly,
         * stop emitting corners when a SEG_CUBICTO or SEG_QUADTO instruction is
         * encountered, until once again, a third straight-line segment has been
         * collected.
         * </p>
         *
         * @param apexPointIndexWithinShape The index of the logical point
         * within the shape at which the passed corner occurs, to map it back to
         * information about that point
         * @param angle The angle of the corner encountered
         */
        void accept(int apexPointIndexWithinShape, CornerAngle angle, double x, double y, int shapeSelfIntersections);
    }

    public static RotationDirection forShape(Shape shape, CornerAngleConsumer c) {
        return forShape(shape, null, c);
    }

    public static RotationDirection forShape(Shape shape, AffineTransform xform, CornerAngleConsumer c) {
        return forShape(shape.getPathIterator(xform), c);
    }

    public static RotationDirection forShape(PathIterator iter, CornerAngleConsumer c) {
        double[] data = new double[6];
        Emitter emitter = new Emitter();
        int lastType = -1;
        double startX, startY, lastX, lastY, secondX, secondY;
        startX = lastX = lastY = startY = secondX = secondY = 0;
        // Holder for the point index which can be incremented inside a
        // lambda
        IntWithChildren pointIndex = Int.createWithChildren();
        // Holder for the point index within the current shape, which can
        // be reset independently but will be incremented when pointIndex is
        Int pointIndexWithinShape = pointIndex.child();

        Bool hasSecondPoint = Bool.create();

        Runnable onNewSubshape = () -> {
            pointIndexWithinShape.reset();
            emitter.reset();
            hasSecondPoint.reset();
            emitter.intersections = 0;
        };

        List<Intersectable> lines = new ArrayList<>();
        ToIntFunction<Intersectable> intersectionCounter = isect -> {
            if (lines.isEmpty()) {
                lines.add(isect);
                return 0;
            }
            int result = 0;
            for (Intersectable i : lines) {
                result += i.intersectionCount(isect, false);
            }
            if (result != 0) {
                System.out.println("At " + pointIndex + " " + result
                        + " intersections");
            } else {
                System.out.println("No new intersections at "
                        + pointIndex + " with " + lines.size()
                );
            }
            lines.add(isect);
            return result;
        };

        // XXX to do this right, we need to duplicate the
        // intersection counting code in the com.sun geom Java2D
        // package
        while (!iter.isDone()) {
            int type = iter.currentSegment(data);
            switch (type) {
                case SEG_CLOSE:
                    if (lastType != SEG_CLOSE && lastType != SEG_MOVETO) {
                        int pix = pointIndex.getAsInt()
                                - pointIndexWithinShape.getAsInt();
                        // Handle the trailing series of lines where the
                        // last point of this sub-path is the apex
                        if (lastX != startX || lastY != startY) {
                            EqLine ln = new EqLine(lastX, lastY, startX, startY);
                            int isects = intersectionCounter.applyAsInt(ln);
                            CornerAngle ang = emitter.apply(startX, startY);
                            if (ang != null) {
                                c.accept(pix, ang, lastX, lastY, emitter.intersections);
                            }
                            emitter.intersections += isects;
                            // And handle the trailing series of lines where the
                            // 0th point of this sub-path is the apex - that
                            // provides a corner we will have skipped because
                            // we only had two points when we initially iterated
                            // past it
                            if (hasSecondPoint.getAsBoolean()) {
                                ln.setLine(startX, startY, secondX, secondY);
                                isects = intersectionCounter.applyAsInt(ln);
                                CornerAngle ang2 = emitter.apply(secondX, secondY);
                                if (ang2 != null) {
                                    c.accept(pix + 1, ang2, startX, startY, emitter.intersections);
                                }
                                emitter.intersections += isects;
                            }
                        }
                    }
                    onNewSubshape.run();
                    break;
                case SEG_MOVETO:
                    onNewSubshape.run();
                    emitter.apply(lastX = data[0], lastY = data[1]);
                    startX = data[0];
                    startY = data[1];
                    pointIndex.increment();
                    break;
                // fallthrough
                case SEG_LINETO:
                    EqLine ln = new EqLine(lastX, lastY, data[0], data[1]);
                    int isects = intersectionCounter.applyAsInt(ln);
                    CornerAngle ang = emitter.apply(data[0], data[1]);
                    if (ang != null) {
                        c.accept(pointIndex.getAsInt(), ang, lastX, lastY,
                                emitter.intersections);
                    }
                    lastX = data[0];
                    lastY = data[1];
                    emitter.intersections += isects;
                    if (pointIndexWithinShape.equals(1)) {
                        secondX = data[0];
                        secondY = data[1];
                        hasSecondPoint.set();
                    }
                    pointIndex.increment();
                    break;
                // We are only interested in straight line angles here,
                // so reset the emitter's state, but not the subshape
                // state
                case SEG_QUADTO:
                    // Increment twice, on the off chance the emitter
                    // is altered to emit something on quad points, so
                    // the right point is set at the time we call apply
                    Polygon2D approximateQ = GeometryUtils.approximateQuadraticCurve(lastX, lastY, data[0], data[1], data[2], data[3]);
                    int qIsects = intersectionCounter.applyAsInt(approximateQ);
                    pointIndex.increment();
                    emitter.reset();
                    emitter.apply(lastX = data[2], lastY = data[3]);
                    emitter.intersections += qIsects;
                    pointIndex.increment();
                    break;
                case SEG_CUBICTO:
                    Polygon2D approximateC = GeometryUtils.approximateCubicCurve(lastX, lastY, data[0], data[1], data[2], data[3], data[4], data[5]);
                    int cIsects = intersectionCounter.applyAsInt(approximateC);
                    emitter.reset();
                    pointIndex.increment(2);
                    emitter.apply(lastX = data[4], lastY = data[5]);
                    emitter.intersections += cIsects;
                    pointIndex.increment();
                    break;
            }
            iter.next();
            lastType = type;
        }
        return emitter.direction();
    }

    /**
     * Emits corner angles for points, once it has been passed 3 or more; check
     * for nulls.
     */
    public static final class Emitter implements DoubleBiFunction<CornerAngle>, Function<Point2D, CornerAngle> {

        private double lastX, lastY;
        private double prevX, prevY;
        private int state;
        private int intersections;

        Emitter(Point2D p) {
            this(p.getX(), p.getY());
        }

        Emitter(double lastX, double lastY) {
            this.prevX = lastX;
            this.prevY = lastY;
            state = 1;
        }

        Emitter() {

        }

        public Emitter reset() {
            state = 0;
            return this;
        }

        void emit(double x, double y, Consumer<CornerAngle> into) {
            CornerAngle angle = apply(x, y);
            if (angle != null) {
                into.accept(angle);
            }
        }

        public Emitter fullReset() {
            reset();
            intersections = 0;
            return this;
        }

        public Emitter intersectionEncountered() {
            intersections++;
            return this;
        }

        private int cwCount;
        private int ccwCount;

        public int clockwiseCount() {
            return cwCount;
        }

        public int counterclockwiseCount() {
            return ccwCount;
        }

        public RotationDirection direction() {
            RotationDirection dir = cwCount > ccwCount ? RotationDirection.CLOCKWISE
                    : RotationDirection.COUNTER_CLOCKWISE;
            if ((intersections / 2) % 2 == 1) {
                dir = dir.opposite();
            }
            return dir;
        }

        @Override
        public CornerAngle apply(double x, double y) {
            // XXX to get this perfectly right, we need to count
            // intersections, and reverse the order in which we pass
            // prevX/Y and x/y when the intesection count is odd
            switch (state) {
                case 0:
                    state++;
                    prevX = x;
                    prevY = y;
                    return null;
                case 1:
                    state++;
                    lastX = x;
                    lastY = y;
                    return null;
                case 2:
                    CornerAngle result;
                    if (intersections % 2 == 1) {
                        result = new CornerAngle(x, y, lastX, lastY, prevX, prevY);
//                        result = new CornerAngle(prevX, prevY, lastX, lastY, x, y);
                    } else {
                        result = new CornerAngle(prevX, prevY, lastX, lastY, x, y);
                    }
                    switch (result.direction()) {
                        case CLOCKWISE:
                            cwCount++;
                            break;
                        case COUNTER_CLOCKWISE:
                            ccwCount++;
                            break;
                    }
                    prevX = lastX;
                    prevY = lastY;
                    lastX = x;
                    lastY = y;
                    return result;
                default:
                    throw new AssertionError("Invalid state " + state);
            }

        }

        @Override
        public CornerAngle apply(Point2D t) {
            return apply(t.getX(), t.getY());
        }
    }

    /**
     * Determine if this CornerAngle is the same as the passed one with the a
     * and b angles reversed.
     *
     * @param other
     * @return
     */
    public boolean isHomomorphic(CornerAngle other) {
        return (other.a == a && other.b == b)
                || (other.a == b && other.b == a);
    }

    /**
     * Get the first angle.
     *
     * @return The first angle in degrees
     */
    public double aDegrees() {
        return a;
    }

    /**
     * Get the second angle.
     *
     * @return The second angle in degrees
     */
    public double bDegrees() {
        return b;
    }

    /**
     * Get the first angle
     *
     * @return The first angle
     */
    public Angle a() {
        return Angle.ofDegrees(a);
    }

    /**
     * Get the second angle
     *
     * @return The second angle
     */
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
    public double start() {
        return a;
    }

    @Override
    public double extent() {
        if (b == a) {
            return 0;
        }
//        if (b < a) {
//            return 360 - (b - a);
//        }
        return b - a;
//        double a3 = a + 360;
//        double b3 = b + 360;
//        double res = b3 - a3;
//        if (res < 0) {
//            res = 360 + res;
//        }
//        return res;
//        if (a > 270 && b < 90) {
//            return -(360 - (a - b));
//        }
//        return b - a;
    }

    public Sector toSector() {
//        if (b < a) {
//            double ext = extent();
//            if (ext < 0) {
//                ext = 360 - ext;
//            }
//            return new SectorImpl(b, ext);
//        }
//        return new SectorImpl(a, b - a);
        return new SectorImpl(Math.min(a, b), Math.abs(extent()));
    }

    public CornerAngle opposite2() {
        if (extent() == 360) {
            return this;
        }
        double mid = midAngle();
        double opp = Angle.opposite(mid);
        System.out.println("Oppsite of " + mid + " is " + opp);
        double half = extent() / 2;
        double newA = Angle.subtractAngles(opp, half);
        double newB = Angle.addAngles(opp, half);
        CornerAngle result = new CornerAngle(newB, newA);
//        double oppA = Angle.opposite(a);
//        double oppB = Angle.opposite(b);
//        System.out.println("  opp of " + a + " is " + oppA);
//        System.out.println("  opp of " + b + " is " + oppB);
//        System.out.println("  old ext " + extent());
//        System.out.println("  new next " + (oppA + extent()));
//        CornerAngle result = new CornerAngle(oppA + extent(), oppA);
        System.out.println("    result extent " + result.extent());
        return result;
    }

    @Override
    public boolean isEmpty() {
        return a == b;
    }

    /**
     * Unlike other Sector implementations, opposite() on CornerAngle returns a
     * 180\u00B0 flipped CornerAngle.
     *
     * @return A CornerAngle
     */
    @Override
    public CornerAngle opposite() {
        if (extent() == 360) {
            return this;
        }
        return new CornerAngle(Angle.opposite(b), Angle.opposite(a));
//        return new CornerAngle(Angle.opposite(a), Angle.opposite(b));
    }

    public CornerAngle reversed() {
        if (!isNormalized()) {
            System.out.println("inorm " + b + " to " + (b + 360 - extent()));
            return new CornerAngle(a, a + 360 - extent());
        }
        return new CornerAngle(b, b + 360 - extent());
    }

    @Override
    public CornerAngle inverse() {
        return new CornerAngle(b, a);
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
                + GeometryUtils.toString(b) + "\u00B0"
                + " (" + GeometryUtils.toString(extent()) + "\u00B0)";
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
     * Compare such that CornerAngles with a higher first angle follow those
     * with a second, and if the same, the one with higher second angle comes
     * first (so CornerAngles that enclose others sort before).
     *
     * @param o Another angle
     * @return a comparision direction
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
     * desired to show in a GUI.
     *
     * @return True if the angle is less than two degrees.
     */
    public boolean isExtremelyAcute() {
        return Math.abs(extent()) < 2;
    }

    /**
     * Create a normalized version of this CornerAngle which is consistent with
     * Sector, where the least angle is the leading angle and the extent is
     * positive.
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
     * Get the <i>distance of angles</i> between the angles represented in this
     * CornerAngle, and the angles of the past lines, choosing the lines in
     * whichever order results in a smaller value.
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
     * Given a line with indication of which point is the apex, and a current
     * point, returns a point the same distance from the apex which creates a
     * line which matches this CornerAngle.
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
     * inverse (a and be angles swapped).
     * <p>
     * This allows a collection of CornerAngles to be stored in a
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
                System.out.println("invert");
        }
        double ext = Math.abs(ca.extent());
        double angle = ca.aDegrees();
        int angleMult = (int) (angle * 10000000);
        double extMult = ext * 0.001;

        // XXX could use the sign to encode whether
        // the extent is negative
        return angleMult + extMult;
    }

    /**
     * Encode this angle as a single double, using the sign to indicate which
     * angle is the first.
     *
     * @return A sortable double value
     */
    public double encodeSigned() {
        double ext = extent();
        double angle = ext < 0 ? bDegrees() : aDegrees();

        int angleMult = (int) (angle * 10000000);
        double extMult = Math.abs(ext) * 0.001;

        // XXX could use the sign to encode whether
        // the extent is negative
        return angleMult + extMult;
    }

    /**
     * Decode a double created by <code>encodeNormalized()</code> or
     * <code>encodeSigned()</code> into a CornerAngle.
     *
     * @param val A value that encodes a CornerAngle - one where the base angle
     * was encoded by multiplying it by 10000000 to preserve precision, and the
     * remainder removed, and the extent was multiplied by 0.001 and added to
     * the munged angle; if the sign is negative, the angle encoded is the
     * second angle and the resulting CornerAngle will have a negative extent
     * @return A corner angle
     */
    public static CornerAngle decodeCornerAngle(double val) {
        double v = Math.abs(val);
        int ival = (int) Math.floor(v);
        double ext = 1000D * (v - ival);
        System.out.println("  decode ext " + ext);
        double ang = ival / 10000000D;
        System.out.println("  decode ang " + ang);
        if (val < 0) {
            return new CornerAngle(ang - ext, ang);
        } else {
            return new CornerAngle(ang, ang + ext);
        }
    }

    public boolean isNormalized() {
        return b > a;
    }

    @Override
    public boolean isRightAngle() {
        if (!isNormalized()) {
            return normalized().isRightAngle();
        }
        return Sector.super.isRightAngle();
    }

    @Override
    public double midAngle() {
        return Angle.normalize(a + (extent() / 2));
//
//        if (!isNormalized()) {
//            return normalized().midAngle();
//        }
//        return Sector.super.midAngle();
    }

    public double quarterAngle() {
        double ext = Math.abs(extent());
        return midAngle() - (ext / 4);
    }

    public double threeQuarterAngle() {
        double ext = Math.abs(extent());
        return midAngle() + (ext / 4);
    }

    @Override
    public boolean intersects(Sector other) {
        if (!isNormalized()) {
            return normalized().intersects(other);
        }
        return Sector.super.intersects(other);
    }

    @Override
    public boolean abuts(Sector other) {
        if (!isNormalized()) {
            return normalized().abuts(other);
        }
        return Sector.super.abuts(other);
    }

    @Override
    public Sector union(Sector other) {
        if (!isNormalized()) {
            return normalized().union(other);
        }
        return Sector.super.union(other);
    }

    @Override
    public Sector intersection(Sector other) {
        if (!isNormalized()) {
            return normalized().intersection(other);
        }
        return Sector.super.intersection(other);
    }

    @Override
    public Shape toShape(double x, double y, double radius) {
        if (!isNormalized()) {
            return normalized().toShape(x, y, radius);
        }
        return Sector.super.toShape(x, y, radius);
    }

    @Override
    public boolean contains(double x, double y, double radius) {
        if (!isNormalized()) {
            return normalized().contains(x, y, radius);
        }
        return Sector.super.contains(x, y, radius);
    }

    @Override
    public boolean overlaps(Sector other) {
        if (!isNormalized()) {
            return normalized().overlaps(other);
        }
        return Sector.super.overlaps(other);
    }

    @Override
    public boolean isSameSector(Sector other) {
        if (!isNormalized()) {
            return normalized().isSameSector(other);
        }
        return Sector.super.isSameSector(other);
    }

    @Override
    public boolean contains(Sector sector) {
        if (!isNormalized()) {
            return normalized().contains(sector);
        }
        return Sector.super.contains(sector);
    }

    @Override
    public Sector[] split() {
        if (!isNormalized()) {
            return normalized().split();
        }
        return Sector.super.split();
    }

    @Override
    public Sector[] subdivide(int by) {
        if (!isNormalized()) {
            return normalized().subdivide(by);
        }
        return Sector.super.subdivide(by);
    }

    @Override
    public boolean contains(double degrees) {
        if (!isNormalized()) {
            return normalized().contains(degrees);
        }
        return Sector.super.contains(degrees);
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
