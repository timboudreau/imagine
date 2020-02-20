/*
 * Primitive.java
 *
 * Created on October 23, 2006, 8:47 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector;

import java.awt.Graphics2D;
import java.io.Serializable;
import java.util.function.Consumer;

/**
 * Parent interface for all vector primitives. A primitive is an object that
 * represents the data passed to one call on a Graphics2D object. This includes
 * both shapes that are painted, and colors and strokes that are set. These are
 * used to create a stream of events that can be later replayed to another
 * Graphics context to recreate the elements painted.
 *
 * @see net.java.dev.imagine.api.vector.painting. VectorWrapperGraphics
 * @author Tim Boudreau
 */
public interface Primitive extends Serializable {

    public static final int[] EMPTY_INT = new int[0];

    /**
     * Paint this primitive (this may mean not painting anything, but setting
     * the stroke or color represented by this object) - apply this object's
     * data to the passed graphics context.
     */
    public void paint(Graphics2D g);

    /**
     * Create a duplicate of this Primitive, of the same type as this primitive
     * (or functionally and visually identical to it), whose data is independent
     * from this one, and which can be altered without affecting the original.
     *
     * @return An independent duplicate of this primitive
     */
    public Primitive copy();

    /**
     * Determin if an object of the passed type can be obtained from the
     * <code>as(type)</code> method.
     *
     * @param type The type
     * @return True if the query will succed
     */
    default boolean is(Class<?> type) {
        return as(type) != null;
    }

    /**
     * For looking up this object as a certain type, or nested objects within
     * it, or some transformation of this object.
     *
     * @param <T> The type
     * @param type The type
     * @return An object of type T or null
     */
    default <T> T as(Class<T> type) {
        return type.isInstance(this) ? type.cast(this) : null;
    }

    /**
     * Call the passed consumer with this object (or an object embedded in it or
     * a transform of it) as the passed type, if one can be obtained.
     *
     * @param <T> The type
     * @param type The type
     * @param c A consumer
     * @return this, for call-chaining purposes
     */
    default <T> Primitive as(Class<T> type, Consumer<T> c) {
        if (type.isInstance(this)) {
            c.accept(type.cast(this));
        }
        return this;
    }

    /**
     * Call the passed consumer with this object (or an object embedded in it or
     * a transform of it) as the passed type, if one can be obtained.
     *
     * @param <T> The type
     * @param type The type
     * @param c A consumer
     * @param orElse Runnable to call if no such object could be obtained
     * @return this, for call-chaining purposes
     */
    default <T> Primitive as(Class<T> type, Consumer<T> c, Runnable orElse) {
        if (type.isInstance(this)) {
            c.accept(type.cast(this));
        } else {
            orElse.run();
        }
        return this;
    }

}
