package org.imagine.awt.key;

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

    public String toString() {
        return NbBundle.getMessage(StandardPaintKeyKinds.class, name());
    }
}
