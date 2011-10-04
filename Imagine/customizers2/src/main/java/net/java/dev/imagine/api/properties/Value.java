package net.java.dev.imagine.api.properties;

/**
 * Represents a mutable value of some type.
 *
 * @author Tim Boudreau
 */
public interface Value<T> extends Listenable {
    T get();
}
