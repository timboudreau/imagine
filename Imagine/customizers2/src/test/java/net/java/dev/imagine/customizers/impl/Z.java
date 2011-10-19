package net.java.dev.imagine.customizers.impl;

import java.awt.BasicStroke;
import javax.swing.JPanel;
import net.java.dev.imagine.api.customizers.visualizer.ColumnDataScene;
import net.java.dev.imagine.api.properties.Property;
import net.java.dev.imagine.spi.customizers.CustomizesProperty;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
@CustomizesProperty(BasicStroke.class)
public class Z extends JPanel {

    public Z(Property<BasicStroke> prop) {
    }

    @CustomizesProperty(BasicStroke.class)
    public static class M extends Widget {

        public M(ColumnDataScene scene, Property<BasicStroke> prop) {
            super(scene);
        }
    }
}
