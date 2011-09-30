package net.java.dev.imagine.api.customizers.visualizer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.java.dev.imagine.api.customizers.ToolProperty;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.layout.LayoutFactory.SerialAlignment;
import org.netbeans.api.visual.widget.LayerWidget;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
public class PropertyScene extends ColumnDataScene {
    private final Map<ToolProperty<?,?>, List<Widget>> widgets = new LinkedHashMap<ToolProperty<?,?>, List<Widget>>();
    private final LayerWidget main = new LayerWidget(this);
    PropertyScene() {
        main.setLayout(LayoutFactory.createVerticalFlowLayout(SerialAlignment.JUSTIFY, 5));
    }
    
    public <T, R extends Enum> void add(ToolProperty<T, R> prop) {
        
    }
    
    protected <T, R extends Enum> Widget createWidget(ToolProperty<T, R> prop) {
        return null;
    }
    
    private ToolProperty<?,?> propertyFor(Widget w) {
        for (Map.Entry<ToolProperty<?,?>, List<Widget>> e : widgets.entrySet()) {
            if (e.getValue().contains(w)) {
                return e.getKey();
            }
        }
        return null;
    }
}
