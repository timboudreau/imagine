/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget.collections;

import com.mastfrog.util.collections.CollectionUtils;
import com.mastfrog.util.collections.IntMap;
import com.mastfrog.util.collections.IntMap.EntryMover;
import com.mastfrog.util.collections.IntMapModifier;
import com.mastfrog.util.collections.IntSet;
import static com.mastfrog.util.preconditions.Checks.notNull;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.imagine.geometry.EqPointDouble;
import org.imagine.geometry.util.GeometryUtils;
import org.imagine.utils.Holder;

/**
 *
 * @author Tim Boudreau
 */
public class CoordinateMapSingle<T> implements CoordinateMap<T> {

    private final int minX, minY, maxX, maxY;
    final IntMap<T> map;
    private static final int MULTIPLIER = 4;

    public CoordinateMapSingle(int x, int y, int size) {
        this(x, y, x + size, y + size);
    }

    public CoordinateMapSingle(int minX, int minY, int maxX, int maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        assert maxX > minX : maxX + "," + minY;
        assert maxY > minY : maxY + "," + minY;
        map = CollectionUtils.intMap((maxX - minX) * (maxY - minY) * MULTIPLIER);
    }

    @Override
    public Rectangle bounds() {
        return new Rectangle(minX, minY, width(), height());
    }

    @Override
    public T get(double x, double y) {
        if (containsCoordinate(x, y)) {
            int addr = mapAddressFrom(x, y);
            T result = map.get(addr);
            return result;
        }
        return null;
    }

    @Override
    public boolean containsCoordinate(double tx, double ty) {
        return tx >= minX && tx < maxX && ty >= minY && ty < maxY;
    }

    int width() {
        return maxX - minX;
    }

    int height() {
        return maxY - minY;
    }

    private int mapAddressFrom(double x, double y) {
        return toAddress(x, y, width(), minX, minY);
    }

    static int toAddress(double x, double y, int w, int minX, int minY) {
        x -= minX;
        y -= minY;
        x *= MULTIPLIER;
        y *= MULTIPLIER;

        int ix = (int) Math.floor(x % (w * MULTIPLIER));
        int iy = (int) Math.floor(y * (w * MULTIPLIER));

        return ix + iy;
    }

    static void fromAddress(int addr, double[] into, int offset, int w,
            double minX, double minY) {
        int iy = addr / (w * MULTIPLIER);
        int ix = addr % (w * MULTIPLIER);

        double x = (double) ix / MULTIPLIER;
        double y = (double) iy / MULTIPLIER;
        x += minX;
        y += minY;

        into[offset] = x;
        into[offset + 1] = y;
    }

    private void fromAddress(int addr, double[] into) {
        fromAddress(addr, into, 0);
    }

    private void fromAddress(int addr, double[] into, int offset) {
        fromAddress(addr, into, offset, width(), minX, minY);
    }

    static void fromAddress(int addr, double[] into, int w, double minX, double minY) {
        fromAddress(addr, into, 0, w, minX, minY);
    }

    @Override
    public T nearestValueTo(double x, double y, double tolerance) {
        EqPointDouble d = new EqPointDouble(x, y);
        Holder<T> h = Holder.create();
        double[] bestDist = new double[]{Double.MAX_VALUE};
        int visited = valuesWithin(x - tolerance, y - tolerance, x + tolerance, y + tolerance, (tx, ty, val) -> {
            double dist = d.distance(tx, ty);
            if (dist <= tolerance && dist < bestDist[0]) {
                bestDist[0] = dist;
                h.set(val);
            }
        });
        return visited == 0 ? null : h.get();
    }

    @Override
    public int valuesWithin(double tMinX, double tMinY, double tMaxX, double tMaxY, CoordinateMapVisitor<T> c) {
        if (tMinX >= maxX || tMinY >= maxY || tMaxX < minX || tMaxY < minY) {
            return 0;
        }
        // this may scan a LOT of coordinates that are out of range,
        // because we will iterate the entire row in the map due to our
        // addressing scheme;  hopefully partitioning will keep the number
        // of false matches reasonable
        double mix = Math.max(minX, tMinX);
        double miy = Math.max(minY, tMinY);
        double max = Math.min(maxX, tMaxX);
        double may = Math.min(maxY, tMaxY);
        int minAddress = mapAddressFrom(mix, miy);
        int maxAddress = mapAddressFrom(max, may);
        double[] scratch = new double[2];
        debugLog(() -> {
            fromAddress(minAddress, scratch);
            double[] d2 = new double[2];
            fromAddress(maxAddress, d2);
            return "\nScan address range " + minAddress + " to " + maxAddress
                    + " bound " + GeometryUtils.toString(
                            fromDiagonal(scratch[0], scratch[1], d2[0], d2[1]))
                    + " ending at " + GeometryUtils.toString(d2[0], d2[1])
                    + " passed bounds " + GeometryUtils.toString(
                            fromDiagonal(tMinX, tMinY, tMaxX, tMaxY));
        });
        int result = map.valuesBetween(minAddress, maxAddress, (int value, T object) -> {
            fromAddress(value, scratch);
            if (scratch[0] >= mix && scratch[1] >= miy && scratch[0] <= max && scratch[1] <= may) {
                c.accept(scratch[0], scratch[1], object);
//                debugLog(() -> "  ACCEPT " + scratch[0] + ", " + scratch[1] + " within "
//                        + fromDiagonal(mix, miy, max, may).contains(scratch[0], scratch[1])
//                        + " for " + GeometryUtils.toString(fromDiagonal(mix, miy, max, may))
//                        + " addr " + value);
//            } else {
//                debugLog(() -> " skip " + scratch[0] + ", " + scratch[1] + " not within "
//                        + mix + ", " + miy + " to " + max + ", " + may);
            }
        });
        debugLog(() -> "ValuesWith " + mix + ", " + miy + " to " + max + ", " + may + " in " + bounds()
                + " tested " + result + " entries in " + map.keySet());
        return result;
    }

    static Rectangle2D.Double fromDiagonal(double x1, double x2, double y1, double y2) {
        Rectangle2D.Double result = new Rectangle2D.Double();
        result.setFrameFromDiagonal(x1, y1, x2, y2);
        return result;
    }

    @Override
    public boolean contains(double x, double y) {
        assert noDuplicates();
        if (containsCoordinate(x, y)) {
            int addr = mapAddressFrom(x, y);
            boolean result = map.containsKey(addr);
            return result;
        }
        return false;
    }

    private boolean noDuplicates() {
        int last = Integer.MIN_VALUE;
        for (int i = 0; i < map.size(); i++) {
            int val = map.key(i);
            if (val == last) {
                throw new AssertionError("Duplicate keys at " + i + ": " + val);
            }
        }
        return true;
    }

    @Override
    public void put(double x, double y, T obj, BiFunction<T, T, T> coalesce) {
        int addr = mapAddressFrom(x, y);
        int ix = map.indexOf(addr);
        if (ix < 0) {
            map.put(addr, obj);
            assert noDuplicates();
        } else {
            T old = map.valueAt(ix);
            T nue = coalesce.apply(old, obj);
            if (nue == null) {
                map.removeIndex(ix);
            } else {
                map.setValueAt(ix, nue);
            }
            assert noDuplicates();
        }
    }

    @Override
    public int minX() {
        return minX;
    }

    @Override
    public int minY() {
        return minY;
    }

    @Override
    public int maxX() {
        return maxX;
    }

    @Override
    public int maxY() {
        return maxY;
    }

    @Override
    public int conditionallyRemove(Predicate<T> pred) {
        IntSet indicesToRemove = IntSet.create(map.size());
        map.forEachIndexed((int index, int value, T object) -> {
            if (object == null) {
                indicesToRemove.add(index);
            } else if (pred.test(object)) {
                indicesToRemove.add(index);
            }
        });
        map.removeIndices(indicesToRemove);
        return indicesToRemove.size();
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public void visitAll(CoordinateMapVisitor<T> v) {
        double[] scratch = new double[2];
        for (int i = 0; i < map.size(); i++) {
            int key = map.key(i);
            fromAddress(key, scratch);
            T val = map.valueAt(i);
            v.accept(scratch[0], scratch[1], val);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        double[] scratch = new double[2];
        for (int i = 0; i < map.size(); i++) {
            int key = map.key(i);
            fromAddress(key, scratch);
            T val = map.valueAt(i);
            sb.append('<').append(scratch[0]).append(", ").append(scratch[1])
                    .append(": ").append(val).append('>');
            if (i != map.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    @Override
    public boolean remove(double x, double y) {
        int ix = map.indexOf(mapAddressFrom(x, y));
        if (ix >= 0) {
            map.removeIndex(ix);
            return true;
        }
        return false;
    }

    @Override
    public T moveData(double oldX, double oldY, double newX, double newY, Mover<T> mover) {
        if (oldX == newX && oldY == newY) {
//            System.out.println("single no change");
            return get(oldX, oldY);
        }
        assert containsCoordinate(oldX, oldY) : "Lower bound " + oldX + "," + oldY + " not present";
        assert containsCoordinate(newX, newY) : "Upper bound " + newX + "," + newY + " not present";
        int oldAddress = mapAddressFrom(oldX, oldY);
        int newAddress = mapAddressFrom(newX, newY);
        assert oldAddress != newAddress;
        int srcIndex = map.indexOf(oldAddress);
        int destIndex = map.indexOf(newAddress);
        assert srcIndex != destIndex;

        if (srcIndex < 0) {
            T nearest = nearestValueTo(oldX, oldY, 30);
            throw new IllegalArgumentException("Nothing to move at " + oldX + ", "
                    + oldY + " address " + oldAddress + ".\nNearest: " + nearest
                    + "\nPartition bounds " + bounds()
                    + "\nContents: " + this
            );
        }
        T orig = map.valueAt(srcIndex);
        if (destIndex >= 0) {
            T existing = map.valueAt(destIndex);
            return mover.coalesce(orig, existing, (newOrig, newValue) -> {
                if (newOrig != null && newOrig != orig) {
                    map.setValueAt(srcIndex, newOrig);
                    map.setValueAt(destIndex, newValue);
                } else if (newOrig == orig) {
                    map.setValueAt(destIndex, newValue);
                } else if (newOrig == null) {
                    // do the remove second, since it can change offsets into
                    // the map's array
                    map.setValueAt(destIndex, newValue);
                    map.remove(srcIndex);
                }
            });
        } else {
            mover.coalesce(orig, null, (newOrig, newDest) -> {
                assert newDest != null;
                if (newOrig != null) {
                    if (newOrig != orig) {
                        map.setValueAt(srcIndex, newOrig);
                    }
                    map.put(newAddress, newDest);
                } else {
                    map.removeIndex(srcIndex);
                    map.put(newAddress, newDest);
                }
            });
        }
        return orig;
    }

    public CoordinateMapModifier<T> modifier() {
        return new Mod();
    }

    public CoordinateMapModifier<T> modifier(Mover<T> mover) {
        return new Mod(mover);
    }

    private static void debugLog(Supplier<String> s) {
        if (CoordinateMap.isDebug()) {
            System.out.println("CMS: " + s.get());
        }
    }

    class Mod implements CoordinateMapModifier<T> {

        private final IntMapModifier<T> delegate;

        Mod(Mover<T> mover) {
            delegate = IntMapModifier.create(map, new Em(mover));
        }

        Mod() {
            delegate = IntMapModifier.create(map);
        }

        @Override
        public CoordinateMapModifier<T> add(double x, double y, T value) {
            notNull("value", value);
            delegate.add(mapAddressFrom(x, y), value);
            return this;
        }

        @Override
        public CoordinateMapModifier<T> remove(double x, double y) {
            delegate.remove(mapAddressFrom(x, y));
            return this;
        }

        @Override
        public CoordinateMapModifier<T> move(double fromX, double fromY, double toX, double toY) {
            delegate.move(mapAddressFrom(fromX, fromY), mapAddressFrom(toX, toY));
            return this;
        }

        @Override
        public void commit() {
            delegate.commit();
        }

        @Override
        public CoordinateMapModifier<T> set(double fromX, double fromY, T value) {
            notNull("value", value);
            delegate.set(mapAddressFrom(fromX, fromY), value);
            return this;
        }
    }

    class Em implements EntryMover<T> {

        private final Mover<T> delegate;

        public Em(Mover<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public T onMove(int oldKey, T oldValue, int newKey, T newValue, BiConsumer<T, T> oldNewReceiver) {
            return delegate.coalesce(oldValue, newValue, oldNewReceiver);
        }

    }
}
