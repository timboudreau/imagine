package org.imagine.vector.editor.ui.tools.widget.actions.generic;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.List;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import static org.imagine.vector.editor.ui.tools.widget.actions.generic.ActionBuilder.KEY_HIDE_WHEN_DISABLED;
import org.openide.awt.Mnemonics;
import org.openide.util.WeakListeners;

/**
 *
 * @author Tim Boudreau
 */
final class InvisibleWhenDisabledMenuItem extends JMenuItem implements PropertyChangeListener {

    @SuppressWarnings("LeakingThisInConstructor")
    InvisibleWhenDisabledMenuItem(Action action) {
        super(action);
        if (Boolean.TRUE.equals(action.getValue(KEY_HIDE_WHEN_DISABLED))) {
            action.addPropertyChangeListener(WeakListeners.propertyChange(this, action));
            setVisible(action.isEnabled());
        }
        String name = (String) action.getValue(Action.NAME);
        if (name != null) {
            Mnemonics.setLocalizedText(this, name);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (!isVisible()) {
            return new Dimension(0, 0);
        }
        return super.getPreferredSize(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addNotify() {
        super.addNotify();
        EventQueue.invokeLater(this::handleSeparators);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt == null || "enabled".equals(evt.getPropertyName())) {
            Action a = getAction();
            if (a != null) {
                setVisible(a.isEnabled());
                handleSeparators();
            }
        }
    }

    private void handleSeparators() {
        Action a = getAction();
        if (a != null && !a.isEnabled()) {
            Container parent = getParent();
            List<Component> all;
            if (parent != null) {
                if (parent instanceof JMenu) {
                    all = Arrays.asList(((JMenu) parent).getMenuComponents());
                } else {
                    all = Arrays.asList(parent.getComponents());
                }
                int ix = all.indexOf(this);
                if (ix > 0 && ix < all.size() - 1) {
                    boolean hasSeparatorBefore = all.get(ix - 1) instanceof JSeparator;
                    boolean hasSeparatorAfter = all.get(ix + 1) instanceof JSeparator;
                    if (hasSeparatorAfter && hasSeparatorBefore) {
                        all.get(ix - 1).setVisible(false);
                    }
                }
            }
        }
    }
}
