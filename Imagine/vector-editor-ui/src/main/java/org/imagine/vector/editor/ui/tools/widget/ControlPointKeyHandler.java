package org.imagine.vector.editor.ui.tools.widget;

import org.imagine.vector.editor.ui.spi.ShapeControlPoint;
import org.imagine.vector.editor.ui.tools.widget.actions.AdjustmentKeyActionHandler;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
class ControlPointKeyHandler implements AdjustmentKeyActionHandler {

    @Override
    public WidgetAction.State rotateBy(Widget widget, double deg, String action) {
        return WidgetAction.State.REJECTED;
    }

    @Override
    public WidgetAction.State moveBy(Widget widget, int x, int y, String action) {
        ShapeControlPoint scp = widget.getLookup().lookup(ShapeControlPoint.class);
        if (scp != null && scp.isEditable()) {
            boolean moved = scp.move(x, y);
            if (moved) {
                scp.owner().changed();
                widget.revalidate();
                widget.getScene().validate();
                widget.repaint();
                return WidgetAction.State.CONSUMED;
            }
        }
        return WidgetAction.State.REJECTED;
    }

}
