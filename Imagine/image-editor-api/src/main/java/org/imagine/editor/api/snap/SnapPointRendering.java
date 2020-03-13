/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.editor.api.snap;

import com.mastfrog.function.DoubleBiConsumer;
import org.openide.util.NbPreferences;
import org.openide.util.WeakSet;

/**
 *
 * @author Tim Boudreau
 */
public class SnapPointRendering {

    private static final int DEFAULT_SIZE = 9;
    private static final WeakSet<DoubleBiConsumer> listeners
            = new WeakSet<>();

    public static double visualSize() {
        return NbPreferences.forModule(SnapPointRendering.class)
                .getDouble("snapPointSize", DEFAULT_SIZE);
    }

    public static boolean set(double newValue) {
        if (newValue < 1) {
            throw new IllegalArgumentException("" + newValue);
        }
        double old;
        if ((old = visualSize()) != newValue) {
            NbPreferences.forModule(SnapPointRendering.class)
                    .putDouble("snapPointSize", newValue);
            for (DoubleBiConsumer l : listeners) {
                l.accept(old, newValue);
            }
            return true;
        }
        return false;
    }

    public static void listen(DoubleBiConsumer consumer) {
        listeners.add(consumer);
    }

    public static void unlisten(DoubleBiConsumer consumer) {
        listeners.remove(consumer);
    }
}
