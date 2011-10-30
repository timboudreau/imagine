package net.java.dev.imagine.api.properties;

import java.util.Collection;
import javax.swing.event.ChangeListener;
import org.openide.util.Parameters;

/**
 *
 * @author Tim Boudreau
 */
public class ComposedProperty<T> extends Property.Abstract<T, Value<T>> {

    private final Mutable<T> setter;
    private final Properties subs;

    private ComposedProperty(Value<T> getter, Mutable<T> setter, PropertyID<T> id) {
        this(getter, setter, id, null);
    }

    private ComposedProperty(Value<T> getter, Mutable<T> setter, PropertyID<T> id, Properties subs) {
        super(id, getter);
        this.setter = setter;
        this.subs = subs == null ? new Properties.Simple() : subs;
    }

    public static <T extends Number> Property<T> createExplicit(Value<T> getter, Mutable<T> setter, PropertyID<T> id, Explicit<T> values) {
        return new ExplicitComposedProperty<T>(getter, setter, id, values);
    }

    public static <T extends Number> Property<T> createBounded(Value<T> getter, Mutable<T> setter, PropertyID<T> id, T min, T max) {
        return createBounded(getter, setter, id, new B<T>(min, max));
    }

    private static final class B<T extends Number> implements Constrained<T> {

        private final T min;
        private final T max;

        public B(T min, T max) {
            this.min = min;
            this.max = max;
            assert min.doubleValue() < max.doubleValue() : min + " " + max;
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

    public static <T extends Number> Property<T> createBounded(Property<T> prop, T min, T max) {
        B<T> b = new B<T>(min, max);
        return new BR<T>(prop, b);
    }

    public static <T extends Number> Property<T> createBounded(Value<T> getter, Mutable<T> setter, PropertyID<T> id, Constrained<T> bounded) {
        return new BoundComposedProperty<T>(getter, setter, id, bounded);
    }

    public static <T> Property<T> create(Value<T> getter, Mutable<T> setter, PropertyID<T> id) {
        return new ComposedProperty<T>(getter, setter, id);
    }

    @Override
    public T get() {
        return listenTo.get();
    }

    @Override
    public boolean set(T value) {
        return setter.set(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Property)) {
            return false;
        }
        final Property<?> other = (Property<?>) obj;
        if (this.id != other.id() && (this.id == null || !this.id.equals(other.id()))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    private static class BoundComposedProperty<T extends Number> extends ComposedProperty<T> implements Constrained<T> {

        private final Constrained<T> bounds;

        public BoundComposedProperty(Value<T> getter, Mutable<T> setter, PropertyID<T> id, Constrained<T> bounds) {
            super(getter, setter, id);
            this.bounds = bounds;
        }

        @Override
        public T getMinimum() {
            return bounds.getMinimum();
        }

        @Override
        public T getMaximum() {
            return bounds.getMaximum();
        }
    }

    private static class ExplicitComposedProperty<T> extends ComposedProperty<T> implements Explicit<T> {

        private final Explicit<T> values;

        public ExplicitComposedProperty(Value<T> getter, Mutable<T> setter, PropertyID<T> id, Explicit<T> values) {
            super(getter, setter, id);
            this.values = values;
        }

        @Override
        public Collection<T> getValues() {
            return values.getValues();
        }
    }

    static class BR<T extends Number> implements Property<T>, Constrained<T> {

        private final Property<T> delegate;
        private final Constrained<T> bounds;

        public BR(Property<T> delegate, Constrained<T> bounds) {
            Parameters.notNull("delegate", delegate);
            Parameters.notNull("bounds", bounds);
            this.delegate = delegate;
            this.bounds = bounds;
        }

        @Override
        public PropertyID<T> id() {
            return delegate.id();
        }

        @Override
        public Class<T> type() {
            return delegate.type();
        }

        @Override
        public String getDisplayName() {
            return delegate.getDisplayName();
        }

        @Override
        public T get() {
            return delegate.get();
        }

        @Override
        public void addChangeListener(ChangeListener cl) {
            delegate.addChangeListener(cl);
        }

        @Override
        public void removeChangeListener(ChangeListener cl) {
            delegate.removeChangeListener(cl);
        }

        @Override
        public boolean set(T value) {
            return delegate.set(value);
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
        public Properties getSubProperties() {
            return delegate.getSubProperties();
        }
    }
}
