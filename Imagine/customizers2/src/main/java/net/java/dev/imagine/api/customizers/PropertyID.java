package net.java.dev.imagine.api.customizers;

import org.openide.util.Parameters;

/**
 *
 * @author Tim Boudreau
 */
public class PropertyID<T, R extends Enum> {
    private final R r;
    private final Class<T> type;

    public PropertyID(R r, Class<T> type) {
        Parameters.notNull("type", type);
        Parameters.notNull("r", r);
        this.r = r;
        this.type = type;
    }

    public Class<T> type() {
        return type;
    }
    
    public String name() {
        return r.name();
    }
    
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
    
    public boolean equals(Object o) {
        return o instanceof PropertyID && ((PropertyID) o).constant().equals(constant()) 
                && ((PropertyID) o).type() == type();
    }
}
