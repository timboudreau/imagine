package org.imagine.utils.java2d;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.imagine.utils.java2d.GraphicsUtils;

/**
 *
 * @author Tim Boudreau
 */
public final class DoubleBuffer {

    private final Supplier<Graphics2D> graphics;
    private final Supplier<Dimension> dim;
    private AtomicReference<BufferedImage> cache = new AtomicReference<>();

    DoubleBuffer(Supplier<Graphics2D> graphics, Supplier<Dimension> dim) {
        this.graphics = graphics;
        this.dim = dim;
    }

    public Graphics2D graphics() {
        Graphics2D g = graphics.get();
        Dimension size = dim.get();
        BufferedImage img = new BufferedImage(size.width, size.height, GraphicsUtils.DEFAULT_BUFFERED_IMAGE_TYPE);

        return GraphicsUtils.tee(g, img.createGraphics(), () -> {
            cache.set(img);
        });
    }

    public BufferedImage cached() {
        return cache.get();
    }

    public void paintCacheInto(Graphics2D g, Runnable ifNotPresent) {
        BufferedImage cache = cached();
        if (cache == null) {
            ifNotPresent.run();
            return;
        }
        g.drawRenderedImage(cache, null);
    }

}
