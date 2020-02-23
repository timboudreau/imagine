package org.imagine.awt.cache;

import com.mastfrog.abstractions.Wrapper;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import org.imagine.awt.counters.UsageCounter;

/**
 * Do not use except for leak debugging; using a non-standard paint
 * disables all sorts of optimizations in the java 2d pipeline.
 *
 * @author Tim Boudreau
 */
public final class ReferenceTrackingWrapperPaint implements Paint, Wrapper<Paint> {

    private final Paint delegate;
    private final UsageCountingReference ref;

    public ReferenceTrackingWrapperPaint(Paint delegate, UsageCounter counter) {
        this.delegate = delegate;
        this.ref = new UsageCountingReference(this, counter);
    }

    UsageCountingReference reference() {
        return ref;
    }

    @Override
    public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) {
        return delegate.createContext(cm, deviceBounds, userBounds, xform, hints);
    }

    @Override
    public int getTransparency() {
        return delegate.getTransparency();
    }

    @Override
    public Paint wrapped() {
        return delegate;
    }

}
