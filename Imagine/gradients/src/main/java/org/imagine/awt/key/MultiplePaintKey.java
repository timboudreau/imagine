package org.imagine.awt.key;

import org.imagine.awt.util.IdPathBuilder;
import org.imagine.awt.util.Hasher;
import java.awt.Color;
import java.awt.MultipleGradientPaint;
import java.awt.MultipleGradientPaint.ColorSpaceType;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Paint;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import static java.lang.Float.floatToIntBits;
import static java.lang.Float.intBitsToFloat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import org.imagine.awt.io.KeyWriter;
import org.imagine.awt.io.KeyReader;

/**
 * Key which incorporates the salient values from a LinearGradientPaint.
 *
 * @author Tim Boudreau
 */
public abstract class MultiplePaintKey<T extends MultipleGradientPaint> extends PaintKey<T> {

    // 3323 * Arrays.hashCode(matrix) where matrix is obtained
    // from an identity AffineTransform, so we do not need to
    // store an array for identity transforms
    static final int IDENTITY_MATRIX_HASH = 0x868324bb;
    final int centerX;
    final int centerY;
    final int focusX;
    final int focusY;
    final int[] fractions;
    final int[] colors;
    final byte cycleMethod;
    final byte colorSpaceType;
    final long[] transform;

    MultiplePaintKey(int cx, int cy, int fx, int fy, int[] fracs, int[] colors,
            byte cycle, byte color, long[] transform) {
        this.centerX = cx;
        this.centerY = cy;
        this.focusX = fx;
        this.focusY = fy;
        this.fractions = fracs;
        this.colors = colors;
        this.cycleMethod = cycle;
        this.colorSpaceType = color;
        this.transform = transform == null ? null
                : transform.length == 0 ? null : transform;
    }

    @Override
    public void writeTo(KeyWriter writer) {
        writer.writeByte((byte) (this instanceof LinearPaintKey ? 1 : 2));
        writer.writeInt(centerX);
        writer.writeInt(centerY);
        writer.writeInt(focusX);
        writer.writeInt(focusY);
        writer.writeIntArray(fractions);
        writer.writeIntArray(colors);
        writer.writeByte(cycleMethod);
        writer.writeByte(colorSpaceType);
        writer.writeLongArray(transform == null ? new long[0] : transform);
    }

    public static MultiplePaintKey<?> read(KeyReader reader) throws IOException {
        byte type = reader.readByte();
        if (type != 1 && type != 2) {
            throw new IOException("Unrecognized type " + type);
        }
        int cx = reader.readInt();
        int cy = reader.readInt();
        int fx = reader.readInt();
        int fy = reader.readInt();
        int[] fracs = reader.readIntArray();
        int[] colors = reader.readIntArray();
        if (fracs.length != colors.length) {
            throw new IOException("Wrong array lengths");
        }
        if (fracs.length < 2) {
            throw new IOException("Must have at least 2 fractions and colors for 0.0 and 1.0");
        }
        byte cyc = reader.readByte();
        if (cyc < 0 || cyc >= CycleMethod.values().length) {
            throw new IOException("Unknown cycle method " + cyc);
        }
        byte col = reader.readByte();
        if (col < 0 || col >= ColorSpaceType.values().length) {
            throw new IOException("Unknown color space type " + col);
        }
        long[] xform = reader.readLongArray();
        if (xform.length != 0 && xform.length != 6) {
            throw new IOException("Wrong transform length " + xform.length);
        } else if (xform.length == 0) {
            xform = null;
        }
        if (type == 2) {
            int rad = reader.readInt();
            return new RadialPaintKey(rad, cx, cy, fx, fy, fracs, colors, cyc, col, xform);
        } else {
            return new LinearPaintKey(cx, cy, fx, fy, fracs, colors, cyc, col, xform);
        }
    }

    protected MultiplePaintKey(double x1, double y1, double x2, double y2,
            float[] fractions, Color[] colors, CycleMethod cycle,
            ColorSpaceType type, AffineTransform xform) {
        assert fractions != null : "fractions null";
        assert colors != null : "colors null";
        if (cycle == null) {
            cycle = CycleMethod.REFLECT;
        }
        if (type == null) {
            type = ColorSpaceType.SRGB;
        }
        if (fractions.length != colors.length) {
            throw new IllegalArgumentException("Fractions array size "
                    + fractions.length + " does not match colors array size "
                    + colors.length);
        }
        // XXX do the same normalization as in the other constructor
        centerX = floatToIntBits(roundOff(x1));
        centerY = floatToIntBits(roundOff(y1));
        focusX = floatToIntBits(roundOff(x2));
        focusY = floatToIntBits(roundOff(y2));
        this.fractions = toIntArray(fractions);
        this.colors = toIntArray(colors);
        this.cycleMethod = (byte) cycle.ordinal();
        this.colorSpaceType = (byte) type.ordinal();
        if (xform != null && !xform.isIdentity()) {
            double[] mx = new double[6];
            xform.getMatrix(mx);
            this.transform = toLongArray(mx);
        } else {
            this.transform = null;
        }
    }

    @SuppressWarnings("null")
    protected MultiplePaintKey(T p, Function<T, float[]> pointsFetcher) {
        AffineTransform xform = p.getTransform();

        boolean isIdentity = xform == null || xform.isIdentity();
        float[] coordinates = pointsFetcher.apply(p);

        if (!isIdentity && xform != null && xform.getType() == AffineTransform.TYPE_TRANSLATION) {
            xform.transform(coordinates, 0, coordinates, 0, 2);
            isIdentity = true;
        }
        limitPrecision(coordinates);
        float cx = (float) (coordinates[0]);
        float cy = (float) (coordinates[1]);
        float fx = (float) (coordinates[2]);
        float fy = (float) (coordinates[3]);

        if (isIdentity) {
            this.transform = null;
        } else {
            double[] xf = new double[6];
            xform.getMatrix(xf);
            this.transform = toLongArray(xf);
        }
        Color[] theColors = p.getColors();
        this.colors = new int[theColors.length];
        for (int i = 0; i < theColors.length; i++) {
            this.colors[i] = theColors[i].getRGB();
        }
        fractions = toIntArray(p.getFractions());
        cycleMethod = (byte) p.getCycleMethod().ordinal();
        colorSpaceType = (byte) p.getColorSpace().ordinal();
        this.centerX = floatToIntBits(cx);
        this.centerY = floatToIntBits(cy);
        this.focusX = floatToIntBits(fx);
        this.focusY = floatToIntBits(fy);
    }

    @Override
    protected final void buildId(IdPathBuilder bldr) {
        onStartBuildingId(bldr);
        bldr.add(cycleMethod)
                .add(colorSpaceType)
                .add(fractions)
                .add(colors)
                .add(centerX)
                .add(centerY)
                .add(focusX)
                .add(focusY)
                .add(transform);
        onFinishBuildingId(bldr);
    }

    protected void onStartBuildingId(IdPathBuilder b) {
        // do nothing - for subclasses
    }

    protected void onFinishBuildingId(IdPathBuilder b) {
        // do nothing - for subclasses
    }

    protected void addToToString(StringBuilder sb) {
        // do nothing - for subclasses
    }

    protected void addToHash(Hasher hash) {
        // do nothing - for subclasses
    }

    protected boolean test(MultiplePaintKey<?> other) {
        return true;
    }

    public final AffineTransform transform() {
        if (this.transform == null) {
            return AffineTransform.getTranslateInstance(0, 0);
        }
        double[] d = toDoubleArray(this.transform);
        AffineTransform result = new AffineTransform();
        result.setTransform(d[0], d[1], d[2], d[3], d[4], d[5]);
        return result;
    }

    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName())
                .append("( center ")
                .append(intBitsToFloat(centerX))
                .append(',')
                .append(intBitsToFloat(centerY))
                .append(" focus ")
                .append(intBitsToFloat(focusX))
                .append(',')
                .append(intBitsToFloat(focusY))
                .append(' ')
                .append(MultipleGradientPaint.CycleMethod.values()[cycleMethod])
                .append(' ')
                .append(MultipleGradientPaint.ColorSpaceType.values()[colorSpaceType]);

        addToToString(sb);

        for (int i = 0; i < colors.length; i++) {
            sb.append(' ').append(" @").append(intBitsToFloat(fractions[i]))
                    .append(" <");
            rgb(new Color(colors[i], true), sb);
            sb.append('>');
        }
        if (transform != null) {
            sb.append(" xf ").append(PaintKey.typeString(transform()));
        }
        return sb.append(')').toString();
    }

    @Override
    protected final int computeHashCode() {
        Hasher hasher = new Hasher().add(centerX)
                .add(centerY)
                .add(focusX)
                .add(focusY)
                .add(fractions)
                .add(colors)
                .add(this.transform == null
                        ? IDENTITY_MATRIX_HASH
                        : 3323 * Arrays.hashCode(this.transform))
                .add(cycleMethod)
                .add(colorSpaceType);
        addToHash(hasher);
        return hasher.hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof MultiplePaintKey<?>)) {
            return false;
        }
        final MultiplePaintKey<?> other = (MultiplePaintKey<?>) obj;
        if (!test(other)) {
            return false;
        } else if (cycleMethod != other.cycleMethod) {
            return false;
        } else if (colorSpaceType != other.colorSpaceType) {
            return false;
        } else if (centerX != other.centerX) {
            return false;
        } else if (centerY != other.centerY) {
            return false;
        } else if (focusX != other.focusX) {
            return false;
        } else if (focusY != other.focusY) {
            return false;
        } else if (!Arrays.equals(fractions, other.fractions)) {
            return false;
        } else if (!Arrays.equals(colors, other.colors)) {
            return false;
        } else {
            if ((transform == null) != (other.transform == null)) {
                return false;
            }
            return transform == null ? true
                    : Arrays.equals(transform, other.transform);
        }
    }

    public CycleMethod cycleMethod() {
        return CycleMethod.values()[cycleMethod];
    }

    public ColorSpaceType colorSpaceType() {
        return ColorSpaceType.values()[colorSpaceType];
    }

    public float centerX() {
        return intBitsToFloat(centerX);
    }

    public float centerY() {
        return intBitsToFloat(centerY);
    }

    public float focusX() {
        return intBitsToFloat(focusX);
    }

    public float focusY() {
        return intBitsToFloat(focusY);
    }

    public float[] fractions() {
        return toFloatArray(fractions);
    }

    static Map<MultiplePaintKey<?>, Paint> paintCache
            = Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    public final T toPaint() {
        T result = (T) paintCache.get(this);
        if (result == null) {
            result = createPaint();
            paintCache.put(this, result);
        }
        return result;
    }

    protected abstract T createPaint();

    public Color[] colors() {
        Color[] result = new Color[colors.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = new Color(colors[i], true);
        }
        return result;
    }

    public TexturedPaintWrapperKey<?, T> toTexturedPaintKey(int width, int height) {
        return TexturedPaintWrapperKey.create(this, width, height);
    }
}
