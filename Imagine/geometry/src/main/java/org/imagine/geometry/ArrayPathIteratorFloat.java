package org.imagine.geometry;

import com.mastfrog.util.collections.IntMap;
import com.mastfrog.util.search.Bias;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.util.Arrays;
import static org.imagine.geometry.util.GeometryUtils.arraySizeForType;

/**
 * The smallest-footprint path iterator over a list of points and point
 * types possible. Check your data before you construct one - the iterator
 * assumes it is good.
 */
public final class ArrayPathIteratorFloat implements PathIterator {

    private final IntMap<Integer> rules;
    private final byte[] types;
    private final float[] data;
    private int typeCursor;
    private int dataCursor;

    /**
     * Create a new iterator with the passed winding rule, types and
     * coordinates.
     *
     * @param types The array of point types - constants starting with SEG_
     * on PathIterator (checked)
     * @param types The types
     * @param data The points array, which, depending on the types, may
     * contain from 0 to 6 coordinates per entry in the types array.
     * @param xform A transform or null
     */
    public ArrayPathIteratorFloat(int windingRule, byte[] types, float[] data, AffineTransform xform) {
        this(IntMap.singleton(0, windingRule), types, data, xform);
    }

    /**
     * Create a new iterator with the passed winding rules, types and
     * coordinates.
     *
     *
     * @param rules A sparse IntMap in which the keys are the offsets at
     * which the winding rule *changes* and the value it changes to; in
     * practice, most things read the value once; however, in order to
     * recover the original shapes, and split apart an aggregation of shapes
     * inside one of these, this data is worth having. IntMap.singleton()
     * should be used if there is only one, since its lookup is simply an
     * integer comparison.
     *
     * @param types The array of point types - constants starting with SEG_
     * on PathIterator (checked)
     * @param data The coordinates - make sure this data is good
     * @param xform An optional transform (may be null)
     */
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
        return rules.nearestKey(typeCursor, Bias.BACKWARD);
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
