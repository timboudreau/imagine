/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.utils;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.openide.util.Exceptions;

/**
 *
 * @author Tim Boudreau
 */
public final class TimedExpirationMap<T, R> implements Map<T, R> {

    private final Map<T, R> delegate;

    public TimedExpirationMap(int capacity) {
        delegate = new ConcurrentHashMap<>(capacity);
    }

    public TimedExpirationMap() {
        delegate = new ConcurrentHashMap<>();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public R get(Object key) {
        return delegate.get(key);
    }

    @Override
    public R remove(Object key) {
        return delegate.remove(key);
    }

    @Override
    public void putAll(Map<? extends T, ? extends R> m) {
        delegate.putAll(m);
        for (T key : m.keySet()) {
            touch(key);
        }
    }

    @Override
    public void clear() {
        int oldSize = delegate.size();
        delegate.clear();
        if (oldSize > 0) {
            cleaner.dequeueAll(this);
        }
    }

    @Override
    public Set<T> keySet() {
        return delegate.keySet();
    }

    @Override
    public Collection<R> values() {
        return delegate.values();
    }

    @Override
    public Set<Entry<T, R>> entrySet() {
        return delegate.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public R getOrDefault(Object key, R defaultValue) {
        return delegate.getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super T, ? super R> action) {
        delegate.forEach(action);
    }

    @Override
    public void replaceAll(BiFunction<? super T, ? super R, ? extends R> function) {
        delegate.replaceAll((t, r) -> {
            touch(t);
            return function.apply(t, r);
        });
    }

    @Override
    public boolean remove(Object key, Object value) {
        return delegate.remove(key, value);
    }

    @Override
    public boolean replace(T key, R oldValue, R newValue) {
        if (delegate.replace(key, oldValue, newValue)) {
            touch(key);
            return true;
        }
        return false;
    }

    @Override
    public R replace(T key, R value) {
        R result = delegate.replace(key, value);
        if (result != null) {
            touch(key);
        }
        return result;
    }

    @Override
    public R computeIfAbsent(T key, Function<? super T, ? extends R> mappingFunction) {
        return delegate.computeIfAbsent(key, (t) -> {
            R result = mappingFunction.apply(key);
            touch(key);
            return result;
        });
    }

    @Override
    public R computeIfPresent(T key, BiFunction<? super T, ? super R, ? extends R> remappingFunction) {
        touch(key);
        return delegate.computeIfPresent(key, remappingFunction);
    }

    @Override
    public R compute(T key, BiFunction<? super T, ? super R, ? extends R> remappingFunction) {
        touch(key);
        return delegate.compute(key, remappingFunction);
    }

    @Override
    public R merge(T key, R value, BiFunction<? super R, ? super R, ? extends R> remappingFunction) {
        touch(key);
        return delegate.merge(key, value, remappingFunction);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public R put(T key, R value) {
        R result = delegate.put(key, value);
        touch(key);
        return result;
    }

    @Override
    public R putIfAbsent(T key, R value) {
        R previousValue = delegate.putIfAbsent(key, value);
        if (previousValue == null) {
            touch(key);
        }
        return previousValue;
    }

    private void touch(T key) {
        cleaner.touch(this, key);
    }

    private static final long DELAY = 3 * 60000;
    private static final int EXPIRE_SUCCESS = 1;
    private static final int MAP_GONE = 2;
    private static final int NOT_EXPIRED = 3;

    private static class QueueEntry<T> implements Delayed {

        private final Reference<TimedExpirationMap<T, ?>> mapRef;
        private final T key;
        private volatile long expiration;
        private volatile boolean defunct;
        private final int mapIdHash;

        QueueEntry(TimedExpirationMap<T, ?> map, T key) {
            mapRef = new WeakReference<>(map);
            mapIdHash = System.identityHashCode(map);
            this.key = key;
            expiration = System.currentTimeMillis() + DELAY;
        }

        boolean is(int mapId, Object key) {
            return mapIdHash == mapId && Objects.equals(key, this.key);
        }

        void touch() {
            expiration = System.currentTimeMillis() + DELAY;
        }

        boolean isExpired() {
            return expiration <= System.currentTimeMillis();
        }

        public String toString() {
            return key + " in " + Long.toHexString(mapIdHash);
        }

        int expire() {
            System.out.println("EXPIRE " + this);
            TimedExpirationMap<T, ?> map = mapRef.get();
            if (map == null) {
                defunct = true;
                System.out.println("MAP GONE: " + Long.toHexString(mapIdHash));
                return MAP_GONE;
            }
            if (isExpired()) {
                map.remove(key);
                System.out.println("REMOVE " + key + " from " + map);
                return EXPIRE_SUCCESS;
            }
            return NOT_EXPIRED;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(expiration - System.currentTimeMillis(),
                    TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            long delay = getDelay(TimeUnit.MILLISECONDS);
            long odelay = getDelay(TimeUnit.MILLISECONDS);
            return Long.compare(delay, odelay);
        }
    }

    private static final TimedMapCleaner cleaner = new TimedMapCleaner();

    private static final class TimedMapCleaner implements Runnable, Thread.UncaughtExceptionHandler {

        private final DelayQueue<QueueEntry<?>> q = new DelayQueue<>();
        private final Map<Integer, Set<QueueEntry<?>>> entryForMapIdHash
                = new ConcurrentHashMap<>();

        TimedMapCleaner() {
            Thread t = new Thread(this);
            t.setPriority(Thread.MIN_PRIORITY + 1);
            t.setName("Timed map cleaner");
            t.setDaemon(true);
            t.setUncaughtExceptionHandler(this);
            t.start();
        }

        void dequeueAll(TimedExpirationMap<?, ?> map) {
            int idHash = System.identityHashCode(map);
            Set<QueueEntry<?>> qes = entryForMapIdHash.get(idHash);
            if (qes != null) {
                q.removeAll(qes);
                qes.clear();
            }
        }

        <T> void touch(TimedExpirationMap<T, ?> map, T key) {
            int id = System.identityHashCode(map);
            Set<QueueEntry<?>> set = entryForMapIdHash.get(id);
            boolean created = set == null;
            if (created) {
                set = ConcurrentHashMap.newKeySet();
                entryForMapIdHash.put(id, set);
            }
            if (!created) {
                for (QueueEntry<?> qe : set) {
                    if (qe.is(id, key)) {
                        qe.touch();
                        return;
                    }
                }
            }
            QueueEntry<T> qe = new QueueEntry<>(map, key);
            set.add(qe);
            q.offer(qe);
        }

        @Override
        public void run() {
            System.out.println("Start expiration thread");
            for (;;) {
                try {
                    QueueEntry<?> qe = q.take();
                    System.out.println("  took " + qe);
                    int expireResult = qe.expire();
                    switch (expireResult) {
                        case NOT_EXPIRED:
                            q.offer(qe);
                            break;
                        case EXPIRE_SUCCESS:
                            Set<QueueEntry<?>> s = entryForMapIdHash.get(qe.mapIdHash);
                            if (s != null) {
                                s.remove(qe);
                                if (s.isEmpty()) {
                                    entryForMapIdHash.remove(qe.mapIdHash);
                                }
                            }
                            break;
                        case MAP_GONE:
                            Set<QueueEntry<?>> s2 = entryForMapIdHash.get(qe.mapIdHash);
                            if (s2 != null) {
                                q.removeAll(s2);
                            }
                            break;
                    }
                } catch (InterruptedException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            Exceptions.printStackTrace(e);
        }
    }
/*
    public static void main(String[] args) throws InterruptedException {
        TimedExpirationMap<String, String> a = new TimedExpirationMap<>();
        TimedExpirationMap<String, String> b = new TimedExpirationMap<>();
        TimedExpirationMap<String, String> c = new TimedExpirationMap<>();

        System.out.println("A: " + Long.toHexString(System.identityHashCode(a)));
        System.out.println("B: " + Long.toHexString(System.identityHashCode(b)));
        System.out.println("C: " + Long.toHexString(System.identityHashCode(c)));

        System.out.println("Add first");

        a.put("hello", "world");
        a.put("goodbye", "thingy");
        a.put("hello", "another world");
        b.put("hey", "you");

        Thread.sleep(500);
        a.put("hello", "third world");

        b.put("this", "thing");
        b.put("is", "weird");

        c.put("1", "one");
        c.put("2", "two");
        c.put("3", "three");
        c.put("4", "four");
        c.put("1", "not really one");

        System.out.println("done adding");

        b = null;
        for (int i = 0; i < 5; i++) {
            System.gc();
            System.runFinalization();
        }
        Thread.sleep(60100 * 3);
    }
*/
}
