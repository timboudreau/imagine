/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.geometry.util;

import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import static java.awt.geom.PathIterator.SEG_CLOSE;
import static java.awt.geom.PathIterator.SEG_CUBICTO;
import static java.awt.geom.PathIterator.SEG_LINETO;
import static java.awt.geom.PathIterator.SEG_MOVETO;
import static java.awt.geom.PathIterator.SEG_QUADTO;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import static java.lang.Double.doubleToLongBits;
import java.text.DecimalFormat;
import org.imagine.geometry.EqPointDouble;

/**
 *
 * @author Tim Boudreau
 */
public class GeometryUtils {

    private static final float DEFAULT_TOLERANCE = 0.0000000000001F;
    static final DecimalFormat FMT = new DecimalFormat("######################0.0#################################");
    // Line2D.intersects produces erroneous values for certain tests where
    // one coordinate of the tested line is an exact match; so we offset the
    // values we test very slightly in our containment test
    public static final double INTERSECTION_FUDGE_FACTOR = 1.0E-12;

    public static String toString(double value) {
        return FMT.format(value);
    }

    public static StringBuilder toString(double... dbls) {
        return toString(new StringBuilder(dbls.length * 8), dbls);
    }

    public static StringBuilder toString(StringBuilder sb, double... dbls) {
        return toString(sb, ", ", dbls);
    }

    public static StringBuilder toString(StringBuilder sb, String delim, double... dbls) {
        for (int i = 0; i < dbls.length; i++) {
            sb.append(FMT.format(dbls[i]));
            if (i != dbls.length) {
                sb.append(delim);
            }
        }
        return sb;
    }

    public static String toString(String delim, double... dbls) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dbls.length; i++) {
            sb.append(FMT.format(dbls[i]));
            if (i != dbls.length) {
                sb.append(delim);
            }
        }
        return sb.toString();
    }

    public static String toString(double a, double b) {
        return toString(", ", a, b);
    }

    public static String toString(String delim, double a, double b) {
        return FMT.format(a) + delim + FMT.format(b);
    }

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

    public static double[] equidistantPoint(double x1, double y1, double x2, double y2) {
        return new double[]{(x1 + x2) / 2D, (y1 + y2) / 2D};
    }

    public static EqPointDouble intersection(double ax, double ay, double bx, double by, double cx, double cy, double dx, double dy) {
        double a1 = by - ay;
        double b1 = ax - bx;
        double c1 = a1 * (ax) + b1 * (ay);

        double a2 = dy - cy;
        double b2 = cx - dx;
        double c2 = a2 * (cx) + b2 * (cy);

        double determinant = a1 * b2 - a2 * b1;

        if (determinant == 0) {
            // The lines are parallel. This is simplified
            // by returning a pair of FLT_MAX
            return null;
        } else {
            double x = (b2 * c1 - b1 * c2) / determinant;
            double y = (a1 * c2 - a2 * c1) / determinant;
            return new EqPointDouble(x, y);
        }
    }

    public static int pointsHashCode(double x, double y) {
        double xx = 0.0 + x;
        double yy = 0.0 + y;
        long hash = doubleToLongBits(xx)
                + 217_645_177 * doubleToLongBits(yy);
        return (int) (hash ^ (hash >> 32));
    }

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

    public static String lineToString(double x1, double y1, double x2, double y2) {
        return "<-" + toString(x1, y1) + "-" + toString(x2, y2) + "->";
    }

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

    public static byte[] intArrayToByteArray(int[] ints) {
        byte[] result = new byte[ints.length];
        for (int i = 0; i < ints.length; i++) {
            arraySizeForType(ints[i]); // to validate
            result[i] = (byte) ints[i];
        }
        return result;
    }

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

    public static boolean containsIntersectingLines(double[] points) {
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

    public static EqPointDouble cubicPoint(double t, double ax, double ay,
            double bx, double by,
            double cx, double cy,
            double dx, double dy) {
        return new EqPointDouble(
                cubicPosition(t, ax, bx, cx, dx),
                cubicPosition(t, ay, by, cy, dy)
        );
    }

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

    private static double cubicPosition(double t, double ax, double bx, double cx, double dx) {
        return ((1 - t) * (1 - t) * (1 - t)) * ax
                + 3 * ((1 - t) * (1 - t)) * t * bx
                + 3 * (1 - t) * (t * t) * cx
                + (t * t * t) * dx;
    }

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

    public static String toString(Rectangle2D r) {
        return toString(r.getX(), r.getY()) + " "
                + toString(" x ", r.getWidth(), r.getHeight());
    }

    private GeometryUtils() {
        throw new AssertionError();
    }
}
