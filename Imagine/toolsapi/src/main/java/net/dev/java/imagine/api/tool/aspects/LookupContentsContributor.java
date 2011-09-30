/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.dev.java.imagine.api.tool.aspects;

import java.util.Set;

/**
 * Tool aspect which allows a delegate to contribute objects into the Tool's
 * lookup, without implementing them directly.
 *
 * @author Tim Boudreau
 */
public interface LookupContentsContributor {
    /**
     * Provide some additional objects which should be present in the 
     * Tool's lookup.  
     * @param addTo a Set which objects can be contributed to, and which
     * will appear in Tool.getLookup() which this ToolImplementation is in use.
     * <p/>
     * In particular, if you are implementing WidgetAction or any Swing listener
     * interface, you can implement it in a separate class and simply add
     * it to the passed set.
     */
    public void createLookupContents(Set<? super Object> addTo);
}
