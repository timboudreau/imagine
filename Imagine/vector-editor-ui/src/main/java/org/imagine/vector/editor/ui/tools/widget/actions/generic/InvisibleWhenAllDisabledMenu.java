/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget.actions.generic;

import java.awt.Component;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Set;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 *
 * @author Tim Boudreau
 */
final class InvisibleWhenAllDisabledMenu extends JMenu implements PropertyChangeListener {

    private final Set<Action> listeningTo = new HashSet<>();

    InvisibleWhenAllDisabledMenu() {

    }

    InvisibleWhenAllDisabledMenu(String text) {
        setText(text);
    }

    @Override
    public Dimension getPreferredSize() {
        if (!isVisible()) {
            return new Dimension(0, 0);
        }
        return super.getPreferredSize();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        for (Component c : getMenuComponents()) {
            if (c instanceof JMenuItem) {
                JMenuItem i = (JMenuItem) c;
                Action a = i.getAction();
                if (a != null) {
                    a.addPropertyChangeListener(this);
                    listeningTo.add(a);
                }
            }
        }
        updateVisibility();
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        for (Action a : listeningTo) {
            a.removePropertyChangeListener(this);
        }
        listeningTo.clear();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("enabled".equals(evt.getPropertyName())) {
            updateVisibility();
        }
    }

    private void updateVisibility() {
        boolean vis = false;
        for (Action a : listeningTo) {
            if (a.isEnabled()) {
                vis = true;
                break;
            }
        }
        setVisible(vis);
    }
}
