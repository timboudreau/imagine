package org.imagine.editor.api.grid;

import com.mastfrog.util.collections.DoubleMap;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Paint;
import java.awt.Rectangle;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_STROKE_CONTROL;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_STROKE_PURE;
import java.awt.TexturePaint;
import java.awt.Transparency;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.Preferences;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.util.NbPreferences;
import org.openide.util.WeakSet;

/**
 * Information about painting a snappable grid over the editor, for tools that
 * want that.
 *
 * @author Tim Boudreau
 */
public final class Grid implements Serializable {

    private static final long serialVersionUID = 10290134;
    private static final Grid INSTANCE = new Grid();
    private int size = 16;
    private boolean enabled = true;
    private Color color = Color.GRAY;
    private GridStyle style = GridStyle.DOTS;
    private transient Set<ChangeListener> listeners;
    private transient TexturePaint linesPaint;
    private transient TexturePaint dotsPaint;

    public Grid(int size, boolean enabled, Color color, GridStyle style) {
        this.size = size;
        this.enabled = enabled;
        this.color = color;
        this.style = style;
    }

    public Grid(int size, boolean enabled) {
        this.size = size;
        this.enabled = enabled;
    }

    public Grid() {

    }

    public Point2D nearestPointTo(double x, double y) {
        int xCount = (int) x / size;
        int yCount = (int) y / size;
        double proposedX = xCount * size;
        double proposedY = yCount * size;
        if (proposedX == x && proposedY == y) {
            return new Point2D.Double(x, y);
        }
        double half = size / 2D;
        double offX = x - proposedX;
        double offY = y - proposedY;
        if (offX > proposedX + half) {
            proposedX += size;
        }
        if (offY > proposedY + half) {
            proposedY += size;
        }
        return new Point2D.Double(proposedX, proposedY);
    }

    public Grid copy() {
        Grid result = new Grid(size, enabled, color, style);
        result.linesPaint = linesPaint;
        result.dotsPaint = dotsPaint;
        return result;
    }

    private Set<ChangeListener> supp() {
        if (listeners == null) {
            listeners = new WeakSet<>(10);
        }
        return listeners;
    }

    /**
     * Get the global instance which is saved automatically.
     *
     * @return A grid
     */
    public static Grid getInstance() {
        return INSTANCE;
    }

    /**
     * The distance in pixels between grid points, no less than 2.
     *
     * @return The size
     */
    public int size() {
        return size;
    }

    /**
     * Determine if the grid should be shown in the ui.
     *
     * @return True if it is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Set the distance between grid points.
     *
     * @param size The distance
     * @throws IllegalArgumentException if the argument is &lt; 2
     */
    public void setSize(int size) {
        if (size < 2) {
            throw new IllegalArgumentException("Size < 2: " + size);
        }
        if (size != this.size) {
            this.size = size;
            fire(true);
        }
    }

    /**
     * Set whether or not the grid should be shown. Editors should listen and
     * repaint when this changes.
     *
     * @param ena Enabled or not
     */
    public void setEnabled(boolean ena) {
        if (ena != enabled) {
            enabled = ena;
            fire(false);
        }
    }

    /**
     * Get the color used for the grid.
     *
     * @return The color
     */
    public Color getColor() {
        return color;
    }

    /**
     * Set the color used for the grid.
     *
     * @param color The color
     */
    public void setColor(Color color) {
        if (color == null) {
            throw new IllegalArgumentException("Null color");
        }
        if (!this.color.equals(color)) {
            this.color = color;
            fire(true);
        }
    }

    private void fire(boolean discardPaints) {
        if (discardPaints) {
            linesPaint = null;
            dotsPaint = null;
            if (linesZoomMap != null) {
                linesZoomMap.clear();
            }
            if (dotsZoomMap != null) {
                dotsZoomMap.clear();
            }
        }
        if (INSTANCE == this) {
            save();
        }
        if (listeners != null) {
            fireChange();
        }
    }

    private void fireChange() {
        if (!listeners.isEmpty()) {
            ChangeEvent evt = new ChangeEvent(this);
            Set<ChangeListener> s = new HashSet<>(listeners);
            for (ChangeListener l : s) {
                l.stateChanged(evt);
            }
        }
    }

    public Paint getGridPaint() {
        switch (style) {
            case DOTS:
                return dotsPaint();
            case LINES:
                return linesPaint();
            default:
                throw new AssertionError(style);
        }
    }

    public Paint getGridPaint(double zoom) {
        switch (style) {
            case DOTS:
                return dotsPaint(zoom);
            case LINES:
                return linesPaint(zoom);
            default:
                throw new AssertionError(style);
        }
    }

    /**
     * Get the preferred painting style, which determines how the grid is
     * painted by the paint returned from getPaint().
     *
     * @param style The grid style
     */
    public GridStyle getStyle() {
        return style;
    }

    /**
     * Set the preferred painting style, which will determine how the grid is
     * painted by the paint returned from getPaint().
     *
     * @param style The grid style
     */
    public void setGridStyle(GridStyle style) {
        if (this.style != style) {
            this.style = style;
            fire(false);
        }
    }

    /**
     * Listen for changes.  Listeners are weakly referenced.
     *
     * @param listener The listener
     */
    public void addChangeListener(ChangeListener listener) {
        supp().add(listener);
    }

    /**
     * Stop listening for changes.
     *
     * @param listener
     */
    public void removeChangeListener(ChangeListener listener) {
        supp().remove(listener);
    }

    public TexturePaint dotsPaint() {
        if (dotsPaint != null) {
            return dotsPaint;
        }
        BufferedImage dots = createDotTextureImage();
        return dotsPaint = new TexturePaint(dots, new Rectangle(
                dots.getWidth(), dots.getHeight()));
    }

    public TexturePaint linesPaint() {
        if (linesPaint != null) {
            return linesPaint;
        }
        BufferedImage lines = createLineTextureImage();
        return linesPaint = new TexturePaint(lines, new Rectangle(
                lines.getWidth(), lines.getHeight()));
    }

    private transient DoubleMap<TexturePaint> linesZoomMap;
    private transient DoubleMap<TexturePaint> dotsZoomMap;

    public TexturePaint linesPaint(double zoom) {
        if (1D == zoom) {
            return linesPaint();
        }
        if (linesZoomMap != null) {
            TexturePaint result = linesZoomMap.get(zoom);
            if (result != null) {
                return result;
            }
        } else {
            linesZoomMap = DoubleMap.create(5);
        }
        TexturePaint result = createLineTextureImage(zoom);
        linesZoomMap.put(zoom, result);
        return result;
    }

    public TexturePaint dotsPaint(double zoom) {
        if (1D == zoom) {
            return dotsPaint();
        }
        if (dotsZoomMap != null) {
            TexturePaint result = dotsZoomMap.get(zoom);
            if (result != null) {
                return result;
            }
        } else {
            dotsZoomMap = DoubleMap.create(5);
        }
        TexturePaint result = createDotTextureImage(zoom);
        dotsZoomMap.put(zoom, result);
        return result;
    }

    private TexturePaint createLineTextureImage(double scale) {
        int mult = 7;
        double sz = size * scale;
        double dim = sz * mult;
        double start = 0;
        BufferedImage img = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .createCompatibleImage((int) Math.ceil(dim), (int) Math.ceil(dim), Transparency.TRANSLUCENT);
        Graphics2D g = img.createGraphics();
        g.setStroke(new BasicStroke(0.5F * (float) scale));
        Rectangle2D.Double rect = new Rectangle2D.Double(sz, sz, Math.max(1, 1 * scale),
                Math.max(1, 1 * scale));
        Line2D.Double line = new Line2D.Double();
        try {
            g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
            g.setRenderingHint(KEY_STROKE_CONTROL, VALUE_STROKE_PURE);
            g.setColor(color);
            for (double x = start; x <= dim + 2; x += sz) {
                double coord = x;
                line.setLine(0, coord, img.getWidth(), coord);
                g.draw(line);
                line.setLine(coord, 0, coord, img.getHeight());
                g.draw(line);
            }
        } finally {
            g.dispose();
        }
        rect.x = 0;
        rect.y = 0;
        rect.width = dim;
        rect.height = dim;
        return new TexturePaint(img, rect);
    }

    private TexturePaint createDotTextureImage(double scale) {
        int mult = 7;
        double sz = size * scale;
        double dim = sz * mult;
        double start = 0;
        BufferedImage img = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .createCompatibleImage((int) Math.ceil(dim), (int) Math.ceil(dim), Transparency.TRANSLUCENT);
        Graphics2D g = img.createGraphics();
        Rectangle2D.Double rect = new Rectangle2D.Double(sz, sz, Math.max(1, 1 * scale),
                Math.max(1, 1 * scale));
        try {
            g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
            g.setRenderingHint(KEY_STROKE_CONTROL, VALUE_STROKE_PURE);
            g.setColor(color);
            double half = scale / 2;
            for (double x = start; x <= dim + 2; x += sz) {
                for (double y = start; y <= dim + 2; y += sz) {
                    rect.x = x - half;
                    rect.y = y - half;
                    g.fill(rect);
                }
            }
        } finally {
            g.dispose();
        }
        rect.x = 0;
        rect.y = 0;
        rect.width = dim;
        rect.height = dim;
        return new TexturePaint(img, rect);
    }

    private BufferedImage createLineTextureImage() {
        int mult = 7;
        int w = size * mult;
        int h = size * mult;
        BufferedImage img = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .createCompatibleImage(w, h, Transparency.TRANSLUCENT);
        Graphics2D g = img.createGraphics();
        try {
            BasicStroke stroke = new BasicStroke(0.5F);
            g.setStroke(stroke);
            g.setColor(color);
            g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
            g.setRenderingHint(KEY_STROKE_CONTROL, VALUE_STROKE_PURE);
            for (int xy = size; xy <= w; xy += size) {
                g.drawLine(xy, 0, xy, h);
                g.drawLine(0, xy, w, xy);
            }
        } finally {
            g.dispose();
        }
        return img;
    }

    private BufferedImage createDotTextureImage() {
        int mult = 7;
        int w = size * mult;
        int h = size * mult;
        BufferedImage img = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .createCompatibleImage(w, h, Transparency.TRANSLUCENT);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
            g.setRenderingHint(KEY_STROKE_CONTROL, VALUE_STROKE_PURE);
            g.setColor(color);
            for (int x = 0; x <= w; x += size) {
                for (int y = 0; y <= w; y += size) {
                    g.fillRect(x - 1, y - 1, 1, 1);
                }
            }
        } finally {
            g.dispose();
        }
        return img;
    }

    public void updateFrom(Grid grid) {
        boolean change = false;
        boolean discardPaints = false;
        if (enabled != grid.isEnabled()) {
            enabled = grid.isEnabled();
            change = true;
        }
        if (size != grid.size()) {
            size = grid.size();
            change = true;
            discardPaints = true;
        }
        if (!color.equals(grid.getColor())) {
            color = grid.getColor();
            change = true;
            discardPaints = true;
        }
        if (change) {
            fire(discardPaints);
        }
    }

    private void save() {
        Preferences prefs = NbPreferences.forModule(Grid.class);
        prefs.putInt("grid", size);
        prefs.putBoolean("gridEnabled", enabled);
        prefs.putInt("gridRed", color.getRed());
        prefs.putInt("gridGreen", color.getGreen());
        prefs.putInt("gridBlue", color.getBlue());
        prefs.putInt("gridStyle", style.ordinal());
    }

    static {
        Preferences prefs = NbPreferences.forModule(Grid.class);
        int sz = prefs.getInt("grid", INSTANCE.size);
        INSTANCE.size = sz;
        boolean ena = prefs.getBoolean("gridEnabled", INSTANCE.enabled);
        INSTANCE.enabled = ena;
        int red = prefs.getInt("gridRed", 128);
        int green = prefs.getInt("gridGreen", 128);
        int blue = prefs.getInt("gridBlue", 128);
        INSTANCE.color = new Color(red, green, blue);
        int gs = prefs.getInt("gridStyle", INSTANCE.style.ordinal());
        if (gs >= 0 && gs < GridStyle.values().length) {
            INSTANCE.style = GridStyle.values()[gs];
        }
    }
}
