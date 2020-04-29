package org.imagine.awt;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import org.imagine.awt.cache.CacheManager;
import org.imagine.awt.counters.UsageCounter;
import org.imagine.awt.impl.Accessor;
import org.imagine.awt.key.LinearPaintKey;
import org.imagine.awt.key.PaintKey;
import org.imagine.awt.key.RadialPaintKey;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Tim Boudreau
 */
class DefaultGradientManager extends GradientManager {

    private final RadialCache radialCache = new RadialCache();
    private final LinearCache linearCache = new LinearCache();

    DefaultGradientManager() {
        radialCache.disableStackTraces();
        radialCache.disableReferenceTracking();
        radialCache.enable();
    }

    int lastW = 640;
    int lastH = 480;

    @Override
    protected Paint findPaint(Paint orig, int w, int h) {
        lastW = w;
        lastH = h;

        if (orig instanceof RadialGradientPaint) {
            Paint result = radialCache.get((RadialGradientPaint) orig);
            return result;
        } else if (orig instanceof LinearGradientPaint) {
            Paint result = linearCache.get((LinearGradientPaint) orig);
            return result;
        }
        return orig;
    }

    @Override
    public Paint findPaint(PaintKey<?> key) {
        if (key instanceof RadialPaintKey) {
            return radialCache.forKey((RadialPaintKey) key);
        } else if (key instanceof LinearPaintKey) {
            return linearCache.forKey((LinearPaintKey) key);
        }
        return Accessor.rawPaintForPaintKey(key);
    }

    private final Map<Dimension, RadialCache2> radialCacheForAspectRatio
            = new HashMap<>();
    private final Map<Dimension, LinearCache2> linearCacheForAspectRatio
            = new HashMap<>();

    private final Dimension scratch = new Dimension();

    private RadialCache2 radialCache(Dimension d) {
        RadialCache2 result = radialCacheForAspectRatio.get(d);
        if (result == null) {
            result = new RadialCache2(d.width, d.height);
            radialCacheForAspectRatio.put(new Dimension(d), result);
        }
        return result;
    }

    private LinearCache2 linearCache(Dimension d) {
        LinearCache2 result = linearCacheForAspectRatio.get(d);
        if (result == null) {
            result = new LinearCache2(d.width, d.height);
            linearCacheForAspectRatio.put(new Dimension(d), result);
        }
        return result;
    }

    @Override
    public Paint findPaint(PaintKey<?> key, int w, int h) {
        scratch.width = w;
        scratch.height = h;
        if (key instanceof RadialPaintKey) {
            RadialCache2 rc = radialCache(scratch);
            return rc.forKey((RadialPaintKey) key);
        } else if (key instanceof LinearPaintKey) {
            LinearCache2 lc = linearCache(scratch);
            return lc.forKey((LinearPaintKey) key);
        }
        return Accessor.rawPaintForPaintKey(key);
    }

    private static final RequestProcessor EXPIRY_POOL =
            new RequestProcessor("Gradient cache expiry", 3);

    class RadialCache extends CacheManager<Paint, RadialGradientPaint, RadialPaintKey> {

        private Map<RadialGradientPaint, RadialPaintKey> keyCache = new WeakHashMap<>();

        public RadialCache() {
            super(EXPIRY_POOL, Accessor::rawPaintForPaintKey);
        }

        @Override
        protected Supplier<Paint> wrapWithReferenceChecker(Supplier<Paint> origSupplier, UsageCounter counter) {
            return origSupplier;
        }

        @Override
        protected RadialPaintKey keyForOriginal(RadialGradientPaint orig) {
            return keyCache.getOrDefault(orig, new RadialPaintKey(orig));
        }

        @Override
        protected Function<RadialPaintKey, Paint> factoryFor(RadialGradientPaint orig) {
            return (p) -> {
                return texturedForRadial(orig);
            };
        }

        private Paint texturedForRadial(RadialGradientPaint paint) {
            BufferedImage img = GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .getDefaultConfiguration().createCompatibleImage(lastW, lastH);
            Graphics2D g = img.createGraphics();
            try {
                g.setPaint(paint);
                g.fillRect(0, 0, img.getWidth(), img.getHeight());
            } finally {
                g.dispose();
            }
            return new TexturePaint(img, new Rectangle(0, 0, img.getWidth(),
                    img.getHeight()));
        }
    }

    class LinearCache extends CacheManager<Paint, LinearGradientPaint, LinearPaintKey> {

        private Map<LinearGradientPaint, LinearPaintKey> keyCache = new WeakHashMap<>();

        public LinearCache() {
            super(EXPIRY_POOL, Accessor::rawPaintForPaintKey);
        }

        @Override
        protected Supplier<Paint> wrapWithReferenceChecker(Supplier<Paint> origSupplier, UsageCounter counter) {
            return origSupplier;
        }

        @Override
        protected LinearPaintKey keyForOriginal(LinearGradientPaint orig) {
            return keyCache.getOrDefault(orig, new LinearPaintKey(orig));
        }

        @Override
        protected Function<LinearPaintKey, Paint> factoryFor(LinearGradientPaint orig) {
            return (p) -> {
                return texturedForRadial(orig);
            };
        }

        private Paint texturedForRadial(LinearGradientPaint paint) {
            BufferedImage img = GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .getDefaultConfiguration().createCompatibleImage(lastW, lastH);
            Graphics2D g = img.createGraphics();
            try {
                g.setPaint(paint);
                g.fillRect(0, 0, img.getWidth(), img.getHeight());
            } finally {
                g.dispose();
            }
            return new TexturePaint(img, new Rectangle(0, 0, img.getWidth(), img.getHeight()));
        }
    }

    private static final class RadialCache2 extends CacheManager<Paint, RadialGradientPaint, RadialPaintKey> {

        private Map<RadialGradientPaint, RadialPaintKey> keyCache = new WeakHashMap<>();
        private final int w;
        private final int h;

        public RadialCache2(int w, int h) {
            super(EXPIRY_POOL, Accessor::rawPaintForPaintKey);
            this.w = w;
            this.h = h;
        }

        @Override
        protected Supplier<Paint> wrapWithReferenceChecker(Supplier<Paint> origSupplier, UsageCounter counter) {
            return origSupplier;
        }

        @Override
        protected RadialPaintKey keyForOriginal(RadialGradientPaint orig) {
            return keyCache.getOrDefault(orig, new RadialPaintKey(orig));
        }

        @Override
        protected Function<RadialPaintKey, Paint> factoryFor(RadialGradientPaint orig) {
            return (p) -> {
                return texturedForRadial(orig);
            };
        }

        private Paint texturedForRadial(RadialGradientPaint paint) {
            BufferedImage img = GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .getDefaultConfiguration().createCompatibleImage(w, h);
            Graphics2D g = img.createGraphics();
            try {
                g.setPaint(paint);
                g.fillRect(0, 0, img.getWidth(), img.getHeight());
            } finally {
                g.dispose();
            }
            return new TexturePaint(img, new Rectangle(0, 0, img.getWidth(),
                    img.getHeight()));
        }
    }

    static class LinearCache2 extends CacheManager<Paint, LinearGradientPaint, LinearPaintKey> {

        private Map<LinearGradientPaint, LinearPaintKey> keyCache = new WeakHashMap<>();
        private final int w;
        private final int h;

        public LinearCache2(int w, int h) {
            super(EXPIRY_POOL, Accessor::rawPaintForPaintKey);
            this.w = w;
            this.h = h;
        }

        @Override
        protected Supplier<Paint> wrapWithReferenceChecker(Supplier<Paint> origSupplier, UsageCounter counter) {
            return origSupplier;
        }

        @Override
        protected LinearPaintKey keyForOriginal(LinearGradientPaint orig) {
            return keyCache.getOrDefault(orig, new LinearPaintKey(orig));
        }

        @Override
        protected Function<LinearPaintKey, Paint> factoryFor(LinearGradientPaint orig) {
            return (p) -> {
                return texturedForRadial(orig);
            };
        }

        private Paint texturedForRadial(LinearGradientPaint paint) {
            BufferedImage img = GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .getDefaultConfiguration().createCompatibleImage(w, h);
            Graphics2D g = img.createGraphics();
            try {
                g.setPaint(paint);
                g.fillRect(0, 0, img.getWidth(), img.getHeight());
            } finally {
                g.dispose();
            }
            return new TexturePaint(img, new Rectangle(0, 0, img.getWidth(), img.getHeight()));
        }
    }

}
