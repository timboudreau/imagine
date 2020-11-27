package org.imagine.editor.api.snap;

import com.mastfrog.util.collections.DoubleMap;
import java.awt.geom.Point2D;
import java.util.Set;
import java.util.function.BiPredicate;
import org.imagine.editor.api.snap.Snapper.VectorSnapper;
import com.mastfrog.geometry.Circle;
import com.mastfrog.geometry.CornerAngle;
import com.mastfrog.geometry.EqPointDouble;
import com.mastfrog.geometry.LineVector;
import com.mastfrog.geometry.util.GeometryUtils;

/**
 *
 * @author Tim Boudreau
 */
class SnapperExtents extends VectorSnapper {

    SnapperExtents() {
        super(SnapKind.EXTENT);
    }

    @Override
    protected boolean canSnap(Point2D preceding, Point2D orig, Point2D next, Set<SnapKind> allowedKinds, int grid, SnapPoints pts) {
        return !GeometryUtils.isSamePoint(preceding, orig)
                && !GeometryUtils.isSamePoint(preceding, next)
                && super.canSnap(preceding, orig, next, allowedKinds, grid, pts);
    }

    @Override
    protected <T> boolean snapVector(LineVector vect, DoubleMap<T> map,
            Thresholds thresholds,
            BiPredicate<SnapCoordinate<T>, SnapCoordinate<T>> snapHandler) {

        double ext = vect.corner().extent();
        double thresh = thresholds.threshold(kind);

        double pointThreshold = thresholds.pointThreshold();
        EqPointDouble apex = vect.apex();
        return snapVectors(ext, vect, map, thresh, pointThreshold, apex, snapHandler) /*|| snapVectors(vect.corner().inverse().extent(),
                        vect, map, thresh, pointThreshold, apex, snapHandler)
                 */;
    }

    protected <T> boolean snapVectors(double ext, LineVector vect, DoubleMap<T> map,
            double thresh, double pointThreshold,
            EqPointDouble apex,
            BiPredicate<SnapCoordinate<T>, SnapCoordinate<T>> snapHandler) {

        double min = ext - thresh;
        double max = ext + thresh;

        return visitMiddleOut(map, ext - thresh, ext + thresh, (ix, key, val) -> {
            Circle circ = vect.extentCircle(key);
            EqPointDouble pt = circ.nearestPointTo(vect.apexX(), vect.apexY());

            double ang = circ.angleOf(pt);
            LineVector nue = LineVector.of(vect.trailingPoint(),
                    pt, vect.leadingPoint());
            CornerAngle corn = nue.corner();

            // If we have reversed the vector (wrong hemisphere of the
            // circle), bail and search for another.

            double newExt = corn.extent();
            if (newExt < min || newExt > max) {
                return false;
            }

            if (pt.distance(apex) <= pointThreshold) {
                CornerAngle ca = nue.corner();
                SnapCoordinate<T> xpt = new SnapCoordinate<>(SnapAxis.X, pt.x,
                        kind, val, ca.trailingAngle());
                SnapCoordinate<T> ypt = new SnapCoordinate<>(SnapAxis.Y, pt.y,
                        kind, val, ca.leadingAngle());

                boolean result = snapHandler.test(xpt, ypt);
//                if (result) {
//                    System.out.println(
//                            "ANG " + GeometryStrings.toDegreesStringShort(ang)
//                            + " for trail " + GeometryStrings.toDegreesStringShort(corn.trailingAngle())
//                            + " lead " + GeometryStrings.toDegreesStringShort(corn.leadingAngle())
//                            + " ext " + GeometryStrings.toDegreesStringShort(corn.extent())
//                            + " sectExt " + GeometryStrings.toDegreesStringShort(corn.toSector().extent())
//                            + " targetExt " + GeometryStrings.toDegreesStringShort(ext)
//                    );
//                }
                return result;
            }
            return false;
        });
    }
}
