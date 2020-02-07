/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.spi.image.support;

import java.util.concurrent.Callable;
import java.util.function.Supplier;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.UndoManager;
import org.netbeans.paint.api.util.UndoTransaction;
import org.openide.util.Exceptions;
import org.openide.util.Utilities;

/**
 *
 * @author Tim Boudreau
 */
class UndoSupport {

    private int undoEntryCount;
    private LayersUndoableOperation currentOp = null;
    private boolean pendingChange;
    private final Runnable onChange;
    private final Supplier<LayersState> stateSupplier;
    private boolean enabled;

    UndoSupport(Runnable onChange, Supplier<LayersState> stateSupplier) {
        this.onChange = onChange;
        this.stateSupplier = stateSupplier;
    }

    void enable() {
        enabled = true;
    }

    void disable() {
        enabled = false;
    }

    void fire() {
        if (!enabled) {
            return;
        }
        // We suspend firing until the end of an undoable operation
        if (undoEntryCount <= 0) {
            try {
                onChange.run();
            } finally {
                pendingChange = false;
            }
        } else {
            pendingChange = true;
        }
    }

    boolean inUndoableOperation(boolean needDeepCopy, String operationName, Runnable run) {
        if (!enabled) {
            run.run();
            return true;
        }
        beginUndoableOperation(needDeepCopy, operationName);
        boolean ok = true;
        try {
            run.run();
        } catch (Exception ex) {
            cancelUndoableOperation();
            ok = false;
            Exceptions.printStackTrace(ex);
        } finally {
            if (ok) {
                endUndoableOperation();
            }
        }
        return ok;
    }

    <T> T inThrowingUndoableOperation(boolean needDeepCopy, String operationName, Callable<T> run) throws Exception {
        if (enabled) {
            return run.call();
        }
        beginUndoableOperation(needDeepCopy, operationName);
        boolean ok = true;
        try {
            return run.call();
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            cancelUndoableOperation();
            ok = false;
        } finally {
            if (ok) {
                endUndoableOperation();
            }
        }
        return null;
    }

    UndoTransaction transaction(String opName, boolean needDeepCopy) {
        return new UndoTransactionImpl(this, opName, () -> {
            undoEntryCount = Math.max(0, undoEntryCount - 1);
        });
    }

    void beginUndoableOperation(boolean needDeepCopy, String what) {
        UndoManager undo = (UndoManager) Utilities.actionsGlobalContext().lookup(UndoManager.class);

        if (undo != null) {
            undoEntryCount++;
            if (currentOp == null) {
                currentOp = new LayersUndoableOperation(stateSupplier.get(), needDeepCopy);
            } else if (needDeepCopy != currentOp.isDeepCopy()) {
                currentOp.becomeDeepCopy();
            }
            currentOp.opName = what;
        }
    }

    void cancelUndoableOperation() {
        if (!enabled) {
            return;
        }
        if (currentOp != null) {
            currentOp.undo();
        }
        currentOp = null;
        undoEntryCount = 0;
        pendingChange = false;
    }

    void endUndoableOperation() {
        if (!enabled) {
            return;
        }
        undoEntryCount--;
        if (undoEntryCount == 0) {
            UndoManager undo = (UndoManager) Utilities.actionsGlobalContext().lookup(UndoManager.class);

            if (undo != null) {
                assert undo != null;
                assert currentOp != null;
                undo.undoableEditHappened(new UndoableEditEvent(this, currentOp));
                // Thread.dumpStack();
                currentOp = null;
            }
            if (pendingChange) {
                fire();
            }
        } else if (undoEntryCount < 0) { // exception thrown
            undoEntryCount = 0;
        }
    }
}
