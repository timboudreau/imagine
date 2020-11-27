package org.imagine.editor.api.snap;

import com.mastfrog.function.state.Bool;
import com.mastfrog.util.collections.DoubleMap;
import com.mastfrog.util.collections.DoubleMapConsumer;
import com.mastfrog.util.collections.IntMap;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import com.mastfrog.geometry.Angle;
import com.mastfrog.geometry.Circle;
import com.mastfrog.geometry.CornerAngle;
import com.mastfrog.geometry.EqLine;
import com.mastfrog.geometry.EqPointDouble;
import com.mastfrog.geometry.LineVector;

/**
 *
 * @author Tim Boudreau
 */
public final class SnapPoints<T> {

    private final DoubleMap<T> xs;
    private final DoubleMap<T> ys;
    private final double radius;
    private final OnSnap notify;

    private static final DoubleMap<Object> EMPTY_DBLS = DoubleMap.emptyDoubleMap();
    public static final SnapPoints<Object> EMPTY = new SnapPoints(0, null,
            EMPTY_DBLS, EMPTY_DBLS, EMPTY_DBLS, EMPTY_DBLS, EMPTY_DBLS,
            EMPTY_DBLS, EMPTY_DBLS, EMPTY_DBLS);
    private final DoubleMap<T> xDists;
    private final DoubleMap<T> yDists;
    private final DoubleMap<T> angles;
    private final DoubleMap<T> corners;
    private final DoubleMap<T> extents;
    private final DoubleMap<T> lengths;

    SnapPoints(double radius, OnSnap<T> notify, DoubleMap<T> xs, DoubleMap<T> ys,
            DoubleMap<T> xDists, DoubleMap<T> yDists, DoubleMap<T> angles,
            DoubleMap<T> corners, DoubleMap<T> extents, DoubleMap<T> lengths) {
        this.xs = xs;
        this.ys = ys;
        this.radius = radius;
        this.notify = notify;
        this.xDists = xDists;
        this.yDists = yDists;
        this.angles = angles;
        this.corners = corners;
        this.extents = extents;
        this.lengths = lengths;
    }

    double radius() {
        return radius;
    }

    DoubleMap mapFor(SnapKind kind) {
        switch (kind) {
            case ANGLE:
                return angles;
            case CORNER:
                return corners;
            case EXTENT:
                return extents;
            case LENGTH :
                return lengths;
            case GRID:
            case NONE:
            case DISTANCE:
            case POSITION:
            default:
                throw new UnsupportedOperationException(kind.name());
        }
    }

    DoubleMap<T> lengths() {
        return lengths;
    }

    DoubleMap<T> extents() {
        return extents;
    }

    DoubleMap<T> xs() {
        return xs;
    }

    DoubleMap<T> ys() {
        return ys;
    }

    DoubleMap<T> xDists() {
        return xDists;
    }

    DoubleMap<T> yDists() {
        return yDists;
    }

    DoubleMap<T> angles() {
        return angles;
    }

    DoubleMap<T> corners() {
        return corners;
    }

    @SuppressWarnings("unchecked")
    public static <T> SnapPoints<T> emptySnapPoints() {
        return (SnapPoints<T>) EMPTY;
    }

    @Override
    public String toString() {
        return "SnapPoints(X:" + xs + ", Y:" + ys + ")";
    }

    public Point2D snap(Point2D orig) {
        DoubleMap.Entry<? extends T> nearX = xs.nearestValueTo(orig.getX(), radius);
        DoubleMap.Entry<? extends T> nearY = ys.nearestValueTo(orig.getY(), radius);
        if (nearX == null && nearY != null) {
            if (notify != null) {
                notify.onSnap(null, new SnapCoordinate<>(SnapAxis.Y, nearY.key(), SnapKind.POSITION, nearY.value()));
            }
            return new Point2D.Double(orig.getX(), nearY.key());
        } else if (nearY == null && nearX != null) {
            if (notify != null) {
                notify.onSnap(new SnapCoordinate<>(SnapAxis.X, nearX.key(), SnapKind.POSITION, nearX.value()), null);
            }
            return new Point2D.Double(nearX.key(), orig.getY());
        } else if (nearY != null && nearX != null) {
            if (notify != null) {
                notify.onSnap(new SnapCoordinate<>(SnapAxis.X, nearX.key(), SnapKind.POSITION, nearX.value()),
                        new SnapCoordinate<>(SnapAxis.Y, nearY.key(), SnapKind.POSITION, nearY.value()));
            }
            return new Point2D.Double(nearX.key(), nearY.key());
        } else {
            return orig;
        }
    }

    private <T> void visitMiddleOut(IntMap<T> indices, DoubleMap<T> dbls, DoubleMapConsumer<T> c) {
        switch (indices.size()) {
            case 0:
                return;
            case 1:
                c.accept(indices.key(0), dbls.key(indices.key(0)), indices.valueAt(0));
                break;
            case 2:
                c.accept(indices.key(0), dbls.key(indices.key(0)), indices.valueAt(0));
                c.accept(indices.key(1), dbls.key(indices.key(1)), indices.valueAt(1));
                break;
            default:
                int mid = indices.size() / 2;
                int curUp = mid;
                int curDown = mid - 1;
                while (curUp < indices.size() || curDown >= 0) {
                    if (curUp < indices.size()) {
                        c.accept(indices.key(curUp), dbls.key(indices.key(curUp)), indices.valueAt(curUp));
                    }
                    if (curDown >= 0) {
                        c.accept(indices.key(curDown), dbls.key(indices.key(curDown)), indices.valueAt(curDown));
                    }
                    curUp++;
                    curDown--;
                }

        }
    }

    private final Snappers snappers = new Snappers();

    public Point2D snapExclusive(Point2D preceding, Point2D orig, Point2D next,
            int grid, Set<SnapKind> allowedKinds, Thresholds thresholds) {
        EqPointDouble result = new EqPointDouble(orig);
        boolean res = snappers.snap(preceding, orig, next, grid, allowedKinds, thresholds, this, (xpt, ypt) -> {
            boolean snapResult = true;
            if (notify != null) {
                snapResult = notify.onSnap(xpt, ypt);
            }
            if (snapResult) {
                if (xpt != null) {
                    result.x = xpt.coordinate();
                }
                if (ypt != null) {
                    result.y = ypt.coordinate();
                }
            }
            return snapResult;
        });
        return res ? result : orig;
    }

    public Point2D xsnapExclusive(Point2D preceding, Point2D orig, Point2D next,
            int grid, Set<SnapKind> allowedKinds, Thresholds thresholds) {
        if (allowedKinds.isEmpty()) {
            return orig;
        }
        Point2D result = snapSeparateXYTypes(preceding, orig, next, allowedKinds, thresholds);
        if (result == orig && allowedKinds.contains(SnapKind.CORNER) && preceding != null && next != null) {
            LineVector vect = LineVector.of(preceding, orig, next);
            CornerAngle ca = vect.corner();
            double val = ca.encodeSigned();
//            System.out.println("Try cornerAngle " + ca + " " + val + " in " + corners.keySet());

            // Corner angles trailing angles are scaled by 1 million,
            // the second angle is encoded as the fractional portion
            double scanThreshold = thresholds.threshold(SnapKind.CORNER);
//            double min = val - (10 * 10000000D);
//            double max = val + (10 * 10000000D);
            double min = val - (scanThreshold);
            double max = val + (scanThreshold);
            IntMap<T> targets = IntMap.create(20);
            corners.valuesBetween(min, max, (int index, double value, T object) -> {
                targets.put(index, object);
            });
            double threshold = thresholds.pointThreshold();
            Bool done = Bool.create();
            EqPointDouble nue = new EqPointDouble();
            double degreesThreshold = thresholds.threshold(SnapKind.ANGLE);
            visitMiddleOut(targets, corners, (ix, key, t) -> {
                if (done.get()) {
                    return;
                }
                CornerAngle ang = CornerAngle.decodeCornerAngle(key);
                CornerAngle workingAngle = ang;
                double dist = ang.distance(ca);
                if (dist > degreesThreshold) {
                    workingAngle = ang.inverse();
                    dist = workingAngle.distance(ca);
                }
                if (dist <= degreesThreshold) {
                    EqLine ln1 = new EqLine(preceding, orig);
                    EqLine ln2 = new EqLine(orig, next);

                    double trailing = workingAngle.trailingAngle();
                    double leading = workingAngle.leadingAngle();

                    ln1.setAngle(trailing);
                    ln2.setAngle(leading);

                    EqPointDouble newCenter = ln1.intersectionPoint(ln2);
                    if (newCenter.distance(newCenter) < threshold) {
                        if (notify != null) {
                            SnapCoordinate<T> xpt = new SnapCoordinate<>(SnapAxis.X, newCenter.getX(),
                                    SnapKind.CORNER, t);
                            SnapCoordinate<T> ypt = new SnapCoordinate<>(SnapAxis.Y, newCenter.getY(),
                                    SnapKind.CORNER, t);

                            if (notify.onSnap(xpt, ypt)) {
                                nue.setLocation(newCenter);
                                done.set();
                            }
                        } else {
                            nue.setLocation(newCenter);
                            done.set(true);
                        }
                    }
                }
            });
            if (done.get()) {
                result = nue;
            }
        }

        if (result == orig && allowedKinds.contains(SnapKind.ANGLE) && preceding != null && !angles.isEmpty()) {
            // Get a vector for the corner whose apex is being dragged
            LineVector vect = LineVector.of(preceding, orig, next).inverse();
            CornerAngle corn = vect.corner();
            // We only pass angles 0-180 into the collector, since
            // we're interested in the relative angle, notwithstanding
            // whether the start or end point comes first
            double trailingCanon = Angle.canonicalize(corn.trailingAngle());
            double leadingCanon = Angle.canonicalize(corn.leadingAngle());
            // Find stored angles within our threshold

            // XXX complex shapes suck all the oxygen out of the room,
            // so most angle matches when adjusting a point on them
            // are against another point on them, and will be rejected
            // Need to find the range of possible angles and loop until
            // we find a usable one
            // XXX need DoubleMap.nearestIndexTo
            DoubleMap.Entry<? extends T> foundAng1 = angles.nearestValueTo(trailingCanon);
            DoubleMap.Entry<? extends T> foundAng2 = angles.nearestValueTo(leadingCanon);

            // Take whichever has the smallest diff to the target angle
            double diff1 = Math.abs(foundAng1.key() - trailingCanon);
            double diff2 = Math.abs(foundAng2.key() - leadingCanon);
            double thresh = thresholds.threshold(SnapKind.ANGLE);
            DoubleMap.Entry<? extends T> best;
            double targetAngle;
            double diff;
            double dist;
            if (diff1 < diff2) {
                targetAngle = trailingCanon;
                best = foundAng1;
                diff = diff1;
                dist = vect.trailingLineLength();
            } else {
                targetAngle = leadingCanon;
                best = foundAng2;
                diff = diff2;
                dist = vect.leadingLineLength();
            }
            if (diff < thresh) {
                // Collect 4 points - those of the angle in question relative
                // to the previous and next points, and the inverse angle
                // relative to those - we'll take whichever has the smallest
                // diff

                EqPointDouble rel1 = new EqPointDouble();
                EqPointDouble rel2 = new EqPointDouble();
                EqPointDouble rel3 = new EqPointDouble();
                EqPointDouble rel4 = new EqPointDouble();
                double opp = Angle.opposite(targetAngle);
                Circle.positionOf(targetAngle, preceding.getX(), preceding.getY(),
                        dist, rel1::setLocation);
                Circle.positionOf(targetAngle, next.getX(), next.getY(),
                        dist, rel2::setLocation);
                Circle.positionOf(opp, preceding.getX(), preceding.getY(),
                        dist, rel3::setLocation);
                Circle.positionOf(opp, next.getX(), next.getY(),
                        dist, rel4::setLocation);

                EqPointDouble[] pts = new EqPointDouble[]{
                    rel1, rel2, rel3, rel4
                };
                // Sort by nearest to the original point
                Arrays.sort(pts, (a, b) -> {
                    return Double.compare(a.distance(orig), b.distance(orig));
                });
                // Nearest point will be sorted
                EqPointDouble target = pts[0];

                double threshold = thresholds.pointThreshold();
                double targetDistance = target.distance(orig);

                // If it is close enough, snap
                if (targetDistance <= threshold) {
                    if (notify != null) {
                        SnapCoordinate<T> xpt = new SnapCoordinate<>(SnapAxis.X, target.getX(),
                                SnapKind.ANGLE, best.value());
                        SnapCoordinate<T> ypt = new SnapCoordinate<>(SnapAxis.Y, target.getY(),
                                SnapKind.ANGLE, best.value());
                        if (notify.onSnap(xpt, ypt)) {
                            result = target;
                        }
                    } else {
                        result = target;
                    }
                }
            }
        }
        if (grid > 2 && result == orig && allowedKinds.contains(SnapKind.GRID)) {
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
            if (candidate != null && notify != null && (xp != null || yp != null)) {
                if (notify.onSnap(xp, yp)) {
                    result = candidate;
                }
            } else if (notify == null) {
                result = candidate;
            }
        }
        return result;
    }

    public Point2D snapSeparateXYTypes(Point2D preceding, Point2D orig, Point2D next, Set<SnapKind> allowedKinds, Thresholds thresholds) {
        DoubleMap.Entry<? extends T> nearX
                = allowedKinds.contains(SnapKind.POSITION)
                ? xs.nearestValueExclusive(orig.getX(), thresholds.threshold(SnapKind.POSITION))
                : null;
        DoubleMap.Entry<? extends T> nearY
                = allowedKinds.contains(SnapKind.POSITION)
                ? ys.nearestValueExclusive(orig.getY(), thresholds.threshold(SnapKind.POSITION))
                : null;
        SnapKind xKind = SnapKind.POSITION;
        SnapKind yKind = SnapKind.POSITION;
        T xObj = nearX == null ? null : nearX.value();
        T yObj = nearY == null ? null : nearY.value();
        double distanceThreshold = thresholds.threshold(SnapKind.DISTANCE);

        if (nearX == null && allowedKinds.contains(SnapKind.DISTANCE) && preceding != null && next != null) {
            double dist = Math.abs(preceding.getX() - orig.getX());
            DoubleMap.Entry<? extends T> en = xDists.nearestValueExclusive(dist, distanceThreshold);
            if (en != null) {
                nearX = xs.nearestValueExclusive(orig.getX() + dist, distanceThreshold);
                if (nearX == null) {
                    nearX = xs.nearestValueExclusive(orig.getX() - dist, distanceThreshold);
                }
                if (nearX != null) {
                    xObj = nearX.value();
                    xKind = SnapKind.DISTANCE;
                }
            }
            if (nearX == null) {
                dist = Math.abs(next.getX() - orig.getX());
                en = xDists.nearestValueExclusive(dist, distanceThreshold);
                if (en != null) {
                    nearX = xs.nearestValueExclusive(orig.getX() + dist, distanceThreshold);
                    if (nearX == null) {
                        nearX = xs.nearestValueExclusive(orig.getX() - dist, distanceThreshold);
                    }
                    if (nearX != null) {
                        xObj = nearX.value();
                        xKind = SnapKind.DISTANCE;
                    }
                }
            }
        }
        if (nearY == null && allowedKinds.contains(SnapKind.DISTANCE) && preceding != null && next != null) {
            double dist = Math.abs(preceding.getY() - orig.getY());
            DoubleMap.Entry<? extends T> en = yDists.nearestValueExclusive(dist, distanceThreshold);
            if (en != null) {
                nearY = ys.nearestValueExclusive(orig.getY() + dist, distanceThreshold);
                if (nearY == null) {
                    nearY = ys.nearestValueExclusive(orig.getY() - dist, distanceThreshold);
                }
                if (nearY != null) {
                    yObj = nearY.value();
                    yKind = SnapKind.DISTANCE;
                }
            }
            if (nearY == null) {
                dist = Math.abs(next.getY() - orig.getY());
                en = yDists.nearestValueExclusive(dist, distanceThreshold);
                if (en != null) {
                    nearY = ys.nearestValueExclusive(orig.getY() + dist, distanceThreshold);
                    if (nearY == null) {
                        nearY = ys.nearestValueExclusive(orig.getY() - dist, distanceThreshold);
                    }
                    if (nearY != null) {
                        yObj = nearY.value();
                        yKind = SnapKind.DISTANCE;
                    }
                }
            }
        }

        if (nearX == null && nearY != null) {
            if (notify != null) {
                if (!notify.onSnap(null, new SnapCoordinate<>(SnapAxis.Y, nearY.key(), yKind, yObj))) {
                    return orig;
                }
            }
            return new EqPointDouble(orig.getX(), nearY.key());
        } else if (nearY == null && nearX != null) {
            if (notify != null) {
                if (!notify.onSnap(new SnapCoordinate<>(SnapAxis.X, nearX.key(), xKind, xObj), null)) {
                    return orig;
                }
            }
            return new EqPointDouble(nearX.key(), orig.getY());
        } else if (nearY != null && nearX != null) {
            if (notify != null) {
                if (!notify.onSnap(new SnapCoordinate<>(SnapAxis.X, nearX.key(), xKind, xObj),
                        new SnapCoordinate<>(SnapAxis.Y, nearY.key(), yKind, yObj))) {
                    return orig;
                }
            }
            return new EqPointDouble(nearX.key(), nearY.key());
        } else {
            return orig;
        }
    }

    public SnapCoordinate nearestWithinRadius(Point2D p, Thresholds thresholds) {
        double radius = thresholds.threshold(SnapKind.POSITION);
        DoubleMap.Entry<? extends T> nearX = xs.nearestValueTo(p.getX(), radius);
        DoubleMap.Entry<? extends T> nearY = ys.nearestValueTo(p.getY(), radius);
        if (nearX == null && nearY != null) {
            return new SnapCoordinate<>(SnapAxis.Y, nearY.key(), SnapKind.POSITION, nearY.value());
        } else if (nearY == null && nearX != null) {
            return new SnapCoordinate<>(SnapAxis.X, nearX.key(), SnapKind.POSITION, nearX.value());
        } else if (nearY != null && nearX != null) {
            double distX = Math.abs(p.getX() - nearX.key());
            double distY = Math.abs(p.getY() - nearY.key());
            if (distY < distX) {
                return new SnapCoordinate<>(SnapAxis.Y, nearY.key(), SnapKind.POSITION, nearY.value());
            } else {
                return new SnapCoordinate<>(SnapAxis.X, nearX.key(), SnapKind.POSITION, nearX.value());
            }
        } else {
            return null;
        }
    }

    public SnapCoordinate nearest(Point2D p) {
        SnapCoordinate a = nearest(SnapAxis.X, p);
        SnapCoordinate b = nearest(SnapAxis.X, p);
        double da = a.distance(p);
        double db = b.distance(p);
        return da < db ? a : b;
    }

    public SnapCoordinate nearestWithinRadius(SnapAxis axis, Point2D p) {
        DoubleMap<T> pts = axis == SnapAxis.X ? xs : ys;
        DoubleMap.Entry<? extends T> val = pts.nearestValueTo(axis.value(p), radius);
        if (val == null) {
            return null;
        }
        return new SnapCoordinate<>(axis, val.key(), SnapKind.POSITION, val.value());
    }

    public SnapCoordinate nearest(SnapAxis axis, Point2D p) {
        DoubleMap<T> pts = axis == SnapAxis.X ? xs : ys;
        DoubleMap.Entry<? extends T> val = pts.nearestValueTo(axis.value(p));
        if (val == null) {
            return null;
        }
        return new SnapCoordinate<>(axis, val.key(), SnapKind.POSITION, val.value());
    }

    public static <T> SnapPointsBuilder<T> builder(double radius) {
        return new SnapPointsBuilder<>(radius);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.xs);
        hash = 37 * hash + Objects.hashCode(this.ys);
        hash = 37 * hash + (int) (Double.doubleToLongBits(this.radius) ^ (Double.doubleToLongBits(this.radius) >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof SnapPoints<?>)) {
            return false;
        }
        final SnapPoints<?> other = (SnapPoints<?>) obj;
        if (Double.doubleToLongBits(this.radius) != Double.doubleToLongBits(other.radius)) {
            return false;
        }
        if (!Objects.equals(this.xs, other.xs)) {
            return false;
        }
        return Objects.equals(this.ys, other.ys);
    }

}
