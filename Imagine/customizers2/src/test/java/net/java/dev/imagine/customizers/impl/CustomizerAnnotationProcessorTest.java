package net.java.dev.imagine.customizers.impl;

import java.awt.Component;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeListener;
import net.java.dev.imagine.api.customizers.visualizer.ColumnDataScene;
import net.java.dev.imagine.api.properties.EnumPropertyID;
import net.java.dev.imagine.api.properties.Listenable;
import net.java.dev.imagine.api.properties.Properties;
import net.java.dev.imagine.api.properties.Property;
import net.java.dev.imagine.spi.customizers.CustomizesProperty;
import org.junit.Test;
import static org.junit.Assert.*;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.TextFieldInplaceEditor;
import org.netbeans.api.visual.action.WidgetAction;
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
        CustomizerFactory f = CustomizerFactory.find(in, false);
        assertNotNull(f);
        System.out.println("F is " + f);
        Object o = f.createCustomizer(in);
        System.out.println("O is " + o);
        assertNotNull(o);
//        assertTrue(o.getClass().getName(), o instanceof X);
//        assertTrue(o.getClass().getName(), o.getClass().getSimpleName().equals("SceneComponent"));
        f = CustomizerFactory.find(in, true);
        assertNotNull(f);
        ColumnDataScene s = new ColumnDataScene();
        o = f.createCustomizerWidget(s, in);
        assertNotNull (o);
//        assertTrue(o.getClass().getName(), o instanceof net.java.dev.imagine.customizers.impl.Q);
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
    
    @Test
    public void testUI() throws Exception {
        P p = new P();
        P1 p1 = new P1();
        Properties pp = new Properties.Simple(p, p1);
        
        CustomizerFactory<?,Properties> cf = CustomizerFactory.find(pp, false);
        ColumnDataScene s = new ColumnDataScene();
        Widget w = cf.createCustomizerWidget(s, pp);
        assertNotNull(w);
        JComponent comp = s.createView();
//        JComponent comp = (JComponent) cf.createCustomizer(pp);
        JFrame jf = new JFrame();
        jf.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        jf.setContentPane(comp);
        jf.setBounds(40,40, 400, 400);
        jf.setVisible(true);
        Thread.sleep(60000);
    }

    static enum YY {

        FOO, BAR
    }

    @CustomizesProperty(StringBuilder.class)
    public static class CP extends Widget implements TextFieldInplaceEditor {
        private final Property<StringBuilder> prop;

        public CP(ColumnDataScene s, Property<StringBuilder> prop) {
            super(s);
            addChild(new LabelWidget(s, prop.getDisplayName()));
            addChild(new LabelWidget(s, prop.get().toString()));
            this.prop = prop;
            WidgetAction a = ActionFactory.createInplaceEditorAction(this);
            getActions().addAction(a);
            setLayout (s.getColumns().createLayout());
        }

        @Override
        public boolean isEnabled(Widget widget) {
            return true;
        }

        @Override
        public String getText(Widget widget) {
            return this.prop.get().toString();
        }

        @Override
        public void setText(Widget widget, String string) {
            this.prop.set(new StringBuilder(string));
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
    
    static class P1 extends Property.Abstract<Integer, Q> {

        P1() {
            this (new Q());
        }
        P1(Q q) {
            super(new EnumPropertyID<Integer, YY>(YY.BAR, Integer.class), q);
            q.sb.setLength(0);
            q.sb.append("23");
        }

        @Override
        public Integer get() {
            return Integer.parseInt(super.listenTo.sb.toString());
        }

        @Override
        public boolean set(Integer value) {
            super.listenTo.sb.setLength(0);
            super.listenTo.sb.append(value + "");
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
