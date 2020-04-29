package org.imagine.editor.api.grid;

import com.mastfrog.util.collections.DoubleMap;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Paint;
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
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.imagine.editor.api.CheckerboardBackground;
import org.imagine.editor.api.ImageEditorBackground;
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
    private transient Map<CheckerboardBackground, DoubleMap<TexturePaint>> linesPaintMapForBackground = new EnumMap<>(CheckerboardBackground.class);
    private transient Map<CheckerboardBackground, DoubleMap<TexturePaint>> dotsPaintMapForBackground = new EnumMap<>(CheckerboardBackground.class);


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
            linesPaintMapForBackground.clear();
            dotsPaintMapForBackground.clear();
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
        return getGridPaint(1);
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
     * Listen for changes. Listeners are weakly referenced.
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

    private DoubleMap<TexturePaint> linesZoomMap(CheckerboardBackground bg) {
        if (linesPaintMapForBackground == null) { // deserialized instance
            linesPaintMapForBackground = new EnumMap<>(CheckerboardBackground.class);
        }
        DoubleMap<TexturePaint> result = linesPaintMapForBackground.get(bg);
        if (result == null) {
            result = DoubleMap.create(5);
            linesPaintMapForBackground.put(bg, result);
        }
        return result;
    }

    private DoubleMap<TexturePaint> dotsZoomMap(CheckerboardBackground bg) {
        if (dotsPaintMapForBackground == null) { // deserialized instance
            dotsPaintMapForBackground = new EnumMap(CheckerboardBackground.class);
        }
        DoubleMap<TexturePaint> result = dotsPaintMapForBackground.get(bg);
        if (result == null) {
            result = DoubleMap.create(5);
            dotsPaintMapForBackground.put(bg, result);
        }
        return result;
    }

    public TexturePaint linesPaint(double zoom) {
        CheckerboardBackground bg = ImageEditorBackground.getDefault().style();
        DoubleMap<TexturePaint> linesZoomMap = linesZoomMap(bg);
        TexturePaint result = linesZoomMap.get(zoom);
        if (result != null) {
            return result;
        }
        result = createLineTextureImage(zoom, bg);
        linesZoomMap.put(zoom, result);
        return result;
    }

    public TexturePaint dotsPaint(double zoom) {
        CheckerboardBackground bg = ImageEditorBackground.getDefault().style();
        DoubleMap<TexturePaint> dotsZoomMap = dotsZoomMap(bg);
        TexturePaint result = dotsZoomMap.get(zoom);
        if (result != null) {
            return result;
        }
        result = createDotTextureImage(zoom, bg);
        dotsZoomMap.put(zoom, result);
        return result;
    }

    private TexturePaint createLineTextureImage(double scale, CheckerboardBackground bg) {
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
            g.setColor(bg.contrasting());
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

    private TexturePaint createDotTextureImage(double scale, CheckerboardBackground bg) {
        int mult = 7;
        double sz = size * scale;
        double dim = sz * mult;
        double start = 0;
        BufferedImage img = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .createCompatibleImage((int) Math.ceil(dim), (int) Math.ceil(dim),
                        Transparency.TRANSLUCENT);
        Graphics2D g = img.createGraphics();
        Rectangle2D.Double rect = new Rectangle2D.Double(sz, sz, Math.max(1, 1 * scale),
                Math.max(1, 1 * scale));
        try {
            g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
            g.setRenderingHint(KEY_STROKE_CONTROL, VALUE_STROKE_PURE);
            g.setColor(bg.contrasting());
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
