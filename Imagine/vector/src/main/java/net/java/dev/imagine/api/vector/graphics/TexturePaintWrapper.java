/*
 * TexturePaintWrapper.java
 *
 * Created on October 31, 2006, 9:21 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.graphics;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.function.Function;
import net.java.dev.imagine.api.vector.Attribute;
import net.java.dev.imagine.api.vector.Primitive;

/**
 *
 * @author Tim Boudreau
 */
public class TexturePaintWrapper implements Primitive, PaintWrapper, Attribute<TexturePaint> {

    public BufferedImage image;
    public double x;
    public double y;
    public double w;
    public double h;

    public TexturePaintWrapper(TexturePaint p) {
        Rectangle2D anchorRect = p.getAnchorRect();
        BufferedImage img = p.getImage();
        x = anchorRect.getX();
        y = anchorRect.getY();
        w = anchorRect.getWidth();
        h = anchorRect.getHeight();
        image = img;
    }

    public TexturePaintWrapper(TexturePaint p, BufferedImage img) {
        Rectangle2D anchorRect = p.getAnchorRect();
        x = anchorRect.getX();
        y = anchorRect.getY();
        w = anchorRect.getWidth();
        h = anchorRect.getHeight();
        image = img;
    }

    private TexturePaintWrapper(double x, double y, double w, double h, BufferedImage img) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.image = img;
    }

    public void paint(Graphics2D g) {
        g.setPaint(toPaint());
    }

    public Paint toPaint() {
        return get();
    }

    public Color toColor() {
        return Color.BLACK;
    }

    public TexturePaintWrapper copy() {
        return new TexturePaintWrapper(x, y, w, h, image);
    }

    public PaintWrapper createScaledInstance(AffineTransform xform) {
        double[] pts = new double[]{
            x, y, x + w, y + h,};
        xform.transform(pts, 0, pts, 0, 2);
        return new TexturePaintWrapper(pts[0], pts[1], pts[2] - pts[0],
                pts[3] - pts[1], image);
    }

    public TexturePaint get(Function<BufferedImage, BufferedImage> converter) {
        Rectangle2D bds = new Rectangle2D.Double(x, y, w, h);
        return new TexturePaint(converter.apply(image), bds);
    }

    public TexturePaint get() {
        BufferedImage img = image;
        Rectangle2D bds = new Rectangle2D.Double(x, y, w, h);
        return new TexturePaint(img, bds);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 11 * hash + Objects.hashCode(this.image);
        hash = 11 * hash + (int) (Double.doubleToLongBits(this.x) ^ (Double.doubleToLongBits(this.x) >>> 32));
        hash = 11 * hash + (int) (Double.doubleToLongBits(this.y) ^ (Double.doubleToLongBits(this.y) >>> 32));
        hash = 11 * hash + (int) (Double.doubleToLongBits(this.w) ^ (Double.doubleToLongBits(this.w) >>> 32));
        hash = 11 * hash + (int) (Double.doubleToLongBits(this.h) ^ (Double.doubleToLongBits(this.h) >>> 32));
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
        final TexturePaintWrapper other = (TexturePaintWrapper) obj;
        if (Double.doubleToLongBits(this.x) != Double.doubleToLongBits(other.x)) {
            return false;
        }
        if (Double.doubleToLongBits(this.y) != Double.doubleToLongBits(other.y)) {
            return false;
        }
        if (Double.doubleToLongBits(this.w) != Double.doubleToLongBits(other.w)) {
            return false;
        }
        if (Double.doubleToLongBits(this.h) != Double.doubleToLongBits(other.h)) {
            return false;
        }
        if (!Objects.equals(this.image, other.image)) {
            return false;
        }
        return true;
    }


}
