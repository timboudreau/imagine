/*
 * ControlPoint.java
 *
 * Created on October 30, 2006, 10:45 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.design;

import java.awt.geom.Point2D;
import java.util.Set;
import net.java.dev.imagine.api.vector.Adjustable;

/**
 *
 * @author Tim Boudreau
 */
public interface ControlPoint extends Comparable<ControlPoint> {

    ControlPointKind kind();

    int index();

    Adjustable getPrimitive();

    Point2D.Double location();

    boolean isValid();

    boolean move(double dx, double dy);

    double getX();

    double getY();

    boolean set(double dx, double dy);

    boolean delete();

    boolean canDelete();

    boolean isVirtual();

    boolean hit(double hx, double hy);

    @Override
    public default int compareTo(ControlPoint o) {
        return o == this ? 0 : Integer.compare(index(), o.index());
    }

    Set<ControlPointKind> availableControlPointKinds();

}
