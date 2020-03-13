package org.imagine.geometry;

import com.mastfrog.util.collections.CollectionUtils;
import static com.mastfrog.util.collections.CollectionUtils.toIterable;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Tim Boudreau
 */
public class TriangleDouble implements Shape, Iterable<Point2D.Double>, Comparable<TriangleDouble> {

    EqPointDouble[] points;

    public TriangleDouble(Point2D a, Point2D b, Point2D c) {
        this(a.getX(), a.getY(), b.getX(), b.getY(), c.getX(), c.getY());
    }

    public TriangleDouble(double ax, double ay, double bx, double by, double cx, double cy) {
        EqPointDouble a = new EqPointDouble(ax, ay);
        EqPointDouble b = new EqPointDouble(bx, by);
        EqPointDouble c = new EqPointDouble(cx, cy);
        points = new EqPointDouble[]{a, b, c};
    }

    public TriangleDouble(float ax, float ay, float bx, float by, float cx, float cy) {
        EqPointDouble a = new EqPointDouble(ax, ay);
        EqPointDouble b = new EqPointDouble(bx, by);
        EqPointDouble c = new EqPointDouble(cx, cy);
        points = new EqPointDouble[]{a, b, c};
    }

    public TriangleDouble(TriangleDouble other) {
        points = new EqPointDouble[3];
        for (int i = 0; i < 3; i++) {
            points[i] = new EqPointDouble(other.points[i]);
        }
    }

    public static TriangleDouble isoceles(double centerX, double centerY, double distanceToCenter) {
        Circle circ = new Circle(centerX, centerY, distanceToCenter);
        return new TriangleDouble(
                circ.getPosition(0),
                circ.getPosition(135),
                circ.getPosition(225)
        );
    }

    public void setPoints(double ax, double ay, double bx, double by, double cx, double cy) {
        points[0].x = ax;
        points[0].y = ay;
        points[1].x = bx;
        points[1].y = by;
        points[2].x = cx;
        points[2].y = cy;
    }

    public void setPoints(float ax, float ay, float bx, float by, float cx, float cy) {
        points[0].x = ax;
        points[0].y = ay;
        points[1].x = bx;
        points[1].y = by;
        points[2].x = cx;
        points[2].y = cy;
    }

    public static TriangleDouble isoceles(Point2D center, double size) {
        return isoceles(center.getX(), center.getY(), size);
    }

    public TriangleDouble adjustedBy(float oax, float oay, float obx, float oby, float ocx, float ocy) {
        return new TriangleDouble(points[0].x + oax, points[0].y + oay,
                points[1].x + obx, points[1].y + oby,
                points[2].x + ocx, points[2].y + ocy);
    }

    public TriangleDouble adjustedBy(double oax, double oay, double obx, double oby, double ocx, double ocy) {
        return new TriangleDouble(points[0].x + oax, points[0].y + oay,
                points[1].x + obx, points[1].y + oby,
                points[2].x + ocx, points[2].y + ocy);
    }

    public void rotate(double degrees) {
        applyTransform(AffineTransform.getRotateInstance(Math.toRadians(degrees)));
    }

    public void applyTransform(AffineTransform xform) {
        for (Point2D.Double p : points) {
            xform.transform(p, p);
        }
    }

    public double ax() {
        return points[0].x;
    }

    public double bx() {
        return points[1].x;
    }

    public double cx() {
        return points[2].x;
    }

    public double ay() {
        return points[0].y;
    }

    public double by() {
        return points[1].y;
    }

    public double cy() {
        return points[2].y;
    }

    public Circle toCircle(int point) {
        assert point >= 0 && point < 3;
        Point2D.Double center = points[point];
        Circle circle = new Circle(center.getX(), center.getY());
        Point2D.Double[] others = new Point2D.Double[2];
        int ix = 0;
        for (int i = 0; i < points.length; i++) {
            if (i == point) {
                continue;
            }
            others[ix++] = points[i];
        }
        double len1 = others[0].distance(center);
        double len2 = others[1].distance(center);
        circle.setRadius(Math.max(len1, len2));
        return circle;
    }

    @Override
    public boolean equals(Object o) {
        return o == this ? true : o == null ? false
                : o instanceof TriangleDouble
                        ? Arrays.equals(points, ((TriangleDouble) o).points) : false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(points);
    }

    public TriangleDouble normalize() {
        return new TriangleDouble(round(points[0]), round(points[1]), round(points[2]));
    }

    private static Point2D.Double round(Point2D.Double point) {
        return new EqPointDouble(Math.round(point.x), Math.round(point.y));
    }

    public double maxX() {
        return Math.max(points[0].x, Math.max(points[1].x, points[2].x));
    }

    public double maxY() {
        return Math.max(points[0].y, Math.max(points[1].y, points[2].y));
    }

    public double minX() {
        return Math.min(points[0].x, Math.min(points[1].x, points[2].x));
    }

    public double minY() {
        return Math.min(points[0].y, Math.min(points[1].y, points[2].y));
    }

    private double length(int side) {
        Point2D.Double a, b;
        switch (side) {
            case 0:
                a = points[0];
                b = points[1];
                break;
            case 1:
                a = points[1];
                b = points[2];
                break;
            case 2:
                a = points[2];
                b = points[0];
                break;
            default:
                throw new AssertionError("" + side);
        }
        if (a.x == b.x) {
            return Math.abs(a.y - b.y);
        } else if (a.y == b.y) {
            return Math.abs(a.x - b.x);
        }
        return Math.abs(a.distance(b));
    }

    public Point2D.Double oppositeCorner(int side) {
        switch (side) {
            case 0:
                return points[2];
            case 1:
                return points[0];
            case 2:
                return points[1];
            default:
                throw new AssertionError("" + side);
        }
    }

    public Line2D.Double side(int side) {
        Point2D.Double a, b;
        switch (side) {
            case 0:
                a = points[0];
                b = points[1];
                break;
            case 1:
                a = points[1];
                b = points[2];
                break;
            case 2:
                a = points[2];
                b = points[0];
                break;
            default:
                throw new AssertionError("" + side);
        }
        return new Line2D.Double(a, b);
    }

    public boolean isEmpty() {
        return points[0].equals(points[1]) || points[1].equals(points[2]) || points[2].equals(points[0]);
    }

    public Point2D center() {
        double x = (points[0].x + points[1].x + points[2].x) / 3;
        double y = (points[0].y + points[1].y + points[2].y) / 3;
        return new EqPointDouble(x, y);
    }

    public Point2D.Double topMostPoint() {
        int ix = -1;
        double min = Double.MAX_VALUE;
        for (int i = 0; i < points.length; i++) {
            if (points[i].y < min) {
                ix = i;
                min = points[i].y;
            }
        }
        return points[ix];
    }

    public Point2D.Double bottomMostPoint() {
        int ix = -1;
        double max = Double.MIN_VALUE;
        for (int i = 0; i < points.length; i++) {
            if (points[i].y > max) {
                ix = i;
                max = points[i].y;
            }
        }
        return points[ix];
    }

    public Point2D.Double leftMostPoint() {
        int index = -1;
        double min = Double.MAX_VALUE;
        for (int i = 0; i < points.length; i++) {
            if (points[i].x < min) {
                index = i;
                min = points[i].x;
            }
        }
        return points[index];
    }

    public Point2D.Double rightMostPoint() {
        int index = -1;
        double max = Double.MIN_VALUE;
        for (int i = 0; i < points.length; i++) {
            if (points[i].x > max) {
                index = i;
                max = points[i].x;
            }
        }
        return points[index];
    }

    public int longestSide() {
        double[] d = new double[]{length(0), length(1), length(2)};
        double val = 0;
        int ix = 0;
        for (int i = 0; i < 3; i++) {
            if (d[i] > val) {
                ix = i;
            }
            val = Math.max(val, d[i]);
        }
        return ix;
    }

    public TriangleDouble[] tesselate() {
        return tesselate(longestSide());
    }

    public int sharedPoints(TriangleDouble other) {
        int matchingPoints = 0;
        for (int i = 0; i < 3; i++) {
            Point2D.Double p = points[i];
            if (other.points[0].equals(p)) {
                matchingPoints++;
            }
            if (other.points[1].equals(p)) {
                matchingPoints++;
            }
            if (other.points[2].equals(p)) {
                matchingPoints++;
            }
        }
        return matchingPoints;
    }

    public boolean sharesCorner(TriangleDouble other) {
        return sharedPoints(other) == 1;
    }

    public boolean isAdjacent(TriangleDouble other) {
        return sharedPoints(other) == 2;
    }

    public boolean containsCorner(Point2D pt) {
        if (!(pt instanceof EqPointDouble)) {
            pt = new EqPointDouble(pt);
        }
        return points[0].equals(pt) || points[1].equals(pt) || points[2].equals(pt);
    }

    public TriangleDouble[] tesselate(int side) {
        Line2D.Double l = side(side);

        double len = length(side);
        double ratio = (len / 2) / len;
        double x = ratio * l.x2 + (1.0 - ratio) * l.x1;
        double y = ratio * l.y2 + (1.0 - ratio) * l.y1;

        Point2D.Double a = oppositeCorner(side);

        TriangleDouble[] result = new TriangleDouble[2];
        result[0] = new TriangleDouble(l.x1, l.y1, x, y, a.x, a.y);
        result[1] = new TriangleDouble(l.x2, l.y2, x, y, a.x, a.y);

        return result;
    }

    public TriangleDouble[] tesselateAndReplace(int side, List<TriangleDouble> triangles) {
        return tesselateAndReplace(side, triangles, triangles.indexOf(this));
    }

    public TriangleDouble[] tesselateAndReplace(List<TriangleDouble> triangles) {
        return tesselateAndReplace(triangles, triangles.indexOf(this));
    }

    public TriangleDouble[] tesselateAndReplace(List<TriangleDouble> triangles, int index) {
        return tesselateAndReplace(longestSide(), triangles, index);
    }

    public TriangleDouble[] tesselateAndReplace(int side, List<TriangleDouble> triangles, int index) {
        assert index >= 0;
        TriangleDouble[] result = tesselate(side);
        triangles.remove(index);
        triangles.add(index, result[1]);
        triangles.add(index, result[0]);
        return result;
    }

    @Override
    public String toString() {
        return "Tri"
                + "<" + points[0].getX() + "," + points[0].getY() + ">"
                + "<" + points[1].getX() + "," + points[1].getY() + ">"
                + "<" + points[2].getX() + "," + points[2].getY() + ">";
    }

    public static TriangleDouble[] fromRectangle(Rectangle rect) {
        double midX = ((double) rect.x + ((double) rect.width / 2D));
        TriangleDouble a = new TriangleDouble(rect.x, rect.y, rect.x + midX, rect.y, rect.x, rect.y + rect.height);
        TriangleDouble b = new TriangleDouble((float) rect.x + midX, rect.y, rect.x + rect.width, rect.y + rect.height, rect.x, rect.y + rect.height);
        TriangleDouble c = new TriangleDouble((float) rect.x + midX, rect.y, rect.width + rect.x, rect.y, rect.x + rect.width, rect.y + rect.height);
        return new TriangleDouble[]{a, b, c};
    }

    @Override
    public Rectangle getBounds() {
        return getBounds2D().getBounds();
    }

    @Override
    public Rectangle2D getBounds2D() {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;
        for (Point2D.Double p : points) {
            minX = Math.min(p.x, minX);
            minY = Math.min(p.y, minY);
            maxX = Math.max(p.x, maxX);
            maxY = Math.max(p.y, maxY);
        }
        return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
    }

    @Override
    public boolean contains(double x, double y) {
        double alpha = ((points[1].y - points[2].y) * (x - points[2].x) + (points[2].x - points[1].x) * (y - points[2].y)) / ((points[1].y - points[2].y) * (points[0].x - points[2].x) + (points[2].x - points[1].x) * (points[0].y - points[2].y));
        double beta = ((points[2].y - points[0].y) * (x - points[2].x) + (points[0].x - points[2].x) * (y - points[2].y)) / ((points[1].y - points[2].y) * (points[0].x - points[2].x) + (points[2].x - points[1].x) * (points[0].y - points[2].y));
        double gamma = 1.0 - alpha - beta;
        return alpha > 0 && beta > 0 && gamma > 0;
    }

    @Override
    public boolean contains(Point2D p) {
        return contains(p.getX(), p.getY());
    }

    @Override
    public boolean intersects(double x, double y, double w, double h) {
        return contains(x, y) || contains(x + w, y + h) || contains(x, y + h) || contains(x + w, y);
    }

    @Override
    public boolean intersects(Rectangle2D r) {
        return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
        return contains(x, y) && contains(x + w, y + h) && contains(x, y + h) && contains(x + w, y);
    }

    @Override
    public boolean contains(Rectangle2D r) {
        return contains(r.getCenterX(), r.getCenterY()) && contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        return new PI(at);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return getPathIterator(at);
    }

    @Override
    public Iterator<Point2D.Double> iterator() {
        return CollectionUtils.toIterator(points);
    }

    public Iterable<Line2D.Double> lines() {
        Line2D.Double[] result = new Line2D.Double[]{side(0), side(1), side(2)};
        return toIterable(result);
    }

    int indexOfPoint(Point2D p) {
        return points[0].equals(p) ? 0 : points[1].equals(p) ? 1 : points[2].equals(p) ? 2 : -1;
    }

    @Override
    public int compareTo(TriangleDouble o) {
        int sharedPoints = this.sharedPoints(o);
        if (sharedPoints == 3) {
            return 0;
        }
//        if (sharedPoints > 0) {
        double mx = minX();
        double omx = o.minX();
        if (mx > omx) {
            return 1;
        } else if (mx < omx) {
            return -1;
        } else {
            double my = minY();
            double omy = o.minY();
            return my == omy ? 0 : my > omy ? 1 : -1;
        }
//        }
    }

    public Point2D.Double[] sortedByDistanceTo(Point2D pt) {
        Point2D.Double[] result = new Point2D.Double[]{points[0], points[1], points[2]};
        Arrays.sort(result, (pa, pb) -> {
            double da = pa.distance(pt);
            double db = pb.distance(pt);
            return da > db ? 1 : da == db ? 0 : -1;
        });
        return result;
    }

    final class PI implements PathIterator {

        int index = 0;
        private final AffineTransform xform;

        PI(AffineTransform xform) {
            this.xform = xform;
        }

        @Override
        public int getWindingRule() {
            return PathIterator.WIND_NON_ZERO;
        }

        @Override
        public boolean isDone() {
            return index > 3;
        }

        @Override
        public void next() {
            index++;
        }

        @Override
        public int currentSegment(float[] coords) {
            if (index == 3) {
                return SEG_CLOSE;
            }
            coords[0] = (float) points[index].x;
            coords[1] = (float) points[index].y;
            if (xform != null) {
                xform.transform(coords, 0, coords, 0, 1);
            }
            return index == 0 ? SEG_MOVETO : index == 1 ? SEG_LINETO : SEG_LINETO;
        }

        @Override
        public int currentSegment(double[] coords) {
            if (index == 3) {
                return SEG_CLOSE;
            }
            coords[0] = points[index].x;
            coords[1] = points[index].y;
            if (xform != null) {
                xform.transform(coords, 0, coords, 0, 1);
            }
            return index == 0 ? SEG_MOVETO : index == 1 ? SEG_LINETO : SEG_LINETO;
        }
    }

    static final DecimalFormat fmt = new DecimalFormat("#######0.00#############################################");
}
