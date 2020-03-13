package org.imagine.vector.editor.ui.palette;

import java.util.List;
import java.util.function.BiConsumer;

/**
 *
 * @author Tim Boudreau
 */
public interface PaletteBackend<T> {

    PaletteBackend<T> allNames(BiConsumer<? super Throwable, ? super List<String>> names);

    void delete(String name, BiConsumer<? super Throwable, Boolean> c);

    void load(String name, BiConsumer<? super Throwable, ? super T> c);

    void save(String name, T obj, BiConsumer<? super Throwable, ? super String> bi);

    PaletteBackend<T> listen(Listener<? super T> listener);

    PaletteBackend<T> unlisten(Listener<? super T> listener);

}
