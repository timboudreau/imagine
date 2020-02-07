package net.java.dev.imagine.api.properties;

import net.java.dev.imagine.customizers.impl.ChangeProxy;

/**
 *
 * @author Tim Boudreau
 */
public interface Property<T> extends Value<T>, Mutable<T>, Listenable {

    public PropertyID<T> id();

    public Class<T> type();

    public String getDisplayName();

    default Properties getSubProperties() {
        return Properties.EMPTY;
    }

    public static abstract class Abstract<T, R extends Listenable> extends ChangeProxy<R> implements Property<T> {

        protected final PropertyID<T> id;

        public Abstract(PropertyID<T> id, R getter) {
            super(getter);
            this.id = id;
        }

        @Override
        public Properties getSubProperties() {
            return new Properties.Simple();
        }
        
        @Override
        public final Class<T> type() {
            return id.type();
        }

        @Override
        public final String getDisplayName() {
            return id.getDisplayName();
        }

        @Override
        public final PropertyID<T> id() {
            return id;
        }

        @Override
        public final String toString() {
            return id.toString();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Property<?> && ((Property<?>) o).id().equals(id());
        }

        @Override
        public int hashCode() {
            return id().hashCode();
        }
    }
}
