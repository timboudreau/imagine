/*
 * VectorPrimitive.java
 *
 * Created on October 23, 2006, 9:18 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import net.java.dev.imagine.api.vector.elements.PathIteratorWrapper;
import net.java.dev.imagine.api.vector.util.Pt;

/**
 * Represents an object which can be resolved to a java.awt.Shape and has a
 * location.
 *
 * @author Tim Boudreau
 */
public interface Vector extends Primitive, Transformable, Shaped {

    /**
     * Resolve this object to a Shape object which can be painted by a
     * Graphics2D.
     *
     * @return a shape
     */
    @Override
    Shape toShape();

    /**
     * Get an immutable location object representing the current position of
     * this object.
     */
    Pt getLocation();

    /**
     * Set the location of this object
     */
    void setLocation(double x, double y);

    /**
     * Set the location of this object to 0, 0 or the coordinates necessary to
     * place the objects bounding box such that the logical origin of the shape
     * is no less than 0, 0.
     */
    void clearLocation();

    /**
     * Create a duplicate of this object whose coordinates have been transformed
     * by the passed AffineTransform.
     */
    @Override
    Vector copy(AffineTransform transform);

    default PathIteratorWrapper toPaths() {
        if (this instanceof PathIteratorWrapper) {
            return (PathIteratorWrapper) this;
        }
        boolean fill = this instanceof Fillable ? ((Fillable) this).isFill()
                : false;
        return new PathIteratorWrapper(toShape().getPathIterator(null), fill);
    }

    @Override
    public Vector copy();

    @Override
    Rectangle getBounds();
}
