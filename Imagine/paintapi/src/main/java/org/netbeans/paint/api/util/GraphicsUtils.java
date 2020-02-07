package org.netbeans.paint.api.util;

import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import static java.awt.RenderingHints.KEY_ALPHA_INTERPOLATION;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_FRACTIONALMETRICS;
import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.KEY_RENDERING;
import static java.awt.RenderingHints.KEY_STROKE_CONTROL;
import static java.awt.RenderingHints.KEY_TEXT_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_ON;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC;
import static java.awt.RenderingHints.VALUE_RENDER_QUALITY;
import static java.awt.RenderingHints.VALUE_STROKE_PURE;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON;
import java.awt.TexturePaint;
import static java.awt.Transparency.TRANSLUCENT;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import org.openide.util.ImageUtilities;

/**
 *
 * @author Tim Boudreau
 */
public final class GraphicsUtils {

    private GraphicsUtils() {
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

    public static BufferedImage newBufferedImage(int w, int h) {
        return new BufferedImage(w, h, DEFAULT_BUFFERED_IMAGE_TYPE);
    }

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

    private static final RenderingHints HQ_HINTS = new RenderingHints(KEY_RENDERING, VALUE_RENDER_QUALITY);

    static {
        HQ_HINTS.put(KEY_RENDERING, VALUE_RENDER_QUALITY);
        HQ_HINTS.put(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        HQ_HINTS.put(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_ON);
        HQ_HINTS.put(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
        HQ_HINTS.put(KEY_STROKE_CONTROL,VALUE_STROKE_PURE);
        HQ_HINTS.put(KEY_FRACTIONALMETRICS, VALUE_FRACTIONALMETRICS_ON);
        HQ_HINTS.put(KEY_ALPHA_INTERPOLATION, VALUE_ALPHA_INTERPOLATION_QUALITY);
    }

    public static void setHighQualityRenderingHints(Graphics2D g) {
        g.setRenderingHints(HQ_HINTS);
    }

    public static Graphics2D noOpGraphics() {
        return new DummyGraphics();
    }

    public static boolean isNoOpGraphics(Graphics g) {
        return g instanceof DummyGraphics;
    }

    public static Composite combine(Composite a, Composite b) {
        return MetaComposite.combine(a, b);
    }

    private static TexturePaint CHECKERBOARD_BACKGROUND;

    public static TexturePaint checkerboardBackground() {
        if (CHECKERBOARD_BACKGROUND == null) {
            CHECKERBOARD_BACKGROUND = new TexturePaint(
                    ((BufferedImage) ImageUtilities.loadImage(
                            "org/netbeans/paint/api/util/backgroundpattern.png")), //NOI18N
                    new Rectangle(0, 0, 16, 16));
        }
        return CHECKERBOARD_BACKGROUND;
    }
}
