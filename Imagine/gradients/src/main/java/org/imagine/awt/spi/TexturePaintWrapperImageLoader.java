package org.imagine.awt.spi;

import java.awt.MultipleGradientPaint;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import org.imagine.awt.key.MultiplePaintKey;
import org.imagine.awt.key.TexturedPaintWrapperKey;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
public abstract class TexturePaintWrapperImageLoader {

    private static TexturePaintWrapperImageLoader INSTANCE;

    static TexturePaintWrapperImageLoader getDefault() {
        if (INSTANCE == null) {
            INSTANCE = Lookup.getDefault().lookup(TexturePaintWrapperImageLoader.class);
            if (INSTANCE == null) {
                INSTANCE = new DefaultTexturePaintLoader();
            }
        }
        return INSTANCE;
    }

    protected abstract <P extends MultiplePaintKey<T>, T extends MultipleGradientPaint>
            BufferedImage imageFor(TexturedPaintWrapperKey<P, T> key);

    protected <P extends MultiplePaintKey<T>, T extends MultipleGradientPaint>
            TexturePaint paintFor(TexturedPaintWrapperKey<P, T> key) {
        BufferedImage img = imageFor(key);
        TexturePaint paint = new TexturePaint(img, new Rectangle(0, 0, key.width(), key.height()));
        return paint;
    }

}
