package net.dev.java.imagine.api.tool.aspects;

import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
public interface Attachable {

    public void attach(Lookup.Provider on);

    public void detach();
}
