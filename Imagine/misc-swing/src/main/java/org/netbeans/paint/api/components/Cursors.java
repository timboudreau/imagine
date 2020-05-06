/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.api.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import org.imagine.geometry.Quadrant;
import org.imagine.geometry.Triangle2D;
import org.imagine.geometry.util.PooledTransform;

/**
 * Provides horizontal, vertical, diagonal and circle with line through it
 * cursors.
 *
 * @author Tim Boudreau
 */
public final class Cursors {

    private static Cursors DARK;
    private static Cursors LIGHT;

    private static final boolean DISABLED = Boolean.getBoolean("disable.custom.cursors");
    private final boolean light;
    private final Cursor[] cursors = new Cursor[8];

    private Cursors(boolean light) {
        this.light = light;
    }

    public static Cursors dark() {
        if (DARK == null) {
            DARK = new Cursors(false);
        }
        return DARK;
    }

    public static Cursors light() {
        if (LIGHT == null) {
            LIGHT = new Cursors(true);
        }
        return LIGHT;
    }

    public static void main(String[] args) {
        JFrame jf = new JFrame();
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        JPanel pnl = new JPanel();
        pnl.add(new Comp());
        pnl.setPreferredSize(new Dimension(400, 400));
        jf.setContentPane(pnl);
//        pnl.setBackground(Color.DARK_GRAY);
//        pnl.setForeground(Color.WHITE);
        System.out.println("Use light " + useLight(pnl));
//        jf.setCursor(Cursors.light().southEastNorthWest());
//        jf.setCursor(Cursors.forComponent(pnl).horizontal());
//        jf.setCursor(Cursors.forComponent(pnl).southEastNorthWest());
//        jf.setCursor(Cursors.forComponent(pnl).vertical());
//        jf.setCursor(Cursors.forComponent(pnl).star());
        jf.setCursor(Cursors.forComponent(pnl).x());
        jf.pack();
        jf.setVisible(true);
    }

    /**
     * Get the cursors instance for this component based on its background
     * colors.
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
        boolean useLight = useLight(comp);
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

    private static boolean useLight(JComponent comp) {
        Color bg = comp.getBackground();
        Color fg = comp.getForeground();
        boolean useLight = brightnessOf(fg) > brightnessOf(bg);
        return !useLight;
    }

    private static final String CLIENT_PROP_CURSORS = "angleCursors";

    private static float brightnessOf(Color c) {
        float[] hsb = new float[4];
        Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsb);
        return hsb[2];
    }

    private static void cursors(Cursor[] cursors, boolean light) {

        BufferedImage base_45 = createCursorImage(gg -> {
            drawAngle45(gg, light);
        });

        // diagonal bottom-left to top-right
        cursors[0] = Toolkit.getDefaultToolkit().
                createCustomCursor(base_45, new Point(7, 7), "northwest");
        // diagonal top-left to bottom-right
        cursors[1] = Toolkit.getDefaultToolkit().
                createCustomCursor(rotated(base_45, 1), new Point(7, 7), "northeast");

        BufferedImage base0_90 = createCursorImage(gg -> {
            drawAngle0_90(gg, light);
        });

        // horizontal
        cursors[2] = Toolkit.getDefaultToolkit().
                createCustomCursor(base0_90, new Point(7, 7), "southeast");
        // vertical
        cursors[3] = Toolkit.getDefaultToolkit().
                createCustomCursor(rotated(base0_90, 1), new Point(5, 5), "southwest");

        BufferedImage no = createCursorImage(g -> drawNoImage(g, light));
        // no cursor
        cursors[4] = Toolkit.getDefaultToolkit().
                createCustomCursor(rotated(no, 1), new Point(5, 5), "no");

        BufferedImage starImage = createCursorImage(gg -> drawStarImage(light, gg, 16, 16));
        // diagonal bottom-left to top-right
        cursors[5] = Toolkit.getDefaultToolkit().
                createCustomCursor(starImage, new Point(8, 8), "star");

        BufferedImage xImage = createCursorImage(gg -> drawX(gg, light));
        cursors[6] = Toolkit.getDefaultToolkit().
                createCustomCursor(xImage, new Point(7, 7), "x");

        BufferedImage axImage = createCursorImage(gg -> arrowsX(gg, light));
        cursors[7] = Toolkit.getDefaultToolkit().
                createCustomCursor(axImage, new Point(7, 7), "arrowsInH");

    }

    static final Color LG = new Color(214, 214, 214);

    private static void drawAngle45(Graphics2D g, boolean light1) {
        g.setStroke(new BasicStroke(4));
        g.setColor(light1 ? LG : Color.BLACK);
        g.drawLine(3, 3, 13, 13);
        g.drawLine(2, 2, 2, 6);
        g.drawLine(2, 2, 6, 2);
        g.drawLine(14, 14, 14, 10);
        g.drawLine(14, 14, 10, 14);
        g.setStroke(new BasicStroke(2));
        g.setColor(light1 ? Color.BLACK : LG);
        g.drawLine(3, 3, 13, 13);
        g.drawLine(2, 2, 2, 6);
        g.drawLine(2, 2, 6, 2);
        g.drawLine(14, 14, 14, 10);
        g.drawLine(14, 14, 10, 14);
    }

    private static void drawAngle0_90(Graphics2D g, boolean light1) {
        int start = 2;
        int end = 14;
        g.setColor(light1 ? LG : Color.BLACK);
        g.setStroke(new BasicStroke(4));
        g.drawLine(start + 2, 8, end - 1, 8);
        g.drawLine(start, 8, 4, 6);
        g.drawLine(start, 8, 4, 10);
        g.drawLine(end, 8, 12, 6);
        g.drawLine(end, 8, 12, 10);
        g.setStroke(new BasicStroke(2));
        g.setColor(light1 ? Color.BLACK : LG);
        g.drawLine(start + 2, 8, end - 1, 8);
        g.drawLine(start, 8, 4, 6);
        g.drawLine(start, 8, 4, 10);
        g.drawLine(end, 8, 12, 6);
        g.drawLine(end, 8, 12, 10);
    }

    private static void drawX(Graphics2D g, boolean light) {
        g.setStroke(new BasicStroke(4));
        g.setColor(light ? LG : Color.BLACK);

        g.drawLine(4, 12, 12, 4);
        g.drawLine(4, 4, 12, 12);

        g.setStroke(new BasicStroke(2));
        g.setColor(light ? Color.BLACK : LG);
        g.drawLine(4, 12, 12, 4);
        g.drawLine(4, 4, 12, 12);
    }

    private static void arrowsX(Graphics2D g, boolean light) {

//        Triangle2D tri = new Triangle2D(4, 12, 12, 4, 4, 4);
        Triangle2D tri = new Triangle2D(1, 3, 6, 8, 1, 13);
        Triangle2D tri2 = new Triangle2D(15, 13, 10, 8, 15, 3);

//        g.setStroke(new BasicStroke(6));
//        g.setColor(light ? LG : Color.BLACK);
//        g.draw(tri);
//        g.fill(tri2);
        g.setStroke(new BasicStroke(4));
        g.setColor(light ? Color.BLACK : LG);
        g.fill(tri);
        g.fill(tri2);

        g.setStroke(new BasicStroke(2));
        g.draw(tri);
        g.draw(tri2);
        g.setStroke(new BasicStroke(1.5F));
        g.drawLine(8, 0, 8, 16);
    }

    private static BufferedImage createCursorImage(Consumer<Graphics2D> c) {
        BufferedImage result = createCursorImage();
        Graphics2D g = result.createGraphics();
        try {
            renderingHints(g);
            c.accept(g);
        } finally {
            g.dispose();
        }
        return result;
    }

    private static BufferedImage createCursorImage() {
        return createCursorImage(16, 16);
    }

    private static BufferedImage createCursorImage(int w, int h) {
        BufferedImage starImage = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration()
                .createCompatibleImage(w, h, Transparency.TRANSLUCENT);
        return starImage;

    }

    private static void renderingHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
    }

    private static void drawNoImage(Graphics2D g, boolean light) {
        renderingHints(g);
        Ellipse2D.Double ell = new Ellipse2D.Double(2, 2, 12, 12);
        g.setStroke(new BasicStroke(4));
        g.setColor(light ? LG : Color.BLACK);
        g.drawLine(3, 3, 13, 13);
        g.draw(ell);
        g.setStroke(new BasicStroke(2));
        g.setColor(light ? Color.BLACK : LG);
//        ell.x--;
//        ell.y--;
        g.draw(ell);
        g.drawLine(2, 2, 14, 14);

    }

    static class Comp extends JComponent {

        private final AffineTransform xf = AffineTransform.getScaleInstance(16, 16);

        public Dimension getPreferredSize() {
            Rectangle r = new Rectangle(0, 0, 16, 16);
            return xf.createTransformedShape(r).getBounds().getSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());
            Graphics2D gg = (Graphics2D) g;
            gg.transform(xf);
//            drawStarImage(false, gg, 16, 16);
            drawX(gg, false);
//            drawAngle0_90(gg, false);
//            drawAngle45(gg, true);
        }
    }

    private static void drawStarImage(boolean light, Graphics2D g, int w, int h) {
        g.setStroke(new BasicStroke(2));
        g.setColor(light ? Color.BLACK : Color.LIGHT_GRAY);

        int centerFree = Math.min(w, h) / 8;
        int edgeFree = centerFree / 2;
        int cornerFree = centerFree * 2;

        int cx = (w / 2);
        int cy = (h / 2);
        g.drawLine(cornerFree, cornerFree, cx - centerFree, cy - centerFree);
        g.drawLine(cornerFree, h - cornerFree, cx - centerFree, cy + centerFree);
        g.drawLine(w - cornerFree, h - cornerFree, cx + centerFree, cy + centerFree);
        g.drawLine(w - cornerFree, cornerFree, cx + centerFree, cy - centerFree);
        // horizontal
        g.drawLine(centerFree, cy, cx - (centerFree + edgeFree), cy);
        g.drawLine(w - centerFree, cy, cx + centerFree + edgeFree, cy);
        // horizontal
        g.drawLine(cx, centerFree, cx, cy - (centerFree + edgeFree));
        g.drawLine(cx, cy + centerFree + edgeFree, cx, h - centerFree);

        g.fillRect(cx - edgeFree, cy - edgeFree, centerFree, centerFree);

    }

    public Cursor star() {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        }
        checkInit();
        return cursors[5];
    }

    public Cursor x() {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        }
        checkInit();
        return cursors[6];
    }

    public Cursor hin() {
        if (DISABLED) {
            return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        }
        checkInit();
        return cursors[7];
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
        BufferedImage nue = new BufferedImage(img.getWidth(), img.getHeight(),
                img.getType());
        PooledTransform.withQuadrantRotateInstance(quadrants, img.getWidth() / 2D, img.getHeight() / 2D, xform -> {
            Graphics2D g = (Graphics2D) nue.getGraphics();
            try {
                g.drawImage(img, xform, null);
            } finally {
                g.dispose();
            }
        });
        return nue;
    }
}
