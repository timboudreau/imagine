package org.imagine.vector.editor.ui.tools.widget.actions;

import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.action.WidgetAction.WidgetKeyEvent;
import org.netbeans.api.visual.widget.Widget;
import org.openide.util.Utilities;

/**
 *
 * @author Tim Boudreau
 */
public interface NextPrevProvider {

    WidgetAction.State selectNext(Widget following, SelectOperation op);

    WidgetAction.State selectPrev(Widget preceding, SelectOperation op);

    public static enum SelectOperation {
        ADD_TO_SELECTION,
        REMOVE_FROM_SELECTION,
        REPLACE_SELECTION;

        static SelectOperation fromKeyEvent(WidgetKeyEvent evt) {
            if (Utilities.isMac()) {
                if (evt.isControlDown()) {
                    return REMOVE_FROM_SELECTION;
                } else if (evt.isMetaDown()) {
                    return ADD_TO_SELECTION;
                }
                return REPLACE_SELECTION;

            } else {
                if (evt.isControlDown()) {
                    return ADD_TO_SELECTION;
                } else if (evt.isMetaDown()) {
                    return REMOVE_FROM_SELECTION;
                }
                return REPLACE_SELECTION;
            }
        }
    }

}
