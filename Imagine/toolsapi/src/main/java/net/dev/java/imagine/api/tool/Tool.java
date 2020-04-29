package net.dev.java.imagine.api.tool;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.Icon;
import net.dev.java.imagine.spi.tool.ToolDefinition;
import net.dev.java.imagine.spi.tool.ToolDriver;
import net.dev.java.imagine.spi.tool.ToolImplementation;
import net.dev.java.imagine.spi.tool.ToolUIContext;
import net.dev.java.imagine.spi.tool.impl.DefaultTools;
import net.dev.java.imagine.spi.tool.impl.ToolFactory;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.Parameters;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

/**
 * API class representing a Tool which can somehow modify an image layer.
 * <p/>
 * Instances are not constructed directly;  rather see Tools.getDefault() for
 * a registry of all known Tools.
 * <p/>
 * Tool names are globally unique;  multiple implementations of a single tool
 * can exist, each sensitive to a different type in the selected layer's lookup.
 * <p/>
 * A Tool can be minimally implemented simply by annotating a class of some 
 * listener type, e.g.
 * <pre>
 * &#064;Tool(Surface.class)
 * public class DrawTool extends MouseAdapter {
 *   private final Surface surface;
 *   public DrawTool(Surface surface) {
 *     this.surface = surface;
 *   }
 * 
 *   public void mouseDragged(MouseEvent me) {
 *     Graphics2D g = surface.getGraphics();
 *     g.setColor(Color.BLACK);
 *     g.fillRect(e.getPoint().x, e.getPoint().y, 1, 1);
 *   }
 * }
 * </pre>
 * Generally a tool also has a <i>definition</i> which defines things like
 * the icon and localized display name of a tool for use in menus and toolbars.
 * That is accomplished by using the &#064;ToolDef annotation (which may be on
 * the same class or another class).  For more nuanced uses, subclass 
 * <code>ToolImplementation</code>.
 *
 * @see net.dev.java.imagine.spi.tool.Tool
 * @see net.dev.java.imagine.spi.tool.ToolDef
 * @author Tim Boudreau
 */
public final class Tool {

    private final ToolDefinition definition;
    public static final String TOOLS_PATH = "tools/";
    private final MPL lookup;
    private final Object lock = new Object();

    @SuppressWarnings("LeakingThisInConstructor")
    Tool(ToolDefinition definition) {
        Parameters.notNull("definition", definition);
        this.definition = definition;
        lookup = new MPL(Lookups.fixed(this, definition));
    }
    
    static {
        DefaultTools.INSTANCE = new ToolFactory() {

            @Override
            public Tool create(ToolDefinition def) {
                return new Tool(def);
            }

            @Override
            public Category newCategory(String name) {
                return new Category(name);
            }
        };
    }

    public <T> T lookup(Class<T> type) {
        return current == null ? null : getLookup().lookup(type);
    }

    public Lookup getLookup() {
        return lookup;
    }
    private ToolImplementation<?> current = null;


    public boolean attach(Lookup.Provider layer) {
        new Exception("Call attach(Lookup.Provider, ToolUIContext instead").printStackTrace();
        return attach(layer, ToolUIContext.DUMMY_INSTANCE);
    }
    /**
     * Attach this tool to a Layer (or other Lookup.Provider).  This
     * signals that the tool is about to be used.
     * 
     * @param layer The layer
     * @return True if attachment succeeds
     */
    public boolean attach(Lookup.Provider layer, ToolUIContext ctx) {
        ToolImplementation<?> impl = getImplementation(layer);
        ToolImplementation<?> old;
        synchronized (lock) {
            old = current;
            if (impl == current) {
                return impl != null;
            }
            current = impl;
        }
        //don't call attach/detach under lock
        if (old != null) {
            old.detach();
        }
        if (impl != null) {
            lookup.set(impl.getLookup());
            impl.attach(layer, ctx);
        } else {
            lookup.set(null);
        }
        return impl != null;
    }

    /**
     * Detach this tool from its current layer if any
     */
    public void detach() {
        ToolImplementation<?> curr;
        synchronized(lock) {
            curr = this.current;
            this.current = null;
        }
        if (curr != null) {
            curr.detach();
        }
    }

    public boolean canAttach(Lookup.Provider layer) {
        return getImplementation(layer) != null;
    }

    public boolean isAttachedTo(Lookup.Provider layer) {
        if (current == null) {
            return false;
        }
        return current == getImplementation(layer);
    }

    private ToolImplementation<?> getImplementation(Lookup.Provider layer) {
        for (ToolDriver<?,?> driver : allImpls()) {
            ToolImplementation<?> impl = driver.create(layer);
            if (impl != null) {
                return impl;
            }
        }
        return null;
    }
    
    public Set<Class<?>> getTypes() {
        Set<Class<?>> result = new HashSet<Class<?>>();
        for (ToolDriver<?,?> d : allImpls()) {
            result.add(d.sensitiveTo());
        }
        return result;
    }
    
    private List<ToolDriver<?,?>> allImpls() {
        //XXX why is Lookups.forPath() fine in tests but broken here?
        List<ToolDriver<?,?>> result = new ArrayList<ToolDriver<?,?>>();
        FileObject fo = FileUtil.getConfigFile(TOOLS_PATH + getName());
        for (FileObject ch : fo.getChildren()) {
            Object o = ch.getAttribute("instanceCreate"); //XXX
            if (o instanceof ToolDriver) {
                result.add((ToolDriver<?,?>)o);
            }
        }
        return result;
    }

    /**
     * Get the programmatic name of the tool
     * @return 
     */
    public String getName() {
        return definition.name();
    }

    /**
     * Get an icon for the tool
     * @return 
     */
    public Icon getIcon() {
        return definition.icon();
    }

    /**
     * Get a display name for the tool
     * @return 
     */
    public String getDisplayName() {
        return definition.displayName();
    }
    
    /**
     * Get the logical category of the tool (used for dividing between toolbars, etc.)
     * @return 
     */
    public Category getCategory() {
        return definition.category();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Tool && ((Tool) obj).definition.equals(definition);
    }

    @Override
    public int hashCode() {
        return definition.hashCode();
    }

    public String toString() {
        return super.toString() + '[' + definition + ']' + ":[" + current + ']';
    }
    
    static class MPL extends ProxyLookup {

        private final Lookup base;

        MPL(Lookup base) {
            super(base);
            this.base = base;
        }

        void set(Lookup lookup) {
            Lookup[] lkps = lookup == null ? new Lookup[]{base}
                    : new Lookup[]{base, lookup};
            setLookups(lkps);
        }
    }
}
