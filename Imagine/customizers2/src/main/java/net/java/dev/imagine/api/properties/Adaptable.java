package net.java.dev.imagine.api.properties;

import java.util.Collection;

/**
 *
 * @author Tim Boudreau
 */
public interface Adaptable {
    public <R> R get(Class<R> type);
    public <R> Collection<? extends R> getAll(Class<R> type);
}
