package org.netbeans.paint.api.cursor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import static java.awt.Toolkit.getDefaultToolkit;
import java.awt.image.BufferedImage;
import static java.lang.Math.max;
import java.util.Objects;
import java.util.function.Consumer;
import static org.netbeans.paint.api.cursor.CursorUtils.applyRenderingHints;

/**
 *
 * @author Tim Boudreau
 */
class CursorPropertiesImpl implements CursorProperties {

    final Color shadow;
    final Color main;
    final int width;
    final int height;
    final boolean darkBackground;
    private final GraphicsConfiguration config;

    CursorPropertiesImpl(Color shadow, Color main, int width, int height, boolean darkBackground, GraphicsConfiguration config) {
        this.shadow = shadow;
        this.config = config;
        this.main = main;
        Dimension d = getDefaultToolkit().getBestCursorSize(width, height);
        this.width = max(4, d.width);
        this.height = max(4, d.height);
        this.darkBackground = darkBackground;
    }

    private int imageWidth() {
        int w = this.width;
        if ((w / 16) * 16 < w) {
            w = max(16, ((1 + (w / 16)) * 16));
        }
        return w;
    }

    private int imageHeight() {
        int height = this.height;
        if ((height / 16) * 16 < height) {
            height = max(16, ((1 + (height / 16)) * 16));
        }
        return height;
    }

    @Override
    public BufferedImage createCursorImage(Consumer<Graphics2D> c) {
        int w = imageWidth();
        int h = imageHeight();
        return CursorUtils.createCursorImage(config, w, h, g -> {
            // where the edges of a white border are antialiased into the
            // color value of the transparent background of the image, which
            // is initially black
            g.setBackground(new Color(shadow.getRed(), shadow.getGreen(), shadow.getBlue(), 0));
            g.setColor(new Color(shadow.getRed(), shadow.getGreen(), shadow.getBlue(), 0));
//            g.clearRect(0, 0, w, h);
//            g.fillRect(0, 0, w, h);
//            System.out.println("clear to " + g.getBackground());
            c.accept(g);
        });
    }

    @Override
    public CursorProperties withSize(int w, int h) {
        return new CursorPropertiesImpl(shadow, main, w, h, darkBackground, config);
    }

    @Override
    public CursorProperties withColors(Color primary, Color shad) {
        return new CursorPropertiesImpl(shad, primary, width, height,
                darkBackground, config);
    }

    @Override
    public Graphics2D hint(Graphics2D g) {
        applyRenderingHints(g);
        return g;
    }

    @Override
    public boolean isDarkBackground() {
        return darkBackground;
    }

    @Override
    public CursorProperties scaled(double by, GraphicsConfiguration config) {
        int w = (int) Math.ceil(width * by);
        int h = (int) Math.ceil(height * by);
        return new CursorPropertiesImpl(shadow, main, w, h,
                darkBackground, config);
    }

    @Override
    public CursorProperties scaled(double by) {
        int w = (int) Math.ceil(width * by);
        int h = (int) Math.ceil(height * by);
        return new CursorPropertiesImpl(shadow, main, w, h,
                darkBackground, config);
    }

    @Override
    public int minimumHollow() {
        return (width / 8) + 1;
    }

    @Override
    public int edgeOffset() {
        return width / 16;
    }

    @Override
    public int cornerOffset() {
        return (width / 8) + 1;
    }

    @Override
    public Color shadow() {
        return shadow;
    }

    @Override
    public Color primary() {
        return main;
    }

    @Override
    public Color attention() {
        return main;
    }

    @Override
    public Color warning() {
        return main;
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public String toString() {
        return "CursorPropertiesImpl{" + "shadow=" + shadow + ", main="
                + main + ", width=" + width + ", height=" + height
                + ", darkBackground=" + darkBackground + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.shadow);
        hash = 29 * hash + Objects.hashCode(this.main);
        hash = 29 * hash + this.width;
        hash = 29 * hash + this.height;
        hash = 29 * hash + (this.darkBackground ? 1 : 0);
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
        final CursorPropertiesImpl other = (CursorPropertiesImpl) obj;
        if (this.config != other.config) {
            return false;
        }
        if (this.width != other.width) {
            return false;
        }
        if (this.height != other.height) {
            return false;
        }
        if (this.darkBackground != other.darkBackground) {
            return false;
        }
        if (!Objects.equals(this.shadow, other.shadow)) {
            return false;
        }
        return Objects.equals(this.main, other.main);
    }
}
