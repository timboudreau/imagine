package org.imagine.awt;

import java.awt.GradientPaint;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import org.imagine.awt.key.GradientPaintKey;
import org.imagine.awt.key.LinearPaintKey;
import org.imagine.awt.key.PaintKey;
import org.imagine.awt.key.RadialPaintKey;

/**
 *
 * @author Tim Boudreau
 */
public class CachingGradientManager extends GradientManager {

    private final PaintCache<RadialGradientPaint, RadialPaintKey> radialCache
            = new PaintCache<>(RadialPaintKey::new);
    private final PaintCache<LinearGradientPaint, LinearPaintKey> linearCache
            = new PaintCache<>(LinearPaintKey::new);
    private final PaintCache<GradientPaint, GradientPaintKey> gradientCache
            = new PaintCache<>(GradientPaintKey::new);

    @Override
    protected Paint findPaint(Paint orig, int w, int h) {
        if (orig instanceof GradientPaint) {
            return gradientCache.get((GradientPaint) orig);
        } else if (orig instanceof RadialGradientPaint) {
            return radialCache.get((RadialGradientPaint) orig);
        } else if (orig instanceof LinearGradientPaint) {
            return linearCache.get((LinearGradientPaint) orig);
        }
        return orig;
    }

    public boolean isEnabled() {
        return radialCache.isEnabled();
    }

    public void setEnabled(boolean rc) {
        radialCache.setEnabled(rc);
        linearCache.setEnabled(rc);
        gradientCache.setEnabled(rc);
    }

    @Override
    public Paint findPaint(PaintKey<?> key) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
