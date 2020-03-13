package org.imagine.vector.editor.ui.palette;

import java.util.function.Consumer;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Tim Boudreau
 */
public abstract class PaletteItemEditorSupplier<T> {

    public abstract void edit(Consumer<? super T> saver);

    public static <T> PaletteItemEditorSupplier<T> find(Class<? super T> type) {
        String path = "palette-edit/" + type.getName().replace('.', '/');
        Lookup lkp = Lookups.forPath(path);
        PaletteItemEditorSupplier<T> supplier
                = lkp.lookup(PaletteItemEditorSupplier.class);
        return supplier;
    }
}
