package net.java.dev.imagine.api.properties;

/**
 * For bounded values, an interface that can be implemented by a ToolProperty
 * to specify minimum/maximum values - for displaying, for example, a slider.
 *
 * @author Tim Boudreau
 */
public interface Constrained<T extends Number> {
    public T getMinimum();
    public T getMaximum();
}
