package org.imagine.geometry.util;

import com.mastfrog.function.DoubleBiConsumer;
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
import static java.lang.Double.doubleToLongBits;
import static java.lang.Math.pow;
import org.imagine.geometry.EqPointDouble;
import org.imagine.geometry.Polygon2D;

/**
 * Various utilities for geometric functions and dealing with rounding errors.
 *
 * @author Tim Boudreau
 */
public class GeometryUtils {

    private static final float DEFAULT_TOLERANCE = 0.0000000000001F;
    private static final double[] APPROXIMATION_POSITIONS
            = new double[]{0.125, 0.25, 0.375, 0.5, 0.625, 0.75, 0.825, 1};

    public static int curveApproximationPointCount() {
        return APPROXIMATION_POSITIONS.length;
    }

    // Line2D.intersects produces erroneous values for certain tests where
    // one coordinate of the tested line is an exact match; so we offset the
    // values we test very slightly in our containment test
    public static final double INTERSECTION_FUDGE_FACTOR = 1.0E-12;

    /**
     * Determine if two line segments intersect, working around the false
     * negatives <code>Line2D.linesIntersect()</code> can return.
     *
     * @param a The first line segment
     * @param b The second line segment
     * @return true if they intersect
     */
    public static boolean linesIntersect(Line2D a, Line2D b) {
        return linesIntersect(a, b.getX1(), b.getY1(), b.getX2(), b.getY2());
    }

    /**
     * Determine if two line segments intersect, working around the false
     * negatives <code>Line2D.linesIntersect()</code> can return.
     *
     * @param line A line segment
     * @param x1 Another line segment's first x coordinate
     * @param y1 Another line segment's first y coordinate
     * @param x2 Another line segment's second x coordinate
     * @param y2 Another line segment's second y coordinate
     * @return True if they intersect
     */
    public static boolean linesIntersect(Line2D line, double x1, double y1, double x2, double y2) {
        return linesIntersect(line.getX1(), line.getY1(), line.getX2(), line.getY2(),
                x1, y1, x2, y2);
    }

    /**
     * Determine if two line segments intersect, working around the false
     * negatives <code>Line2D.linesIntersect()</code> can return.
     *
     * @param x1 The first line segment's first x coordinate
     * @param y1 The first line segment's first y coordinate
     * @param x2 The first line segment's second x coordinate
     * @param y2 The first line segment's second y coordinate
     * @param x3 The second line segment's first x coordinate
     * @param y3 The second line segment's first y coordinate
     * @param x4 The second line segment's second x coordinate
     * @param y4 The second line segment's second y coordinate
     * @return True if the lines intersect
     */
    public static boolean linesIntersect(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
        if ((x1 == x3 && y1 == y3)
                || (x2 == x3 && y2 == y3)
                || (x1 == x4 && y1 == y4)
                || (x2 == x4 && y2 == y4)) {
            return true;
        }
        if (Line2D.linesIntersect(x1, y1, x2, y2, x3, y3, x4, y4)) {
            return true;
        }
        return Line2D.linesIntersect(x1, y1 + INTERSECTION_FUDGE_FACTOR, x2, y2 - INTERSECTION_FUDGE_FACTOR, x3, y3, x4, y4);
    }

    /**
     * Determine if two line segments intersect, working around the false
     * negatives <code>Line2D.linesIntersect()</code> can return.
     *
     * @param x1 The first line segment's first x coordinate
     * @param y1 The first line segment's first y coordinate
     * @param x2 The first line segment's second x coordinate
     * @param y2 The first line segment's second y coordinate
     * @param x3 The second line segment's first x coordinate
     * @param y3 The second line segment's first y coordinate
     * @param x4 The second line segment's second x coordinate
     * @param y4 The second line segment's second y coordinate
     * @param ifAbutting The result to return if two of the points are equal
     * @return True if the lines intersect
     */
    public static boolean linesIntersect(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4, boolean ifAbutting) {
        if ((x1 == x3 && y1 == y3)
                || (x2 == x3 && y2 == y3)
                || (x1 == x4 && y1 == y4)
                || (x2 == x4 && y2 == y4)) {
            return ifAbutting;
        }
        if (Line2D.linesIntersect(x1, y1, x2, y2, x3, y3, x4, y4)) {
            return true;
        }
        return Line2D.linesIntersect(x1, y1 + INTERSECTION_FUDGE_FACTOR, x2, y2 - INTERSECTION_FUDGE_FACTOR, x3, y3, x4, y4);
    }

    /**
     * Determine if two points are the same point within the default tolerance
     * to account for rounding errors.
     *
     * @param a The first point
     * @param b The second point
     * @return True if they match
     */
    public static boolean isSamePoint(Point2D a, Point2D b) {
        return isSamePoint(a.getX(), a.getY(), b.getX(), b.getY());
    }

    public static boolean isSamePoint(double x1, double y1, double x2, double y2) {
        return isSameCoordinate(x1, x2) && isSameCoordinate(y1, y2);
    }

    public static boolean isSamePoint(double x1, double y1, double x2, double y2, double tolerance) {
        return isSameCoordinate(x1, x2, tolerance) && isSameCoordinate(y1, y2, tolerance);
    }

    public static boolean isSamePoint(float x1, float y1, float x2, float y2, float tolerance) {
        return isSameCoordinate(x1, x2, tolerance) && isSameCoordinate(y1, y2, tolerance);
    }

    public static boolean isSameCoordinate(float a, float b) {
        return isSameCoordinate(a, b, DEFAULT_TOLERANCE);
    }

    public static boolean isSameCoordinate(float a, float b, float tolerance) {
        if (a == b || (a + 0.0F) == (b + 0.0F)) {
            return true;
        }
        return Math.abs(a - b) < tolerance;
    }

    public static boolean isSameCoordinate(double a, double b) {
        return isSameCoordinate(a, b, DEFAULT_TOLERANCE);
    }

    public static boolean isSameCoordinate(double a, double b, double tolerance) {
        if (a == b || (a + 0.0) == (b + 0.0)) {
            return true;
        }
        return Math.abs(a - b) < tolerance;
    }

    /**
     * Reverse an array of point coordinate pairs.
     *
     * @param points The points
     */
    public static void reversePointsInPlace(double[] points) {
        double[] temp = (double[]) points.clone();
        for (int i = 0, i2 = points.length - 2; i < points.length; i += 2, i2 -= 2) {
            points[i] = temp[i2];
            points[i + 1] = temp[i2 + 1];
        }
    }

    /**
     * Get the midpoint of a line.
     *
     * @param x1 The first x
     * @param y1 The first y
     * @param x2 The second x
     * @param y2 The second y
     * @return A point
     */
    public static EqPointDouble midPoint(double x1, double y1, double x2, double y2) {
        if (x1 == x2 && y1 == y2) {
            return new EqPointDouble(x1, y1);
        }
        double xm = x1 + ((x2 - x1) / 2);
        double ym = y1 + ((y2 - y1) / 2);
        return new EqPointDouble(xm, ym);
    }

    /**
     * Get a point equidistant between two points.
     *
     * @param x1 The first x coordinate
     * @param y1 The first y coordinate
     * @param x2 The second x coordinate
     * @param y2 The second y coordinate
     * @return An array of two double coordinates
     */
    public static double[] equidistantPoint(double x1, double y1, double x2, double y2) {
        if (x1 == x2 && y1 == y2) {
            return new double[]{x1, y1};
        }
        return new double[]{(x1 + x2) / 2D, (y1 + y2) / 2D};
    }

    /**
     * Get a point equidistant between two points into the passed array at the
     * passed offset.
     *
     * @param x1 The first x coordinate
     * @param y1 The first y coordinate
     * @param x2 The second x coordinate
     * @param y2 The second y coordinate
     * @param at The array offset
     * @param into The array to modify
     * @return An array of two double coordinates
     */
    public static void equidistantPoint(double x1, double y1, double x2, double y2, double[] into, int at) {
        if (x1 == x2 && y1 == y2) {
            into[at] = x1;
            into[at + 1] = y1;
        }
        double ex = (x1 + x2) / 2D;
        double ey = (y1 + y2) / 2D;
        into[at] = ex;
        into[at + 1] = ey;
    }

    /**
     * Get the intersection point of two lines, or null if they are parallel.
     *
     * @param ax The first line segment's first x coordinate
     * @param ay The first line segment's first y coordinate
     * @param bx The first line segment's second x coordinate
     * @param by The first line segment's second y coordinate
     * @param cx The second line segment's first x coordinate
     * @param cy The second line segment's first y coordinate
     * @param dx The second line segment's second x coordinate
     * @param dy The second line segment's second y coordinate
     * @return True if they intersect
     */
    public static EqPointDouble intersection(double ax, double ay, double bx, double by, double cx, double cy, double dx, double dy) {
        double a1 = by - ay;
        double b1 = ax - bx;
        double c1 = a1 * (ax) + b1 * (ay);

        double a2 = dy - cy;
        double b2 = cx - dx;
        double c2 = a2 * (cx) + b2 * (cy);

        double determinant = a1 * b2 - a2 * b1;

        if (determinant == 0) {
            // The lines are parallel
            return null;
        } else {
            double x = (b2 * c1 - b1 * c2) / determinant;
            double y = (a1 * c2 - a2 * c1) / determinant;
            return new EqPointDouble(x, y);
        }
    }

    /**
     * Provides a standard way to compute the hash code of a pair of x/y
     * coordinates, normalizing the difference between -0.0 and and 0.0.
     *
     * @param x An x coordinate
     * @param y A y coordinate
     * @return An integer
     */
    public static int pointHashCode(double x, double y) {
        double xx = 0.0 + x;
        double yy = 0.0 + y;
        long hash = doubleToLongBits(xx)
                + 217_645_177 * doubleToLongBits(yy);
        return (int) (hash ^ (hash >> 32));
    }

    /**
     * Determine if a triangular region contains a test point.
     *
     * @param ax The triangle's first point's x coordinate
     * @param ay The triangle's first point's y coordinate
     * @param bx The triangle's second point's x coordinate
     * @param by The triangle's second point's y coordinate
     * @param cx The triangle's third point's x coordinate
     * @param cy The triangle's third point's y coordinate
     * @param tx The test point's x coordinate
     * @param ty The test point's y coordinate
     * @return True if the test point is within the triangle
     */
    public static boolean triangleContains(double ax, double ay, double bx, double by, double cx, double cy, double tx, double ty) {
        double alpha = ((by - cy) * (tx - cx)
                + (cx - bx) * (ty - cy)) / ((by - cy) * (ax - cx)
                + (cx - bx) * (ay - cy));
        double beta = ((cy - ay) * (tx - cx)
                + (ax - cx) * (ty - cy)) / ((by - cy) * (ax - cx)
                + (cx - bx) * (ay - cy));
        double gamma = 1.0f - alpha - beta;
        return alpha > 0 && beta > 0 && gamma > 0;
    }

    /**
     * Provides a standard comparison between points based on y-axis then
     * x-axis.
     *
     * @param a A point
     * @param b Another point
     * @return A comparison result
     */
    public int compare(Point2D a, Point2D b) {
        if (isSamePoint(a, b)) {
            return 0;
        }
        int result = Double.compare(a.getY(), b.getY());
        if (result == 0) {
            result = Double.compare(a.getX(), b.getX());
        }
        return result;
    }

    /**
     * Convert an array of ints to an array of bytes, without range checking.
     *
     * @param ints An array of ints
     * @return An array of bytes
     */
    public static byte[] intArrayToByteArray(int[] ints) {
        byte[] result = new byte[ints.length];
        for (int i = 0; i < ints.length; i++) {
            arraySizeForType(ints[i]); // to validate
            result[i] = (byte) ints[i];
        }
        return result;
    }

    /**
     * Convert a double to either the floor or ceiling integer value equivalent.
     *
     * @param val The value
     * @param ceil If true, use <code>Math.ceil()</code> instead of
     * <code>Math.floor()</code>
     * @return An integer representation of the value
     */
    public static int toInt(double val, boolean ceil) {
        return (int) (ceil ? Math.ceil(val) : Math.floor(val));
    }

    /**
     * Utility method for getting the coordinate array size for different
     * segment types on a PathIterator.
     *
     * @param type The type
     * @return the number of coordinates that type uses
     */
    public static int arraySizeForType(int type) {
        switch (type) {
            case SEG_MOVETO:
            case SEG_LINETO:
                return 2;
            case PathIterator.SEG_CUBICTO:
                return 6;
            case PathIterator.SEG_QUADTO:
                return 4;
            case PathIterator.SEG_CLOSE:
                return 0;
            default:
                throw new AssertionError(type);
        }
    }

    /**
     * Determine if an array of points contains any lines that intersect other
     * lines within that array.
     *
     * @param points The points
     * @return True if there are intersections
     */
    public static boolean containsIntersectingLines(double[] points) {
        // triangular and smaller shapes cannot meaningfully self-intersect,
        // they can only contain the same point
        if (points.length < 7) {
            return false;
        }
        for (int test = 0; test < points.length; test += 2) {
            double tx = points[test];
            double ty = points[test + 1];
            int tNext = test + 2 >= points.length ? 0 : test + 2;
            double nx = points[tNext];
            double ny = points[tNext + 1];

            if (tx == nx && ty == ny) {
                return true;
            }

            int against = test + 4 >= points.length ? 0 : test + 4;
            int last = test - 4;
            if (last < 0) {
                last = points.length - 2;
            }
            System.out.println("last for " + test + " is " + last);
            do {
                System.out.println(test + " and " + against);
                double ax = points[against];
                double ay = points[against + 1];

                int aNext = against + 2 >= points.length ? 0 : against + 2;
                double anx = points[aNext];
                double any = points[aNext + 1];
                // Same as above:  Line2D will return false positives for
                // exactly matching coordinates in some corner cases
                if (test != aNext && Line2D.linesIntersect(tx, ty, nx, ny + GeometryUtils.INTERSECTION_FUDGE_FACTOR,
                        ax, ay - GeometryUtils.INTERSECTION_FUDGE_FACTOR, anx, any)) {
                    return true;
                }
                against = (against + 2) % points.length;
            } while (against != last);
        }
        return false;
    }

    /**
     * Compute the (sometimes approximate) length of the perimeter of a shape;
     * lengths for quadratic and cubic curves are approximated by sampling a
     * series of points from <code>t=0</code> to <code>t=1</code> and taking
     * their distance.
     *
     * @param shape The shape
     * @return The shape length
     */
    public static double shapeLength(Shape shape) {
        PathIterator iter = shape.getPathIterator(null);
        double cumulativeResult = 0;
        double current = 0;
        double lastX = 0;
        double lastY = 0;
        double[] data = new double[6];
        while (!iter.isDone()) {
            int type = iter.currentSegment(data);
            switch (type) {
                case SEG_CLOSE:
                    cumulativeResult += current;
                    current = 0;
                    break;
                case SEG_MOVETO:
                    cumulativeResult += current;
                    current = 0;
                    lastX = data[0];
                    lastY = data[1];
                    break;
                case SEG_LINETO:
                    current += Point2D.distance(lastX, lastY, data[0], data[1]);
                    break;
                case SEG_CUBICTO:
                    current += cubicSegmentLength(lastX, lastY, data[0], data[1],
                            data[2], data[3], data[4], data[5]);
                    break;
                case SEG_QUADTO:
                    current += quadraticSegmentLength(lastX, lastY,
                            data[0], data[1], data[2], data[3]);
                    break;
            }
            iter.next();
        }
        cumulativeResult += current;
        return cumulativeResult;
    }

    /**
     * Approximate the shape in the passed iterator as a polygon, iterpolating
     * some quadratic and cubic curve points.
     *
     * @param iter A shape
     * @return A polygon
     */
    public static Polygon2D approximate(Shape shape) {
        return approximate(shape, null);
    }

    /**
     * Approximate the shape in the passed iterator as a polygon, iterpolating
     * some quadratic and cubic curve points.
     *
     * @param iter A shape
     * @param xform A transform
     * @return A polygon
     */
    public static Polygon2D approximate(Shape shape, AffineTransform xform) {
        return approximate(shape.getPathIterator(xform));
    }

    /**
     * Approximate the shape in the passed iterator as a polygon, iterpolating
     * some quadratic and cubic curve points.
     *
     * @param iter A path iterator
     * @return A polygon
     */
    public static Polygon2D approximate(PathIterator iter) {
        DoubleList l = new DoubleList(100);
        double[] data = new double[6];
        while (!iter.isDone()) {
            int type = iter.currentSegment(data);
            double lastStartX = 0;
            double lastStartY = 0;
            switch (type) {
                case SEG_MOVETO:
                    lastStartX = data[0];
                    lastStartY = data[1];
                // fallthrough
                case SEG_LINETO:
                    l.add(data[0]);
                    l.add(data[1]);
                    break;
                case SEG_CLOSE:
                    break;
                case SEG_CUBICTO:
                    assert l.size() > 0;
                    double prevX = l.get(l.size() - 2);
                    double prevY = l.get(l.size() - 1);
                    approximateCubicCurve(prevX, prevY, data[0], data[1], data[2], data[3], data[4], data[5], (nx, ny) -> {
                        l.add(nx);
                        l.add(ny);
                    });
                    break;
                case SEG_QUADTO:
                    assert l.size() > 0;
                    double pX = l.get(l.size() - 2);
                    double pY = l.get(l.size() - 1);
                    approximateQuadraticCurve(pX, pY, data[0], data[1], data[2], data[3], (nx, ny) -> {
                        l.add(nx);
                        l.add(ny);
                    });
            }
            iter.next();
        }
        return new Polygon2D(l.toDoubleArray());
    }

    /**
     * Creates a Polygon2D which approximates a cubic curve by sampling points
     * along it.
     *
     * @param ax The curve's initial point's x coordinate
     * @param ay The curve's initial point's y coordinate
     * @param bx The curve's first control point's x coordinate
     * @param by The curve's first control point's y coordinate
     * @param cx The curve's second control point's x coordinate
     * @param cy The curve's second control point's y coordinate
     * @param dx The curve's destination point's x coordinate
     * @param dy The curve's destination point's xY coordinate
     * @return A polygon
     */
    public static Polygon2D approximateCubicCurve(double ax, double ay,
            double bx, double by,
            double cx, double cy,
            double dx, double dy) {
        double[] points = new double[APPROXIMATION_POSITIONS.length * 2];

        for (int i = 0; i < APPROXIMATION_POSITIONS.length; i++) {
            int arrayOffset = i * 2;
            EqPointDouble pt = cubicPoint(APPROXIMATION_POSITIONS[i], ax, ay, bx, by, cx, cy, dx, dy);
            points[arrayOffset] = pt.x;
            points[arrayOffset + 1] = pt.y;
        }
        return new Polygon2D(points);
    }

    /**
     * Creates a Polygon2D which approximates a cubic curve by sampling points
     * along it.
     *
     * @param ax The curve's initial point's x coordinate
     * @param ay The curve's initial point's y coordinate
     * @param bx The curve's first control point's x coordinate
     * @param by The curve's first control point's y coordinate
     * @param cx The curve's second control point's x coordinate
     * @param cy The curve's second control point's y coordinate
     * @param dx The curve's destination point's x coordinate
     * @param dy The curve's destination point's xY coordinate
     * @param c a consumer
     */
    public static void approximateCubicCurve(double ax, double ay,
            double bx, double by,
            double cx, double cy,
            double dx, double dy, DoubleBiConsumer c) {

        for (int i = 0; i < APPROXIMATION_POSITIONS.length; i++) {
            EqPointDouble pt = cubicPoint(APPROXIMATION_POSITIONS[i], ax, ay, bx, by, cx, cy, dx, dy);
            c.accept(pt.x, pt.y);
        }
    }

    /**
     * Creates a Polygon2D which approximates a cubic curve by sampling points
     * along it.
     *
     * @param ax The curve's initial point's x coordinate
     * @param ay The curve's initial point's y coordinate
     * @param bx The curve's first control point's x coordinate
     * @param by The curve's first control point's y coordinate
     * @param cx The curve's second control point's x coordinate
     * @param cy The curve's second control point's y coordinate
     * @return A polygon
     */
    public static Polygon2D approximateQuadraticCurve(double ax, double ay,
            double bx, double by,
            double cx, double cy) {
        double[] points = new double[APPROXIMATION_POSITIONS.length * 2];

        for (int i = 0; i < APPROXIMATION_POSITIONS.length; i++) {
            int arrayOffset = i * 2;
            EqPointDouble pt = quadraticPoint(APPROXIMATION_POSITIONS[i], ax, ay, bx, by, cx, cy);
            points[arrayOffset] = pt.x;
            points[arrayOffset + 1] = pt.y;
        }
        return new Polygon2D(points);
    }

    /**
     * Creates a Polygon2D which approximates a cubic curve by sampling points
     * along it.
     *
     * @param ax The curve's initial point's x coordinate
     * @param ay The curve's initial point's y coordinate
     * @param bx The curve's first control point's x coordinate
     * @param by The curve's first control point's y coordinate
     * @param cx The curve's second control point's x coordinate
     * @param cy The curve's second control point's y coordinate
     * @return A polygon
     */
    public static void approximateQuadraticCurve(double ax, double ay,
            double bx, double by,
            double cx, double cy, DoubleBiConsumer pointConsumer) {
        for (int i = 0; i < APPROXIMATION_POSITIONS.length; i++) {
            EqPointDouble pt = quadraticPoint(APPROXIMATION_POSITIONS[i], ax, ay, bx, by, cx, cy);
            pointConsumer.accept(pt.x, pt.y);
        }
    }

    /**
     * Compute the position of a point along a quadratic curve at the specified
     * value of t (&gt;=0 and &lt;=1).
     *
     * @param t The fraction of the curve to compute for
     * @param x1 The curve's starting x coordinate
     * @param y1 The curve's starting y coordinate
     * @param x2 The curve's control point's x coordinate
     * @param y2 The curve's control point's y coordinate
     * @param x3 The curve's destination point's x coordinate
     * @param y3 The curve's destination point's y coordinate
     * @return A point
     */
    public static EqPointDouble quadraticPoint(double t,
            double x1, double y1, double x2, double y2, double x3, double y3) {
        double a = pow((1.0 - t), 2.0);
        double b = 2.0 * t * (1.0 - t);
        double c = pow(t, 2.0);
        double x = a * x1 + b * x2 + c * x3;
        double y = a * y1 + b * y2 + c * y3;
        return new EqPointDouble(x, y);
    }

    /**
     * Compute the position of a point along a cubic curve at the specified
     * value of t (&gt;=0 and &lt;=1).
     *
     * @param t The fraction of the curve to compute for
     * @param ax The curve's starting x coordinate
     * @param ay The curve's starting y coordinate
     * @param bx The curve's first control point's x coordinate
     * @param by The curve's first control point's y coordinate
     * @param cx The curve's second control point's x coordinate
     * @param cy The curve's second control point's y coordinate
     * @param d3 The curve's destination point's x coordinate
     * @param d3 The curve's destination point's y coordinate
     * @return A point
     */
    public static EqPointDouble cubicPoint(double t, double ax, double ay,
            double bx, double by,
            double cx, double cy,
            double dx, double dy) {
        return new EqPointDouble(
                cubicPosition(t, ax, bx, cx, dx),
                cubicPosition(t, ay, by, cy, dy)
        );
    }

    /**
     * Get the approximate length of a cubic curve.
     *
     * @param ax The curve's starting x coordinate
     * @param ay The curve's starting y coordinate
     * @param bx The curve's first control point's x coordinate
     * @param by The curve's first control point's y coordinate
     * @param cx The curve's second control point's x coordinate
     * @param cy The curve's second control point's y coordinate
     * @param d3 The curve's destination point's x coordinate
     * @param d3 The curve's destination point's y coordinate
     * @return A length
     */
    public static double cubicSegmentLength(
            double ax, double ay,
            double bx, double by,
            double cx, double cy,
            double dx, double dy) {
        double base = Point2D.distance(ax, ay, dx, dy);
        base += Point2D.distance(bx, by, cx, cy) / 2;
        int samples = Math.max(100, (int) base) / 10;
        return cubicSegmentLength(ax, ay, bx, by, cx, cy, dx, dy, samples);
    }

    /**
     * Get the approximate length of a cubic curve.
     *
     * @param ax The curve's starting x coordinate
     * @param ay The curve's starting y coordinate
     * @param bx The curve's first control point's x coordinate
     * @param by The curve's first control point's y coordinate
     * @param cx The curve's second control point's x coordinate
     * @param cy The curve's second control point's y coordinate
     * @param d3 The curve's destination point's x coordinate
     * @param d3 The curve's destination point's y coordinate
     * @param samples The number of fractions between 0 and 1 at which to sample
     * when approximating
     * @return A length
     */
    public static double cubicSegmentLength(
            double ax, double ay,
            double bx, double by,
            double cx, double cy,
            double dx, double dy,
            int samples
    ) {
        double sampleStep = 1D / samples;
        double dist = 0;
        double lastX = 0, lastY = 0;
        for (double s = 0; s < 1; s += sampleStep) {
            double x = cubicPosition(s, ax, bx, cx, dx);
            double y = cubicPosition(s, ay, by, cy, dy);
            if (s > 0) {
                dist += Point2D.distance(lastX, lastY, x, y);
            }
            lastX = x;
            lastY = y;
        }
        return dist;
    }

    /**
     * Get the position of a coordinate between t=0 and t=1 on a cubic curve
     * (call once with x and y coordinate info to compute a point).
     *
     * @param t The coordinate position along the curve between 0.0 and 1.0 to
     * indicate the distance along the curve
     * @param ax The preceding coordinate
     * @param bx The first control coordinate
     * @param cx The second control coordinate
     * @param dx The destination coordinate
     * @return The coordinate at position t in a quadratic curve
     */
    public static double cubicPosition(double t, double ax, double bx, double cx, double dx) {
        return ((1 - t) * (1 - t) * (1 - t)) * ax
                + 3 * ((1 - t) * (1 - t)) * t * bx
                + 3 * (1 - t) * (t * t) * cx
                + (t * t * t) * dx;
    }

    /**
     * Estimates the length of a quadratic segment.
     *
     * @param ax Preceding point x
     * @param ay Preceding point y
     * @param bx Second control point x
     * @param by Second control point y
     * @param cx Destination point x
     * @param cy Destination point y
     * @return A length
     */
    public static double quadraticSegmentLength2(double ax, double ay, double bx, double by, double cx, double cy) {
        double axSquared = Math.pow(ax, 2);
        double aySquared = Math.pow(ay, 2);
        double bxSquared = Math.pow(bx, 2);
        double bySquared = Math.pow(by, 2);
        double cxSquared = Math.pow(cx, 2);
        double cySquared = Math.pow(cy, 2);

        double result
                = (axSquared
                * (bySquared - 2 * by * cy + cySquared + 2 * ax
                * (cy - by
                * (bx
                * (ay - cy + cx
                * (by - ay))))))
                + (bx
                * (ay - cy + cx
                * (by - ay))) * Math.log(
                        (Math.sqrt(axSquared - 2 * ax * bx + bxSquared + aySquared - 2 * ay * by + bySquared * Math.sqrt(axSquared + 2 * ax
                                * (cx - 2 * bx + 4 * bxSquared - 4 * bx * cx + cxSquared
                                + (ay - 2 * by + cySquared))
                        )
                        ))
                ) + axSquared + ax
                * (cx - 3 * bx + 2 * bxSquared - bx * cx
                + (ay - by
                * (ay - 2 * by + cy)))
                / (Math.sqrt(axSquared + 2 * ax
                        * (cx - 2 * bx + 4 * bxSquared - 4 * bx * cx + cxSquared
                        + (ay - 2 * by + cySquared))
                )) * Math.sqrt(bxSquared - 2 * bx * cx + cxSquared + bySquared - 2 * by * cy + cySquared + ax
                * (bx - cx - 2 * bxSquared + 3 * bx * cx - cxSquared
                + (ay - 2 * by + cy
                * (by - cy)))
        ) / Math.pow(
                (axSquared + 2 * ax
                * (cx - 2 * bx + 4 * bxSquared - 4 * bx * cx + cxSquared
                + (ay - 2 * by + cySquared))),
                (3 / 2
                + (Math.sqrt(axSquared - 2 * ax * bx + bxSquared + aySquared - 2 * ay * by + bySquared
                        * (axSquared + ax
                        * (cx - 3 * bx + 2 * bxSquared - bx * cx
                        + (ay - by
                        * (ay - 2 * by + cy))))
                )))
        ) - Math.sqrt(bxSquared - 2 * bx * cx + cxSquared + bySquared - 2 * by * cy + cySquared
                * (ax
                * (bx - cx - 2 * bxSquared + 3 * bx * cx - cxSquared
                + (ay - 2 * by + cy
                * (by - cy))))
        ) / (axSquared + 2 * ax
                * (cx - 2 * bx + 4 * bxSquared - 4 * bx * cx + cxSquared
                + (ay - 2 * by + cySquared)));
        return result;
    }

    /**
     * Estimates the length of a quadratic segment.
     *
     * @param ax Preceding point x
     * @param ay Preceding point y
     * @param bx Second control point x
     * @param by Second control point y
     * @param cx Destination point x
     * @param cy Destination point y
     * @return A length
     */
    public static double quadraticSegmentLength(double ax, double ay, double bx, double by, double cx, double cy) {
        double vx = 2 * (bx - ax);
        double vy = 2 * (by - ay);
        double wx = cx - 2 * bx + ax;
        double wy = cy - 2 * by + ay;

        double uu = 4 * (wx * wx + wy * wy);

        if (uu < 0.00001) {
            return Math.sqrt((cx - ax) * (cx - ax) + (cy - ay) * (cy - ay));
        }

        double vv = 4 * (vx * wx + vy * wy);
        double ww = vx * vx + vy * vy;

        double t1 = (2 * Math.sqrt(uu * (uu + vv + ww)));
        double t2 = 2 * uu + vv;
        double t3 = vv * vv - 4 * uu * ww;
        double t4 = (2 * Math.sqrt(uu * ww));

        return (float) ((t1 * t2 - t3 * Math.log(t2 + t1) - (vv * t4 - t3 * Math.log(vv + t4))) / (8 * Math.pow(uu, 1.5)));
    }

    /**
     * Round a float to 6 decimal places.
     *
     * @param val A float
     * @return That float, rounded
     */
    public static float roundOff(float val) {
        return roundOff(val, 100000);
    }

    /**
     * Round a double to 6 decimal places.
     *
     * @param val A double
     * @return That double, rounded
     */
    public static float roundOff(double val) {
        return roundOff(val, 100000);
    }

    /**
     * Round a float arbitrarily.
     *
     * @param val The float
     * @param multiplier The multiplier to multiply and then divide by
     * @return That float, rounded
     */
    public static float roundOff(float val, int multiplier) {
        return (float) Math.round(val * multiplier) / multiplier;
    }

    /**
     * Round a double arbitrarily.
     *
     * @param val The double
     * @param multiplier The multiplier to multiply and then divide by
     * @return That double, rounded
     */
    public static float roundOff(double val, int multiplier) {
        return (float) Math.round(val * multiplier) / multiplier;
    }

    /**
     * Get the greatest common divisor of two numbers.
     *
     * @param first The first number
     * @param second The second number
     * @return The greatest common divisor
     */
    public static int greatestCommonDivisor(int first, int second) {
        first = Math.abs(first);
        second = Math.abs(second);
        if (first == 0) {
            return second;
        } else if (second == 0) {
            return first;
        }
        int shiftBy;
        for (shiftBy = 0; ((first | second) & 1) == 0; shiftBy++) {
            first >>= 1;
            second >>= 1;
        }
        while ((first & 1) == 0) {
            first >>= 1;
            if (first == 1) {
                break;
            }
        }
        do {
            while ((second & 1) == 0) {
                second >>= 1;
                if (second == 1) {
                    break;
                }
            }
            if (first > second) {
                int swap = first;
                first = second;
                second = swap;
            }
            second = (second - first);
            if (second == 1) {
                break;
            }
        } while (second != 0);
        return first << shiftBy;
    }

    private GeometryUtils() {
        throw new AssertionError();
    }
}
