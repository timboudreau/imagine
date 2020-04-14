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
import static org.imagine.geometry.util.GeometryUtils.isSameCoordinate;

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

    @Override
    public int intersectionCount(Intersectable other, boolean includeClose) {
        if (other == this) {
            return 0;
        }
        if (other instanceof LineVector) {
            LineVector lv = (LineVector) other;
            if (lv.trailingLine().intersectsLine(leadingLine())) {
                return 1;
            }
            return 0;
        } else if (other instanceof EqLine) {
            EqLine ln = (EqLine) other;
            if (ln.intersectsLine(leadingLine())) {
                return 1;
            }
            return 0;
        }
        return LineVector.super.intersectionCount(other, includeClose);
    }

    /**
     * Returns a circle where which any point within one hemisphere of results
     * in an angle with the same extent as this LineVector's corner angle.
     *
     * @return A circle
     */
    @Override
    public Circle extentCircle(double ext) {
        if (ext < 0) {
            ext += 360;
            ext = Angle.normalize(ext);
        }
        EqLine l1 = new EqLine(trailingX(), trailingY(), 1, 1);
        EqLine l2 = new EqLine(leadingX(), leadingY(), 1, 1);

        EqLine l3 = new EqLine(trailingX(), trailingY(), 1, 1);
        EqLine l4 = new EqLine(leadingX(), leadingY(), 1, 1);

        Sector nue = Sector.create(toSector().start(), ext);
        double tla = nue.minDegrees();
        double lla = nue.maxDegrees();
        double ang1 = Angle.opposite(tla);
        double ang2 = Angle.opposite(lla);

        l1.setAngle(ang1);
        l2.setAngle(ang2);

        double a3 = Angle.opposite(tla - 90);
        double a4 = Angle.opposite(lla - 90);

        l3.setAngle(a3);
        l4.setAngle(a4);

        EqPointDouble pt = l1.intersectionPoint(l2);

        EqPointDouble pt2 = l3.intersectionPoint(l4);

        EqLine ln = new EqLine(pt, pt2);

        EqPointDouble mid = ln.midPoint();
        return new Circle(mid, mid.distance(trailingPoint()));
    }

    @Override
    public Circle extentCircle() {
        EqLine l1 = new EqLine(trailingX(), trailingY(), 1, 1);
        EqLine l2 = new EqLine(leadingX(), leadingY(), 1, 1);

        EqLine l3 = new EqLine(trailingX(), trailingY(), 1, 1);
        EqLine l4 = new EqLine(leadingX(), leadingY(), 1, 1);

        double tla = trailingLineAngle();
        double lla = leadingLineAngle();

        double ang1 = Angle.opposite(tla);
        double ang2 = Angle.opposite(lla);

        l1.setAngle(ang1);
        l2.setAngle(ang2);

        double a3 = Angle.opposite(tla - 90);
        double a4 = Angle.opposite(lla - 90);

        l3.setAngle(a3);
        l4.setAngle(a4);

        EqPointDouble pt = l1.intersectionPoint(l2);
        EqPointDouble pt2 = l3.intersectionPoint(l4);
        EqLine ln = new EqLine(pt, pt2);
        EqPointDouble mid = ln.midPoint();
        return new Circle(mid, mid.distance(trailingPoint()));
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
        return "<" + GeometryStrings.toString(ax, bx)
                + " : " + GeometryStrings.toString(sx, sy)
                + " : " + GeometryStrings.toString(bx, by)
                + " (" + GeometryStrings.toDegreesString(trailingLineAngle())
                + " / " + GeometryStrings.toDegreesString(leadingLineAngle())
                + ")>";
    }

    public String toShortString() {
        return "<" + GeometryStrings.toShortString(ax, bx)
                + " : " + GeometryStrings.toShortString(sx, sy)
                + " : " + GeometryStrings.toShortString(bx, by)
                + " (" + GeometryStrings.toDegreesString(trailingLineAngle())
                + " / " + GeometryStrings.toDegreesString(leadingLineAngle())
                + ")>";
    }

    @Override
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
    public double trailingLineAngle() {
        return Circle.angleOf(sx, sy, ax, ay);
    }

    @Override
    public double leadingLineAngle() {
        return Circle.angleOf(sx, sy, bx, by);
    }

    @Override
    public double trailingLineLength() {
        return Point2D.distance(ax, ay, sx, sy);
    }

    @Override
    public double leadingLineLength() {
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
    public boolean intersectsTrailingLine(double x1, double y1, double x2, double y2) {
        return GeometryUtils.linesIntersect(ax, ay, sx, sy, x1, y1, x2, y2, false);
    }

    @Override
    public boolean intersectsLeadingLine(double x1, double y1, double x2, double y2) {
        return GeometryUtils.linesIntersect(sx, sy, bx, by, x1, y1, x2, y2, false);
    }

    @Override
    public double trailingX() {
        return ax;
    }

    @Override
    public double trailingY() {
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
    public double leadingX() {
        return bx;
    }

    @Override
    public double leadingY() {
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

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + (int) (Double.doubleToLongBits(this.ax) ^ (Double.doubleToLongBits(this.ax) >>> 32));
        hash = 53 * hash + (int) (Double.doubleToLongBits(this.ay) ^ (Double.doubleToLongBits(this.ay) >>> 32));
        hash = 53 * hash + (int) (Double.doubleToLongBits(this.sx) ^ (Double.doubleToLongBits(this.sx) >>> 32));
        hash = 53 * hash + (int) (Double.doubleToLongBits(this.sy) ^ (Double.doubleToLongBits(this.sy) >>> 32));
        hash = 53 * hash + (int) (Double.doubleToLongBits(this.bx) ^ (Double.doubleToLongBits(this.bx) >>> 32));
        hash = 53 * hash + (int) (Double.doubleToLongBits(this.by) ^ (Double.doubleToLongBits(this.by) >>> 32));
        return hash;
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
        final LineVectorImpl other = (LineVectorImpl) obj;
        return isSameCoordinate(apexX(), other.apexX())
                && isSameCoordinate(apexY(), other.apexY())
                && isSameCoordinate(trailingX(), other.trailingX())
                && isSameCoordinate(trailingY(), other.trailingY())
                && isSameCoordinate(leadingX(), other.leadingX())
                && isSameCoordinate(leadingY(), other.leadingY());
    }

    public boolean equals(LineVector other, double tolerance) {
        return isSameCoordinate(apexX(), other.apexX(), tolerance)
                && isSameCoordinate(apexY(), other.apexY(), tolerance)
                && isSameCoordinate(trailingX(), other.trailingX(), tolerance)
                && isSameCoordinate(trailingY(), other.trailingY(), tolerance)
                && isSameCoordinate(leadingX(), other.leadingX(), tolerance)
                && isSameCoordinate(leadingY(), other.leadingY(), tolerance);

    }
}
