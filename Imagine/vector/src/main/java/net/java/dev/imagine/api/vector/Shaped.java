package net.java.dev.imagine.api.vector;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

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
        Rectangle r = getBounds();
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
}
