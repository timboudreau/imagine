package org.imagine.awt.key;

import java.awt.Color;
import java.awt.MultipleGradientPaint;
import org.imagine.awt.util.IdPathBuilder;
import org.imagine.awt.util.Hasher;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import static java.lang.Float.floatToIntBits;
import static java.lang.Float.intBitsToFloat;
import org.imagine.io.KeyWriter;

/**
 * Key which incorporates the salient values from a LinearGradientPaint.
 *
 * @author Tim Boudreau
 */
public final class RadialPaintKey extends MultiplePaintKey<RadialGradientPaint> {

    public static final String ID_BASE = "radial";
    private final int radius;

    public RadialPaintKey(RadialGradientPaint p) {
        super(p, RadialPaintKey::pointsArrayForRadialPaint);
        radius = floatToIntBits(p.getRadius());
    }

    public RadialPaintKey(double radius, double x1, double y1, double x2, double y2, float[] fractions, Color[] colors, MultipleGradientPaint.CycleMethod cycle, MultipleGradientPaint.ColorSpaceType type, AffineTransform xform) {
        super(x1, y1, x2, y2, fractions, colors, cycle, type, xform);
        this.radius = floatToIntBits(roundOff(radius));
    }

    public RadialPaintKey(int radius, int cx, int cy, int fx, int fy, int[] fracs, int[] colors, byte cycle, byte color, long[] transform) {
        super(cx, cy, fx, fy, fracs, colors, cycle, color, transform);
        this.radius = radius;
    }

    @Override
    public PaintKeyKind kind() {
        return StandardPaintKeyKinds.RADIAL_GRADIENT;
    }

    @Override
    public void writeTo(KeyWriter writer) {
        super.writeTo(writer);
        writer.writeInt(radius);
    }

    private static float[] pointsArrayForRadialPaint(RadialGradientPaint p) {
        Point2D start = p.getCenterPoint();
        Point2D end = p.getFocusPoint();
        return new float[]{
            (float) start.getX(),
            (float) start.getY(),
            (float) end.getX(),
            (float) end.getY()
        };
    }

    @Override
    protected RadialGradientPaint createPaint() {
        return new RadialGradientPaint(
                new Point2D.Float(centerX(), centerY()),
                radius(),
                new Point2D.Float(focusX(), focusY()),
                fractions(), colors(),
                cycleMethod(), colorSpaceType(), transform()
        );
    }

    @Override
    protected void addToHash(Hasher hasher) {
        hasher.add(radius);
    }

    @Override
    protected boolean test(MultiplePaintKey<?> other) {
        return other instanceof RadialPaintKey
                && ((RadialPaintKey) other).radius == radius;
    }

    @Override
    protected void addToToString(StringBuilder sb) {
        sb.append(" r").append(intBitsToFloat(radius));
    }

    @Override
    protected void onFinishBuildingId(IdPathBuilder b) {
        b.add(radius);
    }

    @Override
    public String idBase() {
        return ID_BASE;
    }

    @Override
    protected Class<RadialGradientPaint> type() {
        return RadialGradientPaint.class;
    }

    public float radius() {
        return intBitsToFloat(radius);
    }

    public TexturedPaintWrapperKey<RadialPaintKey, RadialGradientPaint>
            toTexturedPaintKey(int width, int height) {
        return TexturedPaintWrapperKey.create(this, width, height);
    }
}
