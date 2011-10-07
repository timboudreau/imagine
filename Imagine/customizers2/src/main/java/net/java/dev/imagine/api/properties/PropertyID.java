package net.java.dev.imagine.api.properties;

import org.openide.util.NbBundle;
import org.openide.util.Parameters;

/**
 * Identifier, type and display name for a property.
 *
 * @author Tim Boudreau
 */
public interface PropertyID<T> {

    String name();

    Class<T> type();
    
    String getDisplayName();
    
    Class<?> keyType();

    public <T, M extends Enum<M>> PropertyID<T> subId(M name, Class<T> type);
    
    public static final class Simple<T> implements PropertyID<T> {
        private final Class<T> type;
        private final String name;
        private final Class<?> bundle;

        public Simple(Class<T> type, String name, Class<?> bundle) {
            Parameters.notNull("bundle", bundle);
            Parameters.notNull("type", type);
            Parameters.notNull("name", name);
            this.type = type;
            this.name = name;
            this.bundle = bundle;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Class<T> type() {
            return type;
        }

        @Override
        public String getDisplayName() {
            return NbBundle.getMessage(bundle, name);
        }

        @Override
        public Class<?> keyType() {
            return type();
        }

        @Override
        public <T, M extends Enum<M>> PropertyID<T> subId(M name, Class<T> type) {
            return new Simple<T>(type, name() + '.' + name.name(), type);
        }
    }
}
