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

    default void getBounds(Rectangle2D.Double dest) {
        dest.setFrame(toShape().getBounds());
    }

    Rectangle getBounds();

    Shaped copy();
}
