package org.imagine.utils;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 *
 * @author Tim Boudreau
 */
public interface Holder<T> extends Supplier<T> {

    T get();

    void set(T t);

    default boolean isSet() {
        return get() != null;
    }

    default boolean ifSet(Consumer<T> c) {
        T obj = get();
        if (obj != null) {
            c.accept(obj);
            return true;
        }
        return false;
    }

    default Holder<T> setFrom(Supplier<T> other) {
        T o = other.get();
        if (o != null) {
            set(o);
        }
        return this;
    }

    default T get(Supplier<T> ifNull) {
        T result = get();
        if (result == null) {
            result = ifNull.get();
        }
        return result;
    }

    static <T> Holder<T> create() {
        return new HolderImpl<>();
    }

    static <T> Holder<T> of(T t) {
        return new HolderImpl<>(t);
    }

    default Holder<T> copy() {
        T obj = get();
        if (obj == null) {
            return create();
        } else {
            return of(obj);
        }
    }
}
