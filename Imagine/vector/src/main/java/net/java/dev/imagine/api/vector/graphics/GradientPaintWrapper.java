/*
 * GradientPaintWrapper.java
 *
 * Created on September 28, 2006, 5:20 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.graphics;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import net.java.dev.imagine.api.vector.Attribute;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.Transformable;

/**
 *
 * @author Tim Boudreau
 */
public final class GradientPaintWrapper implements Primitive, PaintWrapper, Attribute<GradientPaint>, Transformable {

    public int color1;
    public int color2;
    public float x1;
    public float y1;
    public float x2;
    public float y2;
    public boolean cyclic;

    public GradientPaintWrapper(GradientPaint gp) {
        color1 = gp.getColor1().getRGB();
        color2 = gp.getColor2().getRGB();
        Point2D p = gp.getPoint1();
        x1 = (float) p.getX();
        y1 = (float) p.getY();
        p = gp.getPoint2();
        x2 = (float) p.getX();
        y2 = (float) p.getY();
        cyclic = gp.isCyclic();
    }

    private GradientPaintWrapper(int color1, int color2,
            float x1, float y1, float x2, float y2, boolean cyclic) {
        this.color1 = color1;
        this.color2 = color2;
        this.x1 = x1;
        this.x2 = x2;
        this.y1 = y1;
        this.y2 = y2;
        this.cyclic = cyclic;
    }

    public GradientPaint toGradientPaint() {
        GradientPaint result = new GradientPaint(x1, y1, new Color(color1),
                x2, y2, new Color(color2));

        return result;
    }

    public void paint(Graphics2D g) {
        g.setPaint(toPaint());
    }

    public Paint toPaint() {
        return toGradientPaint();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("GradientPaintWrapper(");
        sb.append('<');
        rgb(color1(), sb);
        sb.append('>');
        sb.append("@").append(x1).append(',').append(y1);
        sb.append(' ').append('<');
        rgb(color2(), sb);
        sb.append('@').append(x2).append(',').append(y2).append('>');
        sb.append(cyclic ? " cyclic" : "non-cyclic");
        return sb.append(')').toString();
    }

    static void rgb(Color c, StringBuilder sb) {
        sb.append(c.getRed()).append(',').append(c.getGreen())
                .append(',').append(c.getBlue());
    }

    public Color toColor() {
        return color1();
    }

    public Color color1() {
        return new Color(color1);
    }

    public Color color2() {
        return new Color(color2);
    }

    public GradientPaintWrapper copy() {
        return new GradientPaintWrapper(color1, color2, x1,
                y1, x2, y2, cyclic);
    }

    public PaintWrapper createScaledInstance(AffineTransform xform) {
        float[] pts = new float[]{
            x1, y1, x2, y1,};
        xform.transform(pts, 0, pts, 0, pts.length);
        return new GradientPaintWrapper(color1, color2, pts[0],
                pts[1], pts[2], pts[3], cyclic);
    }

    public GradientPaint get() {
        return toGradientPaint();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 61 * hash + this.color1;
        hash = 61 * hash + this.color2;
        hash = 61 * hash + Float.floatToIntBits(this.x1);
        hash = 61 * hash + Float.floatToIntBits(this.y1);
        hash = 61 * hash + Float.floatToIntBits(this.x2);
        hash = 61 * hash + Float.floatToIntBits(this.y2);
        hash = 61 * hash + (this.cyclic ? 1 : 0);
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
        final GradientPaintWrapper other = (GradientPaintWrapper) obj;
        if (this.color1 != other.color1) {
            return false;
        }
        if (this.color2 != other.color2) {
            return false;
        }
        if (Float.floatToIntBits(this.x1) != Float.floatToIntBits(other.x1)) {
            return false;
        }
        if (Float.floatToIntBits(this.y1) != Float.floatToIntBits(other.y1)) {
            return false;
        }
        if (Float.floatToIntBits(this.x2) != Float.floatToIntBits(other.x2)) {
            return false;
        }
        if (Float.floatToIntBits(this.y2) != Float.floatToIntBits(other.y2)) {
            return false;
        }
        if (this.cyclic != other.cyclic) {
            return false;
        }
        return true;
    }

    @Override
    public void applyScale(AffineTransform xform) {
        Point2D.Float a = new Point2D.Float(x1, y1);
        Point2D.Float b = new Point2D.Float(x2, y2);
        xform.transform(a, a);
        xform.transform(b, b);
        x1 = a.x;
        y1 = a.y;
        x2 = b.x;
        y2 = b.y;
    }

    @Override
    public GradientPaintWrapper copy(AffineTransform transform) {
        Point2D.Float a = new Point2D.Float(x1, y1);
        Point2D.Float b = new Point2D.Float(x2, y2);
        transform.transform(a, a);
        transform.transform(b, b);
        return new GradientPaintWrapper(color1, color2, a.x, a.y, b.x, b.y, cyclic);
    }
}
