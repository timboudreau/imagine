package net.java.dev.imagine.api.customizers;

import java.util.Collection;

/**
 *
 * @author Tim Boudreau
 */
public interface Explicit<T> {
    public Collection<T> getValues();
}
