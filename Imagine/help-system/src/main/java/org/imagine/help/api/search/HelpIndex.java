/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.help.api.search;

import org.imagine.help.impl.HeadingComparator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import org.imagine.help.api.HelpItem;
import org.imagine.help.impl.HelpComponentManagerTrampoline;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
public abstract class HelpIndex implements Comparable<HelpIndex> {

    static {
        HelpComponentManagerTrampoline.setIndexTrampline(HelpIndex::resolve);
    }

    private static Collection<? extends HelpIndex> allIndices() {
        return withGlobalClassloader(() -> HelpComponentManagerTrampoline.getIndices().get());
    }

    private static <T> T withGlobalClassloader(Supplier<T> supp) {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader ldr = Lookup.getDefault().lookup(ClassLoader.class);
            Thread.currentThread().setContextClassLoader(ldr);
            return supp.get();
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    public static SearchControl search(String searchTerm, int maxResults, HelpSearchCallback callback, HelpSearchConstraint... constraints) {
        return search(Locale.getDefault(), searchTerm, maxResults, callback, constraints);
    }

    public static Map<String, List<HelpItem>> allItemsByTopic() {
        return allItemsByTopic(Locale.getDefault());
    }

    /**
     * Resolve a help item by name; if using the default implementation of the
     * help system, this should be the package name of the generated enum, plus
     * the enum constant name of the help item, <i>omitting the generated enum's
     * class name</i>.
     *
     * @param qualifiedName A name such as <code>com.foo.Overview</code>
     * @return A help item
     */
    static HelpItem resolve(String qualifiedName) {
        int ix = qualifiedName.lastIndexOf('.');
        if (ix < 0 || ix == qualifiedName.length() - 1) {
            return null;
        }
        String pkg = qualifiedName.substring(0, ix);
        String item = qualifiedName.substring(ix + 1);
        for (HelpIndex index : allIndices()) {
            HelpItem result = index.resolve(pkg, item);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    protected abstract HelpItem resolve(String pkg, String id);

    public static Map<String, List<HelpItem>> allItemsByTopic(Locale locale) {
        Map<String, List<HelpItem>> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (HelpIndex index : allIndices()) {
            index.itemsByTopic(locale, (topic, item) -> {
                List<HelpItem> items = result.get(topic);
                if (items == null) {
                    items = new ArrayList<>(5);
                    result.put(topic, items);
                }
                items.add(item);
            });
        }
        HeadingComparator comp = new HeadingComparator(locale);
        for (Map.Entry<String, List<HelpItem>> e : result.entrySet()) {
            e.getValue().sort(comp);
        }
        return result;
    }

    public static SearchControl search(Locale locale, String searchTerm, int maxResults, HelpSearchCallback callback, HelpSearchConstraint... constraints) {
        if (maxResults <= 0) {
            throw new IllegalArgumentException("Requested search result count <= 0: " + maxResults);
        }
        if (searchTerm.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty search term");
        }
        Set<HelpSearchConstraint> constraintSet = EnumSet.noneOf(HelpSearchConstraint.class);
        constraintSet.addAll(Arrays.asList(constraints));
        HelpSearcher searcher = new HelpSearcher(locale, searchTerm, allIndices(), callback, constraintSet, maxResults);
        searcher.start();
        return searcher;
    }

    protected abstract Set<? extends HelpItem> allItems();

    protected abstract void runSearch(Locale locale, String searchTerm, Set<HelpSearchConstraint> constraints, int maxResults, HelpSearchCallback callback);

    protected void itemsByTopic(Locale locale, BiConsumer<String, HelpItem> consumer) {
        for (HelpItem item : allItems()) {
            String topic = item.topic(locale);
            consumer.accept(topic, item);
        }
    }

    @Override
    public int compareTo(HelpIndex o) {
        return o == null ? 0 : getClass().getName().compareTo(o.getClass().getName());
    }
}
