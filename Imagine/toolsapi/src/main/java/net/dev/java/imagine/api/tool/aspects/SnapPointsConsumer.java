/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.dev.java.imagine.api.tool.aspects;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.dev.java.imagine.api.tool.aspects.SnapPointsConsumer.SnapPoints;

/**
 *
 * @author Tim Boudreau
 */
public interface SnapPointsConsumer extends Consumer<Supplier<SnapPoints>> {

    public enum Axis {
        X, Y;

        double value(Point2D p) {
            return this == X ? p.getX() : p.getY();
        }
    }

    public static final class SnapPoints {

        private final List<SnapPoint> xs = new ArrayList<>();
        private final List<SnapPoint> ys = new ArrayList<>();
        private final double radius;
        private final BiConsumer<SnapPoint, SnapPoint> notify;

        SnapPoints(double radius, BiConsumer<SnapPoint, SnapPoint> notify) {
            this.radius = radius;
            this.notify = notify;
        }

        public Point2D snap(Point2D orig) {
            SnapPoint xpt = nearestWithinRadius(Axis.X, orig);
            SnapPoint ypt = nearestWithinRadius(Axis.Y, orig);
            Point2D.Double result = null;
            if (xpt != null) {
                result = new Point2D.Double(orig.getX(), orig.getY());
                result.x = xpt.coordinate;
            }
            if (ypt != null) {
                if (result == null) {
                    result = new Point2D.Double(orig.getX(), orig.getY());
                }
                result.y = ypt.coordinate;
            }
            if (notify != null && (xpt != null || ypt != null)) {
                onSnap(xpt, ypt);
            }
            return result == null ? orig : result;
        }

        void onSnap(SnapPoint xpt, SnapPoint ypt) {
            notify.accept(xpt, ypt);
        }

        public SnapPoint nearestWithinRadius(Point2D p) {
            SnapPoint pt = nearest(p);
            return pt == null ? null : pt.distance(p) <= radius ? pt : null;
        }

        public SnapPoint nearest(Point2D p) {
            SnapPoint a = nearest(Axis.X, p);
            SnapPoint b = nearest(Axis.X, p);
            double da = a.distance(p);
            double db = b.distance(p);
            return da < db ? a : b;
        }

        public SnapPoint nearestWithinRadius(Axis axis, Point2D p) {
            SnapPoint result = nearest(axis, p);
            if (result != null && result.distance(p) <= radius) {
                return result;
            }
            return null;
        }

        public SnapPoint nearest(Axis axis, Point2D p) {
            List<SnapPoint> pts = axis == Axis.X ? xs : ys;
            // XXX we can do binary search here
            double dist = Double.MAX_VALUE;
            SnapPoint best = null;
            for (SnapPoint s : pts) {
                double d = s.distance(p);
                if (d < dist) {
                    best = s;
                    dist = d;
                }
            }
            return best;
        }

        public static Builder builder(double radius) {
            return new Builder(radius);
        }

        public static class Builder {

            private final Set<SnapPoint> points = new HashSet<>();
            private final double rad;
            private BiConsumer<SnapPoint,SnapPoint> notify;

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

            public Builder notifying(BiConsumer<SnapPoint,SnapPoint> notify) {
                this.notify = notify;
                return this;
            }

            public void add(Axis axis, double coord) {
                add(new SnapPoint(axis, coord));
            }

            private void add(SnapPoint p) {
                points.add(p);
            }

            public SnapPoints build() {
                SnapPoints sp = new SnapPoints(rad, notify);
                for (SnapPoint s : points) {
                    switch (s.axis) {
                        case X:
                            sp.xs.add(s);
                            break;
                        case Y:
                            sp.ys.add(s);
                            break;
                    }
                }
                Collections.sort(sp.xs);
                Collections.sort(sp.ys);
                return sp;
            }
        }
    }

    public static class SnapPoint implements Comparable<SnapPoint> {

        private final Axis axis;
        private final double coordinate;

        public SnapPoint(Axis axis, double coordinate) {
            this.axis = axis;
            this.coordinate = coordinate;
        }

        public double coordinate() {
            return coordinate;
        }

        public Axis axis() {
            return axis;
        }

        public double distance(double x, double y) {
            double c = axis == Axis.X ? x : y;
            return Math.abs(c - coordinate);
        }

        public double distance(Point2D p) {
            double c = axis.value(p);
            return Math.abs(c - coordinate);
        }

        @Override
        public String toString() {
            return axis + ":" + coordinate;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 83 * hash + Objects.hashCode(this.axis);
            hash = 83 * hash + (int) (Double.doubleToLongBits(
                    Math.floor(this.coordinate) + 0.5)
                    ^ (Double.doubleToLongBits(this.coordinate) >>> 32));
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
            final SnapPoint other = (SnapPoint) obj;
            if (Math.abs(this.coordinate - other.coordinate) < 0.5) {
                return true;
            }
            if (this.axis != other.axis) {
                return false;
            }
            return true;
        }

        @Override
        public int compareTo(SnapPoint o) {
            int result = axis.compareTo(o.axis);
            if (result == 0) {
                result = Double.compare(coordinate, o.coordinate);
            }
            return result;
        }

    }
}
