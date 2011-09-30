package net.java.dev.imagine.api.customizers;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import javax.swing.event.ChangeListener;
import org.openide.util.ChangeSupport;
import org.openide.util.NbPreferences;
import org.openide.util.WeakListeners;

/**
 * A property, persisted globally.
 *
 * @author Tim Boudreau
 */
public abstract class AbstractToolProperty<T, R extends Enum> extends ToolProperty<T, R> {

    private final R name;
    private final Class<T> type;
    private T value;
    private final ChangeSupport supp = new ChangeSupport(this);
    private static final Map<Class<?>, Preferences> prefsCache = new HashMap<Class<?>, Preferences>();

    protected AbstractToolProperty(R name, Class<T> type) {
        this.name = name;
        this.type = type;
        PreferenceChangeListener weakListener = WeakListeners.create(PreferenceChangeListener.class, pcl, getPreferences());
        getPreferences().addPreferenceChangeListener(weakListener);
    }
    private final PCL pcl = new PCL();

    final class PCL implements PreferenceChangeListener {

        @Override
        public void preferenceChange(PreferenceChangeEvent evt) {
//            System.out.println("Preference change " + evt.getKey() + " - " + evt + " to " + AbstractToolProperty.this);
            if (isRelevant(evt)) {
                value = null;
                supp.fireChange();
            }
        }
    }

    protected boolean isRelevant(PreferenceChangeEvent e) {
        return !saving && 
                (e.getKey().equals(name().name()) 
                || e.getKey().startsWith(name().name()));
    }

    protected final Preferences getPreferences() {
        Preferences result = prefsCache.get(name.getClass());
        if (result == null) {
            result = NbPreferences.forModule(name.getClass());
            prefsCache.put(name.getClass(), result);
        }
        return result;
    }

    @Override
    public final Class<T> type() {
        return type;
    }

    @Override
    public final R name() {
        return name;
    }

    public final String getName() {
        return name.name();
    }

    protected abstract T load();

    protected abstract void save(T t);

    protected final boolean equals(T oldValue, T newValue) {
        if (oldValue == newValue) {
            return true;
        }
        if ((oldValue == null) != (newValue == null)) {
            return false;
        }
        return oldValue.equals(newValue);
    }

    @Override
    public final void set(T value) {
        if (!equals(this.value, value)) {
            System.out.println("Set " + name() + " to " + value);
            this.value = value;
            supp.fireChange();
            doSave(value);
        }
    }
    boolean saving = false;

    private void doSave(T value) {
        saving = true;
        try {
            save(value);
        } finally {
            saving = false;
        }
    }

    @Override
    public final T get() {
        if (value == null) {
            value = load();
        }
        return value;
    }

    @Override
    public final void removeChangeListener(ChangeListener listener) {
        supp.removeChangeListener(listener);
    }

    @Override
    public final void addChangeListener(ChangeListener listener) {
        supp.addChangeListener(listener);
    }

    public String toString() {
        return name().name() + " (" + type.getSimpleName() + ")=" + get();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ToolProperty && ((ToolProperty<?, ?>) obj).name() == name();
    }

    public int hashCode() {
        return name().hashCode() * name().getClass().hashCode();
    }
}
