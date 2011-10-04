package net.java.dev.imagine.api.properties.persistence;

/**
 *
 * @author tim
 */
public interface Saver<T> {
    boolean save(T value);
}
