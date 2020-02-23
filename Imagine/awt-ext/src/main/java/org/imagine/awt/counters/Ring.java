package org.imagine.awt.counters;

import com.mastfrog.util.collections.CollectionUtils;
import com.mastfrog.util.strings.Strings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author Tim Boudreau
 */
public final class Ring<T> implements Iterable<T> {

    final List<T> contents;
    private final int limit;

    public Ring(int limit) {
        this.limit = limit;
        contents = new CopyOnWriteArrayList<>();
    }

    public String toString() {
        return Strings.join(',', contents);
    }

    public int size() {
        return contents.size();
    }

    public int limit() {
        return limit;
    }

    public double usage() {
        return (double) size() / limit;
    }

    public List<T> copy() {
        return Collections.unmodifiableList(new ArrayList<>(contents));
    }

    public void put(T t) {
        contents.add(t);
        while (contents.size() > limit) {
            contents.remove(0);
        }
    }

    @Override
    public Iterator<T> iterator() {
        return CollectionUtils.unmodifiableIterator(contents.iterator());
    }

}
