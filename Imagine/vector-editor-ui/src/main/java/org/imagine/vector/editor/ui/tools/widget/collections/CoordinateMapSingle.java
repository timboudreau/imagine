/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget.collections;

import com.mastfrog.util.collections.DoubleMap;
import com.mastfrog.util.collections.IntSet;
import java.awt.Rectangle;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import org.imagine.geometry.EqPointDouble;
import org.imagine.utils.Holder;

/**
 *
 * @author Tim Boudreau
 */
public class CoordinateMapSingle<T> implements CoordinateMap<T> {

    private final int minX, minY, maxX, maxY;
    private final DoubleMap<T> map;

    public CoordinateMapSingle(double x, double y, int size) {
        this(x, y, size, size);
    }

    public CoordinateMapSingle(double x, double y, int w, int h) {
        this((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(x) + w,
                (int) Math.floor(y) + h);
    }

    public CoordinateMapSingle(int minX, int minY, int maxX, int maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        map = DoubleMap.create(maxX * maxY);
    }

    @Override
    public Rectangle bounds() {
        return new Rectangle(minX, minY, width(), height());
    }

    @Override
    public T get(double x, double y) {
        if (containsCoordinate(x, y)) {
            double addr = mapAddressFrom(x, y);
            return map.get(addr);
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

    private double mapAddressFrom(double x, double y) {
        double xNorm = x - minX;
        double yNorm = y - minY;
        return ((yNorm) * width()) + (xNorm == 0 ? 0 : xNorm % width());
    }

    private void fromAddress(double addr, double[] into) {
        fromAddress(addr, into, 0);
    }

    private void fromAddress(double addr, double[] into, int offset) {
        int w = width();
        double x = addr == 0 ? 0 : addr % w;
        double y = (addr - x) / w;
        x += minX;
        y += minY;
        into[offset] = x;
        into[offset + 1] = y;
    }

    @Override
    public T nearestValueTo(double x, double y, double tolerance) {
//        System.out.println("\n\nNEAREST TO " + x + ", " + y
//                + " search space " + (x - tolerance) + ", " + (y - tolerance)
//                + " to " + (x + tolerance) + ", " + (y + tolerance));
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
//            System.out.println("Out of range: " + tMinX + "," + tMinY + " - "
//                    + tMaxX + ", " + tMaxY + " within "
//                    + minX + ", " + minY + " - " + maxX + ", " + maxY);
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
//        System.out.println("Values within " + mix + ", " + miy + " to " + max + "," + may);
        double minAddress = mapAddressFrom(mix, miy);
        double maxAddress = mapAddressFrom(max, may);
        double[] scratch = new double[2];
        int result = map.valuesBetween(minAddress, maxAddress, (int index, double value, T object) -> {
            fromAddress(value, scratch);
            if (scratch[0] >= tMinX && scratch[1] >= tMinY && scratch[0] <= tMaxX && scratch[0] <= tMaxY) {
                c.accept(scratch[0], scratch[1], object);
            }
        });
        return result;
    }

    @Override
    public boolean contains(double x, double y) {
        return containsCoordinate(x, y)
                && map.containsKey(mapAddressFrom(x, y));
    }

    @Override
    public void put(double x, double y, T obj, BiFunction<T, T, T> coalesce) {
        double addr = mapAddressFrom(x, y);

        int ix = map.indexOf(addr);
        if (ix < 0) {
            map.put(addr, obj);
        } else {
            T old = map.valueAt(ix);
            T nue = coalesce.apply(old, obj);
            if (nue == null) {
                map.removeIndex(ix);
            } else {
                map.setValueAt(ix, nue);
            }
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
        map.forEach((int index, double value, T object) -> {
            if (pred.test(object)) {
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

    public void visitAll(CoordinateMapVisitor<T> v) {
        double[] scratch = new double[2];
        for (int i = 0; i < map.size(); i++) {
            double key = map.key(i);
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
            double key = map.key(i);
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
}
