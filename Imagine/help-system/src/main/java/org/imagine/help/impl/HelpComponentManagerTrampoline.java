/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.help.impl;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.function.Supplier;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import org.imagine.help.api.HelpItem;
import org.imagine.help.api.search.HelpIndex;
import org.imagine.help.spi.HelpComponentManager;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;

/**
 * Separates API from SPI using the
 * s<a href="http://java.cz/dwn/1003/5802_apidesign-jug-praha-2007.pdf">trampoline
 * patttern</a>.
 *
 * @author Tim Boudreau
 */
public abstract class HelpComponentManagerTrampoline {

    private static HelpComponentManagerTrampoline INSTANCE;
    private static IndexTrampoline INDEX_TRAMPOLINE;

    public interface IndexTrampoline {

        public abstract HelpItem resolve(String qId);
    }
    /**
     * Allows tests to supply their own set of help indices without having to
     * use MockServices.
     */
    private static Supplier<Collection<? extends HelpIndex>> INDICES
            = new IndexSupplier();

    /*() -> {

                ClassLoader ldr = Lookup.getDefault().lookup(ClassLoader.class);
                System.out.println("Help lookup using " + ldr);
                Collection<? extends HelpIndex> result = Lookups.metaInfServices(ldr).lookupAll(HelpIndex.class);
                System.out.println("Found " + result.size() + " help indices: " + result);
                return result;
                return Lookup.getDefault().lookupAll(HelpIndex.class);
            };
     */
//    public static Supplier<Collection<? extends HelpIndex>> INDEXES
//            = () -> Lookup.getDefault().lookupAll(HelpIndex.class);
    static class IndexSupplier implements Supplier<Collection<? extends HelpIndex>>, LookupListener {

        private final Lookup.Result<HelpIndex> result = Lookup.getDefault().lookupResult(HelpIndex.class);
        private volatile Collection<? extends HelpIndex> allIndices;

        IndexSupplier() {
            result.addLookupListener(this);
        }

        @Override
        public Collection<? extends HelpIndex> get() {
            Collection<? extends HelpIndex> allIndices = this.allIndices;
            if (allIndices == null) {
                synchronized (this) {
                    allIndices = this.allIndices;
                    if (allIndices == null) {
                        this.allIndices = allIndices = result.allInstances();
                    }
                }
            }
            return allIndices;
        }

        @Override
        public void resultChanged(LookupEvent le) {
            synchronized (this) {
                allIndices = null;
            }
        }
    }

    /**
     * Get the default instance, initialized in a static block on
     * HelpComponentManager.
     *
     * @return the INSTANCE
     */
    public static HelpComponentManagerTrampoline getInstance() {
        if (INSTANCE == null) {
            try {
                // Ensure the trampoline instance is initialized
                Class.forName(HelpComponentManager.class.getName(), true, HelpComponentManager.class.getClassLoader());
            } catch (ClassNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            }
            if (INSTANCE == null) {
                throw new Error("HelpComponentManagerTrampoline default instance not initialized");
            }
        }
        return INSTANCE;
    }

    /**
     * Set the default instance.
     *
     * @param instance the INSTANCE to set
     */
    public static void setInstance(HelpComponentManagerTrampoline instance) {
        if (INSTANCE != null) {
            throw new Error("setInstance called with " + instance + " when we already have " + INSTANCE);
        }
        INSTANCE = instance;
    }

    /**
     * @return the INDEX_TRAMPOLINE
     */
    public static IndexTrampoline getIndexTrampoline() {
        if (INDEX_TRAMPOLINE == null) {
            try {
                // Ensure the trampoline instance is initialized
                Class.forName(HelpIndex.class.getName(), true, HelpIndex.class.getClassLoader());
            } catch (ClassNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            }
            if (INDEX_TRAMPOLINE == null) {
                throw new Error("IndexTrampoline default instance not initialized");
            }
        }
        return INDEX_TRAMPOLINE;
    }

    /**
     * Set the index trampoline that allows HelpId.find() to call into the
     * protected resolve method on HelpIndex.
     *
     * @param aINDEX_TRAMPOLINE the INDEX_TRAMPOLINE to set
     */
    public static void setIndexTrampline(IndexTrampoline trampoline) {
        INDEX_TRAMPOLINE = trampoline;
    }

    /**
     * Get the collection of available help indices.
     *
     * @return the indices
     */
    public static Supplier<Collection<? extends HelpIndex>> getIndices() {
        return INDICES;
    }

    /**
     * For unit tests, allow the set of indices to be overridden without
     * exposing a public API for that outside this module.
     *
     * @param indices the INDEXES to set
     */
    public static void setIndices(Supplier<Collection<? extends HelpIndex>> indices) {
        INDICES = indices;
    }

    public abstract void activate(HelpItem item, MouseEvent evt);

    public abstract void activate(HelpItem item, Component target);

    public abstract void activate(JComponent target);

    public abstract void deactivate();

    protected abstract void dismissGesturePerformed(JRootPane root);

    public abstract void enqueue(HelpItem item, Component target);

    public abstract void open(HelpItem item);

}
