/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.undo;

/**
 * Passed to consumers when running an undoable transaction, to allow them to
 * cancel adding an undo event if nothing was changed and that could not be
 * determined a-priori.
 *
 * @author Tim Boudreau
 */
public interface Abortable {

    /**
     * To be called if no undoable edit should be generated for the current call
     * sequence.
     */
    void abort();

    default boolean isAborted() {
        return false;
    }
}
