package org.imagine.geometry;

import org.imagine.geometry.util.DoubleList;
import com.mastfrog.function.DoubleBiConsumer;
import com.mastfrog.function.DoubleQuadConsumer;
import com.mastfrog.function.DoubleSextaConsumer;
import com.mastfrog.util.collections.CollectionUtils;
import com.mastfrog.util.collections.IntList;
import com.mastfrog.util.collections.IntMap;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import static java.awt.geom.PathIterator.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import org.imagine.geometry.util.GeometryUtils;
import static org.imagine.geometry.util.GeometryUtils.arraySizeForType;
import static org.imagine.geometry.util.GeometryUtils.intArrayToByteArray;

/**
 * A minimal-memory-footprint, high-performance Shape implementation which
 * aggregates multiple Shape instances a shape which provides a single
 * PathIterator for all of them, which internally stores its data as a single
 * array of doubles.
 *
 * @author Tim Boudreau
 */
public final class MinimalAggregateShapeDouble implements Shape, EnhancedShape {

    private final byte[] types;
    private final double[] data;
    private double minX, minY, maxX, maxY;
    private final IntMap<Integer> im;

    /**
     * Create a new instance.
     *
     * @param types The array of PathIterator constants provided by the
     * PathIterator
     * @param data The array of points provided by the PathIterator - minimally
     * sanity checked that it is not impossibly short compared with the length
     * of the types array (minimum is <code>(types.length-1) * 2</code> for a
     * shape that contains a single CLOSE_PATH with no associated points; make
     * SURE the data is correct or the surprise when Java2D tries to render it
     * will be unpleasant - Graphics2D is not forgiving.
     * @param windingRule The winding rule
     */
    public MinimalAggregateShapeDouble(int[] types, double[] data, int windingRule) {
        this(intArrayToByteArray(types), data, windingRule);
    }

    /**
     * Create a new instance.
     *
     * @param types The array of PathIterator constants provided by the
     * PathIterator
     * @param data The array of points provided by the PathIterator - minimally
     * sanity checked that it is not impossibly short compared with the length
     * of the types array (minimum is <code>(types.length-1) * 2</code> for a
     * shape that contains a single CLOSE_PATH with no associated points; make
     * SURE the data is correct or the surprise when Java2D tries to render it
     * will be unpleasant - Graphics2D is not forgiving.
     * @param windingRule The winding rule
     */
    @SuppressWarnings("UnnecessaryBoxing")
    public MinimalAggregateShapeDouble(byte[] types, double[] data, int windingRule) {
        this.types = types;
        this.data = data;
        im = IntMap.singleton(0, Integer.valueOf(windingRule));
        if (data.length < (types.length - 1) * 2) {
            throw new IllegalArgumentException("Data length " + data.length
                    + " is less than that needed for " + (types.length - 1)
                    + " points and a CLOSE_PATH");
        }
        minX = minY = Double.MAX_VALUE;
        maxX = maxY = Double.MIN_VALUE;
        for (int i = 0; i < data.length; i += 2) {
            minX = Math.min(minX, data[i]);
            minY = Math.min(minY, data[i + 1]);
            maxX = Math.max(maxX, data[i]);
            maxY = Math.max(maxY, data[i + 1]);
        }
    }

    /**
     * Create a new instance from a set of shapes.
     *
     * @param shapes The shapes
     */
    public MinimalAggregateShapeDouble(Shape... shapes) {
        this(null, shapes);
    }

    /**
     * Create a new instance from a set of shapes.
     *
     * @param shapes The shapes
     * @param xform A transform to apply when extracting the path
     */
    public MinimalAggregateShapeDouble(AffineTransform xform, Shape... shapes) {
        minX = minY = Double.MAX_VALUE;
        maxX = maxY = Double.MIN_VALUE;
        IntList il = IntList.create(shapes.length * 4);
        DoubleList fl = new DoubleList(shapes.length * 8);
        double[] scratch = new double[6];
        im = shapes.length == 1
                ? IntMap.singleton(0, shapes[0].getPathIterator(null).getWindingRule())
                : CollectionUtils.intMap(shapes.length);
        for (Shape shape : shapes) {
            PathIterator it = shape.getPathIterator(xform);
            while (!it.isDone()) {
                int type = it.currentSegment(scratch);
                int length = arraySizeForType(type);
                if (shapes.length > 1) {
                    im.put(il.size(), (Integer) it.getWindingRule());
                }
                il.add(type);
                fl.put(scratch, length);
                for (int i = 0; i < length; i += 2) {
                    minX = Math.min(minX, scratch[i]);
                    minY = Math.min(minY, scratch[i + 1]);
                    maxX = Math.max(maxX, scratch[i]);
                    maxY = Math.max(maxY, scratch[i + 1]);
                }
                it.next();
            }
        }
        types = new byte[il.size()];
        for (int i = 0; i < types.length; i++) {
            types[i] = (byte) il.getAsInt(i);
        }
        data = fl.toDoubleArray();
        if (types.length == 0) {
            minX = maxX = minY = maxY = 0;
        }
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle((int) Math.floor(minX),
                (int) Math.floor(minY),
                (int) Math.ceil(maxX - minX),
                (int) Math.ceil(maxY - minY));
    }

    @Override
    public Rectangle2D getBounds2D() {
        return new Rectangle2D.Double(
                minX,
                minY,
                maxX - minX,
                maxY - minY);
    }

    @Override
    public boolean contains(double x, double y) {
        return (x >= minX && x < maxX && y >= minY && y < maxY);
    }

    @Override
    public boolean contains(Point2D p) {
        return contains(p.getX(), p.getY());
    }

    @Override
    public boolean intersects(double x, double y, double w, double h) {
        return getBounds2D().intersects(x, y, w, h);
    }

    @Override
    public boolean intersects(Rectangle2D r) {
        return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
        return contains(x, y) && contains(x + w, y + h);
    }

    @Override
    public boolean contains(Rectangle2D r) {
        return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return getPathIterator(at);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        return new ArrayPathIteratorDouble(im, types, data, at);
    }

    @Override
    public Point2D point(int index) {
        int dataCursor = 0;
        double x, y;
        int pointCursor = 0;
        for (int i = 0; i < types.length; i++) {
            switch (types[i]) {
                case SEG_MOVETO:
                case SEG_LINETO:
                    x = data[dataCursor];
                    y = data[dataCursor + 1];
                    dataCursor += 2;
                    pointCursor++;
                    break;
                case SEG_QUADTO:
                    x = data[dataCursor + 2];
                    y = data[dataCursor + 3];
                    dataCursor += 4;
                    pointCursor++;
                    break;
                case SEG_CUBICTO:
                    x = data[dataCursor + 4];
                    y = data[dataCursor + 5];
                    dataCursor += 6;
                    pointCursor++;
                    break;
                case SEG_CLOSE:
                    continue;
                default:
                    throw new AssertionError("Erroneous point type " + types[i] + " at " + i);
            }
            if (pointCursor == index) {
                return new EqPointDouble(x, y);
            }
        }
        throw new IndexOutOfBoundsException("No point " + index);
    }

    @Override
    public void visitPoints(DoubleBiConsumer consumer) {
        int dataCursor = 0;
        for (int i = 0; i < types.length; i++) {
            switch (types[i]) {
                case SEG_MOVETO:
                case SEG_LINETO:
                    consumer.accept(data[dataCursor], data[dataCursor + 1]);
                    dataCursor += 2;
                    break;
                case SEG_QUADTO:
                    consumer.accept(data[dataCursor + 2], data[dataCursor + 3]);
                    dataCursor += 4;
                    break;
                case SEG_CUBICTO:
                    consumer.accept(data[dataCursor + 4], data[dataCursor + 5]);
                    dataCursor += 6;
                    break;
            }
        }
    }

    @Override
    public List<? extends EqPointDouble> points() {
        List<EqPointDouble> result = new ArrayList<>();
        int dataCursor = 0;
        for (int i = 0; i < types.length; i++) {
            switch (types[i]) {
                case SEG_MOVETO:
                case SEG_LINETO:
                    result.add(new EqPointDouble(data[dataCursor], data[dataCursor + 1]));
                    dataCursor += 2;
                    break;
                case SEG_QUADTO:
                    result.add(new EqPointDouble(data[dataCursor + 2], data[dataCursor + 3]));
                    dataCursor += 4;
                    break;
                case SEG_CUBICTO:
                    result.add(new EqPointDouble(data[dataCursor + 4], data[dataCursor + 5]));
                    dataCursor += 6;
                    break;
            }
        }
        return result;
    }

    @Override
    public int pointCount() {
        int result = 0;
        for (int i = 0; i < types.length; i++) {
            if (types[i] != SEG_CLOSE) {
                result++;
            }
        }
        return result;
    }

    @Override
    public boolean isClosed() {
        if (types.length < 3) {
            return false;
        }
        if (types[types.length - 1] == SEG_CLOSE) {
            return true;
        }
        if (GeometryUtils.isSamePoint(data[0], data[1], data[data.length - 2], data[data.length - 1])) {
            return true;
        }
        return false;
    }

    @Override
    public void visitLines(DoubleQuadConsumer consumer) {
        int dataCursor = 0;
        boolean prevWasClose = true;
        double prevX = -1;
        double prevY = -1;
        int ixLastStart = -1;
        boolean unclosed = true;
        for (int i = 0; i < types.length; i++) {
            int dix = -1;
            boolean isClose = false;
            switch (types[i]) {
                case SEG_MOVETO:
                case SEG_LINETO:
                    if (prevWasClose) {
                        ixLastStart = dataCursor;
                        unclosed = true;
                    }
                    dix = dataCursor;
                    dataCursor += 2;
                    break;
                case SEG_QUADTO:
                    dix = dataCursor + 2;
                    dataCursor += 4;
                    break;
                case SEG_CUBICTO:
                    dix = dataCursor + 4;
                    dataCursor += 6;
                    break;
                case SEG_CLOSE:
                    isClose = true;
                    unclosed = false;
                    dix = -1;
                    break;
            }
            if (dix != -1) {
                double x = data[dix];
                double y = data[dix + 1];
                if (!prevWasClose) {
                    consumer.accept(prevX, prevY, x, y);
                }
                prevX = x;
                prevY = y;
                prevWasClose = isClose;
            } else if (isClose) {
                consumer.accept(data[ixLastStart], data[ixLastStart + 1], prevX, prevY);
            }
        }
        if (ixLastStart != 0 && unclosed) {
            consumer.accept(prevX, prevY, data[ixLastStart], data[ixLastStart + 1]);
        }
    }

    @Override
    public void visitAdjoiningLines(DoubleSextaConsumer sex) {
        Line2D.Double prev = new Line2D.Double(Double.MIN_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MIN_VALUE);
        Line2D.Double first = new Line2D.Double(Double.MIN_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MIN_VALUE);
        visitLines((x1, y1, x2, y2) -> {
            if ((prev.x2 == x1 || Math.abs(prev.x2 - x1) < 0.000000000001)
                    && (prev.y2 == y1 || Math.abs(prev.y2 - y1) < 0.000000000001)) {
                sex.accept(prev.x1, prev.y1, x1, y1, x2, y2);
            }
            prev.setLine(x1, y1, x2, y2);
            if (first.x1 == Double.MIN_VALUE) {
                first.setLine(prev);
            }
        });
        if (first.x1 != Double.MAX_VALUE) {
            if (!GeometryUtils.isSamePoint(prev.x2, prev.y2, first.x1, first.y1)) {
                sex.accept(prev.x2, prev.y2, first.x1, first.y1, first.x2, first.y2);
            }
        }
    }
}
