package org.imagine.awt.counters;

import com.mastfrog.util.collections.CollectionUtils;
import com.mastfrog.util.collections.LongList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

/**
 * Counts usages of an instance or wrapper for an instance, tracks lifecycle and
 * creation info.
 *
 * @author Tim Boudreau
 */
public final class UsageCounter {

    private final AtomicInteger usages = new AtomicInteger();
    private final AtomicInteger liveInstances = new AtomicInteger();
    private final BooleanSupplier stackTraces;
    private final Ring<Exception> stackTraceRing;
    private long averageAge = 0;
    private long minAge = 0;
    private long maxAge = 0;
    private final LongList ages = CollectionUtils.longList(5);
    private final Exception creation;
    private final long created = System.currentTimeMillis();

    public UsageCounter() {
        this(false);
    }

    public UsageCounter(boolean stackTraces) {
        this(() -> stackTraces);
    }

    public UsageCounter(BooleanSupplier stackTraces) {
        this(stackTraces, 5);
    }

    public UsageCounter(BooleanSupplier stackTraces, int ringSize) {
        this.stackTraces = stackTraces;
        stackTraceRing = new Ring<>(ringSize);
        creation = stackTraces.getAsBoolean() ? new Exception() : null;
    }

    public long age() {
        return System.currentTimeMillis() - created;
    }

    public void reset() {
        stackTraceRing.contents.clear();
        minAge = 0;
        maxAge = 0;
        averageAge = 0;
        ages.clear();
        usages.set(0);
        liveInstances.set(0);
    }

    public void coalesceInto(UsageCounter cumulative) {
        cumulative.usages.addAndGet(usages.get());
        long age = cumulative.averageAge;
        if (age == 0) {
            if (averageAge > 0) {
                cumulative.averageAge = averageAge;
            }
        } else {
            if (averageAge > 0) {
                cumulative.averageAge = (age + averageAge) / 2;
            }
        }
        long max = cumulative.maxAge;
        if (max == 0) {
            cumulative.maxAge = maxAge;
        } else {
            cumulative.maxAge = Math.max(maxAge, max);
        }
        long min = cumulative.minAge;
        if (min == 0) {
            cumulative.minAge = minAge;
        } else {
            cumulative.minAge = Math.min(minAge, min);
        }
    }

    @Override
    public String toString() {
        return "Used " + usages() + " live " + liveInstances.get()
                + " avgAge " + averageAge;
    }

    public List<StackTraceElement> creation() {
        if (creation == null) {
            return Collections.emptyList();
        } else {
            StackTraceElement[] st = creation.getStackTrace();
            List<StackTraceElement> elements = new ArrayList(st.length - 2);
            boolean started = false;
            int javaPackagesSeen = 0;
            for (int i = 0; i < st.length; i++) {
                StackTraceElement el = st[i];
                String name = el.getClassName();
                if (!started && name.startsWith("org.imagine.awt")) {
                    continue;
                } else {
                    started = true;
                }
                elements.add(el);
                if (name.startsWith("java.") || name.startsWith("javax")) {
                    javaPackagesSeen++;
                    if (javaPackagesSeen > 6) {
                        break;
                    }
                }
            }
            return elements;
        }
    }

    public long averageAge() {
        return averageAge;
    }

    public int liveInstances() {
        return liveInstances.get();
    }

    public int usages() {
        return usages.get();
    }

    public List<Exception> recentStackTraces() {
        return stackTraceRing.copy();
    }

    public int touch() {
        int result = usages.incrementAndGet();
        liveInstances.incrementAndGet();
        if (stackTraces.getAsBoolean()) {
            stackTraceRing.put(new Exception());
        }
        return result;
    }

    public int disposed(long age) {
        if (minAge == 0) {
            minAge = age;
        } else {
            minAge = Math.min(age, minAge);
        }
        if (maxAge == 0) {
            maxAge = age;
        } else {
            maxAge = Math.max(age, maxAge);
        }
        int result = liveInstances.decrementAndGet();
        ages.add(age);
        int size = ages.size();
        if (size > 5) {
            long[] arr = ages.toLongArray();
            ages.clear();
            long sum = 0;
            for (int i = 0; i < arr.length; i++) {
                sum += arr[i];
            }
            long avg = sum / size;
            if (averageAge != 0) {
                averageAge = (averageAge + avg) / 2;
            } else {
                averageAge = avg;
            }
        }
        return result;
    }

}
