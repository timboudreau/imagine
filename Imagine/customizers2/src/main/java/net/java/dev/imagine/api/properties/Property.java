package net.java.dev.imagine.api.properties;

import java.util.Collection;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.util.ChangeSupport;
import org.openide.util.Lookup;
import org.openide.util.WeakListeners;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 *
 * @author Tim Boudreau
 */
public interface Property<T, IdType extends PropertyID<T>> extends Value<T>, Mutable<T>, Listenable, Adaptable {

    public IdType id();

    public Class<T> type();
    
    public String getDisplayName();

    public static abstract class Abstract<T, R extends Listenable, IdType extends PropertyID<T>> implements Property<T, IdType> {

        protected final R getter;
        private final Lookup lookup;
        protected final IdType id;
        private ChangeListener proxy;
        private final ChangeSupport supp = new ChangeSupport(this);

        public Abstract(IdType id, R getter, Object... contents) {
            this.getter = getter;
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
        
        public final String getDisplayName() {
            return id.getDisplayName();
        }

        protected final void fire() {
            supp.fireChange();
        }

        @Override
        public final IdType id() {
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
        
        protected void addNotify() {
            //do nothing
        }
        
        protected void removeNotify() {
            //do nothing;
        }

        @Override
        public final void addChangeListener(ChangeListener l) {
            boolean had = supp.hasListeners();
            supp.addChangeListener(l);
            if (!had) {
                addNotify();
                getter.addChangeListener(proxy = WeakListeners.change(cl, getter));
            }
        }

        public Collection<? extends Property<?,?>> getSubproperties() {
            return (Collection<? extends Property<?,?>>) lookup.lookupAll(Property.class);
        }

        @Override
        public final void removeChangeListener(ChangeListener cl) {
            supp.removeChangeListener(cl);
            if (!supp.hasListeners() && proxy != null) {
                getter.removeChangeListener(proxy);
                proxy = null;
                removeNotify();
            }
        }

        @Override
        public final String toString() {
            return id.toString();
        }
        private final CL cl = new CL();

        private final class CL implements ChangeListener {

            @Override
            public void stateChanged(ChangeEvent ce) {
                fire();
            }
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Property && ((Property<?,?>) o).id().equals(id());
        }

        @Override
        public int hashCode() {
            return id().hashCode();
        }
    }
}
