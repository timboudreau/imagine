package org.imagine.editor.api.util;

import com.mastfrog.util.collections.IntList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * A generic way of ordering work according to most-recently accessed object;
 * create an ActivationOrder, and pass it to things which should call
 * <code>activated()</code> when they are used; create a sequenceBuilder and add
 * runnables associated with the items, and build a Runnable which will run all
 * the runnables with that. Whenever the Runnable is called, the work will run
 * in the order in which those things or their proxies have most recently called
 * <code>activated()</code>.
 *
 * @author Tim Boudreau
 */
public class ActivationOrder<T> {

    private final IntList list;
    private final ToIntFunction<T> converter;

    public ActivationOrder(int expectedSize, ToIntFunction<T> converter) {
        list = IntList.create(expectedSize);
        this.converter = converter;
    }

    /**
     * Objects whose associated work is sorted by this ActivationOrder should
     * call this method to move their work to the head of the list of work to
     * do.
     *
     * @param o The object
     */
    public void activated(T o) {
        list.toFront(toInt(o));
    }

    /**
     * Objects whose associated work is sorted by this ActivationOrder should
     * call this method to move their work to the tail of the list of work to
     * do, on explicit deactivation, closure or similar.
     *
     * @param o The object
     */
    public void prejudice(T o) {
        list.toBack(toInt(o));
    }

    private int toInt(T obj) {
        return converter.applyAsInt(obj);
    }

    private int indexOf(T obj) {
        return indexOf(toInt(obj));
    }

    private int indexOf(int val) {
        return list.indexOf(val);
    }

    /**
     * Create a builder that can associate work to run with items, and run it in
     * order. The Runnable returned by the builder will strongly reference this
     * ActivationOrder.
     *
     * @return A builder
     */
    public ActivationSequenceBuilder<T> sequenceBuilder() {
        return new ActivationSequenceBuilder<>(this);
    }

    public static final class ActivationSequenceBuilder<T> {

        private final ActivationOrder<T> order;
        private final Map<T, Runnable> workForItem = new HashMap<>();

        private ActivationSequenceBuilder(ActivationOrder<T> order) {
            this.order = order;
        }

        public ActivationSequenceBuilder<T> add(T obj, Runnable work) {
            workForItem.put(obj, work);
            return this;
        }

        /**
         * Create a runnable that will run all of the runnables passed in calls
         * to <code>add(T, Runnable)</code> in the sequence that the objects in
         * question most recently called <code>activate()</code> without a subsequent
         * call to <code>prejudice()</code>.
         *
         * @return A runnable
         */
        public Runnable build() {
            return runner;
        }

        private final Runnable runner = this::run;

        private void run() {
            List<T> all = new ArrayList<>(workForItem.keySet());
            Collections.sort(all, this::compareItems);
            for (T obj : all) {
                Runnable r = workForItem.get(obj);
                r.run();
            }
        }

        private int compareItems(T a, T b) {
            int ia = order.toInt(a);
            int ib = order.toInt(b);
            if (ia == ib) {
                return 0;
            }
            int ixa = order.indexOf(a);
            int ixb = order.indexOf(b);
            if (ixa == ixb) {
                return 0;
            }
            if (ixa == -1) {
                return -1;
            } else if (ixb == -1) {
                return 1;
            }
            return Integer.compare(ixa, ixb);
        }
    }
}
