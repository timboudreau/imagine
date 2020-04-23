package net.dev.java.imagine.spi.tool.impl;

import java.awt.Image;
import java.beans.BeanInfo;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
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

    private List<ToolDriver<?, ?>> allImpls(FileObject fo) {
        //XXX why is Lookups.forPath() fine in tests but broken here?
        List<ToolDriver<?, ?>> result = new ArrayList<ToolDriver<?, ?>>();
        for (FileObject ch : fo.getChildren()) {
            Object o = ch.getAttribute("instanceCreate"); //XXX
            if (o instanceof ToolDriver) {
                result.add((ToolDriver<?, ?>) o);
            }
        }
        return result;
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
                List<ToolDriver<?, ?>> drivers = allImpls(fo);
                boolean hasDrivers = !drivers.isEmpty();
//                boolean hasDrivers = !lkp.lookupResult(ToolDriver.class).allItems().isEmpty();
                if (!hasDrivers) {
                    Logger.getLogger(DefaultTools.class.getName()).log(Level.WARNING, "{0} defines a tool, but no ToolDriver instances found in it", fo.getPath() + ": " + lkp.lookup(Object.class));
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
        private boolean logged;

        public TD(DataObject fld, Category category) {
            Parameters.notNull("category", category);
            Parameters.notNull("fld", fld);
            this.fld = fld;
            this.category = category;
        }

        @Override
        public String displayName() {
            try {
                return fld.getNodeDelegate().getDisplayName();
            } catch (MissingResourceException mre) {
                if (!logged) {
                    Exceptions.printStackTrace(mre);
                    logged = true;
                }
                return fld.getName();
            }
        }

        @Override
        public Icon icon() {
            // Do this directly to try to use the right loader for SVG icons
            URL url = (URL) fld.getPrimaryFile().getAttribute("SystemFileSystem.icon");
            if (url != null) {
                String pth = url.getPath();
                if (!pth.isEmpty() && pth.charAt(0) == '/') {
                    pth = pth.substring(1);
                }
                System.out.println("try load icon " + pth);
                Icon result = ImageUtilities.loadImageIcon(url.getPath(), false);
//                if (result != null && result.getIconWidth() > 0 && result.getIconHeight() > 0) {
//                    return result;
//                } else {
//                    System.out.println("Got bad icon: " + result);
//                }
                if (result != null) {
                    return result;
                } else {
                    System.out.println("No icon for " + fld.getName());
                }
            }

            Image ic = fld.getNodeDelegate().getIcon(BeanInfo.ICON_COLOR_16x16);
            try {
                return ImageUtilities.image2Icon(ic);
            } catch (IllegalArgumentException ex) {
                // tests - we get ToolkitImage which isn't handled correctly
                return new ImageIcon(ic);
            }
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
