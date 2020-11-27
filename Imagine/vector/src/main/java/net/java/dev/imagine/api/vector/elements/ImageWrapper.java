/*
 * ImageWrapper.java
 *
 * Created on September 27, 2006, 6:53 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.elements;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.util.Objects;
import javax.imageio.ImageIO;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.util.Pt;
import org.imagine.utils.java2d.GraphicsUtils;
import net.java.dev.imagine.api.vector.Vectors;
import static com.mastfrog.geometry.util.GeometryStrings.transformToString;
import com.mastfrog.geometry.util.PooledTransform;

/**
 *
 * @author Tim Boudreau
 */
public class ImageWrapper implements Primitive, Vectors {

    public transient BufferedImage img;
    public AffineTransform xform;

    public ImageWrapper(RenderedImage img) {
        this.img = getBufferedImage(img, false);
    }

    public ImageWrapper(RenderableImage img) {
        this.img = getBufferedImage(img);
    }

    public ImageWrapper(RenderedImage img, double x, double y) {
        this.img = getBufferedImage(img, false);
        this.xform = PooledTransform.getTranslateInstance(x, y, this);
    }

    public ImageWrapper(RenderableImage img, double x, double y) {
        this.img = getBufferedImage(img);
        this.xform = PooledTransform.getTranslateInstance(x, y, this);
    }

    public ImageWrapper(double x, double y, Image img) {
        this.img = getBufferedImage(img);
        this.xform = PooledTransform.getTranslateInstance(x, y, this);
    }

    public ImageWrapper(AffineTransform xform, Image img) {
        this.xform = PooledTransform.copyOf(xform, this);
        this.img = getBufferedImage(img);
    }

    public ImageWrapper(RenderedImage img, AffineTransform xform) {
        this.img = getBufferedImage(img, false);
        this.xform = PooledTransform.copyOf(xform, this);
    }

    public ImageWrapper(RenderableImage img, AffineTransform xform) {
        this.xform = PooledTransform.copyOf(xform, this);
        this.img = getBufferedImage(img);
    }

    @Override
    public Runnable restorableSnapshot() {
        BufferedImage oim = img;
        AffineTransform xf = xform == null ? null
                : new AffineTransform(xform);
        return () -> {
            img = oim;
            xform = PooledTransform.copyOf(xf, this);
        };
    }

    @Override
    public void translate(double x, double y) {
        if (xform == null) {
            xform = PooledTransform.getTranslateInstance(x, y, this);
        } else {
            PooledTransform.withTranslateInstance(x, y, xform::preConcatenate);
//            xform.preConcatenate(AffineTransform.getTranslateInstance(x, y));
        }
    }

    public int imageWidth() {
        return img.getWidth();
    }

    public int imageHeight() {
        return img.getHeight();
    }

    @Override
    public double cumulativeLength() {
        Rectangle2D.Double r = new Rectangle2D.Double();
        getBounds(r);
        return (r.width * 2) + r.height * 2;
    }

    @Override
    public void applyTransform(AffineTransform xform) {
        if (xform != null && !xform.isIdentity()) {
            if (this.xform == null) {
                this.xform = xform;
            } else {
                this.xform.preConcatenate(xform);
            }
        }
    }

    private static BufferedImage getBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            BufferedImage bi = (BufferedImage) img;
            if (bi.getType() == GraphicsUtils.DEFAULT_BUFFERED_IMAGE_TYPE) {
                return (BufferedImage) img;
            }
        }
        return GraphicsUtils.newBufferedImage(img.getWidth(null), img.getHeight(null), g -> {
            g.drawImage(img, null, null);
        });
    }

    private static BufferedImage getBufferedImage(RenderedImage img, boolean forceCopy) {
        if (img instanceof BufferedImage && !forceCopy) {
            BufferedImage bi = (BufferedImage) img;
            if (bi.getType() == GraphicsUtils.DEFAULT_BUFFERED_IMAGE_TYPE) {
                return (BufferedImage) img;
            }
        }
        return GraphicsUtils.newBufferedImage(img.getWidth(), img.getHeight(), g -> {
            g.drawRenderedImage(img, null);
        });
    }

    private static BufferedImage getBufferedImage(RenderableImage img) {
        if (img instanceof BufferedImage) {
            BufferedImage bi = (BufferedImage) img;
            if (bi.getType() == GraphicsUtils.DEFAULT_BUFFERED_IMAGE_TYPE) {
                return (BufferedImage) img;
            }
        }
        return GraphicsUtils.newBufferedImage((int) Math.ceil(img.getWidth()),
                (int) Math.ceil(img.getHeight()), g -> {
            g.drawRenderableImage(img, null);
        });
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        ImageIO.write(img, "png", out);
        boolean writeXform = xform != null && !xform.isIdentity();
        out.writeBoolean(writeXform);
        if (writeXform) {
            double[] mx = new double[6];
            xform.getMatrix(mx);
            for (int i = 0; i < 6; i++) {
                out.writeDouble(mx[i]);
            }
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        img = ImageIO.read(in);
        boolean hasXform = in.readBoolean();
        if (hasXform) {
            double[] mx = new double[6];
            for (int i = 0; i < 6; i++) {
                mx[i] = in.readDouble();
            }
            xform = new AffineTransform(mx);
        }
    }

    private void readObjectNoData() throws ObjectStreamException {
        img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    }

    @Override
    public String toString() {
        return "ImageWrapper " + img + " " + transformToString(xform);
    }

    @Override
    public void paint(Graphics2D g) {
        Object old = g.getRenderingHint(KEY_INTERPOLATION);
        g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
        g.drawRenderedImage(img, xform);
        g.setRenderingHint(KEY_INTERPOLATION, old);
    }

    @Override
    public void addToBounds(Rectangle2D bds) {
        if (bds.isEmpty()) {
            bds.setFrame(toShape().getBounds2D());
        } else {
            bds.add(toShape().getBounds2D());
        }
    }

    public void getBounds(java.awt.Rectangle r) {
        r.setBounds(toShape().getBounds());
    }

    @Override
    public ImageWrapper copy() {
        return new ImageWrapper(new AffineTransform(xform), img);
    }

    @Override
    public Shape toShape() {
        Shape result = new Rectangle2D.Double(0, 0, img.getWidth(), img.getHeight());
        if (xform != null) {
            result = xform.createTransformedShape(result);
        }
        return result;
    }

    @Override
    public Pt getLocation() {
        Rectangle r = getBounds();
        return new Pt(r.x, r.y);
    }

    @Override
    public void setLocation(double x, double y) {
        Rectangle2D.Double r = new Rectangle2D.Double();
        addToBounds(r);
        double offX = x - r.x;
        double offY = y - r.y;
        xform.translate(-offX, -offY);
    }

    @Override
    public void clearLocation() {
        setLocation(0, 0);
    }

    @Override
    public ImageWrapper copy(AffineTransform xform) {
        AffineTransform origXform = this.xform;
        ImageWrapper nue = new ImageWrapper(origXform, img);
        if (xform != null && !xform.isIdentity()) {
            nue.applyTransform(xform);
        }
        return nue;
    }

    @Override
    public void getBounds(Rectangle2D dest) {
        dest.setFrame(toShape().getBounds2D());
    }

    @Override
    public Rectangle getBounds() {
        Rectangle result = new Rectangle();
        getBounds(result);
        return result;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.img);
        hash = 59 * hash + GraphicsUtils.transformHashCode(xform);
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
        final ImageWrapper other = (ImageWrapper) obj;
        return GraphicsUtils.transformsEqual(this.xform, other.xform)
                && Objects.equals(this.img, other.img);
    }
}
