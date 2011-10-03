package net.java.dev.imagine.api.customizers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.geom.AffineTransform;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.util.ChangeSupport;

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
public abstract class ToolProperty<T, R extends Enum> implements Value<T>, Mutable<T> {

    public abstract void addChangeListener(ChangeListener listener);

    public abstract T get();

    public abstract R name();

    public abstract void removeChangeListener(ChangeListener listener);

    public abstract void set(T value);

    public abstract Class<T> type();

    public ToolProperty<?, ?>[] getSubProperties() {
        return new ToolProperty<?, ?>[0];
    }

    public static <R extends Enum> ToolProperty<BasicStroke, R> createStrokeProperty(R name) {
        return new StrokeToolProperty<R>(name);
    }

    public static <R extends Enum> ToolProperty<AffineTransform, R> createTransformProperty(R name) {
        return new AffineTransformToolProperty<R>(name);
    }

    public static <R extends Enum> ToolProperty<Color, R> createColorProperty(R name) {
        return createColorProperty(name, Color.BLUE);
    }

    public static <R extends Enum> ToolProperty<Color, R> createColorProperty(R name, Color defaultValue) {
        return new ColorToolProperty<R>(name, defaultValue);
    }

    public static <R extends Enum> ToolProperty<Font, R> createFontProperty(R name) {
        return new FontToolProperty<R>(name);
    }

    public static <R extends Enum, T extends Enum> ToolProperty<T, R> createEnumProperty(R name, T defaultValue) {
        return new EnumToolProperty<T, R>(name, defaultValue);
    }

    public static <R extends Enum> ToolProperty<Integer, R> createIntegerProperty(R name, int defaultValue) {
        return new IntegerToolProperty<R>(name, defaultValue);
    }

    public static <R extends Enum> ToolProperty<Integer, R> createIntegerProperty(R name, int min, int max, int defaultValue) {
        SimpleBounds<Integer> b = new SimpleBounds<Integer>(min, max);
        return new BoundedWrapper(new IntegerToolProperty<R>(name, defaultValue), b);
    }

    public static <R extends Enum> ToolProperty<Boolean, R> createBooleanProperty(R name, boolean defaultValue) {
        return new BooleanToolProperty<R>(name, defaultValue);
    }

    public static <R extends Enum> ToolProperty<Float, R> createFloatProperty(R name, float defaultValue) {
        return new FloatToolProperty<R>(name, defaultValue);
    }

    public static <R extends Enum> ToolProperty<Float, R> createFloatProperty(R name, float min, float max, float defaultValue) {
        assert min < max;
        SimpleBounds<Float> b = new SimpleBounds<Float>(min, max);
        return new BoundedWrapper(new FloatToolProperty<R>(name, defaultValue), b);
    }

    public static <R extends Enum> ToolProperty<Double, R> createDoubleProperty(R name, double defaultValue) {
        return new DoubleToolProperty<R>(name, defaultValue);
    }

    public static <R extends Enum> ToolProperty<Double, R> createDoubleProperty(R name, double min, double max, double defaultValue) {
        assert min < max;
        SimpleBounds<Double> b = new SimpleBounds<Double>(min, max);
        return new BoundedWrapper(new DoubleToolProperty<R>(name, defaultValue), b);
    }

    public final ToolProperty<Integer, R> scaled(R name) {
        ToolProperty<Double, R> prop = createDoubleProperty(name, 0, 1, 1);
        return new Scaled<R, Double>(prop);
    }

    public static final <R extends Enum, T extends Number> ToolProperty<Integer, R> scale(ToolProperty<T, R> prop) {
        return new Scaled<R, T>(prop);
    }

    public static interface Provider<T, R extends Enum> {

        public ToolProperty<T, R> create();
    }

    static final class BoundedWrapper<T extends Number, R extends Enum> extends ToolProperty<T, R> implements Bounded<T>, ChangeListener {

        private final ToolProperty<T, R> property;
        private final Bounded<T> bounds;
        private final ChangeSupport supp = new ChangeSupport(this);

        public BoundedWrapper(ToolProperty<T, R> property, Bounded<T> bounds) {
            this.property = property;
            this.bounds = bounds;
        }

        @Override
        public void addChangeListener(ChangeListener listener) {
            boolean hadListeners = supp.hasListeners();
            supp.addChangeListener(listener);
            if (!hadListeners) {
                property.addChangeListener(this);
            }
        }

        @Override
        public T get() {
            return property.get();
        }

        @Override
        public R name() {
            return property.name();
        }

        @Override
        public void removeChangeListener(ChangeListener listener) {
            supp.removeChangeListener(listener);
            if (!supp.hasListeners()) {
                property.removeChangeListener(this);
            }
        }

        @Override
        public void set(T value) {
            property.set(value);
        }

        @Override
        public Class<T> type() {
            return property.type();
        }

        @Override
        public T getMinimum() {
            return bounds.getMinimum();
        }

        @Override
        public T getMaximum() {
            return bounds.getMaximum();
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            supp.fireChange();
        }

        @Override
        public ToolProperty<?, ?>[] getSubProperties() {
            return property.getSubProperties();
        }
    }

    static class SimpleBounds<T extends Number> implements Bounded<T> {

        private final T min;
        private final T max;

        SimpleBounds(T min, T max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public T getMinimum() {
            return min;
        }

        @Override
        public T getMaximum() {
            return max;
        }
    }

    static class Scaled<R extends Enum, T extends Number> extends ToolProperty<Integer, R> implements ChangeListener, Bounded<Integer> {

        private final ToolProperty<T, R> prop;

        public Scaled(ToolProperty<T, R> prop) {
            this.prop = prop;
            assert prop.type() == Float.TYPE || prop.type() == Double.TYPE;
        }
        private final ChangeSupport supp = new ChangeSupport(this);

        @Override
        public void addChangeListener(ChangeListener listener) {
            boolean hadListeners = supp.hasListeners();
            supp.addChangeListener(listener);
            if (!hadListeners) {
                prop.addChangeListener(this);
            }
        }

        @Override
        public void removeChangeListener(ChangeListener listener) {
            supp.removeChangeListener(listener);
            if (!supp.hasListeners()) {
                prop.removeChangeListener(this);
            }
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            supp.fireChange();
        }

        @Override
        public Integer get() {
            double val = prop.get().doubleValue();
            int actual = (int) Math.round(val * 100D);
            return actual;
        }

        @Override
        public R name() {
            return prop.name();
        }

        @Override
        public void set(Integer value) {
            double val = value;
            val /= 100;
            if (prop.type() == Float.TYPE) {
                prop.set((T) Float.valueOf((float) val));
            } else {
                prop.set((T) Double.valueOf(val));
            }
        }

        @Override
        public Class<Integer> type() {
            return Integer.TYPE;
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
