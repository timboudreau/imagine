package org.netbeans.paint.api.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Property change listener which is interested in a single property and 
 * casts the value.
 *
 * @author Tim Boudreau
 */
public abstract class GenericPropertyChangeListener<T> implements PropertyChangeListener {
    protected final Class<T> type;
    protected final String name;

    protected GenericPropertyChangeListener(Class<T> type, String name) {
        this.type = type;
        this.name = name;
    }

    public static <T> PropertyChangeListener create(String name, Class<T> type, BiConsumer<T, T> c) {
        return new Impl<>(type, name, c);
    }

    public static <T> PropertyChangeListener create(String name, Class<T> type, Consumer<T> c) {
        return new Impl<>(type, name, (ignored, v) -> {
            c.accept(v);
        });
    }

    static class Impl<T> extends GenericPropertyChangeListener<T> {

        private final BiConsumer<T,T> consumer;

        public Impl(Class<T> type, String name, BiConsumer<T,T> consumer) {
            super(type, name);
            this.consumer = consumer;
        }

        @Override
        public void change(T oldValue, T newValue) {
            consumer.accept(oldValue, newValue);
        }

    }

    @Override
    public final void propertyChange(PropertyChangeEvent evt) {
        if (accept(evt)) {
            change(evt.getOldValue() == null ? null
                    : type.cast(evt.getOldValue()),
                    evt.getNewValue() == null ? null
                    : type.cast(evt.getNewValue()));
        }
    }

    protected boolean accept(PropertyChangeEvent event) {
        return name.equals(event.getPropertyName());
    }

    public abstract void change(T oldValue, T newValue);
}
