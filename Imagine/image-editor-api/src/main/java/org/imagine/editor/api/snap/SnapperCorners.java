package org.imagine.editor.api.snap;

import com.mastfrog.util.collections.DoubleMap;
import com.mastfrog.util.collections.DoubleMapPredicate;
import java.util.function.BiPredicate;
import java.util.function.DoublePredicate;
import org.imagine.geometry.CornerAngle;
import org.imagine.geometry.EqPointDouble;
import org.imagine.geometry.LineVector;

/**
 *
 * @author Tim Boudreau
 */
class SnapperCorners extends Snapper.RangeVisitVectorSnapper {

    SnapperCorners() {
        super(SnapKind.CORNER);
    }

    @Override
    protected boolean canSnap(LineVector vect) {
        return !vect.corner().isExtreme();
    }

    @Override
    protected boolean withVectorDerivedValue(LineVector vect, DoublePredicate c) {
        // XXX who else is inverting this that we're compensating for here?
//        double val = vect.corner().inverse().encodeSigned();
        double val = vect.corner().encodeSigned();
        return c.test(val);
    }

    @Override
    protected <T> boolean scan(double val, DoubleMap<T> map, double min, double max, DoubleMapPredicate<T> pred) {
        boolean result = super.scan(val, map, min, max, pred);
        // Here, our maximum value adding in the threshold can be > 360 degrees,
        // in which case scan from 0 degrees to max-360, OR
        // our minimum value can be < 0 in which case we want to scan from
        // 360 - minDegrees to 360
        if (!result) {
            double maxDegrees = max / CornerAngle.ENCODING_MULTIPLIER;
            if (maxDegrees > 360) {
                double max1 = (maxDegrees - 360) * CornerAngle.ENCODING_MULTIPLIER;
                result = super.scan(val, map, map.key(0) - 1, max1, pred);
            }
        }
        if (!result && map.size() > 0) {
            double minDegrees = min / CornerAngle.ENCODING_MULTIPLIER;
            if (minDegrees < 0) {
                double min1 = (360 + minDegrees) * CornerAngle.ENCODING_MULTIPLIER;
                result = super.scan(val, map, min1, map.key(map.size() - 1) + 1, pred);
            }
        }
        return result;
    }

    @Override
    protected <T> boolean handleMapValue(double key, T value, LineVector vect,
            DoubleMap<T> map, Thresholds thresholds,
            BiPredicate<SnapCoordinate<T>, SnapCoordinate<T>> notify) {
        double degreesThreshold = thresholds.threshold(SnapKind.ANGLE);
        CornerAngle ca = vect.corner();
        CornerAngle ang = CornerAngle.decodeCornerAngle(key);
        CornerAngle workingAngle = ang;
        double dist = ang.distance(ca);
        if (dist > degreesThreshold) {
            workingAngle = ang.inverse();
            dist = workingAngle.distance(ca);
//            vect = vect.inverse();
        }
        if (true || dist <= degreesThreshold * 2) {
            LineVector lv2
                    = workingAngle.toLineVector(vect.trailingPoint(),
                            vect.leadingPoint());

            double threshold = thresholds.pointThreshold();

            EqPointDouble newCenter = lv2.apex();
            if (newCenter.distance(vect.apex()) < threshold) {
                double sig = ang.encodeSigned();
                SnapCoordinate<T> xpt = new SnapCoordinate<>(SnapAxis.X, newCenter.getX(),
                        SnapKind.CORNER, value, sig);
                SnapCoordinate<T> ypt = new SnapCoordinate<>(SnapAxis.Y, newCenter.getY(),
                        SnapKind.CORNER, value, sig);
                return notify.test(xpt, ypt);
            }
        }
        return false;
    }

}
