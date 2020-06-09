package org.imagine.markdown.uiapi.graphics;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.function.Consumer;

/**
 *
 * @author Tim Boudreau
 */
final class GenericPaintItem implements PaintItem {

    final Consumer<Graphics2D> consumer;
    final Rectangle2D bounds;

    public GenericPaintItem(Consumer<Graphics2D> consumer, Rectangle2D bounds) {
        this.consumer = consumer;
        this.bounds = bounds;
    }

    @Override
    public void paint(Graphics2D g) {
        consumer.accept(g);
    }

    @Override
    public void fetchBounds(Rectangle2D into) {
        if (bounds != null) {
            into.setFrame(bounds);
        }
    }

}
