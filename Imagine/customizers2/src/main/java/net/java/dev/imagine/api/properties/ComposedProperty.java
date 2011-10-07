package net.java.dev.imagine.api.properties;

import java.util.Collection;
import javax.swing.event.ChangeListener;
import org.openide.util.Parameters;

/**
 *
 * @author Tim Boudreau
 */
public class ComposedProperty<T, IdType extends PropertyID<T>> extends Property.Abstract<T, Value<T>, IdType> {

    private final Mutable<T> setter;

    private ComposedProperty(Value<T> getter, Mutable<T> setter, IdType id, Object... contents) {
        super(id, getter, contents);
        this.setter = setter;
    }

    public static <T extends Number, IdType extends PropertyID<T>> Property<T, IdType> createExplicit(Value<T> getter, Mutable<T> setter, IdType id, Explicit<T> values, Object... contents) {
        return new ExplicitComposedProperty<T, IdType>(getter, setter, id, values, contents);
    }

    public static <T extends Number, IdType extends PropertyID<T>> Property<T, IdType> createBounded(Value<T> getter, Mutable<T> setter, IdType id, T min, T max, Object... contents) {
        return createBounded(getter, setter, id, new B<T>(min, max), contents);
    }
    
    private static final class B<T extends Number> implements Bounded<T> {
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
    
    public static <T extends Number, R extends PropertyID<T>> Property<T, R> createBounded(Property<T, R> prop, T min, T max) {
        B<T> b = new B<T>(min, max);
        return new BR<T, R>(prop, b);
    }
    
    public static <T extends Number, IdType extends PropertyID<T>> Property<T, IdType> createBounded(Value<T> getter, Mutable<T> setter, IdType id, Bounded<T> bounded, Object... contents) {
        return new BoundComposedProperty<T, IdType>(getter, setter, id, bounded, contents);
    }

    public static <T, IdType extends PropertyID<T>> Property<T, IdType> create(Value<T> getter, Mutable<T> setter, IdType id, Object... contents) {
        return new ComposedProperty<T, IdType>(getter, setter, id, contents);
    }

    @Override
    public T get() {
        return getter.get();
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
        final Property<?, ?> other = (Property<?, ?>) obj;
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

    private static class BoundComposedProperty<T extends Number, IdType extends PropertyID<T>> extends ComposedProperty<T, IdType> implements Bounded<T> {

        private final Bounded<T> bounds;

        public BoundComposedProperty(Value<T> getter, Mutable<T> setter, IdType id, Bounded<T> bounds, Object... contents) {
            super(getter, setter, id, contents);
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

    private static class ExplicitComposedProperty<T, IdType extends PropertyID<T>> extends ComposedProperty<T, IdType> implements Explicit<T> {

        private final Explicit<T> values;

        public ExplicitComposedProperty(Value<T> getter, Mutable<T> setter, IdType id, Explicit<T> values, Object... contents) {
            super(getter, setter, id, contents);
            this.values = values;
        }

        @Override
        public Collection<T> getValues() {
            return values.getValues();
        }
    }
    
    static class BR<T extends Number, R extends PropertyID<T>> implements Property<T, R>, Bounded<T> {
        private final Property<T, R> delegate;
        private final Bounded<T> bounds;

        public BR(Property<T, R> delegate, Bounded<T> bounds) {
            Parameters.notNull("delegate", delegate);
            Parameters.notNull("bounds", bounds);
            this.delegate = delegate;
            this.bounds = bounds;
        }

        @Override
        public R id() {
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
        public <R> R get(Class<R> type) {
            return delegate.get(type);
        }

        @Override
        public <R> Collection<? extends R> getAll(Class<R> type) {
            return delegate.getAll(type);
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
}
