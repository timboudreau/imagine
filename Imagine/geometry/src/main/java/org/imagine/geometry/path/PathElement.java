/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.geometry.path;

import com.mastfrog.function.DoubleBiConsumer;
import static com.mastfrog.util.preconditions.Checks.notNull;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import org.imagine.geometry.EqPointDouble;

/**
 * Encapsulates a single PathElement within a PathIterator or shape.
 *
 * @author Tim Boudreau
 */
public interface PathElement extends Iterable<EqPointDouble> {

    /**
     * The type, one of the constants on PathIterator.
     *
     * @return The type
     */
    int type();

    /**
     * The point data, which may be a larger array than required for this type.
     *
     * @return An array
     */
    double[] points();

    /**
     * Create a flyweight path element which can be updated from a path
     * iterator.
     *
     * @return A flyweight path element
     */
    public static FlyweightPathElement createFlyweight() {
        return new FlyweightPathElement();
    }

    /**
     * Create a flyweight path element which can be updated from a path
     * iterator, initialized with the passed data.
     *
     * @param type The type
     * @param data The point data
     * @return A flyweight path element
     * @throws IllegalArgumentException if the array is too small
     */
    public static FlyweightPathElement createFlyweight(int type, double[] data) {
        if (notNull("data", data).length < PathElementKind.of(type).arraySize()) {
            throw new IllegalArgumentException("Array too small");
        }
        return new FlyweightPathElement(type, data);
    }

    /**
     * Creates a supplier which on each call to <code>get()</code>, gets the
     * next element in the passed <code>PathIterator</code>, if any, and returns
     * it as a <code>PathElement</code> instance. If you plan to keep and use
     * objects supplied by this supplier, call <code>copy()</code> on the
     * returned object and keep that, as data structures may be reused. The
     * supplier, which will return null when iteration is completed.
     *
     * @param iter A path iterator
     * @return a Supplier which will return null when all elements have been
     * iterated
     */
    public static Supplier<PathElement> elementSupplier(PathIterator iter) {
        final FlyweightPathElement fly = new FlyweightPathElement();
        return () -> {
            boolean notDone = fly.update(iter);
            if (notDone) {
                return fly;
            }
            return null;
        };
    }

    /**
     * Creates an iterable of PathElements over a shape.If you plan to keep and
     * use objects supplied by the iterator, call <code>copy()</code> on the
     * returned PathElement and keep that, as data structures may be reused.
     *
     * @param shape A shape
     * @return An iterator
     */
    public static Iterable<PathElement> iterable(Shape shape) {
        return iterable(shape, null);
    }

    /**
     * Creates an iterable of PathElements over a shape.If you plan to keep and
     * use objects supplied by the iterator, call <code>copy()</code> on the
     * returned PathElement and keep that, as data structures may be reused.
     *
     * @param shape A shape
     * @param xform An optional transform
     * @return An iterator
     */
    public static Iterable<PathElement> iterable(Shape shape, AffineTransform xform) {
        return () -> elementIterator(shape.getPathIterator(xform));
    }

    /**
     * Creates an iterator of PathElements over a PathIterator.If you plan to
     * keep and use objects supplied by this iterator, call <code>copy()</code>
     * on the returned object and keep that, as data structures may be reused.
     *
     * @param iter A path iterator
     * @return An iterator
     */
    public static Iterator<PathElement> elementIterator(PathIterator iter) {
        final FlyweightPathElement fly = new FlyweightPathElement();
        return new Iterator<PathElement>() {
            private byte state = 0;

            private void updateState() {
                if (state == 0) {
                    boolean hasNext = fly.update(iter);
                    if (hasNext) {
                        state = 1;
                    } else {
                        state = 2;
                    }
                }
            }

            @Override
            public boolean hasNext() {
                switch (state) {
                    case 0:
                        updateState();
                        return hasNext();
                    case 1:
                        return true;
                    case 2:
                        return false;
                    default:
                        throw new IllegalStateException("Bad state " + state);
                }
            }

            @Override
            public PathElement next() {
                updateState();
                if (!iter.isDone()) {
                    iter.next();
                }
                switch (state) {
                    case 0:
                        throw new AssertionError("Cannot be in 0 after updateState()");
                    case 1:
                        return fly;
                    case 2:
                        throw new NoSuchElementException("Done");
                    default:
                        throw new IllegalStateException("Bad state " + state);
                }
            }
        };
    }

    /**
     * Create an independent copy of this element.
     *
     * @return A path element that is not this instance
     */
    default PathElement copy() {
        return new SimplePathElement(type(), pointData());
    }

    /**
     * Returns a <i>copy</i> of the point data which is sized to the size
     * required for this element's type; will return an empty array for CLOSE
     * elements.
     *
     * @return An array
     */
    default double[] pointData() {
        double[] pts = points();
        PathElementKind k = kind();
        return k == PathElementKind.CLOSE ? new double[0]
                : Arrays.copyOf(pts, k.arraySize());
    }

    /**
     * Get the destination point (last one in the array) for this element, or
     * null if this is a path-closing element.
     *
     * @param c A consumer that will be passed coordinates
     * @return true if the consumer was invoked
     */
    default boolean destinationPoint(DoubleBiConsumer c) {
        return kind().destinationPoint(points(), c);
    }

    /**
     * Fetch the point at a given index within this element's points into the
     * passed DoubleBiConsumer.
     *
     * @param index The index
     * @param c A consumer
     * @throws IndexOutOfBoundsException if the point index is greater than the
     * number of points present (max 3).
     */
    default void point(int index, DoubleBiConsumer c) {
        if (index >= pointCount()) {
            throw new IndexOutOfBoundsException("No point " + index + " in " + kind());
        }
        double[] d = points();
        int off = index * 2;
        c.accept(d[off], d[off + 1]);
    }

    /**
     * Get the point at a given index within this element's points.
     *
     * @param index The point index relative to this element
     * @return A point
     * @throws IndexOutOfBoundsException if the point index is greater than the
     * number of points present (max 3).
     */
    default EqPointDouble point(int index) {
        if (index >= pointCount()) {
            throw new IndexOutOfBoundsException("No point " + index
                    + " in " + kind());
        }
        double[] d = points();
        int off = index * 2;
        return new EqPointDouble(d[off], d[off + 1]);
    }

    /**
     * Get the destination point (the physical point / last in the points array)
     * for this path element, or null if it is a path closing element.
     *
     * @return The destination point or null
     */
    default EqPointDouble destinationPoint() {
        return kind().destinationPoint((x, y) -> {
            return new EqPointDouble(x, y);
        }, points());
    }

    /**
     * Get the kind of this element.
     *
     * @return The element kind
     */
    default PathElementKind kind() {
        return PathElementKind.of(type());
    }

    /**
     * Get the number of points for this element.
     *
     * @return The number of points
     */
    default int pointCount() {
        return kind().pointCount();
    }

    /**
     * Get an iterator of all the points in this element.
     *
     * @return An iterator
     */
    @Override
    default Iterator<EqPointDouble> iterator() {
        return new Iterator<EqPointDouble>() {
            int cursor = -1;
            final int count = pointCount();

            @Override
            public boolean hasNext() {
                return cursor + 1 < count;
            }

            @Override
            public EqPointDouble next() {
                return point(++cursor);
            }
        };
    }

    default void applyTo(Path2D path) {
        double[] data = points();
        PathElementKind k = kind();
        assert k == PathElementKind.CLOSE || data.length >= k.arraySize();
        switch (k) {
            case CLOSE:
                path.closePath();
                break;
            case LINE:
                path.lineTo(data[0], data[1]);
                break;
            case MOVE:
                path.moveTo(data[0], data[1]);
                break;
            case QUADRATIC:
                path.quadTo(data[0], data[1], data[2], data[3]);
                break;
            case CUBIC:
                path.curveTo(data[0], data[1], data[2], data[3], data[4], data[5]);
                break;
            default:
                throw new AssertionError(k);
        }
    }
}
