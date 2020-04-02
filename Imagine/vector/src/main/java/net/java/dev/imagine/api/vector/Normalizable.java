package net.java.dev.imagine.api.vector;

/**
 * Indicates a shape can check whether it is normalized - which typically means
 * the 0th point is the top-leftmost point, and the point iteration order is
 * clockwise from the top left.
 *
 * @author Tim Boudreau
 */
public interface Normalizable extends Primitive {

    /**
     * Determine if this shape is normalized.
     *
     * @return True if it is normalized
     */
    boolean isNormalized();

    /**
     * Adjust the points in this shape such that it is normalized.
     */
    void normalize();
}
