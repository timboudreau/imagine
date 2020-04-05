/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.geometry;

import com.mastfrog.function.DoubleQuadConsumer;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import org.imagine.geometry.util.GeometryStrings;
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

    @Override
    public LineVector withAlternatePoints(double newAx, double newAy, double newBx, double newBy) {
        CornerAngle ang = corner();
        double a = ang.trailingAngle();
        double l1 = firstLineLength();
        EqLine lnA = new EqLine(newAx, newAy, 0, 0);
        lnA.setAngleAndLength(a, l1);

        double b = ang.leadingAngle();
        double l2 = secondLineLength();
        EqLine lnB = new EqLine(newBx, newBy, 0, 0);
        lnB.setAngleAndLength(b, l2);

        EqPointDouble apex = lnA.intersection(lnB);
        return new LineVectorImpl(newAx, newAy, apex.x, apex.y, newBx, newBy);
    }

    @Override
    public LineVector inverse() {
        return new LineVectorImpl(bx, by, sx, sy, ax, ay);
    }

    @Override
    public LineVector toLineVector(double atX, double atY) {
        if (atX == apexX() && atY == apexY()) {
            return this;
        }
        return LineVector.super.toLineVector(atX, atY);
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
        return new CornerAngle(ax, ay, sx, sy, bx, by);
    }

    @Override
    public CornerAngle inverseCorner() {
        return new CornerAngle(bx, by, sx, sy, ax, ay);
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
        return "<--" + GeometryStrings.toShortString(ax, bx)
                + " -> " + GeometryStrings.toShortString(sx, sy)
                + " <- " + GeometryStrings.toShortString(bx, by)
                + " (" + GeometryStrings.toDegreesString(firstLineAngle())
                + ":" + GeometryStrings.toDegreesString(secondLineAngle())
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
    public double apexX() {
        return sx;
    }

    @Override
    public double apexY() {
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
