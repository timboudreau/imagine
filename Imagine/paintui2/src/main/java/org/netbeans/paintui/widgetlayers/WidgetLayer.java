package org.netbeans.paintui.widgetlayers;

import java.awt.Point;
import org.netbeans.api.visual.widget.Widget;

/**
 * Object which can be in the Lookup of a Layer which wants to provide its own
 * widgets rather than use plain painting logic.
 *
 * @author Tim Boudreau
 */
public abstract class WidgetLayer {

    public abstract WidgetFactory createWidgetController(Widget container, WidgetController controller);

    /**
     * Object which is responsible for populating one Widget
     */
    public interface WidgetFactory {

        /**
         * Add children to the widget, set layout, etc.
         */
        public void attach();
        
        /**
         * Remove children from the widget, leave it in a clean state.
         */
        public void detach();

        public SetterResult setLocation(Point location);

        public SetterResult setOpacity(float opacity);

        public void setName(String name);

        /**
         * Calls which set properties on the layer that might be handled
         * internally by a layer are delegated to the WidgetFactory, which
         * can return one of these values to indicate if the normal handling
         * of the setter should proceed.  For example, a layer which shows text
         * and uses that text for its name might want to handle setName()
         * in its own way.
         */
        public enum SetterResult {
            HANDLED,
            NOT_HANDLED;
        }
    }
    
    public interface WidgetController {
        
    }
}
