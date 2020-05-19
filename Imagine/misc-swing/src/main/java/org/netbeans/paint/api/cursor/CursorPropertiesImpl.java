package org.netbeans.paint.api.cursor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import static java.awt.Toolkit.getDefaultToolkit;
import static java.lang.Math.max;
import java.util.Objects;
import static org.netbeans.paint.api.cursor.CursorUtils.twoColorRenderingHints;

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

    CursorPropertiesImpl(Color shadow, Color main, int width, int height, boolean darkBackground) {
        this.shadow = shadow;
        this.main = main;
        if (width % 16 != 0) {
            width = max(16, (width / 16) * 16);
        }
        if (height % 16 != 0) {
            height = max(16, (height / 16) * 16);
        }
        Dimension d = getDefaultToolkit().getBestCursorSize(width, height);
        this.width = max(4, d.width);
        this.height = max(4, d.height);
        this.darkBackground = darkBackground;
    }

    @Override
    public CursorProperties withSize(int w, int h) {
        return new CursorPropertiesImpl(shadow, main, w, h, darkBackground);
    }

    @Override
    public CursorProperties withColors(Color primary, Color shad) {
        return new CursorPropertiesImpl(primary, shad, width, height, darkBackground);
    }

    @Override
    public Graphics2D hint(Graphics2D g) {
        twoColorRenderingHints(g);
        return g;
    }

    @Override
    public boolean isDarkBackground() {
        return darkBackground;
    }

    @Override
    public CursorProperties scaled(int by) {
        return new CursorPropertiesImpl(shadow, main, width * by, height * by, darkBackground);
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
