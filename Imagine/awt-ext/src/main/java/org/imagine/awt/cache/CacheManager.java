/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.awt.cache;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import org.imagine.awt.counters.UsageCounter;

/**
 *
 * @author Tim Boudreau
 */
public abstract class CacheManager<B, P extends B, K> {

    private boolean allStackTraces = false;
    private final Set<K> enabledStackTraces = ConcurrentHashMap.newKeySet(10);
    private boolean allReferenceTracking = false;
    private boolean isEnabled = true;
    private final InstanceCache<B, P, K> cache;
    private final UsageCounter cumulativeUsage = new UsageCounter();

    protected CacheManager(ExecutorService svc, Function<? super K, P> converter) {
        cache = new InstanceCache<>(
                this::factoryFor,
                converter,
                this::getExpirationDelayAfterUsage,
                this::keyForOriginal,
                svc,
                this::newUsageCounter,
                this::wrapWithReferenceChecker,
                this::isReferenceCheckingEnabled,
                this::cacheEntryExpired);

    }

    public final B get(P orig) {
        return isEnabled ? cache.get(orig) : orig;
    }

    public final B forKey(K key) {
        if (isEnabled) {
            return cache.forKey(key);
        }
        return null;
    }

    public CacheManager enable() {
        isEnabled = true;
        return this;
    }

    public CacheManager disable() {
        isEnabled = true;
        return this;
    }

    public CacheManager setEnabled(boolean val) {
        isEnabled = val;
        return this;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    protected long getExpirationDelayAfterUsage() {
        return 20000;
    }

    protected abstract Supplier<B> wrapWithReferenceChecker(Supplier<B> origSupplier, UsageCounter counter);

    protected abstract K keyForOriginal(P orig);

    protected abstract Function<K, B> factoryFor(P orig);

    private void cacheEntryExpired(K key, UsageCounter counter) {
        if (counter != null) {
            counter.coalesceInto(cumulativeUsage);
        }
        onExpire(key, counter);
    }

    protected void onExpire(K key, UsageCounter counter) {

    }

    public UsageCounter cumulativeUsage() {
        return cumulativeUsage;
    }

    final UsageCounter newUsageCounter(K key) {
        return new UsageCounter(stackTraceChecker(key));
    }

    private BooleanSupplier stackTraceChecker(K key) {
        return () -> {
            return isStackTraceEnabled(key);
        };
    }

    private boolean isStackTraceEnabled(K key) {
        return allStackTraces || enabledStackTraces.contains(key);
    }

    public boolean isReferenceCheckingEnabled(K key) {
        return allReferenceTracking;
    }

    public boolean isStackTracesEnabled() {
        return allStackTraces;
    }

    public boolean isReferenceTrackingEnabled() {
        return allReferenceTracking;
    }

    public final CacheManager<B, P, K> enableStackTraces() {
        allStackTraces = true;
        return this;
    }

    public final CacheManager<B, P, K> disableStackTraces() {
        allStackTraces = false;
        enabledStackTraces.clear();
        return this;
    }

    public final CacheManager<B, P, K> enableReferenceTracking() {
        allReferenceTracking = true;
        return this;
    }

    public final CacheManager<B, P, K> disableReferenceTracking() {
        allReferenceTracking = true;
        return this;
    }

    public final CacheManager<B, P, K> enableStackTraces(K key) {
        enabledStackTraces.add(key);
        return this;
    }

    public final CacheManager<B, P, K> disableStackTraces(K key) {
        enabledStackTraces.remove(key);
        return this;
    }
}
