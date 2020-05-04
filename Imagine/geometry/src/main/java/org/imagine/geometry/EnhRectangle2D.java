/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.geometry;

import com.mastfrog.function.DoubleBiConsumer;
import com.mastfrog.function.DoubleQuadConsumer;
import com.mastfrog.function.DoubleSextaConsumer;
import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import org.imagine.geometry.util.GeometryStrings;
import org.imagine.geometry.util.GeometryUtils;

/**
 * Extends rectangle with decoration methods and a few other things.
 *
 * @author Tim Boudreau
 */
public class EnhRectangle2D extends Rectangle2D.Double implements EnhancedShape, Tesselable {

    public EnhRectangle2D() {
    }

    public EnhRectangle2D(double x, double y, double w, double h) {
        super(x, y, w, h);
    }

    public EnhRectangle2D(EnhRectangle2D other) {
        super(other.x, other.y, other.width, other.height);
    }

    public EnhRectangle2D(Rectangle2D other) {
        super(other.getX(), other.getY(), other.getWidth(), other.getHeight());
    }

    public EnhRectangle2D copy() {
        return new EnhRectangle2D(this);
    }

    public static EnhRectangle2D of(RectangularShape shape) {
        return shape instanceof EnhRectangle2D ? (EnhRectangle2D) shape
                : new EnhRectangle2D(shape.getX(), shape.getY(), shape.getWidth(), shape.getHeight());
    }

    public void setLocation(Point2D loc) {
        this.x = loc.getX();
        this.y = loc.getY();
    }

    @Override
    public Triangle2D[] tesselate() {
        if (isEmpty()) {
            return new Triangle2D[0];
        }
        double cx = getCenterX();
        double cy = getCenterY();
        return new Triangle2D[]{
            new Triangle2D(cx, cy, x, y, x + width, y),
            new Triangle2D(cx, cy, x + width, y, x + width, y + height),
            new Triangle2D(cx, cy, x + width, y + height, x, y + height),
            new Triangle2D(cx, cy, x, y + height, x, y)
        };
    }

    @Override
    public int indexOfTopLeftmostPoint() {
        return 0;
    }

    @Override
    public EnhRectangle2D getBounds2D() {
        return copy();
    }

    public double diagonalLength() {
        return Point2D.distance(x, y, x + width, y + height);
    }

    @Override
    public EnhRectangle2D getFrame() {
        return getBounds2D();
    }

    public EqPointDouble getLocation() {
        return new EqPointDouble(x, y);
    }

    @Override
    public EnhRectangle2D createUnion(Rectangle2D r) {
        return of(super.createUnion(r));
    }

    @Override
    public EnhRectangle2D createIntersection(Rectangle2D r) {
        return of(super.createIntersection(r));
    }

    @Override
    public boolean normalize() {
        return false;
    }

    @Override
    public void visitPoints(DoubleBiConsumer consumer) {
        consumer.accept(x, y);
        if (!isEmpty()) {
            consumer.accept(x + width, y);
            consumer.accept(x + width, y + height);
            consumer.accept(x, y + height);
        }
    }

    @Override
    public void visitLines(DoubleQuadConsumer consumer) {
        if (isEmpty()) {
            return;
        }
        consumer.accept(x, y, x + width, y);
        consumer.accept(x, y + width, x + width, y + height);
        consumer.accept(x + width, y + width, x, y + height);
        consumer.accept(x, y + height, x, y);
    }

    @Override
    public void visitAdjoiningLines(DoubleSextaConsumer consumer) {
        consumer.accept(x, y, x + width, y, x + width, y + height);
        consumer.accept(x + width, y, x + width, y + height, x, y + height);
        consumer.accept(x + width, y + height, x, y + height, x, y);
        consumer.accept(x, y + height, x, y, x + width, y);
    }

    @Override
    public boolean isClosed() {
        return true;
    }

    @Override
    public boolean isNormalized() {
        return true;
    }

    @Override
    public boolean selfIntersects() {
        return !isEmpty();
    }

    @Override
    public Point2D topLeftPoint() {
        return new EqPointDouble(x, y);
    }

    @Override
    public Point2D point(int index) {
        switch (index) {
            case 0:
                return new EqPointDouble(x, y);
            case 1:
                return new EqPointDouble(x + width, y);
            case 2:
                return new EqPointDouble(x + width, y + height);
            case 3:
                return new EqPointDouble(x, y + height);
            default:
                throw new IndexOutOfBoundsException("" + index);
        }
    }

    @Override
    public int pointCount() {
        return isEmpty() ? 1 : 4;
    }

    public boolean isSquare() {
        return GeometryUtils.isSameCoordinate(width, height);
    }

    public String toString() {
        return GeometryStrings.toString(this);
    }

    public EnhRectangle2D add(Shape shapeForBounds) {
        add(shapeForBounds.getBounds2D());
        return this;
    }

    public EnhRectangle2D add(Shape shapeForBounds, BasicStroke stroke) {
        return add(shapeForBounds, stroke == null ? 0 : stroke.getLineWidth());
    }

    public EnhRectangle2D add(Shape shapeForBounds, double stroke) {
        Rectangle2D bds = shapeForBounds.getBounds2D();
        bds.setFrame(bds.getX() - stroke, bds.getY() - stroke, bds.getX() + bds.getWidth() + stroke, bds.getY() + bds.getHeight() + stroke);
        add(bds);
        return this;
    }
}
