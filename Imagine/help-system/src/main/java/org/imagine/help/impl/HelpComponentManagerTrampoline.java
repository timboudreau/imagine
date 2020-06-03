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
 *
 * @author Tim Boudreau
 */
public abstract class HelpComponentManagerTrampoline {

    public static HelpComponentManagerTrampoline INSTANCE;
    public static IndexTrampoline INDEX_TRAMPOLINE;

    public interface IndexTrampoline {

        public abstract HelpItem resolve(String qId);
    }
    /**
     * Allows tests to supply their own set of help indices without having to
     * use MockServices.
     */
    public static Supplier<Collection<? extends HelpIndex>> INDEXES
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

    static {
        try {
            // Ensure the trampoline instance is initialized
            Class<?> type = Class.forName(HelpComponentManager.class.getName());
        } catch (ClassNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }
        if (INSTANCE == null) {
            throw new Error("HelpComponentManagerTrampoline default instance not initialized");
        }
        try {
            // Ensure the trampoline instance is initialized
            Class<?> type = Class.forName(HelpIndex.class.getName());
        } catch (ClassNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }
        if (INDEX_TRAMPOLINE == null) {
            throw new Error("HelpComponentManagerTrampoline default instance not initialized");
        }

    }

    public abstract void activate(HelpItem item, MouseEvent evt);

    public abstract void activate(HelpItem item, Component target);

    public abstract void activate(JComponent target);

    public abstract void deactivate();

    protected abstract void dismissGesturePerformed(JRootPane root);

    public abstract void enqueue(HelpItem item, Component target);

    public abstract void open(HelpItem item);

}
