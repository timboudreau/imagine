package net.dev.java.imagine.spi.tool;

import net.dev.java.imagine.spi.tool.impl.DefaultTools;
import java.util.Iterator;
import java.util.Set;
import net.dev.java.imagine.api.tool.Tool;
import net.dev.java.imagine.api.tool.Category;
import org.openide.util.Lookup;

/**
 * Registry of all available tools
 *
 * @author Tim Boudreau
 */
public abstract class Tools implements Iterable<Category> {
    private volatile static Tools INSTANCE;
    public static Tools getDefault() {
        if (INSTANCE == null) {
            synchronized (Tools.class) {
                if (INSTANCE == null) {
                    INSTANCE = Lookup.getDefault().lookup(Tools.class);
                    if (INSTANCE == null) {
                        INSTANCE = new DefaultTools();
                    }
                }
            }
        }
        return INSTANCE;
    }

    /***
     * Get all registered tools
     * @return 
     */
    public abstract Set<Tool> allTools();

    /**
     * Get all categories
     * @return 
     */
    public abstract Iterator<Category> iterator();
    
    /**
     * Get a tool in a given category
     * @param name
     * @param category
     * @return 
     */
    public abstract Tool getTool(String name, String category);

    /**
     * Get all the tools in a cetagory
     * @param category
     * @return 
     */
    public abstract Iterable<Tool> forCategory(Category category);
    
    /**
     * Get a tool by name
     * @param name
     * @return 
     */
    public abstract Tool get(String name);
    
    /**
     * Construct a new instance of category
     * @param name
     * @return 
     */
    protected static final Category newCategory(String name) {
        return DefaultTools.INSTANCE.newCategory(name);
    }
}
