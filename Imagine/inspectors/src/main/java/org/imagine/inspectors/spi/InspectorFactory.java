package org.imagine.inspectors.spi;

import java.awt.Component;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
public abstract class InspectorFactory<T> {

    protected final Class<T> type;

    public InspectorFactory(Class<T> type) {
        this.type = type;
    }

    public final Class<T> type() {
        return type;
    }

    public abstract Component get(T obj, Lookup lookup, int item, int of);

    @Override
    public String toString() {
        return getClass().getSimpleName() + "("
                + type.getName() + ")";
    }
}
