package net.java.dev.imagine.api.properties;

import javax.swing.event.ChangeListener;

/**
 *
 * @author Tim Boudreau
 */
public interface Listenable {
    void addChangeListener(ChangeListener cl);
    void removeChangeListener(ChangeListener cl);
}
