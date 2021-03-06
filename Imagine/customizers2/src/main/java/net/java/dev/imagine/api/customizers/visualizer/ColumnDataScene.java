package net.java.dev.imagine.api.customizers.visualizer;

import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.widget.Scene;
import org.openide.util.Utilities;

/**
 *
 * @author Tim Boudreau
 */
public class ColumnDataScene extends Scene {
    private static final int gap = Utilities.isMac() ? 13 : 5;
    private final Columns columns = new Columns(gap);
    
    public ColumnDataScene() {
        setLayout (LayoutFactory.createVerticalFlowLayout(LayoutFactory.SerialAlignment.LEFT_TOP, 5));
    }
    
    public Columns getColumns() {
        return columns;
    }
}
