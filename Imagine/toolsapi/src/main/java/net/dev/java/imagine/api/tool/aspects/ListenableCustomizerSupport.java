/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.dev.java.imagine.api.tool.aspects;

import java.awt.EventQueue;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.openide.util.Exceptions;

/**
 *
 * @author Tim Boudreau
 */
public abstract class ListenableCustomizerSupport<T> implements Customizer<T> {

    private final List<Reference<Consumer<? super T>>> listeners
            = new CopyOnWriteArrayList<>();

    public final Runnable listen(Consumer<? super T> consumer) {
        WeakReference<Consumer<? super T>> ref = new WeakReference<>(consumer);
        listeners.add(ref);
        return new Remover(this, ref);
    }

    protected final int pruneListeners() {
        for (Iterator<Reference<Consumer<? super T>>> it = listeners.iterator(); it.hasNext();) {
            Reference<Consumer<? super T>> r = it.next();
            Consumer<? super T> c = r.get();
            if (c == null) {
                it.remove();
            }
        }
        return listeners.size();
    }

    protected final boolean hasListeners() {
        return pruneListeners() > 0;
    }

    protected final void fire() {
        notifier.enqueue();
    }

    protected void onAfterFire() {
        // do nothing
    }

    protected final void fireImmediately() {
        T obj = null;
        List<Reference<Consumer<? super T>>> toRemove = new ArrayList<>();
        for (Iterator<Reference<Consumer<? super T>>> it = listeners.iterator(); it.hasNext();) {
            Reference<Consumer<? super T>> r = it.next();
            Consumer<? super T> c = r.get();
            if (c == null) {
                toRemove.add(r);
            } else {
                if (obj == null) {
                    obj = get();
                }
                try {
                    c.accept(obj);
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
        onAfterFire();
        listeners.removeAll(toRemove);
    }

    private final Notifier notifier = new Notifier();

    class Notifier implements Runnable {

        private final AtomicBoolean enqueued = new AtomicBoolean();

        void enqueue() {
            if (enqueued.compareAndSet(false, true)) {
                EventQueue.invokeLater(this);
            }
        }

        @Override
        public void run() {
            enqueued.set(false);
            fireImmediately();
        }
    }

    static final class Remover implements Runnable {

        private final ListenableCustomizerSupport<?> supp;
        private final Reference<? extends Consumer<?>> ref;

        public Remover(ListenableCustomizerSupport<?> supp, Reference<? extends Consumer<?>> ref) {
            this.supp = supp;
            this.ref = ref;
        }

        @Override
        @SuppressWarnings("element-type-mismatch")
        public void run() {
            supp.listeners.remove(ref);
        }

    }
}
