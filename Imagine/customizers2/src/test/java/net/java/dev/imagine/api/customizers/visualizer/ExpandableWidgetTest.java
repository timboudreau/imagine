package net.java.dev.imagine.api.customizers.visualizer;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import net.java.dev.imagine.api.customizers.ToolProperty;
import net.java.dev.imagine.api.customizers.visualizer.ExpandableWidget.Expandable;
import net.java.dev.imagine.api.properties.Property;
import org.junit.Test;
import org.netbeans.api.visual.border.BorderFactory;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.layout.LayoutFactory.SerialAlignment;
import org.netbeans.api.visual.widget.ComponentWidget;
import static org.junit.Assert.*;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author tim
 */
public class ExpandableWidgetTest {
    
    public ExpandableWidgetTest() {
        assertTrue(true);
    }

    @Test
    public void testSomeMethod() throws InterruptedException {
        if (true) {
            return;
        }
        X x = new X();
        JFrame jf = new JFrame();
        jf.setDefaultCloseOperation(3);
        jf.setContentPane(x.createView());
        jf.pack();
        jf.setSize(500, 500);
        jf.setVisible(true);
        jf.setBackground(Color.WHITE);
        Thread.sleep(62000);
    }
    
    static class X extends ColumnDataScene {
        X() {
            setFont(new Font("Arial", Font.PLAIN, 14));
            addChild(new ExpandableWidget(this, new ExpandableImpl(this)), true);
            addChild(new ExpandableWidget(this, new ExpandableImpl(this)), true);
            setBorder(BorderFactory.createEmptyBorder(13));
            setLayout(LayoutFactory.createVerticalFlowLayout(SerialAlignment.JUSTIFY, 5));
        }
    }
    
    static class ExpandableImpl implements Expandable {
        private final ColumnDataScene scene;
        ExpandableImpl(ColumnDataScene scene) {
            this.scene = scene;
        }

        @Override
        public String getTitle() {
            return "Stuff Here";
        }

        @Override
        public List<Widget> getWidgets(boolean expanded) {
            if (!expanded) {
                JComboBox box = new JComboBox();
                DefaultComboBoxModel m = new DefaultComboBoxModel(new Object[] { "one", "two", "three" });
                box.setModel(m);
                return Collections.<Widget>singletonList(new ComponentWidget(scene, box));
            } else {
                List<Widget> results = new ArrayList<Widget>();
                Widget c1 = new Widget(scene);
                Widget c2 = new Widget(scene);
                Widget c3 = new Widget(scene);
                c1.setLayout(LayoutFactory.createHorizontalFlowLayout(SerialAlignment.JUSTIFY, 5));
                c2.setLayout(LayoutFactory.createHorizontalFlowLayout(SerialAlignment.JUSTIFY, 5));
                c1.addChild(new LabelWidget(scene, "one"));
                c1.addChild(new LabelWidget(scene, "two"));
                c1.addChild(new LabelWidget(scene, "three"));
                c2.addChild(new LabelWidget(scene, "four"));
                c2.addChild(new LabelWidget(scene, "five"));
                c2.addChild(new LabelWidget(scene, "six"));
                c2.addChild(new LabelWidget(scene, "seven"));
                c2.addChild(new LabelWidget(scene, "eight"));
                c2.addChild(new ComponentWidget(scene, new JComboBox(new Object[] {"one", "two"})));
                
                c3.addChild(new LabelWidget(scene, "a"));
                c3.addChild(new LabelWidget(scene, "b"));
                c3.addChild(new ComponentWidget(scene, new JComboBox(new Object[] {"one", "two"})));
                c3.addChild(new LabelWidget(scene, "c"));
                c3.addChild(new LabelWidget(scene, "d"));
                c3.addChild(new LabelWidget(scene, "e"));
                c3.addChild(new LabelWidget(scene, "f"));
                c3.addChild(new LabelWidget(scene, "g"));
                c3.addChild(new LabelWidget(scene, "h"));
                c3.addChild(new LabelWidget(scene, "i"));
                c3.addChild(new LabelWidget(scene, "j"));
                
                results.add(c1);
                results.add(c2);
                results.add(new ExpandableWidget(scene, new ExpandableImpl(scene)));
                results.add(c3);
                results.add(new LabelWidget(scene, "hmm hey hey"));
                results.add(new LabelWidget(scene, "this is more stuff here"));
                Property<Integer, ?> ip = ToolProperty.createIntegerProperty(E.Width, 0, 100, 50);
                Property<Color, ?> cp = ToolProperty.createColorProperty(E.Color, Color.RED);
                Property<Font, ?> fp = ToolProperty.createFontProperty(E.Font);
                results.add(new IntegerWidget(scene, ip));
                results.add(new ColorWidget(scene, cp));
                results.add(new FontWidget(scene, fp));
                
                return results;
            }
        }
    }
    
    enum E {
        Color,
        Width,
        Font
    }
}
