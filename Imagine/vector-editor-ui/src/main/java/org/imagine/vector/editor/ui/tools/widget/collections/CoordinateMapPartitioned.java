/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget.collections;

import com.mastfrog.function.IntBiFunction;
import com.mastfrog.util.collections.CollectionUtils;
import com.mastfrog.util.collections.IntMap;
import com.mastfrog.util.collections.IntMap.IntMapConsumer;
import com.mastfrog.util.search.Bias;
import java.awt.Rectangle;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import com.mastfrog.geometry.EqPointDouble;
import com.mastfrog.geometry.util.GeometryStrings;
import com.mastfrog.geometry.util.GeometryUtils;
import org.imagine.utils.Holder;

/**
 *
 * @author Tim Boudreau
 */
public class CoordinateMapPartitioned<T> implements CoordinateMap<T> {

    private final int partitionSize;
    final IntMap<CoordinateMap<T>> partitions
            = CollectionUtils.intMap(20);

    private static final long BASE_RANGE = (long) Integer.MAX_VALUE - (long) Integer.MIN_VALUE;
    private IntBiFunction<CoordinateMap<T>> childFactory;

    public CoordinateMapPartitioned(int partitionSize) {
        assert partitionSize > 0 : "Illegal partition size";
        this.partitionSize = partitionSize;
        this.childFactory = (int x, int y) -> {
            return new CoordinateMapSingle<T>(x, y, partitionSize);
        };
    }

    public CoordinateMapPartitioned(int partitionSize, IntBiFunction<CoordinateMap<T>> childFactory) {
        this.partitionSize = partitionSize;
        this.childFactory = childFactory;
    }

    private int addressOf(double x, double y) {
        return addressOf(x, y, partitionSize);
    }

    private static int maxPartitions(int partitionSize) {
        int result = (int) (BASE_RANGE / 8) / partitionSize;
        return result;
    }

    static int addressOf(double x, double y, int partitionSize) {
        int maxPartitions = maxPartitions(partitionSize);
        int maxPerPartition = maxPartitions * partitionSize;
        int cellY = (int) Math.abs(Math.floor(y / partitionSize));
        int cellX = (int) Math.abs(Math.floor(x / partitionSize));
        cellY *= partitionSize;
        if (y < 0 && cellY % partitionSize != 0) {
            cellY++;
        }
        int cellAddress = (cellY * partitionSize) + cellX;

        if (y < 0 && x >= 0) {
            cellAddress += maxPerPartition;
        } else if (y >= 0 && x < 0) {
            cellAddress += maxPerPartition * 2;
        } else if (x < 0 && y < 0) {
            cellAddress += maxPerPartition * 3;
        }
        return cellAddress;
    }

    static <T> T coordsOf(int partitionSize, double x, double y, IntBiFunction<T> c) {
        // XXX also is pointlessly baroque
        boolean xNeg = x < 0;
        boolean yNeg = y < 0;
        int ox = (int) Math.abs(Math.floor(x));
        int oy = (int) Math.abs(Math.floor(y));
        x = Math.abs(x);
        y = Math.abs(y);
        x /= partitionSize;
        y /= partitionSize;
        int ix = (int) Math.floor(x);
        int iy = (int) Math.floor(y);
        if (yNeg && oy % partitionSize != 0) {
            iy++;
        }
        if (xNeg && ox % partitionSize != 0) {
            ix++;
        }
        if (xNeg) {
            ix *= -1;
        }
        if (yNeg) {
            iy *= -1;
        }
        ix *= partitionSize;
        iy *= partitionSize;
        return c.apply(ix, iy);
    }

    CoordinateMap<T> partitionFor(double x, double y, boolean createIfAbsent) {
        int addr = addressOf(x, y);
        CoordinateMap<T> result = partitions.get(addr);
        if (result == null && createIfAbsent) {
            result = coordsOf(partitionSize, x, y, (partX, partY) -> {
                return childFactory.apply(partX, partY);
            });
            assert result.bounds().contains(x, y) : "Bounds of partition does not contain requested point: "
                    + result.bounds() + " for " + x + "," + y;
            CoordinateMap<T> old = partitions.put(addr, result);
//            System.out.println("put new partition " + result + " for address " + addr
//                    + " for " + x + "," + y + " old " + old);
            assert old == null : "Clobbered " + old;
        } else if (result != null) {
            assert result.bounds().contains(x, y) : "Bounds of partition "
                    + addr + " does not contain requested point: "
                    + result.bounds() + " for " + x + "," + y
                    + " minX " + result.minX() + " minY " + result.minY();
        }
        return result;
    }

    @Override
    public void clear() {
        partitions.clear();
    }

    @Override
    public Rectangle bounds() {
        if (partitions.isEmpty()) {
            return new Rectangle();
        }
        CoordinateMap<T> first = partitions.leastValue();
        if (partitions.size() == 1) {
            return first.bounds();
        }
        CoordinateMap<T> last = partitions.greatestValue();
        int minX = Math.min(first.minX(), last.minX());
        int minY = Math.min(first.minY(), last.minY());
        int maxX = Math.max(first.maxX(), last.maxX());
        int maxY = Math.max(first.maxY(), last.maxY());
        return new Rectangle(minX, minY, maxX - minX, maxY - maxY);
    }

    @Override
    public T get(double x, double y) {
        CoordinateMap<T> part = partitionFor(x, y, false);
        return part == null ? null : part.get(x, y);
    }

    @Override
    public boolean contains(double x, double y) {
        CoordinateMap<T> part = partitionFor(x, y, false);
        return part != null && part.contains(x, y);
    }

    @Override
    public boolean containsCoordinate(double tx, double ty) {
        CoordinateMap<T> part = partitionFor(tx, ty, false);
        return part != null;
    }

    @Override
    public void put(double x, double y, T obj, BiFunction<T, T, T> coalesce) {
        CoordinateMap<T> part = partitionFor(x, y, true);
        part.put(x, y, obj, coalesce);
    }

    @Override
    public int minX() {
        if (partitions.isEmpty()) {
            return 0;
        }
        CoordinateMap<T> first = partitions.leastValue();
        return first.minX();
    }

    @Override
    public int minY() {
        if (partitions.isEmpty()) {
            return 0;
        }
        CoordinateMap<T> first = partitions.leastValue();
        return first.minY();
    }

    @Override
    public int maxX() {
        if (partitions.isEmpty()) {
            return 0;
        }
        int result = Integer.MIN_VALUE;
        for (int i = 0; i < partitions.size(); i++) {
            result = Math.max(result, partitions.valueAt(i).maxX());
        }
        return result;
    }

    @Override
    public int maxY() {
        if (partitions.isEmpty()) {
            return 0;
        }
        CoordinateMap<T> last = partitions.greatestValue();
        return last.maxY();
    }

    @Override
    public int valuesWithin(double tMinX, double tMinY, double tMaxX, double tMaxY, CoordinateMapVisitor<T> c) {
        int minY = minY();
        int minX = minX();
        int mix = Math.max((int) Math.floor(tMinX), minX);
        int miy = Math.max((int) Math.floor(tMinY), minY);
        int max = Math.min((int) Math.ceil(tMaxX), maxX());
        int may = Math.min((int) Math.ceil(tMaxY), maxY());
        int minAddr = addressOf(mix, miy);
        int maxAddr = addressOf(max, may);
        int result = 0;
        int minValue = partitions.nearestKey(minAddr, Bias.FORWARD);
        int maxValue = partitions.nearestKey(maxAddr, Bias.BACKWARD);

        int minIndex = partitions.indexOf(minValue);
        int maxIndex = partitions.indexOf(maxValue);

//        debugLog(() -> "Scan partitions " + minIndex + " thru " + maxIndex);
        for (int i = Math.min(minIndex, maxIndex); i <= Math.max(minIndex, maxIndex); i++) {
            CoordinateMap<T> partition = partitions.valueAt(i);
//            debugLog(() -> " partition " + minIndex + " bounds " + GeometryUtils.toString(partition.bounds()));
            result += partition.valuesWithin(tMinX, tMinY, tMaxX, tMaxY, c);
        }
        return result;
    }

    private void debugLog(Supplier<String> s) {
        if (CoordinateMap.isDebug()) {
            System.out.println("CMP: " + s.get());
        }
    }

    @Override
    public T nearestValueTo(double x, double y, double tolerance) {
        EqPointDouble d = new EqPointDouble(x, y);
        Holder<T> h = Holder.create();
        double[] bestDist = new double[]{Double.MAX_VALUE};
        int visited = valuesWithin(x - tolerance, y - tolerance, x + tolerance, y + tolerance, (tx, ty, val) -> {
            double dist = d.distance(tx, ty);
            boolean isBest = dist < bestDist[0];
            if (isBest) {
                bestDist[0] = dist;
                h.set(val);
            }
//            debugLog(() -> "nearestKey " + val + " best? " + isBest + " dist " + dist);
        });
//        debugLog(() -> "Visited " + visited);
        return visited == 0 ? null : h.get();
    }

    @Override
    public int conditionallyRemove(Predicate<T> pred) {
        int max = partitions.size();
        int result = 0;
        for (int i = 0; i < max; i++) {
            CoordinateMap<T> part = partitions.valueAt(i);
            result += part.conditionallyRemove(pred);
        }
        return result;
    }

    @Override
    public T moveData(double oldX, double oldY, final double newX, final double newY, Mover<T> coalescer) {
        int srcAddress = addressOf(oldX, oldY);
        int destAddress = addressOf(newX, newY);
        CoordinateMap<T> src = partitions.get(srcAddress);
        if (src == null) {
            throw new IllegalArgumentException("No partition for "
                    + oldX + "," + oldY + " at address " + srcAddress + " in "
                    + partitions);
        }
        if (srcAddress == destAddress) {
//            System.out.println("Move by same ");
            return src.moveData(oldX, oldY, newX, newY, coalescer);
        }
        T old = src.get(oldX, oldY);
        if (old == null) {
            throw new IllegalArgumentException("Partition does not contain "
                    + oldX + ", " + oldY + ": " + src + ".\nNearest is "
                    + src.nearestValueTo(oldX, oldY, 30) + ".\nPartition bounds "
                    + src.bounds() + ".\nContents: " + src);
        }
        CoordinateMap<T> dest = partitions.get(destAddress);
        if (dest == null) {
            CoordinateMap<T> newDest = partitionFor(newX, newY, true);
            return coalescer.coalesce(old, null, (T origReplacement, T nue) -> {
                if (nue == null) {
                    throw new IllegalArgumentException("New item cannot be null");
                }
                newDest.put(newX, newY, nue, (T ignored1, T ignored2) -> ignored2);
                if (origReplacement != old) {
                    if (origReplacement != null) {
                        src.put(oldX, oldY, origReplacement, (T ignored1, T ignored2) -> ignored2);
                    } else {
                        src.remove(oldX, oldY);
                    }
                }
            });
        } else {
            T orig = dest.get(newX, newY);
            if (orig != null) {
                return coalescer.coalesce(old, orig, (newOld, newNew) -> {
                    assert newNew != null : "No new value";
                    if (newOld == null) {
                        src.remove(oldX, oldY);
                    } else if (newOld != old) {
                        src.put(oldX, oldY, newOld, (T ign1, T ign2) -> ign2);
                    }
                    dest.put(newX, newY, newNew, (T ignored1, T ignored2) -> ignored2);
                });
            } else {
                return coalescer.coalesce(old, null, (newOld, newNew) -> {
                    assert newNew != null : "No new value";
                    if (newOld == null) {
                        src.remove(oldX, oldY);
                    } else {
                        src.put(oldX, oldY, newOld, (T ign1, T ign2) -> ign2);
                    }
                    dest.put(newX, newY, newNew, (T ignored1, T ignored2) -> ignored2);
                });
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CoordinateMapPartitioned with "
        ).append(partitions.size()).append(" Partitions:");
        for (int i = 0; i < partitions.size(); i++) {
            sb.append('\n');
            sb.append(i).append(". ")
                    .append(partitions.key(i))
                    .append("k ")
                    .append(GeometryStrings.toString(partitions.valueAt(i).bounds()))
                    .append(partitions.valueAt(i));
        }
        return sb.toString();
    }

    @Override
    public boolean remove(double x, double y) {
        int addr = addressOf(x, y);
        CoordinateMap<T> pt = partitions.get(addr);
        if (pt == null) {
            return false;
        }
        return pt.remove(x, y);
    }

    @Override
    public void visitAll(CoordinateMapVisitor<T> v) {
        partitions.forEach((IntMapConsumer<CoordinateMap<T>>) (int key, CoordinateMap<T> value) -> {
            value.visitAll(v);
        });
    }

    @Override
    public int size() {
        int max = partitions.size();
        int result = 0;
        for (int i = 0; i < max; i++) {
            CoordinateMap<T> part = partitions.valueAt(i);
            result += part.size();
        }
        return result;
    }

    @Override
    public CoordinateMapModifier<T> modifier() {
        return new DelegatingMod(null);
    }

    @Override
    public CoordinateMapModifier<T> modifier(Mover<T> mover) {
        return new DelegatingMod(mover);
    }

    class DelegatingMod implements CoordinateMapModifier<T> {

        private final Map<CoordinateMap<T>, CoordinateMapModifier<T>> delegates = new IdentityHashMap<>();

        private final Mover<T> mover;

        public DelegatingMod(Mover<T> mover) {
            this.mover = mover;
        }

        @Override
        public CoordinateMapModifier<T> add(double x, double y, T value) {
            CoordinateMap<T> map = partitionFor(x, y, true);
            modifierFor(map).add(x, y, value);
            return this;
        }

        @Override
        public CoordinateMapModifier<T> remove(double x, double y) {
            CoordinateMap<T> map = partitionFor(x, y, false);
            if (map == null) {
                return this;
            }
            modifierFor(map).remove(x, y);
            return this;
        }

        @Override
        public CoordinateMapModifier<T> move(double fromX, double fromY, double toX, double toY) {
            CoordinateMap<T> fromMap = partitionFor(fromX, fromY, true);
            CoordinateMap<T> toMap = partitionFor(toX, toY, true);
            if (fromMap == toMap) {
                modifierFor(fromMap).move(fromX, fromY, toX, toY);
                return this;
            }
            T oldOrig = fromMap.get(fromX, fromY);
            if (oldOrig == null) {
                throw new IllegalArgumentException("Not present: " + fromX + ", " + fromY);
            }
            T oldNue = toMap.get(fromX, fromY);
            CoordinateMapModifier<T> fromModifier = modifierFor(fromMap);
            CoordinateMapModifier<T> toModifier = modifierFor(toMap);
            mover.coalesce(oldNue, oldNue, (newOld, newNew) -> {
                if (newOld == null) {
                    fromModifier.remove(fromX, fromY);
                } else if (newOld != oldOrig) {
                    fromModifier.set(fromX, fromY, oldNue);
                }
                if (newNew != null) {
                    if (toMap.contains(toX, toY)) {
                        toModifier.set(toX, toY, newNew);
                    } else {
                        toModifier.add(toX, toY, newNew);
                    }
                }
            });
            return this;
        }

        @Override
        public CoordinateMapModifier<T> set(double fromX, double fromY, T value) {
            CoordinateMap<T> map = partitionFor(fromX, fromY, true);
            modifierFor(map).set(fromX, fromY, value);
            return this;
        }

        private CoordinateMapModifier<T> modifierFor(CoordinateMap<T> map) {
            CoordinateMapModifier<T> mod = delegates.get(map);
            if (mod == null) {
                mod = mover == null ? map.modifier() : map.modifier(mover);
                delegates.put(map, mod);
            }
            return mod;
        }

        @Override
        public void commit() {
            for (Map.Entry<CoordinateMap<T>, CoordinateMapModifier<T>> e : delegates.entrySet()) {
                e.getValue().commit();
            }
        }

    }
}
