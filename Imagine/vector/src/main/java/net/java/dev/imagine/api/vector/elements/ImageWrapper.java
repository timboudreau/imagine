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
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import javax.imageio.ImageIO;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.Vector;
import net.java.dev.imagine.api.vector.util.Pt;
import org.imagine.utils.java2d.GraphicsUtils;
import org.openide.util.Exceptions;

/**
 *
 * @author Tim Boudreau
 */
public class ImageWrapper implements Primitive, Vector {

    public transient BufferedImage img;
    public double x;
    public double y;
    public AffineTransform xform;

    public ImageWrapper(RenderedImage img) {
        this.img = getBufferedImage(img, false);
    }

    public ImageWrapper(RenderableImage img) {
        this.img = getBufferedImage(img);
    }

    public ImageWrapper(RenderedImage img, double x, double y) {
        this.img = getBufferedImage(img, false);
        this.x = x;
        this.y = y;
    }

    public ImageWrapper(RenderableImage img, double x, double y) {
        this.img = getBufferedImage(img);
        this.x = x;
        this.y = y;
    }

    public ImageWrapper(double x, double y, Image img) {
        this.img = getBufferedImage(img);
        this.x = x;
        this.y = y;
    }

    public ImageWrapper(AffineTransform xform, Image img) {
        setupCoordinatesAndTransform(0, 0, xform);
        this.img = getBufferedImage(img);
    }

    public ImageWrapper(RenderedImage img, AffineTransform xform, double x, double y) {
        this.img = getBufferedImage(img, false);
        setupCoordinatesAndTransform(x, y, xform);
    }

    public ImageWrapper(RenderedImage img, AffineTransform xform) {
        this.img = getBufferedImage(img, false);
        setupCoordinatesAndTransform(x, y, xform);
    }

    public ImageWrapper(RenderableImage img, AffineTransform xform, double x, double y) {
        this.img = getBufferedImage(img);
        setupCoordinatesAndTransform(x, y, xform);
    }

    public ImageWrapper(RenderableImage img, AffineTransform xform) {
        setupCoordinatesAndTransform(0, 0, xform);
        this.img = getBufferedImage(img);
    }

    public ImageWrapper(double x, double y, AffineTransform xform, Image img) {
        setupCoordinatesAndTransform(x, y, xform);
        this.img = getBufferedImage(img);
    }

    private void setupCoordinatesAndTransform(double x, double y, AffineTransform xform) {
        if (xform != null && !xform.isIdentity()) {
            this.x = x + xform.getTranslateX();
            this.y = y + xform.getTranslateY();
            this.xform = GraphicsUtils.removeTranslation(xform);
        } else {
            this.xform = null;
            this.x = x;
            this.y = y;
        }
    }

    public Runnable restorableSnapshot() {
        BufferedImage oim = img;
        double ox = x;
        double oy = y;
        AffineTransform xf = xform == null ? null
                : new AffineTransform(xform);
        return () -> {
            x = ox;
            y = oy;
            img = oim;
            xform = xf;
        };
    }

    @Override
    public void translate(double x, double y) {
        if (xform != null) {
            double[] offsets = new double[]{x, y};
            try {
                AffineTransform xf = xform.createInverse();
                xf.deltaTransform(offsets, 0, offsets, 0, 1);
                x = offsets[0];
                y = offsets[1];
            } catch (NoninvertibleTransformException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        this.x += x;
        this.y += y;
    }

    public int imageWidth() {
        return img.getWidth();
    }

    public int imageHeight() {
        return img.getHeight();
    }

    @Override
    public void applyTransform(AffineTransform xform) {
        if (xform == null || xform.isIdentity()) {
            return;
        } else {
            if (xform.getType() == AffineTransform.TYPE_TRANSLATION) {
                double[] xy = new double[]{0, 0};
                xform.transform(xy, 0, xy, 0, 1);
                x += xy[0];
                y += xy[1];
                return;
            }
            if (this.xform == null) {
                this.xform = xform;
            } else {
                this.xform.concatenate(xform);
                if (this.xform.isIdentity()) {
                    this.xform = null;
                }
            }
        }
    }

    private static BufferedImage getBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        } else {
            BufferedImage result = new BufferedImage(img.getWidth(null), img.getHeight(null),
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = result.createGraphics();
            g2d.drawImage(img, AffineTransform.getTranslateInstance(0, 0), null);
            g2d.dispose();
            return result;
        }
    }

    private static BufferedImage getBufferedImage(RenderedImage img, boolean force) {
        if (img instanceof BufferedImage && !force) {
            return (BufferedImage) img;
        } else {
            BufferedImage result = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = result.createGraphics();
            g2d.drawRenderedImage(img, AffineTransform.getTranslateInstance(0, 0));
            g2d.dispose();
            return result;
        }
    }

    private static BufferedImage getBufferedImage(RenderableImage img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        } else {
            BufferedImage result = new BufferedImage((int) img.getWidth(), (int) img.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = result.createGraphics();
            g2d.drawRenderableImage(img, AffineTransform.getTranslateInstance(0, 0));
            g2d.dispose();
            return result;
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        ImageIO.write(img, "png", out);
        out.writeDouble(x);
        out.writeDouble(y);
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
        x = in.readDouble();
        y = in.readDouble();
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
        x = 0;
        y = 0;
    }

    @Override
    public String toString() {
        return "ImageWrapper " + img;
    }

    @Override
    public void paint(Graphics2D g) {
        AffineTransform xf = AffineTransform.getTranslateInstance(x, y);
        if (xform != null) {
            xf.concatenate(xform);
        }
        g.drawRenderedImage(img, xf);
    }

    @Override
    public void addToBounds(Rectangle2D bds) {
        double[] pts = new double[]{x, y, x + img.getWidth(),
            y + img.getHeight()};
        if (xform != null) {
            xform.transform(pts, 0, pts, 0, 2);
        }
        if (bds.isEmpty()) {
            bds.setFrameFromDiagonal(pts[0], pts[1], pts[2], pts[3]);
        } else {
            bds.add(pts[0], pts[1]);
            bds.add(pts[2], pts[3]);
        }
    }

    public void getBounds(java.awt.Rectangle r) {
        double[] pts = new double[]{x, y, x + img.getWidth(),
            y + img.getHeight()};
        if (xform != null) {
            xform.transform(pts, 0, pts, 0, 2);
        }
        r.setFrameFromDiagonal(pts[0], pts[1], pts[2], pts[3]);
    }

    @Override
    public ImageWrapper copy() {
        return new ImageWrapper(x, y, xform, img);
    }

    @Override
    public Shape toShape() {
        Rectangle2D.Double result = new Rectangle2D.Double();
        addToBounds(result);
        return result;
    }

    @Override
    public Pt getLocation() {
        return new Pt((int) x, (int) y);
    }

    @Override
    public void setLocation(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void clearLocation() {
        this.x = 0;
        this.y = 0;
    }

    @Override
    public ImageWrapper copy(AffineTransform xform) {
        AffineTransform origXform = this.xform == null ? null : new AffineTransform(this.xform);
        ImageWrapper nue = new ImageWrapper(x, y, origXform, img);
        if (xform != null && !xform.isIdentity()) {
            nue.applyTransform(xform);
        }
        return nue;
    }

    @Override
    public void getBounds(Rectangle2D dest) {
        double[] pts = new double[]{x, y, x + img.getWidth(),
            y + img.getHeight()};
        if (xform != null) {
            xform.transform(pts, 0, pts, 0, 2);
        }
        dest.setFrameFromDiagonal(pts[0], pts[1], pts[2], pts[3]);
    }

    @Override
    public Rectangle getBounds() {
        Rectangle result = new Rectangle();
        getBounds(result);
        return result;
    }

}
