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

/**
 *
 * @author Tim Boudreau
 */
public abstract class HelpComponentManagerTrampoline {

    public static HelpComponentManagerTrampoline INSTANCE;

    /**
     * Allows tests, whose ServiceProvider annotations won't work, to supply
     * their own set of help indices without having to use MockServices.
     */
    public static Supplier<Collection<? extends HelpIndex>> INDEXES
            = () -> Lookup.getDefault().lookupAll(HelpIndex.class);

    static {
        try {
            Class<?> type = Class.forName(HelpComponentManager.class.getName());
        } catch (ClassNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }
        if (INSTANCE == null) {
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
