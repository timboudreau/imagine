/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.netbeans.paint.api.components.dialog;

import java.awt.Window;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author Tim Boudreau
 */
final class DialogPool {

    private static final int DEFAULT_MAX = 3;
    private int max;
    private final Set<PooledDialog> available = new HashSet<>();
    private final Set<PooledDialog> inUse = new HashSet<>();

    DialogPool() {
        this(DEFAULT_MAX);
    }

    DialogPool(int max) {
        assert max > 0;
        this.max = max;
        System.out.println("create a dialog pool");
    }

    public PooledDialog borrow(Window owner) {
        Iterator<PooledDialog> avail = available.iterator();
        PooledDialog result = avail.hasNext() ? avail.next() : null;
        if (result != null && owner == result.getOwner()) {
            avail.remove();
            inUse.add(result);
            System.out.println("reuse existing dialog");
        } else {
            System.out.println("new pooled dialog");
            result = create(owner);
            inUse.add(result);
        }
        result.lease(this::disposed);
        return result;
    }

    void disposed(PooledDialog dlg, Runnable superDispose) {
        inUse.remove(dlg);
        if (available.size() < max) {
            System.out.println("return dialog to pool");
            available.add(dlg);
        } else {
            System.out.println("dialog returned but pool is full");
            superDispose.run();
        }
    }

    private PooledDialog create(Window owner) {
        return new PooledDialog(owner);
    }
}
