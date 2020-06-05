package org.imagine.markdown.uiapi;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *
 * @author Tim Boudreau
 */
final class EmbeddedImageCache {

    private static final Map<Markdown, EmbeddedImageCache> cache
            = new WeakHashMap<>();
    private static final ThreadLocal<EmbeddedImageCache> tl = new ThreadLocal<>();

    private Map<String, BufferedImage> imageCache = new HashMap<>();

    public static final <T> T open(Markdown md, Function<EmbeddedImageCache, T> func) {
        EmbeddedImageCache c = tl.get();
        if (c == null) {
            synchronized (cache) {
                c = cache.get(md);
                if (c == null) {
                    c = new EmbeddedImageCache();
                    cache.put(md, c);
                }
            }
        }
        return func.apply(c);
    }

    public BufferedImage get(String url, Supplier<BufferedImage> loader) {
        synchronized (imageCache) {
            BufferedImage img = imageCache.get(url);
            if (img == null) {
                img = loader.get();
                if (img != null) {
                    imageCache.put(url, img);
                }
            }
            return img;
        }
    }
}
