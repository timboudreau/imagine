package org.imagine.helpimpl;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import org.imagine.help.api.HelpItem;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;
import org.openide.windows.WindowManager;

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
        HelpItem item = findHelpItem();
        if (item != null) {
            item.open();
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    /**
     * Find the current help context according to the following algorithm:
     * <ul>
     * <li>If the currently focused component is a JComponent and has a HelpItem as
     * the client property _help or _popupHelp, return that</li>
     * <li>If the Utilities.actionsGlobalContext() lookup contains a HelpItem, return that</li>
     * <li>If the Utilities.actionsGlobalContext() lookup contains a HelpItem.Provider, which
     * returns a non-null help item, return that</li>
     * <li>If the currently focused component has an ancestor that implements HelpItem.Provider,
     * return that</li>
     * <li>If the root pane of the current focus owner, or if none, that of the application main
     * window has a HelpItem as the client property _help, return that</li>
     * </ul>
     *
     * @return
     */
    private HelpItem findHelpItem() {
        Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
        HelpItem item = null;
        if (owner instanceof JComponent) {
            item = findHelpItem((JComponent) owner, false);
        }
        if (item == null && owner instanceof HelpItem.Provider) {
            item = ((HelpItem.Provider) owner).getHelpItem();
        }
        if (item == null) {
            item = Utilities.actionsGlobalContext().lookup(HelpItem.class);
            if (item == null) {
                HelpItem.Provider helpItemProvider = Utilities
                        .actionsGlobalContext().lookup(HelpItem.Provider.class);
                if (helpItemProvider != null) {
                    item = helpItemProvider.getHelpItem();
                }
            }
        }
        if (owner != null) {
            HelpItem.Provider anc = (HelpItem.Provider) SwingUtilities.getAncestorOfClass(HelpItem.Provider.class, owner);
            if (anc != null) {
                item = anc.getHelpItem();
            }
        }
        // A HelpItem set on the main window's root pane is the global help,
        // so search for that only last, as a fallback
        if (item == null) {
            JRootPane root = null;
            if (owner != null) {
                root = SwingUtilities.getRootPane(owner);
            } else {
                Frame f = WindowManager.getDefault().getMainWindow();
                if (f instanceof JFrame) {
                    root = ((JFrame) f).getRootPane();
                }
            }
            if (root != null) {
                Object o = root.getClientProperty("_help");
                if (o instanceof HelpItem) {
                    item = ((HelpItem) o);
                }
            }
        }
        return item;
    }

    private HelpItem findHelpItem(JComponent comp, boolean includeRootPane) {
        Object o = comp.getClientProperty("_help");
        if (o instanceof HelpItem) {
            return ((HelpItem) o);
        }
        o = comp.getClientProperty("_popupHelp");
        if (o instanceof HelpItem) {
            return ((HelpItem) o);
        }
        Container parent = comp.getParent();
        if (!includeRootPane && parent instanceof JRootPane) {
            return null;
        }
        if (parent instanceof JComponent) {
            return findHelpItem((JComponent) parent, includeRootPane);
        }
        return null;
    }
}
