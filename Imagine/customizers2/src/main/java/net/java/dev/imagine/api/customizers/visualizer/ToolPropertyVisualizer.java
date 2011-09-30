package net.java.dev.imagine.api.customizers.visualizer;

import javax.swing.JComponent;
import net.java.dev.imagine.api.customizers.AbstractToolProperty;

/**
 *
 * @author Tim Boudreau
 */
public abstract class ToolPropertyVisualizer {
    public abstract JComponent createVisualizer(AbstractToolProperty<?,?>... props);
}
