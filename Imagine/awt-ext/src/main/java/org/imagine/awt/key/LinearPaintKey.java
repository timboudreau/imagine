package org.imagine.awt.key;

import java.awt.Color;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint.ColorSpaceType;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

/**
 * Key which incorporates the salient values from a LinearGradientPaint.
 *
 * @author Tim Boudreau
 */
public final class LinearPaintKey extends MultiplePaintKey<LinearGradientPaint> {

    public static final String ID_BASE = "linear";

    public LinearPaintKey(LinearGradientPaint p) {
        super(p, LinearPaintKey::pointsArrayForLinearPaint);
    }

    public LinearPaintKey(double x1, double y1, double x2, double y2,
            float[] fractions, Color[] colors, CycleMethod cycle,
            ColorSpaceType type, AffineTransform xform) {
        super(x1, y1, x2, y2, fractions, colors, cycle, type, xform);
    }

    LinearPaintKey(int cx, int cy, int fx, int fy, int[] fracs, int[] colors, byte cycle, byte color, long[] transform) {
        super(cx, cy, fx, fy, fracs, colors, cycle, color, transform);
    }

    @Override
    public PaintKeyKind kind() {
        return StandardPaintKeyKinds.LINEAR_GRADIENT;
    }

    private static float[] pointsArrayForLinearPaint(LinearGradientPaint p) {
        Point2D start = p.getStartPoint();
        Point2D end = p.getEndPoint();
        return new float[]{
            (float) start.getX(),
            (float) start.getY(),
            (float) end.getX(),
            (float) end.getY()
        };
    }

    @Override
    protected LinearGradientPaint createPaint() {
        return new LinearGradientPaint(new Point2D.Float(centerX(), centerY()),
                new Point2D.Float(focusX(), focusY()), fractions(), colors(),
                cycleMethod(), colorSpaceType(), transform());
    }

    @Override
    public String idBase() {
        return ID_BASE;
    }

    @Override
    protected boolean test(MultiplePaintKey<?> other) {
        return other instanceof LinearPaintKey;
    }

    @Override
    protected Class<LinearGradientPaint> type() {
        return LinearGradientPaint.class;
    }

    public TexturedPaintWrapperKey<LinearPaintKey, LinearGradientPaint>
            toTexturedPaintKey(int width, int height) {
        return TexturedPaintWrapperKey.create(this, width, height);
    }
}
