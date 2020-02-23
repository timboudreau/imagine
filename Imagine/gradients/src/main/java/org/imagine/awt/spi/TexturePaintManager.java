package org.imagine.awt.spi;

import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import org.imagine.awt.key.ManagedTexturePaintKey;
import org.imagine.awt.key.PaintKey;
import org.imagine.awt.key.TexturePaintKey;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
public abstract class TexturePaintManager {

    private static TexturePaintManager INSTANCE;

    protected abstract BufferedImage imageFor(String id, ManagedTexturePaintKey key);

    protected boolean isManaged(TexturePaint paint) {
        return false;
    }

    protected abstract byte[] hashAndSave(BufferedImage img);

    protected abstract TexturePaint paintFor(ManagedTexturePaintKey key);

    protected abstract PaintKey<TexturePaint> keyFor(TexturePaint paint);

    static TexturePaintManager getDefault() {
        if (INSTANCE == null) {
            INSTANCE = Lookup.getDefault().lookup(TexturePaintManager.class);
            if (INSTANCE == null) {
                INSTANCE = new DefaultTexturePaintManager();
            }
        }
        return INSTANCE;
    }

    protected abstract ManagedTexturePaintKey toManagedKey(TexturePaintKey key);
}
