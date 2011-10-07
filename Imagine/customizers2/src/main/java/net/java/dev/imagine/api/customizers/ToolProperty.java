package net.java.dev.imagine.api.customizers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.geom.AffineTransform;
import java.util.Collection;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.java.dev.imagine.api.properties.Bounded;
import net.java.dev.imagine.api.properties.ComposedProperty;
import net.java.dev.imagine.api.properties.EnumPropertyID;
import net.java.dev.imagine.api.properties.Property;
import net.java.dev.imagine.api.properties.PropertyID;
import net.java.dev.imagine.api.properties.preferences.PreferencesFactory;
import org.openide.util.ChangeSupport;
import org.openide.util.WeakListeners;

/**
 * A property which can be persisted and shared between tools, such as 
 * foreground or background color, stroke width, etc.
 * <p/>
 * The name of the property is specified as an enum.  Enum.name() is used
 * as the preferences key;  the enum's type is used for looking up the preferences
 * node to display it;  toString on the enum should return a localized display
 * name.
 *
 * @author Tim Boudreau
 */
public abstract class ToolProperty {
    
    private ToolProperty() {}

    public static <R extends Enum<R>> Property<BasicStroke, ?> createStrokeProperty(R name) {
        return new StrokeToolProperty<R>(name);
    }

    public static <R extends Enum<R>> Property<AffineTransform, ?> createTransformProperty(R name) {
        return new AffineTransformToolProperty<R>(name);
    }

    public static <R extends Enum<R>> Property<Color, ?> createColorProperty(R name) {
        return createColorProperty(name, null);
    }

    public static <R extends Enum<R>> Property<Color, ?> createColorProperty(R name, Color defaultValue) {
        return new ColorToolProperty<R>(name, defaultValue);
    }

    public static <R extends Enum<R>> Property<Font, ?> createFontProperty(R name) {
        return new FontToolProperty<R>(name);
    }

    public static <R extends Enum<R>, T extends Enum> Property<T, ?> createEnumProperty(R name, T defaultValue) {
        return PreferencesFactory.createPreferencesFactory(new EnumPropertyID<T, R>(name, defaultValue.getDeclaringClass())).createProperty(defaultValue);
    }

    public static <R extends Enum<R>> Property<Integer, ?> createIntegerProperty(R name, int defaultValue) {
        return PreferencesFactory.createPreferencesFactory(new EnumPropertyID<Integer, R>(name, Integer.TYPE)).createProperty((Integer) defaultValue);
    }

    public static <R extends Enum<R>> Property<Integer, ?> createIntegerProperty(R name, int min, int max, int defaultValue) {
        return ComposedProperty.createBounded(createIntegerProperty(name, defaultValue), min, max);
    }

    public static <R extends Enum<R>> Property<Boolean, ?> createBooleanProperty(R name, boolean defaultValue) {
        return PreferencesFactory.createPreferencesFactory(new EnumPropertyID<Boolean, R>(name, Boolean.TYPE)).createProperty((Boolean) defaultValue);
    }

    public static <R extends Enum<R>> Property<Float, ?> createFloatProperty(R name, float defaultValue) {
        return PreferencesFactory.createPreferencesFactory(new EnumPropertyID<Float, R>(name, Float.TYPE)).createProperty((Float) defaultValue);
    }

    public static <R extends Enum<R>> Property<Float, ?> createFloatProperty(R name, float min, float max, float defaultValue) {
        assert min < max;
        return ComposedProperty.createBounded(createFloatProperty(name, defaultValue), min, max);
    }

    public static <R extends Enum<R>> Property<Double, ?> createDoubleProperty(R name, double defaultValue) {
        return PreferencesFactory.createPreferencesFactory(new EnumPropertyID<Double, R>(name, Double.TYPE)).createProperty((Double) defaultValue);
    }

    public static <R extends Enum<R>> Property<Double, ?> createDoubleProperty(R name, double min, double max, double defaultValue) {
        assert min < max;
        return ComposedProperty.createBounded(createDoubleProperty(name, defaultValue), min, max);
    }

    public final <R extends Enum<R>> Property<Integer, ?> scaled(R name) {
        Property<Double, ?> prop = createDoubleProperty(name, 0, 1, 1);
        return scale(prop);
    }
    
    public interface Provider<T> {
        public Property<T, ?> create();
    }

    public static final <R extends PropertyID<T>, T extends Number> Property<Integer, ?> scale(Property<T, R> prop) {
        return new Scaled<T>(prop);
    }

    static final class Scaled<T extends Number> implements Property<Integer, Scaled<T>>, PropertyID<Integer>, Bounded<Integer> {
        private final Property<T, ?> delegate;
        private ChangeListener proxy;
        private final ChangeSupport supp = new ChangeSupport(this);

        public Scaled(Property<T, ?> delegate) {
            this.delegate = delegate;
            if (delegate.type() != Double.class && delegate.type() != Double.TYPE && 
                    delegate.type() != Float.class && delegate.type() != Float.TYPE) {
                throw new ClassCastException("Not a double or float property: " + delegate);
            }
        }

        @Override
        public Class<Integer> type() {
            return Integer.TYPE;
        }

        @Override
        public String getDisplayName() {
            return delegate.getDisplayName();
        }

        @Override
        public Integer get() {
            double res = delegate.get().doubleValue() * 100D;
            return (int) res;
        }

        @Override
        public void addChangeListener(ChangeListener cl) {
            boolean had = supp.hasListeners();
            supp.addChangeListener(cl);
            if (!had) {
                addNotify();
            }
        }

        @Override
        public void removeChangeListener(ChangeListener cl) {
            supp.removeChangeListener(cl);
            if (!supp.hasListeners()) {
                removeNotify();
            }
        }
        
        private void addNotify() {
            proxy = WeakListeners.create(ChangeListener.class, listener, delegate);
            delegate.addChangeListener(proxy);
        }
        
        private void removeNotify() {
            if (proxy != null) {
                delegate.removeChangeListener(proxy);
            }
        }
        
        private ChangeListener listener = new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                supp.fireChange();
            }
            
        };

        @Override
        public boolean set(Integer value) {
            double val = value.doubleValue();
            val = val / 100;
            if (delegate.type() == Double.class || delegate.type() == Double.TYPE) {
                Double d = Double.valueOf(val);
                return delegate.set((T)d);
            } else if (delegate.type() == Float.class || delegate.type() == Float.TYPE) {
                Float f = Float.valueOf((float) val);
                return delegate.set((T) f);
            } else {
                throw new AssertionError(delegate.type());
            }
        }

        @Override
        public <R> R get(Class<R> type) {
            return delegate.get(type);
        }

        @Override
        public <R> Collection<? extends R> getAll(Class<R> type) {
            return delegate.getAll(type);
        }

        @Override
        public Scaled id() {
            return this;
        }

        @Override
        public String name() {
            return delegate.id().name();
        }

        @Override
        public Class<?> keyType() {
            return delegate.id().keyType();
        }

        @Override
        public <T, M extends Enum<M>> PropertyID<T> subId(M name, Class<T> type) {
            return delegate.id().subId(name, type);
        }

        @Override
        public Integer getMinimum() {
            return 0;
        }

        @Override
        public Integer getMaximum() {
            return 100;
        }
    }
}
