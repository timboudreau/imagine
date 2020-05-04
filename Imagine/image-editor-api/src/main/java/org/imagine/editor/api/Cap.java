package org.imagine.editor.api;

import java.awt.BasicStroke;
import org.openide.util.NbBundle;

/**
 * Enum for stroke int constants in BasicStroke.
 *
 * @author Tim Boudreau
 */
@NbBundle.Messages(value = {"ROUND=Round", "BUTT=Butt", "SQUARE=Square"})
public enum Cap {
    BUTT, ROUND, SQUARE;

    public static Cap forStroke(BasicStroke stroke) {
        switch (stroke.getEndCap()) {
            case BasicStroke.CAP_BUTT:
                return BUTT;
            case BasicStroke.CAP_ROUND:
                return ROUND;
            case BasicStroke.CAP_SQUARE:
                return SQUARE;
            default:
                throw new AssertionError("Unknown stroke type " + stroke.getEndCap());
        }
    }

    public int constant() {
        switch (this) {
            case BUTT:
                return BasicStroke.CAP_BUTT;
            case ROUND:
                return BasicStroke.CAP_ROUND;
            case SQUARE:
                return BasicStroke.CAP_SQUARE;
            default:
                throw new AssertionError(this);
        }
    }

    @Override
    public String toString() {
        return NbBundle.getMessage(Join.class, name());
    }
}
