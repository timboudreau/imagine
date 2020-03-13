package org.imagine.geometry;

import com.mastfrog.function.IntBiConsumer;
import com.mastfrog.function.IntBiPredicate;
import com.mastfrog.util.collections.CollectionUtils;
import static com.mastfrog.util.collections.CollectionUtils.supplierMap;
import static com.mastfrog.util.collections.CollectionUtils.toIterable;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Tim Boudreau
 */
public class Triangle implements Shape, Iterable<Point2D.Float>, Comparable<Triangle> {

    Point2D.Float[] points;

    public Triangle(Point2D a, Point2D b, Point2D c) {
        this(a.getX(), a.getY(), b.getX(), b.getY(), c.getX(), c.getY());
    }

    public Triangle(Point2D.Float a, Point2D.Float b, Point2D.Float c) {
        this(a.x, a.y, b.x, b.y, c.x, c.y);
    }

    public Triangle(double ax, double ay, double bx, double by, double cx, double cy) {
        this((float) ax, (float) ay, (float) bx, (float) by, (float) cx, (float) cy);
    }

    public Triangle(float ax, float ay, float bx, float by, float cx, float cy) {
        Point2D.Float a = new EqPoint(ax, ay);
        Point2D.Float b = new EqPoint(bx, by);
        Point2D.Float c = new EqPoint(cx, cy);
        points = new Point2D.Float[]{a, b, c};
    }

    public static Triangle isoceles(double centerX, double centerY, double distanceToCenter) {
        Circle circ = new Circle(centerX, centerY, distanceToCenter);
        return new Triangle(
                circ.getPosition(0),
                circ.getPosition(135),
                circ.getPosition(225)
        );
    }

    public void setPoints(double ax, double ay, double bx, double by, double cx, double cy) {
        points[0].x = (float) ax;
        points[0].y = (float) ay;
        points[1].x = (float) bx;
        points[1].y = (float) by;
        points[2].x = (float) cx;
        points[2].y = (float) cy;
    }

    public void setPoints(float ax, float ay, float bx, float by, float cx, float cy) {
        points[0].x = ax;
        points[0].y = ay;
        points[1].x = bx;
        points[1].y = by;
        points[2].x = cx;
        points[2].y = cy;
    }

    public static Triangle isoceles(Point2D center, double size) {
        return isoceles(center.getX(), center.getY(), size);
    }

    public Triangle adjustedBy(float oax, float oay, float obx, float oby, float ocx, float ocy) {
        return new Triangle(points[0].x + oax, points[0].y + oay,
                points[1].x + obx, points[1].y + oby,
                points[2].x + ocx, points[2].y + ocy);
    }

    public Triangle adjustedBy(double oax, double oay, double obx, double oby, double ocx, double ocy) {
        return new Triangle(points[0].x + oax, points[0].y + oay,
                points[1].x + obx, points[1].y + oby,
                points[2].x + ocx, points[2].y + ocy);
    }

    public void rotate(double degrees) {
        applyTransform(AffineTransform.getRotateInstance(Math.toRadians(degrees)));
    }

    public void applyTransform(AffineTransform xform) {
        for (Point2D.Float p : points) {
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
        Point2D.Float center = points[point];
        Circle circle = new Circle(center.getX(), center.getY());
        Point2D.Float[] others = new Point2D.Float[2];
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
                : o instanceof Triangle
                        ? Arrays.equals(points, ((Triangle) o).points) : false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(points);
    }

    public Triangle normalize() {
        return new Triangle(round(points[0]), round(points[1]), round(points[2]));
    }

    private static Point2D.Float round(Point2D.Float point) {
        return new EqPoint(Math.round(point.x), Math.round(point.y));
    }

    public float maxX() {
        return Math.max(points[0].x, Math.max(points[1].x, points[2].x));
    }

    public float maxY() {
        return Math.max(points[0].y, Math.max(points[1].y, points[2].y));
    }

    public float minX() {
        return Math.min(points[0].x, Math.min(points[1].x, points[2].x));
    }

    public float minY() {
        return Math.min(points[0].y, Math.min(points[1].y, points[2].y));
    }

    private double length(int side) {
        Point2D.Float a, b;
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

    public Point2D.Float oppositeCorner(int side) {
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

    public Line2D.Float side(int side) {
        Point2D.Float a, b;
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
        return new Line2D.Float(a, b);
    }

    void neighborsOf(int x, int y, IntBiConsumer v) {
        v.accept(x - 1, y - 1);
        v.accept(x, y - 1);
        v.accept(x - 1, y);

        v.accept(x + 1, y + 1);
        v.accept(x + 1, y);
        v.accept(x, y + 1);

        v.accept(x - 1, y + 1);
        v.accept(x + 1, y - 1);
    }

    public boolean isEmpty() {
        return points[0].equals(points[1]) || points[1].equals(points[2]) || points[2].equals(points[0]);
    }

//    private int visitNeighborsIfContained(int x, int y, InteriorVisitor visitor, IntSet is, int width) {
//        int count[] = new int[1];
//        System.out.println("vnic " + x + "," + y);
//        List<int[]> l = new ArrayList<>();
//        neighborsOf(x, y, (x1, y1) -> {
//            int pos = (x1 * width) + y1;
//            System.out.println("check " + x1 + "," + y1 + " = " + pos);
//            if (!is.contains(pos)) {
//                if (contains(x1, y1)) {
//                    System.out.println("MATCH " + x1 + "," + y1);
//                    count[0]++;
//                    visitor.visit(x1, y1);
//                    is.add(pos);
//                    l.add(new int[] { x1, y1 });
////                    count[0] += visitNeighborsIfContained(x1, y1, visitor, is, width);
//                } else {
//                    System.out.println("Not contained");
//                    is.add(pos);
//                }
//            }
//            return true;
//        });
//        for (int[] i : l) {
//            if (i[0] != x && i[0] != y) {
//                count[0] += visitNeighborsIfContained(i[0], i[1], visitor, is, width);
//            }
//        }
//        return count[0];
//    }
    public int visitInterior(IntBiPredicate visitor) {
//        int x = (int) points[1].x;
//        int y = (int) points[1].y;
//        Rectangle r = getBounds();
//        visitor.visit(x, y);
//        final IntSet is = new IntSet(r.width * r.height);
//        is.add((x * r.width) + y);
//        int count = visitNeighborsIfContained(x, y, visitor, is, r.width);

        // XXX hideously inefficient
        Rectangle bds = getBounds();
        int visited = 0;
        for (int i = bds.x; i < bds.x + bds.width; i++) {
            for (int j = bds.y; j < bds.y + bds.height; j++) {
                if (contains(i, j)) {
                    visited++;
                    if (!visitor.test(i, j)) {
                        break;
                    }
                }
            }
        }
        return visited;
    }

    public Point2D.Float center() {
        float x = (points[0].x + points[1].x + points[2].x) / 3;
        float y = (points[0].y + points[1].y + points[2].y) / 3;
        return new EqPoint((int) Math.floor(x), (int) Math.floor(y));
    }

    public Point2D.Float topMostPoint() {
        int ix = -1;
        float min = Float.MAX_VALUE;
        for (int i = 0; i < points.length; i++) {
            if (points[i].y < min) {
                ix = i;
                min = points[i].y;
            }
        }
        return points[ix];
    }

    public Point2D.Float bottomMostPoint() {
        int ix = -1;
        float max = Float.MIN_VALUE;
        for (int i = 0; i < points.length; i++) {
            if (points[i].y > max) {
                ix = i;
                max = points[i].y;
            }
        }
        return points[ix];
    }

    public Point2D.Float leftMostPoint() {
        int index = -1;
        float min = Float.MAX_VALUE;
        for (int i = 0; i < points.length; i++) {
            if (points[i].x < min) {
                index = i;
                min = points[i].x;
            }
        }
        return points[index];
    }

    public Point2D.Float rightMostPoint() {
        int index = -1;
        float max = Float.MIN_VALUE;
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

    public Triangle[] tesselate() {
        return tesselate(longestSide());
    }

    public int sharedPoints(Triangle other) {
        int matchingPoints = 0;
        for (int i = 0; i < 3; i++) {
            Point2D.Float p = points[i];
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

    public boolean sharesCorner(Triangle other) {
        return sharedPoints(other) == 1;
    }

    public boolean isAdjacent(Triangle other) {
        return sharedPoints(other) == 2;
    }

    public boolean containsCorner(Point2D.Float pt) {
        if (!(pt instanceof EqPoint)) {
            pt = new EqPoint(pt);
        }
        return points[0].equals(pt) || points[1].equals(pt) || points[2].equals(pt);
    }

    public Triangle[] tesselate(int side) {
        Line2D.Float l = side(side);

        double len = length(side);
        double ratio = (len / 2) / len;
        double x = ratio * l.x2 + (1.0 - ratio) * l.x1;
        double y = ratio * l.y2 + (1.0 - ratio) * l.y1;

        Point2D.Float a = oppositeCorner(side);

        Triangle[] result = new Triangle[2];
        result[0] = new Triangle(l.x1, l.y1, (float) x, (float) y, a.x, a.y);
        result[1] = new Triangle(l.x2, l.y2, (float) x, (float) y, a.x, a.y);

        return result;
    }

    public Triangle[] tesselateAndReplace(int side, List<Triangle> triangles) {
        return tesselateAndReplace(side, triangles, triangles.indexOf(this));
    }

    public Triangle[] tesselateAndReplace(List<Triangle> triangles) {
        return tesselateAndReplace(triangles, triangles.indexOf(this));
    }

    public Triangle[] tesselateAndReplace(List<Triangle> triangles, int index) {
        return tesselateAndReplace(longestSide(), triangles, index);
    }

    public Triangle[] tesselateAndReplace(int side, List<Triangle> triangles, int index) {
        assert index >= 0;
        Triangle[] result = tesselate(side);
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

    public static Triangle[] fromRectangle(Rectangle rect) {
        float midX = ((float) rect.x + ((float) rect.width / 2f));
        Triangle a = new Triangle(rect.x, rect.y, rect.x + midX, rect.y, rect.x, rect.y + rect.height);
        Triangle b = new Triangle((float) rect.x + midX, rect.y, rect.x + rect.width, rect.y + rect.height, rect.x, rect.y + rect.height);
        Triangle c = new Triangle((float) rect.x + midX, rect.y, rect.width + rect.x, rect.y, rect.x + rect.width, rect.y + rect.height);
        return new Triangle[]{a, b, c};
    }

    @Override
    public Rectangle getBounds() {
        return getBounds2D().getBounds();
    }

    @Override
    public Rectangle2D getBounds2D() {
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;
        for (Point2D.Float p : points) {
            minX = Math.min(p.x, minX);
            minY = Math.min(p.y, minY);
            maxX = Math.max(p.x, maxX);
            maxY = Math.max(p.y, maxY);
        }
        return new Rectangle2D.Float(minX, minY, maxX - minX, maxY - minY);
    }

    @Override
    public boolean contains(double x, double y) {
        double alpha = ((points[1].y - points[2].y) * (x - points[2].x) + (points[2].x - points[1].x) * (y - points[2].y)) / ((points[1].y - points[2].y) * (points[0].x - points[2].x) + (points[2].x - points[1].x) * (points[0].y - points[2].y));
        double beta = ((points[2].y - points[0].y) * (x - points[2].x) + (points[0].x - points[2].x) * (y - points[2].y)) / ((points[1].y - points[2].y) * (points[0].x - points[2].x) + (points[2].x - points[1].x) * (points[0].y - points[2].y));
        double gamma = 1.0f - alpha - beta;
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
        PathIterator iter = new PI(at);
        return iter;
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return getPathIterator(at);
    }

    @Override
    public Iterator<Point2D.Float> iterator() {
        return CollectionUtils.toIterator(points);
    }

    public Iterable<Line2D.Float> lines() {
        Line2D.Float[] result = new Line2D.Float[]{side(0), side(1), side(2)};
        return toIterable(result);
    }

    int indexOfPoint(Point2D.Float p) {
        return points[0].equals(p) ? 0 : points[1].equals(p) ? 1 : points[2].equals(p) ? 2 : -1;
    }

    boolean shiftPoint(int ix, float xoff, float yoff) {
        if (ix != -1) {
            points[ix].x += xoff;
            points[ix].y += yoff;
            return true;
        }
        return false;
    }

    public static int shiftPoint(float xoff, float yoff, Point2D.Float orig, Map<Point2D.Float, Set<Triangle>> m) {
        Set<Triangle> s = m.get(orig);
        int count = 0;
        for (Triangle t : s) {
            int ix = t.indexOfPoint(orig);
            if (ix == -1) {
                System.out.println("No index of " + orig + " in " + t);
            }
            if (t.shiftPoint(ix, xoff, yoff)) {
                count++;
            }
        }
        m.remove(orig);
        m.put(new EqPoint(orig.x + xoff, orig.y + xoff), s);
        return count;
    }

    public static int shiftPoint2(float xoff, float yoff, Point2D.Float orig, Map<Point2D.Float, Set<TriangleAndPointIndex>> m) {
        Set<TriangleAndPointIndex> s = m.get(orig);
        int count = 0;
        for (TriangleAndPointIndex t : s) {
            int ix = t.pointIndex;
            if (t.triangle.shiftPoint(ix, xoff, yoff)) {
                count++;
            }
        }
        m.remove(orig);
        m.put(new EqPoint(orig.x + xoff, orig.y + xoff), s);
        return count;
    }

    public boolean movePoint(int ix, Point2D.Float nue) {
        for (int i = 0; i < 3; i++) {
            Point2D.Float p = points[i];
            if (p.equals(nue)) {
                return false;
            }
        }
        points[ix].x = nue.x;
        points[ix].y = nue.y;
        return true;
    }

    public static int movePoint(Point2D.Float orig, Point2D.Float dest, Map<Point2D.Float, Set<TriangleAndPointIndex>> m) {
        Set<TriangleAndPointIndex> s = m.get(orig);
        int count = 0;
        for (TriangleAndPointIndex t : s) {
            int ix = t.pointIndex;
            if (t.triangle.movePoint(ix, dest)) {
                count++;
            }
        }
        m.remove(orig);
        m.put(new EqPoint(dest.x, dest.y), s);
        return count;
    }

    public static Map<Point2D.Float, Set<Triangle>> pointMap(Collection<Triangle> triangles) {
        Map<Point2D.Float, Set<Triangle>> result = supplierMap(HashSet<Triangle>::new);
        for (Triangle t : triangles) {
            for (Point2D.Float p : t) {
                result.get(new EqPoint(p)).add(t);
            }
        }
        return result;
    }

    public static Map<Point2D.Float, Set<TriangleAndPointIndex>> pointMap2(Collection<Triangle> triangles) {
        Map<Point2D.Float, Set<TriangleAndPointIndex>> result = supplierMap(HashSet<TriangleAndPointIndex>::new);
        for (Triangle t : triangles) {
            for (int i = 0; i < 3; i++) {
                Point2D.Float p = t.points[i];
                result.get(new EqPoint(p)).add(new TriangleAndPointIndex(t, i));
            }
        }
        return result;
    }

    public static final class TriangleAndPointIndex {

        public final Triangle triangle;
        public final int pointIndex;

        public TriangleAndPointIndex(Triangle triangle, int pointIndex) {
            this.triangle = triangle;
            this.pointIndex = pointIndex;
        }
    }

    @Override
    public int compareTo(Triangle o) {
        int sharedPoints = this.sharedPoints(o);
        if (sharedPoints == 3) {
            return 0;
        }
//        if (sharedPoints > 0) {
        float mx = minX();
        float omx = o.minX();
        if (mx > omx) {
            return 1;
        } else if (mx < omx) {
            return -1;
        } else {
            float my = minY();
            float omy = o.minY();
            return my == omy ? 0 : my > omy ? 1 : -1;
        }
//        }
    }

    public Point2D.Float[] sortedByDistanceTo(Point2D.Float pt) {
        Point2D.Float[] result = new Point2D.Float[]{points[0], points[1], points[2]};
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
            coords[0] = points[index].x;
            coords[1] = points[index].y;
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
}
