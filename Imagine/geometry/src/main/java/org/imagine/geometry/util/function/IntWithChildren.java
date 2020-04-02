package org.imagine.geometry.util.function;

/**
 *
 * @author Tim Boudreau
 */
public interface IntWithChildren extends Int {

    /**
     * Create a child instance which is incremented and decremented when this
     * one is, but can be set or reset independently.
     *
     * @return
     */
    public IntWithChildren child();
}
