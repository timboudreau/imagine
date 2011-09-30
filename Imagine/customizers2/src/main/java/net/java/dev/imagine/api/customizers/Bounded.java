package net.java.dev.imagine.api.customizers;

/**
 * For bounded values, an interface that can be implemented by a ToolProperty
 * to specify minimum/maximum values - for displaying, for example, a slider.
 *
 * @author Tim Boudreau
 */
public interface Bounded<T extends Number> {
    public T getMinimum();
    public T getMaximum();
}
