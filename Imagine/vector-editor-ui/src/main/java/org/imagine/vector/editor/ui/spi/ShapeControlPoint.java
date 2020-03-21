package org.imagine.vector.editor.ui.spi;

import net.java.dev.imagine.api.vector.design.ControlPoint;

/**
 * Extension to the ControlPoint interface defined for shapes, to allow them to
 * be wrappered by a ShapeElement, trigger repaints and expose siblings; most
 * importantly, a ShapeControlPoint can survive replacement of the underlying
 * shape (as long as the replacement has at least as many control points as this
 * one's index), so it is not necessary to be aware of geometry-changes such as
 * convert-to-path where the point count and positoin remains the same, but the
 * shape they belong to is replaced - so ShapeControlPoints can be fairly
 * long-lived objects that can be wrapped in GUI components, etc.
 * <code>isValid()</code> determines if a ShapeControlPoint is defunct.
 *
 * @author Tim Boudreau
 */
public interface ShapeControlPoint extends ControlPoint {

    /**
     * Get the shape element this control point was created from; its underlying
     * vector shape is the source of the raw ControlPoint instance that this
     * control point wraps.
     *
     * @return The owner element
     */
    ShapeElement owner();

    /**
     * Get the set of this and all siblings of this control point at the time of
     * its creation.
     *
     * @return An array of control points, one of which has the same identity as
     * this one. Returns an empty array if the point has become invalid.
     */
    ShapeControlPoint[] family();

    default ShapeControlPoint previous() {
        ShapeControlPoint result = null;
        ShapeControlPoint[] others = family();
        if (others.length > 0) { // invalid point
            if (index() == 0) {
                if (others.length > 0) { // invalid
                    result = others[others.length - 1];
                }
            } else {
                result = others[index() - 1];
            }
        }
        return result == this ? null : result;
    }

    default ShapeControlPoint next() {
        ShapeControlPoint result = null;
        ShapeControlPoint[] others = family();
        if (others.length > 0) {
            int ix = index();
            if (ix < others.length - 1) {
                result = others[ix + 1];
            } else {
                result = others[0];
            }
        }
        return result == this ? null : result;
    }

    default ShapeControlPoint nextPhysical() {
        ShapeControlPoint result = next();
        while (result != null && result != this && result.isVirtual()) {
            result = result.next();
        }
        return result;
    }

    default ShapeControlPoint previousPhysical() {
        ShapeControlPoint result = previous();
        while (result != null && result != this && result.isVirtual()) {
            result = result.previous();
        }
        return result;
    }

    @Override
    default int compareTo(ControlPoint o) {
        if (o instanceof ShapeControlPoint) {
            ShapeControlPoint sc = (ShapeControlPoint) o;
            int result = Long.compare(owner().id(), sc.owner().id());
            if (result != 0) {
                return result;
            }
        }
        return ControlPoint.super.compareTo(o);
    }

}
