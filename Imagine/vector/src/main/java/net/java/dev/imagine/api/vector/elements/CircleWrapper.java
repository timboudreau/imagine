/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.elements;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import static java.lang.Double.doubleToLongBits;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.Fillable;
import net.java.dev.imagine.api.vector.Strokable;
import net.java.dev.imagine.api.vector.Vector;
import net.java.dev.imagine.api.vector.Volume;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import static net.java.dev.imagine.api.vector.design.ControlPointKind.PHYSICAL_POINT;
import static net.java.dev.imagine.api.vector.design.ControlPointKind.RADIUS;
import net.java.dev.imagine.api.vector.util.Pt;
import org.imagine.geometry.Circle;

/**
 *
 * @author Tim Boudreau
 */
public class CircleWrapper implements Strokable, Fillable, Volume, Adjustable, Vector {

    public double centerX, centerY, radius;
    public boolean fill;

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

    public double cumulativeLength() {
        return 2D * Math.PI * radius;
    }

    @Override
    public Runnable restorableSnapshot() {
        double cx = centerX;
        double cy = centerY;
        double rad = radius;
        return () -> {
            centerX = cx;
            centerY = cy;
            radius = rad;
        };
    }

    public double centerX() {
        return centerX;
    }

    public double centerY() {
        return centerY;
    }

    public double radius() {
        return radius;
    }

    public void setCenterX(double centerX) {
        this.centerX = centerX;
    }

    public void setCenterY(double centerY) {
        this.centerY = centerY;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    @Override
    public void translate(double x, double y) {
        centerX += x;
        centerY += y;
    }

    @Override
    public CircleWrapper copy() {
        return new CircleWrapper(centerX, centerY, radius, fill);
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
    public Pt getLocation() {
        return new Pt(centerX, centerY);
    }

    @Override
    public void setLocation(double x, double y) {
        centerX = x;
        centerY = y;
    }

    @Override
    public void clearLocation() {
        centerX = centerY = 0;
    }

    @Override
    public Vector copy(AffineTransform transform) {
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
                return new CircleWrapper(pts[0], pts[1], newRadius);
        }
    }

    @Override
    public ControlPointKind[] getControlPointKinds() {
        return new ControlPointKind[]{PHYSICAL_POINT, RADIUS};
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
                centerX = pts[0];
                centerY = pts[1];
                radius = newRadius;
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
        return 2;
    }

    @Override
    public void getControlPoints(double[] xy) {
        xy[0] = centerX;
        xy[1] = centerY;
        toShape().positionOf(0, radius, 2, xy);
    }

    @Override
    public int[] getVirtualControlPointIndices() {
        return new int[]{1};
    }

    @Override
    public void setControlPointLocation(int pointIndex, Pt location) {
        switch (pointIndex) {
            case 0:
                centerX = location.x;
                centerY = location.y;
                break;
            case 1:
                radius = Point2D.distance(centerX, centerY, location.x,
                        location.y);
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
}
