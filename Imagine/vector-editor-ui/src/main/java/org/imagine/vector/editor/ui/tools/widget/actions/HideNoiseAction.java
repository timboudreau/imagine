/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget.actions;

import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.InputEvent.META_DOWN_MASK;
import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_H;
import org.imagine.editor.api.grid.Grid;
import org.imagine.vector.editor.ui.tools.widget.util.UIState;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
public class HideNoiseAction extends WidgetAction.Adapter {

    private static final int MODS_MASK
            = ALT_DOWN_MASK | SHIFT_DOWN_MASK | CTRL_DOWN_MASK
            | META_DOWN_MASK;
    private final int keyCode;
    private final UIState state;

    public HideNoiseAction(UIState state) {
        this(VK_H, state);
    }

    public HideNoiseAction(int keyCode, UIState state) {
        this.keyCode = keyCode;
        this.state = state;
    }

    private boolean isOurs(WidgetKeyEvent evt) {
        return keyCode == evt.getKeyCode()
                && (evt.getModifiersEx() & MODS_MASK) == 0;
    }

    UIState copy;
    boolean gridWasVisible;

    @Override
    public State keyPressed(Widget widget, WidgetKeyEvent event) {
        if (isOurs(event)) {
            copy = state.copy();
            Grid grid = Grid.getInstance();
            gridWasVisible = grid.isEnabled();
            state.setConnectorLinesVisible(false);
            state.setControlPointsVisible(false);
            grid.setEnabled(false);
            widget.getScene().repaint();
            return State.CONSUMED;
        }
        return State.REJECTED;
    }

    @Override
    public State keyReleased(Widget widget, WidgetKeyEvent event) {
        if (isOurs(event)) {
            if (copy != null) {
                state.restore(copy);
                copy = null;
                if (gridWasVisible) {
                    Grid.getInstance().setEnabled(true);
                }
            }
            widget.getScene().repaint();
            return State.CONSUMED;
        }
        return State.REJECTED;
    }
}
