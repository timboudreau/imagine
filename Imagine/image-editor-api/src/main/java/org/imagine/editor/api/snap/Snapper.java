package org.imagine.editor.api.snap;

import com.mastfrog.util.collections.DoubleMap;
import com.mastfrog.util.collections.DoubleMapPredicate;
import java.awt.geom.Point2D;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.DoublePredicate;
import com.mastfrog.geometry.LineVector;
import com.mastfrog.geometry.util.GeometryUtils;

/**
 * A thing which snaps one type of point.
 *
 * @author Tim Boudreau
 */
abstract class Snapper {

    public final <T> boolean snap(Point2D preceding, Point2D orig, Point2D next,
            int grid, Set<SnapKind> allowedKinds, Thresholds thresholds,
            SnapPoints<T> pts,
            BiPredicate<SnapCoordinate<T>, SnapCoordinate<T>> snapHandler) {
        if (canSnap(preceding, orig, next, allowedKinds, grid, pts)) {
            return doSnap(preceding, orig, next, grid, allowedKinds, thresholds,
                    pts, snapHandler);
        }
        return false;
    }

    protected abstract boolean canSnap(Point2D preceding, Point2D orig, Point2D next, Set<SnapKind> allowedKinds, int grid, SnapPoints pts);

    protected abstract <T> boolean doSnap(Point2D preceding, Point2D orig, Point2D next,
            int grid, Set<SnapKind> allowedKinds, Thresholds thresholds,
            SnapPoints<T> pts, BiPredicate<SnapCoordinate<T>, SnapCoordinate<T>> snapHandler);

    protected final <T> boolean visitMiddleOut(DoubleMap<T> map, double min, double max, DoubleMapPredicate<T> c) {
        if (map.isEmpty()) {
            return false;
        }
        return map.visitMiddleOut(min, max, c);
    }

    static abstract class BasicSnapper extends Snapper {

        protected final SnapKind kind;

        protected BasicSnapper(SnapKind kind) {
            this.kind = kind;
        }

        @Override
        protected boolean canSnap(Point2D preceding, Point2D orig, Point2D next, Set<SnapKind> allowedKinds, int grid, SnapPoints pts) {
            if (kind == SnapKind.GRID && grid <= 2) {
                return false;
            }
            return allowedKinds.contains(kind);
        }
    }

    static abstract class VectorSnapper extends BasicSnapper {

        protected VectorSnapper(SnapKind kind) {
            super(kind);
            assert kind.requiresVector();
        }

        @Override
        protected <T> boolean doSnap(Point2D preceding, Point2D orig, Point2D next,
                int grid, Set<SnapKind> allowedKinds, Thresholds thresholds,
                SnapPoints<T> pts, BiPredicate<SnapCoordinate<T>, SnapCoordinate<T>> snapHandler) {
            assert preceding != null && next != null;
            assert allowedKinds.contains(kind);
            LineVector vect = LineVector.of(preceding, orig, next);
            DoubleMap<T> map = pts.mapFor(kind);
            return snapVector(vect, map, thresholds, snapHandler);
        }

        @Override
        protected boolean canSnap(Point2D preceding, Point2D orig, Point2D next, Set<SnapKind> allowedKinds, int grid, SnapPoints pts) {
            boolean result = super.canSnap(preceding, orig, next, allowedKinds, grid, pts);
            if (result) {
                result = preceding != null && orig != null && next != null;
                if (result) {
                    result = !GeometryUtils.isSamePoint(preceding, orig)
                            && !GeometryUtils.isSamePoint(orig, next);
                }
            }
            return result;
        }

        protected abstract <T> boolean snapVector(LineVector vect, DoubleMap<T> map, Thresholds thresholds, BiPredicate<SnapCoordinate<T>, SnapCoordinate<T>> snapHandler);
    }

    static abstract class RangeVisitVectorSnapper extends VectorSnapper {

        public RangeVisitVectorSnapper(SnapKind kind) {
            super(kind);
        }

        protected abstract boolean withVectorDerivedValue(LineVector vect, DoublePredicate c);

        protected abstract <T> boolean handleMapValue(double key, T value, LineVector vect, DoubleMap<T> map,
                Thresholds thresholds,
                BiPredicate<SnapCoordinate<T>, SnapCoordinate<T>> snapHandler);

        protected boolean canSnap(LineVector vect) {
            return true;
        }

        protected <T> boolean scan(double val, DoubleMap<T> map, double min, double max, DoubleMapPredicate<T> pred) {
            return visitMiddleOut(map, min, max, pred);
        }

        @Override
        protected <T> boolean snapVector(LineVector vect, DoubleMap<T> map,
                Thresholds thresholds, BiPredicate<SnapCoordinate<T>, SnapCoordinate<T>> snapHandler) {
            if (!canSnap(vect)) {
                return false;
            }
            double thresh = thresholds.threshold(kind);

            return withVectorDerivedValue(vect, val -> {
                return scan(val, map, val - thresh, val + thresh,
                        (int index, double key, T value) -> {
                            return handleMapValue(key, value, vect, map, thresholds, snapHandler);
                        });
            });
        }
    }
}
