/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.design;

import com.mastfrog.util.collections.IntSet;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.Mutable;
import net.java.dev.imagine.api.vector.Versioned;
import net.java.dev.imagine.api.vector.util.Pt;
import net.java.dev.imagine.api.vector.util.Size;
import org.imagine.geometry.EqPointDouble;

/**
 *
 * @author Tim Boudreau
 */
final class ControlPointSharedData<T extends Adjustable & Versioned> {

    private final T adj;
    private int cpCount;
    private double[] controlPoints;
    private ControlPointKind[] kinds;
    private final IntSet virtuals = IntSet.create(63);
    private int revAtLastUpd = Integer.MIN_VALUE;
    private final ControlPointController ctrllr;
    private ControlPoint[] instances = new ControlPoint[0];
    private final int idHash;

    public ControlPointSharedData(T adj, ControlPointController ctrllr) {
        this.adj = adj;
        this.ctrllr = ctrllr;
        idHash = System.identityHashCode(adj);
    }

    ControlPoint[] getControlPoints() {
        upd();
        return instances;
    }

    private void upd() {
        int curr = adj.rev();
        if (curr != revAtLastUpd) {
            revAtLastUpd = curr;
            cpCount = adj.getControlPointCount();
            int arrSize = cpCount * 2;
            if (controlPoints == null || controlPoints.length < arrSize) {
                controlPoints = new double[arrSize];
            }
            adj.getControlPoints(controlPoints);
            kinds = adj.getControlPointKinds();
            virtuals.clear();
            virtuals.addAll(adj.getVirtualControlPointIndices());
            if (instances.length != cpCount) {
                if (cpCount == 0) {
                    instances = new ControlPoint[0];
                } else if (cpCount > instances.length) {
                    int oldLength = instances.length;
                    instances = Arrays.copyOf(instances, cpCount);
                    for (int i = oldLength; i < instances.length; i++) {
                        instances[i] = new CPImpl(i);
                    }
                } else if (cpCount < instances.length) {
                    instances = Arrays.copyOf(instances, cpCount);
                }
            }
        }
    }

    ControlPointKind kind(int index) {
        if (isValid(index)) {
            return kinds[index];
        }
        return ControlPointKind.OTHER;
    }

    Point2D.Double point(int index) {
        if (upd(index)) {
            return new EqPointDouble(controlPoints[index * 2], controlPoints[(index * 2) + 1]);
        }
        return new EqPointDouble(-1, -1);
    }

    double getX(int index) {
        if (upd(index)) {
            return controlPoints[index * 2];
        }
        return -1;
    }

    double getY(int index) {
        if (upd(index)) {
            return controlPoints[(index * 2) + 1];
        }
        return -1;
    }

    boolean isVirtual(int index) {
        return virtuals.contains(index);
    }

    boolean isValid(int index) {
        return upd(index);
    }

    boolean isEditable(int index) {
        if (upd(index)) {
            if (adj.hasReadOnlyControlPoints()) {
                return adj.isControlPointReadOnly(index);
            }
            return true;
        }
        return false;
    }

    boolean isDeletable(int index) {
        return index > 0 && adj instanceof Mutable && isEditable(index);
    }

    boolean delete(int index) {
        assert index > 0 && adj instanceof Mutable : "canDelete not called";
        return ((Mutable) adj).delete(index);
    }

    boolean set(int index, double x, double y) {
        if (upd(index)) {
            int oldRev = revAtLastUpd;
            adj.setControlPointLocation(index, new Pt(x, y));
            revAtLastUpd = adj.rev();
            controlPoints[index * 2] = x;
            controlPoints[(index * 2) + 1] = y;
            return revAtLastUpd != oldRev;
        }
        return false;
    }

    boolean isHit(int index, double x, double y) {
        if (upd(index)) {
            Size s = ctrllr.getControlPointSize();
            if (s.w == 0 || s.h == 0) {
                return false;
            }
            upd();
            double px = controlPoints[index * 2];
            double py = controlPoints[(index * 2) + 1];
            double halfx = s.w / 2;
            double halfy = s.h / 2;
            return (x >= px - halfx && x <= px + halfx)
                    && (y >= py - halfy && y <= py + halfy);
        }
        return false;
    }

    Set<ControlPointKind> availableKinds(int index) {
        if (upd(index)) {
            return adj.availablePointKinds(index);
        }
        return EnumSet.noneOf(ControlPointKind.class);
    }

    private boolean upd(int forIndex) {
        upd();
        return forIndex < cpCount;
    }

    class CPImpl implements ControlPoint {

        private final int index;

        public CPImpl(int index) {
            this.index = index;
        }

        @Override
        public ControlPoint[] family() {
            return instances;
        }

        @Override
        public ControlPointKind kind() {
            return ControlPointSharedData.this.kind(index);
        }

        @Override
        public int index() {
            return index;
        }

        @Override
        public Adjustable getPrimitive() {
            return adj;
        }

        @Override
        public Point2D.Double location() {
            return ControlPointSharedData.this.point(index);
        }

        @Override
        public boolean isValid() {
            return ControlPointSharedData.this.isValid(index);
        }

        private void change() {
            ctrllr.changed(this);
        }

        @Override
        public boolean move(double dx, double dy) {
            if (!isValid()) {
                return false;
            }
            if (!ControlPointSharedData.this.isEditable(index)) {
                return false;
            }
            if (dx != 0 || dy != 0) {
                double x = controlPoints[index * 2];
                double y = controlPoints[(index * 2) + 1];
                x += dx;
                y += dy;
                if (ControlPointSharedData.this.set(index, x, y)) {
                    change();
                    return true;
                }
            } else {
                upd();
            }
            return false;
        }

        @Override
        public double getX() {
            return ControlPointSharedData.this.getX(index);
        }

        @Override
        public double getY() {
            return ControlPointSharedData.this.getY(index);
        }

        @Override
        public boolean set(double newX, double newY) {
            if (ControlPointSharedData.this.set(index, newX, newY)) {
                change();
                return true;
            }
            return false;
        }

        @Override
        public boolean delete() {
            boolean result = ControlPointSharedData.this.delete(index);
            if (result) {
                ctrllr.deleted(this);
            }
            return result;
        }

        @Override
        public boolean canDelete() {
            return ControlPointSharedData.this.isDeletable(index);
        }

        @Override
        public boolean isVirtual() {
            return ControlPointSharedData.this.isVirtual(index);
        }

        @Override
        public boolean hit(double hx, double hy) {
            return ControlPointSharedData.this.isHit(index, hx,
                    hy);
        }

        @Override
        public Set<ControlPointKind> availableControlPointKinds() {
            return ControlPointSharedData.this.availableKinds(index);
        }

        @Override
        public int hashCode() {
            return index * 98_479 + idHash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj == null) {
                return false;
            } else if (!(obj instanceof ControlPoint)) {
                return false;
            }
            final ControlPoint other = (ControlPoint) obj;
            return this.index == other.index()
                    && other.getPrimitive() == adj;
        }

        @Override
        public String toString() {
            return "CpN("
                    + index
                    + " "
                    + Integer.toString(idHash, 36);
        }
    }
}
