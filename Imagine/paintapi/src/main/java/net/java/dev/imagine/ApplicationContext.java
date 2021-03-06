/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2005 Sun
 * Microsystems, Inc. All Rights Reserved.
 */
package net.java.dev.imagine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.imageio.ImageIO;
import net.java.dev.imagine.spi.*;
import org.imagine.editor.api.ContextLog;
import org.openide.util.ContextGlobalProvider;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.lookup.ProxyLookup;
import org.openide.util.lookup.ServiceProvider;

/**
 * This class serves to give us a persistent selection context; NetBeans'
 * default context sensitivity is very aggressive - selection depends completely
 * on what component has focus.
 * <p>
 * Our application is one where components <i>combine</i> context - the selected
 * tool along with the current image editor, etc. So this class gives us a
 * lookup which merges the default, component-sensitive lookup with other, more
 * persistent lookups, to form a unified "selection context" for the
 * application.
 * <p>
 * So what really will be in this lookup? Well, it proxies
 * Utilities.actionsGlobalContext, so anything a TopComponent (top level
 * component in a tab in the main window) will be there when that component is
 * focused.
 * <p>
 * In addition to this, several pieces of infrastructure will merge their own
 * Lookups into this one - for example, when the selected tool changes, because
 * the user selected a menu item or pressed a button provided by the ToolsUI
 * module, whatever Tool the user selected will appear, and the previously
 * selected Tool, if any, will disappear. So any UI that is interested in what
 * tool is active needs to simply listen for changes in the presence or absence
 * of Tool.class:
 * <pre>
 * Lookup.Result result = ApplicationContext.get (new Lookup.Template(Tool.class));
 * result.addLookupListener (...)
 * </pre> and it will be notified whenever the active tool changes.
 * <p>
 * What generally can you expect to find here?
 * <ul>
 * <li>Picture.class - the layers object of the active editor, if any </li>
 * <li>Layer.class - the active layer of the active editor, if any </li>
 * <li>IO.class - the object for saving/reloading the image in the active
 * editor</li>
 * <li>Zoom.class - the object for adjusting the zoom of the active editor</li>
 * <li>UndoRedo.Manager - a NetBeans subclass of UndoManager which supports
 * listening for undoable edits being added/removed</li>
 * <li>PaintTopComponent.class - this class is not visible outside the PaintUI
 * module, but some actions it itself provides use it directly
 * </ul>
 * <p>
 * <h2>How to merger other Lookups into the Application Context</h2>
 * The ApplicationContext is composed of a set of lookups, including
 * Utilities.actionsGlobalContext(). To merge your own lookup into this,
 * implement Lookup.Provider and put an instance of your Lookup.Provider in the
 * default Lookup by adding a file to META-INF/services in your module jar.
 *
 * @author Timothy Boudreau
 */
@ServiceProvider(service = ContextGlobalProvider.class, supersedes = "org.netbeans.modules.openide.windows.GlobalActionContextImpl", position = Integer.MIN_VALUE)
public final class ApplicationContext implements ContextGlobalProvider {

    private final MutableProxyLookup proxy = new MutableProxyLookup();
    private final Lookup.Result<SelectionContextContributor> lookupsLookup;
    private final LookupListener listenerStrongReference;

    /**
     * Creates a new instance of ApplicationContext
     */
    @SuppressWarnings("unchecked")
    public ApplicationContext() {
        lookupsLookup = Lookup.getDefault()
                .lookupResult(SelectionContextContributor.class);

        // Note the listener below will only really be called if
        // a module providing a lookup we are proxying is
        // installed/uninstalled
        // Need to hold a reference to the listener or it will be garbage
        // collected and never called; as of JDK 10 this applies to method
        // references
        lookupsLookup.addLookupListener(
                listenerStrongReference = this::updateLookups);
        updateLookups(lookupsLookup.allInstances());
    }

    private void updateLookups(LookupEvent evt) {
        updateLookups(lookupsLookup.allInstances());
    }

    private static final ContextLog CLOG = ContextLog.get("selection");

    private void updateLookups(Collection<? extends SelectionContextContributor> allContributors) {
        List<Lookup> all = new ArrayList<>(allContributors.size());
        CLOG.log("ApplicationContext.updateLookups with " + allContributors.size()
                + " lookups: " + allContributors);
        for (SelectionContextContributor c : allContributors) {
            all.add(c.getLookup());
        }
        proxy.changeLookups(all.toArray(new Lookup[all.size()]));
    }

    @Override
    public Lookup createGlobalContext() {
        CLOG.log("ApplicationContext: Create global context");
        return proxy;
    }

    private static final class MutableProxyLookup extends ProxyLookup {

        void changeLookups(Lookup[] lkp) {
            super.setLookups(lkp);
        }
    }

    static {
        //ImageIO init - needs to be in a class loaded early
        ImageIO.setUseCache(false);
        ImageIO.scanForPlugins();
    }

}
