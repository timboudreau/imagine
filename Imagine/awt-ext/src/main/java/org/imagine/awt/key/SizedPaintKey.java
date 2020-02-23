package org.imagine.awt.key;

import java.awt.Paint;
import java.awt.geom.AffineTransform;

/**
 * Superclass for paint keys which have a specific size (such as TexturedPaint).
 *
 * @author Tim Boudreau
 */
public abstract class SizedPaintKey<T extends Paint> extends PaintKey<T> {

    public abstract int width();

    public abstract int height();

    public AffineTransform createScalingTransform(int width, int height) {
        double myWidth = width();
        double myHeight = height();
        double xFactor = width / myWidth;
        double yFactor = height / myHeight;
        return AffineTransform.getScaleInstance(xFactor, yFactor);
    }
}
