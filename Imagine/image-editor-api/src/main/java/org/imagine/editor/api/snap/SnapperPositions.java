package org.imagine.editor.api.snap;

import com.mastfrog.util.collections.DoubleMap;
import java.awt.geom.Point2D;
import java.util.Set;
import java.util.function.BiPredicate;
import org.imagine.editor.api.snap.Snapper.BasicSnapper;

/**
 *
 * @author Tim Boudreau
 */
final class SnapperPositions extends BasicSnapper {

    SnapperPositions() {
        super(SnapKind.POSITION);
    }

    @Override
    protected <T> boolean doSnap(Point2D preceding, Point2D orig, Point2D next,
            int grid, Set<SnapKind> allowedKinds, Thresholds thresholds,
            SnapPoints<T> pts, BiPredicate<SnapCoordinate<T>, SnapCoordinate<T>> notify) {
        DoubleMap.Entry<? extends T> nearX = pts.xs().nearestValueExclusive(orig.getX(), thresholds.threshold(SnapKind.POSITION));
        DoubleMap.Entry<? extends T> nearY = pts.ys().nearestValueExclusive(orig.getY(), thresholds.threshold(SnapKind.POSITION));

        if (nearX == null && nearY != null) {
            return notify.test(null,
                    new SnapCoordinate<>(SnapAxis.Y, nearY.key(), kind, nearY.value()));
        } else if (nearY == null && nearX != null) {
            return notify.test(new SnapCoordinate<>(SnapAxis.X, nearX.key(), kind, nearX.value()), null);
        } else if (nearY != null && nearX != null) {
            return notify.test(
                    new SnapCoordinate<>(SnapAxis.X, nearX.key(), kind, nearX.value()),
                    new SnapCoordinate<>(SnapAxis.Y, nearY.key(), kind, nearY.value()));
        }
        return false;
    }

}
