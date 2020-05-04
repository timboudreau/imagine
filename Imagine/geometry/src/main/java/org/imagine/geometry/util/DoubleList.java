/*
 * The MIT License
 *
 * Copyright 2019 Mastfrog Technologies.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.imagine.geometry.util;

import com.mastfrog.util.collections.ArrayUtils;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoublePredicate;

/**
 *
 * @author Tim Boudreau
 */
public final class DoubleList /* extends AbstractList<Double> */ implements Iterable<Double> {

    private double[] vals;
    private int size;
    private final int initialCapacity;
    private boolean sorted;

    public DoubleList() {
        this(64);
    }

    public DoubleList(int cap) {
        this.vals = new double[initialCapacity = Math.max(10, cap)];
    }

    private DoubleList(double[] arr, boolean sorted) {
        initialCapacity = arr.length;
        size = arr.length;
        this.sorted = sorted;
        this.vals = Arrays.copyOf(arr, arr.length);
    }

    public DoubleList(double[] arr) {
        initialCapacity = arr.length;
        size = arr.length;
        sorted = false;
        this.vals = Arrays.copyOf(arr, arr.length);
    }

    private DoubleList(double[] vals, int size, int initialCapacity, boolean sorted) {
        this.vals = vals;
        this.size = size;
        this.initialCapacity = initialCapacity;
        this.sorted = sorted;
    }

    private DoubleList(DoubleList other) {
        this.vals = Arrays.copyOf(other.vals, other.size);
        this.size = other.size;
        this.initialCapacity = other.initialCapacity;
        this.sorted = other.sorted;
    }

    public DoubleList copy() {
        return new DoubleList(this);
    }

    public double mean() {
        // Stats does more work, so for quick uses just requiring the
        // mean, do it separately
        double sum = 0;
        for (int i = 0; i < size; i++) {
            sum += vals[i];
        }
        return sum / (double) size;
    }

    public DoubleList pruningNonFinite() {
        DoubleList result = new DoubleList(size);
        for (int i = 0; i < size; i++) {
            double v = vals[i];
            if (!Double.isNaN(v) && Double.isFinite(v)) {
                continue;
            }
            result.add(v);
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        DecimalFormat df = new DecimalFormat("#####0.0########");
        for (int i = 0; i < size; i++) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
//            sb.append(df.format(vals[i]));
            sb.append(vals[i]);
        }
        return sb.toString();
    }

    public DoubleList extractValuesAtOrAbove(double value) {
        if (!sorted) {
            sort();
        }
        int firstNew = -1;
        for (int i = 0; i < size; i++) {
            double v = vals[i];
            if (v >= value) {
                firstNew = i;
                break;
            }
        }
        if (firstNew == -1) {
            return new DoubleList(10);
        }
        double[] nue = Arrays.copyOfRange(vals, firstNew, size);
        double[] mine = Arrays.copyOf(vals, firstNew);
        this.vals = mine;
        this.size = mine.length;
        return new DoubleList(vals, size, initialCapacity, sorted);
    }

    public int size() {
        return size;
    }

    public boolean contains(double value) {
        return indexOf(value) >= 0;
    }

    public double first() {
        if (size == 0) {
            throw new IndexOutOfBoundsException();
        }
        return vals[0];
    }

    public double last() {
        if (size == 0) {
            throw new IndexOutOfBoundsException();
        }
        return vals[size - 1];
    }

    public void setAsDouble(int index, double value) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(index + " of " + size);
        }
        sorted = false;
        vals[index] = value;
    }

    public int indexOf(double value) {
        if (sorted) {
            return Arrays.binarySearch(vals, 0, size, value);
        }
        for (int i = 0; i < size; i++) {
            if (value == vals[i]) {
                return i;
            }
        }
        return -1;
    }

    public double sum() {
        double result = 0;
        for (int i = 0; i < size; i++) {
            result += vals[i];
        }
        return result;
    }

    public void addAll(DoubleList other) {
        double[] nue = ArrayUtils.concatenate(toDoubleArray(), other.toDoubleArray());
        sorted = false;
        vals = nue;
        size += other.size();
    }

    public double getDouble(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(index + " out of bounds (size "
                    + size + ")");
        }
        return vals[index];
    }

    public double percentageMatching(DoublePredicate test) {
        double ct = count(test);
        double sz = size;
        return ct / sz;
    }

    public int count(DoublePredicate test) {
        int result = 0;
        for (int i = 0; i < size; i++) {
            if (test.test(vals[i])) {
                result++;
            }
        }
        return result;
    }

    public int countValuesBoundedBy(double least, double greatest) {
        int result = 0;
        for (int i = 0; i < size; i++) {
            double v = vals[i];
            if (v >= least && v <= greatest) {
                result++;
            }
        }
        return result;
    }

    public DoubleList sort() {
        if (sorted) {
            return this;
        }
        Arrays.sort(vals, 0, size);
        sorted = true;
        return this;
    }

    public double min() {
        if (size == 0) {
            return 0;
        }
        if (sorted) {
            return vals[0];
        } else {
            double min = vals[0];
            for (int i = 1; i < size; i++) {
                if (vals[i] < min) {
                    min = vals[i];
                }
            }
            return min;
        }
    }

    public double max() {
        if (size == 0) {
            return 0;
        }
        if (sorted) {
            return vals[0];
        } else {
            double max = vals[0];
            for (int i = 1; i < size; i++) {
                if (vals[i] > max) {
                    max = vals[i];
                }
            }
            return max;
        }
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public double[] toDoubleArray() {
        return Arrays.copyOf(vals, size);
    }

    public DoubleList trim() {
        vals = Arrays.copyOf(vals, size);
        return this;
    }

    public DoubleList topHalf() {
        sort();
        if (this.size > 1) {
            int newSize = size - (size / 2);
            vals = ArrayUtils.extract(vals, size / 2, newSize);
            size = newSize;
        }
        return this;
    }

    public DoubleList truncate(int size) {
        if (size < this.size) {
            this.size = size;
            trim();
        }
        return this;
    }

    public void add(double val) {
        if (size >= vals.length - 1) {
            vals = Arrays.copyOf(vals, size + initialCapacity);
        }
        vals[size++] = val;
        if (size > 1 && sorted) {
            sorted = val > vals[size - 2];
        }
    }

    public List<DoubleList> split() {
        if (size() <= 1) {
            return Arrays.asList(copy());
        }
        trim();
        double[][] ab = ArrayUtils.split(vals, size / 2);
        return Arrays.asList(new DoubleList(ab[0], sorted), new DoubleList(ab[1], sorted));
    }

    public PrimitiveIterator.OfDouble iterator() {
        return new Iter();
    }

    public void addAll(double[] vals) {
        if (vals.length == 0) {
            return;
        }
        int newSize = size + vals.length;
        if (newSize > size) {
            this.vals = Arrays.copyOf(this.vals, newSize + initialCapacity);
        }
        System.arraycopy(vals, 0, this.vals, size, vals.length);
        size = newSize;
        sorted = false;
    }

    public List<DoubleList> subdivide(int times) {
        return split(this, times);
    }

    private static List<DoubleList> split(DoubleList dl, int times) {
        List<DoubleList> l = new ArrayList<>(times * 2);
        split(dl, times, l);
        return l;
    }

    private static void split(DoubleList dl, int times, List<DoubleList> into) {
        if (times == 1 || dl.size() <= 1) {
            into.add(dl);
            return;
        }
        for (DoubleList spl : dl.split()) {
            split(spl, times - 1, into);
        }
    }

//    @Override
    public Double get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Out of range 0:"
                    + (size - 1) + ": " + index);
        }
        return this.getDouble(index);
    }

    @Override
    public void forEach(Consumer<? super Double> action) {
        for (int i = 0; i < size; i++) {
            action.accept(vals[i]);
        }
    }

    public void forEachDouble(DoubleConsumer dc) {
        for (int i = 0; i < size; i++) {
            dc.accept(vals[i]);
        }
    }

    public void put(float[] arr, int length) {
        if (length == 0) {
            return;
        }
        for (int i = 0; i < length; i++) {
            add(arr[i]);
        }
    }

    public void put(double[] arr, int length) {
        if (length == 0) {
            return;
        }
        int newSize = size + length;
        if (newSize > vals.length) {
            this.vals = Arrays.copyOf(this.vals, newSize + Math.max(16, (initialCapacity / 2)));
        }
        System.arraycopy(arr, 0, this.vals, size, length);
        size = newSize;
        sorted = false;
    }

    private final class Iter implements PrimitiveIterator.OfDouble {

        int ix = -1;

        @Override
        public double nextDouble() {
            return vals[++ix];
        }

        @Override
        public boolean hasNext() {
            return ix + 1 < vals.length;
        }
    }
}
