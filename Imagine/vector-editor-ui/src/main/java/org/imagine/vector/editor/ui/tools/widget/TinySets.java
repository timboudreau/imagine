package org.imagine.vector.editor.ui.tools.widget;

import com.mastfrog.util.collections.CollectionUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * Sets with the most minimal memory footprint possible, for dealing with large
 * numbers of sets of 0, 1 or 2 items; for these, hash lookups just add
 * overhead. Since the situation where three or more points occupy the same
 * coordinates is exceedingly rare, this will be the best option most of the
 * time.
 * <p>
 * What you get is a Set implementation which delegates to either an empty,
 * single-item, double-item or regular HashSet implementation, and replaces it
 * on mutation operations - so basically you're paying for 1-3 fields, as
 * opposed to a hash tree, for small sets - and for these sizes of sets,
 * membership tests are cheaper as direct equality tests.
 * </p>
 *
 * @author Tim Boudreau
 */
public class TinySets {

    private static boolean disabled = Boolean.getBoolean("TinySets.disabled");
    private TinySets() {
        throw new AssertionError();
    }

    public static <T> Set<T> empty() {
        if (disabled) {
            return new HashSet<>(1);
        }
        return new TinySet<>();
    }

    public static <T> Set<T> of(T a) {
        if (disabled) {
            Set<T> result = new HashSet<>(1);
            result.add(a);
            return result;
        }
        return new TinySet<>(new SingleSet<>(a));
    }

    public static <T> Set<T> of(T a, T b) {
        if (disabled) {
            Set<T> result = new HashSet<>(2);
            result.add(a);
            result.add(b);
            return result;
        }
        if (Objects.equals(a, b)) {
            return new TinySet<>(new SingleSet<>(a));
        }
        return new TinySet<>(new BiSet<>(a, b));
    }

    private interface AdaptiveSet<T> extends Set<T> {

        AdaptiveSet<T> adding(T obj);

        AdaptiveSet<T> removing(Object obj);
    }

    private static final Empty<Object> EMPTY = new Empty<>();

    private static <T> Empty<T> emptyInstance() {
        return (Empty<T>) EMPTY;
    }

    private static final class Empty<T> implements AdaptiveSet<T> {

        @Override
        public AdaptiveSet<T> adding(T obj) {
            return new SingleSet<>(obj);
        }

        @Override
        public String toString() {
            return "[]";
        }

        @Override
        public AdaptiveSet<T> removing(Object obj) {
            return this;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @Override
        public Iterator<T> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public Object[] toArray() {
            return new Object[0];
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] a) {
            if (a.length == 0) {
                return a;
            }
            return CollectionUtils.genericArray((Class<T>) a.getClass().getComponentType(), 0);
        }

        @Override
        public boolean add(T e) {
            throw new UnsupportedOperationException("empty");
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException("empty");
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean addAll(Collection<? extends T> c) {
            throw new UnsupportedOperationException("empty");
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException("empty");
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("empty");
        }

        public boolean equals(Object o) {
            if (o == null) {
                return false;
            } else if (o instanceof Empty<?>) {
                return true;
            } else if (o instanceof Collection<?>) {
                return ((Collection<?>) o).isEmpty();
            }
            return false;
        }

        public int hashCode() {
            return 0;
        }
    }

    private static final class SingleSet<T> implements AdaptiveSet<T> {

        private final T val;

        public SingleSet(T val) {
            this.val = val;
        }

        public AdaptiveSet<T> adding(T val) {
            if (Objects.equals(val, this.val)) {
                return this;
            }
            return new BiSet<>(this.val, val);
        }

        @Override
        public AdaptiveSet<T> removing(Object obj) {
            if (Objects.equals(obj, val)) {
                return emptyInstance();
            }
            return this;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean contains(Object o) {
            return Objects.equals(o, val);
        }

        @Override
        public Iterator<T> iterator() {
            return CollectionUtils.singletonIterator(val);
        }

        @Override
        public Object[] toArray() {
            return new Object[]{val};
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] a) {
            if (a.length == 1) {
                a[0] = (T) val;
                return a;
            }
            a = CollectionUtils.genericArray(
                    (Class<T>) a.getClass().getComponentType(), 1);
            a[0] = (T) val;
            return a;
        }

        @Override
        public boolean add(T e) {
            throw new UnsupportedOperationException("singleton");
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException("singleton");
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return c.size() == 1 && contains(c.iterator().next());
        }

        @Override
        public boolean addAll(Collection<? extends T> c) {
            throw new UnsupportedOperationException("singleton");
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            if (containsAll(c)) {
                return false;
            }
            throw new UnsupportedOperationException("singleton");
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException("singleton");
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("singleton");
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            } else if (o == this) {
                return true;
            }
            if (o instanceof SingleSet<?>) {
                return Objects.equals(((SingleSet<?>) o).val, val);
            } else if (o instanceof Collection<?>) {
                Collection<?> c = (Collection<?>) o;
                return c.size() == 1 && Objects.equals(val, c.iterator().next());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(val);
        }

        @Override
        public String toString() {
            return "[" + val + ']';
        }
    }

    private static final class BiSet<T> implements AdaptiveSet<T> {

        private final T val1;
        private final T val2;

        public BiSet(T val1, T val2) {
            this.val1 = val1;
            this.val2 = val2;
        }

        @Override
        public AdaptiveSet<T> adding(T obj) {
            if (Objects.equals(val1, obj) || Objects.equals(val2, obj)) {
                return this;
            }
            return new MultiSet<>(this, obj);
        }

        @Override
        public AdaptiveSet<T> removing(Object obj) {
            if (Objects.equals(val1, obj)) {
                return rightSide();
            } else if (Objects.equals(val2, obj)) {
                return leftSide();
            }
            return this;
        }

        SingleSet<T> leftSide() {
            return new SingleSet<>(val1);
        }

        SingleSet<T> rightSide() {
            return new SingleSet<>(val2);
        }

        @Override
        public int size() {
            return 2;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public String toString() {
            return "[" + val1 + ", " + val2 + ']';
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(val1) + Objects.hashCode(val2);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o == null) {
                return false;
            } else if (o instanceof BiSet<?>) {
                BiSet<?> b = (BiSet<?>) o;
                return (Objects.equals(val1, b.val1)
                        && Objects.equals(val2, b.val2))
                        || (Objects.equals(val2, b.val1)
                        && Objects.equals(val1, b.val2));
            } else if (o instanceof Collection<?>) {
                Collection<?> c = (Collection<?>) o;
                return c.size() == 2 && containsAll(c);
            }
            return false;
        }

        @Override
        public boolean contains(Object o) {
            return Objects.equals(o, val1)
                    || Objects.equals(o, val2);
        }

        @Override
        public Iterator<T> iterator() {
            return new BiIterator<>(val1, val2);
        }

        static final class BiIterator<T> implements Iterator<T> {

            private final T a, b;
            private byte cursor = -1;

            public BiIterator(T a, T b) {
                this.a = a;
                this.b = b;
            }

            @Override
            public boolean hasNext() {
                switch (cursor) {
                    case 0:
                        return true;
                    case 1:
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public T next() {
                switch (++cursor) {
                    case 0:
                        return a;
                    case 1:
                        return b;
                    default:
                        throw new NoSuchElementException();
                }
            }
        }

        @Override
        public Object[] toArray() {
            return new Object[]{val1, val2};
        }

        @Override
        public <T> T[] toArray(T[] a) {
            if (a.length != 2) {
                a = CollectionUtils.genericArray((Class<T>) a.getClass().getComponentType(), 2);
                a[0] = (T) val1;
                a[1] = (T) val2;
                return a;
            }
            a[0] = (T) val1;
            a[1] = (T) val2;
            return a;
        }

        @Override
        public boolean add(T e) {
            throw new UnsupportedOperationException("dual");
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException("dual");
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            for (Object o : c) {
                if (!Objects.equals(val1, o) && !Objects.equals(val2, o)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean addAll(Collection<? extends T> c) {
            throw new UnsupportedOperationException("dual");
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException("dual");
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException("dual");
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("dual");
        }
    }

    private static class MultiSet<T> extends HashSet<T> implements AdaptiveSet<T> {

        MultiSet() {
            super(3);
        }

        MultiSet(T... objs) {
            super(objs.length);
            for (T obj : objs) {
                add(obj);
            }
        }

        MultiSet(BiSet<T> set, T next) {
            super(3);
            add(set.val1);
            add(set.val2);
            add(next);
        }

        @Override
        public AdaptiveSet<T> adding(T obj) {
            add(obj);
            return this;
        }

        @Override
        @SuppressWarnings("element-type-mismatch")
        public AdaptiveSet<T> removing(Object obj) {
            if (remove(obj) && size() == 2) {
                Iterator<T> iter = iterator();
                return new BiSet<>(iter.next(), iter.next());
            }
            return this;
        }
    }

    private static class TinySet<T> implements Set<T> {

        private AdaptiveSet<T> delegate;

        public TinySet() {
            delegate = new Empty<>();
        }

        public TinySet(AdaptiveSet<T> delegate) {
            this.delegate = delegate;
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
        public String toString() {
            return delegate.toString();
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
        public boolean contains(Object o) {
            return delegate.contains(o);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Iterator<T> iterator() {
            if (delegate instanceof MultiSet<?>) {
                // Avoid ConcurrentModificationExceptions
                return CollectionUtils.toIterator((T[]) delegate.toArray());
            }
            return new TsIter(delegate.iterator());
        }

        @Override
        public Object[] toArray() {
            return delegate.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return delegate.toArray(a);
        }

        @Override
        public boolean add(T e) {
            Set<T> old = delegate;
            if (old instanceof MultiSet<?>) {
                return old.add(e);
            }
            delegate = delegate.adding(e);
            return old != delegate;
        }

        @Override
        public boolean remove(Object o) {
            Set<T> old = delegate;
            if (delegate instanceof MultiSet<?>) {
                boolean result = delegate.remove(o);
                if (result && delegate.size() == 2) {
                    Iterator<T> objs = delegate.iterator();
                    BiSet<T> bis = new BiSet<>(objs.next(), objs.next());
                    delegate = bis;
                    return true;
                }
            }
            delegate = delegate.removing(o);
            return delegate != old;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return delegate.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends T> c) {
            if (delegate instanceof MultiSet<?>) {
                return delegate.addAll(c);
            }
            boolean result = false;
            for (T obj : c) {
                result |= add(obj);
            }
            return result;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            if (delegate instanceof MultiSet<?>) {
                boolean result = delegate.retainAll(c);
                if (result) {
                    switch (delegate.size()) {
                        case 0:
                            delegate = emptyInstance();
                            break;
                        case 1:
                            delegate = new SingleSet<>(delegate.iterator().next());
                            break;
                        case 2:
                            Iterator<T> iter = delegate.iterator();
                            delegate = new BiSet<>(iter.next(), iter.next());
                            break;
                    }
                }
                return result;
            }
            AdaptiveSet<T> old = delegate;
            for (Object o : delegate) {
                if (!c.contains(o)) {
                    delegate = delegate.removing(o);
                    if (isEmpty()) {
                        return true;
                    }
                }
            }
            return old != delegate;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            int size = size();
            for (Object o : c) {
                delegate = delegate.removing(o);
                if (isEmpty()) {
                    break;
                }
            }
            return size() != size;
        }

        @Override
        public void clear() {
            delegate = emptyInstance();
        }

        class TsIter implements Iterator<T> {

            private final Iterator<T> iter;
            private T last;

            public TsIter(Iterator<T> delegate) {
                this.iter = delegate;
            }

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public T next() {
                return last = iter.next();
            }

            @Override
            public void remove() {
                if (last == null) {
                    throw new NoSuchElementException("Last element was null - "
                            + "assuming iteration not started.");
                }
                if (delegate instanceof SingleSet<?>) {
                    delegate = delegate.removing(last);
                    return;
                }
                if (delegate instanceof MultiSet<?>) {
                    MultiSet<?> ms = (MultiSet<?>) delegate;
                    if (ms.size() > 3) {
                        iter.remove();
                        return;
                    }
                }
                delegate = delegate.removing(last);
            }
        }
    }
}
