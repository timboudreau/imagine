package org.netbeans.paintui.widgetlayers;

import org.netbeans.api.visual.widget.Widget;

/**
 * Object which can be in the Lookup of a Layer which wants to provide its own
 * widgets rather than use plain painting logic.
 *
 * @author Tim Boudreau
 */
public abstract class WidgetLayer {

    public abstract WidgetFactory createWidgetController(Widget container, WidgetController controller);

}
