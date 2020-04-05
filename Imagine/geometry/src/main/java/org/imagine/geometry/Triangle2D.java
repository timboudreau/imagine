package org.imagine.geometry;

import com.mastfrog.function.DoubleBiConsumer;
import com.mastfrog.function.DoubleQuadConsumer;
import com.mastfrog.function.DoubleSextaConsumer;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import static java.awt.geom.PathIterator.SEG_CLOSE;
import static java.awt.geom.PathIterator.SEG_LINETO;
import static java.awt.geom.PathIterator.SEG_MOVETO;
import static java.awt.geom.PathIterator.WIND_NON_ZERO;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.imagine.geometry.util.GeometryStrings;
import org.imagine.geometry.util.GeometryUtils;

/**
 * A triangle.
 *
 * @author Tim Boudreau
 */
public class Triangle2D extends AbstractShape implements EnhancedShape, Tesselable {

    private double[] points;

    public Triangle2D() {
        points = new double[6];
    }

    public Triangle2D(double ax, double ay, double bx, double by, double cx, double cy) {
        this.points = new double[]{ax, ay, bx, by, cx, cy};
    }

    public Triangle2D(Point2D a, Point2D b, Point2D c) {
        this(a.getX(), a.getY(), b.getX(), b.getY(), c.getX(), c.getY());
    }

    public Triangle2D(double[] points) {
        if (points.length != 6) {
            throw new IllegalArgumentException("Wrong number of coordinates: " + points.length);
        }
        this.points = points;
    }

    public Triangle2D(Triangle2D other) {
        this.points = Arrays.copyOf(other.points, other.points.length);
    }

    @Override
    public <T extends Rectangle2D> T addToBounds(T into) {
        double minX = Math.min(points[0], Math.min(points[2], points[4]));
        double maxX = Math.max(points[0], Math.max(points[2], points[4]));
        double minY = Math.min(points[1], Math.min(points[3], points[5]));
        double maxY = Math.max(points[1], Math.max(points[3], points[5]));
        if (into.isEmpty()) {
            into.setFrameFromDiagonal(minX, minY, maxX, maxY);
        } else {
            into.add(minX, minY);
            into.add(maxX, maxY);
        }
        return into;
    }

    @Override
    public boolean contains(double x, double y) {
        double alpha = ((points[3] - points[5]) * (x - points[4])
                + (points[4] - points[2]) * (y - points[5])) / ((points[3] - points[5]) * (points[0] - points[4])
                + (points[4] - points[2]) * (points[1] - points[5]));
        double beta = ((points[5] - points[1]) * (x - points[4])
                + (points[0] - points[4]) * (y - points[5])) / ((points[3] - points[5]) * (points[0] - points[4])
                + (points[4] - points[2]) * (points[1] - points[5]));
        double gamma = 1.0f - alpha - beta;
        return alpha > 0 && beta > 0 && gamma > 0;
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        byte[] types = new byte[]{SEG_MOVETO, SEG_LINETO, SEG_LINETO, SEG_CLOSE};
        return new ArrayPathIteratorDouble(WIND_NON_ZERO, types, points, at);
    }

    @Override
    public Point2D point(int index) {
        switch (index) {
            case 0:
                return new EqPointDouble(points[0], points[1]);
            case 1:
                return new EqPointDouble(points[2], points[3]);
            case 2:
                return new EqPointDouble(points[4], points[5]);
            default:
                throw new IndexOutOfBoundsException("" + index);
        }
    }

    @Override
    public void visitPoints(DoubleBiConsumer consumer) {
        consumer.accept(points[0], points[1]);
        consumer.accept(points[2], points[3]);
        consumer.accept(points[4], points[5]);
    }

    @Override
    public int pointCount() {
        return 3;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof Triangle2D) {
            Triangle2D t = (Triangle2D) o;
            return Arrays.equals(points, t.points);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(points);
    }

    public int sharedPoints(Triangle2D other) {
        int matchingPoints = 0;
        for (int i = 0; i < 6; i += 2) {
            double ax = points[i];
            double ay = points[i + 1];
            for (int j = 0; j < 6; j += 2) {
                double bx = other.points[j];
                double by = other.points[j + 1];
                if (GeometryUtils.isSamePoint(ax, ay, bx, by)) {
                    matchingPoints++;
                }
            }
        }
        return matchingPoints;
    }

    public boolean isHomomorphic(Triangle2D other) {
        return sharedPoints(other) == 3;
    }

    public double length(int side) {
        double x1, y1, x2, y2;
        switch (side) {
            case 0:
                x1 = points[0];
                y1 = points[1];
                x2 = points[2];
                y2 = points[3];
                break;
            case 1:
                x1 = points[2];
                y1 = points[3];
                x2 = points[4];
                y2 = points[5];
                break;
            case 2:
                x1 = points[4];
                y1 = points[5];
                x2 = points[0];
                y2 = points[1];
                break;
            default:
                throw new IndexOutOfBoundsException("" + side);
        }
        return Point2D.distance(x1, y1, x2, y2);
    }

    public EqPointDouble oppositeCorner(int side) {
        switch (side) {
            case 0:
                return new EqPointDouble(points[4], points[5]);
            case 1:
                return new EqPointDouble(points[0], points[1]);
            case 2:
                return new EqPointDouble(points[2], points[3]);
            default:
                throw new AssertionError("" + side);
        }
    }

    public EqLine side(int side) {
        switch (side) {
            case 0:
                return new EqLine(points[0], points[1], points[2], points[3]);
            case 1:
                return new EqLine(points[2], points[3], points[4], points[5]);
            case 2:
                return new EqLine(points[4], points[5], points[0], points[1]);
            default:
                throw new IndexOutOfBoundsException("" + side);
        }
    }

    public boolean isEmpty() {
        return (eq(points[0], points[2]) && eq(points[2], points[4]))
                || (eq(points[1], points[3]) && eq(points[3], points[5]));
    }

    private boolean eq(double a, double b) {
        return (a == b) || Math.abs(a - b) < 0.000000000001;
    }

    public EqPointDouble center() {
        return new EqPointDouble(centerX(), centerY());
    }

    public double centerX() {
        return (points[0] + points[2] + points[4]) / 3;
    }

    public double centerY() {
        return (points[1] + points[3] + points[5]) / 3;
    }

    public EqPointDouble topMostPoint() {
        int ix = -1;
        double min = Double.MAX_VALUE;
        for (int i = 0; i < 3; i++) {
            if (points[(i * 2) + 1] < min) {
                ix = i;
                min = points[(i * 2) + 1];
            }
        }
        return new EqPointDouble(points[ix], points[ix + 1]);
    }

    public EqPointDouble bottomMostPoint() {
        int ix = -1;
        double max = Double.MIN_VALUE;
        for (int i = 0; i < 3; i++) {
            if (points[i * 2] > max) {
                ix = i;
                max = points[i * 2];
            }
        }
        return new EqPointDouble(points[ix], points[ix + 1]);
    }

    public EqPointDouble leftMostPoint() {
        int ix = -1;
        double min = Double.MAX_VALUE;
        for (int i = 0; i < 3; i++) {
            if (points[i * 2] < min) {
                ix = i;
                min = points[i * 2];
            }
        }
        return new EqPointDouble(points[ix], points[ix + 1]);
    }

    public EqPointDouble rightMostPoint() {
        int ix = -1;
        double max = Double.MIN_VALUE;
        for (int i = 0; i < 3; i++) {
            if (points[i * 2] > max) {
                ix = i;
                max = points[i * 2];
            }
        }
        return new EqPointDouble(points[ix], points[ix + 1]);
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

    public Triangle2D[] tesselate() {
        if (isEmpty()) {
            return new Triangle2D[0];
        }
        return tesselate(longestSide());
    }

    public Triangle2D[] tesselate(int side) {
        EqLine l = side(side);

        double len = length(side);
        double ratio = (len / 2) / len;
        double x = ratio * l.x2 + (1.0 - ratio) * l.x1;
        double y = ratio * l.y2 + (1.0 - ratio) * l.y1;

        EqPointDouble a = oppositeCorner(side);

        Triangle2D[] result = new Triangle2D[2];
        result[0] = new Triangle2D(l.x1, l.y1, x, y, a.x, a.y);
        result[1] = new Triangle2D(l.x2, l.y2, x, y, a.x, a.y);
        return result;
    }

    public Triangle2D[] tesselateAndReplace(int side, List<Triangle2D> triangles) {
        return tesselateAndReplace(side, triangles, triangles.indexOf(this));
    }

    public Triangle2D[] tesselateAndReplace(List<Triangle2D> triangles) {
        return tesselateAndReplace(triangles, triangles.indexOf(this));
    }

    public Triangle2D[] tesselateAndReplace(List<Triangle2D> triangles, int index) {
        return tesselateAndReplace(longestSide(), triangles, index);
    }

    public Triangle2D[] tesselateAndReplace(int side, List<Triangle2D> triangles, int index) {
        assert index >= 0;
        Triangle2D[] result = tesselate(side);
        triangles.remove(index);
        triangles.add(index, result[1]);
        triangles.add(index, result[0]);
        return result;
    }

    @Override
    public String toString() {
        return "Tri(" + GeometryStrings.toStringCoordinates(points) + ")";
    }

    public static Triangle2D[] fromRectangle(RectangularShape rect) {
        double midX = (rect.getX() + (rect.getWidth() / 2));
        Triangle2D a = new Triangle2D(rect.getX(), rect.getY(), rect.getX() + midX, rect.getY(), rect.getX(), rect.getY() + rect.getHeight());
        Triangle2D b = new Triangle2D(rect.getX() + midX, rect.getY(), rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight(), rect.getX(), rect.getY() + rect.getHeight());
        Triangle2D c = new Triangle2D(rect.getX() + midX, rect.getY(), rect.getWidth() + rect.getY(), rect.getY(), rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight());
        return new Triangle2D[]{a, b, c};
    }

    public void setPoints(double ax, double ay, double bx, double by, double cx, double cy) {
        points[0] = ax;
        points[1] = ay;
        points[2] = bx;
        points[3] = by;
        points[4] = cx;
        points[5] = cy;
    }

    public double ax() {
        return points[0];
    }

    public double ay() {
        return points[1];
    }

    public double bx() {
        return points[2];
    }

    public double by() {
        return points[3];
    }

    public double cx() {
        return points[4];
    }

    public double cy() {
        return points[5];
    }

    public Triangle2D adjustedBy(double oax, double oay, double obx, double oby, double ocx, double ocy) {
        return new Triangle2D(points[0] + oax, points[1] + oay,
                points[2] + obx, points[3] + oby,
                points[4] + ocx, points[5] + ocy);
    }

    public static Triangle2D isoceles(double centerX, double centerY, double distanceToCenter) {
        Circle circ = new Circle(centerX, centerY, distanceToCenter);
        return new Triangle2D(
                circ.getPosition(0),
                circ.getPosition(135),
                circ.getPosition(225)
        );
    }

    public static Triangle2D right(double ax, double ay, double vlength, double hlength) {
        Circle circ = new Circle(ax, ay, vlength);
        double[] b = circ.positionOf(0, vlength);
        double[] c = circ.positionOf(90, hlength);
        return new Triangle2D(ax, ay, b[0], b[1], c[0], c[1]);
    }

    public static Triangle2D isoceles(Point2D center, double size) {
        return isoceles(center.getX(), center.getY(), size);
    }

    public void rotate(double degrees) {
        applyTransform(AffineTransform.getRotateInstance(Math.toRadians(degrees)));
    }

    public void applyTransform(AffineTransform xform) {
        xform.transform(points, 0, points, 0, 3);
    }

    public boolean sharesCorner(Triangle2D other) {
        return sharedPoints(other) == 1;
    }

    public boolean abuts(Triangle2D other) {
        return sharedPoints(other) == 2;
    }

    @SuppressWarnings("element-type-mismatch")
    public boolean containsCorner(Point2D pt) {
        return points().contains(pt instanceof EqPointDouble ? (EqPointDouble) pt
                : new EqPointDouble(pt));
    }

    public List<? extends EqPointDouble> points() {
        List<EqPointDouble> result
                = Arrays.asList(
                        new EqPointDouble(points[0], points[1]),
                        new EqPointDouble(points[2], points[3]),
                        new EqPointDouble(points[4], points[5])
                );
        return result;
    }

    public EqPointDouble midA() {
        return new EqLine(points[0], points[1], points[2], points[3]).midPoint();
    }

    public EqPointDouble midB() {
        return new EqLine(points[2], points[3], points[4], points[5]).midPoint();
    }

    public EqPointDouble midC() {
        return new EqLine(points[4], points[5], points[0], points[1]).midPoint();
    }

    public void midPoints(double[] into, int offset) {
        if (offset < 0 || offset + 6 >= into.length) {
            throw new IllegalArgumentException(offset + " of " + into.length);
        }
        midA().copyInto(into, offset);
        midB().copyInto(into, offset + 2);
        midC().copyInto(into, offset + 4);
    }

    public void setMidPoint(int side, double sx, double sy) {
        double len;
        int offset1, offset2;
        switch (side) {
            case 0:
                len = length(0);
                offset1 = 0;
                offset2 = 2;
                break;
            case 1:
                len = length(1);
                offset1 = 2;
                offset2 = 4;
                break;
            case 2:
                len = length(2);
                offset1 = 4;
                offset2 = 0;
                break;
            default:
                throw new IllegalArgumentException("No line " + side + " in a triangle");
        }

        double ang = angles()[side];
        Circle circ = new Circle(sx, sy, len / 2);
        circ.positionOf(ang, Angle.opposite(len / 2), offset1, points);
        circ.positionOf(ang, len / 2, offset2, points);
    }

    /**
     * Move the mid point of one side of this triangle in a direction
     * perpendicular to the angle of that side by the specified amount.
     *
     * @param side - 0, 1 or 2
     * @param by The amount to move by
     */
    public void moveMidPoint(int side, double by) {
        if (by == 0) {
            return;
        }
        EqPointDouble pt;
        double len;
        int offset1, offset2;
        switch (side) {
            case 0:
                len = length(0);
                pt = midA();
                offset1 = 0;
                offset2 = 2;
                break;
            case 1:
                len = length(1);
                pt = midB();
                offset1 = 2;
                offset2 = 4;
                break;
            case 2:
                len = length(2);
                pt = midC();
                offset1 = 4;
                offset2 = 0;
                break;
            default:
                throw new IllegalArgumentException("No line " + side + " in a triangle");
        }
        double ang = angles()[side];
        Circle circ = new Circle(pt, len / 2);
        double perp = by > 0 ? Angle.perpendicularClockwise(ang)
                : Angle.perpendicularCounterclockwise(ang);
        circ.positionOf(perp, by, (nx, ny) -> {
            double offX = nx - pt.x;
            double offY = ny - pt.y;
            points[offset1] += offX;
            points[offset1 + 1] += offY;
            points[offset2] += offX;
            points[offset2 + 1] += offY;
        });
    }

    @Override
    public void visitLines(DoubleQuadConsumer consumer, boolean includeClose) {
        visitLines(consumer);
    }

    @Override
    public void visitLines(DoubleQuadConsumer consumer) {
        consumer.accept(points[0], points[1], points[2], points[3]);
        consumer.accept(points[2], points[3], points[4], points[5]);
        consumer.accept(points[4], points[5], points[0], points[1]);
    }

    @Override
    public void visitAdjoiningLines(DoubleSextaConsumer sex) {
        sex.accept(points[0], points[1], points[2], points[3], points[4], points[5]);
        sex.accept(points[4], points[5], points[0], points[1], points[2], points[3]);
        sex.accept(points[2], points[3], points[4], points[5], points[0], points[1]);
    }

    public void setPoint(int point, double x, double y) {
        int offset = 0;
        switch (point) {
            case 0:
                break;
            case 1:
                offset = 2;
                break;
            case 2:
                offset = 4;
                break;
            default:
                throw new IndexOutOfBoundsException("" + point);
        }
        points[offset] = x;
        points[offset + 1] = y;
    }

    @Override
    public boolean selfIntersects() {
        return isEmpty();
    }

    @Override
    public boolean normalize() {
        if (isNormalized()) {
            return false;
        }
        List<? extends EqPointDouble> pts = points();
        ArrayList<EqPointDouble> nue = new ArrayList<>(pts);
        Collections.sort(nue);
        if (!pts.equals(nue)) {
            double[] newPoints = new double[6];
            newPoints[0] = nue.get(0).x;
            newPoints[1] = nue.get(0).y;
            newPoints[2] = nue.get(1).x;
            newPoints[3] = nue.get(1).y;
            newPoints[4] = nue.get(2).x;
            newPoints[5] = nue.get(2).y;
            points = newPoints;
            return true;
        }
        return false;
    }
}
