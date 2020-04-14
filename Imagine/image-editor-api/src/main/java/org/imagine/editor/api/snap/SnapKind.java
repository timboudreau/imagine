package org.imagine.editor.api.snap;

import java.util.EnumSet;
import java.util.Set;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Tim Boudreau
 */
@Messages({
    "POSITION=Position",
    "DISTANCE=Size",
    "GRID=Grid",
    "ANGLE=Angle",
    "CORNER=Corner",
    "EXTENT=Extent",
    "LENGTH=Lengths",
    "NONE=None"
})
public enum SnapKind {
    GRID,
    POSITION,
    ANGLE,
    EXTENT,
    LENGTH,
    DISTANCE,
    CORNER,
    NONE;

    public boolean canSnapSingleAxis() {
        return this == POSITION || this == DISTANCE;
    }

    boolean requiresVector() {
        switch (this) {
            case POSITION:
            case GRID:
                return false;
            case LENGTH:
            case ANGLE:
            case CORNER:
            case DISTANCE:
            case EXTENT :
                return true;
            case NONE:
                return false;
            default:
                throw new AssertionError(this);
        }
    }

    @Override
    public String toString() {
        return NbBundle.getMessage(SnapKind.class, name());
    }

    public boolean isNoSnap() {
        return this == NONE;
    }

    public static Set<SnapKind> kinds(boolean includeGrid) {
        Set<SnapKind> result = EnumSet.noneOf(SnapKind.class);
        for (SnapKind kind : values()) {
            switch (kind) {
                case NONE:
                    continue;
                case GRID:
                    if (!includeGrid) {
                        continue;
                    }
                default:
                    result.add(kind);
            }
        }
        return result;
    }
}
