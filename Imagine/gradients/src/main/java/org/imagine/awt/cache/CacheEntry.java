/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.imagine.awt.cache;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import org.imagine.awt.counters.UsageCounter;

/**
 *
 * @author Tim Boudreau
 */
final class CacheEntry<K, P> implements Delayed, Supplier<P> {

    private volatile long expiration;
    private final Supplier<P> realPaintSupplier;
    private final LongSupplier delay;
    private final K key;
    private final UsageCounter counter;

    CacheEntry(Supplier<P> realPaintSupplier, LongSupplier delay, K key, UsageCounter counter) {
        this.delay = delay;
        this.realPaintSupplier = new CachingSupplier<>(realPaintSupplier);
        this.key = key;
        expiration = System.currentTimeMillis() + delay.getAsLong();
        this.counter = counter;
    }

    public UsageCounter counter() {
        return counter;
    }

    public K key() {
        return key;
    }

    @Override
    public P get() {
        touch();
        return realPaintSupplier.get();
    }

    void touch() {
        expiration = System.currentTimeMillis() + delay.getAsLong();
        if (counter != null) {
            counter.touch();
        }
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(expiration - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        long myDelay = getDelay(TimeUnit.MILLISECONDS);
        long otherDelay = getDelay(TimeUnit.MILLISECONDS);
        return Long.compare(myDelay, otherDelay);
    }
}
