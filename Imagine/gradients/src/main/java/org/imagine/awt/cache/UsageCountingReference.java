/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.awt.cache;

import java.lang.ref.WeakReference;
import org.imagine.awt.counters.UsageCounter;
import org.openide.util.BaseUtilities;

/**
 *
 * @author Tim Boudreau
 */
public class UsageCountingReference<T> extends WeakReference<T> implements Runnable {

    private final UsageCounter counter;
    private final long created = System.currentTimeMillis();
    private long collected = 0;

    UsageCountingReference(T referent, UsageCounter counter) {
        super(referent, BaseUtilities.activeReferenceQueue());
        this.counter = counter;
    }

    public long age() {
        long coll = collected;
        if (coll < created) {
            synchronized (this) {
                coll = collected;
                if (coll < created) {
                    return System.currentTimeMillis() - created;
                }
            }
        }
        return collected - created;
    }

    public synchronized boolean isCollected() {
        return collected < created;
    }

    @Override
    public void run() {
        long age;
        synchronized (this) {
            collected = System.currentTimeMillis();
            age = collected - created;
        }
        counter.disposed(age);
    }
}
