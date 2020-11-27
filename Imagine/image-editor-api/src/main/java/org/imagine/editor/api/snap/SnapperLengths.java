/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.editor.api.snap;

import com.mastfrog.util.collections.DoubleMap;
import java.awt.geom.Point2D;
import java.util.Set;
import java.util.function.BiPredicate;
import com.mastfrog.geometry.EqLine;
import com.mastfrog.geometry.EqPointDouble;
import com.mastfrog.geometry.LineVector;

/**
 *
 * @author Tim Boudreau
 */
public class SnapperLengths extends Snapper.VectorSnapper {

    SnapperLengths() {
        super(SnapKind.LENGTH);
    }

    @Override
    protected boolean canSnap(Point2D preceding, Point2D orig, Point2D next, Set<SnapKind> allowedKinds, int grid, SnapPoints pts) {
        boolean result = super.canSnap(preceding, orig, next, allowedKinds, grid, pts);
        return result;
    }

    @Override
    protected <T> boolean snapVector(LineVector vect, DoubleMap<T> map,
            Thresholds thresholds,
            BiPredicate<SnapCoordinate<T>, SnapCoordinate<T>> snapHandler) {

        double len1 = vect.trailingLineLength();
        double len2 = vect.leadingLineLength();

        double thresh = thresholds.threshold(SnapKind.LENGTH);

        int ix1 = map.nearestIndexExclusive(len1, thresh);
        int ix2 = map.nearestIndexExclusive(len2, thresh);

        int bestIndex;
        boolean useTrailing = false;
        if (ix1 < 0 && ix2 < 0) {
            return false;
        } else if (ix1 >= 0 && ix2 < 0) {
            bestIndex = ix1;
            useTrailing = true;
        } else if (ix2 >= 0 && ix1 < 0) {
            bestIndex = ix2;
        } else if (ix1 == ix2) {
            bestIndex = ix1;
        } else {
            double v1 = map.key(ix1);
            double v2 = map.key(ix2);
            if (Math.abs(v1 - len1) < Math.abs(v2 - len2)) {
                bestIndex = ix1;
                useTrailing = true;
            } else {
                bestIndex = ix2;
            }
        }
        if (bestIndex >= map.size()) {
            return false;
        }
        EqLine line = useTrailing ? vect.trailingLine() : vect.leadingLine();
        EqPointDouble pt = useTrailing ? line.getP2() : line.getP1();
        EqPointDouble otherPoint = useTrailing ? line.getP1() : line.getP2();
        if (pt.distance(vect.apex()) <= thresholds.pointThreshold()) {
            T val = map.valueAt(bestIndex);
            SnapCoordinate<T> xpt = new SnapCoordinate<>(SnapAxis.X, pt.x,
                    SnapKind.LENGTH, val, otherPoint.x);
            SnapCoordinate<T> ypt = new SnapCoordinate<>(SnapAxis.Y, pt.y,
                    SnapKind.LENGTH, val, otherPoint.y);
            return snapHandler.test(xpt, ypt);
        }
        return false;
    }
}
