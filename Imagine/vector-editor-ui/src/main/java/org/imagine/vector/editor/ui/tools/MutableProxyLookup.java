package org.imagine.vector.editor.ui.tools;

import com.mastfrog.util.collections.ArrayUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openide.util.Lookup;
import org.openide.util.lookup.ProxyLookup;

/**
 *
 * @author Tim Boudreau
 */
public class MutableProxyLookup extends ProxyLookup {

    public MutableProxyLookup() {
    }

    public boolean containsLookup(Lookup lkp) {
        for (Lookup l : getLookups()) {
            if (lkp == l) {
                return true;
            }
        }
        return false;
    }

    public void updateLookups(Lookup... lkps) {
        assert !Arrays.asList(lkps).contains(this) : "Add to self";
        setLookups(lkps);
    }

    public void addLookup(Lookup lkp) {
        Lookup[] old = getLookups();
        if (Arrays.asList(old).contains(lkp)) {
            return;
        }
        Lookup[] nue = ArrayUtils.concatenate(old, new Lookup[]{lkp});
        setLookups(nue);
    }

    public void removeLookup(Lookup lkp) {
        Lookup[] old = getLookups();
        int ix = Arrays.asList(old).indexOf(lkp);
        if (ix < 0) {
            return;
        }
        if (old.length == 1) {
            setLookups();
        } else {
            List<Lookup> nue = new ArrayList<>(Arrays.asList(old));
            nue.add(lkp);
            setLookups(nue.toArray(new Lookup[nue.size()]));
        }
    }
}
