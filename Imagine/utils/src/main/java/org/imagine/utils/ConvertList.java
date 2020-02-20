package org.imagine.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 *
 * @author Tim Boudreau
 */
public class ConvertList<T, R> implements List<T> {

    private final Class<T> type;
    private final Class<R> origType;
    private final List<R> orig;
    private final Converter<T, R> converter;

    public ConvertList(Class<T> type, Class<R> origType, List<R> orig, Converter<T, R> converter) {
        this.type = type;
        this.origType = origType;
        this.orig = orig;
        this.converter = converter;
    }

    @Override
    public int size() {
        return orig.size();
    }

    @Override
    public boolean isEmpty() {
        return orig.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        if (type.isInstance(o)) {
            return orig.contains(converter.unconvert(type.cast(o)));
        }
        return false;
    }

    @Override
    public Iterator<T> iterator() {
        return listIterator();
    }

    @Override
    public Object[] toArray() {
        int max = size();
        T[] result = (T[]) Array.newInstance(type, max);
        for (int i = 0; i < max; i++) {
            result[i] = get(i);
        }
        return result;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        int max = size();
        if (a.length != max) {
            a = (T[]) Array.newInstance(a.getClass().getComponentType(), max);
        }
        for (int i = 0; i < max; i++) {
            a[i] = (T) get(i);
        }
        return a;
    }

    @Override
    public boolean add(T e) {
        return orig.add(converter.unconvert(e));
    }

    @Override
    public boolean remove(Object o) {
        if (type.isInstance(o)) {
            R r = converter.unconvert(type.cast(o));
            return orig.remove(r);
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        for (T t : c) {
            orig.add(converter.unconvert(t));
        }
        return true;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        return orig.addAll(new ConvertList<R, T>(origType, type, toList(c), new ReverseConverter(converter)));
    }

    private List<T> toList(Collection<? extends T> c) {
        if (c instanceof List) {
            return (List<T>) c;
        } else {
            return new ArrayList<>(c);
        }
    }

    private List<R> convertList(Collection<?> c) {
        List<R> result = new ArrayList<>();
        for (Object o : c) {
            if (type.isInstance(o)) {
                result.add(converter.unconvert(type.cast(o)));
            }
        }
        return result;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return orig.removeAll(convertList(c));
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return orig.retainAll(convertList(c));
    }

    @Override
    public void clear() {
        orig.clear();
    }

    @Override
    public T get(int index) {
        return converter.convert(orig.get(index));
    }

    @Override
    public T set(int index, T element) {
        return converter.convert(orig.set(index, converter.unconvert(element)));
    }

    @Override
    public void add(int index, T element) {
        orig.add(index, converter.unconvert(element));
    }

    @Override
    public T remove(int index) {
        return converter.convert(orig.remove(index));
    }

    @Override
    public int indexOf(Object o) {
        return type.isInstance(o) ? orig.indexOf(converter.unconvert(type.cast(o))) : -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        return type.isInstance(o) ? orig.lastIndexOf(converter.unconvert(type.cast(o))) : -1;
    }

    @Override
    public ListIterator<T> listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<T> listIterator(final int index) {
        return new ListIterator<T>() {
            private final ListIterator<R> delegate = orig.listIterator(index);

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public T next() {
                return converter.convert(delegate.next());
            }

            @Override
            public boolean hasPrevious() {
                return delegate.hasPrevious();
            }

            @Override
            public T previous() {
                return converter.convert(delegate.previous());
            }

            @Override
            public int nextIndex() {
                return delegate.nextIndex();
            }

            @Override
            public int previousIndex() {
                return delegate.previousIndex();
            }

            @Override
            public void remove() {
                delegate.remove();
            }

            @Override
            public void set(T e) {
                delegate.set(converter.unconvert(e));
            }

            @Override
            public void add(T e) {
                delegate.add(converter.unconvert(e));
            }
        };
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return new ConvertList<>(type, origType, orig.subList(fromIndex, toIndex), converter);
    }

    public interface Converter<T, R> {

        public T convert(R r);

        public R unconvert(T t);
    }

    static final class ReverseConverter<T, R> implements Converter<T, R> {

        private final Converter<R, T> delegate;

        public ReverseConverter(Converter<R, T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public T convert(R r) {
            return delegate.unconvert(r);
        }

        @Override
        public R unconvert(T t) {
            return delegate.convert(t);
        }
    }
}
