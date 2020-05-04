package org.imagine.geometry;

import com.mastfrog.util.collections.IntMap;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.util.Arrays;
import static org.imagine.geometry.util.GeometryUtils.arraySizeForType;

/**
 * The smallest-footprint path iterator over a list of double-precision points
 * and point types possible. Check your data before you construct one - the
 * iterator assumes it is good.
 */
public final class ArrayPathIteratorDouble implements PathIterator {

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
        assert data.length % 2 == 0 : "Odd number of coordinates in what should be an array of x/y coords: " + data.length;
        if (xform == null || xform.isIdentity()) {
            this.data = data;
        } else {
            this.data = Arrays.copyOf(data, data.length);
            xform.transform(this.data, 0, this.data, 0, this.data.length / 2);
        }
    }

    @Override
    public int getWindingRule() {
        return windingRules.nearestKey(0, true);
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
