package net.dev.java.imagine.spi.tool.impl;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.dev.java.imagine.spi.tool.ToolsTest.Surface;
import net.dev.java.imagine.spi.tool.ToolsTest;
import net.dev.java.imagine.spi.tool.ToolsTest.Layer;
import org.netbeans.ProxyURLStreamHandlerFactory;
import javax.swing.Action;
import javax.swing.JToggleButton;
import javax.swing.JMenuItem;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Lookup;
import java.util.Set;
import net.dev.java.imagine.spi.tool.ToolsTest.FakeLayer;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JCheckBoxMenuItem;
import net.dev.java.imagine.api.tool.SelectedTool;
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import net.dev.java.imagine.spi.tool.ToolImplementation;
import net.dev.java.imagine.spi.tool.ToolUIContext;
import net.dev.java.imagine.spi.tool.Tools;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 * @author tim
 */
public class ToolAnnotationProcessorTest {

    @BeforeAll
    public static void init() {
        System.out.println("BEFORE DO THE THING");
        //ensure nbres protocol works
        ProxyURLStreamHandlerFactory.register();
        org.netbeans.core.startup.Main.initializeURLFactory();
    }

    @Test
    public void testActions() {
        System.out.println("DO THE THING");
        Tools t = Tools.getDefault();
        System.out.println("TOOLS: " + t);
        net.dev.java.imagine.api.tool.Tool thing = t.get("thing");
        assertNotNull(thing);
        assertEquals("Thing Tool", thing.getDisplayName());
        assertEquals("thing", thing.getName());

        InstanceContent c = new InstanceContent();
        AbstractLookup lkp = new AbstractLookup(c);

        ToolAction a = new ToolAction(thing.getName(), lkp);
        assertFalse(a.isEnabled());

        class PCL implements PropertyChangeListener {

            private PropertyChangeEvent evt;

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                this.evt = evt;
            }
        }
        PCL pcl = new PCL();
        a.addPropertyChangeListener(pcl);
        assertFalse(a.isEnabled());

        assertEquals(thing.getDisplayName(), a.getValue(Action.NAME));

        Integer toFind = Integer.valueOf(23);
        c.add(toFind);

        assertTrue(a.isEnabled());

        assertFalse(SelectedTool.getDefault().isToolSelected(thing.getName()));

        a.actionPerformed(new ActionEvent(new JMenuItem(), ActionEvent.ACTION_PERFORMED, thing.getName()));

        assertTrue(SelectedTool.getDefault().isToolSelected(thing.getName()));

//        assertNotNull (pcl.evt);
//        assertEquals ("enabled", pcl.evt.getPropertyName());

        JCheckBoxMenuItem item = (JCheckBoxMenuItem) a.getMenuPresenter();
        JToggleButton b = (JToggleButton) a.getToolbarPresenter();
        assertTrue(item.isSelected());
        assertTrue(b.isSelected());
        assertNotNull(pcl.evt);
        assertEquals(Action.SELECTED_KEY, pcl.evt.getPropertyName());
        assertTrue(a.isEnabled());
        assertTrue(item.isEnabled());
        assertTrue(b.isEnabled());

        SelectedTool.getDefault().setSelectedTool(null);
        assertFalse(item.isSelected());
//        assertFalse(b.isSelected());
    }

    @Test
    public void testSomeMethod() {
        Tools t = Tools.getDefault();
        for (net.dev.java.imagine.api.tool.Tool tool : t.allTools()) {
            System.err.println("TOOL: " + tool.getName() + " " + tool.getDisplayName() + " " + tool.getTypes());
        }

        net.dev.java.imagine.api.tool.Tool thing = t.get("thing");
        assertNotNull(thing);
        assertEquals("Thing Tool", thing.getDisplayName());
        assertEquals("thing", thing.getName());

        assertTrue(thing.getTypes().contains(Integer.class));
        assertTrue(thing.getTypes().contains(String.class));
        assertTrue(thing.getTypes().contains(Surface.class));

        assertNotNull(thing);
        Layer fl = new FakeLayer(Integer.valueOf(1)).getLayer();
        boolean attached = thing.attach(fl, ToolUIContext.DUMMY_INSTANCE);
        assertTrue(attached);
        assertNotNull("Delegate missing from lookup contents: " + 
                thing.getLookup().lookup(Object.class), thing.getLookup().lookup(Q.class));
        assertNotNull(thing.getLookup().lookup(PaintParticipant.class));
        assertNotNull(q);
        q = null;

        Layer cantAttach = new FakeLayer(Float.valueOf(1)).getLayer();
        attached = thing.attach(cantAttach, ToolUIContext.DUMMY_INSTANCE);
        assertFalse(attached);

        Layer hasSurface = new FakeLayer(new FakeSurface().getSurface()).getLayer();
        attached = thing.attach(hasSurface, ToolUIContext.DUMMY_INSTANCE);
        assertTrue(attached);
        assertNotNull(someTool);
        assertTrue(someTool.attached);
        assertNotNull(thing.getLookup().lookup(MouseListener.class));
        assertNotNull(thing.getLookup().lookup(Z.class));
        thing.detach();
        SomeTool old = someTool;
        assertFalse(old.attached);
        someTool = null;

        attached = thing.attach(hasSurface, ToolUIContext.DUMMY_INSTANCE);
        assertTrue(attached);
        assertNotNull(someTool);
        assertTrue(someTool.attached);
        assertNotSame("Instance should not have been reused", old, someTool);

        thing.attach(cantAttach, ToolUIContext.DUMMY_INSTANCE);
        assertFalse("Detach should be called on attaching to a different layer",
                someTool.attached);

        net.dev.java.imagine.api.tool.Tool sub = t.get("subtest");
        assertNotNull(sub);
        assertEquals("Sub Test", sub.getDisplayName());
        assertEquals("thingies", sub.getCategory().name());
        
    }

    static abstract class AbToolImpl extends ToolImplementation<Float> {

        protected AbToolImpl(Float f) {
            super(f);
        }
    }

    @ToolDef(category = "thingies")
    @Tool(name = "subtest", value = Float.class)
    @Messages(value = "subtest=Sub Test")
    static class AbToolSub extends AbToolImpl {

        public AbToolSub(Float f) {
            super(f);
        }
    }
    static Q q;

    @Tool(name = "thing", value = Integer.class)
    public static class Q extends MouseAdapter implements PaintParticipant {

        public Q(Integer s) {
            q = this;
        }

        @Override
        public void attachRepainter(Repainter repainter) {
            
        }

        @Override
        public void paint(Graphics2D g2d, Rectangle layerBounds, boolean commit) {
            
        }
    }

    static class Z {
    }
    static SomeTool someTool;

    @Tool(name = "thing", value = Surface.class)
    @ToolDef(name = "thing", displayNameBundle = "net/dev/java/imagine/spi/tool/impl/OtherBundle",
    iconPath = "net/dev/java/imagine/spi/tool/impl/icon.png")
    public static class SomeTool extends ToolImplementation<Surface> implements MouseListener {

        public SomeTool(Surface surf) {
            super(null);
            someTool = this;
        }
        boolean attached;

        @Override
        public void attach(Lookup.Provider on, ToolUIContext ctx) {
            super.attach(on);
            attached = true;
        }

        @Override
        public void attach(Lookup.Provider layer) {
            super.attach(layer);
            attached = true;
        }

        @Override
        public void detach() {
            super.detach();
            attached = false;
        }

        @Override
        public void createLookupContents(Set<? super Object> addTo) {
            addTo.add(new Z());
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void mousePressed(MouseEvent e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void mouseExited(MouseEvent e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    @Tool(String.class)
    public static class Whatzit extends MouseAdapter {

        public Whatzit(String surface) {
        }
    }

    @Tool(name = "thing", value = String.class)
    public static class StringThing extends KeyAdapter {

        public StringThing(String thing) {
        }
    }

    static class FakeSurface extends ToolsTest.Surface {

    }
}
