package org.imagine.helpimpl;

import java.awt.Component;
import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComponent;
import org.imagine.help.api.HelpItem;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Help",
        id = "org.imagine.helpimpl.MainHelpAction"
)
@ActionRegistration(
        displayName = "#CTL_MainHelpAction"
)
@ActionReferences({
    @ActionReference(id = @ActionID(id = "org.imagine.helpimpl.MainHelpAction", category = "Help"),
            name = "Help", path = "Menu/Help")
})
@Messages("CTL_MainHelpAction=Help")
public final class MainHelpAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
        if (owner instanceof JComponent) {
            HelpItem item = findHelpItem((JComponent) owner);
            if (item != null) {
                item.open();
                return;
            }
        }
        Toolkit.getDefaultToolkit().beep();
    }

    private HelpItem findHelpItem(JComponent comp) {
        Object o = comp.getClientProperty("_help");
        if (o instanceof HelpItem) {
            return ((HelpItem) o);
        }
        Container parent = comp.getParent();
        if (parent instanceof JComponent) {
            return findHelpItem((JComponent) parent);
        }
        return null;
    }
}
