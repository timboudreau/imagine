/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.spi.image.support;

import java.lang.ref.WeakReference;
import org.netbeans.paint.api.util.UndoTransaction;
import org.openide.util.Utilities;

/**
 *
 * @author Tim Boudreau
 */
public final class UndoTransactionImpl implements UndoTransaction {

    private final UndoSupport udo;
    private final UdoRef ref;

    UndoTransactionImpl(UndoSupport udo, String name, Runnable onDiscard) {
        this.udo = udo;
        ref = new UdoRef(this, onDiscard);
    }

    public void commit() {
        if (ref.finished) {
            throw new IllegalStateException("Already finished");
        }
        ref.finished = true;
        udo.endUndoableOperation();
    }

    public void rollback() {
        if (ref.finished) {
            throw new IllegalStateException("Already finished");
        }
        ref.finished = true;
        udo.cancelUndoableOperation();
    }

    /**
     * Ensure a forgotten object (say belonging to a window that was closed
     * while an operation was in progress) is eventually cleaned up.
     */
    static class UdoRef extends WeakReference<UndoTransactionImpl> implements Runnable {

        private Runnable onDiscard;
        boolean finished;

        public UdoRef(UndoTransactionImpl u, Runnable onDiscard) {
            super(u, Utilities.activeReferenceQueue());
        }

        @Override
        public void run() {
            if (!finished) {
                onDiscard.run();
            }
        }
    }
}
