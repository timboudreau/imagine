package org.imagine.editor.api.snap;

import java.awt.geom.Point2D;
import java.util.Set;
import java.util.function.BiPredicate;
import org.imagine.editor.api.snap.Snapper.BasicSnapper;
import org.imagine.geometry.EqPointDouble;

/**
 *
 * @author Tim Boudreau
 */
public class SnapperGrid extends BasicSnapper {

    public SnapperGrid() {
        super(SnapKind.GRID);
    }

    @Override
    protected <T> boolean doSnap(Point2D preceding, Point2D orig, Point2D next,
            int grid, Set<SnapKind> allowedKinds, Thresholds thresholds,
            SnapPoints<T> pts, BiPredicate<SnapCoordinate<T>, SnapCoordinate<T>> notify) {
        double prevX = grid * ((int) (Math.round(orig.getX()) / grid));
        double prevY = grid * ((int) (Math.round(orig.getY()) / grid));
        double nextX = grid * ((int) (Math.round(orig.getX()) / grid) + 1);
        double nextY = grid * ((int) (Math.round(orig.getY()) / grid) + 1);
        double targetRadius = Math.min(thresholds.threshold(SnapKind.GRID),
                grid / 4);
        SnapCoordinate<T> xp = null;
        SnapCoordinate<T> yp = null;

        if (Math.abs(orig.getX() - prevX) <= targetRadius) {
            xp = new SnapCoordinate<>(SnapAxis.X, prevX, SnapKind.GRID, null);
        } else if (Math.abs(orig.getX() - nextX) <= targetRadius) {
            xp = new SnapCoordinate<>(SnapAxis.X, nextX, SnapKind.GRID, null);
        }
        if (Math.abs(orig.getY() - prevY) <= targetRadius) {
            yp = new SnapCoordinate<>(SnapAxis.Y, prevY, SnapKind.GRID, null);
        } else if (Math.abs(orig.getY() - nextY) <= targetRadius) {
            yp = new SnapCoordinate<>(SnapAxis.Y, nextY, SnapKind.GRID, null);
        }
        Point2D candidate = null;
        if (xp != null && yp != null) {
            candidate = new EqPointDouble(xp.coordinate(), yp.coordinate());
        } else if (xp == null && yp != null) {
            candidate = new EqPointDouble(orig.getX(), yp.coordinate());
        } else if (yp == null && xp != null) {
            candidate = new EqPointDouble(xp.coordinate(), orig.getY());
        }
        if (candidate != null && (xp != null || yp != null)) {
            return notify.test(xp, yp);
        }
        return false;
    }
}
