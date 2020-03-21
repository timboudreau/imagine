/*
 * ControlPoint.java
 *
 * Created on October 30, 2006, 10:45 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.design;

import java.awt.Toolkit;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Set;
import java.util.function.Supplier;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.Mutable;
import net.java.dev.imagine.api.vector.util.Pt;
import net.java.dev.imagine.api.vector.util.Size;

/**
 *
 * @author Tim Boudreau
 */
final class ControlPointImpl implements ControlPoint {

    private final Adjustable primitive;
    private final ControlPointController controller;
    private final int index;
    private final boolean virtual;
    private boolean invalid;
    private double[] vals;
    private final ControlPointKind kind;
    private final Supplier<ControlPoint[]> family;

    ControlPointImpl(Adjustable primitive, ControlPointController controller, int index, boolean virtual, ControlPointKind kind,
                Supplier<ControlPoint[]> family) {
        this.family = family;
        this.primitive = primitive;
        this.index = index;
        this.controller = controller;
        this.virtual = virtual;
        vals = new double[primitive.getControlPointCount() * 2];
        this.kind = kind;
    }

    public ControlPoint[] family() {
        return family.get();
    }

    @Override
    public String toString() {
        return "Cp("
                + index
                + " "
                + Integer.toString(System.identityHashCode(primitive), 36);
    }

    @Override
    public ControlPointKind kind() {
        return kind;
    }

    @Override
    public int index() {
        return index;
    }

    @Override
    public Adjustable getPrimitive() {
        return primitive;
    }

    private void upd() {
        if (!isValid()) {
            return;
        }
        int ct = primitive.getControlPointCount();
        if (ct <= index) {
            invalid = true;
            return;
        }
        if (ct > vals.length / 2) {
            vals = new double[ct * 2];
        }
        primitive.getControlPoints(vals);
    }

    @Override
    public Point2D.Double location() {
        if (!isValid()) {
            return new Point2D.Double(-1, -1);
        }
        upd();
        int offset = index * 2;
        return new Point2D.Double(vals[offset], vals[offset + 1]);
    }

    @Override
    public boolean isValid() {
        if (invalid) {
            return false;
        }
        // get control point count on PathIterator
        // is not so cheap
        boolean result = primitive.getControlPointCount() > index;
        if (!result) {
            invalid = true;
        }
        return result;
    }

    @Override
    public boolean move(double dx, double dy) {
        if (!isValid()) {
            return false;
        }
        if (primitive.hasReadOnlyControlPoints() && primitive.isControlPointReadOnly(index)) {
            return false;
        }
        if (dx != 0 || dy != 0) {
            upd();
            if (invalid) {
                return false;
            }
            double x = vals[index * 2];
            double y = vals[(index * 2) + 1];
            x += dx;
            y += dy;
            primitive.setControlPointLocation(index, new Pt(x, y));
            change();
            return true;
        } else {
            upd();
        }
        return false;
    }

    @Override
    public double getX() {
        upd();
        if (invalid) {
            return -1;
        }
        int offset = index * 2;
        return vals[offset];
    }

    @Override
    public double getY() {
        upd();
        if (invalid) {
            return -1;
        }
        int offset = (index * 2) + 1;
        return vals[offset];
    }

    @Override
    public boolean set(double newX, double newY) {
        if (!isValid()) {
            return false;
        }
        if (primitive.hasReadOnlyControlPoints() && primitive.isControlPointReadOnly(index)) {
            return false;
        }
        primitive.setControlPointLocation(index, new Pt(newX, newY));
        change();
        return true;
    }

    @Override
    public boolean isEditable() {
        if (primitive.hasReadOnlyControlPoints() && primitive.isControlPointReadOnly(index)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean delete() {
        if (!isVirtual() && primitive instanceof Mutable) {
            if (primitive.hasReadOnlyControlPoints() && primitive.isControlPointReadOnly(index)) {
                return false;
            }
            if (!((Mutable) primitive).delete(index)) {
                System.out.println("Mutable delete " + index + " returned false");
                Toolkit.getDefaultToolkit().beep();
            } else {
                upd();
                System.out.println("Do delete on " + this);
                controller.deleted(this);
                return true;
            }
        }
        return false;
    }

    private void change() {
        controller.changed(this);
    }

    @Override
    public boolean canDelete() {
        if (primitive.hasReadOnlyControlPoints() && primitive.isControlPointReadOnly(index)) {
            return false;
        }
        return !isVirtual() && primitive instanceof Mutable
                && !kind().isInitial();
    }

    @Override
    public boolean isVirtual() {
        return virtual;
    }

    @Override
    public boolean hit(double hx, double hy) {
        Size s = controller.getControlPointSize();
        upd();
        double x = vals[index * 2];
        double y = vals[(index * 2) + 1];
        double halfx = s.w / 2;
        double halfy = s.h / 2;
        return new Rectangle2D.Double(x - halfx, y - halfy, s.w, s.h).contains(hx, hy);
    }

    @Override
    public int hashCode() {
        return index * 98_479 + System.identityHashCode(primitive);
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
                && other.getPrimitive() == primitive;
    }

    @Override
    public Set<ControlPointKind> availableControlPointKinds() {
        return primitive.availablePointKinds(index);
    }
}
