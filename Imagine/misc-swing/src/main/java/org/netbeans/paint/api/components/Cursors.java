/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.api.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import org.imagine.geometry.Quadrant;

/**
 * Provides horizontal, vertical, diagonal and circle with
 * line through it cursors.
 *
 * @author Tim Boudreau
 */
public final class Cursors {

    private static Cursors DARK;
    private static Cursors LIGHT;

    private static final boolean DISABLED = Boolean.getBoolean("disable.custom.cursors");
    private final boolean light;
    private final Cursor[] cursors = new Cursor[5];

    private Cursors(boolean light) {
        this.light = light;
    }

    /**
     * Get the cursors instance for this component based
     * on its background colors.
     *
     * @param comp
     * @return
     */
    public static Cursors forComponent(JComponent comp) {
        Cursors result = (Cursors) comp.getClientProperty(
                CLIENT_PROP_CURSORS);
        if (result != null) {
            return result;
        }
        Color bg = comp.getBackground();
        Color fg = comp.getForeground();
        boolean useLight = brightnessOf(fg) > brightnessOf(bg);
        if (useLight) {
            if (LIGHT == null) {
                LIGHT = new Cursors(true);
            }
            result = LIGHT;
        } else {
            if (DARK == null) {
                DARK = new Cursors(false);
            }
            result = DARK;
        }
        comp.putClientProperty(CLIENT_PROP_CURSORS, result);
        return result;
    }
    private static final String CLIENT_PROP_CURSORS = "angleCursors";

    private static float brightnessOf(Color c) {
        float[] hsb = new float[4];
        Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsb);
        return hsb[2];
    }

    private static void cursors(Cursor[] cursors, boolean light) {
        BufferedImage base_45 = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration()
                .createCompatibleImage(16, 16, Transparency.TRANSLUCENT);
        Graphics2D g = (Graphics2D) base_45.getGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            g.setStroke(new BasicStroke(3));
            g.setColor(light ? Color.BLACK : Color.LIGHT_GRAY);
            g.drawLine(1, 1, 17, 17);
            g.setStroke(new BasicStroke(2));
            g.setColor(light ? Color.LIGHT_GRAY : Color.BLACK);
            g.drawLine(3, 3, 13, 13);
            g.drawLine(3, 3, 3, 7);
            g.drawLine(3, 3, 7, 3);
            g.drawLine(13, 13, 13, 9);
            g.drawLine(13, 13, 9, 13);
        } finally {
            g.dispose();
        }
        // diagonal bottom-left to top-right
        cursors[0] = Toolkit.getDefaultToolkit().
                createCustomCursor(base_45, new Point(7, 7), "northwest");
        // diagonal top-left to bottom-right
        cursors[1] = Toolkit.getDefaultToolkit().
                createCustomCursor(rotated(base_45, 1), new Point(7, 7), "northeast");

        BufferedImage base0_90 = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration()
                .createCompatibleImage(16, 16, Transparency.TRANSLUCENT);
        g = (Graphics2D) base0_90.getGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            g.setStroke(new BasicStroke(4));
            g.setColor(light ? Color.BLACK : Color.LIGHT_GRAY);
            g.drawLine(0, 7, 16, 7);

            g.setStroke(new BasicStroke(2));
            g.setColor(light ? Color.LIGHT_GRAY : Color.BLACK);
            g.drawLine(0, 8, 16, 8);
            g.drawLine(0, 8, 3, 5);
            g.drawLine(0, 8, 3, 11);
            g.drawLine(3, 5, 3, 11);
            g.drawLine(16, 8, 13, 5);
            g.drawLine(16, 8, 13, 11);
            g.drawLine(13, 5, 13, 11);
        } finally {
            g.dispose();
        }

        // horizontal
        cursors[2] = Toolkit.getDefaultToolkit().
                createCustomCursor(base0_90, new Point(7, 7), "southeast");
        // vertical
        cursors[3] = Toolkit.getDefaultToolkit().
                createCustomCursor(rotated(base0_90, 1), new Point(5, 5), "southeast");

        BufferedImage no = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration()
                .createCompatibleImage(16, 16, Transparency.TRANSLUCENT);

        g = no.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            Ellipse2D.Double ell = new Ellipse2D.Double(2, 2, 14, 14);
            g.setStroke(new BasicStroke(3));
            g.setColor(Color.WHITE);
            g.draw(ell);
            g.setStroke(new BasicStroke(2));
            g.setColor(Color.BLACK);
            ell.x--;
            ell.y--;
            g.draw(ell);
            g.drawLine(0, 0, 16, 16);
        } finally {
            g.dispose();
        }
        // no cursor
        cursors[4] = Toolkit.getDefaultToolkit().
                createCustomCursor(rotated(no, 1), new Point(5, 5), "no");
    }

    public Cursor no() {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        }
        checkInit();
        return cursors[4];
    }

    public Cursor horizontal() {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
        }
        checkInit();
        return cursors[2];
    }

    public Cursor vertical() {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
        }
        checkInit();
        return cursors[3];
    }

    public Cursor southWestNorthEast() {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        }
        checkInit();
        return cursors[1];
    }

    public Cursor southEastNorthWest() {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        }
        checkInit();
        return cursors[0];
    }

    private void checkInit() {
        if (DISABLED) {
            return;
        }
        if (cursors[0] == null) {
            cursors(this.cursors, this.light);
        }
    }

    public Cursor cursorPerpendicularTo(double angle) {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        }
        Quadrant quad = Quadrant.forAngle(angle);
        int quarter = quad.quarter(angle);
        checkInit();
        switch (quad) {
            case NORTHEAST:
            case SOUTHWEST:
                switch (quarter) {
                    case 0:
                        return cursors[2];
                    case 3:
                        return cursors[3];
                }
                break;
            default:
                switch (quarter) {
                    case 0:
                        return cursors[3];
                    case 3:
                        return cursors[2];
                }
        }
        return cursorPerpendicularToQuadrant(quad);
    }

    public Cursor cursorPerpendicularToQuadrant(Quadrant quad) {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        }
        switch (quad) {
            case SOUTHEAST:
            case NORTHWEST:
                return cursors[1];
            case SOUTHWEST:
            case NORTHEAST:
                return cursors[0];
            default:
                throw new AssertionError(quad);
        }
    }

    private static BufferedImage rotated(BufferedImage img, int quadrants) {
        AffineTransform xform = AffineTransform.getQuadrantRotateInstance(
                quadrants, img.getWidth() / 2D, img.getHeight() / 2D);
        BufferedImage nue = new BufferedImage(img.getWidth(), img.getHeight(),
                img.getType());
        Graphics2D g = (Graphics2D) nue.getGraphics();
        try {
            g.drawImage(img, xform, null);
        } finally {
            g.dispose();
        }
        return nue;
    }
}
