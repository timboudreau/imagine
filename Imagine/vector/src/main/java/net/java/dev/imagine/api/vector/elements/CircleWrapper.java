package net.java.dev.imagine.api.vector.elements;

import com.mastfrog.util.collections.IntSet;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import static java.lang.Double.doubleToLongBits;
import java.util.BitSet;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.Fillable;
import net.java.dev.imagine.api.vector.Strokable;
import net.java.dev.imagine.api.vector.Versioned;
import net.java.dev.imagine.api.vector.Volume;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import static net.java.dev.imagine.api.vector.design.ControlPointKind.PHYSICAL_POINT;
import static net.java.dev.imagine.api.vector.design.ControlPointKind.RADIUS;
import net.java.dev.imagine.api.vector.util.Pt;
import org.imagine.geometry.Circle;
import net.java.dev.imagine.api.vector.Vectors;
import org.imagine.geometry.EqPointDouble;

/**
 *
 * @author Tim Boudreau
 */
public class CircleWrapper implements Strokable, Fillable, Volume, Adjustable, Vectors, Versioned {

    private static final long serialVersionUID = 1;
    public double centerX, centerY, radius;
    public boolean fill;
    private int rev;

    public CircleWrapper(Circle circle) {
        this(circle, true);
    }

    public CircleWrapper(Circle circle, boolean fill) {
        centerX = circle.centerX();
        centerY = circle.centerY();
        radius = circle.radius();
        this.fill = fill;
    }

    public CircleWrapper(double centerX, double centerY, double radius) {
        this(centerX, centerY, radius, true);
    }

    public CircleWrapper(double centerX, double centerY, double radius, boolean fill) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
        this.fill = fill;
    }

    public CircleWrapper(CircleWrapper other) {
        this.rev = other.rev;
        this.fill = other.fill;
        this.centerX = other.centerX;
        this.centerY = other.centerY;
        this.radius = other.radius;
    }

    @Override
    public int rev() {
        return rev;
    }

    private void change() {
        rev++;
    }

    @Override
    public double cumulativeLength() {
        return 2D * Math.PI * radius;
    }

    @Override
    public Runnable restorableSnapshot() {
        double cx = centerX;
        double cy = centerY;
        double rad = radius;
        int oldRev = rev;
        return () -> {
            centerX = cx;
            centerY = cy;
            radius = rad;
            rev = oldRev;
        };
    }

    @Override
    public double centerX() {
        return centerX;
    }

    @Override
    public double centerY() {
        return centerY;
    }

    public double radius() {
        return radius;
    }

    public void setCenterX(double centerX) {
        if (centerX != this.centerX) {
            this.centerX = centerX;
            change();
        }
    }

    public void setCenterY(double centerY) {
        if (centerY != this.centerY) {
            this.centerY = centerY;
            change();
        }
    }

    public void setRadius(double radius) {
        if (radius != this.radius) {
            this.radius = radius;
            change();
        }
    }

    @Override
    public void translate(double x, double y) {
        if (x != 0 || y != 0) {
            centerX += x;
            centerY += y;
            change();
        }
    }

    @Override
    public CircleWrapper copy() {
        return new CircleWrapper(this);
    }

    @Override
    public void draw(Graphics2D g) {
        g.draw(toShape());
    }

    @Override
    public Circle toShape() {
        return new Circle(centerX, centerY, radius);
    }

    @Override
    public EqPointDouble center() {
        return new EqPointDouble(centerX, centerY);
    }

    @Override
    public Pt getLocation() {
        return new Pt(centerX, centerY);
    }

    @Override
    public void setLocation(double x, double y) {
        if (x != centerX || y != centerY) {
            centerX = x;
            centerY = y;
            change();
        }
    }

    @Override
    public void clearLocation() {
        if (centerX != 0 || centerY != 0) {
            centerX = centerY = 0;
            change();
        }
    }

    @Override
    public Vectors copy(AffineTransform transform) {
        switch (transform.getType()) {
            case AffineTransform.TYPE_QUADRANT_ROTATION:
            case AffineTransform.TYPE_GENERAL_ROTATION:
                return copy();
            default:
                double[] pts = new double[]{centerX, centerY, 0, 0};
                Circle circ = new Circle(centerX, centerY, radius);
                circ.positionOf(0, radius, 2, pts);
                transform.transform(pts, 0, pts, 0, 2);
                double newRadius = Point2D.distance(pts[0], pts[1], pts[2], pts[3]);
                CircleWrapper result = new CircleWrapper(pts[0], pts[1], newRadius);
                result.rev = rev + 1;
                return result;
        }
    }

    @Override
    public ControlPointKind[] getControlPointKinds() {
        return new ControlPointKind[]{PHYSICAL_POINT, RADIUS, RADIUS, RADIUS, RADIUS};
    }

    @Override
    public void getBounds(Rectangle2D dest) {
        dest.setFrameFromDiagonal(centerX - radius, centerY - radius,
                centerX + radius, centerY + radius);
    }

    @Override
    public void addToBounds(Rectangle2D bds) {
        if (bds.isEmpty()) {
            getBounds(bds);
        } else {
            bds.add(centerX - radius, centerY - radius);
            bds.add(centerX + radius, centerY + radius);
        }
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle((int) Math.floor(centerX - radius),
                (int) Math.floor(centerY - radius),
                (int) Math.ceil(centerX + radius),
                (int) Math.ceil(centerY + radius));
    }

    @Override
    public void paint(Graphics2D g) {
        if (fill) {
            draw(g);
        } else {
            fill(g);
        }
    }

    @Override
    public void applyTransform(AffineTransform xform) {
        switch (xform.getType()) {
            case AffineTransform.TYPE_QUADRANT_ROTATION:
            case AffineTransform.TYPE_GENERAL_ROTATION:
                return;
            default:
                double[] pts = new double[]{centerX, centerY, 0, 0};
                Circle circ = new Circle(centerX, centerY, radius);
                circ.positionOf(0, radius, 2, pts);
                xform.transform(pts, 0, pts, 0, pts.length / 2);
                double newRadius = Point2D.distance(pts[0], pts[1], pts[2], pts[3]);
                boolean changed = radius != newRadius || centerX != pts[0] || centerY != pts[1];
                if (changed) {
                    centerX = pts[0];
                    centerY = pts[1];
                    radius = newRadius;
                    change();
                }
        }
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
        return 5;
    }

    @Override
    public void getControlPoints(double[] xy) {
        xy[0] = centerX;
        xy[1] = centerY;
        Circle circ = toShape();
        circ.positionOf(0, radius, 2, xy); // 1
        circ.positionOf(90, radius, 4, xy); // 2
        circ.positionOf(180, radius, 6, xy); // 3
        circ.positionOf(270, radius, 8, xy); // 4
    }

    @Override
    public void collectSizings(SizingCollector c) {
        c.dimension(radius, true, 0, 1);
        c.dimension(radius * 2, true, 1, 3);
        c.dimension(radius, false, 0, 2);
        c.dimension(radius * 2, false, 2, 4);
    }

    @Override
    public int[] getVirtualControlPointIndices() {
        return new int[]{1, 2, 3, 4};
    }

    @Override
    public IntSet virtualControlPointIndices() {
        BitSet bits = new BitSet(5);
        bits.set(1, 5);
        return IntSet.of(bits);
    }

    @Override
    public void setControlPointLocation(int pointIndex, Pt location) {
        switch (pointIndex) {
            case 0:
                if (location.x != centerX || location.y != centerY) {
                    centerX = location.x;
                    centerY = location.y;
                    change();
                }
                break;
            case 1:
            case 2:
            case 3:
            case 4:
                double newRadius = Point2D.distance(centerX, centerY, location.x,
                        location.y);
                if (newRadius != radius) {
                    radius = newRadius;
                    change();
                }
                break;
            default:
                throw new IllegalArgumentException("Only 2 control points, got "
                        + pointIndex);
        }
    }

    @Override
    public String toString() {
        return "Circle(" + radius + " @ " + centerX + ", " + centerY + ")";
    }

    @Override
    public int hashCode() {
        long bits = (doubleToLongBits(centerX) * 39)
                + (doubleToLongBits(centerY) * 2_633)
                + (doubleToLongBits(radius) * 13_757);
        return ((int) bits) ^ ((int) (bits >> 32));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof CircleWrapper)) {
            return false;
        }
        final CircleWrapper other = (CircleWrapper) obj;
        if (doubleToLongBits(centerX) != doubleToLongBits(other.centerX)) {
            return false;
        }
        if (doubleToLongBits(centerY) != doubleToLongBits(other.centerY)) {
            return false;
        }
        return doubleToLongBits(this.radius) == doubleToLongBits(other.radius);
    }

    @Override
    public boolean canApplyTransform(AffineTransform xform) {
        switch (xform.getType()) {
            case AffineTransform.TYPE_GENERAL_ROTATION:
            case AffineTransform.TYPE_QUADRANT_ROTATION:
                return false;
            default:
                return true;
        }
    }

    @Override
    public void collectAngles(AngleCollector c) {
        // do nothing
    }
}
