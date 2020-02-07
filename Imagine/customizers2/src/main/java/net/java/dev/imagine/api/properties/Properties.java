package net.java.dev.imagine.api.properties;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Tim Boudreau
 */
public interface Properties extends Iterable<Property<?>> {

    public static Properties EMPTY = new Properties() {
        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public Iterator<Property<?>> iterator() {
            return Collections.emptyIterator();
        }
    };

    boolean isEmpty();
    
    public static final class Simple implements Properties {
        private final List<Property<?>> props;
        public Simple(Property<?>... props) {
            this.props = Arrays.asList(props);
        }

        @Override
        public Iterator<Property<?>> iterator() {
            return props.iterator();
        }

        @Override
        public boolean isEmpty() {
            return props.isEmpty();
        }
        
    }
}
