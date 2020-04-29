package org.imagine.vector.editor.ui.spi;

import java.awt.Point;
import java.util.function.Consumer;
import org.imagine.editor.api.AspectRatio;
import org.imagine.editor.api.Zoom;
import org.netbeans.paintui.widgetlayers.SetterResult;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
public interface LayerRenderingWidget {

    SetterResult setOpacity(double opacity);

    SetterResult setLocation(Point location);

    default SetterResult setAspectRatio(AspectRatio ratio) {
        return SetterResult.NOT_HANDLED;
    }

    void setZoom(Zoom zoom);

    default void setLookupConsumer(Consumer<Lookup[]> additionaLookupConsumer) {
        // do nothing
    }
}
