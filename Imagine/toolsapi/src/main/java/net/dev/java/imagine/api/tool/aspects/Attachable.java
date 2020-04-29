package net.dev.java.imagine.api.tool.aspects;

import net.dev.java.imagine.spi.tool.ToolUIContext;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
public interface Attachable {

    default void attach(Lookup.Provider on, ToolUIContext ctx) {
        attach(on);
    }

    default void attach(Lookup.Provider on) {

    }

    void detach();
}
