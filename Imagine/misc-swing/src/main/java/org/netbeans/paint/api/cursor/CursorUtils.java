/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.api.cursor;

import java.awt.Color;
import static java.awt.Color.RGBtoHSB;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import static java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment;
import static java.awt.RenderingHints.KEY_ALPHA_INTERPOLATION;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_COLOR_RENDERING;
import static java.awt.RenderingHints.KEY_DITHERING;
import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.KEY_RENDERING;
import static java.awt.RenderingHints.KEY_STROKE_CONTROL;
import static java.awt.RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_OFF;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_COLOR_RENDER_QUALITY;
import static java.awt.RenderingHints.VALUE_DITHER_ENABLE;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
import static java.awt.RenderingHints.VALUE_RENDER_QUALITY;
import static java.awt.RenderingHints.VALUE_STROKE_NORMALIZE;
import static java.awt.RenderingHints.VALUE_STROKE_PURE;
import static java.awt.Transparency.TRANSLUCENT;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import static org.imagine.geometry.util.PooledTransform.withQuadrantRotateInstance;

/**
 *
 * @author Tim Boudreau
 */
final class CursorUtils {

    static final boolean mac
            = System.getProperty("os.name", "").toLowerCase().contains("mac os")
            || System.getProperty("os.name", "").toLowerCase().contains("darwin")
            || System.getProperty("mrj.version") != null;

    static BufferedImage createCursorImage(int w, int h, Consumer<Graphics2D> c) {
        BufferedImage result = createCursorImage(w, h);
        Graphics2D g = result.createGraphics();
        System.out.println("XF " + g.getTransform());
        try {
            c.accept(g);
        } finally {
            g.dispose();
        }
        return result;
    }

    static BufferedImage createCursorImage(int w, int h) {
        System.out.println("Create cursor image " + w + " x " + h);
        BufferedImage result = getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration()
                .createCompatibleImage(w, h, TRANSLUCENT);
        return result;
    }

    static void twoColorRenderingHints(Graphics2D g) {
        if (mac) {
            g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
            g.setRenderingHint(KEY_ALPHA_INTERPOLATION, VALUE_ALPHA_INTERPOLATION_QUALITY);
            g.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY);
//            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_DEFAULT);
//            g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            g.setRenderingHint(KEY_STROKE_CONTROL, VALUE_STROKE_PURE);
            g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
//            g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
//            g.setRenderingHint(RenderingHints.KEY_RESOLUTION_VARIANT, RenderingHints.VALUE_RESOLUTION_VARIANT_DPI_FIT);
            return;
        }
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(KEY_ALPHA_INTERPOLATION, VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY);
        g.setRenderingHint(KEY_COLOR_RENDERING, VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(KEY_STROKE_CONTROL, VALUE_STROKE_NORMALIZE);
        g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(KEY_DITHERING, VALUE_DITHER_ENABLE);
    }

    static GraphicsConfiguration configFor(Component comp) {
        GraphicsConfiguration config = comp.getGraphicsConfiguration();
        if (config == null) {
            Frame[] fr = Frame.getFrames();
            if (fr != null && fr.length > 0) {
                config = fr[0].getGraphicsConfiguration();
            }
        }
        if (config == null) {
            config = getLocalGraphicsEnvironment().getDefaultScreenDevice()
                    .getDefaultConfiguration();
        }
        return config;
    }

    static BufferedImage rotated(BufferedImage img, int quadrants) {
        BufferedImage nue = new BufferedImage(img.getWidth(), img.getHeight(),
                img.getType());
        withQuadrantRotateInstance(quadrants, img.getWidth() / 2D, img.getHeight() / 2D, xform -> {
            Graphics2D g = (Graphics2D) nue.getGraphics();
            try {
                g.drawImage(img, xform, null);
            } finally {
                g.dispose();
            }
        });
        return nue;
    }

    static boolean isDarker(Color a, Color b) {
        return brightnessOf(a) < brightnessOf(b);
    }

    static float brightnessOf(Color c) {
        float[] hsb = new float[4];
        RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsb);
        return hsb[2];
    }

    private CursorUtils() {
        throw new AssertionError();
    }
}
