package org.netbeans.paintui.widgetlayers;

import java.awt.Point;
import java.util.function.Consumer;
import org.imagine.editor.api.AspectRatio;
import org.openide.util.Lookup;

/**
 * Object which is responsible for populating one Widget for one layer of one
 * picture.
 */
public interface WidgetFactory {

    /**
     * Add children to the widget, set layout, etc.
     */
    void attach(Consumer<Lookup[]> additionalLookupConsumer);

    /**
     * Remove children from the widget, leave it in a clean state.
     */
    void detach();

    SetterResult setLocation(Point location);

    SetterResult setOpacity(float opacity);

    void setName(String name);

    boolean isUsingInternalWidget();

    default SetterResult setAspectRatio(AspectRatio ratio) {
        return SetterResult.NOT_HANDLED;
    }

}
