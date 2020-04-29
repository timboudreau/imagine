package net.dev.java.imagine.spi.tool;

import java.awt.Dimension;
import java.awt.Rectangle;
import net.dev.java.imagine.api.tool.ToolUIContextImplementation;
import net.dev.java.imagine.spi.tool.impl.UIContextAccessor;
import org.imagine.editor.api.AspectRatio;
import org.imagine.editor.api.CheckerboardBackground;
import org.imagine.editor.api.Zoom;

/**
 *
 * @author Tim Boudreau
 */
public final class ToolUIContext {

    private final ToolUIContextImplementation impl;

    ToolUIContext(ToolUIContextImplementation impl) {
        this.impl = impl;
    }

    public Zoom zoom() {
        return impl.zoom();
    }

    public AspectRatio aspectRatio() {
        return impl.aspectRatio();
    }

    public CheckerboardBackground background() {
        return impl.background();
    }

    public void fetchVisibleBounds(Rectangle into) {
        impl.fetchVisibleBounds(into);
    }


    static class Adap extends UIContextAccessor {

        @Override
        public ToolUIContext getContext(ToolUIContextImplementation impl) {
            return new ToolUIContext(impl);
        }
    }

    static {
        UIContextAccessor.DEFAULT = new Adap();
    }

    public static ToolUIContext DUMMY_INSTANCE = new ToolUIContext(new ToolUIContextImplementation(){
        @Override
        public Zoom zoom() {
            return Zoom.ONE_TO_ONE;
        }

        @Override
        public AspectRatio aspectRatio() {
            return AspectRatio.create(new Dimension(640, 480));
        }

        @Override
        public ToolUIContext toToolUIContext() {
            return DUMMY_INSTANCE;
        }

        @Override
        public void fetchVisibleBounds(Rectangle into) {
            into.setFrame(0, 0, 640, 480);
        }
    });
}
