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
import java.awt.image.Raster;
import static java.lang.Double.doubleToLongBits;
import java.util.Arrays;
import java.util.function.Function;
import net.java.dev.imagine.api.vector.Attribute;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.Transformable;

/**
 *
 * @author Tim Boudreau
 */
public class TexturePaintWrapper implements Primitive, PaintWrapper, Attribute<TexturePaint>, Transformable {

    public BufferedImage image;
    public double x;
    public double y;
    public double w;
    public double h;
    public byte transparency;

    public TexturePaintWrapper(TexturePaint p) {
        Rectangle2D anchorRect = p.getAnchorRect();
        BufferedImage img = p.getImage();
        x = anchorRect.getX();
        y = anchorRect.getY();
        w = anchorRect.getWidth();
        h = anchorRect.getHeight();
        transparency = (byte) p.getTransparency();
        image = img;
    }

    public TexturePaintWrapper(TexturePaint p, BufferedImage img) {
        Rectangle2D anchorRect = p.getAnchorRect();
        x = anchorRect.getX();
        y = anchorRect.getY();
        w = anchorRect.getWidth();
        h = anchorRect.getHeight();
        transparency = (byte) p.getTransparency();
        image = img;
    }

    private TexturePaintWrapper(double x, double y, double w, double h, BufferedImage img, byte transparency) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.image = img;
        this.transparency = transparency;
    }

    @Override
    public void paint(Graphics2D g) {
        g.setPaint(toPaint());
    }

    @Override
    public Paint toPaint() {
        return get();
    }

    @Override
    public Color toColor() {
        return Color.BLACK;
    }

    public int transparency() {
        return transparency;
    }

    @Override
    public TexturePaintWrapper copy() {
        return new TexturePaintWrapper(x, y, w, h, image, transparency);
    }

    @Override
    public TexturePaintWrapper createScaledInstance(AffineTransform xform) {
        double[] pts = new double[]{
            x, y, x + w, y + h,};
        xform.transform(pts, 0, pts, 0, 2);
        return new TexturePaintWrapper(pts[0], pts[1], pts[2] - pts[0],
                pts[3] - pts[1], image, transparency);
    }

    public TexturePaint get(Function<BufferedImage, BufferedImage> converter) {
        Rectangle2D bds = new Rectangle2D.Double(x, y, w, h);
        return new TexturePaint(converter.apply(image), bds);
    }

    @Override
    public TexturePaint get() {
        BufferedImage img = image;
        Rectangle2D bds = new Rectangle2D.Double(x, y, w, h);
        return new TexturePaint(img, bds);
    }

    @Override
    public int hashCode() {
        long bits = (transparency * 1_201)
                + (11 * doubleToLongBits(x))
                + (257 * doubleToLongBits(y))
                + (733 * doubleToLongBits(w))
                + (3 * doubleToLongBits(h));
        if (rasterHash == 0) {
            rasterHash = rasterHash(image.getRaster());
        }
        bits += (7 * rasterHash);

        return (int) (bits ^ (bits >>> 32));
    }

    private int rasterHash = 0;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (getClass() != obj.getClass()) {
            return false;
        }
        final TexturePaintWrapper other = (TexturePaintWrapper) obj;
        if (other.transparency != transparency) {
            return false;
        } else if (Double.doubleToLongBits(this.x) != Double.doubleToLongBits(other.x)) {
            return false;
        } else if (Double.doubleToLongBits(this.y) != Double.doubleToLongBits(other.y)) {
            return false;
        } else if (Double.doubleToLongBits(this.w) != Double.doubleToLongBits(other.w)) {
            return false;
        } else if (Double.doubleToLongBits(this.h) != Double.doubleToLongBits(other.h)) {
            return false;
        } else {
            // XXX this is hideous.  Cache to and
            // make some effort to do this only once
            BufferedImage a = this.image;
            BufferedImage b = other.image;
            return imagesEqual(a, b);
        }
    }

    private static boolean imagesEqual(BufferedImage a, BufferedImage b) {
        if (a.getWidth() != b.getWidth()) {
            return false;
        } else if (a.getHeight() != b.getHeight()) {
            return false;
        }
        if (a == b) {
            return true;
        }
        int[] ra = rasterArray(a.getRaster());
        int[] rb = rasterArray(b.getRaster());
        return Arrays.equals(ra, rb);
    }

    private static int[] rasterArray(Raster raster) {
        return raster.getPixels(0, 0, raster.getWidth(), raster.getHeight(), (int[]) null);
    }

    private static int rasterHash(Raster raster) {
        return Arrays.hashCode(rasterArray(raster));
    }

    private void setFrame(double x, double y, double w, double h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    @Override
    public void applyTransform(AffineTransform xform) {
        if (xform.isIdentity()) {
            return;
        }
        double[] pts = new double[]{
            x, y, x + w, y + h};
        xform.transform(pts, 0, pts, 0, 2);
        setFrame(pts[0], pts[1], pts[2] - pts[0],
                pts[3] - pts[1]);
    }

    @Override
    public TexturePaintWrapper copy(AffineTransform transform) {
        return createScaledInstance(transform);
    }
}
