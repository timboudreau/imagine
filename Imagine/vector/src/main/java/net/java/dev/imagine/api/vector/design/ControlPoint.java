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
 * Provides a wrapper for one point in a shape, to allow for mutating it with
 * notification of things that may need to repaint it, and allows for reasonable
 * behavior in the case a point has been deleted.
 *
 * @author Tim Boudreau
 */
public interface ControlPoint extends Comparable<ControlPoint> {

    /**
     * The type of control point this is, for the UI to potentially render it
     * differently.
     *
     * @return A kind
     */
    ControlPointKind kind();

    /**
     * The index of this point in the set of control points of the owning shape.
     *
     * @return An index
     */
    int index();

    /**
     * Get the shape this control point alters.
     *
     * @return An Adjustable
     */
    Adjustable getPrimitive();

    /**
     * Get the current coordinates of this point as a Point2D.
     *
     * @return A point
     */
    Point2D.Double location();

    /**
     * If this returns false, this control point is defunct and attempts to
     * mutate it will fail - i.e. the shape has been replaced, or the number of
     * control points it now has is less than this one's index.
     *
     * @return True if the control point is valid
     */
    boolean isValid();

    /**
     * Move this point by some delta values.
     *
     * @param dx The x delta
     * @param dy The y delta
     * @return True if the shape was changed
     */
    boolean move(double dx, double dy);

    /**
     * Get the X coordinate.
     *
     * @return X coordinate
     */
    double getX();

    /**
     * Get the Y coordinate.
     *
     * @return Y coordinate
     */
    double getY();

    /**
     * Set the location of this control point.
     *
     * @param newX
     * @param newY
     * @return True if the shape was changed
     */
    boolean set(double newX, double newY);

    default boolean set(Point2D pos) {
        return set(pos.getX(), pos.getY());
    }

    /**
     * Delete this control point. Some shape types (paths) allow for dynamic
     * insertion and deletion of points; most do not.
     *
     * @return True if the point was deleted
     */
    boolean delete();

    /**
     * Determine if this control point can be deleted. If it is a virtual point,
     * likely no; if it is a destination point in a path, yes. If it is the
     * first MOVE_TO in a path, no.
     *
     * @return True if this point can be deleted
     */
    boolean canDelete();

    /**
     * Determine if this point is a phyical point which is part of the shape, or
     * a control point for quadratic or cubic curves.
     *
     * @return true if it is a control point, not a physical point
     */
    boolean isVirtual();

    /**
     * Determine if the passed coordinates are within the bounds of the control
     * point, based on the control point size passed when this instance was
     * created.
     *
     * @param hx The x coordinate
     * @param hy The y coordinate
     * @return True if it is a hit
     */
    boolean hit(double hx, double hy);

    @Override
    public default int compareTo(ControlPoint o) {
        return o == this ? 0 : Integer.compare(index(), o.index());
    }

    public default boolean isEditable() {
        return true;
    }
    /**
     * Get the set of control point kinds which are not this point's kind, which
     * this control point could be changed to. Applies to paths, where a LINE_TO
     * instruction can be replaced with a QUAD_TO or CURVE_TO.
     *
     * @return A set of possible kinds
     */
    Set<ControlPointKind> availableControlPointKinds();

    default Point2D toPoint() {
        return new Point2D.Double(getX(), getY());
    }
}
