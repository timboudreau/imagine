package org.imagine.vector.editor.ui.tools.widget.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
public final class OperationListenerBroadcaster implements OperationListener {

    List<OperationListener> listeners;

    public Runnable add(OperationListener l) {
        if (listeners == null) {
            listeners = new CopyOnWriteArrayList();
        }
        listeners.add(l);
        return () -> {
            listeners.remove(l);
        };
    }

    @Override
    public void onDragStarted(Lookup lkp) {
        if (listeners != null) {
            for (OperationListener l : listeners) {
                l.onDragStarted(lkp);
            }
        }
    }

    @Override
    public void onDragCompleted(Lookup lkp) {
        if (listeners != null) {
            for (OperationListener l : listeners) {
                l.onDragCompleted(lkp);
            }
        }
    }

    @Override
    public void onDragCancelled(Lookup lkp) {
        if (listeners != null) {
            for (OperationListener l : listeners) {
                l.onDragCancelled(lkp);
            }
        }
    }

}
