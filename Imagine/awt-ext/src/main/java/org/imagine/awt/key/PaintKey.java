package org.imagine.awt.key;

import org.imagine.awt.util.IdPathBuilder;
import java.awt.Color;
import java.awt.Paint;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.Serializable;
import java.util.function.Consumer;
import org.imagine.awt.impl.Accessor;
import org.imagine.awt.io.PaintKeyWriter;

/**
 * Cache keys for different types of paints. Used for several reasons:
 * <ol>
 * <li>Some paints, notably MultipleGradientPaint subtypes, do some non-trivial
 * work in their constructor which is undesirable to do on every paint, when
 * reconstructing the paint from data about it</li>
 * <li>Some paints, notably RadialGradientPaint, have serious performance
 * problems, and are better off wrapped in a TexturePaint, so a cache doesn't
 * have to return a paint of the type requested, and can reuse the underlying
 * raster for a gradient multiple times, cache them, etc.</li>
 * <li>Provides a file-naming scheme for caches of user-loaded TexturePaint
 * instances, and for caching thumbnails (for example, in a palette)</li>
 * <li>Decoupling the data describing how to paint a color or pattern from the
 * things that actually do the painting is useful.</li>
 * </ol>
 *
 * @author Tim Boudreau
 */
@SuppressWarnings("EqualsAndHashcode")
public abstract class PaintKey<T extends Paint> implements Serializable {

    private transient int hash = 0;

    public static <T extends Paint> PaintKey<T> forPaint(T paint) {
        return Accessor.paintKeyFor(paint);
    }

    protected abstract Class<T> type();

    public <T extends PaintKey> boolean as(Class<T> type, Consumer<T> c) {
        if (type.isInstance(this)) {
            c.accept(type.cast(this));
            return true;
        }
        return false;
    }

    @Override
    public final int hashCode() {
        if (hash == 0) {
            hash = computeHashCode();
        }
        return hash;
    }

    protected abstract T toPaint();

    protected abstract int computeHashCode();

    public final String id() {
        IdPathBuilder bldr = new IdPathBuilder().add(idBase());
        buildId(bldr);
        return bldr.toString();
    }

    public abstract String idBase();

    protected abstract void buildId(IdPathBuilder bldr);

    public abstract void writeTo(PaintKeyWriter writer) throws IOException;

    public abstract PaintKeyKind kind();

    protected static StringBuilder rgb(Color c, StringBuilder sb) {
        sb.append(c.getRed()).append(',').append(c.getGreen())
                .append(',').append(c.getBlue());
        return sb;
    }

    protected static CharSequence rgb(Color c) {
        return rgb(c, new StringBuilder());
    }

    protected static final int[] toIntArray(float[] fl) {
        int[] result = new int[fl.length];
        for (int i = 0; i < fl.length; i++) {
            result[i] = Float.floatToIntBits(fl[i]);
        }
        return result;
    }

    protected static final int[] toIntArray(Color[] colors) {
        int[] result = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            result[i] = colors[i].getRGB();
        }
        return result;
    }

    protected static final float[] toFloatArray(int[] ints) {
        float[] result = new float[ints.length];
        for (int i = 0; i < ints.length; i++) {
            result[i] = Float.intBitsToFloat(ints[i]);
        }
        return result;
    }

    protected static final double[] toDoubleArray(long[] ints) {
        double[] result = new double[ints.length];
        for (int i = 0; i < ints.length; i++) {
            result[i] = Double.longBitsToDouble(ints[i]);
        }
        return result;
    }

    protected static final long[] toLongArray(double[] dbls) {
        long[] result = new long[dbls.length];
        for (int i = 0; i < dbls.length; i++) {
            result[i] = Double.doubleToLongBits(dbls[i]);
        }
        return result;
    }

    protected static IdPathBuilder idPath() {
        return new IdPathBuilder();
    }

    protected static final String typeString(int type) {
        switch (type) {
            case AffineTransform.TYPE_FLIP:
                return "Flip";
            case AffineTransform.TYPE_GENERAL_ROTATION:
                return "General Rotation";
            case AffineTransform.TYPE_GENERAL_SCALE:
                return "General Scale";
            case AffineTransform.TYPE_GENERAL_TRANSFORM:
                return "General Transform";
            case AffineTransform.TYPE_IDENTITY:
                return "Identity";
            case AffineTransform.TYPE_QUADRANT_ROTATION:
                return "Quadrant Rotation";
            case AffineTransform.TYPE_TRANSLATION:
                return "Translation";
            case AffineTransform.TYPE_UNIFORM_SCALE:
                return "Uniform Scale";
            case AffineTransform.TYPE_MASK_ROTATION:
                return "Rotation";
            case AffineTransform.TYPE_MASK_SCALE:
                return "Scale";
            default:
                return "Unknown";
        }
    }

    protected static final String typeString(AffineTransform value) {
        StringBuilder sb = new StringBuilder();
        int[] flags = new int[]{AffineTransform.TYPE_FLIP, AffineTransform.TYPE_GENERAL_ROTATION,
            AffineTransform.TYPE_GENERAL_SCALE, AffineTransform.TYPE_GENERAL_TRANSFORM,
            AffineTransform.TYPE_QUADRANT_ROTATION, AffineTransform.TYPE_TRANSLATION, AffineTransform.TYPE_UNIFORM_SCALE};
        int type = value.getType();
        for (int i = 0; i < flags.length; i++) {
            int flg = flags[i];
            if ((type & flg) != 0) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(typeString(type & flg));
            }
        }
        return sb.toString();
    }

    protected static float[] limitPrecision(float[] dbls) {
        for (int i = 0; i < dbls.length; i++) {
            dbls[i] = roundOff(dbls[i]);
        }
        return dbls;
    }

    protected static float[] limitPrecision(double[] dbls) {
        float[] result = new float[dbls.length];
        for (int i = 0; i < dbls.length; i++) {
            result[i] = roundOff(dbls[i]);
        }
        return result;
    }

    protected static float roundOff(float val) {
        return roundOff(val, 100000);
    }

    protected static float roundOff(double val) {
        return roundOff(val, 100000);
    }

    protected static float roundOff(float val, int multiplier) {
        return (float) Math.round(val * multiplier) / multiplier;
    }

    protected static float roundOff(double val, int multiplier) {
        return (float) Math.round(val * multiplier) / multiplier;
    }

    static {
        Accessor.DEFAULT_PAINT_KEY = new PaintKeyAccessorImpl();
    }

    private static class PaintKeyAccessorImpl extends Accessor.PaintKeyAccessor {

        @Override
        protected <K extends PaintKey<P>, P extends Paint> P toRawPaint(K key) {
            return key.toPaint();
        }
    }
}
