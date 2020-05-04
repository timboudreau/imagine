/*
 * ToolSensitiveAction.java
 *
 * Created on October 25, 2006, 10:08 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.netbeans.paint.api.actions;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.netbeans.paint.api.actions.Sensor.Notifiable;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 * Any subclass needs two public constructors - one no-arg and one taking an arg
 * of Lookup
 *
 * @author Tim Boudreau
 */
public abstract class GenericContextSensitiveAction<T> implements ContextAwareAction {

    protected final Lookup lookup;
    protected Class<T> targetClass;

    protected GenericContextSensitiveAction(Lookup lookup, Class<T> c) {
        this.lookup = lookup == null ? Utilities.actionsGlobalContext()
                : lookup;

        if (this.lookup == null) {
            throw new NullPointerException("Null lookup!"); //NOI18N
        }
        init(c);
        Collection<? extends T> coll = this.lookup.lookupAll(c);
        setEnabled(checkEnabled(coll, c));
    }

    protected GenericContextSensitiveAction(Class<T> c) {
        this((Lookup) null, c);
    }

    protected GenericContextSensitiveAction(String bundleKey, Class<T> c) {
        this((Lookup) null, c);
        if (bundleKey != null) {
            String name;
            try {
                name = NbBundle.getMessage(getClass(), bundleKey);
            } catch (MissingResourceException mre) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE,
                        "Missing string from Bundle.properties in package"
                        + //NOI18N
                        "with " + getClass().getName(), mre); //NOI18N
                name = bundleKey;
            }
            setDisplayName(name);
        }
    }

    protected GenericContextSensitiveAction(Lookup lkp) {
        this.lookup = lkp;
        if (this.lookup == null) {
            throw new NullPointerException("Null lookup!");
        }
    }

    private void init(Class c) {
        if (c == null) {
            throw new NullPointerException("Passed class is null"); //NOI18N
        }
        this.targetClass = c;
        Sensor.register(lookup, c, n);
    }

    protected final Class getClassesNeededInLookupForEnablement() {
        return targetClass;
    }

    public Action createContextAwareInstance(Lookup lookup) {
        Class clazz = getClass();
        try {
            Constructor c = clazz.getConstructor(Lookup.class);
            GenericContextSensitiveAction result
                    = (GenericContextSensitiveAction) c.newInstance(lookup);
            result.init(targetClass);
            String name = (String) getValue(Action.NAME);
            if (name != null) {
                result.setDisplayName(name);
            }
            Icon icon = (Icon) getValue(Action.SMALL_ICON);
            if (icon != null) {
                result.setIcon(icon);
            }
            return result;
        } catch (IllegalArgumentException | InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException ex) {
            Logger.getLogger(clazz.getName()).log(Level.SEVERE,
                    clazz + " does not have a constructor that takes a Lookup", //NOI18N
                    ex);
        } catch (SecurityException ex) {
            throw new AssertionError(ex);
        }
        return this;
    }

    //Yes, it's a microoptimization, but why not...
    private Map<String, Object> map;

    public final Object getValue(String key) {
        return map == null ? null : map.get(key);
    }

    public final void putValue(String key, Object val) {
        Object old = map == null ? null : map.get(key);
        if (map == null) {
            map = new HashMap<>();
        }
        map.put(key, val);
        firePropertyChange(key, old, val);
    }

    public final void setEnabled(boolean b) {
        boolean was = isEnabled();
        enabled = b;
        if (enabled != was) {
            firePropertyChange("enabled", was, enabled);
        }
    }

    private boolean enabled = true;

    public final boolean isEnabled() {
        return enabled;
    }

    private void firePropertyChange(String s, Object o, Object n) {
        List<PropertyChangeListener> listeners;
        synchronized (this) {
            listeners = l;
        }
        if (listeners != null) {
            PropertyChangeListener[] ll = (PropertyChangeListener[]) listeners.toArray(new PropertyChangeListener[0]);
            if (ll.length != 0) {
                PropertyChangeEvent evt = new PropertyChangeEvent(this, s, o, n);
                for (int i = 0; i < ll.length; i++) {
                    ll[i].propertyChange(evt);
                }
            }
        }
    }

    private List<PropertyChangeListener> l;

    public final synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        boolean wasEmpty;
        if (l == null) {
            wasEmpty = true;
            l = new LinkedList<>();
        } else {
            wasEmpty = l.isEmpty();
        }
        if (l.add(listener) && wasEmpty) {
            addNotify();
        }
    }

    public final synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        if (l != null && l.remove(listener) && l.isEmpty()) {
            removeNotify();
        }
    }

    protected void addNotify() {

    }

    protected void removeNotify() {

    }

    private final N n = new N();

    final class N implements Notifiable {

        public final <T> void notify(Collection<? extends T> coll, Class<T> clazz) {
            boolean old = enabled;
            enabled = !coll.isEmpty() && checkEnabled(coll, clazz);
            if (old != enabled) {
                firePropertyChange("enabled", old, enabled); //NOI18N
            }
        }
    }

    protected <T> boolean checkEnabled(Collection<? extends T> coll, Class<T> clazz) {
        return !coll.isEmpty();
    }

    protected final void setDisplayName(String name) {
        putValue(Action.NAME, name);
    }

    protected final void setDescription(String desc) {
        putValue(Action.SHORT_DESCRIPTION, desc);
    }

    protected final void setIcon(Icon icon) {
        putValue(Action.SMALL_ICON, icon);
        putValue(Action.LARGE_ICON_KEY, icon);
    }

    protected final void setIcon(Image img) {
        Icon icon = new ImageIcon(img);
        setIcon(icon);
    }

    @Override
    public final void actionPerformed(ActionEvent ae) {
        T t = (T) lookup.lookup(targetClass);
        performAction(t);
    }

    protected abstract void performAction(T t);
}
