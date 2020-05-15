/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.undo;

import java.util.function.Consumer;

/**
 *
 * @author Tim Boudreau
 */
final class AbortableImpl implements Abortable {

    private boolean aborted;
    private Consumer<Abortable> owner;
    static final AbortableImpl SHARED_INSTANCE = new AbortableImpl();

    boolean borrow(Consumer<Abortable> consumer) {
        if (owner != null) {
            return new AbortableImpl().borrow(consumer);
        }
        aborted = false;
        boolean result;
        try {
            owner = consumer;
            consumer.accept(this);
            result = aborted;
        } catch (Exception | Error e) {
            result = true;
            return com.mastfrog.util.preconditions.Exceptions.chuck(e);
        } finally {
            owner = null;
            aborted = false;
        }
        return result;
    }

    public boolean isAborted() {
        return aborted;
    }

    @Override
    public void abort() {
        aborted = true;
    }
}
