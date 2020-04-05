package org.imagine.editor.api.snap;

import com.mastfrog.function.state.Bool;
import com.mastfrog.util.collections.DoubleMap;
import com.mastfrog.util.collections.DoubleMapConsumer;
import com.mastfrog.util.collections.IntMap;
import java.awt.geom.Point2D;
import java.util.Objects;
import java.util.Set;
import org.imagine.geometry.Circle;
import org.imagine.geometry.CornerAngle;
import org.imagine.geometry.EqLine;
import org.imagine.geometry.EqPointDouble;
import org.imagine.geometry.LineVector;

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
    public static final SnapPoints<Object> EMPTY = new SnapPoints(0, null, EMPTY_DBLS, EMPTY_DBLS, EMPTY_DBLS, EMPTY_DBLS, EMPTY_DBLS, EMPTY_DBLS);
    private final DoubleMap<T> xDists;
    private final DoubleMap<T> yDists;
    private final DoubleMap<T> angles;
    private final DoubleMap<T> corners;

    SnapPoints(double radius, OnSnap<T> notify, DoubleMap<T> xs, DoubleMap<T> ys,
            DoubleMap<T> xDists, DoubleMap<T> yDists, DoubleMap<T> angles,
            DoubleMap<T> corners) {
        this.xs = xs;
        this.ys = ys;
        this.radius = radius;
        this.notify = notify;
        this.xDists = xDists;
        this.yDists = yDists;
        this.angles = angles;
        this.corners = corners;
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
                notify.onSnap(null, new SnapPoint<>(SnapAxis.Y, nearY.key(), SnapKind.MATCH, nearY.value()));
            }
            return new Point2D.Double(orig.getX(), nearY.key());
        } else if (nearY == null && nearX != null) {
            if (notify != null) {
                notify.onSnap(new SnapPoint<>(SnapAxis.X, nearX.key(), SnapKind.MATCH, nearX.value()), null);
            }
            return new Point2D.Double(nearX.key(), orig.getY());
        } else if (nearY != null && nearX != null) {
            if (notify != null) {
                notify.onSnap(new SnapPoint<>(SnapAxis.X, nearX.key(), SnapKind.MATCH, nearX.value()),
                        new SnapPoint<>(SnapAxis.Y, nearY.key(), SnapKind.MATCH, nearY.value()));
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

    public Point2D snapExclusive(Point2D preceding, Point2D orig, Point2D next, int grid, Set<SnapKind> allowedKinds) {
        if (allowedKinds.isEmpty()) {
            return orig;
        }
        Point2D result = snapExclusive(orig, allowedKinds);
        if (result == orig && allowedKinds.contains(SnapKind.CORNER) && preceding != null && next != null) {
            LineVector vect = LineVector.of(preceding, orig, next);
            CornerAngle ca = vect.corner();
            double val = ca.encodeSigned();
//            System.out.println("Try cornerAngle " + ca + " " + val + " in " + corners.keySet());

            // Corner angles trailing angles are scaled by 1 million,
            // the second angle is encoded as the fractional portion
            double min = val - (10 * 10000000D);
            double max = val + (10 * 10000000D);
            IntMap<T> targets = IntMap.create(20);
            corners.valuesBetween(min, max, (int index, double value, T object) -> {
                targets.put(index, object);
            });
            double threshold = 10; // xxx scale to zoom
            Bool done = Bool.create();
            EqPointDouble nue = new EqPointDouble();
            visitMiddleOut(targets, corners, (ix, key, t) -> {
                if (done.get()) {
                    return;
                }
                CornerAngle ang = CornerAngle.decodeCornerAngle(key);
                double dist = ang.distance(ca);
                if (dist <= 15) {
                    EqLine ln1 = new EqLine(preceding, orig);
                    EqLine ln2 = new EqLine(orig, next);

                    ln1.setAngleAndLength(ang.trailingAngle(), ln1.length());
                    ln2.setAngleAndLength(ang.leadingAngle(), ln2.length());

                    EqPointDouble newCenter = ln1.intersectionPoint(ln2);
                    if (newCenter.distance(newCenter) < threshold) {
                        if (notify != null) {
                            SnapPoint<T> xpt = new SnapPoint<>(SnapAxis.X, newCenter.getX(),
                                    SnapKind.CORNER, t);
                            SnapPoint<T> ypt = new SnapPoint<>(SnapAxis.Y, newCenter.getY(),
                                    SnapKind.CORNER, t);

                            System.out.println("snap to corner " + ang + " " + t);

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

        if (result == orig && allowedKinds.contains(SnapKind.ANGLE) && preceding != null) {
            LineVector vect = LineVector.of(preceding, orig, next).inverse();
            CornerAngle corn = vect.corner();
            DoubleMap.Entry<? extends T> foundAng1 = angles.nearestValueTo(corn.trailingAngle());
            DoubleMap.Entry<? extends T> foundAng2 = angles.nearestValueTo(corn.leadingAngle());

            double diff1 = Math.abs(foundAng1.key() - corn.trailingAngle());
            double diff2 = Math.abs(foundAng2.key() - corn.leadingAngle());
            DoubleMap.Entry<? extends T> best;
            double targetAngle;
            double diff;
            double dist;
            if (diff1 < diff2) {
                targetAngle = corn.trailingAngle();
                best = foundAng1;
                diff = diff1;
                dist = vect.firstLineLength();
            } else {
                targetAngle = corn.leadingAngle();
                best = foundAng2;
                diff = diff2;
                dist = vect.secondLineLength();
            }
            if (diff < 15) {
                EqPointDouble pt = new EqPointDouble();
                Circle.positionOf(targetAngle, orig.getX(), orig.getY(), dist, pt::setLocation);

                // XXX maybe also test the distance to this particular
                // point, so we don't shove the dragged point WAY off-target?
                if (notify != null) {
                    SnapPoint<T> xpt = new SnapPoint<>(SnapAxis.X, pt.getX(),
                            SnapKind.ANGLE, best.value());
                    SnapPoint<T> ypt = new SnapPoint<>(SnapAxis.Y, pt.getY(),
                            SnapKind.ANGLE, best.value());
                    if (notify.onSnap(xpt, ypt)) {
                        result = pt;
                    }
                } else {
                    result = pt;
                }
            }
        }
//        if (result == orig && next != null && allowedKinds.contains(SnapKind.ANGLE)) {
//            Circle circ = new Circle(next.getX(), next.getY(),
//                    Point2D.distance(next.getX(), next.getY(), orig.getX(),
//                            orig.getY()));
//            double angle = circ.angleOf(orig.getX(), orig.getY());
//            DoubleMap.Entry<? extends T> foundAng = angles.nearestValueTo(angle, 5);
//
//            if (foundAng == null) {
//                foundAng = angles.nearestValueTo(Angle.opposite(angle), 5);
//            }
//
//            System.out.println("Try angle " + angle + " in " + angles.keySet() + " result " + foundAng);
//
//            if (foundAng != null) {
//                Point2D pos = circ.getPosition(foundAng.key());
//                if (Point2D.distance(pos.getX(), pos.getY(), orig.getX(), orig.getY()) <= radius) {
//                    if (notify != null) {
//                        SnapPoint<T> xpt = new SnapPoint<>(SnapAxis.X, result.getX(),
//                                SnapKind.ANGLE, foundAng.value());
//                        SnapPoint<T> ypt = new SnapPoint<>(SnapAxis.Y, result.getY(),
//                                SnapKind.ANGLE, foundAng.value());
//                        if (notify.onSnap(xpt, ypt)) {
//                            result = pos;
//                        }
//                    } else {
//                        result = pos;
//                    }
//                }
//            }
//        }
        if (grid > 2 && result == orig && allowedKinds.contains(SnapKind.GRID)) {
            double prevX = grid * ((int) (Math.round(orig.getX()) / grid));
            double prevY = grid * ((int) (Math.round(orig.getY()) / grid));
            double nextX = grid * ((int) (Math.round(orig.getX()) / grid) + 1);
            double nextY = grid * ((int) (Math.round(orig.getY()) / grid) + 1);
            double targetRadius = Math.min(radius, grid / 4);
            SnapPoint<T> xp = null;
            SnapPoint<T> yp = null;

            if (Math.abs(orig.getX() - prevX) <= targetRadius) {
                xp = new SnapPoint<>(SnapAxis.X, prevX, SnapKind.GRID, null);
            } else if (Math.abs(orig.getX() - nextX) <= targetRadius) {
                xp = new SnapPoint<>(SnapAxis.X, nextX, SnapKind.GRID, null);
            }
            if (Math.abs(orig.getY() - prevY) <= targetRadius) {
                yp = new SnapPoint<>(SnapAxis.Y, prevY, SnapKind.GRID, null);
            } else if (Math.abs(orig.getY() - nextY) <= targetRadius) {
                yp = new SnapPoint<>(SnapAxis.Y, nextY, SnapKind.GRID, null);
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

    public Point2D snapExclusive(Point2D orig, Set<SnapKind> allowedKinds) {
        DoubleMap.Entry<? extends T> nearX
                = allowedKinds.contains(SnapKind.MATCH)
                ? xs.nearestValueExclusive(orig.getX(), radius)
                : null;
        DoubleMap.Entry<? extends T> nearY
                = allowedKinds.contains(SnapKind.MATCH)
                ? ys.nearestValueExclusive(orig.getY(), radius)
                : null;
        SnapKind xKind = SnapKind.MATCH;
        SnapKind yKind = SnapKind.MATCH;
        T xObj = nearX == null ? null : nearX.value();
        T yObj = nearY == null ? null : nearY.value();
        if (nearX == null && allowedKinds.contains(SnapKind.DISTANCE)) {
            for (int i = 0; i < xDists.size(); i++) {

                double dist = xDists.key(i);
                if (dist <= 0) {
                    continue;
                }
                double d1 = orig.getX() + dist;

                nearX = xs.nearestValueExclusive(d1, radius);
                if (nearX != null) {
                    xKind = SnapKind.DISTANCE;
                    xObj = xDists.valueAt(i);
                    break;
                }

                double d2 = orig.getX() - dist;
                nearX = xs.nearestValueExclusive(d2, radius);
                if (nearX != null) {
                    xKind = SnapKind.DISTANCE;
                    xObj = xDists.valueAt(i);
                    break;
                }
            }
        }
        if (nearY == null && allowedKinds.contains(SnapKind.DISTANCE)) {
            for (int i = 0; i < yDists.size(); i++) {
                double dist = yDists.key(i);
                if (dist <= 0) {
                    continue;
                }
                double d1 = orig.getY() + dist;

                nearY = ys.nearestValueExclusive(d1, radius);
                if (nearY != null) {
                    yKind = SnapKind.DISTANCE;
                    yObj = yDists.valueAt(i);
                    break;
                }

                double d2 = orig.getY() - dist;
                nearY = ys.nearestValueExclusive(d2, radius);
                if (nearY != null) {
                    yKind = SnapKind.DISTANCE;
                    yObj = yDists.valueAt(i);
                    break;
                }
            }
        }

        if (nearX == null && nearY != null) {
            if (notify != null) {
                if (!notify.onSnap(null, new SnapPoint<>(SnapAxis.Y, nearY.key(), yKind, yObj))) {
                    return orig;
                }
            }
//            System.out.println("SNAP-Y " + notify + " " + yKind);
            return new Point2D.Double(orig.getX(), nearY.key());
        } else if (nearY == null && nearX != null) {
            if (notify != null) {
                if (!notify.onSnap(new SnapPoint<>(SnapAxis.X, nearX.key(), xKind, xObj), null)) {
                    return orig;
                }
            }
//            System.out.println("SNAP-X " + notify + " " + xKind);
            return new Point2D.Double(nearX.key(), orig.getY());
        } else if (nearY != null && nearX != null) {
            if (notify != null) {
                if (!notify.onSnap(new SnapPoint<>(SnapAxis.X, nearX.key(), xKind, xObj),
                        new SnapPoint<>(SnapAxis.Y, nearY.key(), yKind, yObj))) {
                    return orig;
                }
            }
//            System.out.println("SNAP-X/Y " + notify);
            return new EqPointDouble(nearX.key(), nearY.key());
        } else {
            return orig;
        }
    }

    public SnapPoint nearestWithinRadius(Point2D p) {
        DoubleMap.Entry<? extends T> nearX = xs.nearestValueTo(p.getX(), radius);
        DoubleMap.Entry<? extends T> nearY = ys.nearestValueTo(p.getY(), radius);
        if (nearX == null && nearY != null) {
            return new SnapPoint<>(SnapAxis.Y, nearY.key(), SnapKind.MATCH, nearY.value());
        } else if (nearY == null && nearX != null) {
            return new SnapPoint<>(SnapAxis.X, nearX.key(), SnapKind.MATCH, nearX.value());
        } else if (nearY != null && nearX != null) {
            double distX = Math.abs(p.getX() - nearX.key());
            double distY = Math.abs(p.getY() - nearY.key());
            if (distY < distX) {
                return new SnapPoint<>(SnapAxis.Y, nearY.key(), SnapKind.MATCH, nearY.value());
            } else {
                return new SnapPoint<>(SnapAxis.X, nearX.key(), SnapKind.MATCH, nearX.value());
            }
        } else {
            return null;
        }
    }

    public SnapPoint nearest(Point2D p) {
        SnapPoint a = nearest(SnapAxis.X, p);
        SnapPoint b = nearest(SnapAxis.X, p);
        double da = a.distance(p);
        double db = b.distance(p);
        return da < db ? a : b;
    }

    public SnapPoint nearestWithinRadius(SnapAxis axis, Point2D p) {
        DoubleMap<T> pts = axis == SnapAxis.X ? xs : ys;
        DoubleMap.Entry<? extends T> val = pts.nearestValueTo(axis.value(p), radius);
        if (val == null) {
            return null;
        }
        return new SnapPoint<>(axis, val.key(), SnapKind.MATCH, val.value());
    }

    public SnapPoint nearest(SnapAxis axis, Point2D p) {
        DoubleMap<T> pts = axis == SnapAxis.X ? xs : ys;
        DoubleMap.Entry<? extends T> val = pts.nearestValueTo(axis.value(p));
        if (val == null) {
            return null;
        }
        return new SnapPoint<>(axis, val.key(), SnapKind.MATCH, val.value());
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
