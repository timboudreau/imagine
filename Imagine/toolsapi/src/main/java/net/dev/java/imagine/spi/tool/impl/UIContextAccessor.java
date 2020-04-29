package net.dev.java.imagine.spi.tool.impl;

import net.dev.java.imagine.api.tool.ToolUIContextImplementation;
import net.dev.java.imagine.spi.tool.ToolUIContext;
import org.openide.util.Exceptions;

/**
 * The usual accessor pattern.
 *
 * @author Tim Boudreau
 */
public abstract class UIContextAccessor {

    public static UIContextAccessor DEFAULT;

    public static ToolUIContext contextForImpl(ToolUIContextImplementation impl) {
        if (DEFAULT == null) {
            try {
                Class<?> c = Class.forName(ToolUIContext.class.getName());
            } catch (ClassNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        assert DEFAULT != null : "No implementation of UIContextAccessor";
        return DEFAULT.getContext(impl);
    }

    public abstract ToolUIContext getContext(ToolUIContextImplementation impl);
}
