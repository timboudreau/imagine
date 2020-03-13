package org.imagine.geometry;

import com.mastfrog.util.collections.CollectionUtils;
import com.mastfrog.util.collections.IntList;
import com.mastfrog.util.collections.IntMap;
import com.mastfrog.util.search.Bias;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import static java.awt.geom.PathIterator.SEG_LINETO;
import static java.awt.geom.PathIterator.SEG_MOVETO;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

/**
 * A minimal-memory-footprint, high-performance Shape implementation which
 * aggregates multiple Shape instances a shape which provides a single
 * PathIterator for all of them, which internally stores its data as a single
 * array of floats.
 *
 * @author Tim Boudreau
 */
public final class MinimalAggregateShapeFloat implements Shape {

    private final byte[] types;
    private final float[] data;
    private float minX, minY, maxX, maxY;
    private final IntMap<Integer> im;

    public MinimalAggregateShapeFloat(int[] types, float[] data, int windingRule) {
        this(intToByteArray(types), data, windingRule);
    }

    @SuppressWarnings("UnnecessaryBoxing")
    public MinimalAggregateShapeFloat(byte[] types, float[] data, int windingRule) {
        this.types = types;
        this.data = data;
        im = IntMap.singleton(0, Integer.valueOf(windingRule));
        if (data.length < (types.length - 1) * 2) {
            throw new IllegalArgumentException("Data length " + data.length
                    + " is less than that needed for " + (types.length - 1)
                    + " points and a CLOSE_PATH");
        }
        minX = minY = Float.MAX_VALUE;
        maxX = maxY = Float.MIN_VALUE;
        for (int i = 0; i < data.length; i += 2) {
            minX = Math.min(minX, data[i]);
            minY = Math.min(minY, data[i + 1]);
            maxX = Math.max(maxX, data[i]);
            maxY = Math.max(maxY, data[i + 1]);
        }
    }

    static byte[] intToByteArray(int[] ints) {
        byte[] result = new byte[ints.length];
        for (int i = 0; i < ints.length; i++) {
            arraySizeForType(i); // to validate
            result[i] = (byte) ints[i];
        }
        return result;
    }

    /**
     * Create a new instance from a set of shapes.
     *
     * @param shapes The shapes
     */
    public MinimalAggregateShapeFloat(Shape... shapes) {
        this(null, shapes);
    }

    /**
     * Create a new instance from a set of shapes.
     *
     * @param shapes The shapes
     * @param xform A transform to apply when extracting the path
     */
    public MinimalAggregateShapeFloat(AffineTransform xform, Shape... shapes) {
        minX = minY = Float.MAX_VALUE;
        maxX = maxY = Float.MIN_VALUE;
        IntList il = IntList.create(shapes.length * 4);
        FloatList fl = new FloatList(shapes.length * 6);
        float[] scratch = new float[6];
        im = CollectionUtils.intMap(shapes.length);
        for (Shape shape : shapes) {
            PathIterator it = shape.getPathIterator(xform);
            im.put(il.size(), (Integer) it.getWindingRule());
            while (!it.isDone()) {
                int type = it.currentSegment(scratch);
                int length = arraySizeForType(type);
                il.add(type);
                if (length > 0) {
                    fl.put(scratch, length);
                    for (int i = 0; i < length; i += 2) {
                        minX = Math.min(minX, scratch[i]);
                        minY = Math.min(minY, scratch[i + 1]);
                        maxX = Math.max(maxX, scratch[i]);
                        maxY = Math.max(maxY, scratch[i + 1]);
                    }
                }
                it.next();
            }
        }
        types = new byte[il.size()];
        for (int i = 0; i < types.length; i++) {
            types[i] = (byte) il.getAsInt(i);
        }
        data = fl.toFloatArray();
        if (types.length == 0) {
            minX = maxX = minY = maxY = 0;
        }
    }

    private static int arraySizeForType(int type) {
        switch (type) {
            case SEG_MOVETO:
            case SEG_LINETO:
                return 2;
            case PathIterator.SEG_CUBICTO:
                return 6;
            case PathIterator.SEG_QUADTO:
                return 4;
            case PathIterator.SEG_CLOSE:
                return 0;
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
        return new Rectangle2D.Float(
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
        return new ArrayPathIteratorFloat(im, types, data, at);
    }

    public static final class ArrayPathIteratorFloat implements PathIterator {

        private final IntMap<Integer> rules;

        private final byte[] types;
        private final float[] data;
        private int typeCursor;
        private int dataCursor;

        public ArrayPathIteratorFloat(int windingRule, byte[] types, float[] data, AffineTransform xform) {
            this(IntMap.singleton(0, windingRule), types, data, xform);
        }

        public ArrayPathIteratorFloat(IntMap<Integer> rules, byte[] types, float[] data, AffineTransform xform) {
            this.rules = rules;
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
            return rules.nearest(typeCursor, Bias.BACKWARD);
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
            System.arraycopy(data, dataCursor, coords, 0, len);
            return type;
        }

        @Override
        public int currentSegment(double[] coords) {
            int type = types[typeCursor];
            int len = arraySizeForType(type);
            for (int i = 0; i < len; i++) {
                coords[i] = data[i + dataCursor];
            }
            return type;
        }
    }
}
