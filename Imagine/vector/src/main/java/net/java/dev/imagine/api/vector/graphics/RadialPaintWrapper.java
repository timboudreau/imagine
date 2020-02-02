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
import net.java.dev.imagine.api.vector.Attribute;
import net.java.dev.imagine.api.vector.Primitive;

/**
 *
 * @author Tim Boudreau
 */
public class RadialPaintWrapper implements Primitive, PaintWrapper, Attribute<RadialGradientPaint> {

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
        for (int i = 0; i < 10; i++) {
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
    public Primitive copy() {
        return new RadialPaintWrapper(centerX, centerY, focusX, focusY,
                fractions, colors, cycleMethod, colorSpaceType, xpar, radius, transform);
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
}
