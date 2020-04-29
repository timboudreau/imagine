package net.dev.java.imagine.api.tool;

import java.awt.Rectangle;
import net.dev.java.imagine.spi.tool.ToolUIContext;
import net.dev.java.imagine.spi.tool.impl.UIContextAccessor;
import org.imagine.editor.api.AspectRatio;
import org.imagine.editor.api.CheckerboardBackground;
import org.imagine.editor.api.ImageEditorBackground;
import org.imagine.editor.api.Zoom;

/**
 *
 * @author Tim Boudreau
 */
public interface ToolUIContextImplementation {

    Zoom zoom();

    AspectRatio aspectRatio();

    default CheckerboardBackground background() {
        return ImageEditorBackground.getDefault().style();
    }

    default ToolUIContext toToolUIContext() {
        return UIContextAccessor.contextForImpl(this);
    }

    void fetchVisibleBounds(Rectangle into);
}
