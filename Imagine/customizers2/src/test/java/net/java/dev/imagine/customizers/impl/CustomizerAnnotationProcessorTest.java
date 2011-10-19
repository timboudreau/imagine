package net.java.dev.imagine.customizers.impl;

import java.awt.Component;
import javax.swing.event.ChangeListener;
import net.java.dev.imagine.api.customizers.visualizer.ColumnDataScene;
import net.java.dev.imagine.api.properties.EnumPropertyID;
import net.java.dev.imagine.api.properties.Listenable;
import net.java.dev.imagine.api.properties.Property;
import net.java.dev.imagine.spi.customizers.CustomizesProperty;
import org.junit.Test;
import static org.junit.Assert.*;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.Widget;
import org.openide.util.ChangeSupport;

/**
 *
 * @author tim
 */
public class CustomizerAnnotationProcessorTest {

    @Test
    public void testGetSupportedAnnotationTypes() throws Exception {
        Integer in = Integer.valueOf(1);
        CustomizerFactory f = CustomizerFactory.find(in, true);
        assertNotNull(f);
        Object o = f.createCustomizer(in);
        assertNotNull(o);
//        assertTrue(o.getClass().getName(), o instanceof X);
        assertTrue(o.getClass().getName(), o.getClass().getSimpleName().equals("SceneComponent"));
        f = CustomizerFactory.find(in, false);
        assertNotNull(f);
        o = f.createCustomizer(in);
//        assertTrue(o.getClass().getName(), o instanceof X);
    }

    @Test
    public void testProperties() throws Exception {
        P p = new P();
        CustomizerFactory<?, P> f = CustomizerFactory.find(p, true);
        System.err.println("GOT A " + f);
        assertNotNull(f);
        ColumnDataScene s = new ColumnDataScene();
        Widget w = f.createCustomizerWidget(s, p);
        assertNotNull (w);
        
        Component c = f.createCustomizer(p);
        assertNotNull (c);
        assertEquals ("SceneComponent", c.getClass().getSimpleName());
    }

    static enum YY {

        FOO, BAR
    }

    @CustomizesProperty(StringBuilder.class)
    public static class CP extends Widget {

        public CP(ColumnDataScene s, Property<StringBuilder> prop) {
            super(s);
            addChild(new LabelWidget(s, prop.getDisplayName()));
            addChild(new LabelWidget(s, prop.get().toString()));
        }
    }

    static class P extends Property.Abstract<StringBuilder, Q> {

        P() {
            super(new EnumPropertyID<StringBuilder, YY>(YY.FOO, StringBuilder.class), new Q());
        }

        @Override
        public StringBuilder get() {
            return super.listenTo.sb;
        }

        @Override
        public boolean set(StringBuilder value) {
            if (value == super.listenTo.sb) {
                return false;
            }
            super.listenTo.sb.setLength(0);
            super.listenTo.sb.append(value);
            return true;
        }
    }

    static class Q implements Listenable {

        private final ChangeSupport supp = new ChangeSupport(this);
        private final StringBuilder sb = new StringBuilder("Hello world");

        @Override
        public void addChangeListener(ChangeListener cl) {
            supp.addChangeListener(cl);
        }

        @Override
        public void removeChangeListener(ChangeListener cl) {
            supp.removeChangeListener(cl);
        }
    }
}
