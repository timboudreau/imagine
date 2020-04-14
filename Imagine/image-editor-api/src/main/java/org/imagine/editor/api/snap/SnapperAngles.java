package org.imagine.editor.api.snap;

import com.mastfrog.util.collections.DoubleMap;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.function.BiPredicate;
import java.util.function.DoublePredicate;
import org.imagine.geometry.Angle;
import org.imagine.geometry.Circle;
import org.imagine.geometry.CornerAngle;
import org.imagine.geometry.EqPointDouble;
import org.imagine.geometry.LineVector;

/**
 *
 * @author Tim Boudreau
 */
final class SnapperAngles extends Snapper.RangeVisitVectorSnapper {

    SnapperAngles() {
        super(SnapKind.ANGLE);
    }

    @Override
    protected boolean withVectorDerivedValue(LineVector vect, DoublePredicate c) {
        CornerAngle corn = vect.corner();
        double trailingCanon = Angle.canonicalize(corn.trailingAngle());
        double leadingCanon = Angle.canonicalize(corn.leadingAngle());
        boolean result = c.test(trailingCanon);
        if (!result) {
            result = c.test(leadingCanon);
        }
        return result;
    }

    @Override
    protected <T> boolean handleMapValue(double targetAngle, T value,
            LineVector vect, DoubleMap<T> map, Thresholds thresholds,
            BiPredicate<SnapCoordinate<T>, SnapCoordinate<T>> notify) {
        CornerAngle corn = vect.corner();
        double trailingCanon = Angle.canonicalize(corn.trailingAngle());
        double leadingCanon = Angle.canonicalize(corn.leadingAngle());

        double diff1 = Math.abs(targetAngle - trailingCanon);
        double diff2 = Math.abs(targetAngle - leadingCanon);

        double diff;
        double dist;
        double theAngle;
        EqPointDouble otherPoint;
        if (diff1 < diff2) {
            theAngle = trailingCanon;
            diff = diff1;
            dist = vect.trailingLineLength();
//            otherPoint = vect.leadingPoint();
        } else {
            theAngle = leadingCanon;
            diff = diff2;
            dist = vect.leadingLineLength();
//            otherPoint = vect.trailingPoint();
        }

        if (diff > thresholds.threshold(kind)) {
            return false;
        }

        EqPointDouble rel1 = new EqPointDouble();
        EqPointDouble rel2 = new EqPointDouble();
        EqPointDouble rel3 = new EqPointDouble();
        EqPointDouble rel4 = new EqPointDouble();
        double opp = Angle.opposite(theAngle);
        Circle.positionOf(theAngle, vect.trailingX(), vect.trailingY(),
                dist, rel1::setLocation);
        Circle.positionOf(theAngle, vect.leadingX(), vect.leadingY(),
                dist, rel2::setLocation);
        Circle.positionOf(opp, vect.trailingX(), vect.trailingY(),
                dist, rel3::setLocation);
        Circle.positionOf(opp, vect.leadingX(), vect.leadingY(),
                dist, rel4::setLocation);

        EqPointDouble[] pts = new EqPointDouble[]{
            rel1, rel2, rel3, rel4
        };
        Point2D orig = vect.apex();
        // Sort by nearest to the original point
        Arrays.sort(pts, (a, b) -> {
            return Double.compare(a.distance(orig), b.distance(orig));
        });

        // Nearest point will be sorted
        EqPointDouble target = pts[0];

        if (target == rel1 || target == rel3) {
            otherPoint = vect.trailingPoint();
        } else {
            otherPoint = vect.leadingPoint();
        }

        double pointThreshold = thresholds.pointThreshold();
        double targetDistance = target.distance(orig);

        // If it is close enough, snap
        if (targetDistance <= pointThreshold) {
            if (notify != null) {
                SnapCoordinate<T> xpt = new SnapCoordinate<>(SnapAxis.X, target.getX(),
                        SnapKind.ANGLE, value, otherPoint.x);
                SnapCoordinate<T> ypt = new SnapCoordinate<>(SnapAxis.Y, target.getY(),
                        SnapKind.ANGLE, value, otherPoint.y);
                return notify.test(xpt, ypt);
            }
        }

        return false;
    }

}
