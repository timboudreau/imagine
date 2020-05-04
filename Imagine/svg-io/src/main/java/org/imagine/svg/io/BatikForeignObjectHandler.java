/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.svg.io;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.graphics.LinearPaintWrapper;
import net.java.dev.imagine.api.vector.graphics.RadialPaintWrapper;
import net.java.dev.imagine.api.vector.painting.ForeignPaintObjectHandler;
import org.apache.batik.ext.awt.LinearGradientPaint;
import org.apache.batik.ext.awt.MultipleGradientPaint;
import org.apache.batik.ext.awt.RadialGradientPaint;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider(service = ForeignPaintObjectHandler.class)
public class BatikForeignObjectHandler extends ForeignPaintObjectHandler {

    @Override
    protected Primitive forPaint(Paint paint) {
        if (paint instanceof RadialGradientPaint) {
            RadialGradientPaint rgp = (RadialGradientPaint) paint;
            MultipleGradientPaint.ColorSpaceEnum cs = rgp.getColorSpace();
            MultipleGradientPaint.CycleMethodEnum cycle = rgp.getCycleMethod();
            Point2D center = rgp.getCenterPoint();
            Color[] colors = rgp.getColors();
            Point2D focus = rgp.getFocusPoint();
            float radius = rgp.getRadius();
            AffineTransform xform = rgp.getTransform();
            float[] fracs = rgp.getFractions();
            double[] mx = null;
            int xpar = hasAlpha(colors) ? Transparency.TRANSLUCENT : Transparency.OPAQUE;
            if (xform != null) {
                mx = new double[6];
                xform.getMatrix(mx);
            }
            return new RadialPaintWrapper(center.getX(), center.getY(), focus.getX(), focus.getY(),
                    fracs, toIntArray(colors), get(cycle).ordinal(), get(cs).ordinal(), xpar,
                    radius, mx);
        } else if (paint instanceof LinearGradientPaint) {
            LinearGradientPaint lgp = (LinearGradientPaint) paint;
            MultipleGradientPaint.ColorSpaceEnum cs = lgp.getColorSpace();
            MultipleGradientPaint.CycleMethodEnum cyc = lgp.getCycleMethod();
            Color[] colors = lgp.getColors();
            Point2D start = lgp.getStartPoint();
            Point2D end = lgp.getEndPoint();
            float[] fracs = lgp.getFractions();
            AffineTransform xform = lgp.getTransform();
            int xpar = hasAlpha(colors) ? Transparency.TRANSLUCENT : Transparency.OPAQUE;
            double[] mx = null;
            if (xform != null) {
                mx = new double[6];
                xform.getMatrix(mx);
            }
            return new LinearPaintWrapper(start.getX(), start.getY(), end.getX(), end.getY(), fracs,
                    toIntArray(colors), get(cyc).ordinal(), get(cs).ordinal(), xpar, mx);
        }
        return null;
    }

    private boolean hasAlpha(Color[] colors) {
        for (Color c : colors) {
            if (c.getAlpha() != 255) {
                return true;
            }
        }
        return false;
    }

    private int[] toIntArray(Color[] colors) {
        int[] result = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            result[i] = colors[i].getRGB();
        }
        return result;
    }

    private java.awt.MultipleGradientPaint.ColorSpaceType get(MultipleGradientPaint.ColorSpaceEnum col) {
        if (col == MultipleGradientPaint.LINEAR_RGB) {
            return java.awt.MultipleGradientPaint.ColorSpaceType.LINEAR_RGB;
        }
        return java.awt.MultipleGradientPaint.ColorSpaceType.SRGB;
    }

    private java.awt.MultipleGradientPaint.CycleMethod get(MultipleGradientPaint.CycleMethodEnum cyc) {
        if (MultipleGradientPaint.NO_CYCLE == cyc) {
            return java.awt.MultipleGradientPaint.CycleMethod.NO_CYCLE;
        } else if (MultipleGradientPaint.REFLECT == cyc) {
            return java.awt.MultipleGradientPaint.CycleMethod.REFLECT;
        } else if (MultipleGradientPaint.REPEAT == cyc) {
            return java.awt.MultipleGradientPaint.CycleMethod.REPEAT;
        }
        return java.awt.MultipleGradientPaint.CycleMethod.NO_CYCLE;
    }

}
