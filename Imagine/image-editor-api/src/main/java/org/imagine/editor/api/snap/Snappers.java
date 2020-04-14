package org.imagine.editor.api.snap;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 *
 * @author Tim Boudreau
 */
final class Snappers extends Snapper {

    private final List<Snapper> snappers = new ArrayList<>(SnapKind.values().length);

    Snappers() {
        snappers.add(new SnapperExtents());
        snappers.add(new SnapperCorners());
        snappers.add(new SnapperPositions());
        snappers.add(new SnapperLengths());
        snappers.add(new SnapperAngles());
        snappers.add(new SnapperDistances());
        snappers.add(new SnapperGrid());
    }

    @Override
    protected boolean canSnap(Point2D preceding, Point2D orig, Point2D next, Set<SnapKind> allowedKinds, int grid, SnapPoints pts) {
        for (Snapper sn : snappers) {
            if (sn.canSnap(preceding, orig, next, allowedKinds, grid, pts)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected <T> boolean doSnap(Point2D preceding, Point2D orig, Point2D next, int grid, Set<SnapKind> allowedKinds, Thresholds thresholds, SnapPoints<T> pts, BiPredicate<SnapCoordinate<T>, SnapCoordinate<T>> snapHandler) {
        for (Snapper sn : snappers) {
            if (sn.snap(preceding, orig, next, grid, allowedKinds, thresholds, pts, snapHandler)) {
                return true;
            }
        }
        snapHandler.test(null, null);
        return false;
    }
}
