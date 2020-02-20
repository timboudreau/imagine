package org.imagine.vector.editor.ui.tools;

import org.openide.util.Lookup;
import org.openide.util.lookup.ProxyLookup;

/**
 *
 * @author Tim Boudreau
 */
class MutableProxyLookup extends ProxyLookup {

    public MutableProxyLookup() {
    }

    public void lookups(Lookup... lkps) {
        setLookups(lkps);
    }

}
