package net.java.dev.imagine.api.properties;

/**
 *
 * @author Tim Boudreau
 */
public interface PropertyID<T> {

    String name();

    Class<T> type();
    
    String getDisplayName();
    
}
