package org.imagine.editor.api;

import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
public enum PaintingStyle {
    OUTLINE_AND_FILL,
    OUTLINE,
    FILL;

    @Override
    public String toString() {
        return NbBundle.getMessage(PaintingStyle.class, name());
    }

    public static PaintingStyle forDrawAndFill(boolean draw, boolean fill) {
        if (!draw && !fill) {
            return OUTLINE_AND_FILL;
        }
        if (draw && fill) {
            return OUTLINE_AND_FILL;
        } else if (draw && !fill) {
            return OUTLINE;
        } else {
            return FILL;
        }
    }

    public PaintingStyle andDrawn() {
        return forDrawAndFill(true, isFill());
    }

    public PaintingStyle andFilled() {
        return forDrawAndFill(isOutline(), true);
    }

    public PaintingStyle notDrawn() {
        return forDrawAndFill(false, isFill());
    }

    public PaintingStyle notFilled() {
        return forDrawAndFill(isOutline(), false);
    }

    public boolean isOutline() {
        switch (this) {
            case OUTLINE:
            case OUTLINE_AND_FILL:
                return true;
            default:
                return false;
        }
    }

    public boolean isFill() {
        switch (this) {
            case FILL:
            case OUTLINE_AND_FILL:
                return true;
            default:
                return false;
        }
    }
}
