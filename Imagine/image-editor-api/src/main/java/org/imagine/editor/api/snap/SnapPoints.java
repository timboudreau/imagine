package org.imagine.editor.api.snap;

import com.mastfrog.util.collections.DoubleMap;
import java.awt.geom.Point2D;
import java.util.Objects;
import java.util.Set;
import org.imagine.geometry.Circle;

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
    public static final SnapPoints<Object> EMPTY = new SnapPoints(0, null, EMPTY_DBLS, EMPTY_DBLS, EMPTY_DBLS, EMPTY_DBLS, EMPTY_DBLS);
    private final DoubleMap<T> xDists;
    private final DoubleMap<T> yDists;
    private final DoubleMap<T> angles;

    SnapPoints(double radius, OnSnap<T> notify, DoubleMap<T> xs, DoubleMap<T> ys,
            DoubleMap<T> xDists, DoubleMap<T> yDists, DoubleMap<T> angles) {
        this.xs = xs;
        this.ys = ys;
        this.radius = radius;
        this.notify = notify;
        this.xDists = xDists;
        this.yDists = yDists;
        this.angles = angles;
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
                notify.onSnap(null, new SnapPoint<>(Axis.Y, nearY.key(), SnapKind.MATCH, nearY.value()));
            }
            return new Point2D.Double(orig.getX(), nearY.key());
        } else if (nearY == null && nearX != null) {
            if (notify != null) {
                notify.onSnap(new SnapPoint<>(Axis.X, nearX.key(), SnapKind.MATCH, nearX.value()), null);
            }
            return new Point2D.Double(nearX.key(), orig.getY());
        } else if (nearY != null && nearX != null) {
            if (notify != null) {
                notify.onSnap(new SnapPoint<>(Axis.X, nearX.key(), SnapKind.MATCH, nearX.value()),
                        new SnapPoint<>(Axis.Y, nearY.key(), SnapKind.MATCH, nearY.value()));
            }
            return new Point2D.Double(nearX.key(), nearY.key());
        } else {
            return orig;
        }
    }

    public Point2D snapExclusive(Point2D preceding, Point2D orig, Point2D next, int grid, Set<SnapKind> allowedKinds) {
        if (allowedKinds.isEmpty()) {
            return orig;
        }
        Point2D result = snapExclusive(orig, allowedKinds);
        if (result == orig && allowedKinds.contains(SnapKind.ANGLE) && preceding != null) {
            Circle circ = new Circle(orig.getX(), orig.getY(),
                    Point2D.distance(preceding.getX(), preceding.getY(), orig.getX(), orig.getY()));
            double angle = circ.angleOf(preceding.getX(), preceding.getY());
            DoubleMap.Entry<? extends T> foundAng = angles.nearestValueTo(angle, 3.25);
            if (foundAng != null) {
                Point2D p = circ.getPosition(foundAng.key());
                if (Point2D.distance(p.getX(), p.getY(), orig.getX(), orig.getY()) <= radius) {
                    result = p;
                    if (notify != null) {
                        SnapPoint<T> xpt = new SnapPoint<>(Axis.X, result.getX(),
                                SnapKind.ANGLE, foundAng.value());
                        SnapPoint<T> ypt = new SnapPoint<>(Axis.Y, result.getY(),
                                SnapKind.ANGLE, foundAng.value());
                        notify.onSnap(xpt, ypt);
                    }
                }
            }
        }
        if (result == orig && next != null && allowedKinds.contains(SnapKind.ANGLE)) {
            Circle circ = new Circle(orig.getX(), orig.getY(),
                    Point2D.distance(next.getX(), next.getY(), orig.getX(),
                            orig.getY()));
            double angle = circ.angleOf(next.getX(), next.getY());
            DoubleMap.Entry<? extends T> foundAng = angles.nearestValueTo(angle, 3.25);
            if (foundAng != null) {
                Point2D pos = circ.getPosition(foundAng.key());
                if (Point2D.distance(pos.getX(), pos.getY(), orig.getX(), orig.getY()) <= radius) {
                    result = pos;
                    if (notify != null) {
                        SnapPoint<T> xpt = new SnapPoint<>(Axis.X, result.getX(),
                                SnapKind.ANGLE, foundAng.value());
                        SnapPoint<T> ypt = new SnapPoint<>(Axis.Y, result.getY(),
                                SnapKind.ANGLE, foundAng.value());
                        notify.onSnap(xpt, ypt);
                    }
                }
            }
        }
        if (grid > 2 && result == orig && allowedKinds.contains(SnapKind.GRID)) {
            double prevX = grid * ((int) (Math.round(orig.getX()) / grid));
            double prevY = grid * ((int) (Math.round(orig.getY()) / grid));
            double nextX = grid * ((int) (Math.round(orig.getX()) / grid) + 1);
            double nextY = grid * ((int) (Math.round(orig.getY()) / grid) + 1);
            double targetRadius = Math.min(radius, grid / 4);
            SnapPoint<T> xp = null;
            SnapPoint<T> yp = null;

            if (Math.abs(orig.getX() - prevX) <= targetRadius) {
                xp = new SnapPoint<>(Axis.X, prevX, SnapKind.GRID, null);
            } else if (Math.abs(orig.getX() - nextX) <= targetRadius) {
                xp = new SnapPoint<>(Axis.X, nextX, SnapKind.GRID, null);
            }
            if (Math.abs(orig.getY() - prevY) <= targetRadius) {
                yp = new SnapPoint<>(Axis.Y, prevY, SnapKind.GRID, null);
            } else if (Math.abs(orig.getY() - nextY) <= targetRadius) {
                yp = new SnapPoint<>(Axis.Y, nextY, SnapKind.GRID, null);
            }

            if (xp != null && yp != null) {
                result = new Point2D.Double(xp.coordinate(), yp.coordinate());
            } else if (xp == null && yp != null) {
                result = new Point2D.Double(orig.getX(), yp.coordinate());
            } else if (yp == null && xp != null) {
                result = new Point2D.Double(xp.coordinate(), orig.getY());
            }
            if (notify != null && (xp != null || yp != null)) {
                notify.onSnap(xp, yp);
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
                notify.onSnap(null, new SnapPoint<>(Axis.Y, nearY.key(), yKind, yObj));
            }
//            System.out.println("SNAP-Y " + notify + " " + yKind);
            return new Point2D.Double(orig.getX(), nearY.key());
        } else if (nearY == null && nearX != null) {
            if (notify != null) {
                notify.onSnap(new SnapPoint<>(Axis.X, nearX.key(), xKind, xObj), null);
            }
//            System.out.println("SNAP-X " + notify + " " + xKind);
            return new Point2D.Double(nearX.key(), orig.getY());
        } else if (nearY != null && nearX != null) {
            if (notify != null) {
                notify.onSnap(new SnapPoint<>(Axis.X, nearX.key(), xKind, xObj),
                        new SnapPoint<>(Axis.Y, nearY.key(), yKind, yObj));
            }
//            System.out.println("SNAP-X/Y " + notify);
            return new Point2D.Double(nearX.key(), nearY.key());
        } else {
            return orig;
        }
    }

    public SnapPoint nearestWithinRadius(Point2D p) {
        DoubleMap.Entry<? extends T> nearX = xs.nearestValueTo(p.getX(), radius);
        DoubleMap.Entry<? extends T> nearY = ys.nearestValueTo(p.getY(), radius);
        if (nearX == null && nearY != null) {
            return new SnapPoint<>(Axis.Y, nearY.key(), SnapKind.MATCH, nearY.value());
        } else if (nearY == null && nearX != null) {
            return new SnapPoint<>(Axis.X, nearX.key(), SnapKind.MATCH, nearX.value());
        } else if (nearY != null && nearX != null) {
            double distX = Math.abs(p.getX() - nearX.key());
            double distY = Math.abs(p.getY() - nearY.key());
            if (distY < distX) {
                return new SnapPoint<>(Axis.Y, nearY.key(), SnapKind.MATCH, nearY.value());
            } else {
                return new SnapPoint<>(Axis.X, nearX.key(), SnapKind.MATCH, nearX.value());
            }
        } else {
            return null;
        }
    }

    public SnapPoint nearest(Point2D p) {
        SnapPoint a = nearest(Axis.X, p);
        SnapPoint b = nearest(Axis.X, p);
        double da = a.distance(p);
        double db = b.distance(p);
        return da < db ? a : b;
    }

    public SnapPoint nearestWithinRadius(Axis axis, Point2D p) {
        DoubleMap<T> pts = axis == Axis.X ? xs : ys;
        DoubleMap.Entry<? extends T> val = pts.nearestValueTo(axis.value(p), radius);
        if (val == null) {
            return null;
        }
        return new SnapPoint<>(axis, val.key(), SnapKind.MATCH, val.value());
    }

    public SnapPoint nearest(Axis axis, Point2D p) {
        DoubleMap<T> pts = axis == Axis.X ? xs : ys;
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
