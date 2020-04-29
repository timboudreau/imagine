package org.imagine.vector.editor.ui.palette;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import javax.swing.border.Border;
import org.imagine.geometry.util.PooledTransform;
import org.imagine.utils.java2d.GraphicsUtils;
import org.openide.util.Exceptions;

/**
 *
 * @author Tim Boudreau
 */
abstract class Tile<T> extends JComponent implements Comparable<Tile<?>> {

    private T item;
    private int lastWidth;
    private int lastHeight;
    private AffineTransform xform;

    Tile(String name) {
        setName(name);
        Color c = UIManager.getColor("control");
        if (c == null) {
            c = Color.LIGHT_GRAY;
        }
        setBackground(c);
        Font f = UIManager.getFont("controlFont");
        if (f == null) {
            f = UIManager.getFont("Label.font");
            if (f == null) {
                f = super.getFont();
                if (f == null) {
                    f = new Font("Times New Roman", Font.PLAIN, 12);
                }
            }
        }
        setToolTipText(name);
        Font ff = f;
        PooledTransform.withScaleInstance(0.8, 0.8, xf -> {
            setFont(ff.deriveFont(xf));
        });
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + item() + ")";
    }

    T item() {
        return item;
    }

    protected void populatePopupMenu(JPopupMenu menu) {

    }

    @Override
    public final void setBackground(Color bg) {
        super.setBackground(bg);
    }

    @Override
    public final void setToolTipText(String text) {
        super.setToolTipText(text);
    }

    @Override
    public final void setName(String name) {
        super.setName(name);
    }

    private void resetTransform() {
        PooledTransform.returnToPool(xform);
        xform = null;
    }

    void setItem(T item) {
        T oldItem = this.item;
//        if (!Objects.equals(oldItem, item)) {
        this.item = item;
        resetTransform();
        onItemLoadedOrReplaced(oldItem, item);
        invalidate();
        revalidate();
        repaint();
//        }
    }

    protected void onItemLoadedOrReplaced(T old, T nue) {
        // update transform, etc.
    }

    @Override
    public void addNotify() {
        super.addNotify();
        resetTransform();
        reload();
    }

    @Override
    public void removeNotify() {
        item = null;
        lastWidth = -1;
        lastHeight = -1;
        resetTransform();
        super.removeNotify();
    }

    @Override
    @SuppressWarnings(value = "deprecation")
    public void reshape(int x, int y, int w, int h) {
        // Yes, it's deprecated; it's also the ONLY sure way
        // to intercept *every* call that changes the component
        // bounds.  Been there, got the t-shirt.
        super.reshape(x, y, w, h);
        if (w != lastWidth || h != lastHeight) {
            resetTransform();
        }
        lastWidth = w;
        lastHeight = h;
    }

    protected final AffineTransform transform() {
        if (xform == null) {
            Insets ins = getInsets();
            int x = ins.left;
            int y = ins.top;
            int w = getWidth() - (ins.left + ins.right);
            int h = getHeight() - (ins.top + ins.bottom);
            if (w <= 0 || h <= 0) {
                return PooledTransform.getTranslateInstance(0, 0, null);
            }
            xform = recomputeTransform(x, y, w, h);
        }
        return xform;
    }

    @Override
    public void setBorder(Border border) {
        xform = null;
        super.setBorder(border);
    }

    private AffineTransform recomputeTransform(int x, int y, int w, int h) {
        AffineTransform result = null;
        if (item != null) {
            AffineTransform nue = recomputeTransform(item, x, y, w, h);
            if (nue != null) {
                result = nue;
            }
        }
        if (result == null) {
            result = PooledTransform.getTranslateInstance(0, 0, null);
        }
        return result;
    }

    /**
     * Compute a transform that positions and scales the item to be drawn within
     * the coordinate space of the content area of this component.
     *
     * @param item The item
     * @param x The leftmost position
     * @param y The topmost position
     * @param w The usable height
     * @param h The usable width
     * @return A transform, or null if no specific transform is needed.
     */
    protected AffineTransform recomputeTransform(T item, double x, double y, double w, double h) {
        return null;
    }

    /**
     * Get the palette storage for this particular item's type; may return null
     * for test usage.
     *
     * @return A palette storage
     */
    protected abstract PaletteBackend<? extends T> storage();

    public void reload() {
        PaletteBackend<? extends T> storage = storage();
        if (storage != null) {
            storage.load(getName(), (thrown, se) -> {
                if (thrown != null) {
                    Exceptions.printStackTrace(thrown);
                } else if (se != null) {
                    setItem(se);
                    return;
                }
                EventQueue.invokeLater(() -> {
                    getParent().remove(this);
                });
            });
        }
    }

    protected void paintBackground(Graphics2D g, int x, int y, int w, int h) {
        g.setPaint(getBackground());
        g.fillRect(x, y, w, h);
    }

    @Override
    public final void paintComponent(Graphics gr) {
        Graphics2D g = (Graphics2D) gr;
        GraphicsUtils.setHighQualityRenderingHints(g);
        int w = getWidth();
        int h = getHeight();
        Insets ins = getInsets();
        int workingWidth = w - (ins.left + ins.right);
        int workingHeight = h - (ins.top + ins.bottom);
        paintBackground(g, ins.left, ins.top, workingWidth, workingHeight);
        if (item != null) {
            AffineTransform old = g.getTransform();
            AffineTransform x = transform();
            if (x != null && !x.isIdentity()) {
                g.transform(x);
            } else {
                x = null;
            }
            paintContent(item, g, ins.left, ins.top, workingWidth, workingHeight);
            if (x != null) {
                try {
                    g.transform(x.createInverse());
                } catch (NoninvertibleTransformException ex) {
                    g.setTransform(old);
                }
            }
        } else {
            paintUnloadedContent(g, ins.left, ins.top, workingWidth, workingHeight);
        }
    }

    protected String getUnloadedText() {
        return "?";
    }

    protected final void paintTextToFit(String s, Graphics2D g, int x, int y, int w, int h) {
        if (s == null || s.trim().isEmpty()) {
            return;
        } else {
            s = s.trim();
        }
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Font f = getFont();
        g.setColor(getForeground());
        FontMetrics fm = g.getFontMetrics(f);
        float fh = fm.getMaxAscent() + fm.getMaxDescent();
        float fw = fm.stringWidth(s);
        float minD = Math.max(fh, fw);
        float minWH = Math.min(w, h) / 2;

        AffineTransform fontXform = PooledTransform.getScaleInstance(minWH / minD, minWH / minD, null);
        f = f.deriveFont(fontXform);
        PooledTransform.returnToPool(fontXform);
        g.setFont(f);
        fw = g.getFontMetrics().stringWidth(s);
        fh = g.getFontMetrics().getMaxAscent() + g.getFontMetrics().getMaxDescent();
        float tx = (w / 2) - (fw / 2);
        float ty = (h / 2) - (fh / 2) + g.getFontMetrics().getAscent();
        g.drawString(s, tx, ty);
    }

    protected void paintUnloadedContent(Graphics2D g, int x, int y, int w, int h) {
        paintTextToFit(getUnloadedText(), g, x, y, w, h);
    }

    protected void paintContent(T item, Graphics2D g, int x, int y, int w, int h) {
    }

    @Override
    public int compareTo(Tile<?> o) {
        String a = o.getName();
        String b = getName();
        if (a == null) {
            return 0;
        }
        if (b == null) {
            return 1;
        }
        return a.compareToIgnoreCase(b);
    }
}
