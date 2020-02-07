package net.java.dev.imagine.effects.impl;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Action;
import net.java.dev.imagine.effects.api.Effect;
import net.java.dev.imagine.effects.api.EffectReceiver;
import org.openide.awt.StatusDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;

/**
 *
 * @author Tim Boudreau
 */
public class EffectAction implements Action, ContextAwareAction {

    private final String name;
    private final Lookup.Result<EffectReceiver> r;
    private final LookupListener ll = new LookupListener() {

        @Override
        public void resultChanged(LookupEvent le) {
            boolean ena = !r.allItems().isEmpty();
            if (ena) {
                boolean anyEnabled = false;
                for (EffectReceiver e : r.allInstances()) {
                    anyEnabled |= e.canApplyEffects();
                }
                if (!anyEnabled) {
                    ena = false;
                }
            }
            setEnabled(ena);
        }
    };

    public static EffectAction create(FileObject fo) {
        return new EffectAction(fo.getName());
    }

    private EffectAction(String name) {
        this(name, Utilities.actionsGlobalContext());
    }

    private EffectAction(String name, Lookup lkp) {
        this.name = name;
        r = lkp.lookupResult(EffectReceiver.class);
        r.allItems();
    }

    private Effect<?, ?> effect() {
        return Effect.getEffectByName(name);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Action a = createContextAwareInstance(Utilities.actionsGlobalContext());
        if (a.isEnabled()) {
            a.actionPerformed(e);
        } else {
            StatusDisplayer.getDefault().setStatusText(getValue(NAME) + " not enabled");
        }
    }

    @Override
    public Action createContextAwareInstance(Lookup lkp) {
        return new EffectAction(name, lkp);
    }

    private final Map<String, Object> pairs = new HashMap<String, Object>();

    @Override
    public Object getValue(String key) {
        Effect<?, ?> e = effect();
        if (Action.NAME.equals(key)) {
            return e == null ? name : e.getName();
        } else if (Action.SHORT_DESCRIPTION.equals(key)) {
            return e == null ? null : e.getDescription();
        } else {
            return pairs.get(key);
        }
    }

    @Override
    public void putValue(String key, Object value) {
        pairs.put(key, value);
    }

    private boolean enabled;
    private final PropertyChangeSupport supp = new PropertyChangeSupport(this);

    @Override
    public void setEnabled(boolean b) {
        if (b != this.enabled) {
            this.enabled = b;
            supp.firePropertyChange("enabled", !b, b);
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        boolean hadListeners = supp.getPropertyChangeListeners().length > 0;
        supp.addPropertyChangeListener(listener);
        if (!hadListeners) {
            r.addLookupListener(ll);
            r.allInstances();
            ll.resultChanged(null);
        }
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        supp.removePropertyChangeListener(listener);
        if (supp.getPropertyChangeListeners().length == 0) {
            r.removeLookupListener(ll);
        }
    }
}
