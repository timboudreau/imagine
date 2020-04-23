/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.api.components;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.util.WeakSet;

/**
 * Like ChangeSupport but listeners are weakly referenced.
 *
 * @author Tim Boudreau
 */
public final class WeakChangeSupport {

    private final Object source;
    private WeakSet<ChangeListener> listeners;

    public WeakChangeSupport(Object source) {
        this.source = source;
    }

    public boolean hasListeners() {
        return listeners != null && !listeners.isEmpty();
    }

    public void addChangeListener(ChangeListener cl) {
        if (listeners == null) {
            listeners = new WeakSet<>();
        }
        listeners.add(cl);
    }

    public void removeChangeListener(ChangeListener cl) {
        listeners.remove(cl);
    }

    public void fire() {
        if (listeners != null) {
            ChangeEvent ce = new ChangeEvent(source);
            for (ChangeListener cl : listeners) {
                cl.stateChanged(ce);
            }
        }
    }
}
