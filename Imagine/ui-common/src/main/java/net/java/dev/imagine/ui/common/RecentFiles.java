/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.ui.common;

import com.mastfrog.function.throwing.io.IOBiConsumer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.openide.awt.Mnemonics;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;

/**
 *
 * @author Tim Boudreau
 */
public class RecentFiles {

    private static final int LIMIT = 12;
    private final Map<Category, List<Path>> cache = new HashMap<>();

    private static RecentFiles INSTANCE;

    public static RecentFiles getDefault() {
        if (INSTANCE == null) {
            INSTANCE = new RecentFiles();
        }
        return INSTANCE;
    }

    public static String name() {
        return NbBundle.getMessage(RecentFiles.class, "RECENT_FILES");
    }

    private List<Path> listFor(Category cat) {
        List<Path> result = cache.get(cat);
        if (result == null) {
            result = new ArrayList<>(LIMIT);
            load(cat, result);
            cache.put(cat, result);
        }
        return result;
    }

    public RecentFiles add(Category category, Path path) {
        List<Path> list = listFor(category);
        if (addOrMoveToFront(path, list)) {
            save(category, list);
        }
        return this;
    }

    private boolean addOrMoveToFront(Path path, List<Path> list) {
        int ix = list.indexOf(path);
        if (ix < 0) {
            list.add(0, path);
            prune(list);
            return true;
        } else if (ix == 0) {
            return false;
        } else {
            list.remove(ix);
            list.add(0, path);
            return true;
        }
    }

    private void prune(List<Path> list) {
        while (list.size() > LIMIT) { // in case the limit across revs, use a while loop
            list.remove(list.size() - 1);
        }
    }

    private Preferences prefs() {
        return NbPreferences.forModule(RecentFiles.class);
    }

    private String catName(Category cat, String postfix) {
        return cat.name().toLowerCase() + "-" + postfix;
    }

    private String catName(Category cat, String postfix, int index) {
        return cat.name().toLowerCase() + "-" + postfix + "-" + index;
    }

    private void save(Category cat, List<Path> result) {
        int count = result.size();
        Preferences prefs = prefs();
        prefs.putInt(catName(cat, "count"), count);
        for (int i = 0; i < count; i++) {
            Path p = result.get(i);
            prefs.put(catName(cat, "file", i), p.toString());
        }
    }

    private void load(Category cat, List<Path> result) {
        Preferences prefs = prefs();
        int count = prefs.getInt(catName(cat, "count"), 0);
        if (count > 0) {
            for (int i = 0; i < count; i++) {
                String key = catName(cat, "file", i);
                String pathName = prefs.get(key, null);
                if (pathName != null) {
                    Path path = Paths.get(pathName);
                    if (Files.exists(path)) {
                        result.add(path);
                    }
                }
            }
        }
    }

    public JMenuItem createMenu(IOBiConsumer<Category, Path> opener) {
        JMenu result = new JMenu();
        Mnemonics.setLocalizedText(result, NbBundle.getMessage(RecentFiles.class, "RECENT_FILES"));
        for (Category cat : Category.values()) {
            JMenu sub = createSubmenu(cat, opener);
            result.add(sub);
        }
        return result;
    }

    public JMenu createSubmenu(Category cat, IOBiConsumer<Category, Path> opener) {
        JMenu sub = new JMenu(cat.toString());
        List<Path> items = listFor(cat);
        if (items.isEmpty()) {
            sub.setEnabled(false);
        } else {
            for (Path p : items) {
                JMenuItem oneItem = new JMenuItem(p.getFileName().toString());
                oneItem.setToolTipText(p.getParent().toString());
                oneItem.addActionListener(ae -> {
                    try {
                        opener.accept(cat, p);
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                });
                sub.add(oneItem);
            }
        }
        return sub;
    }

    public enum Category {
        IMAGINE_NATIVE,
        IMAGE,
        SVG;

        public String toString() {
            return NbBundle.getMessage(Category.class, name());
        }
    }
}
