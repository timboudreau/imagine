package org.imagine.vector.editor.ui.tools.widget.collections;

import java.awt.Rectangle;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import org.imagine.geometry.EqPointDouble;
import org.imagine.utils.Holder;

/**
 *
 * @author Tim Boudreau
 */
public interface CoordinateMap<T> {

    default Rectangle bounds() {
        int mx = minX();
        int my = minY();
        return new Rectangle(mx, my, maxX() - mx, maxY() - my);
    }

    T get(double x, double y);

    boolean contains(double x, double y);

    boolean containsCoordinate(double tx, double ty);

    void put(double x, double y, T obj, BiFunction<T, T, T> coalesce);

    int minX();

    int minY();

    int maxX();

    int maxY();

    int valuesWithin(double tMinX, double tMinY, double tMaxX, double tMaxY, CoordinateMapVisitor<T> c);

    /**
     * Finds the nearest value within a circular region centered on the passed
     * coordinates of radius <code>tolerance</code>.
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @param radius The radius
     * @return The value closest to the center of a circle anchored at the
     * passed coordinates
     */
    default T nearestValueTo(double x, double y, double radius) {
        EqPointDouble d = new EqPointDouble(x, y);
        Holder<T> h = Holder.create();
        double[] bestDist = new double[]{Double.MAX_VALUE};
        int visited = valuesWithin(x - radius, y - radius, x + radius, y + radius, (tx, ty, val) -> {
            double dist = d.distance(tx, ty);
            if (dist < bestDist[0]) {
                bestDist[0] = dist;
                h.set(val);
            }
        });
        return visited == 0 ? null : h.get();
    }

    int conditionallyRemove(Predicate<T> pred);

    void clear();

    int size();

    void visitAll(CoordinateMapVisitor<T> v);
}
