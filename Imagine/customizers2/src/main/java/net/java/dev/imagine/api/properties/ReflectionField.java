package net.java.dev.imagine.api.properties;

import javax.swing.event.ChangeListener;
import org.openide.util.ChangeSupport;
import org.openide.util.Exceptions;

/**
 *
 * @author Tim Boudreau
 */
public final class ReflectionField<T, R> implements Value<T>, Mutable<T> {
    private final R instance;
    private final java.lang.reflect.Field fld;
    private final ChangeSupport supp = new ChangeSupport(this);

    ReflectionField(Class<R> on, R instance, String name) throws NoSuchFieldException {
        fld = on.getField(name);
        this.instance = instance;
    }

    private boolean equals(Object a, Object b) {
        if ((a == null) != (b == null)) {
            return false;
        }
        if (a == null && b == null) {
            return true;
        }
        if (a.getClass() != b.getClass()) {
            return false;
        }
        if (a.getClass().isArray()) {
            //XXX
            //XXX
        }
        return a.equals(b);
    }

    @Override
    public boolean set(T value) {
        try {
            boolean change = equals(get(), value);
            fld.set(instance, value);
            if (change) {
                supp.fireChange();
            }
            return change;
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalAccessException ex) {
            Exceptions.printStackTrace(ex);
        }
        return false;
    }

    @Override
    public T get() {
        try {
            return (T) fld.get(instance);
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalAccessException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    @Override
    public void addChangeListener(ChangeListener cl) {
        supp.addChangeListener(cl);
    }

    @Override
    public void removeChangeListener(ChangeListener cl) {
        supp.removeChangeListener(cl);
    }
    
}
