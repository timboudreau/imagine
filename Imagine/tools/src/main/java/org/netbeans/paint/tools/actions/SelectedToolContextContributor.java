/*
 * ToolSelectorImpl.java
 *
 * Created on October 15, 2005, 5:42 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.netbeans.paint.tools.actions;

import java.util.Collections;
import net.dev.java.imagine.api.tool.SelectedTool;
import net.dev.java.imagine.api.tool.Tool;
import net.java.dev.imagine.spi.SelectionContextContributor;
import org.openide.ErrorManager;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Timothy Boudreau
 */
@ServiceProvider(service = SelectionContextContributor.class)
public class SelectedToolContextContributor implements SelectionContextContributor {
    
    //XXX this should be in a different module, probably PaintUI - 
    //having the tool available in the global selection should *not* 
    //depend on having a standard surface-tools module installed.
    
    //It also cannot be in Imagine API w/o creating a circular dependency

    private InstanceContent content = new InstanceContent();
    private AbstractLookup lkp = new AbstractLookup(content);
    //XXX get this package non-public!
    private static SelectedToolContextContributor INSTANCE = null;

    @SuppressWarnings("LeakingThisInConstructor")
    public SelectedToolContextContributor() {
        INSTANCE = this;
    }

    public Lookup getLookup() {
        return lkp;
    }

    public static void setSelectedTool(Tool tool) {
        ensureCreated();
        INSTANCE.content.set(tool == null ? Collections.EMPTY_SET
                : Collections.singleton(tool), null);
    }

    private static void ensureCreated() {
        if (true) {
            return;
        }
        if (INSTANCE == null) {
            Object o =
                    Lookup.getDefault().lookup(SelectionContextContributor.class);
            if (INSTANCE == null) {
                ErrorManager.getDefault().notify(new IllegalStateException(
                        "Implementation of "
                        + "SelectedToolLookupProvider not found in default lookup."
                        + " Check the META-INF/services directory of ToolsUI,"
                        + " and make sure it provides a Lookup.Provider.  Probably"
                        + " tool selection is broken for the application."));
                INSTANCE = new SelectedToolContextContributor();
            }
        }
    }

    @ServiceProvider(service = SelectedTool.class)
    public static class SelectedToolImpl extends SelectedTool {

        @Override
        protected boolean onChange(net.dev.java.imagine.api.tool.Tool old, net.dev.java.imagine.api.tool.Tool nue) {
            ensureCreated();
            INSTANCE.content.set(nue == null ? Collections.EMPTY_SET
                    : Collections.singleton(nue), null);
            return true;
        }
    }
}
