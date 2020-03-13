package org.imagine.vector.editor.ui.tools.widget.actions;

import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.InputEvent.META_DOWN_MASK;
import java.util.function.Consumer;
import org.imagine.vector.editor.ui.tools.MutableProxyLookup;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
public final class FocusAction extends WidgetAction.Adapter {

    private static final int ANY_MODIFIERS_MASK
            = /*SHIFT_DOWN_MASK | */CTRL_DOWN_MASK
            | ALT_DOWN_MASK | META_DOWN_MASK;

    private final MutableProxyLookup lookup;
    private final Consumer<Widget> focusPerformer;

    public FocusAction(MutableProxyLookup lookup, Consumer<Widget> focusPerformer) {
        this.lookup = lookup;
        this.focusPerformer = focusPerformer;
    }

    @Override
    public State mousePressed(Widget widget, WidgetMouseEvent event) {
        if (event.isPopupTrigger()) {
            return State.REJECTED;
        }
        if ((event.getModifiersEx() & ANY_MODIFIERS_MASK) != 0 || event.getButton() != 1) {
            return State.REJECTED;
        }
        Widget old = widget.getScene().getFocusedWidget();
        if (old == widget) {
            return State.REJECTED;
        }
        if (event.isShiftDown()) {
            lookup.addLookup(widget.getLookup());
        } else {
            lookup.lookups(widget.getLookup());
        }
        focusPerformer.accept(widget);
        widget.repaint();
        if (old != null && old != widget) {
            old.repaint();
        }
        return State.CHAIN_ONLY;
    }
}
