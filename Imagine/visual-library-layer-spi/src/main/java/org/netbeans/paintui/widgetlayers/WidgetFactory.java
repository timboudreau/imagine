package org.netbeans.paintui.widgetlayers;

import java.awt.Point;
import java.util.function.Consumer;
import org.imagine.editor.api.AspectRatio;
import org.openide.util.Lookup;

/**
 * Object which is responsible for populating one Widget
 */
public interface WidgetFactory {

    /**
     * Add children to the widget, set layout, etc.
     */
    public void attach(Consumer<Lookup[]> additionalLookupConsumer);

    /**
     * Remove children from the widget, leave it in a clean state.
     */
    public void detach();

    public SetterResult setLocation(Point location);

    public SetterResult setOpacity(float opacity);

    public void setName(String name);


    default SetterResult setAspectRatio(AspectRatio ratio) {
        return SetterResult.NOT_HANDLED;
    }


}
