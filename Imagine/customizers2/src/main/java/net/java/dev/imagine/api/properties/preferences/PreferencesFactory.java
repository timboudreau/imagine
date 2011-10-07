package net.java.dev.imagine.api.properties.preferences;

import java.util.Arrays;
import java.util.prefs.BackingStoreException;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import javax.swing.event.ChangeListener;
import net.java.dev.imagine.api.properties.ComposedProperty;
import net.java.dev.imagine.api.properties.Mutable;
import net.java.dev.imagine.api.properties.Property;
import net.java.dev.imagine.api.properties.PropertyID;
import net.java.dev.imagine.api.properties.Value;
import org.openide.util.ChangeSupport;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;
import org.openide.util.WeakListeners;

/**
 *
 * @author Tim Boudreau
 */
public abstract class PreferencesFactory<T> {

    public abstract Preferences getPreferences();

    public static <T> PreferencesFactory<T> createPreferencesFactory(PropertyID<T> id) {
        return new EnumPreferencesFactory<T>(id);
    }

    public abstract Value<T> createGetter(T defaultValue);

    public abstract Mutable<T> createSetter();

    public abstract Property<T, ?> createProperty(T defaultVal, Object... contents);

    private static class EnumPreferencesFactory<T> extends PreferencesFactory<T> {

        private volatile Preferences preferences;
        private final PropertyID<T> id;
        private static final Class<?>[] TYPES = new Class<?>[]{
            Integer.class, Integer.TYPE,
            Float.class, Float.TYPE, Double.class,
            Double.TYPE, Long.class, Long.TYPE, String.class, Boolean.class,
            Boolean.TYPE
        };

        public EnumPreferencesFactory(PropertyID<T> id) {
            this.id = id;
            if (!Enum.class.isAssignableFrom(id.type())) {
                if (!Arrays.asList(TYPES).contains(id.type())) {
                    throw new IllegalArgumentException("Not a type supported by preferences: " + id.type());
                }
            }
        }

        public Property<T, ?> createProperty(T defaultVal, Object... contents) {
            V v = new V(defaultVal);
            M m = new M();
            m.toNotify = v;
            return ComposedProperty.create(v, m, id, contents);
        }

        @Override
        public Preferences getPreferences() {
            if (preferences == null) {
                synchronized (this) {
                    if (preferences == null) {
                        preferences = NbPreferences.forModule(id.keyType());
                        return preferences;
                    }
                }
            }
            return preferences;
        }

        public Value<T> createGetter(T defaultValue) {
            return new V(defaultValue);
        }

        public Mutable<T> createSetter() {
            return new M();
        }

        private String name() {
            return id.name();
        }

        class M implements Mutable<T> {
            
            V toNotify;

            @Override
            public boolean set(T value) {
//                if (value != null && !id.type().isInstance(value)) {
//                    throw new ClassCastException(value.getClass().getName() + " is not a " + id.type().getName());
//                }
                Preferences p = getPreferences();
                String n = name();
                if (value == null) {
                    boolean hasIt = p.get(n, null) != null;
                    p.remove(name());
                    return hasIt;
                } else {
                    boolean result;
                    if (Integer.class == id.type() || Integer.TYPE.equals(id.type())) {
                        int old = p.getInt(n, Integer.MIN_VALUE);
                        int nue = ((Integer) value).intValue();
                        result = old != nue;
                        if (result) {
                            p.putInt(n, nue);
                        }
                    } else if (Long.class == id.type() || Long.TYPE.equals(id.type())) {
                        long old = p.getLong(n, Long.MIN_VALUE);
                        long nue = ((Long) value).longValue();
                        result = old != nue;
                        if (result) {
                            p.putLong(n, nue);
                        }
                    } else if (Float.class == id.type() || Float.TYPE.equals(id.type())) {
                        float old = p.getFloat(n, Float.MIN_VALUE);
                        float nue = ((Float) value).floatValue();
                        result = old != nue;
                        if (result) {
                            p.putFloat(n, nue);
                        }
                    } else if (Double.class == id.type() || Double.TYPE.equals(id.type())) {
                        double old = p.getDouble(n, Double.MIN_VALUE);
                        double nue = ((Double) value).doubleValue();
                        result = old != nue;
                        if (result) {
                            p.putDouble(n, nue);
                        }
                    } else if (String.class == id.type()) {
                        String old = p.get(n, "");
                        String nue = (String) value;
                        result = !old.equals(nue);
                        if (result) {
                            p.put(n, nue);
                        }
                    } else if (Boolean.class == id.type() || Boolean.TYPE.equals(id.type())) {
                        boolean hasIt = p.get(n, null) != null;
                        boolean old = p.getBoolean(n, false);
                        boolean nue = ((Boolean) value).booleanValue();
                        result = !hasIt || (old != nue);
                        if (old != nue) {
                            p.putBoolean(n, nue);
                        }
                    } else if (Enum.class.isAssignableFrom(id.type())) {
                        Enum e = (Enum) value;
                        String nm = e.name();
                        String old = p.get(n, nm);
                        result = !nm.equals(old);
                        if (result) {
                            p.put(n, nm);
                        }
                    } else {
                        throw new AssertionError(id);
                    }
                    if (result) {
                        try {
                            p.flush();
                        } catch (BackingStoreException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                    if (toNotify != null && result) {
                        toNotify.supp.fireChange();
                    }
                    return result;
                }
            }
        }

        class V implements Value<T>, PreferenceChangeListener {

            private final ChangeSupport supp = new ChangeSupport(this);
            private PreferenceChangeListener proxy;
            private final T defaultValue;

            V(T defaultValue) {
                this.defaultValue = defaultValue;
//                if (defaultValue != null && !id.type().isInstance(defaultValue)) {
//                    throw new ClassCastException("Default value " + defaultValue.getClass().getName() + " is not a " + id.type().getName());
//                }
            }

            @Override
            public T get() {
                if (Integer.class == id.type() || Integer.TYPE.equals(id.type())) {
                    int val = defaultValue == null ? 0 : ((Integer) defaultValue).intValue();
                    Object o = getPreferences().getInt(name(), val);
                    return (T) o;
                } else if (Long.class == id.type() || Long.TYPE.equals(id.type())) {
                    int val = defaultValue == null ? 0 : ((Integer) defaultValue).intValue();
                    Object o = getPreferences().getInt(name(), val);
                    return (T) o;
                } else if (Float.class == id.type() || Float.TYPE.equals(id.type())) {
                    float val = defaultValue == null ? 0 : ((Float) defaultValue).intValue();
                    Object o = getPreferences().getFloat(name(), val);
                    return (T) o;
                } else if (Double.class == id.type() || Double.TYPE.equals(id.type())) {
                    double val = defaultValue == null ? 0 : ((Double) defaultValue).doubleValue();
                    Object o = getPreferences().getDouble(name(), val);
                    return (T) o;
                } else if (String.class == id.type()) {
                    String val = (String) defaultValue;
                    Object o = getPreferences().get(name(), val);
                    return (T) o;
                } else if (Boolean.class == id.type() || Boolean.TYPE.equals(id.type())) {
                    boolean val = defaultValue == null ? false : ((Boolean) defaultValue).booleanValue();
                    Object o = getPreferences().getBoolean(name(), val);
                    return (T) o;
                } else if (Enum.class.isAssignableFrom(id.type())) {
                    String s = getPreferences().get(name(), null);
                    if (s != null) {
                        for (T t : id.type().getEnumConstants()) {
                            Enum e = (Enum) t;
                            if (s.equals(e.name())) {
                                return t;
                            }
                        }
                    }
                    return null;
                }
                throw new AssertionError(id);
            }

            @Override
            public void addChangeListener(ChangeListener cl) {
                boolean had = supp.hasListeners();
                supp.addChangeListener(cl);
                if (!had) {
                    proxy = WeakListeners.create(PreferenceChangeListener.class, this, getPreferences());
                    getPreferences().addPreferenceChangeListener(proxy);
                }
            }

            @Override
            public void removeChangeListener(ChangeListener cl) {
                supp.removeChangeListener(cl);
            }

            @Override
            public void preferenceChange(PreferenceChangeEvent evt) {
                supp.fireChange();
            }
        }
    }
}
