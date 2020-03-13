/*
 * BasicStrokeWrapper.java
 *
 * Created on September 27, 2006, 9:29 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.graphics;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Arrays;
import net.java.dev.imagine.api.vector.Attribute;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.Transformable;

/**
 * Sets the Stroke of a Graphics2D.
 *
 * @author Tim Boudreau
 */
public final class BasicStrokeWrapper implements Primitive, Attribute<BasicStroke>, Transformable {

    public float miterLimit;
    public float[] dashArray;
    public float dashPhase;
    public float lineWidth;
    public byte endCap;
    public byte lineJoin;

    public BasicStrokeWrapper(float miterLimit, float[] dashArray, float dashPhase, float lineWidth, byte endCap, byte lineJoin) {
        this.miterLimit = miterLimit;
        this.dashArray = dashArray;
        this.dashPhase = dashPhase;
        this.lineWidth = lineWidth;
        switch (endCap) {
            case BasicStroke.CAP_BUTT:
            case BasicStroke.CAP_ROUND:
            case BasicStroke.CAP_SQUARE:
                break;
            default:
                throw new IllegalArgumentException("Unknown end cap value " + endCap);
        }
        this.endCap = endCap;
        switch (lineJoin) {
            case BasicStroke.JOIN_MITER:
                if (miterLimit < 1) {
                    throw new IllegalArgumentException("Miter limit must be > 1 for"
                            + " JOIN_MITER");
                }
            case BasicStroke.JOIN_BEVEL:
            case BasicStroke.JOIN_ROUND:
                break;
            default:
                throw new IllegalArgumentException("Unknown line join value " + endCap);
        }
        if (dashArray != null) {
            if (dashArray.length == 0) {
                throw new IllegalArgumentException("Dash array present but 0-length");
            }
            boolean foundNonZero = false;
            for (int i = 0; i < dashArray.length; i++) {
                if (dashArray[i] != 0) {
                    foundNonZero = true;
                    break;
                }
            }
            if (!foundNonZero) {
                throw new IllegalArgumentException("Dash array is all zeros: " + Arrays.toString(dashArray));
            }
        }
        this.lineJoin = lineJoin;
    }

    public BasicStrokeWrapper(BasicStroke stroke) {
        this.dashArray = stroke.getDashArray();
        this.dashPhase = stroke.getDashPhase();
        this.endCap = (byte) stroke.getEndCap();
        this.lineJoin = (byte) stroke.getLineJoin();
        this.lineWidth = stroke.getLineWidth();
        this.miterLimit = stroke.getMiterLimit();
    }

    private BasicStrokeWrapper(BasicStrokeWrapper w) {
        this.dashArray = w.dashArray == null ? null : new float[w.dashArray.length];
        if (w.dashArray != null) {
            System.arraycopy(w.dashArray, 0, dashArray, 0, dashArray.length);
        }
        this.dashPhase = w.dashPhase;
        this.endCap = w.endCap;
        this.lineJoin = w.lineJoin;
        this.lineWidth = w.lineWidth;
        this.miterLimit = w.miterLimit;
    }

    public BasicStrokeWrapper copy(float newLineWidth) {
        BasicStrokeWrapper result = new BasicStrokeWrapper(this);
        result.lineWidth = newLineWidth;
        return result;
    }

    public BasicStroke toStroke() {
        try {
            return new BasicStroke(lineWidth, endCap, lineJoin, miterLimit,
                    dashArray, dashPhase);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(this.toString(), ex);
        }
    }

    @Override
    public String toString() {
        return "BasicStroke: lineWidth "
                + lineWidth + " lineJoin  " + lineJoin
                + " endCap " + endCap + " dashPhase "
                + dashPhase + " miterLimit "
                + miterLimit + " dashArray "
                + toString(dashArray);
    }

    private static String toString(float[] f) {
        if (f == null) {
            return "null";
        }
        StringBuilder b = new StringBuilder(30);
        b.append('[');
        for (int i = 0; i < f.length; i++) {
            b.append(f[i]);
            if (i != f.length - 1) {
                b.append(", ");
            }
        }
        b.append(']');
        return b.toString();
    }

    @Override
    public boolean equals(Object o) {
        boolean result = o instanceof BasicStrokeWrapper;
        if (result) {
            BasicStrokeWrapper b = (BasicStrokeWrapper) o;
            result &= endCap == b.endCap && lineJoin == b.lineJoin
                    && lineWidth == b.lineWidth && miterLimit
                    == b.miterLimit && dashPhase == b.dashPhase
                    && ((dashArray == null && b.dashArray == null)
                    || (dashArray != null && b.dashArray != null
                    && Arrays.equals(dashArray, b.dashArray)));
        }
        return result;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 71 * hash + Float.floatToIntBits(this.miterLimit);
        hash = 71 * hash + Arrays.hashCode(this.dashArray);
        hash = 71 * hash + Float.floatToIntBits(this.dashPhase);
        hash = 71 * hash + Float.floatToIntBits(this.lineWidth);
        hash = 71 * hash + this.endCap;
        hash = 71 * hash + this.lineJoin;
        return hash;
    }

    @Override
    public void paint(Graphics2D g) {
        g.setStroke(toStroke());
    }

    @Override
    public BasicStrokeWrapper copy() {
        return new BasicStrokeWrapper(this);
    }

    @Override
    public BasicStroke get() {
        return toStroke();
    }

    public double getLineWidth() {
        return lineWidth;
    }

    @Override
    public void applyTransform(AffineTransform xform) {
        switch (xform.getType()) {
            case AffineTransform.TYPE_GENERAL_TRANSFORM:
                double[] pts = new double[]{0, 0, lineWidth, lineWidth};
                double origDist = Point2D.distance(0, 0, lineWidth, lineWidth);
                xform.transform(pts, 0, pts, 0, pts.length / 2);
                double newDist = Point2D.distance(pts[0], pts[0], pts[1], pts[1]);
                double factor = newDist / origDist;
                lineWidth *= factor;
                break;
            case AffineTransform.TYPE_GENERAL_SCALE:
                // No right way to do this, but doing nothing is
                // definitely wrong
                double avg = (xform.getScaleX() + xform.getScaleY()) / 2D;
                lineWidth *= avg;
                break;
            case AffineTransform.TYPE_UNIFORM_SCALE:
                lineWidth *= xform.getScaleX();
                break;
        }
    }

    @Override
    public void applyScale(double scale) {
        lineWidth *= scale;
    }

    @Override
    public BasicStrokeWrapper copy(AffineTransform transform) {
        BasicStrokeWrapper result = copy(transform);
        result.applyTransform(transform);
        return result;
    }
}
