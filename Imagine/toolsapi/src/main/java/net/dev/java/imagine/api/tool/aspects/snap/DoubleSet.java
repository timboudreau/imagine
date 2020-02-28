package net.dev.java.imagine.api.tool.aspects.snap;

import com.mastfrog.util.search.Bias;
import static com.mastfrog.util.search.Bias.BACKWARD;
import static com.mastfrog.util.search.Bias.FORWARD;
import static com.mastfrog.util.search.Bias.NEAREST;
import static com.mastfrog.util.search.Bias.NONE;
import static java.lang.Double.doubleToLongBits;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.PrimitiveIterator;
import java.util.function.DoubleConsumer;

/**
 * A high-performance, binary-search based Set of primitive doubles.
 *
 * @author Tim Boudreau
 */
public final class DoubleSet implements Iterable<Double> {

    private static final DecimalFormat FMT
            = new DecimalFormat("#####################0.########################"
                    + "##########");

    double[] data;
    private boolean clean;
    private final int initialCapacity;
    private int size;

    public DoubleSet() {
        this(128);
    }

    public DoubleSet(int capacity) {
        data = new double[capacity];
        initialCapacity = capacity;
    }

    private DoubleSet(double[] data) {
        this.data = data;
        size = data.length;
        initialCapacity = size;
        clean = true;
    }

    private DoubleSet(double[] data, int size, boolean clean) {
        this.data = data;
        this.size = size;
        this.clean = clean;
        this.initialCapacity = data.length;
    }

    public void clear() {
        size = 0;
        clean = true;
    }

    public static DoubleSet of(Collection<? extends Number> c) {
        DoubleSet set = new DoubleSet(c.size());
        for (Number n : c) {
            if (n == null) {
                throw new IllegalArgumentException("Collection contains null: " + c);
            }
            set.add(n.doubleValue());
        }
        return set;
    }

    public static DoubleSet ofFloats(float... floats) {
        DoubleSet result = new DoubleSet(floats.length);
        for (int i = 0; i < floats.length; i++) {
            result.add(floats[i]);
        }
        return result;
    }

    public static DoubleSet ofDoubles(double... doubles) {
        return ofDoubles(doubles.length, doubles);
    }

    public static DoubleSet ofDoubles(int capacity, double... doubles) {
        DoubleSet result = new DoubleSet(capacity);
        for (int i = 0; i < doubles.length; i++) {
            result.add(doubles[i]);
        }
        return result;
    }

    @Override
    public String toString() {
        ensureClean();
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < size; i++) {
            sb.append(FMT.format(data[i]));
            if (i != size - 1) {
                sb.append(", ");
            }
        }
        return sb.append('}').toString();
    }

    public DoubleSet[] partition(int maxPartitions) {
        ensureClean();
        if (maxPartitions < 0) {
            throw new IllegalArgumentException("Negative partitions " + maxPartitions);
        }
        if (maxPartitions == 0 || size < maxPartitions * 4) {
            return new DoubleSet[]{this};
        }
        int itemsPer = size / maxPartitions;
        // Don't allocate one partition that's going to have, say,
        // three items
        if (size - ((itemsPer - 1) * size) < 5) {
            itemsPer--;
        }
        if (itemsPer <= 1) {
            return new DoubleSet[]{this};
        }
        int partitions = size / itemsPer;
        if (partitions * itemsPer < size) {
            partitions++;
        }
        DoubleSet[] result = new DoubleSet[partitions];
        for (int i = 0; i < partitions; i++) {
            int start = (i * itemsPer);
            int length = i == partitions - 1 ? size - start : itemsPer;
            double[] copy = new double[length];
            System.arraycopy(data, start, copy, 0, length);
            result[i] = new DoubleSet(copy);
        }
        return result;
    }

    private void maybeGrow() {
        if (size == data.length - 1) {
            data = Arrays.copyOf(data, data.length + (initialCapacity - (initialCapacity / 3)));
        }
    }

    private void clean() {
        if (size <= 1) {
            return;
        }
        Arrays.sort(data, 0, size);
        double last = data[0];
        int dedupFrom = -1;
        for (int i = 1; i < size; i++) {
            double v = data[i];
            if (v == last) {
                dedupFrom = i;
                break;
            }
            last = v;
        }
        if (dedupFrom != -1) {
            if (dedupFrom == size - 1) {
                size--;
                return;
            }
            int removed = 1;
            for (int i = dedupFrom; i < size; i++) {
                double v = data[i];
                if (v == last && i > dedupFrom) {
                    removed++;
                } else {
                    data[i - removed] = v;
                }
                last = v;
            }
            size -= removed;
        }
    }

    public double getAsDouble(int index) {
        ensureClean();
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("" + index);
        }
        return data[index];
    }

    public void addAll(DoubleSet set) {
        if (set == this) {
            return;
        }
        int sz = set.size();
        if (sz == 0) {
            return;
        }
        if (size + sz > data.length) {
            data = Arrays.copyOf(data, size + sz);
        }
        clean = false;
        System.arraycopy(set.data, 0, data, size, sz);
        size += sz;
    }

    private void ensureClean() {
        if (!clean && size > 0) {
            clean();
            clean = true;
        }
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void add(double value) {
        if (value == Double.MIN_VALUE) {
            throw new IllegalArgumentException("Illegal value "
                    + "Double.MIN_VALUE (is used to indicate null results)");
        }
        maybeGrow();
        data[size++] = value;
        if (size > 1 && data[size - 1] >= value) {
            clean = false;
        }
    }

    public int size() {
        ensureClean();
        return size;
    }

    public double least() {
        if (size == 0) {
            return 0;
        }
        ensureClean();
        return data[0];
    }

    public double greatest() {
        if (size == 0) {
            return 0;
        }
        ensureClean();
        return data[size - 1];
    }

    public void forEachDouble(DoubleConsumer dc) {
        ensureClean();
        for (int i = 0; i < size; i++) {
            dc.accept(data[i]);
        }
    }

    public void forEachReversed(DoubleConsumer dc) {
        ensureClean();
        for (int i = size - 1; i >= 0; i--) {
            dc.accept(data[i]);
        }
    }

    public void removeAll(DoubleSet remove) {
        remove.ensureClean();
        ensureClean();
        remove.forEachReversed(val -> {
            int ix = indexOf(val);
            if (ix != -1) {
                System.arraycopy(data, ix + 1, data, ix, size - (ix + 1));
                size--;
            }
        });
    }

    public double range() {
        return greatest() - least();
    }

    public void retainAll(DoubleSet retain) {
        if (greatest() < retain.least() || least() > retain.greatest()) {
            clear();
            return;
        }
        retain.ensureClean();
        ensureClean();
        boolean anyRetained = false;
        for (int i = size - 1; i >= 0; i--) {
            double v = data[i];
            if (!retain.contains(v)) {
                if (!anyRetained) {
                    size = i;
                } else {
                    System.arraycopy(data, i + 1, data, i, size - (i + 1));
                    size--;
                }
            } else {
                anyRetained = true;
            }
        }
    }

    public PrimitiveIterator.OfDouble iterator() {
        return new It();
    }

    class It implements PrimitiveIterator.OfDouble {

        private int cursor = -1;

        @Override
        public double nextDouble() {
            return data[++cursor];
        }

        @Override
        public boolean hasNext() {
            return cursor + 1 < size;
        }
    }

    public double[] toDoubleArray() {
        ensureClean();
        return Arrays.copyOf(data, size);
    }

    public boolean contains(double d) {
        ensureClean();
        return Arrays.binarySearch(data, 0, size, d) >= 0;
    }

    public int indexOf(double d) {
        ensureClean();
        int result = Arrays.binarySearch(data, 0, size, d);
        return result >= 0 ? result : -1;
    }

    @Override
    public int hashCode() {
        ensureClean();
        long result = 5 * (size + 1);
        for (int i = 0; i < size; i++) {
            result += 43 * doubleToLongBits(data[i]);
        }
        return (int) (result | (result << 32));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (o instanceof DoubleSet) {
            DoubleSet ds = (DoubleSet) o;
            if (ds.size != size) {
                return false;
            }
            if (ds.greatest() != greatest()) {
                return false;
            } else {
                ensureClean();
                ds.ensureClean();
                for (int i = 0; i < size; i++) {
                    if (data[i] != ds.data[i]) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Get the value closest to the specified value where the distance, positive
     * or negative, to that value is less than or equal to the passed tolerance;
     * returns Double.MIN_VALUE as null result.
     *
     * @param val A value
     * @return The nearest value to that value
     */
    public double nearestValueTo(double val, double tolerance) {
        double result = nearestValueTo(val);
        if (Math.abs(val - result) > tolerance) {
            return Double.MIN_VALUE;
        }
        return result;
    }

    /**
     * Get the value closest to the specified value; returns Double.MIN_VALUE as
     * null result.
     *
     * @param val A value
     * @return The nearest value to that value
     */
    public double nearestValueTo(double val) {
        if (size == 0) {
            return Double.MIN_VALUE;
        } else if (size == 1) {
            return data[0];
        }
        int ix = nearestIndexTo(val, Bias.NEAREST);
        return data[ix];
    }

    // adapted from com.mastfrog.util.collections.IntListImpl
    public int nearestIndexTo(double value, Bias bias) {
        if (size == 0) {
            return -1;
        } else if (size == 1) {
            double v = data[0];
            switch (bias) {
                case BACKWARD:
                    return value >= v ? 0 : -1;
                case FORWARD:
                    return value <= v ? 0 : -1;
                case NEAREST:
                    return 0;
                case NONE:
                    return value == v ? 0 : -1;
                default:
                    throw new AssertionError(bias);
            }
        }
        ensureClean();
        if (size == 0) {
            return -1;
        } else if (size == 1) {
            double val = data[0];
            switch (bias) {
                case BACKWARD:
                    if (val <= value) {
                        return 0;
                    } else {
                        return -1;
                    }
                case FORWARD:
                case NEAREST:
                    if (val >= value) {
                        return 0;
                    } else {
                        return -1;
                    }
            }
        }
        switch (bias) {
            case NONE:
                int result = indexOf(value);
//                if (result < -1) {
//                    new IllegalStateException("Weird answer for indexOfPresumingSorted with bias none "
//                        + " " + result + " for value " + value + " in " + this).printStackTrace();
//                }
                if (result >= 0) {
                    while (result < size - 1 && data[result + 1] == data[result]) {
                        result++;
                    }
                }
                return result;
            case FORWARD:
            case BACKWARD:
                int res2 = nearestIndexTo(0, size - 1, bias, value);
                if (res2 != -1) {
                    while (res2 < size - 1 && data[res2 + 1] == data[res2]) {
                        res2++;
                    }
                }
                return res2;
            case NEAREST:
                int fwd = nearestIndexTo(0, size - 1, Bias.FORWARD, value);
                int bwd = nearestIndexTo(0, size - 1, Bias.BACKWARD, value);
                if (fwd == -1) {
                    return bwd;
                } else if (bwd == -1) {
                    return fwd;
                } else if (fwd == bwd) {
                    return fwd;
                } else {
                    double fwdDiff = Math.abs(data[fwd] - value);
                    double bwdDiff = Math.abs(data[bwd] - value);
                    if (fwdDiff == bwdDiff) {
                        return fwd;
                    } else if (fwdDiff < bwdDiff) {
                        return fwd;
                    } else {
                        return bwd;
                    }
                }
            default:
                throw new AssertionError(bias);
        }
    }

    private int nearestIndexTo(int start, int end, Bias bias, double value) {
        if (start == end) {
            double currentVal = data[start];
            if (currentVal == value) {
                return start;
            }
            switch (bias) {
                case BACKWARD:
                    if (currentVal <= value) {
                        return start;
                    } else {
                        return -1;
                    }
                case FORWARD:
                    if (currentVal >= value) {
                        return start;
                    } else {
                        return -1;
                    }
            }
        }
        double startVal = data[start];
        if (startVal == value) {
            return start;
        }
        if (startVal > value) {
            switch (bias) {
                case BACKWARD:
                    if (startVal > value) {
                        return -1;
                    }
                    return start - 1;
                case FORWARD:
                    return start;
                default:
                    return -1;
            }
        }
        double endVal = data[end];
        if (endVal == value) {
            return end;
        }
        if (endVal < value) {
            switch (bias) {
                case BACKWARD:
                    return end;
                case FORWARD:
                    int result = end + 1;
                    return result < size ? result : -1;
                default:
                    return -1;
            }
        }
        int mid = start + ((end - start) / 2);
        double midVal = data[mid];
//        while (mid < size-1 && data[mid+1] == midVal) {
//            mid++;
//        }
        if (midVal == value) {
            return mid;
        }
        // If we have an odd number of slots, we can getAsDouble into trouble here:
        if (midVal < value && endVal > value) {
            int newStart = mid + 1;
            int newEnd = end - 1;
            double nextStartValue = data[newStart];
            if (nextStartValue > value && bias == Bias.BACKWARD && (newEnd - newStart <= 1 || midVal < value)) {
                return mid;
            }
            double nextEndValue = data[newEnd];
            if (nextEndValue < value && bias == Bias.FORWARD && newEnd - newStart <= 1) {
                return end;
            }
            return nearestIndexTo(newStart, newEnd, bias, value);
        } else if (midVal > value && startVal < value) {
            int nextEnd = mid - 1;
            int nextStart = start + 1;
            double nextEndValue = data[nextEnd];
            if (nextEndValue < value && bias == Bias.FORWARD && nextEnd - nextStart <= 1) {
                return mid;
            }
            double newStartValue = data[nextStart];
            if (bias == Bias.BACKWARD && newStartValue > value && (startVal < value || nextEnd - nextStart <= 1)) {
                return start;
            }
            return nearestIndexTo(nextStart, nextEnd, bias, value);
        }
        return -1;
    }
}
