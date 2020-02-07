package org.netbeans.paint.tools.fills;

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
