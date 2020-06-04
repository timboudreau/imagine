package org.imagine.help.api;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.JComponent;
import org.imagine.help.impl.HelpComponentManagerTrampoline;

/**
 *
 * @author Tim Boudreau
 */
final class HelpItemFocusListener extends FocusAdapter {

    static final HelpItemFocusListener INSTANCE = new HelpItemFocusListener();

    @Override
    public void focusGained(FocusEvent e) {
        JComponent comp = (JComponent) e.getComponent();
        HelpComponentManagerTrampoline.getInstance().activate(comp);
    }
}
