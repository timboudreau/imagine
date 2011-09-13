package org.netbeans.paint.api.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

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
