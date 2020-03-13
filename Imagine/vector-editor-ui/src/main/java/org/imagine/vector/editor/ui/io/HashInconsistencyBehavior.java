/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.io;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.java.dev.imagine.api.vector.Primitive;

/**
 *
 * @author Tim Boudreau
 */
public enum HashInconsistencyBehavior {

    THROW,
    WARN,
    SILENT;

    public static HashInconsistencyBehavior defaultBehavior() {
        return THROW;
    }

    public void apply(Primitive loaded, int expectedHashCode, int actualHashCode)
            throws IOException {
        if (expectedHashCode != actualHashCode) {
            switch (this) {
                case SILENT:
                    return;
                case WARN:
                    Logger.getLogger(HashInconsistencyBehavior.class.getName()).log(
                            Level.WARNING, "Saved and loaded hash codes inconsitent - expected {0} but got {1} for {2}",
                            new Object[]{expectedHashCode, actualHashCode, loaded});
                    break;
                case THROW:
                    throw new IOException("Saved and loaded hash codes inconsitent - expected "
                            + expectedHashCode + " but got " + actualHashCode + " for "
                            + loaded);
                default:
                    throw new AssertionError(this);
            }
        }
    }
}
