/*
 * ToolSelectorImpl.java
 *
 * Created on October 15, 2005, 5:42 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.java.dev.imagine.ui.common;

import java.util.Arrays;
import java.util.Collection;
import net.java.dev.imagine.spi.SelectionContextContributor;
import org.openide.ErrorManager;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.ProxyLookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Timothy Boudreau
 */
@ServiceProvider(service=SelectionContextContributor.class)
public final class UIContextLookupProvider implements SelectionContextContributor {
    private final InstanceContent content = new InstanceContent();
    private final AbstractLookup custom = new AbstractLookup (content);
    private final PL lkp = new PL (new Lookup[] { custom });
    
    private static UIContextLookupProvider INSTANCE = null;
    
    @SuppressWarnings("LeakingThisInConstructor")
    public UIContextLookupProvider() {
	if (INSTANCE != null) {
	    throw new IllegalStateException ("Tried to create " +
		    "ToolSelectorImpl twice");
	}
	INSTANCE = this;
    }
        
    public Lookup getLookup() {
        return lkp;
    }
    
    public static Lookup theLookup() {
        ensureCreated();
        return INSTANCE.lkp;
    }
    
    public static void set (Object[] o) {
	ensureCreated();
        INSTANCE.content.set(Arrays.asList(o), null);
    }
    
    public static void set (Collection c) {
        ensureCreated();
        INSTANCE.content.set(c, null);
    }
    
    public static <T> T lookup (Class<T> clazz) {
	ensureCreated();
	return INSTANCE.lkp.lookup(clazz);
    }
    
    public static <T> Lookup.Result<T> lookupResult (Class<T> type) {
	ensureCreated();
	return INSTANCE.lkp.lookupResult (type);
    }
    
    static void setLayer(Lookup layer) {
        INSTANCE.lkp.setOtherLookup(layer == null ?
                null : layer);
    }
    
    private static void ensureCreated() {
	if (INSTANCE == null) {
	    Lookup.getDefault().lookup(UIContextLookupProvider.class);
	    if (INSTANCE == null) {
		ErrorManager.getDefault().notify (new IllegalStateException(
			"Implementation of " +
			"UIContextLookupProvider not found in default lookup." +
			" Check the META-INF/services directory of ToolsUI," +
			" and make sure it provides a Lookup.Provider.  Probably" +
			" tool selection is broken for the application."));
		INSTANCE = new UIContextLookupProvider();
	    }
	}
    }
    
    static class PL extends ProxyLookup {

        public PL(Lookup... lookups) {
            super(lookups);
        }
        
        void setOtherLookup(Lookup lkp) {
            if (lkp == null) {
                setLookups(INSTANCE.custom);
            } else {
                setLookups(INSTANCE.custom, lkp);
            }
        }
        
    }
}
