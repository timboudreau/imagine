package org.imagine.editor.api;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Size and aspect ratio information for the editor area, needed for drawing
 * previews where a point is to be located within an area.
 *
 * @author Tim Boudreau
 */
public interface AspectRatio {

    double width();

    double height();

    default Rectangle2D.Double rectangle() {
        return new Rectangle2D.Double(0, 0, width(), height());
    }

    default double diagonal() {
        return Point2D.distance(0F, 0F, (float) width(), (float) height());
    }

    default double maxDimension() {
        return Math.max(width(), height());
    }

    default double minDimension() {
        return Math.min(width(), height());
    }

    static AspectRatio create(Supplier<Dimension> dim, BooleanSupplier flex) {
        return new SimpleAspectRatio(dim, flex);
    }

    static AspectRatio create(Supplier<Dimension> dim) {
        return new SimpleAspectRatio(dim, null);
    }

    static AspectRatio create(Dimension dim) {
        return new SimpleAspectRatio(() -> dim, null);
    }

    default boolean isFlexible() {
        return false;
    }

    default double fraction() {
        double w = width();
        double h = height();
        if (w == 0 || h == 0) {
            return 1;
        }
        return width() / height();
    }

    default Dimension size(double width) {
        double w = Math.ceil(width);
        double h = Math.ceil(width * fraction());
        return new Dimension((int) Math.round(w), (int) Math.round(h));
    }
}
