package org.imagine.awt.key;

import com.sun.prism.j2d.paint.RadialGradientPaint;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.TexturePaint;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;

/**
 * Built in kinds of paint keys
 *
 * @author Tim Boudreau
 */
@Messages({
    "COLOR=Color",
    "GRADIENT=Gradient",
    "LINEAR_GRADIENT=Linear Gradient",
    "RADIAL_GRADIENT=Radial Gradient",
    "TEXTURE=Texture",
    "UNKNOWN=Unknown"
})
public enum StandardPaintKeyKinds implements PaintKeyKind {

    COLOR,
    GRADIENT,
    LINEAR_GRADIENT,
    RADIAL_GRADIENT,
    TEXTURE,
    UNKNOWN;

    public boolean isGradient() {
        return this != COLOR && this != TEXTURE;
    }

    @Override
    public String kindName() {
        return toString();
    }

    @Override
    public String toString() {
        return NbBundle.getMessage(StandardPaintKeyKinds.class, name());
    }

    public Class<? extends Paint> type() {
        switch (this) {
            case COLOR:
                return Color.class;
            case GRADIENT:
                return GradientPaint.class;
            case LINEAR_GRADIENT:
                return LinearGradientPaint.class;
            case RADIAL_GRADIENT:
                return RadialGradientPaint.class;
            case TEXTURE:
                return TexturePaint.class;
            default:
                return Paint.class;
        }
    }
}
