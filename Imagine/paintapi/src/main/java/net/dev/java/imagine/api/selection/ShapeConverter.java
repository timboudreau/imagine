package net.dev.java.imagine.api.selection;

import java.awt.Shape;

/**
 *
 * @author Tim Boudreau
 */
public interface ShapeConverter<T> {
    public Class<T> type();
    public Shape toShape(T obj);
}
