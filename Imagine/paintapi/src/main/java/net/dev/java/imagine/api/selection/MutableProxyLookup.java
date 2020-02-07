package net.dev.java.imagine.api.selection;

import org.openide.util.Lookup;
import org.openide.util.lookup.ProxyLookup;

/**
 *
 * @author Tim Boudreau
 */
final class MutableProxyLookup extends ProxyLookup {

    void changeLookups(Lookup[] lkp) {
        super.setLookups(lkp);
    }

}
