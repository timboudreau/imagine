package org.imagine.vector.editor.ui.tools.widget.actions;

import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
public interface AdjustmentKeyActionHandler {

    WidgetAction.State rotateBy(Widget widget, double deg, String action);

    WidgetAction.State moveBy(Widget widget, int x, int y, String action);
}
