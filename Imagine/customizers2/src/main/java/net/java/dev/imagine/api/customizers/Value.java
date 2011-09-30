package net.java.dev.imagine.api.customizers;

import javax.swing.event.ChangeListener;

/**
 * Represents a mutable value of some type.
 *
 * @author Tim Boudreau
 */
public interface Value<T> {

    public T get();

    public void addChangeListener(ChangeListener c);

    public void removeChangeListener(ChangeListener c);
}
