package net.java.dev.imagine.api.properties;

import java.util.Collection;

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
}
