/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.netbeans.paint.misc.image;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Tim Boudreau
 */
class DisposerImpl implements Runnable {

    private final AtomicInteger refCt;
    private final AtomicBoolean disposed;
    private final Runnable realDisposer;

    public DisposerImpl(AtomicInteger refCt, AtomicBoolean disposed, Runnable realDisposer) {
        refCt.incrementAndGet();
        this.refCt = refCt;
        this.disposed = disposed;
        this.realDisposer = realDisposer;
    }

    @Override
    public void run() {
        if (refCt.decrementAndGet() == 0) {
            if (disposed.compareAndSet(false, true)) {
                realDisposer.run();
            }
        }
    }

}
