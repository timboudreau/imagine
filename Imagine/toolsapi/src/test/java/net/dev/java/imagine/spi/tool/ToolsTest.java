package net.dev.java.imagine.spi.tool;

import java.net.MalformedURLException;
import java.net.URL;
import org.openide.util.lookup.AbstractLookup;
import net.dev.java.imagine.api.tool.Category;
import net.dev.java.imagine.spi.tool.impl.DefaultTools;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import org.junit.Before;
import org.openide.util.Lookup;
import org.junit.Test;
import org.junit.BeforeClass;
import org.netbeans.junit.MockServices;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.MultiFileSystem;
import org.openide.filesystems.XMLFileSystem;
import org.openide.util.lookup.InstanceContent;
import static org.junit.Assert.*;
import org.openide.util.lookup.ServiceProvider;
import org.xml.sax.SAXException;

/**
 *
 * @author tim
 */
public class ToolsTest {

    public ToolsTest() {
        assertTrue(true);
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        MockServices.setServices(R.class);
    }

    @Before
    public void setup() throws IOException, NoSuchMethodException {
        attachedToA = null;
        attachedToB = null;
    }

    @Test
    public void sanityCheck() {
        assertNotNull (FileUtil.getConfigFile("tools"));
        assertTrue (FileUtil.getConfigFile("tools").isFolder());
    }
    
    @Test
    public void test() {
        Tools t = Tools.getDefault();
        assertTrue (t instanceof DefaultTools);
        assertFalse (t.allTools().isEmpty());
        
        Map<String, Category> m = new HashMap<String, Category>();
        for (Category c : t) {
            m.put (c.name(), c);
        }
        assertEquals(m.values() + "", 4, m.size());
        assertTrue (m.containsKey("stuff"));
        assertTrue (m.containsKey("otherStuff"));
        
        System.err.println("ALL TOOLS " + t.allTools());
        for (Category c : t) {
            System.err.println("CATEGORY " + c);
        }
        
        assertEquals (5, t.allTools().size());
        A a = new A();
        B b = new B();
        Layer layer = new FakeLayer(a, b).getLayer();
        
        net.dev.java.imagine.api.tool.Tool ta = t.getTool("someTool", "stuff");
        
        assertEquals ("Some Tool", ta.getDisplayName());
        assertEquals ("someTool", ta.getName());
        
        assertNotNull(ta);
        System.err.println("Tool A " + ta.getDisplayName() + " - " + ta.getName() + " cat " + ta.getCategory());
        
        boolean attached = ta.attach(layer);
        assertTrue(attached);
        
        assertNull (attachedToB);
        assertSame (layer, attachedToA);
        
        ta.detach();
        assertNull (attachedToA);
        
        Layer anotherLayer = new FakeLayer(b).getLayer();
        attached = ta.attach(anotherLayer);
        assertTrue (attached);
        
        assertNotNull (attachedToB);
        assertSame (anotherLayer, attachedToB);
        
        Icon icon = ta.getIcon();
        assertNotNull(icon);
        assertEquals (16, icon.getIconWidth());
        assertEquals (18, icon.getIconHeight());
        
        net.dev.java.imagine.api.tool.Tool tb = t.getTool("anotherTool", "stuff");
        assertNull(tb);
        tb = t.getTool("anotherTool", "otherStuff");
        assertNotNull(tb);
        
        System.err.println("TYPES " + tb.getTypes());
        assertEquals (1, tb.getTypes().size());
        assertSame (B.class, tb.getTypes().iterator().next());

        System.err.println("---------------------------------");
        Layer onlyA = new FakeLayer(a).getLayer();
        attached = tb.attach(onlyA);
        assertFalse(tb + "", attached);
        
        net.dev.java.imagine.api.tool.Tool ta1 = t.getTool("someTool", "stuff");
        assertEquals(ta, ta1);
    }
    
    @ServiceProvider(service=FileSystem.class)
    public static class R extends MultiFileSystem {
        public R() throws SAXException, MalformedURLException {
            URL u = ToolsTest.class.getResource("ToolsTest.xml");
            assertNotNull(u);
            System.err.println("URL is " + u);
            XMLFileSystem fs = new XMLFileSystem(u);
            setDelegates(fs);
        }
    }

    static Layer attachedToA;
    static Layer attachedToB;
    static Layer attachedToC;
    
    //Since we refactored, these just mock a bit of the API
    
    public static class Surface {
        public Surface getSurface() {
            return this;
        }
    }
    
    public static abstract class Layer implements Lookup.Provider {
        public Layer getLayer() {
            return this;
        }
        
        public Surface getSurface() {
            return new Surface();
        }
    }
    
    
    public static final class FakeLayer extends Layer {
        private final InstanceContent c = new InstanceContent();
        private final Lookup lkp = new AbstractLookup(c);
        public FakeLayer(Object... contents) {
            c.add(this);
            for (Object o : contents) {
                c.add(o);
            }
        }

        public Lookup getLookup() {
            return lkp;
        }

        
    }
}
