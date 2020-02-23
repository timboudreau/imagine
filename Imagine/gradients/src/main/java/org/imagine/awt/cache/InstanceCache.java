/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.awt.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.imagine.awt.counters.UsageCounter;

/**
 * A generic cache for objects of type P which can be wrappered or replaced with
 * objects of supertype B, keyed by key type K, with a LRU eviction policy.
 * So, for example with Paint instances, you look up with a Paint and get back
 * something which may or may not be the same instance.  Also supports tracking
 * and collecting stats on wrapped objects.
 *
 * @author Tim Boudreau
 */
final class InstanceCache<B, P extends B, K> {

    private static long DEFAULT_PAINT_EXPIRE_DELAY = 60000;
    private final Map<K, UsageCounter> counterCache
            = new ConcurrentHashMap<>();
    private final Map<K, CacheEntry<K, B>> cache
            = new ConcurrentHashMap<>();

    private final Function<? super P, Function<? super K, B>> factoryFactory;
    private final LongSupplier delay;
    private final Function<? super P, K> keyFactory;
    private final DelayQueue<CacheEntry<K, B>> cacheExpirer = new DelayQueue<>();
    private final Function<? super K, UsageCounter> usageCounterFactory;
    private final BiFunction<Supplier<B>, UsageCounter, Supplier<B>> referenceCheckingWrapperFactory;
    private final Predicate<K> referenceCheckingEnabled;
    private final BiConsumer<K, UsageCounter> onExpire;
    private final Function<? super K, P> converter;

    public InstanceCache(
            Function<P, Function<? super K, B>> factory,
            Function<? super K, P> converter,
            Function<? super P, K> keyFactory,
            ExecutorService expiryThreadPool,
            Function<K, UsageCounter> usageCounterFactory,
            BiFunction<Supplier<B>, UsageCounter, Supplier<B>> referenceCheckingWrapperFactory,
            Predicate<K> referenceCheckingEnabled,
            BiConsumer<K, UsageCounter> onExpire) {
        this(factory, converter, () -> DEFAULT_PAINT_EXPIRE_DELAY, keyFactory,
                expiryThreadPool, usageCounterFactory,
                referenceCheckingWrapperFactory,
                referenceCheckingEnabled, onExpire);
    }

    public InstanceCache(
            Function<? super P, Function<? super K, B>> factory,
            Function<? super K, P> converter,
            LongSupplier delay,
            Function<? super P, K> keyFactory,
            ExecutorService expiryThreadPool,
            Function<? super K, UsageCounter> usageCounterFactory,
            BiFunction<Supplier<B>, UsageCounter, Supplier<B>> referenceCheckingWrapperFactory,
            Predicate<K> referenceCheckingEnabled,
            BiConsumer<K, UsageCounter> onExpire) {
        this.factoryFactory = factory;
        this.delay = delay;
        this.converter = converter;
        this.keyFactory = keyFactory;
        this.usageCounterFactory = usageCounterFactory;
        this.referenceCheckingWrapperFactory = referenceCheckingWrapperFactory;
        this.referenceCheckingEnabled = referenceCheckingEnabled;
        this.onExpire = onExpire;
        expiryThreadPool.submit(new Expirer());
    }

    public int size() {
        return cache.size();
    }

    public int expireNow(double factor) {
        int sz = size();
        int count = (int) (Math.max(0, Math.min(1, factor)) * sz);
        List<CacheEntry<K, B>> entries = new ArrayList<>(cacheExpirer);
        Collections.sort(entries);
        entries = entries.subList(0, Math.min(entries.size(), count));
        for (CacheEntry<K, B> e : entries) {
            K key = e.key();
            counterCache.remove(key);
            cache.remove(key);
            onExpire(e, e.counter());
        }
        return entries.size();
    }

    private int tick;
    private static int TICK_PERIOD = 120;

    private void periodicCheck(int tick) {
        if (tick++ % TICK_PERIOD == 0) {
            System.out.println("---------------- " + tick + " -----------------");
            int sz = size();
            System.out.println("Cache size:\t" + sz);
            System.out.println("Contents:");
            List<CacheEntry<K, B>> entries = new ArrayList<>(cache.values());
            for (CacheEntry<K, B> e : entries) {
                System.out.println("  " + e.key() + " " + e.counter());
            }
            System.out.println("\n");
        }
    }

    public B forKey(K key) {
        CacheEntry<K, B> entry = cache.get(key);
        if (entry == null) {
            return get(converter.apply(key));
        }
        periodicCheck(tick++);
        return entry.get();

    }

    public B get(P orig) {
        K key = keyFactory.apply(orig);
        CacheEntry<K, B> entry = cache.get(key);
        if (entry == null) {
            Function<? super K, B> factory = factoryFactory.apply(orig);
            Supplier<B> supp = new FunctionToSupplierAdapter(key, factory);
            UsageCounter counter = counter = usageCounterFactory.apply(key);
            counterCache.put(key, counter);
            if (referenceChecking(key)) {
//                counter = usageCounterFactory.apply(key);
//                counterCache.put(key, counter);
                supp = referenceCheckingWrapperFactory.apply(supp, counter);
            }
            entry = new CacheEntry<>(supp, delay, key, counter);
            cacheExpirer.offer(entry);
            cache.put(key, entry);
        }
        periodicCheck(tick++);
        return entry.get();
    }

    boolean referenceChecking(K key) {
        return referenceCheckingEnabled.test(key);
    }

    private void onExpire(CacheEntry<K, B> entry, UsageCounter counter) {
        System.out.println("EXPIRED " + entry.key() + " with " + counter);
        onExpire.accept(entry.key(), counter);
    }

    class Expirer implements Runnable {

        @Override
        public void run() {
            System.out.println("Expire thread started");
            Thread.currentThread().setName("Paint cache expirer");
            for (;;) {
                try {
                    CacheEntry<K, B> entry = cacheExpirer.take();
                    if (entry != null) {
                        if (entry.getDelay(TimeUnit.MILLISECONDS) <= 0) {
                            UsageCounter counter = entry.counter();
                            K key = entry.key();
                            cache.remove(key);
                            counterCache.remove(key);
                            onExpire(entry, counter);
                        } else {
                            System.out.println("Used since take, reenqueue");
                            cacheExpirer.offer(entry);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class FunctionToSupplierAdapter<B, K> implements Supplier<B> {

        private final K key;
        private final Function<K, B> factory;

        public FunctionToSupplierAdapter(K key, Function<K, B> factory) {
            this.key = key;
            this.factory = factory;
        }

        @Override
        public B get() {
            return factory.apply(key);
        }
    }
}
