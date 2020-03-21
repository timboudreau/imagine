package org.imagine.vector.editor.ui;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.Shaped;
import net.java.dev.imagine.api.vector.Versioned;
import net.java.dev.imagine.api.vector.design.ControlPoint;
import net.java.dev.imagine.api.vector.design.ControlPointController;
import net.java.dev.imagine.api.vector.design.ControlPointFactory2;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import net.java.dev.imagine.api.vector.util.Pt;
import org.imagine.vector.editor.ui.spi.ShapeControlPoint;
import org.imagine.vector.editor.ui.spi.ShapeElement;

/**
 * Provides control points for a ShapeEntry which tolerates the shape instance
 * being switched out from underneath it, and only become invalid if points are
 * deleted.
 *
 * @author Tim Boudreau
 */
final class ShapeElementControlPointFactory extends ControlPointFactory2 {

    public ShapeControlPoint[] getControlPoints(ShapeEntry entry, ControlPointController ctrllr) {
        ControlPointSupplier supp = new ControlPointSupplier(entry, ctrllr);
        Adjustable adj = entry.item().as(Adjustable.class);
        int cpCount = adj == null ? 0 : adj.getControlPointCount();
        ShapeControlPoint[] result = new ShapeControlPoint[cpCount];
        FamilySupplier fam = new FamilySupplier(entry, ctrllr, result);
        for (int i = 0; i < result.length; i++) {
            result[i] = new DelegatingControlPoint(entry,
                    supp.forIndex(i), i, fam);
        }
        return result;
    }

    class FamilySupplier implements Supplier<ShapeControlPoint[]> {
        private final ShapeEntry entry;
        private ShapeControlPoint[] family;
        private ControlPointController ctrllr;
        public FamilySupplier(ShapeEntry entry, ControlPointController ctrllr, ShapeControlPoint[] initialArray) {
            this.entry = entry;
            this.ctrllr = ctrllr;
            this.family = initialArray;
        }

        @Override
        public ShapeControlPoint[] get() {
            Shaped sh = entry.item();
            if (sh.is(Adjustable.class)) {
                Adjustable adj = sh.as(Adjustable.class);
                int count = adj.getControlPointCount();
                if (count == family.length) {
                    return family;
                }
                family = getControlPoints(entry, ctrllr);
                return family;
            }
            return new ShapeControlPoint[0];
        }

    }

    class ControlPointSupplier implements Supplier<ControlPoint[]> {

        private final ShapeEntry origin;

        private int hashAtLastFetch = -1;
        private int idHashAtLastFetch = -1;
        ControlPoint[] lastPoints;
        private final ControlPointController ctrllr;

        public ControlPointSupplier(ShapeEntry entry, ControlPointController ctrllr) {
            this.origin = entry;
            this.ctrllr = ctrllr;
        }

        private ControlPoint[] fetch(int newHash, int newIdHash) {
            Shaped s = origin.item();
            hashAtLastFetch = newHash != -1 ? newHash
                    : hash();
            idHashAtLastFetch = newIdHash != -1 ? newIdHash
                    : System.identityHashCode(s);

            Adjustable adj = s.as(Adjustable.class);
            if (adj == null) {
                return new ControlPoint[0];
            }
            lastPoints = getControlPoints(adj, ctrllr);
            return lastPoints;
        }

        private int hash() {
            Shaped adj = origin.item();
            if (adj instanceof Versioned) {
                return ((Versioned) adj).rev();
            }
            return origin.item().hashCode();
        }

        @Override
        public ControlPoint[] get() {
            Shaped s = origin.item();
            int hash = hash();
            if (hash != hashAtLastFetch) {
                return fetch(hash, -1);
            }
            int idHash = System.identityHashCode(s);
            if (idHash != idHashAtLastFetch) {
                return fetch(hash, idHash);
            }
            return lastPoints;
        }

        Supplier<ControlPoint> forIndex(int index) {
            return () -> {
                ControlPoint[] all = get();
                if (all.length > index) {
                    return all[index];
                }
                return new InvalidControlPoint(origin, index);
            };
        }
    }

    @Override
    public ControlPoint[] getControlPoints(Adjustable p, ControlPointController c) {
        return super.getControlPoints(p, c);
    }

    private static class DelegatingControlPoint implements ShapeControlPoint {

        private final ShapeEntry origin;
        private final Supplier<ControlPoint> supp;
        private final int index;
        private final Supplier<ShapeControlPoint[]> family;

        public DelegatingControlPoint(ShapeEntry origin, Supplier<ControlPoint> supp, int index, Supplier<ShapeControlPoint[]> family) {
            this.origin = origin;
            this.supp = supp;
            this.index = index;
            this.family = family;
        }

        public ShapeControlPoint[] family() {
            return family.get();
        }

        @Override
        public ShapeElement owner() {
            return origin;
        }

        @Override
        public boolean isEditable() {
            return supp.get().isEditable();
        }

        @Override
        public String toString() {
            return "CP(" + origin.id() + "-" + index
                    + " " + origin.item() + ")";
        }

        @Override
        public int hashCode() {
            long result = (index * 88301)
                    + (origin.id() * 6947);
            return (((int) result) ^ ((int) (result >> 32)));
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o == null) {
                return false;
            } else if (o instanceof DelegatingControlPoint) {
                DelegatingControlPoint del = (DelegatingControlPoint) o;
                return del.index == index
                        && origin.id() == del.origin.id();
            } else if (o instanceof InvalidControlPoint) {
                InvalidControlPoint del = (InvalidControlPoint) o;
                return del.index == index
                        && origin.id() == del.origin.id();
            } else if (o instanceof ShapeControlPoint) {
                ShapeControlPoint scp = (ShapeControlPoint) o;
                return scp.owner().id() == owner().id()
                        && scp.index() == index();
            }
            return false;
        }

        private <T> T fetch(Function<ControlPoint, T> f) {
            return f.apply(supp.get());
        }

        private boolean is(Predicate<ControlPoint> p) {
            return p.test(supp.get());
        }

        private double value(ToDoubleFunction<ControlPoint> f) {
            return f.applyAsDouble(supp.get());
        }

        @Override
        public Set<ControlPointKind> availableControlPointKinds() {
            return fetch(ControlPoint::availableControlPointKinds);
        }

        @Override
        public ControlPointKind kind() {
            return fetch(ControlPoint::kind);
        }

        @Override
        public int index() {
            return index;
        }

        @Override
        public Adjustable getPrimitive() {
            return fetch(ControlPoint::getPrimitive);
        }

        @Override
        public Point2D.Double location() {
            return fetch(ControlPoint::location);
        }

        @Override
        public boolean isValid() {
            ControlPoint cp = supp.get();
            return cp.isValid();
        }

        @Override
        public boolean move(double dx, double dy) {
            return is(cp -> {
                return cp.move(dx, dy);
            });
        }

        @Override
        public double getX() {
            return value(ControlPoint::getX);
        }

        @Override
        public double getY() {
            return value(ControlPoint::getY);
        }

        @Override
        public boolean set(double dx, double dy) {
            return is(cp -> {
                return cp.set(dx, dy);
            });
        }

        @Override
        public boolean delete() {
            return is(ControlPoint::delete);
        }

        @Override
        public boolean canDelete() {
            return is(ControlPoint::canDelete);
        }

        @Override
        public boolean isVirtual() {
            return is(ControlPoint::isVirtual);
        }

        @Override
        public boolean hit(double hx, double hy) {
            return is(cp -> {
                return cp.hit(hx, hy);
            });
        }
    }

    static final class InvalidControlPoint implements ShapeControlPoint {

        private final ShapeEntry origin;

        private final int index;

        public InvalidControlPoint(ShapeEntry origin, int index) {
            this.origin = origin;
            this.index = index;
        }

        @Override
        public boolean isEditable() {
            return false;
        }

        @Override
        public ShapeControlPoint[] family() {
            return new ShapeControlPoint[0];
        }

        @Override
        public ShapeElement owner() {
            return origin;
        }

        @Override
        public String toString() {
            return "Invalid-CP(" + origin.id() + "-" + index
                    + " " + origin.item() + ")";
        }

        @Override
        public int hashCode() {
            long result = (index * 88301)
                    + (origin.id() * 6947);
            return (((int) result) ^ ((int) (result >> 32)));
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o == null) {
                return false;
            } else if (o instanceof DelegatingControlPoint) {
                DelegatingControlPoint del = (DelegatingControlPoint) o;
                return del.index == index
                        && origin.id() == del.origin.id();
            } else if (o instanceof InvalidControlPoint) {
                InvalidControlPoint inv = (InvalidControlPoint) o;
                return inv.index == index
                        && origin.id() == inv.origin.id();
            } else if (o instanceof ShapeControlPoint) {
                ShapeControlPoint scp = (ShapeControlPoint) o;
                return scp.index() == index &&
                        scp.owner().id() == owner().id();
            }
            return false;
        }

        @Override
        public ControlPointKind kind() {
            return ControlPointKind.OTHER;
        }

        @Override
        public int index() {
            return index;
        }

        @Override
        public Adjustable getPrimitive() {
            return FakeAdjustable.INSTANCE;
        }

        @Override
        public Point2D.Double location() {
            return new Point2D.Double(-1, -1);
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public boolean move(double dx, double dy) {
            return false;
        }

        @Override
        public double getX() {
            return -1;
        }

        @Override
        public double getY() {
            return -1;
        }

        @Override
        public boolean set(double dx, double dy) {
            return false;
        }

        @Override
        public boolean delete() {
            return false;
        }

        @Override
        public boolean canDelete() {
            return false;
        }

        @Override
        public boolean isVirtual() {
            return false;
        }

        @Override
        public boolean hit(double hx, double hy) {
            return false;
        }

        @Override
        public Set<ControlPointKind> availableControlPointKinds() {
            return Collections.emptySet();
        }

        static class FakeAdjustable implements Adjustable {

            private static final FakeAdjustable INSTANCE = new FakeAdjustable();
            private static final ControlPointKind[] EMPTY = new ControlPointKind[0];

            @Override
            public int getControlPointCount() {
                return 0;
            }

            @Override
            public void getControlPoints(double[] xy) {
                // do nothing
            }

            @Override
            public int[] getVirtualControlPointIndices() {
                return EMPTY_INT;
            }

            @Override
            public void setControlPointLocation(int pointIndex, Pt location) {
                // do nothing
            }

            @Override
            public ControlPointKind[] getControlPointKinds() {
                return EMPTY;
            }

            @Override
            public void paint(Graphics2D g) {
                // do nothing
            }

            @Override
            public Primitive copy() {
                return this;
            }
        }
    }
}
