package net.dev.java.imagine.api.tool.aspects.snap;

import java.awt.geom.Point2D;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 *
 * @author Tim Boudreau
 */
public final class SnapPoints {

    private final DoubleSet xs;
    private final DoubleSet ys;
    private final double radius;
    private final BiConsumer<SnapPoint, SnapPoint> notify;

    SnapPoints(double radius, BiConsumer<SnapPoint, SnapPoint> notify, DoubleSet xs, DoubleSet ys) {
        this.xs = xs;
        this.ys = ys;
        this.radius = radius;
        this.notify = notify;
    }

    @Override
    public String toString() {
        return "SnapPoints(X:" + xs + ", Y:" + ys + ")";
    }

    public Point2D snap(Point2D orig) {
        double nearX = xs.nearestValueTo(orig.getX(), radius);
        double nearY = ys.nearestValueTo(orig.getY(), radius);
        if (nearX == Double.MIN_VALUE && nearY != Double.MIN_VALUE) {
            if (notify != null) {
                notify.accept(null, new SnapPoint(Axis.Y, nearY));
            }
            return new Point2D.Double(orig.getX(), nearY);
        } else if (nearY == Double.MIN_VALUE && nearX != Double.MIN_VALUE) {
            if (notify != null) {
                notify.accept(new SnapPoint(Axis.X, nearX), null);
            }
            return new Point2D.Double(nearX, orig.getY());
        } else if (nearY != Double.MIN_VALUE && nearX != Double.MIN_VALUE) {
            if (notify != null) {
                notify.accept(new SnapPoint(Axis.X, nearX), new SnapPoint(Axis.Y, nearY));
            }
            return new Point2D.Double(nearX, nearY);
        } else {
            return orig;
        }
    }

    public SnapPoint nearestWithinRadius(Point2D p) {
        double nearX = xs.nearestValueTo(p.getX(), radius);
        double nearY = ys.nearestValueTo(p.getY(), radius);
        if (nearX == Double.MIN_VALUE && nearY != Double.MIN_VALUE) {
            return new SnapPoint(Axis.Y, nearY);
        } else if (nearY == Double.MIN_VALUE && nearX != Double.MIN_VALUE) {
            return new SnapPoint(Axis.X, nearX);
        } else if (nearY != Double.MIN_VALUE && nearX != Double.MIN_VALUE) {
            double distX = Math.abs(p.getX() - nearX);
            double distY = Math.abs(p.getY() - nearY);
            if (distY < distX) {
                return new SnapPoint(Axis.Y, nearY);
            } else {
                return new SnapPoint(Axis.X, nearX);
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
        DoubleSet pts = axis == Axis.X ? xs : ys;
        double val = pts.nearestValueTo(axis.value(p), radius);
        if (val == Double.MIN_VALUE) {
            return null;
        }
        return new SnapPoint(axis, val);
    }

    public SnapPoint nearest(Axis axis, Point2D p) {
        DoubleSet pts = axis == Axis.X ? xs : ys;
        double val = pts.nearestValueTo(axis.value(p));
        if (val == Double.MIN_VALUE) {
            return null;
        }
        return new SnapPoint(axis, val);
    }

    public static Builder builder(double radius) {
        return new Builder(radius);
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
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SnapPoints other = (SnapPoints) obj;
        if (Double.doubleToLongBits(this.radius) != Double.doubleToLongBits(other.radius)) {
            return false;
        }
        if (!Objects.equals(this.xs, other.xs)) {
            return false;
        }
        return Objects.equals(this.ys, other.ys);
    }

    public static class Builder {

        private final DoubleSet xs = new DoubleSet(256);
        private final DoubleSet ys = new DoubleSet(256);
        private final double rad;
        private BiConsumer<SnapPoint, SnapPoint> notify;

        Builder(double rad) {
            if (rad < 0) {
                throw new IllegalArgumentException("" + rad);
            }
            this.rad = rad;
        }

        public Builder add(Point2D p2d) {
            add(Axis.X, p2d.getX());
            add(Axis.Y, p2d.getY());
            return this;
        }

        public Builder notifying(BiConsumer<SnapPoint, SnapPoint> notify) {
            this.notify = notify;
            return this;
        }

        public void add(Axis axis, double coord) {
            switch (axis) {
                case X:
                    xs.add(coord);
                    break;
                case Y:
                    ys.add(coord);
                    break;
                default:
                    throw new AssertionError("Axis " + axis);
            }
        }

        public SnapPoints build() {
            return new SnapPoints(rad, notify, xs, ys);
        }
    }
}
