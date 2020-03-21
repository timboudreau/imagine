/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget.collections;

import com.mastfrog.util.collections.CollectionUtils;
import com.mastfrog.util.collections.IntMap;
import com.mastfrog.util.collections.IntMap.IntMapConsumer;
import java.awt.Rectangle;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import org.imagine.geometry.EqPointDouble;
import org.imagine.geometry.util.GeometryUtils;
import org.imagine.utils.Holder;

/**
 *
 * @author Tim Boudreau
 */
public class CoordinateMapPartitioned<T> implements CoordinateMap<T> {

    private final int partitionSize;
    private final IntMap<CoordinateMap<T>> partitions
            = CollectionUtils.intMap(20);

    public CoordinateMapPartitioned(int partitionSize) {
        this.partitionSize = partitionSize;
    }

    private int addressOf(double x, double y) {
        int ix = (int) Math.floor(x);
        int iy = (int) Math.floor(y);
        return (iy / partitionSize) + (ix % partitionSize);
    }

    private CoordinateMap<T> partitionFor(double x, double y, boolean createIfAbsent) {
        int addr = addressOf(x, y);
        CoordinateMap<T> result = partitions.get(addr);
        if (result == null && createIfAbsent) {
            int xx = (int) Math.floor(x);
            xx = (xx / partitionSize) * partitionSize;
            int partX = xx;

            int yy = (int) Math.floor(y);
            yy = (yy / partitionSize) * partitionSize;
            int partY = yy;

            result = new CoordinateMapSingle<>(partX, partY, partitionSize);
//            System.out.println("Create a partition " + GeometryUtils.toString(result.bounds())
//                    + " for address " + addr + " starting at " + partX + ", " + partY);
            partitions.put(addr, result);
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
//        System.out.println("search within " + minAddr + " to " + maxAddr + " for "
//                + tMinX + ", " + tMinY + " - " + tMaxX + ", " + tMaxY
//                + " winnowed to " + mix + ", " + miy + " - " + max + ", " + may);

        int result = 0;
        for (int i = minAddr; i <= maxAddr; i++) {
            CoordinateMap<T> partition = partitions.get(i);
            if (partition != null) {
//                System.out.println("Try partition " + i + ": " + partition);
                result += partition.valuesWithin(tMinX, tMinY, tMaxX, tMaxY, c);
//            } else {
//                System.out.println("No partition for " + i);
            }
        }
        return result;
    }

    @Override
    public T nearestValueTo(double x, double y, double tolerance) {
//        System.out.println("\n\npart nearest to " + x + ", " + y + " within " + tolerance);
        EqPointDouble d = new EqPointDouble(x, y);
        Holder<T> h = Holder.create();
        double[] bestDist = new double[]{Double.MAX_VALUE};
        int visited = valuesWithin(x - tolerance, y - tolerance, x + tolerance, y + tolerance, (tx, ty, val) -> {
            double dist = d.distance(tx, ty);
            if (dist < bestDist[0]) {
//                System.out.println("Got BEST " + dist + " " + val);
                bestDist[0] = dist;
                h.set(val);
            }
        });
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
    public void visitAll(CoordinateMapVisitor<T> v) {
        partitions.forEach((IntMapConsumer<CoordinateMap<T>>) (int key, CoordinateMap<T> value) -> {
            value.visitAll(v);
        });
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CoordinateMapPartitioned with "
        ).append(partitions.size()).append(" Partitions:");
        for (int i = 0; i < partitions.size(); i++) {
            sb.append('\n');
            sb.append(i).append(". ")
                    .append(GeometryUtils.toString(partitions.valueAt(i).bounds()))
                    .append(partitions.valueAt(i));
        }
        return sb.toString();
    }
}
