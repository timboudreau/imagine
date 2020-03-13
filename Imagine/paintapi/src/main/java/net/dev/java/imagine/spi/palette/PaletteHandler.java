package net.dev.java.imagine.spi.palette;

import com.mastfrog.function.TriConsumer;
import java.util.function.BiConsumer;

/**
 *
 * @author Tim Boudreau
 */
public interface PaletteHandler {

    public String displayName();

    public <T> TriConsumer<String, T, BiConsumer<Throwable, String>>
            saver(Class<T> type);
}
