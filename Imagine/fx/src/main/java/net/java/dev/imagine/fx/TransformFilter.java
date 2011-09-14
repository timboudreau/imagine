/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.java.dev.imagine.fx;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.util.HashMap;
import java.util.Map;
import org.openide.util.Parameters;

/**
 *
 * @author tim
 */
public abstract class TransformFilter implements BufferedImageOp {
    private AffineTransform xf;
    protected abstract AffineTransform createTransform (BufferedImage src);
    
    protected AffineTransform getTransform(BufferedImage src) {
        if (xf == null) {
            xf = createTransform(src);
        }
        return xf;
    }

    public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
        Rectangle2D r = getBounds2D(src);
        BufferedImage result = new BufferedImage(r.getBounds().width, r.getBounds().height, BufferedImage.TYPE_INT_ARGB);
        return result;
    }

    public BufferedImage filter(BufferedImage src, BufferedImage dest) {
        Parameters.notNull("src", src);
        if (dest == null) {
            dest = createCompatibleDestImage(src, src.getColorModel());
        }
        AffineTransform x = getTransform(src);
        Graphics2D g = dest.createGraphics();
        g.setRenderingHints(getRenderingHints());
        g.drawRenderedImage(src, x);
        g.dispose();
        return dest;
    }

    public Rectangle2D getBounds2D(BufferedImage src) {
        AffineTransform x = getTransform(src);
        return x.createTransformedShape(new Rectangle(0, 0, src.getWidth(), src.getHeight())).getBounds2D();
    }

    public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        return getTransform(null).transform(srcPt, dstPt);
    }

    public RenderingHints getRenderingHints() {
        Map<Key, Object> rh = new HashMap<Key, Object>();
        rh.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        rh.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        rh.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        return new RenderingHints(rh);
    }
    
    public String toString() {
        return "" + xf;
    }
    
}
