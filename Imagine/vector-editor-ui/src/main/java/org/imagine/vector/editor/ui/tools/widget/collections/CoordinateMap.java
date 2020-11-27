package org.imagine.vector.editor.ui.tools.widget.collections;

import com.mastfrog.function.IntBiFunction;
import java.awt.Rectangle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import com.mastfrog.geometry.EqPointDouble;
import org.imagine.utils.Holder;

/**
 * Maps objects into a floating-point, two dimensional coordinate space
 * efficiently; there is a straightforward implementation over IntMap, and a
 * partitioned implementation that uses that under the hood and allows fast
 * queries limited to minimal rectangular coordinate spaces, and doesn't require
 * the absolute bounds to be predefined. Where this pays off is in reducing the
 * cost of deletions and moving items, which require shifting array contents far
 * less by using more smaller arrays.
 * <p>
 * So basically:
 * <ul>
 * <li>You put a point and an object into a CoordinateMap</li>
 * <li>The partitioned map looks up the integer coordinate of the partition it
 * lives in (it partitions the space between Integer.MIN_VALUE and
 * Integer.MAX_VALUE into ranges based on the partition size, such that any
 * point lives in exactly one rectangular region which maps to exactly one
 * integer address loosely of the form y * partitionSize + x, with caveats for
 * negative coordinates)</li>
 * <li><i>That</i> child partition assigns the point to an integer bucket using
 * a similar scheme, but mapping buckets to integer coordinates</li>
 * <li>Under the hood, CoordinateMapSingle uses the features of
 * <code>IntMap</code> to minimize memory footprint and provide high-performance
 * binary search, and <code>IntMapModifier</code> to batch and minimize data
 * copies when the underlying data needs to change.
 * </li>
 * <li>When you look up a point or set of points within some bounds, you get may
 * get back more points than you expect; the Refiner you pass then winnows out
 * those that are non-matches.</li>
 * </ul>
 * The result of all this madness is a 1000x improvement in performance when
 * complex shapes are moved around, and taking displaying control points for a
 * shape with thousands of them from the realm of the impossible to the very
 * doable.
 *
 * @author Tim Boudreau
 */
public interface CoordinateMap<T> {

    static <T> CoordinateMap<T> create(int partitionSize) {
        return new CoordinateMapPartitioned<>(partitionSize);
    }

    static <T> CoordinateMap<T> create(int partitionSize, IntBiFunction<CoordinateMap<T>> childFactory) {
        return new CoordinateMapPartitioned<>(partitionSize, childFactory);
    }

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

    int valuesWithin(double tMinX, double tMinY, double tMaxX, double tMaxY,
            CoordinateMapVisitor<T> c);

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

    default T nearestValueTo(double x, double y, double radius, Refiner<T> refiner) {
        EqPointDouble d = new EqPointDouble(x, y);
        Holder<T> h = Holder.create();
        double[] bestDist = new double[]{Double.MAX_VALUE};
        double minX = x - radius;
        double minY = y - radius;
        double maxX = x + radius;
        double maxY = y + radius;
        int visited = valuesWithin(minX, maxX, minY, maxY, (tx, ty, val) -> {
            double dist = d.distance(tx, ty);
            if (dist < bestDist[0]) {
                T obj = refiner.refine(minX, maxX, minY, maxY, val);
                if (obj != null) {
                    bestDist[0] = dist;
                    h.set(val);
                }
            }
        });
        return visited == 0 ? null : h.get();
    }

    T moveData(double oldX, double oldY, double newX, double newY, Mover<T> coalescer);

    int conditionallyRemove(Predicate<T> pred);

    void clear();

    int size();

    void visitAll(CoordinateMapVisitor<T> v);

    boolean remove(double x, double y);

    CoordinateMapModifier<T> modifier();

    CoordinateMapModifier<T> modifier(Mover<T> mover);

    static final AtomicBoolean DEBUG = new AtomicBoolean();

    static boolean isDebug() {
        return DEBUG.get();
    }

    static void debug(Runnable r) {
        boolean old = DEBUG.get();
        DEBUG.set(true);
        r.run();
        DEBUG.set(old);
    }
}
