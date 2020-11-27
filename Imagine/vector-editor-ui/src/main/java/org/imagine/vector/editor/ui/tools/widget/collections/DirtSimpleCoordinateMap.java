/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget.collections;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import com.mastfrog.geometry.util.GeometryStrings;
import com.mastfrog.geometry.util.GeometryUtils;

/**
 *
 * @author Tim Boudreau
 */
public class DirtSimpleCoordinateMap<T> implements CoordinateMap<T> {

    private final Map<Rectangle, T> buckets = new HashMap<>(5);
    private final Rectangle outerBounds;
    private static final int NUM_BUCKETS = 4;
    private final int bucketSize;

    public DirtSimpleCoordinateMap(int x, int y, int size) {
        this.outerBounds = new Rectangle(x, y, size, size);
        if (size % 4 != 0) {
            throw new IllegalArgumentException("Not divisible by 4: " + size);
        }
        bucketSize = size / NUM_BUCKETS;
        System.out.println("size " + size + " bucket size " + bucketSize);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DirtSimpleCoordinateMap:");
        for (Map.Entry<Rectangle, T> e : buckets.entrySet()) {
            sb.append("\n  ");
            sb.append(GeometryStrings.toString(e.getKey()));
            sb.append(": ").append(e.getValue());
        }
        return sb.toString();
    }

    private Rectangle rectangleFor(double x, double y) {
        assert outerBounds.contains(x, y) : "Coordinate out of range: "
                + x + ", " + y + " for " + outerBounds;
        Rectangle result = new Rectangle();
        result.setFrameFromDiagonal(x, y, x + bucketSize, y + bucketSize);
        int offX = result.x % bucketSize;
        int offY = result.y % bucketSize;
        if (x < 0 && offX != 0) {
            offX += bucketSize;
        }
        if (y < 0 && offY != 0) {
            offY += bucketSize;
        }
        result.x -= offX;
        result.y -= offY;
        assert result.contains(x, y) : result + " does not contain " + x + ", " + y;
        return result;
    }

    @Override
    public T get(double x, double y) {
        for (Map.Entry<Rectangle, T> e : buckets.entrySet()) {
            if (e.getKey().contains(x, y)) {
                return e.getValue();
            }
        }
        return null;
    }

    @Override
    public boolean contains(double x, double y) {
        return get(x, y) != null;
    }

    @Override
    public boolean containsCoordinate(double tx, double ty) {
        return outerBounds.contains(tx, ty);
    }

    @Override
    public void put(double x, double y, T obj, BiFunction<T, T, T> coalesce) {
        Rectangle r = rectangleFor(x, y);
        T old = buckets.get(r);
        if (old != null) {
            T nue = coalesce.apply(old, obj);
            buckets.put(r, nue);
        } else {
            buckets.put(r, obj);
        }
        System.out.println("RECTS " + size() + " adding " + r);
    }

    @Override
    public int minX() {
        return outerBounds.x;
    }

    @Override
    public int minY() {
        return outerBounds.y;
    }

    @Override
    public int maxX() {
        return outerBounds.x + outerBounds.width;
    }

    @Override
    public int maxY() {
        return outerBounds.y + outerBounds.height;
    }

    @Override
    public int valuesWithin(double tMinX, double tMinY, double tMaxX, double tMaxY, CoordinateMapVisitor<T> c) {
        Rectangle2D.Double r = new Rectangle2D.Double();
        r.setFrameFromDiagonal(tMinX, tMinY, tMaxX, tMaxY);
        int ct = 0;
        for (Map.Entry<Rectangle, T> e : buckets.entrySet()) {
            if (e.getKey().intersects(r)) {
                c.accept(e.getKey().x, e.getKey().y, e.getValue());
                ct++;
            }
        }
        return ct;
    }

    @Override
    public T moveData(double oldX, double oldY, double newX, double newY,
            Mover<T> coalescer) {
        Rectangle r = rectangleFor(oldX, oldY);
        Rectangle n = rectangleFor(newX, newY);
        T obj = buckets.get(r);
        T other = buckets.get(n);
        if (other != null) {
            return coalescer.coalesce(obj, other, (newOld, newNew) -> {
                if (newOld == null) {
                    buckets.remove(r);
                } else if (newOld != obj) {
                    buckets.put(r, newOld);
                }
//                System.out.println("aput " + newNew);
                buckets.put(n, newNew);
            });
        } else {
            return coalescer.coalesce(obj, null, (newOld, newNew) -> {
                if (newOld != obj) {
                    buckets.put(r, newOld);
                }
//                System.out.println("Put to " + n + ": " + newNew);
                buckets.put(n, newNew);
            });
        }
    }

    @Override
    public int conditionallyRemove(Predicate<T> pred) {
        Set<Rectangle> toRemove = new HashSet<>(buckets.size());
        for (Map.Entry<Rectangle, T> e : buckets.entrySet()) {
            if (pred.test(e.getValue())) {
                toRemove.add(e.getKey());
            }
        }
        for (Rectangle r : toRemove) {
            buckets.remove(r);
        }
        return toRemove.size();
    }

    @Override
    public void clear() {
        buckets.clear();
    }

    @Override
    public int size() {
        return buckets.size();
    }

    @Override
    public void visitAll(CoordinateMapVisitor<T> v) {
        for (Map.Entry<Rectangle, T> e : buckets.entrySet()) {
            v.accept(e.getKey().x, e.getKey().y, e.getValue());
        }
    }

    @Override
    public boolean remove(double x, double y) {
        Rectangle r = rectangleFor(x, y);
        return buckets.remove(r) != null;
    }

    @Override
    public CoordinateMapModifier<T> modifier() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public CoordinateMapModifier<T> modifier(Mover<T> mover) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
