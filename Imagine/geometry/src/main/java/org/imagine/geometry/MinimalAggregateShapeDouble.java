package org.imagine.geometry;

import com.mastfrog.util.collections.CollectionUtils;
import com.mastfrog.util.collections.IntList;
import com.mastfrog.util.collections.IntMap;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import static java.awt.geom.PathIterator.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import static org.imagine.geometry.MinimalAggregateShapeFloat.intToByteArray;

/**
 * A minimal-memory-footprint, high-performance Shape implementation which
 * aggregates multiple Shape instances a shape which provides a single
 * PathIterator for all of them, which internally stores its data as a single
 * array of doubles.
 *
 * @author Tim Boudreau
 */
public final class MinimalAggregateShapeDouble implements Shape {

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
        this(intToByteArray(types), data, windingRule);
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

    private static int arraySizeForType(int type) {
        switch (type) {
            case SEG_CLOSE:
                return 0;
            case SEG_MOVETO:
            case SEG_LINETO:
                return 2;
            case SEG_QUADTO:
                return 4;
            case SEG_CUBICTO:
                return 6;
            default:
                throw new AssertionError(type);
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

    public static final class ArrayPathIteratorDouble implements PathIterator {

        private final IntMap<Integer> windingRules;

        private final byte[] types;
        private final double[] data;
        private int typeCursor;
        private int dataCursor;

        public ArrayPathIteratorDouble(int windingRule, byte[] types, double[] data, AffineTransform xform) {
            this(IntMap.singleton(0, windingRule), types, data, xform);
        }

        public ArrayPathIteratorDouble(IntMap<Integer> windingRules, byte[] types, double[] data, AffineTransform xform) {
            this.windingRules = windingRules;
            this.types = types;
            assert data.length % 2 == 0;
            if (xform == null || xform.isIdentity()) {
                this.data = data;
            } else {
                this.data = Arrays.copyOf(data, data.length);
                xform.transform(this.data, 0, this.data, 0, this.data.length / 2);
            }
        }

        @Override
        public int getWindingRule() {
            return windingRules.nearest(0, true);
        }

        @Override
        public boolean isDone() {
            return typeCursor >= types.length;
        }

        @Override
        public void next() {
            int type = types[typeCursor];
            if (type != PathIterator.SEG_CLOSE) {
                dataCursor += arraySizeForType(type);
            }
            typeCursor++;
        }

        @Override
        public int currentSegment(float[] coords) {
            int type = types[typeCursor];
            int len = arraySizeForType(type);
            for (int i = 0; i < len; i++) {
                coords[i] = (float) data[dataCursor + i];
            }
            return type;
        }

        @Override
        public int currentSegment(double[] coords) {
            int type = types[typeCursor];
            int len = arraySizeForType(type);
            System.arraycopy(data, dataCursor, coords, 0, len);
            return type;

        }
    }
}
