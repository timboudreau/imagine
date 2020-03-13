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
package org.imagine.geometry;

import com.mastfrog.function.FloatConsumer;
import com.mastfrog.util.collections.ArrayUtils;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.DoublePredicate;

/**
 *
 * @author Tim Boudreau
 */
public final class FloatList /* extends AbstractList<Double> */ implements Iterable<Float> {

    private float[] vals;
    private int size;
    private final int initialCapacity;
    private boolean sorted;

    public FloatList() {
        this(64);
    }

    public FloatList(int cap) {
        this.vals = new float[initialCapacity = Math.max(10, cap)];
    }

    private FloatList(float[] arr, boolean sorted) {
        initialCapacity = arr.length;
        size = arr.length;
        this.sorted = sorted;
        this.vals = Arrays.copyOf(arr, arr.length);
    }

    public FloatList(float[] arr) {
        initialCapacity = arr.length;
        size = arr.length;
        sorted = false;
        this.vals = Arrays.copyOf(arr, arr.length);
    }

    private FloatList(float[] vals, int size, int initialCapacity, boolean sorted) {
        this.vals = vals;
        this.size = size;
        this.initialCapacity = initialCapacity;
        this.sorted = sorted;
    }

    private FloatList(FloatList other) {
        this.vals = Arrays.copyOf(other.vals, other.size);
        this.size = other.size;
        this.initialCapacity = other.initialCapacity;
        this.sorted = other.sorted;
    }

    public FloatList copy() {
        return new FloatList(this);
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

    public FloatList pruningNonFinite() {
        FloatList result = new FloatList(size);
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

    public int size() {
        return size;
    }

    public double sum() {
        double result = 0;
        for (int i = 0; i < size; i++) {
            result += vals[i];
        }
        return result;
    }

    public void addAll(FloatList other) {
        float[] nue = ArrayUtils.concatenate(toFloatArray(), other.toFloatArray());
        sorted = false;
        vals = nue;
        size += other.size();
    }

    public float getFloat(int index) {
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

    public FloatList sort() {
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

    public float[] toFloatArray() {
        return Arrays.copyOf(vals, size);
    }

    public FloatList trim() {
        vals = Arrays.copyOf(vals, size);
        return this;
    }

    public FloatList topHalf() {
        sort();
        if (this.size > 1) {
            int newSize = size - (size / 2);
            vals = ArrayUtils.extract(vals, size / 2, newSize);
            size = newSize;
        }
        return this;
    }

    public FloatList truncate(int size) {
        if (size < this.size) {
            this.size = size;
            trim();
        }
        return this;
    }

    public void put(float[] arr, int length) {
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

    public void get(int offset, float[] into) {
        get(offset, into, 0, into.length);
    }

    public void get(int offset, float[] into, int length) {
        get(offset, into, 0, length);
    }

    public void get(int offset, float[] into, int arrayOffset, int length) {
        System.arraycopy(vals, offset, into, arrayOffset, length);
    }

    public void add(double val) {
        if (size >= vals.length - 1) {
            vals = Arrays.copyOf(vals, size + initialCapacity);
        }
        vals[size++] = (float) val;
        if (size > 1 && sorted) {
            sorted = val > vals[size - 2];
        }
    }

    public void add(float val) {
        if (size >= vals.length - 1) {
            vals = Arrays.copyOf(vals, size + initialCapacity);
        }
        vals[size++] = val;
        if (size > 1 && sorted) {
            sorted = val > vals[size - 2];
        }
    }

    public void addAll(float[] vals) {
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

//    @Override
    public Float get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Out of range 0:"
                    + (size - 1) + ": " + index);
        }
        return this.getFloat(index);
    }

    @Override
    public void forEach(Consumer<? super Float> action) {
        for (int i = 0; i < size; i++) {
            action.accept(vals[i]);
        }
    }

    public void forEachFloat(FloatConsumer dc) {
        for (int i = 0; i < size; i++) {
            dc.accept(vals[i]);
        }
    }

    @Override
    public Iterator<Float> iterator() {
        return new Iter();
    }

    private final class Iter implements Iterator<Float> {

        int ix = -1;

        public float nextFloat() {
            return vals[++ix];
        }

        @Override
        public boolean hasNext() {
            return ix + 1 < vals.length;
        }

        @Override
        public Float next() {
            return nextFloat();
        }
    }
}
