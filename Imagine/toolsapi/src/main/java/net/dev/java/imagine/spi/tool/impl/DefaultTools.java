package net.dev.java.imagine.spi.tool.impl;

import java.beans.BeanInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import net.dev.java.imagine.api.tool.Category;
import net.dev.java.imagine.api.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDefinition;
import net.dev.java.imagine.spi.tool.ToolDriver;
import net.dev.java.imagine.spi.tool.Tools;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.Parameters;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Tim Boudreau
 */
public final class DefaultTools extends Tools {

    public static ToolFactory INSTANCE = null;
    private final Map<Category, List<Tool>> toolsByCategory = new HashMap<Category, List<Tool>>();
    private final Map<String, Tool> byName = new HashMap<String, Tool>();

    static {
        try {
            //force initialization of INSTANCE field
            Class.forName(Tool.class.getName());
        } catch (ClassNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public Tool getTool(String name, String category) {
        Category c = INSTANCE.newCategory(category);
        List<Tool> t = toolsByCategory.get(c);
        if (t == null) {
            return null;
        }
        for (Tool tt : t) {
            if (name.equals(tt.getName())) {
                return tt;
            }
        }
        return null;
    }

    public DefaultTools() {
        FileObject fld = FileUtil.getConfigFile(Tool.TOOLS_PATH);
        assert fld != null : "tools/ folder missing";
        for (FileObject fo : fld.getChildren()) {
            try {
                DataObject dob = DataObject.find(fo);
                String cat = (String) fo.getAttribute("category");
                if (cat == null) {
                    cat = "drawing";
                }
                Category c = INSTANCE.newCategory(cat);
                Lookup lkp = Lookups.forPath(fo.getPath());
                List<Tool> l = toolsByCategory.get(c);
                if (l == null) {
                    l = new ArrayList<Tool>();
                    toolsByCategory.put(c, l);
                }
                boolean hasDrivers = !lkp.lookupResult(ToolDriver.class).allItems().isEmpty();
                if (!hasDrivers) {
                    Logger.getLogger(DefaultTools.class.getName()).log(Level.WARNING, "{0} defines a tool, but no ToolDriver instances found in it", fo.getPath() + ": " + lkp.lookup(Object.class)  );
                    for (FileObject ch : fo.getChildren()) {
                        Logger.getLogger(DefaultTools.class.getName()).log(Level.WARNING, ch.getPath());
                        Enumeration<String> en = ch.getAttributes();
                        while (en.hasMoreElements()) {
                            String k = en.nextElement();
                            Logger.getLogger(DefaultTools.class.getName()).log(Level.WARNING, "   {0}={1} ({2})", new Object[]{k, ch.getAttribute(k), ch.getAttribute(k).getClass().getName()});
                        }
                        
                    }
                }
                TD td = new TD(dob, c);
                Tool tool = INSTANCE.create(td);
                l.add(tool);
                byName.put(fo.getNameExt(), tool);
            } catch (DataObjectNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    static final class TD extends ToolDefinition {

        private final DataObject fld;
        private final Category category;

        public TD(DataObject fld, Category category) {
            Parameters.notNull("category", category);
            Parameters.notNull("fld", fld);
            this.fld = fld;
            this.category = category;
        }

        @Override
        public String displayName() {
            return fld.getNodeDelegate().getDisplayName();
        }

        @Override
        public Icon icon() {
            return ImageUtilities.image2Icon(fld.getNodeDelegate().getIcon(BeanInfo.ICON_COLOR_16x16));
        }

        @Override
        public String name() {
            return fld.getPrimaryFile().getNameExt();
        }

        @Override
        public Category category() {
            return category;
        }

        @Override
        public HelpCtx getHelpCtx() {
            return fld.getNodeDelegate().getHelpCtx();
        }

        @Override
        public String getDescription() {
            return fld.getNodeDelegate().getShortDescription();
        }

        public String toString() {
            return fld.getPrimaryFile().getPath() + " name=" + name() + " displayName=" + displayName() + " category=" + category;
        }
        
        
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            return o instanceof ToolDefinition && name().equals(((ToolDefinition) o).name());
        }
        
        public int hashCode() {
            return name().hashCode();
        }
    }

    @Override
    public Set<Tool> allTools() {
        Set<Tool> result = new HashSet<Tool>();
        for (Map.Entry<Category, List<Tool>> e : toolsByCategory.entrySet()) {
            result.addAll(e.getValue());
        }
        return result;
    }

    @Override
    public Iterator<Category> iterator() {
        return Collections.unmodifiableSet(toolsByCategory.keySet()).iterator();
    }

    @Override
    public Iterable<Tool> forCategory(Category category) {
        Parameters.notNull("category", category);
        return Collections.unmodifiableList(toolsByCategory.get(category));
    }

    @Override
    public Tool get(String name) {
        Parameters.notNull("name", name);
        return byName.get(name);
    }
}
