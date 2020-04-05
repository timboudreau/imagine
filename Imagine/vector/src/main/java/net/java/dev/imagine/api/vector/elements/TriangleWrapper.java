package net.java.dev.imagine.api.vector.elements;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.List;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.Fillable;
import net.java.dev.imagine.api.vector.Strokable;
import net.java.dev.imagine.api.vector.Versioned;
import net.java.dev.imagine.api.vector.Volume;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import net.java.dev.imagine.api.vector.util.Pt;
import org.imagine.geometry.Angle;
import org.imagine.geometry.EnhancedShape;
import org.imagine.geometry.Triangle2D;
import net.java.dev.imagine.api.vector.Vectors;
import org.imagine.geometry.Axis;
import org.imagine.geometry.EqLine;
import com.mastfrog.function.state.Int;

/**
 *
 * @author Tim Boudreau
 */
public class TriangleWrapper implements Strokable, Fillable, Volume, Adjustable, Vectors, Versioned {

    private double ax;
    private double ay;
    private double bx;
    private double by;
    private double cx;
    private double cy;
    private boolean fill;
    private int rev;

    public TriangleWrapper(double ax, double ay, double bx, double by, double cx, double cy) {
        this(ax, ay, bx, by, cx, cy, true);
    }

    public TriangleWrapper(double ax, double ay, double bx, double by, double cx, double cy, boolean fill) {
        this.ax = ax;
        this.ay = ay;
        this.bx = bx;
        this.by = by;
        this.cx = cx;
        this.cy = cy;
        this.fill = fill;
    }

    public TriangleWrapper(Triangle2D t) {
        this(t, true);
    }

    public TriangleWrapper(Triangle2D t, boolean fill) {
        ax = t.ax();
        ay = t.ay();
        bx = t.bx();
        by = t.by();
        cx = t.cx();
        cy = t.cy();
        this.fill = fill;
    }

    @Override
    public int rev() {
        return rev;
    }

    private void change() {
        rev++;
    }

    @Override
    public Runnable restorableSnapshot() {
        double oax = ax;
        double oay = ay;
        double obx = bx;
        double oby = by;
        double ocx = cx;
        double ocy = cy;
        int oldRev = rev;
        return () -> {
            rev = oldRev;
            ax = oax;
            ay = oay;
            bx = obx;
            by = oby;
            cx = ocx;
            cy = ocy;
        };
    }

    public double ax() {
        return ax;
    }

    public double ay() {
        return ay;
    }

    public double bx() {
        return bx;
    }

    public double by() {
        return by;
    }

    public double cx() {
        return cx;
    }

    public double cy() {
        return cy;
    }

    public void setAx(double val) {
        if (val != ax) {
            ax = val;
            change();
        }
    }

    public void setAy(double val) {
        if (val != ay) {
            ay = val;
            change();
        }
    }

    public void setBx(double val) {
        if (val != bx) {
            bx = val;
            change();
        }
    }

    public void setBy(double val) {
        if (val != by) {
            by = val;
            change();
        }
    }

    public void setCx(double val) {
        if (val != cx) {
            cx = val;
            change();
        }
    }

    public void setCy(double val) {
        if (val != cy) {
            cy = val;
            change();
        }
    }

    @Override
    public void translate(double x, double y) {
        if (x != 0 || y != 0) {
            this.ax += x;
            this.ay += y;
            this.bx += x;
            this.by += y;
            this.cx += x;
            this.cy += y;
            change();
        }
    }

    public TriangleWrapper[] tesselate() {
        Triangle2D[] triangles = toShape().tesselate();
        TriangleWrapper[] res = new TriangleWrapper[triangles.length];
        for (int i = 0; i < triangles.length; i++) {
            res[i] = new TriangleWrapper(triangles[i], fill);
        }
        return res;
    }

    @Override
    public ControlPointKind[] getControlPointKinds() {
        ControlPointKind[] kinds = new ControlPointKind[6];
        Arrays.fill(kinds, 0, 3, ControlPointKind.PHYSICAL_POINT);
        Arrays.fill(kinds, 3, 6, ControlPointKind.EDGE_HANDLE);
        return kinds;
    }

    @Override
    public TriangleWrapper copy() {
        TriangleWrapper result = new TriangleWrapper(ax, ay, bx, by, cx, cy, fill);
        result.rev = rev;
        return result;
    }

    @Override
    public void draw(Graphics2D g) {
        g.draw(toShape());
    }

    @Override
    public Triangle2D toShape() {
        return new Triangle2D(ax, ay, bx, by, cx, cy);
    }

    @Override
    public void collectSizings(SizingCollector c) {
        Triangle2D t = toShape();
        List<? extends EqLine> all = t.lines();
        Int cursor = Int.create();
        for (EqLine e : all) {
            Axis axis = e.nearestAxis();
            c.dimension(e.distanceIn(axis),
                    axis == Axis.VERTICAL, cursor.getAsInt(),
                    cursor.getAsInt() + 1);
            cursor.increment();
        }
    }

    @Override
    public double cumulativeLength() {
        return Point2D.distance(ax, ay, bx, by)
                + Point2D.distance(bx, by, cx, cy)
                + Point2D.distance(cx, cy, ax, ay);
    }

    @Override
    public <T> T as(Class<T> type) {
        if (EnhancedShape.class == type) {
            return type.cast(toShape());
        }
        return Strokable.super.as(type);
    }

    @Override
    public boolean is(Class<?> type) {
        if (EnhancedShape.class == type) {
            return true;
        }
        return Strokable.super.is(type);
    }

    @Override
    public Pt getLocation() {
        return new Pt(ax, ay);
    }

    @Override
    public void setLocation(double x, double y) {
        if (ax != x && ay != y) {
            double dx = x - ax;
            double dy = y - ay;
            ax = x;
            ay = y;
            bx += dx;
            by += dy;
            cx += dx;
            cy += dy;
            change();
        }
    }

    @Override
    public void clearLocation() {
        setLocation(0, 0);
    }

    @Override
    public TriangleWrapper copy(AffineTransform transform) {
        if (transform == null || transform.isIdentity()) {
            return copy();
        }
        double[] pts = new double[]{ax, ay, bx, by, cx, cy};
        transform.transform(pts, 0, pts, 0, 3);
        TriangleWrapper result = new TriangleWrapper(pts[0], pts[1],
                pts[2], pts[3], pts[4], pts[5]);
        result.rev = rev + 1;
        return result;
    }

    @Override
    public void addToBounds(Rectangle2D bds) {
        double minX = Math.min(ax, Math.min(bx, cx));
        double minY = Math.min(ay, Math.min(by, cy));
        double maxX = Math.max(ax, Math.max(bx, cx));
        double maxY = Math.max(ay, Math.max(by, cy));
        if (bds.isEmpty()) {
            bds.setFrameFromDiagonal(minX, minY, maxX, maxY);
        } else {
            bds.add(minX, minY);
            bds.add(maxX, maxY);
        }
    }

    @Override
    public void getBounds(Rectangle2D dest) {
        double minX = Math.min(ax, Math.min(bx, cx));
        double minY = Math.min(ay, Math.min(by, cy));
        double maxX = Math.max(ax, Math.max(bx, cx));
        double maxY = Math.max(ay, Math.max(by, cy));
        double width = maxX - minX;
        double height = maxY - minY;
        dest.setFrame(minX, minY, width, height);
    }

    @Override
    public Rectangle getBounds() {
        double x = Math.floor(Math.min(ax, Math.min(bx, cx)));
        double y = Math.floor(Math.min(ay, Math.min(by, cy)));
        double mx = Math.ceil(Math.max(ax, Math.max(bx, cx)));
        double my = Math.ceil(Math.max(ay, Math.max(by, cy)));
        return new Rectangle((int) x, (int) y, (int) (mx - x), (int) (my - y));
    }

    @Override
    public void paint(Graphics2D g) {
        if (fill) {
            fill(g);
        } else {
            draw(g);
        }
    }

    @Override
    public void applyTransform(AffineTransform xform) {
        if (xform == null || xform.isIdentity()) {
            return;
        }
        double[] pts = new double[]{ax, ay, bx, by, cx, cy};
        xform.transform(pts, 0, pts, 0, 3);
        ax = pts[0];
        ay = pts[1];
        bx = pts[2];
        by = pts[3];
        cx = pts[4];
        cy = pts[5];
        change();
    }

    @Override
    public void fill(Graphics2D g) {
        g.fill(toShape());
    }

    @Override
    public boolean isFill() {
        return fill;
    }

    @Override
    public int getControlPointCount() {
//        return 6;
        // XXX get midpoints working
        return 3;
    }

    @Override
    public void getControlPoints(double[] xy) {
        xy[0] = ax;
        xy[1] = ay;
        xy[2] = bx;
        xy[3] = by;
        xy[4] = cx;
        xy[5] = cy;
//        toShape().midPoints(xy, 5);
    }

    @Override
    public int[] getVirtualControlPointIndices() {
        return new int[]{3, 4, 5};
    }

    public void setA(double x, double y) {
        if (x != ax || y != ay) {
            ax = x;
            ay = y;
            change();
        }
    }

    public void setB(double x, double y) {
        if (x != bx || y != by) {
            bx = x;
            by = y;
            change();
        }
    }

    public void setC(double x, double y) {
        if (x != cx || y != cy) {
            cx = x;
            cy = y;
            change();
        }
    }

    @Override
    public void setControlPointLocation(int pointIndex, Pt location) {
        switch (pointIndex) {
            case 0:
                setA(location.x, location.y);
                break;
            case 1:
                setB(location.x, location.y);
                break;
            case 2:
                setC(location.x, location.y);
                break;
            case 3 :
            case 4 :
            case 5 :
                setMidPoint(pointIndex - 3, location);
                break;
            default:
                throw new IllegalArgumentException("Index out of range " + pointIndex);
        }
    }
    
    private void setPoints(double ax, double ay, double bx, double by, double cx, double cy) {
        if (this.ax != ax || this.ay != ay || this.bx != bx || this.by != by || this.cx != cx || this.cy != cy) {
            this.ax = ax;
            this.ay = ay;
            this.bx = bx;
            this.by = by;
            this.cx = cx;
            this.cy = cy;
            change();
        }
    }

    private void setMidPoint(int side, Pt location) {
        Triangle2D shape = toShape();
        shape.setMidPoint(side, location.x, location.y);
        setPoints(shape.ax(), shape.ay(), shape.bx(), shape.by(), shape.cx(), shape.cy());
    }

    @Override
    public String toString() {
        return "Triangle(" + ax + ", " + ay + ", "
                + bx + ", " + by + ", "
                + cx + ", " + cy + ")";
    }

    @Override
    public int hashCode() {
        long bits = 0;
        for (int i = 0; i < 6; i++) {
            double val;
            switch (i) {
                case 0:
                    val = ax * 5;
                    break;
                case 1:
                    val = ay * 433;
                    break;
                case 2:
                    val = bx * 691;
                    break;
                case 3:
                    val = by * 3_931;
                    break;
                case 4:
                    val = cx * 8_101;
                    break;
                case 5:
                    val = cy * 11;
                    break;
                default:
                    throw new AssertionError(i);
            }
            bits += val;
        }
        return (int) (Double.doubleToLongBits(bits)
                ^ (Double.doubleToLongBits(bits) >>> 32));
    }

    public boolean isIsomorphic(TriangleWrapper other) {
        if (other == null) {
            return false;
        } else if (other == this) {
            return true;
        }
        return Arrays.equals(pointsSorted(), other.pointsSorted());
    }

    public double[] angles() {
        return new double[]{
            Angle.ofLine(ax, ay, bx, by),
            Angle.ofLine(bx, by, cx, cy),
            Angle.ofLine(cx, cy, ax, ay)
        };
    }

    @Override
    public void collectAngles(AngleCollector c) {
        c.angle(Angle.ofLine(ax, ay, bx, by), 0, 1);
        c.angle(Angle.ofLine(bx, by, cx, cy), 1, 2);
        c.angle(Angle.ofLine(cx, cy, ax, ay), 2, 0);
    }

    public double[] toDoubleArray() {
        return pointsSorted();
    }

    public void normalize() {
        double[] d = pointsSorted();
        ax = d[0];
        ay = d[1];
        bx = d[2];
        by = d[3];
        cx = d[4];
        cy = d[5];
    }

    private double[] pointsSorted() {
        double[][] d = new double[3][2];
        d[0][0] = ax;
        d[0][1] = ay;
        d[1][0] = bx;
        d[1][1] = by;
        d[2][0] = cx;
        d[2][1] = cy;
        Arrays.sort(d, (a, b) -> {
            int result = Double.compare(a[0], b[0]);
            if (result == 0) {
                result = Double.compare(a[1], b[1]);
            }
            return result;
        });
        double[] result = new double[]{
            d[0][0], d[0][1], d[1][0], d[1][1], d[2][0], d[2][1]
        };
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TriangleWrapper other = (TriangleWrapper) obj;
        return doublesEqual(this.ax, other.ax)
                && doublesEqual(this.ay, other.ay)
                && doublesEqual(this.bx, other.bx)
                && doublesEqual(this.by, other.by)
                && doublesEqual(this.cx, other.cx)
                && doublesEqual(this.cy, other.cy);
    }

    private static boolean doublesEqual(double ax, double bx) {
        return Double.doubleToLongBits(ax + 0.0) == Double.doubleToLongBits(bx + 0.0);
    }
}
