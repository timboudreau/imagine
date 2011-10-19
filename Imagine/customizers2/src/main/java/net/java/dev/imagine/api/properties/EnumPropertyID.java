package net.java.dev.imagine.api.properties;

import org.openide.util.Parameters;

/**
 *
 * @author Tim Boudreau
 */
public class EnumPropertyID<T, R extends Enum<R>> implements PropertyID<T> {

    private final R r;
    private final Class<T> type;

    public EnumPropertyID(R r, Class<T> type) {
        Parameters.notNull("type", type);
        Parameters.notNull("r", r);
        this.r = r;
        this.type = type;
    }

    public <O> EnumPropertyID<O, R> convert(Class<O> type) {
        return new EnumPropertyID(r, type);
    }

    @Override
    public Class<T> type() {
        return type;
    }

    @Override
    public String name() {
        return r.name();
    }

    @Override
    public String toString() {
        return r.toString();
    }

    public R constant() {
        return r;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + r.hashCode();
        hash = 29 * hash + this.type.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EnumPropertyID && ((EnumPropertyID) o).constant().equals(constant())
                && ((EnumPropertyID) o).type() == type();
    }

    @Override
    public String getDisplayName() {
        return r.toString();
    }

    public Class<?> keyType() {
        return r.getDeclaringClass();
    }

    public <T, M extends Enum<M>> PropertyID<T> subId(M name, Class<T> type) {
        return new Wrapped<T, M>(name, type);
    }

    private class Wrapped<Q, M extends Enum<M>> implements PropertyID<Q> {

        private final M postfix;
        private final Class<Q> type;

        public Wrapped(M postfix, Class<Q> type) {
            this.postfix = postfix;
            this.type = type;
        }

        @Override
        public String name() {
            return EnumPropertyID.this.name() + '.' + postfix.name();
        }

        @Override
        public Class<Q> type() {
            return type;
        }

        @Override
        public String getDisplayName() {
            return postfix.toString();
        }

        @Override
        public Class<?> keyType() {
            return EnumPropertyID.this.keyType();
        }

        @Override
        public <T, M extends Enum<M>> PropertyID<T> subId(M name, Class<T> type) {
            return new Wrapped<T, M>(name, type);
        }
    }
}
