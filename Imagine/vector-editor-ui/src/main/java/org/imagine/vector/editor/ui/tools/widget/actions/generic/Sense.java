/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget.actions.generic;

import com.mastfrog.util.strings.Strings;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;

/**
 * Thing which listens on a lookup.
 *
 * @author Tim Boudreau
 */
final class Sense<T> implements Supplier<T>, LookupListener {

    final Class<T> type;
    private final Lookup lookup;
    private final List<Runnable> notify = new CopyOnWriteArrayList<>();
    private Lookup.Result<T> result;

    public Sense(Class<T> type, Lookup lookup) {
        this.type = type;
        this.lookup = lookup;
    }

    private Sense(Sense other, Lookup lkp) {
        this.type = other.type;
        this.lookup = lkp;
    }

    public Sense<T> over(Lookup lkp) {
        if (lkp != lookup) {
            return new Sense(this, lkp);
        }
        return this;
    }

    public boolean isEmpty() {
        return all().isEmpty();
    }

    public boolean isNonEmpty() {
        return !all().isEmpty();
    }

    public boolean isExactlyOne() {
        Collection<? extends T> a = all();
        if (a.isEmpty()) {
            return false;
        }
        if (a.size() == 1) {
            return true;
        }
        return new HashSet<>(a).size() == 1;
    }

    public boolean isMoreThanOne() {
        return all().size() > 1;
    }

    @Override
    public T get() {
        if (result != null) {
            Collection<? extends T> coll = result.allInstances();
            return coll.isEmpty() ? null : coll.iterator().next();
        }
        return lookup.lookup(type);
    }

    public Collection<? extends T> all() {
        if (result != null) {
            return result.allInstances();
        }
        return lookup.lookupAll(type);
    }

    public synchronized void listen(Runnable r) {
        boolean wasEmpty = notify.isEmpty();
        notify.add(r);
        if (wasEmpty) {
            addNotify();
        } else {
            removeNotify();
        }
    }

    public synchronized void unlisten(Runnable r) {
        notify.remove(r);
        if (notify.isEmpty()) {
            removeNotify();
        }
    }

    private void removeNotify() {
        if (result != null) {
            result.removeLookupListener(this);
        }
        result = null;
    }

    private void addNotify() {
        result = lookup.lookupResult(type);
        result.addLookupListener(this);
        result.allInstances();
    }

    @Override
    public void resultChanged(LookupEvent le) {
        for (Runnable r : notify) {
            r.run();
        }
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + Objects.hashCode(this.type);
        hash = 53 * hash + Objects.hashCode(this.lookup);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Sense<?> other = (Sense<?>) obj;
        if (!Objects.equals(this.type, other.type)) {
            return false;
        }
        return this.lookup == other.lookup
                || this.lookup.equals(other.lookup);
    }

    @Override
    public String toString() {
        return type.getSimpleName() + "(" + Strings.join(',', all()) + ")";
    }
}
