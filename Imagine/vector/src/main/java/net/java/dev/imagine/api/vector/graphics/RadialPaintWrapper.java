/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.graphics;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint.ColorSpaceType;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Arrays;
import net.java.dev.imagine.api.vector.Attribute;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.Transformable;

/**
 *
 * @author Tim Boudreau
 */
public class RadialPaintWrapper implements Primitive, PaintWrapper, Attribute<RadialGradientPaint>, Transformable {

    private final double centerX;
    private final double centerY;
    private final double focusX;
    private final double focusY;
    private final float[] fractions;
    private final int[] colors;
    private final int cycleMethod;
    private final int colorSpaceType;
    private final int xpar;
    private final float radius;
    private double[] transform;

    public RadialPaintWrapper(double centerX, double centerY, double focusX, double focusY, float[] fractions, int[] colors, int cycleMethod, int colorSpaceType, int xpar, float radius, double[] transform) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.focusX = focusX;
        this.focusY = focusY;
        this.fractions = fractions;
        this.colors = colors;
        this.cycleMethod = cycleMethod;
        this.colorSpaceType = colorSpaceType;
        this.xpar = xpar;
        this.radius = radius;
        this.transform = transform;
    }

    public RadialPaintWrapper(RadialGradientPaint p) {
        fractions = p.getFractions();
        centerX = p.getCenterPoint().getX();
        centerY = p.getCenterPoint().getY();
        focusX = p.getFocusPoint().getX();
        focusY = p.getFocusPoint().getY();
        Color[] colors = p.getColors();
        this.colors = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            this.colors[i] = colors[i].getRGB();
        }
        cycleMethod = p.getCycleMethod().ordinal();
        colorSpaceType = p.getColorSpace().ordinal();
        xpar = p.getTransparency();
        radius = p.getRadius();
        AffineTransform xform = p.getTransform();
        this.transform = new double[6];
        xform.getMatrix(this.transform);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RadialPaintWrapper(");
        sb.append ('r').append(radius).append(' ');
        sb.append(" center ").append(centerX).append(',').append(centerY);
        sb.append(" focus ").append(focusX).append(',').append(focusY);
        sb.append(' ').append(CycleMethod.values()[cycleMethod]);
        for (int i = 0; i < colors.length; i++) {
            sb.append(' ').append(" @").append(fractions[i])
                    .append(" <");
            GradientPaintWrapper.rgb(new Color(colors[i]), sb);
            sb.append('>');
        }
        return sb.append(')').toString();
    }

    private AffineTransform transform() {
        return new AffineTransform(this.transform);
    }

    private Color[] colors() {
        Color[] result = new Color[this.colors.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = new Color(colors[i]);
        }
        return result;
    }

    @Override
    public Paint toPaint() {
        return get();
    }

    @Override
    public void paint(Graphics2D g) {
        g.setPaint(toPaint());
    }

    @Override
    public RadialPaintWrapper copy() {
        double[] newTransform = Arrays.copyOf(transform, transform.length);
        float[] newFractions = Arrays.copyOf(fractions, fractions.length);
        int[] newColors = Arrays.copyOf(colors, colors.length);
        return new RadialPaintWrapper(centerX, centerY, focusX, focusY,
                newFractions, newColors, cycleMethod, colorSpaceType, xpar,
                radius, newTransform);
    }

    @Override
    public Color toColor() {
        return new Color(colors[0]); // XXX huh?
    }

    @Override
    public PaintWrapper createScaledInstance(AffineTransform xform) {
        Point2D.Double scratch = new Point2D.Double(centerX, centerY);
        xform.transform(scratch, scratch);
        double cx = scratch.x;
        double cy = scratch.y;
        scratch.setLocation(focusX, focusY);
        xform.transform(scratch, scratch);
        double fx = scratch.x;
        double fy = scratch.y;
        AffineTransform xf = transform();
        xf.concatenate(xform);
        return new RadialPaintWrapper(cx, cy, fx, fy, fractions, colors,
                cycleMethod, colorSpaceType, xpar, radius, transform);
    }

    @Override
    public RadialGradientPaint get() {
        return new RadialGradientPaint(new Point2D.Double(centerX, centerY),
                radius, new Point2D.Double(focusX, focusY), fractions, colors(),
                CycleMethod.values()[cycleMethod],
                ColorSpaceType.values()[colorSpaceType],
                transform()
        );
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 83 * hash + (int) (Double.doubleToLongBits(this.centerX) ^ (Double.doubleToLongBits(this.centerX) >>> 32));
        hash = 83 * hash + (int) (Double.doubleToLongBits(this.centerY) ^ (Double.doubleToLongBits(this.centerY) >>> 32));
        hash = 83 * hash + (int) (Double.doubleToLongBits(this.focusX) ^ (Double.doubleToLongBits(this.focusX) >>> 32));
        hash = 83 * hash + (int) (Double.doubleToLongBits(this.focusY) ^ (Double.doubleToLongBits(this.focusY) >>> 32));
        hash = 83 * hash + Arrays.hashCode(this.fractions);
        hash = 83 * hash + Arrays.hashCode(this.colors);
        hash = 83 * hash + this.cycleMethod;
        hash = 83 * hash + this.colorSpaceType;
        hash = 83 * hash + this.xpar;
        hash = 83 * hash + Float.floatToIntBits(this.radius);
        hash = 83 * hash + Arrays.hashCode(this.transform);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RadialPaintWrapper other = (RadialPaintWrapper) obj;
        if (Double.doubleToLongBits(this.centerX) != Double.doubleToLongBits(other.centerX)) {
            return false;
        }
        if (Double.doubleToLongBits(this.centerY) != Double.doubleToLongBits(other.centerY)) {
            return false;
        }
        if (Double.doubleToLongBits(this.focusX) != Double.doubleToLongBits(other.focusX)) {
            return false;
        }
        if (Double.doubleToLongBits(this.focusY) != Double.doubleToLongBits(other.focusY)) {
            return false;
        }
        if (this.cycleMethod != other.cycleMethod) {
            return false;
        }
        if (this.colorSpaceType != other.colorSpaceType) {
            return false;
        }
        if (this.xpar != other.xpar) {
            return false;
        }
        if (Float.floatToIntBits(this.radius) != Float.floatToIntBits(other.radius)) {
            return false;
        }
        if (!Arrays.equals(this.fractions, other.fractions)) {
            return false;
        }
        if (!Arrays.equals(this.colors, other.colors)) {
            return false;
        }
        if (!Arrays.equals(this.transform, other.transform)) {
            return false;
        }
        return true;
    }

    @Override
    public void applyScale(AffineTransform xform) {
        AffineTransform curr = transform();
        curr.concatenate(xform);
        curr.getMatrix(transform);
    }

    @Override
    public RadialPaintWrapper copy(AffineTransform transform) {
        double[] newTransform = new double[this.transform.length];
        float[] newFractions = Arrays.copyOf(fractions, fractions.length);
        int[] newColors = Arrays.copyOf(colors, colors.length);
        AffineTransform curr = transform();
        curr.concatenate(transform);
        curr.getMatrix(newTransform);
        return new RadialPaintWrapper(centerX, centerY, focusX, focusY,
                newFractions, newColors, cycleMethod, colorSpaceType, xpar,
                radius, newTransform);
    }


}
