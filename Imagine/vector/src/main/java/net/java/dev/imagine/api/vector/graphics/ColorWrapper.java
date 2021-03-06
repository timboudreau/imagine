/*
 * ColorWrapper.java
 *
 * Created on October 23, 2006, 9:33 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.graphics;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.AffineTransform;
import net.java.dev.imagine.api.vector.Attribute;
import net.java.dev.imagine.api.vector.Primitive;

/**
 *
 * @author Tim Boudreau
 */
public class ColorWrapper implements Primitive, PaintWrapper, Attribute<Color> {

    public final int r, g, b, a;

    public ColorWrapper(int r, int g, int b, int a) {
        this.r = r >= 0 ? r : 255 - r;
        this.g = g >= 0 ? g : 255 - g;
        this.b = b >= 0 ? b : 255 - b;
        this.a = a >= 0 ? a : 255 - a;
    }

    public ColorWrapper(Color color) {
        this(color == null ? 0 : color.getRed(), color == null ? 0 : color.getGreen(),
                color == null ? 0 : color.getBlue(), color == null ? 0 : color.getAlpha());
    }

    @Override
    public Color toColor() {
        return new Color(r, g, b, a);
    }

    @Override
    public void paint(Graphics2D g) {
        g.setPaint(toPaint());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        }
        boolean result = o instanceof ColorWrapper;
        if (result) {
            ColorWrapper c = (ColorWrapper) o;
            result = c.r == r && c.b == b && c.g == g && c.a == a;
        }
        return result;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + this.r;
        hash = 71 * hash + this.g;
        hash = 71 * hash + this.b;
        hash = 71 * hash + this.a;
        return hash;
    }

    @Override
    public Paint toPaint() {
        return toColor();
    }

    @Override
    public ColorWrapper copy() {
        return new ColorWrapper(r, g, b, a);
    }

    @Override
    public ColorWrapper createScaledInstance(AffineTransform xform) {
        return copy();
    }

    @Override
    public Color get() {
        return toColor();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<"
                + r + ", " + g + ", " + b + ", " + a + ">";
    }
}
