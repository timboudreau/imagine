package org.imagine.vector.editor.ui.palette;

import com.mastfrog.function.TriConsumer;
import java.util.function.BiConsumer;
import net.dev.java.imagine.spi.palette.PaletteHandler;
import org.imagine.awt.key.PaintKey;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@Messages({
    "PaintsPalette=Fill Palette"
})
@ServiceProvider(service = PaletteHandler.class, position = 100)
public class PaintsPaletteHandler implements PaletteHandler {

    @Override
    public String displayName() {
        return Bundle.PaintsPalette();
    }

    @Override
    public <T> TriConsumer<String, T, BiConsumer<Throwable, String>> saver(Class<T> type) {
        if (type == PaintKey.class || type.isAssignableFrom(PaintKey.class)) {
            return (String a, T b, BiConsumer<Throwable, String> s) -> {
                PaletteBackend<PaintKey<?>> stor = PaletteStorage.get("paints", PaintStorage::new);
                stor.save(a, (PaintKey<?>) b, s);
            };
        }
        return null;
    }
}
