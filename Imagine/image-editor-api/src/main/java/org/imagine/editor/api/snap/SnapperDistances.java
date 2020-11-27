package org.imagine.editor.api.snap;

import com.mastfrog.util.collections.DoubleMap;
import java.awt.geom.Point2D;
import java.util.Set;
import java.util.function.BiPredicate;
import org.imagine.editor.api.snap.Snapper.BasicSnapper;
import com.mastfrog.geometry.LineVector;

/**
 *
 * @author Tim Boudreau
 */
final class SnapperDistances extends BasicSnapper {

    SnapperDistances() {
        super(SnapKind.DISTANCE);
    }

    private <T> SnapCoordinate<T> handleOne(SnapPoints pts, Thresholds thresholds,
            SnapAxis axis, LineVector vect) {
        DoubleMap<T> map;
        switch (axis) {
            case X:
                map = pts.xDists();
                break;
            case Y:
                map = pts.yDists();
                break;
            default:
                throw new AssertionError(axis);
        }
        double len1 = vect.trailingLineLength();
        double len2 = vect.leadingLineLength();
        SnapCoordinate<T> result1 = handleOneDist(pts, thresholds, axis, vect, len1, map, false);
        SnapCoordinate<T> result2 = handleOneDist(pts, thresholds, axis, vect, len2, map, true);
        SnapCoordinate<T> result = null;
        if (result1 != null && result2 != null) {
            double dist1, dist2;
            switch (axis) {
                case X:
                    dist1 = Math.abs(result1.coordinate() - vect.apexX());
                    dist2 = Math.abs(result2.coordinate() - vect.apexX());
                    break;
                case Y:
                    dist1 = Math.abs(result1.coordinate() - vect.apexY());
                    dist2 = Math.abs(result2.coordinate() - vect.apexY());
                    break;
                default:
                    throw new AssertionError(axis);
            }
            if (dist2 < dist1) {
                result = result2;
            } else {
                result = result1;
            }
        } else if (result1 != null) {
            result = result1;
        } else if (result2 != null) {
            result = result2;
        }
        if (result != null) {
//            System.out.println("SNAP DISTANCE + " + result + " for " + axis);
        }
        return result;
    }

    private <T> SnapCoordinate<T> handleOneDist(SnapPoints pts, Thresholds thresholds,
            SnapAxis axis, LineVector vect, double dist, DoubleMap<T> map, boolean leadingLine) {

        double distanceThreshhold = thresholds.threshold(kind);
        int ix = map.nearestIndexExclusive(dist, distanceThreshhold);
        if (ix >= 0) {
            double val = map.key(ix);
            double originalCoordinate = axis == SnapAxis.X
                    ? vect.apexX() : vect.apexY();
            if (thresholds.pointThreshold() <= (Math.abs(val - originalCoordinate))) {
                double targetCoordinate;
                switch (axis) {
                    case X:
                        if (leadingLine) {
                            targetCoordinate = vect.leadingX();
                        } else {
                            targetCoordinate = vect.trailingX();
                        }
                        break;
                    case Y:
                        if (leadingLine) {
                            targetCoordinate = vect.leadingY();
                        } else {
                            targetCoordinate = vect.trailingY();
                        }
                        break;
                    default:
                        throw new AssertionError(axis);
                }
                return new SnapCoordinate<>(axis, val, kind, map.valueAt(ix),
                        targetCoordinate);
            }
        }
        return null;
    }

    @Override
    protected boolean canSnap(Point2D preceding, Point2D orig, Point2D next,
            Set<SnapKind> allowedKinds, int grid, SnapPoints pts) {
        return preceding != null && orig != null && next != null
                && super.canSnap(preceding, orig, next, allowedKinds, grid, pts);
    }

    @Override
    protected <T> boolean doSnap(Point2D preceding, Point2D orig, Point2D next,
            int grid, Set<SnapKind> allowedKinds, Thresholds thresholds, SnapPoints<T> pts, BiPredicate<SnapCoordinate<T>, SnapCoordinate<T>> snapHandler) {
        LineVector lv = LineVector.of(preceding, orig, next);
        SnapCoordinate<T> xPoint = handleOne(pts, thresholds, SnapAxis.X, lv);
        SnapCoordinate<T> yPoint = handleOne(pts, thresholds, SnapAxis.Y, lv);
        double thresh = thresholds.pointThreshold();
        if (xPoint != null) {
            double diff = Math.abs(orig.getX() -xPoint.coordinate());
            if (diff > thresh) {
                xPoint = null;
            }
        }
        if (yPoint != null) {
            double diff = Math.abs(orig.getY() -yPoint.coordinate());
            if (diff > thresh) {
                yPoint = null;
            }
        }
        if (xPoint == null && yPoint == null) {
            return false;
        }
//        System.out.println("Snap of " + lv + " gets " + xPoint + ", " + yPoint);
        boolean result = false;
        if (xPoint != null || yPoint != null) {
            result = snapHandler.test(xPoint, yPoint);
        }
        return result;
    }
}
