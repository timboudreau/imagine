package net.java.dev.imagine.api.vector;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import org.imagine.geometry.util.GeometryUtils;

/**
 *
 * @author Tim Boudreau
 */
public interface Shaped extends Copyable {

    Shape toShape();

    default void getBounds(Rectangle2D dest) {
        dest.setFrame(toShape().getBounds());
    }

    default void addToBounds(Rectangle2D bds) {
        boolean empty = bds.isEmpty();
        Rectangle r = getBounds();
        if (empty) {
            bds.setFrame(r);
            return;
        }
        double minX = Math.min(r.x, bds.getX());
        double minY = Math.min(r.y, bds.getY());
        double maxX = Math.max(r.x + r.width, bds.getX() + bds.getWidth());
        double maxY = Math.max(r.y + r.height, bds.getY() + bds.getHeight());
        bds.setFrame(minX, minY, maxX - minX, maxY - minY);
    }

    Rectangle getBounds();

    @Override
    Shaped copy();

    Runnable restorableSnapshot();

    default double cumulativeLength() {
        return GeometryUtils.shapeLength(toShape());
    }

    default void collectSizings(SizingCollector c) {

    }

    interface SizingCollector {

        void dimension(double size, boolean vertical, int cpIx1, int cpIx2);
    }

    default void collectAngles(AngleCollector c) {

    }

    interface AngleCollector {

        void angle(double angle, int cpIx1, int cpIx2);
    }
}
