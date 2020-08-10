package org.imagine.inspectors;

import com.mastfrog.function.TriConsumer;
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.imagine.inspectors.spi.InspectorFactory;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;

/**
 *
 * @author Tim Boudreau
 */
final class Inspectors {

    private final Lookup lkp;
    private Set<InspectorsEntry<?>> entries = new HashSet<>();
    private TriConsumer<Lookup, ? super List<? extends InspectorFactory<?>>, ? super List<? extends InspectorFactory<?>>> listener;
    Inspectors() {
        this(Utilities.actionsGlobalContext());
    }

    Inspectors(Lookup lkp) {
        this.lkp = lkp;
    }

    boolean isGlobal() {
        return lkp == Utilities.actionsGlobalContext();
    }

    void listen(TriConsumer<Lookup, ? super List<? extends InspectorFactory<?>>, ? super List<? extends InspectorFactory<?>>> c) {
        if (listener == null && c != null) {
            listener = c;
            for (InspectorFactory<?> f : Lookup.getDefault().lookupAll(InspectorFactory.class)) {
                entries.add(InspectorsEntry.newEntry(f, this::changed, lkp));
            }
        } else if (listener != null && c == null) {
            listener = null;
            entries.clear();
        }
    }

    private void changed(InspectorsEntry<?> e, boolean active) {
        if (active) {
            changeNotify.added(e.factory);
        } else {
            changeNotify.removed(e.factory);
        }
    }

    private final InspectorsChange changeNotify = new InspectorsChange();

    class InspectorsChange implements Runnable {

        Set<InspectorFactory<?>> added = new LinkedHashSet<>();
        Set<InspectorFactory<?>> removed = new LinkedHashSet<>();
        private final AtomicBoolean enqueued = new AtomicBoolean();

        private void ensureEnqueued() {
            if (enqueued.compareAndSet(false, true)) {
                EventQueue.invokeLater(this);
            }
        }

        synchronized void added(InspectorFactory<?> f) {
            removed.remove(f);
            added.add(f);
            ensureEnqueued();
        }

        synchronized void removed(InspectorFactory<?> f) {
            added.remove(f);
            removed.add(f);
            ensureEnqueued();
        }

        @Override
        public void run() {
            List<InspectorFactory<?>> addedLocal, removedLocal;
            synchronized (this) {
                addedLocal = new ArrayList<>(added);
                removedLocal = new ArrayList<>(removed);
            }
            enqueued.set(false);
            added.clear();
            removed.clear();
            if (listener != null) {
                Collections.sort(addedLocal, (a, b) -> {
                    return Integer.compare(indexOf(a), indexOf(b));
                });
                listener.accept(lkp, removedLocal, addedLocal);
            }
        }
    }

    interface IChange {

        <T> void changed(InspectorsEntry<T> e, boolean active);
    }

    private int indexOf(InspectorFactory<?> factory) {
        for (InspectorsEntry<?> entry : entries) {
            if (entry.factory.equals(factory)) {
                return entry.order;
            }
        }
        return 10000;
    }

    static int ids = 0;
    private static final class InspectorsEntry<T> implements LookupListener, Comparable<InspectorsEntry<?>> {

        private final InspectorFactory<T> factory;
        private final IChange onChange;
        private final Lookup.Result res;
        private int order = ids++;

        public InspectorsEntry(InspectorFactory<T> factory, IChange onChange, Lookup lkp) {
            this.factory = factory;
            this.onChange = onChange;
            res = lkp.lookupResult(factory.type());
            res.addLookupListener(this);
            resultChanged(null);
        }

        static <T> InspectorsEntry<T> newEntry(InspectorFactory<T> factory, IChange chg, Lookup lkp) {
            return new InspectorsEntry(factory, chg, lkp);
        }

        @Override
        public void resultChanged(LookupEvent le) {
            boolean active = !res.allItems().isEmpty();
            onChange.changed(this, active);
        }

        Class<T> type() {
            return factory.type();
        }

        @Override
        public int compareTo(InspectorsEntry<?> o) {
            return Integer.compare(order, o.order);
        }
    }
}
