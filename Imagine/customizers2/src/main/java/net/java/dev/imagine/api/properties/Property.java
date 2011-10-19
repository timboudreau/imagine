package net.java.dev.imagine.api.properties;

import java.util.Collection;
import net.java.dev.imagine.customizers.impl.ChangeProxy;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 *
 * @author Tim Boudreau
 */
public interface Property<T> extends Value<T>, Mutable<T>, Listenable, Adaptable {

    public PropertyID<T> id();

    public Class<T> type();
    
    public String getDisplayName();

    public static abstract class Abstract<T, R extends Listenable> extends ChangeProxy<R> implements Property<T> {

        private final Lookup lookup;
        protected final PropertyID<T> id;

        public Abstract(PropertyID<T> id, R getter, Object... contents) {
            super(getter);
            this.id = id;
            InstanceContent ic = new InstanceContent();
            for (Object o : contents) {
                if (o instanceof Adapter) {
                    Adapter<?> a = (Adapter<?>) o;
                    add(a, ic);
                } else {
                    ic.add(o);
                }
            }
            lookup = new AbstractLookup(ic);
        }

        @Override
        public final Class<T> type() {
            return id.type();
        }

        private <Q> void add(Adapter<Q> a, InstanceContent c) {
            c.add(a, a.toConvertor());
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
        public final <R> R get(Class<R> type) {
            if (type.isInstance(this)) {
                return type.cast(this);
            }
            return lookup.lookup(type);
        }

        @Override
        public final <R> Collection<? extends R> getAll(Class<R> type) {
            return lookup.lookupAll(type);
        }

        @Override
        public final String toString() {
            return id.toString();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Property && ((Property<?>) o).id().equals(id());
        }

        @Override
        public int hashCode() {
            return id().hashCode();
        }
    }
}
