package net.java.dev.imagine.api.properties.persistence;

/**
 *
 * @author tim
 */
public interface Loader<T> {
    T load();
}
