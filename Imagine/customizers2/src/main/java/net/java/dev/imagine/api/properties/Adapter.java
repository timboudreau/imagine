package net.java.dev.imagine.api.properties;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import org.openide.util.lookup.InstanceContent;

/**
 * Object which can be included in the array of objects passed to a
 * Property.Abstract constructor in order to have objects returned from
 * get() be lazily created.
 *
 * @author Tim Boudreau
 */
public abstract class Adapter<T> {
    private final Class<T> type;
    private Reference<T> ref;

    protected Adapter(Class<T> type) {
        this.type = type;
    }

    public Class<T> type() {
        return type;
    }

    private T get() {
        T result = ref == null ? null : ref.get();
        if (result == null) {
            result = create();
            ref = new WeakReference<T>(result);
        }
        return result;
    }

    public abstract T create();
    
    public InstanceContent.Convertor<Adapter<T>, T> toConvertor() {
        return new IC<T>();
    }

    private static class IC<T> implements InstanceContent.Convertor<Adapter<T>, T> {

        @Override
        public T convert(Adapter<T> t) {
            return t.create();
        }

        @Override
        public Class<? extends T> type(Adapter<T> t) {
            return t.type();
        }

        @Override
        public String id(Adapter<T> t) {
            return t.toString();
        }

        @Override
        public String displayName(Adapter<T> t) {
            return t.toString();
        }
    }
    
}
