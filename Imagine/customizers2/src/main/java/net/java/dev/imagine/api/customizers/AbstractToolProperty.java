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

    private final PropertyID<T, R> id;
    private T value;
    private final ChangeSupport supp = new ChangeSupport(this);
    private static final Map<Class<?>, Preferences> prefsCache = new HashMap<Class<?>, Preferences>();

    protected AbstractToolProperty(PropertyID<T,R> id) {
        this.id = id;
        PreferenceChangeListener weakListener = WeakListeners.create(PreferenceChangeListener.class, pcl, getPreferences());
        getPreferences().addPreferenceChangeListener(weakListener);
    }
    protected AbstractToolProperty(R name, Class<T> type) {
        this(new PropertyID<T,R>(name, type));
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
        Preferences result = prefsCache.get(id.constant().getClass());
        if (result == null) {
            result = NbPreferences.forModule(id.constant().getClass());
            prefsCache.put(id.constant().getClass(), result);
        }
        return result;
    }

    @Override
    public final Class<T> type() {
        return id.type();
    }

    @Override
    public final R name() {
        return id.constant();
    }

    public final String getName() {
        return id.name();
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
        return name().name() + " (" + id.type().getSimpleName() + ")=" + get();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ToolProperty && ((ToolProperty<?, ?>) obj).name() == name();
    }

    public int hashCode() {
        return name().hashCode() * name().getClass().hashCode();
    }
}
