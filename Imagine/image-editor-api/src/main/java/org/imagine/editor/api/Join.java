package org.imagine.editor.api;

import java.awt.BasicStroke;
import org.openide.util.NbBundle;

/**
 * Enum for stroke int constants in BasicStroke.
 *
 * @author Tim Boudreau
 */
@NbBundle.Messages(value = {"BEVEL=Bevel", "MITER=Miter"})
public enum Join {
    MITER, ROUND, BEVEL;

    public int constant() {
        switch (this) {
            case BEVEL:
                return BasicStroke.JOIN_BEVEL;
            case MITER:
                return BasicStroke.JOIN_MITER;
            case ROUND:
                return BasicStroke.JOIN_ROUND;
            default:
                throw new AssertionError(this);
        }
    }

    public static Join forStroke(BasicStroke stroke) {
        switch (stroke.getLineJoin()) {
            case BasicStroke.JOIN_BEVEL:
                return BEVEL;
            case BasicStroke.JOIN_MITER:
                return MITER;
            case BasicStroke.JOIN_ROUND:
                return ROUND;
            default:
                throw new AssertionError("Unknown line join type " + stroke.getLineJoin());
        }
    }

    public String toString() {
        return NbBundle.getMessage(Join.class, name());
    }

}
