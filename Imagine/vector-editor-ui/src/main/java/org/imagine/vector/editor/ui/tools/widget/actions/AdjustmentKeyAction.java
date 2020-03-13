/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget.actions;

import java.awt.event.KeyEvent;
import net.java.dev.imagine.api.vector.design.ShapeNames;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.widget.Widget;
import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
@NbBundle.Messages({
    "MOVE_UP=Move Up",
    "MOVE_DOWN=Move Down",
    "MOVE_LEFT=Move Left",
    "MOVE_RIGHT=Move Right",
    "# {0} - degrees",
    "# {1} - name",
    "ROTATE_CLOCKWISE=Rotate {1} {0}\u00B0 Clockwise",
    "# {0} - degrees",
    "# {1} - name",
    "ROTATE_COUNTERCLOCKWISE=Rotate {1} {0}\u00B0 Counter-Clockwise",})
public class AdjustmentKeyAction extends WidgetAction.Adapter {

    @Override
    public WidgetAction.State keyPressed(Widget widget, WidgetAction.WidgetKeyEvent event) {
        boolean isFocused = widget.getScene().getFocusedWidget() == widget;
        AdjustmentKeyActionHandler handler = widget.getLookup().lookup(AdjustmentKeyActionHandler.class);
        ShapeElement en = widget.getLookup().lookup(ShapeElement.class);
        String nm = en == null ? ""
                : en.isNameSet() ? en.getName() : ShapeNames.nameOf(en.item());
        if (isFocused && handler != null) {
            if (!event.isControlDown() && !event.isAltDown()) {
                int mult = event.isShiftDown() ? 10 : 1;
                switch (event.getKeyCode()) {
                    case KeyEvent.VK_UP:
                        return handler.moveBy(widget, 0, -1 * mult, Bundle.MOVE_UP());
                    case KeyEvent.VK_DOWN:
                        return handler.moveBy(widget, 0, 1 * mult, Bundle.MOVE_DOWN());
                    case KeyEvent.VK_LEFT:
                        return handler.moveBy(widget, -1 * mult, 0, Bundle.MOVE_LEFT());
                    case KeyEvent.VK_RIGHT:
                        return handler.moveBy(widget, 1 * mult, 0, Bundle.MOVE_RIGHT());
                }
            } else if (event.isControlDown()) {
                double mult = event.isShiftDown() ? 10 : 1;
                switch (event.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                        return handler.rotateBy(widget, -1D * mult,
                                Bundle.ROTATE_COUNTERCLOCKWISE(mult, nm));
                    case KeyEvent.VK_RIGHT:
                        return handler.rotateBy(widget, mult,
                                Bundle.ROTATE_CLOCKWISE(mult, nm));
                }
            }
        }
        return super.keyPressed(widget, event);
    }

}
