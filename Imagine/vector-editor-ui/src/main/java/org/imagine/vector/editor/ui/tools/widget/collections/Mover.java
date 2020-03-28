package org.imagine.vector.editor.ui.tools.widget.collections;

import java.util.function.BiConsumer;

/**
 * Coalesces values when moving existing data within a coordinate map.
 *
 * @author Tim Boudreau
 */
public interface Mover<T> {

    /**
     * Called if data is being moved from one set of coordinates in the map to
     * another (which may actually be a collection), and allows the
     * <i>contents</i> of the data to be manipulated and left in place if
     * desired.
     *
     * @param oldValue The value at the source position
     * @param intoValue The value at the destination position - may be null
     * @param oldNewConsumer Consumes the replacment values for the source and
     * destination coordinate pairs; if the old value passed is null, the
     * original data will be removed from the storage; otherwise it may be
     * replaced or left as-is
     * @return The new value as updated
     */
    T coalesce(T oldValue, T intoValue, BiConsumer<T, T> oldNewConsumer);

}
