package net.java.dev.imagine.api.customizers;

import net.java.dev.imagine.api.properties.Explicit;
import java.util.Arrays;
import java.util.Collection;

/**
 *
 * @author Tim Boudreau
 */
final class EnumToolProperty<T extends Enum, R extends Enum> extends AbstractToolProperty<T, R> implements Explicit<T> {
    private final T initialValue;

    public EnumToolProperty(R name, T initialValue) {
        super(name, initialValue.getDeclaringClass());
        this.initialValue = initialValue;
    }

    @Override
    protected T load() {
        String name = getPreferences().get(name().name(), initialValue.name());
        T result = type().getEnumConstants()[0];
        for (T check : type().getEnumConstants()) {
            if (check.name().equals(name)) {
                result = check;
                break;
            }
        }
        return result;
    }

    @Override
    protected void save(T t) {
        if (t == null) {
            getPreferences().remove(name().name());
        } else {
            getPreferences().put(name().name(), t.name());
        }
    }

    @Override
    public Collection<T> getValues() {
        return Arrays.asList(type().getEnumConstants());
    }
}
