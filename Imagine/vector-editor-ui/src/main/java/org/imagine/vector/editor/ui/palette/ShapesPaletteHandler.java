package org.imagine.vector.editor.ui.palette;

import com.mastfrog.abstractions.Wrapper;
import com.mastfrog.function.TriConsumer;
import java.util.function.BiConsumer;
import net.dev.java.imagine.spi.palette.PaletteHandler;
import org.imagine.vector.editor.ui.ShapeEntry;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@Messages({
    "ShapesPalette=Shapes Palette"
})
@ServiceProvider(service = PaletteHandler.class, position = 200)
public class ShapesPaletteHandler implements PaletteHandler {

    @Override
    public String displayName() {
        return Bundle.ShapesPalette();
    }

    @Override
    public <T> TriConsumer<String, T, BiConsumer<Throwable, String>> saver(Class<T> type) {
        if (type == ShapeEntry.class || type.isAssignableFrom(ShapeElement.class)) {
            return (String name, T t, BiConsumer<Throwable, String> callback) -> {
                ShapeEntry en1 = Wrapper.find(t, ShapeEntry.class);
                PaletteBackend<ShapeElement> stor = PaletteStorage.get("shapes", ShapePaletteStorage::new);
                stor.save(name == null ? en1.getName() : name, en1, callback);
            };
        }
        return null;
    }
}
