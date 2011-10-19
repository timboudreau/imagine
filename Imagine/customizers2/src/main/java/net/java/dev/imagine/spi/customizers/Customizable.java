package net.java.dev.imagine.spi.customizers;

import javax.swing.JComponent;
import net.java.dev.imagine.api.customizers.visualizer.ColumnDataScene;
import org.netbeans.api.visual.widget.Widget;

/**
 * Interfaces objects can implement to supply their own customizers
 *
 * @author Tim Boudreau
 */
public interface Customizable {
    public JComponent getCustomizer();
    public interface WidgetCustomizable {
        public Widget createWidget(ColumnDataScene scene);
    }
}
