/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.netbeans.paint.api.components.dialog;

import java.awt.Window;
import java.util.function.BiConsumer;
import javax.swing.JDialog;

/**
 *
 * @author Tim Boudreau
 */
final class PooledDialog extends JDialog {

    BiConsumer<PooledDialog, Runnable> onDispose;
    private final EmptyComponent emptyContents = new EmptyComponent();

    PooledDialog(Window owner) {
        super(owner);
        resetContents();
        super.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    }

    @Override
    public void setDefaultCloseOperation(int operation) {
        // do nothing
    }

    PooledDialog lease(BiConsumer<PooledDialog, Runnable> onDispose) {
        if (this.onDispose != null) {
            throw new IllegalStateException("Still in use");
        }
        this.onDispose = onDispose;
        return this;
    }

    void resetContents() {
        setContentPane(emptyContents);
        getRootPane().setDefaultButton(null);
    }

    @Override
    public void dispose() {
        try {
            if (onDispose != null) {
                System.out.println("Pooled dialog dispose pass to pool");
                onDispose.accept(this, this::superDispose);
                this.onDispose = null;
            } else {
                System.out.println("Pooled dialog dispose really dispose");
                superDispose();
                this.onDispose = null;
            }
        } finally {
            resetContents();
        }
    }

    private void superDispose() {
        super.dispose();
    }

}
