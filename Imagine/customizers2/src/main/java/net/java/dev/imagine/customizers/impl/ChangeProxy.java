package net.java.dev.imagine.customizers.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.util.ChangeSupport;
import org.openide.util.Exceptions;
import org.openide.util.WeakListeners;

/**
 * Base class for things which delegate change events to some other object which
 * supports change listeners.
 *
 * @author Tim Boudreau
 */
public abstract class ChangeProxy<T> {

    private final ChangeSupport supp = new ChangeSupport(this);
    protected final T listenTo;
    private ChangeListener proxy;
    private final CL cl = new CL();

    public ChangeProxy(T listenTo) {
        this.listenTo = listenTo;
    }

    protected final void addNotify() {
        synchronized (supp) {
            proxy = WeakListeners.change(cl, listenTo);
            attachListener(listenTo, proxy);
        }
        onAddNotify();
    }

    protected final void removeNotify() {
        ChangeListener prx;
        synchronized (this) {
            prx = this.proxy;
            this.proxy = null;
        }
        if (prx != null) {
            detachListener(listenTo, prx);
        }
        onRemoveNotify();
    }

    public final void removeChangeListener(ChangeListener listener) {
        supp.removeChangeListener(listener);
        if (!supp.hasListeners()) {
            removeNotify();
        }
    }

    protected void fireChange() {
        supp.fireChange();
    }

    public final void addChangeListener(ChangeListener listener) {
        boolean had = supp.hasListeners();
        supp.addChangeListener(listener);
        if (!had) {
            addNotify();
        }
    }

    protected void onAddNotify() {
    }

    protected void onRemoveNotify() {
    }

    protected void attachListener(T listenTo, ChangeListener proxy) {
        try {
            Method m = listenTo.getClass().getMethod("addChangeListener", ChangeListener.class);
            m.setAccessible(true);
            m.invoke(listenTo, proxy);
        } catch (IllegalAccessException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InvocationTargetException ex) {
            Exceptions.printStackTrace(ex);
        } catch (NoSuchMethodException ex) {
            Exceptions.printStackTrace(ex);
        } catch (SecurityException ex) {
            Exceptions.printStackTrace(ex);
        }

    }

    protected void detachListener(T listenTo, ChangeListener proxy) {
        try {
            Method m = listenTo.getClass().getMethod("removeChangeListener", ChangeListener.class);
            m.setAccessible(true);
            m.invoke(listenTo, proxy);
        } catch (IllegalAccessException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InvocationTargetException ex) {
            Exceptions.printStackTrace(ex);
        } catch (NoSuchMethodException ex) {
            Exceptions.printStackTrace(ex);
        } catch (SecurityException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    class CL implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent ce) {
            supp.fireChange();
        }
    }
}
