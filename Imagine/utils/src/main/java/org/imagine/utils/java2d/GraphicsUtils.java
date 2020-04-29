package org.imagine.utils.java2d;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import static java.awt.RenderingHints.KEY_ALPHA_INTERPOLATION;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_COLOR_RENDERING;
import static java.awt.RenderingHints.KEY_FRACTIONALMETRICS;
import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.KEY_STROKE_CONTROL;
import static java.awt.RenderingHints.KEY_TEXT_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_COLOR_RENDER_QUALITY;
import static java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_ON;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC;
import static java.awt.RenderingHints.VALUE_STROKE_PURE;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON;
import static java.awt.Transparency.TRANSLUCENT;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.imagine.utils.painting.RepaintHandle;

/**
 * Miscellaneous shaoe, painting and Graphics2D related utilities.
 *
 * @author Tim Boudreau
 */
public final class GraphicsUtils {

    private GraphicsUtils() {
        throw new AssertionError();
    }

    /**
     * The default buffered image type - uses ARGB_PRE on Mac and ARGB
     * everywhere else.
     */
    public static final int DEFAULT_BUFFERED_IMAGE_TYPE;

    static {
        BufferedImage img = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(1, 1, TRANSLUCENT);
        DEFAULT_BUFFERED_IMAGE_TYPE = img.getType();
        img.flush();
    }

    /**
     * Create a screen-compatible BufferedImage.
     *
     * @param w The width
     * @param h The height
     * @return
     */
    public static BufferedImage newBufferedImage(int w, int h) {
        assert w > 0 : "Width <= 0";
        assert h > 0 : "Height <= 0";
        return new BufferedImage(w, h, DEFAULT_BUFFERED_IMAGE_TYPE);
    }

    /**
     * Create a new buffered image, create its graphics, pass it to the passed
     * consumer, then dispose it.
     *
     * @param w Width
     * @param h Height
     * @param initializer The consumer
     * @return An image
     */
    public static BufferedImage newBufferedImage(int w, int h, Consumer<Graphics2D> initializer) {
        BufferedImage result = newBufferedImage(w, h);
        Graphics2D g = result.createGraphics();
        try {
            setHighQualityRenderingHints(g);
            initializer.accept(g);
        } finally {
            g.dispose();
        }
        return result;
    }

    /**
     * Set high quality rendering hints on a graphics.
     *
     * @param g A graphics
     */
    public static void setHighQualityRenderingHints(Graphics2D g) {
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(KEY_STROKE_CONTROL, VALUE_STROKE_PURE);
        g.setRenderingHint(KEY_FRACTIONALMETRICS, VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(KEY_ALPHA_INTERPOLATION, VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setRenderingHint(KEY_COLOR_RENDERING, VALUE_COLOR_RENDER_QUALITY);
        // Note:  Setting the general RENDER_QUALITY hint here seriously degrades
        // performance
    }

    /**
     * Create a graphics which will emulate the screen device in its metrics,
     * but paints to nothing.
     *
     * @return A graphics
     */
    public static Graphics2D noOpGraphics() {
        return new DummyGraphics(true);
    }

    /**
     * Create a graphics which will paints to nothing.
     *
     * @return A graphics
     */
    public static Graphics2D noOpGraphics(boolean emulateScreen) {
        return new DummyGraphics(!emulateScreen);
    }

    /**
     * Determine if the passed graphics was created by our no-op graphics method
     * on this class.
     *
     * @param g A graphics
     * @return this
     */
    public static boolean isNoOpGraphics(Graphics g) {
        return g instanceof DummyGraphics;
    }

    /**
     * Create a composite which combines two other composites.
     *
     * @param a A composite
     * @param b Another composite
     * @return A composite
     */
    public static Composite combine(Composite a, Composite b) {
        return MetaComposite.combine(a, b);
    }

    public static AffineTransform transformFor(Point2D oldPosition, Point2D newPosition) {
        double ox = oldPosition.getX();
        double nx = newPosition.getX();
        double oy = oldPosition.getY();
        double ny = newPosition.getY();
        double dx = nx - ox;
        double dy = ny - oy;
        return AffineTransform.getTranslateInstance(dx, dy);
    }

    public static AffineTransform scalingTransform(Dimension old, Dimension nue) {
        return scalingTransform(old.width, old.height, nue.width, nue.height);
    }

    public static AffineTransform scalingTransform(double oldWidth, double oldHeight, double newWidth, double newHeight) {
        if (oldWidth <= 0D || oldHeight <= 0D || (oldWidth == newWidth && oldHeight == newHeight)) {
            return AffineTransform.getTranslateInstance(0, 0);
        }
        double fx = newWidth / oldWidth;
        double fy = newHeight / oldHeight;
        return AffineTransform.getScaleInstance(fx, fy);
    }

    public static TrackingGraphics wrap(RepaintHandle handle, Graphics2D other, int w, int h) {
        return wrap(handle, other, new Point(), w, h);
    }

    public static TrackingGraphics wrap(RepaintHandle handle, Graphics2D other, Point location, int w, int h) {
        return new WrapperGraphics(handle, other, location, w, h);
    }

    public static Graphics2D tee(Graphics2D a, Graphics2D b) {
        return new TeeGraphics(a, b, null);
    }

    public static Graphics2D tee(Graphics2D a, Graphics2D b, Runnable onDispose) {
        return new TeeGraphics(a, b, onDispose);
    }

    public static DoubleBuffer doubleBuffer(Supplier<Graphics2D> g, Supplier<Dimension> size) {
        return new DoubleBuffer(g, size);
    }

    public static Color average(Color a, Color b) {
        float[] hsba = new float[3];
        float[] hsbb = new float[3];
        Color.RGBtoHSB(a.getRed(), a.getGreen(), a.getBlue(), hsba);
        Color.RGBtoHSB(b.getRed(), b.getGreen(), b.getBlue(), hsbb);
        for (int i = 0; i < 3; i++) {
            hsbb[i] = (hsbb[i] + hsba[i]) / 2F;
        }
        int alpha = (a.getAlpha() + b.getAlpha()) / 2;
        int rgb = Color.HSBtoRGB(hsbb[0], hsbb[1], hsbb[2]);
        Color result = new Color(rgb, false);
        if (alpha != 255) {
            result = new Color(result.getRed(), result.getGreen(), result.getBlue(), alpha);
        }
        return result;
    }

    public static Color average(Color[] colors) {
        if (colors.length == 0) {
            return Color.BLACK;
        }
        float[] hsba = new float[3];
        double[] cumulative = new double[3];
        double cumulativeAlpha = 0;
        for (int i = 0; i < colors.length; i++) {
            Color c = colors[i];
            Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsba);
            cumulativeAlpha += c.getAlpha();
            for (int j = 0; j < 3; j++) {
                cumulative[j] += hsba[j];
            }
        }
        double len = colors.length;
        for (int i = 0; i < colors.length; i++) {
            hsba[i] = Math.round(cumulative[i] / len);
        }
        cumulativeAlpha /= len;
        int alpha = (int) Math.max(0, Math.min(255, Math.round(cumulativeAlpha)));
        int rgb = Color.HSBtoRGB(hsba[0], hsba[1], hsba[2]);
        Color result = new Color(rgb, false);
        if (alpha != 255) {
            result = new Color(result.getRed(), result.getGreen(), result.getBlue(), alpha);
        }
        return result;
    }

    /**
     * Transforms a stroke.
     *
     * @param stroke
     * @param xform
     * @return
     */
    public static BasicStroke createTransformedStroke(BasicStroke stroke, AffineTransform xform) {
        if (xform == null || xform.isIdentity()) {
            return stroke;
        }
        int type = xform.getType();
        switch (type) {
            case AffineTransform.TYPE_FLIP:
            case AffineTransform.TYPE_GENERAL_ROTATION:
            case AffineTransform.TYPE_GENERAL_TRANSFORM:
            case AffineTransform.TYPE_TRANSLATION:
                return stroke;
            default:
                if ((type & AffineTransform.TYPE_MASK_SCALE) == 0) {
                    return stroke;
                }
        }
        // Get the scale change as a fraction, i.e. a transform of 0.5, 0.5
        // will return pts[2]-pts[0] = 0.5
        double[] pts = new double[]{0, 0, 1, 1};
        xform.deltaTransform(pts, 0, pts, 0, 2);
        double scaleFactor;
        if (pts[0] == pts[2] && pts[1] == pts[3]) { // uniform scale
            scaleFactor = pts[2] - pts[0];
        } else {
            // Scaling differently horizontally and vertically; and we
            // are scaling a stroke which does not know horizontal and
            // vertical.  There is no right choice here (min? max? avg?),
            // so we just pick a strategy - in this case averaging.
            double xFactor = pts[2] - pts[0];
            double yFactor = pts[3] - pts[1];
            scaleFactor = (xFactor + yFactor) / 2D;
        }
        // better to do the math as floating point, then reduce
        float width = (float) (stroke.getLineWidth() * scaleFactor);
        float[] dash = stroke.getDashArray();
        if (dash != null) {
            for (int i = 0; i < dash.length; i++) {
                dash[i] = (float) (dash[i] * scaleFactor);
            }
        }
        return new BasicStroke(width, stroke.getEndCap(), stroke.getLineJoin(),
                stroke.getMiterLimit(), dash, stroke.getDashPhase());
    }

    public static AffineTransform removeTranslation(AffineTransform xform) {
//        double[] pts = new double[]{0, 0};
//        xform.transform(pts, 0, pts, 0, 1);
//        AffineTransform nue = new AffineTransform(xform);
//        nue.preConcatenate(AffineTransform.getTranslateInstance(-pts[0], -pts[1]));
//        return nue;
        double x = xform.getTranslateX();
        double y = xform.getTranslateY();
        AffineTransform nue = new AffineTransform(xform);
        nue.translate(-x, -y);
        return nue;
    }

    public static int transformHashCode(AffineTransform xform) {
        if (xform == null) {
            return 0;
        }
        double[] mxa = new double[6];
        xform.getMatrix(mxa);
        long hash = 5;
        for (int i = 0; i < 6; i++) {
            double d = mxa[i] + 0.0;
            if (d == -0.0) {
                d = 0.0;
            }
            long val = Double.doubleToLongBits(d);
            hash = hash * 37 + val;
        }
        return (int) (hash ^ (hash >> 32));
    }

    public static boolean transformsEqual(AffineTransform a, AffineTransform b) {
        if (a == null && b == null) {
            return true;
        } else if ((a == null) != (b == null)) {
            return (a != null && a.isIdentity())
                    || (b != null && b.isIdentity());
        }
        double[] mxa = new double[6];
        double[] mxb = new double[6];
        a.getMatrix(mxa);
        b.getMatrix(mxb);
        return doubleArraysEqual(mxa, mxb);
    }

    public static boolean doubleArraysEqual(double[] mxa, double[] mxb) {
        if (mxa.length != mxb.length) {
            return false;
        }
        int len = mxa.length;
        for (int i = 0; i < len; i++) {
            double da = mxa[i];
            double db = mxb[i];
            // avoids problems deserializing to -0.0 for a previous 0.0
            if ((da + 0.0) != (db + 0.0)) {
                return false;
            }
        }
        return true;
    }

    public static boolean doubleArraysEqual(double[] mxa, double[] mxb, double tolerance) {
        if (mxa.length != mxb.length) {
            return false;
        }
        int len = mxa.length;
        for (int i = 0; i < len; i++) {
            double da = mxa[i];
            double db = mxb[i];
            if (Math.abs(da - db) > tolerance) {
                return false;
            }
        }
        return true;
    }
}
