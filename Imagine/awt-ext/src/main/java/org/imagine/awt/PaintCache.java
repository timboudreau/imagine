/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.awt;

import com.mastfrog.abstractions.Wrapper;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;
import org.imagine.awt.cache.CacheManager;
import org.imagine.awt.cache.ReferenceTrackingPaintWrapperSupplierFactory;
import org.imagine.awt.counters.UsageCounter;
import org.imagine.awt.impl.Accessor;
import org.imagine.awt.key.PaintKey;

/**
 *
 * @author Tim Boudreau
 */
final class PaintCache<P extends Paint, K extends PaintKey<P>> extends CacheManager<Paint, P, K> {

    protected static final ExecutorService svc = Executors.newSingleThreadExecutor();
    private final ReferenceTrackingPaintWrapperSupplierFactory referenceWrappers
            = new ReferenceTrackingPaintWrapperSupplierFactory();
    private final Function<P, K> keyMaker;

    public PaintCache(Function<P, K> keyMaker) {
        super(svc, Accessor::rawPaintForPaintKey);
        this.keyMaker = keyMaker;
    }

    @Override
    protected void onExpire(K key, UsageCounter counter) {
        System.out.println("Expire " + key + " " + counter);
        System.out.println("Cumulative: " + cumulativeUsage());
    }

    @Override
    protected Supplier<Paint> wrapWithReferenceChecker(Supplier<Paint> origSupplier, UsageCounter counter) {
        return referenceWrappers.apply(origSupplier, counter);
    }

    @Override
    protected K keyForOriginal(P orig) {
        return keyMaker.apply(orig);
    }

    @Override
    protected Function<K, Paint> factoryFor(P orig) {
        return this::createRasterCachingPaint;
    }

    private Paint createRasterCachingPaint(K paint) {
        return new RasterCachingPaint(Accessor.rawPaintForPaintKey(paint));
    }

    static class RasterCachingPaint<P extends Paint> implements Paint, Wrapper<P> {

        private final P original;

        public RasterCachingPaint(P paint) {
            this.original = paint;
        }

        @Override
        public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) {
            return new WrapperPaintContext(original.createContext(cm, deviceBounds, userBounds, xform, hints));
        }

        @Override
        public int getTransparency() {
            return original.getTransparency();
        }

        @Override
        public P wrapped() {
            return original;
        }

        static class WrapperPaintContext implements PaintContext {

            private final PaintContext orig;

            public WrapperPaintContext(PaintContext orig) {
                this.orig = orig;
            }

            @Override
            public void dispose() {
                orig.dispose();
            }

            @Override
            public ColorModel getColorModel() {
                return orig.getColorModel();
            }

            @Override
            public Raster getRaster(int x, int y, int w, int h) {
                return orig.getRaster(x, y, w, h);
            }
        }
    }
}
