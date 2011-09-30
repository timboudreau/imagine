package net.java.dev.imagine.api.customizers.visualizer;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.java.dev.colorchooser.ColorChooser;
import net.java.dev.imagine.api.customizers.ToolProperty;
import org.netbeans.api.visual.widget.ComponentWidget;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.Widget;
import org.openide.util.WeakListeners;

/**
 *
 * @author Tim Boudreau
 */
public class ColorWidget extends Widget {

    private final ColorChooser cc = new ColorChooser();
    private final ToolProperty<Color, ?> prop;

    ColorWidget(ColumnDataScene scene, final ToolProperty<Color, ?> prop) {
        super(scene);
        this.prop = prop;
        setLayout(scene.getColumns().createLayout());
        LabelWidget lbl = new LabelWidget(scene, prop.name().name());
        lbl.setAlignment(LabelWidget.Alignment.LEFT);
        lbl.setVerticalAlignment(LabelWidget.VerticalAlignment.CENTER);
        addChild(lbl);
        cc.setColor(prop.get());
        addChild(new ComponentWidget(scene, cc));
        cc.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ColorChooser cc = (ColorChooser) e.getSource();
                prop.set(cc.getColor());
            }
        });
        prop.addChangeListener(WeakListeners.change(cl, prop));
    }
    private final CL cl = new CL();

    private class CL implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            Color c = prop.get();
            System.out.println(prop + " changed now " + c);
            if (c != null) {
                cc.setColor(c);
            }
        }
    }
}
