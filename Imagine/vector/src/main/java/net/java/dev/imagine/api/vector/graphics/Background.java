/*
 * Background.java
 *
 * Created on October 30, 2006, 3:05 PM
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
 * Sets the background of a graphics context via setBackground().
 *
 * @author Tim Boudreau
 */
public final class Background implements Attribute<Color>, Primitive, PaintWrapper {

    public int red;
    public int green;
    public int blue;
    public int alpha;

    public Background(Color c) {
        this.red = c.getRed();
        this.green = c.getGreen();
        this.blue = c.getBlue();
        this.alpha = c.getAlpha();
    }

    private Background(int red, int green, int blue, int alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    @Override
    public String toString() {
        return "Background(" + red + "," + green + "," + blue + "," + alpha + ")";
    }

    @Override
    public void paint(Graphics2D g) {
        g.setBackground(toColor());
    }

    @Override
    public Background copy() {
        return new Background(red, green, blue, alpha);
    }

    @Override
    public Paint toPaint() {
        return toColor();
    }

    @Override
    public Color toColor() {
        return new Color(red, green, blue, alpha);
    }

    @Override
    public PaintWrapper createScaledInstance(AffineTransform xform) {
        return (PaintWrapper) copy();
    }

    @Override
    public Color get() {
        return toColor();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 43 * hash + this.red;
        hash = 43 * hash + this.green;
        hash = 43 * hash + this.blue;
        hash = 43 * hash + this.alpha;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (getClass() != obj.getClass()) {
            return false;
        }
        final Background other = (Background) obj;
        if (this.red != other.red) {
            return false;
        } else if (this.green != other.green) {
            return false;
        } else if (this.blue != other.blue) {
            return false;
        }
        return this.alpha == other.alpha;
    }
}
