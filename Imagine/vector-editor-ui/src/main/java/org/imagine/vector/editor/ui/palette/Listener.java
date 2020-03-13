package org.imagine.vector.editor.ui.palette;

/**
 *
 * @author Tim Boudreau
 */
public interface Listener<T> {

    void onItemAdded(String name, T item);

    void onItemDeleted(String name);

    void onItemChanged(String name, T item);

}
