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
    "MATCH=Position",
    "DISTANCE=Size",
    "GRID=Grid",
    "ANGLE=Angle",
    "CORNER=Corner",
    "NONE=None"
})
public enum SnapKind {
    MATCH,
    DISTANCE,
    GRID,
    ANGLE,
    CORNER,
    NONE;

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
