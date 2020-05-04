package org.netbeans.paint.tools.path;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.imagine.geometry.EqPointDouble;

/**
 * A point editor that can update a previously created point along the path of a
 * vector shape.
 */
final class PointEditor implements Hit {

    final EqPointDouble point;
    final Rectangle bds;
    final Consumer<Rectangle> onChange;
    final Supplier<PointEditor> nextPointEditor;

    /**
     * Create a new instance
     *
     * @param point The point
     * @param bds A bounding rectangle to modify if the point changes.
     * @param onChange A consumer which should be called with the bounding
     * rectangle when it changes to trigger a repaint
     * @param nextPointEditor A supplier which will restore the state to
     * whatever the next point editor would have been had the user not stopped
     * to edit this point
     */
    PointEditor(EqPointDouble point, Rectangle bds, Consumer<Rectangle> onChange, Supplier<PointEditor> nextPointEditor) {
        this.point = point;
        this.bds = bds;
        this.onChange = onChange;
        this.nextPointEditor = nextPointEditor;
    }

    @Override
    public <T> T get(Class<T> type) {
        if (Point2D.class == type) {
            return type.cast(point);
        } else if (EqPointDouble.class == type) {
            return type.cast(point);
        }
        return Hit.super.get(type);
    }

    public void updatePoint(double x, double y) {
        point.setLocation(x, y);
        bds.add(point);
        onChange.accept(bds);
    }

    public void shiftX(double dx) {
        point.x += dx;
        bds.add(point);
        onChange.accept(bds);
    }

    public void shiftY(double dy) {
        point.y += dy;
        bds.add(point);
        onChange.accept(bds);
    }

    PointEditor setPoint(double x, double y) {
        point.setLocation(x, y);
        bds.add(point);
        onChange.accept(bds);
        return nextPointEditor.get();
    }
}
