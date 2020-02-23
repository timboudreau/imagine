package org.imagine.awt.spi;

import java.awt.image.BufferedImage;
import org.imagine.awt.key.PaintKey;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
public abstract class PaintThumbnailFactory {

    static PaintThumbnailFactory getDefault() {
        PaintThumbnailFactory factory = Lookup.getDefault().lookup(PaintThumbnailFactory.class);
        if (factory != null) {
            return factory;
        }
        return DefaultPaintThumbnailFactory.getInstance();
    }

    public abstract BufferedImage thumbnailFor(PaintKey<?> key, int w, int h);
}
