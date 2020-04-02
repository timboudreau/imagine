/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.geometry;

import com.mastfrog.function.DoubleQuadConsumer;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import org.imagine.geometry.util.GeometryUtils;

/**
 *
 * @author Tim Boudreau
 */
final class LineVectorImpl implements LineVector {

    private final double ax, ay, sx, sy, bx, by;

    LineVectorImpl(double ax, double ay, double sx, double sy, double bx, double by) {
        this.ax = ax;
        this.ay = ay;
        this.sx = sx;
        this.sy = sy;
        this.bx = bx;
        this.by = by;
    }

    public int intersectionCount(Intersectable other, boolean includeClose) {
        if (other == this) {
            return 0;
        }
        if (other instanceof LineVector) {
            LineVector lv = (LineVector) other;
            if (lv.firstLine().intersectsLine(secondLine())) {
                return 1;
            }
            return 0;
        } else if (other instanceof EqLine) {
            EqLine ln = (EqLine) other;
            if (ln.intersectsLine(secondLine())) {
                return 1;
            }
            return 0;
        }
        return LineVector.super.intersectionCount(other, includeClose);
    }

    @Override
    public CornerAngle corner() {
        return new CornerAngle(secondLineAngle(), firstLineAngle());
    }

    @Override
    public CornerAngle inverseCorner() {
        return new CornerAngle(firstLineAngle(), secondLineAngle());
    }

    @Override
    public CornerAngle corner(RotationDirection dir) {
        if (dir == ccw()) {
            return corner();
        } else {
            return inverseCorner();
        }
    }

    @Override
    public String toString() {
        return "<--" + GeometryUtils.toShortString(ax, bx)
                + " -> " + GeometryUtils.toShortString(sx, sy)
                + " <- " + GeometryUtils.toShortString(bx, by)
                + " (" + GeometryUtils.toDegreesString(firstLineAngle())
                + ":" + GeometryUtils.toDegreesString(secondLineAngle())
                + ")-->";
    }

    public RotationDirection ccw() {
        int ccw = Line2D.relativeCCW(ax, ax, sx, sy, bx, by);
        switch (ccw) {
            case -1:
                return RotationDirection.CLOCKWISE;
            case 1:
                return RotationDirection.COUNTER_CLOCKWISE;
            case 0:
                return RotationDirection.NONE;
            default:
                throw new AssertionError(ccw);
        }
    }

    @Override
    public double firstLineAngle() {
        return Circle.angleOf(sx, sy, ax, ay);
    }

    @Override
    public double secondLineAngle() {
        return Circle.angleOf(sx, sy, bx, by);
    }

    @Override
    public double firstLineLength() {
        return Point2D.distance(ax, ay, sx, sy);
    }

    @Override
    public double secondLineLength() {
        return Point2D.distance(sx, sy, bx, by);
    }

    @Override
    public LineVectorImpl next(double nx, double ny) {
        return new LineVectorImpl(sx, sy, bx, by, nx, ny);
    }

    @Override
    public LineVectorImpl previous(double px, double py) {
        return new LineVectorImpl(px, py, ax, ay, sx, sy);
    }

    @Override
    public boolean intersectsFirst(double x1, double y1, double x2, double y2) {
        return GeometryUtils.linesIntersect(ax, ay, sx, sy, x1, y1, x2, y2, false);
    }

    @Override
    public boolean intersectsSecond(double x1, double y1, double x2, double y2) {
        return GeometryUtils.linesIntersect(sx, sy, bx, by, x1, y1, x2, y2, false);
    }

    @Override
    public double firstX() {
        return ax;
    }

    @Override
    public double firstY() {
        return ay;
    }

    @Override
    public double sharedX() {
        return sx;
    }

    @Override
    public double sharedY() {
        return sy;
    }

    @Override
    public double secondX() {
        return bx;
    }

    @Override
    public double secondY() {
        return by;
    }

    @Override
    public void visitLines(DoubleQuadConsumer consumer, boolean includeClose) {
        consumer.accept(ax, ay, sx, sy);
        consumer.accept(sx, sy, bx, by);
        if (includeClose) {
            consumer.accept(bx, by, ax, ay);
        }
    }

}
