package org.imagine.utils.java2d;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.function.Supplier;

/**
 *
 * @author Tim Boudreau
 */
public interface GraphicsProvider extends Supplier<Graphics> {

    /**
     * Get a Graphics2D implementation which should be wrapped
     *
     * @return
     */
    public Graphics2D getGraphics();

    @Override
    default Graphics2D get() {
        return getGraphics();
    }

    /**
     * Called before dispose() is called
     *
     * @param graphics
     */
    default void onDispose(Graphics2D graphics) {
    }

}
