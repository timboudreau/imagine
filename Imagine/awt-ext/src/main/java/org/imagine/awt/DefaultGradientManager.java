package org.imagine.awt;

import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import org.imagine.awt.cache.CacheManager;
import org.imagine.awt.cache.ReferenceTrackingPaintWrapperSupplierFactory;
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

    class RadialCache extends CacheManager<Paint, RadialGradientPaint, RadialPaintKey> {

        public RadialCache() {
            super(RequestProcessor.getDefault(), Accessor::rawPaintForPaintKey);
        }
        private final ReferenceTrackingPaintWrapperSupplierFactory referenceWrappers
                = new ReferenceTrackingPaintWrapperSupplierFactory();

        private Map<RadialGradientPaint, RadialPaintKey> keyCache = new WeakHashMap<>();

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

        public LinearCache() {
            super(RequestProcessor.getDefault(), Accessor::rawPaintForPaintKey);
        }
        private final ReferenceTrackingPaintWrapperSupplierFactory referenceWrappers
                = new ReferenceTrackingPaintWrapperSupplierFactory();

        private Map<LinearGradientPaint, LinearPaintKey> keyCache = new WeakHashMap<>();

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

}
