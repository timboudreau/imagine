package net.java.dev.imagine.api.vector.design;

import java.awt.geom.Point2D;
import java.util.Arrays;
import net.java.dev.imagine.api.vector.elements.PathIteratorWrapper;
import org.imagine.geometry.Circle;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Tim Boudreau
 */
@Messages({
    "START_POINT=Path Start",
    "PHYSICAL_POINT=Point",
    "CUBIC_CONTROL_POINT=Cubic Control Point",
    "QUADRATIC_CONTROL_POINT=Quadractic Control Point",
    "LINE_TO_DESTINATION=Straight Line Endpoint",
    "QUADRATIC_CURVE_DESTINATION=Quadratic Curve Endpoint",
    "CUBIC_CURVE_DESTINATION=Cubic Curve Endpoint",
    "RADIUS=Radius",
    "EDGE_HANDLE=Edge Handle",
    "OTHER=Other"
})
public enum ControlPointKind {

    START_POINT, // move to destination
    PHYSICAL_POINT, // point on a rectangle
    LINE_TO_DESTINATION,
    QUADRATIC_CURVE_DESTINATION,
    CUBIC_CURVE_DESTINATION,
    CUBIC_CONTROL_POINT,
    QUADRATIC_CONTROL_POINT,
    RADIUS,
    EDGE_HANDLE,
    OTHER;

    public boolean isPathComponent() {
        switch (this) {
            case START_POINT:
            case LINE_TO_DESTINATION:
            case QUADRATIC_CONTROL_POINT:
            case QUADRATIC_CURVE_DESTINATION:
            case CUBIC_CONTROL_POINT:
            case CUBIC_CURVE_DESTINATION:
                return true;
            default:
                return false;
        }
    }

    public boolean isSplinePoint() {
        switch (this) {
            case QUADRATIC_CONTROL_POINT:
            case CUBIC_CONTROL_POINT:
            case QUADRATIC_CURVE_DESTINATION:
            case CUBIC_CURVE_DESTINATION:
                return true;
            default:
                return false;
        }
    }

    public boolean isCubic() {
        return this == CUBIC_CURVE_DESTINATION || this == CUBIC_CONTROL_POINT;
    }

    public boolean isQuadratic() {
        return this == QUADRATIC_CURVE_DESTINATION || this == QUADRATIC_CONTROL_POINT;
    }

    public boolean isPhysical() {
        switch (this) {
            case START_POINT:
            case PHYSICAL_POINT:
            case LINE_TO_DESTINATION:
            case QUADRATIC_CURVE_DESTINATION:
            case CUBIC_CURVE_DESTINATION:
                return true;
            default:
                return false;
        }
    }

    public boolean isControlPoint() {
        switch (this) {
            case QUADRATIC_CONTROL_POINT:
            case CUBIC_CONTROL_POINT:
                return true;
            default:
                return false;
        }
    }

    public boolean isVirtual() {
        if (isControlPoint()) {
            return false;
        }
        switch (this) {
            case START_POINT:
            case PHYSICAL_POINT:
            case LINE_TO_DESTINATION:
            case QUADRATIC_CURVE_DESTINATION:
            case CUBIC_CURVE_DESTINATION:
                return false;
            default:
                return true;
        }
    }

    public boolean isInitial() {
        return this == START_POINT;
    }

    public boolean changeType(PathIteratorWrapper path, int index) {
        if (!isQuadratic() || isInitial() || isControlPoint()) {
            // If not quadratic, this point is not part of a path

            // If the initial point, that has to be SEG_MOVETO or
            // trying to render the shape will throw an exception

            // We don't allow replacing a control point, since it is
            // not a real point - you have to do this on the physical
            // point
            return false;
        }
        int count = path.getControlPointCount();
        // The model is out of date with the UI - bail out
        if (index >= count) {
            return false;
        }
        // Figure out what kind the current point currently is
        ControlPointKind[] kinds = path.getControlPointKinds();
        ControlPointKind oldKind = kinds[index];
        if (oldKind == this) {
            // Should not happen
            return false;
        }
        // Fetch ALL of the control points into an array
        double[] pts = new double[count * 2];
        path.getControlPoints(pts);
        // Get the current coordinates
        double x = pts[index * 2];
        double y = pts[(index * 2) + 1];
        switch (oldKind) {
            case CUBIC_CONTROL_POINT:
                // Cubic has the most points, so all we do is
                // use two or one of the existing points
                switch (this) {
                    case QUADRATIC_CURVE_DESTINATION:
                        path.setToQuadTo(index, x, y,
                                pts[(index * 2) - 2],
                                pts[(index * 2) - 1]);
                        return true;
                    case LINE_TO_DESTINATION:
                        path.setToLineTo(index, x, y);
                        return true;
                }
            case QUADRATIC_CONTROL_POINT:
                switch (this) {
                    case CUBIC_CONTROL_POINT:
                        // here we need to synthesize a control point
                        // in a reasonable location.  Find the existing
                        // control point, compute the distance, and add
                        // our new point on a line in the oppsite direction
                        double ctrlX = pts[(index * 2) - 2];
                        double ctrlY = pts[(index * 2) - 1];
                        double dist = Point2D.distance(x, y, ctrlX, ctrlY);
                        Circle c = new Circle(x, y, dist);
                        double[] opp = c.positionOf(Circle.opposite(c.angleOf(ctrlX, ctrlY)));
                        path.setToCurveTo(index, ctrlX, ctrlY, opp[0], opp[1], x, y);
                        return true;
                    case LINE_TO_DESTINATION:
                        // We're just getting rid of all the control points
                        path.setToLineTo(index, x, y);
                        return true;
                }
            case LINE_TO_DESTINATION:
                // Here we need to synthesize two reasonable points
                // for initial control points to insert, which will not
                // screw up the shape too much
                int[] concretePoints = path.getConcretePointIndices();
                int indexInConcretePoints = Arrays.binarySearch(concretePoints, index);
                if (indexInConcretePoints < 0) {
                    // should not happen
                    return false;
                }
                // Find the angle of a line from the current point to
                // the non-control point immediately preceding this one,
                // and compute the position of the half-way point along
                // that line from this one
                int prevPoint = indexInConcretePoints - 1;
                double prevX = pts[prevPoint * 2];
                double prevY = pts[(prevPoint * 2) + 1];
                double dist = Point2D.distance(x, y, prevX, prevY);
                Circle circ = new Circle(x, y, dist);
                double angleToPreviousPoint = circ.angleOf(prevX, prevY);
                double[] halfWayToPrecedingPoint = circ.positionOf(angleToPreviousPoint, dist / 2);
                switch (this) {
                    case QUADRATIC_CONTROL_POINT:
                        // For quadratic we have everything we need
                        path.setToQuadTo(index, halfWayToPrecedingPoint[0], halfWayToPrecedingPoint[1], x, y);
                        return true;
                    case CUBIC_CONTROL_POINT:
                        // For cubic, we are starting from a single point and need
                        // to synthese two control points, so do the same thing for
                        // the next concrete point, or wrap around to the first point.
                        //
                        double[] halfWayToNextPoint;
                        if (indexInConcretePoints < concretePoints.length - 1) {
                            // We are not the last point in the path, so look
                            // at the next one
                            int nextPoint = concretePoints[indexInConcretePoints + 1];
                            double nextX = pts[nextPoint * 2];
                            double nextY = pts[(nextPoint * 2) + 1];
                            double angleToNextPoint = circ.angleOf(nextX, nextY);
                            halfWayToNextPoint = circ.positionOf(angleToNextPoint, dist / 2);
                        } else {
                            // we want to instead scan backward to the nearest
                            // SEG_MOVETO, since otherwise if this path contains multiple
                            // shapes, we are wrapping to the start point of some other
                            // shape, not the one we're in
                            int nextPointIndex = 0;
                            for (int i = index; i >= 0; i--) {
                                if (kinds[i].isInitial()) {
                                    nextPointIndex = i;
                                    break;
                                }
                            }
                            int nextPoint = concretePoints[nextPointIndex];
                            double nextX = pts[nextPoint * 2];
                            double nextY = pts[(nextPoint * 2) + 1];
                            double angleToNextPoint = circ.angleOf(nextX, nextY);
                            halfWayToNextPoint = circ.positionOf(angleToNextPoint, dist / 2);
                        }
                        path.setToCurveTo(index, halfWayToPrecedingPoint[0], halfWayToPrecedingPoint[1], halfWayToNextPoint[0], halfWayToNextPoint[1], x, y);
                        return true;
                }
        }
        return false;
    }

    public boolean addDefaultPoint(PathIteratorWrapper path, Point2D near) {
        Point2D nearest = path.nearestPhysicalPointTo(near);
        if (nearest == null) { // very incomplete path
            return false;
        }

        switch (this) {
            case START_POINT:
            case PHYSICAL_POINT:
            case LINE_TO_DESTINATION:
                path.addLineTo(near.getX(), near.getY(), true);
                return true;
        }
        if (isCubic() || isQuadratic()) {
            // XXX need to bring in the path tracer code that replicate's
            // Java2D's path drawing, and get the tangent
            // at the nearest point on the nearest edge to be able to
            // position control points decently
            Circle circ = new Circle(near.getX(), near.getY());
            double ang = circ.angleOf(nearest.getX(), nearest.getY());
            double dist = Point2D.distance(near.getX(), near.getY(), nearest.getX(), nearest.getY());
            double halfDist = dist / 2D;

            double[] pointA = circ.positionOf(ang, halfDist);
            if (isQuadratic()) {
                path.addQuadTo(pointA[0], pointA[1], near.getX(), near.getY(), true);
                return true;
            }
            // fallthrough is cubic - take a point opposite angle
            // half the distance
            double opp = Circle.opposite(ang);
            double[] pointB = circ.positionOf(opp, halfDist);
            path.addCubicTo(pointA[0], pointA[1], pointB[0], pointB[1], near.getX(), near.getY(), true);
            return true;
        } else {
            path.addLineTo(near.getX(), near.getY(), true);
            return true;
        }

    }

    @Override
    public String toString() {
        switch (this) {
            case START_POINT:
                return Bundle.START_POINT();
            case PHYSICAL_POINT:
                return Bundle.PHYSICAL_POINT();
            case CUBIC_CONTROL_POINT:
                return Bundle.CUBIC_CONTROL_POINT();
            case QUADRATIC_CONTROL_POINT:
                return Bundle.QUADRATIC_CONTROL_POINT();
            case RADIUS:
                return Bundle.RADIUS();
            case OTHER:
                return Bundle.OTHER();
            case CUBIC_CURVE_DESTINATION:
                return Bundle.CUBIC_CURVE_DESTINATION();
            case EDGE_HANDLE:
                return Bundle.EDGE_HANDLE();
            case LINE_TO_DESTINATION:
                return Bundle.LINE_TO_DESTINATION();
            case QUADRATIC_CURVE_DESTINATION:
                return Bundle.QUADRATIC_CURVE_DESTINATION();
            default:
                throw new AssertionError(this);
        }
    }
}
