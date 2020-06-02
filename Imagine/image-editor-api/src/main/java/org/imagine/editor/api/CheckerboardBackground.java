/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.editor.api;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import javax.swing.Icon;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Tim Boudreau
 */
@Messages({
    "LIGHT=Light",
    "DARK=Dark",
    "MEDIUM=Medium",})
public enum CheckerboardBackground implements EditorBackground {

    MEDIUM("org/imagine/editor/api/backgroundpattern.png"), //NOI18N
    LIGHT("org/imagine/editor/api/backgroundpattern-light.png"), //NOI18N
    DARK("org/imagine/editor/api/backgroundpattern-dark.png"), //NOI18N
    ;
    private final String resource;
    private TexturePaint paint;
    private Icon icon;
    private static final int LIGHT_ALPHA = 220;
    private static final int DARK_ALPHA = 172;

    CheckerboardBackground(String resource) {
        this.resource = resource;
    }

    public Icon icon() {
        return icon == null ? icon = new EditorBackgroundIcon(this) : icon;
    }

    @Override
    public boolean isBright() {
        return this == LIGHT;
    }

    @Override
    public boolean isDark() {
        return this == DARK;
    }

    @Override
    public boolean isMedium() {
        return this == MEDIUM;
    }

    @Override
    public double meanBrightness() {
        switch (this) {
            case DARK:
                return 0.125;
            case MEDIUM:
                return 0.625;
            case LIGHT:
                return 0.875;
            default:
                throw new AssertionError(this);
        }
    }

    @Override
    public Color contrasting() {
        switch (this) {
            case LIGHT:
                return new Color(10, 10, 10, LIGHT_ALPHA);
            default:
                return new Color(255, 255, 255, DARK_ALPHA);
        }
    }

    @Override
    public Color midContrasting() {
        switch (this) {
            case LIGHT:
                return new Color(80, 80, 80, LIGHT_ALPHA);
            case MEDIUM:
                return new Color(220, 220, 220, LIGHT_ALPHA);
            default:
                return new Color(200, 200, 200, LIGHT_ALPHA);
        }
    }

    @Override
    public Color lowContrasting() {
        switch (this) {
            case LIGHT:
                return new Color(120, 120, 120, LIGHT_ALPHA);
            case MEDIUM:
                return new Color(190, 190, 190, LIGHT_ALPHA);
            default:
                return new Color(150, 150, 150, LIGHT_ALPHA);
        }
    }

    @Override
    public Color nonContrasting() {
        switch (this) {
            case LIGHT:
                return Color.BLACK;
            case DARK:
                return Color.GRAY;
            default:
                return Color.WHITE;
        }
    }

    @Override
    public String toString() {
        return NbBundle.getMessage(CheckerboardBackground.class, name());
    }

    private static final int GRAY_1 = 0x99;
    private static final int GRAY_2 = 0x83;
    private static final int DARK_1 = 0x32;
    private static final int DARK_2 = 0x0f;
    private static final int LIGHT_1 = 0xff;
    private static final int LIGHT_2 = 0xEB;
    private static final int SIZE = 16;

    public int textureSize() {
        return SIZE;
    }

    @Override
    public Paint getPaint() {
        if (paint == null) {
            Color color1, color2;
            switch (this) {
                case DARK:
                    color1 = new Color(DARK_1, DARK_1, DARK_1);
                    color2 = new Color(DARK_2, DARK_2, DARK_2);
                    break;
                case MEDIUM:
                    color1 = new Color(GRAY_1, GRAY_1, GRAY_1);
                    color2 = new Color(GRAY_2, GRAY_2, GRAY_2);
                    break;
                case LIGHT:
                    color1 = new Color(LIGHT_1, LIGHT_1, LIGHT_1);
                    color2 = new Color(LIGHT_2, LIGHT_2, LIGHT_2);
                    break;
                default:
                    throw new AssertionError(this);
            }
            GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
            BufferedImage bi = env.getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(SIZE, SIZE);
            Graphics2D g = env.createGraphics(bi);
            g.setTransform(env.getDefaultScreenDevice().getDefaultConfiguration().getDefaultTransform());
            try {
                g.setColor(color1);
                g.fillRect(0, 0, SIZE, SIZE);
                g.setColor(color2);
                int half = SIZE / 2;
                g.fillRect(half, 0, half, half);
                g.fillRect(0, half, half, half);
            } finally {
                g.dispose();
            }
            int realSize = bi.getWidth();
            String scaleProp = System.getProperty("sun.java2d.uiScale");
            if (scaleProp != null && !"1".equals(scaleProp) && !"1.0".equals(scaleProp)) {
                // Bug workaround - texture paints are scaled completely wrong on Linux
                // if uiScale is set - the returned raster will be 2x the image raster
                // and not tiled
                return new WorkaroundPaint(color1, color2, SIZE);
//                try {
//                    float scale = Float.parseFloat(scaleProp);
////                    realSize = (int) (realSize * (1D / scale));
//                } catch (NumberFormatException nfe) {
//                    Logger.getLogger(CheckerboardBackground.class.getName()).log(Level.INFO,
//                            "sun.java2d.uiScale not a float: " + scaleProp, nfe);
//                }
            }
            System.out.println("Scaled size " + realSize);
            TexturePaint result = new TexturePaint(
                    bi,
                    new Rectangle(realSize, realSize));
            paint = result;
        }
        return paint;
    }

    /**
     * This is SLOWWWW unaccelerated painting.
     */
    static class WorkaroundPaint implements Paint, PaintContext {

        private final Color color1;
        private final Color color2;
        private final int size;
        private BufferedImage cachedImage;

        public WorkaroundPaint(Color color1, Color color2, int size) {
            this.color1 = color1;
            this.color2 = color2;
            this.size = size;
        }

        @Override
        public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) {
            return this;
        }

        @Override
        public int getTransparency() {
            return Transparency.OPAQUE;
        }

        @Override
        public void dispose() {
            // do nothing
        }

        @Override
        public ColorModel getColorModel() {
            return ColorModel.getRGBdefault();
        }

        @Override
        public Raster getRaster(int x, int y, int w, int h) {
            if (x < 0) {
                x = 0;
            }
            if (y < 0) {
                y = 0;
            }
            int iw = w + x;
            int ih = h + y;
            if (cachedImage != null) {
                if (cachedImage.getWidth() >= iw && cachedImage.getHeight() >= ih) {
                    return cachedImage.getSubimage(x, y, w, h).getRaster();
                }
            }
            GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
            cachedImage = env.getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(iw, ih, Transparency.TRANSLUCENT);
            int half = size / 2;
            Graphics2D g = env.createGraphics(cachedImage);
            try {
                g.setColor(color1);
                g.fillRect(0, 0, iw, ih);
                g.setColor(color2);
                for (int xx = 0; xx < (iw / half) + 1; xx++) {
                    boolean oddX = xx % 2 != 0;
                    for (int yy = 0; yy < (ih / half) + 1; yy++) {
                        boolean oddY = yy % 2 == 0;
                        if (oddX != oddY) {
                            int xxx = xx * half;
                            int yyy = yy * half;
                            g.fillRect(xxx, yyy, half, half);
                        }
                    }
                }
            } finally {
                g.dispose();
            }
            return cachedImage.getRaster();
//            return cachedImage.getRaster().createChild(0, 0, w, h, x, y, null);
//            GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().
//            return cachedImage.getSubimage(x, y, w, h).getRaster();
        }
    }

    @Override
    public void fill(Graphics2D g, Rectangle bounds) {
        g.setPaint(getPaint());
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
    }
}
