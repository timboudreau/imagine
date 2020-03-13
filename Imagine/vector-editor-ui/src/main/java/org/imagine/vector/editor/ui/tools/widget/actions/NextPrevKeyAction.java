/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget.actions;

import java.awt.event.KeyEvent;
import org.imagine.vector.editor.ui.tools.widget.actions.NextPrevProvider.SelectOperation;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
public class NextPrevKeyAction extends WidgetAction.Adapter {

    private final NextPrevProvider provider;

    public NextPrevKeyAction(NextPrevProvider provider) {
        this.provider = provider;
    }

    @Override
    public State keyReleased(Widget widget, WidgetKeyEvent event) {
        if (event.getKeyCode() == KeyEvent.VK_TAB && (!event.isAltDown() && !event.isControlDown() && !event.isMetaDown())) {
            boolean backward = event.isShiftDown();
            if (backward) {
                return provider.selectPrev(widget, SelectOperation.fromKeyEvent(event));
            } else {
                return provider.selectNext(widget, SelectOperation.fromKeyEvent(event));
            }
        }
        return super.keyReleased(widget, event);
    }
}
