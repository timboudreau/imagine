package org.imagine.nbutil.filechooser;

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.EnumSet;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.border.Border;
import org.imagine.nbutil.ComponentUtils;

/**
 * An empty border that self-sizes based on the font size.
 *
 * @author Tim Boudreau
 */
final class FlexEmptyBorder implements Border {

    private final float hfactor;
    private final float vfactor;
    private static final Insets EMPTY = new Insets(0, 0, 0, 0);
    private static final float DEFAULT_HFACTOR = 2F;
    private static final float DEFAULT_VFACTOR = 1.5F;
    private final Set<Side> sides = EnumSet.allOf(Side.class);

    public FlexEmptyBorder(Side... sides) {
        this();
        this.sides.clear();
        for (Side s : sides) {
            this.sides.add(s);
        }
    }

    public FlexEmptyBorder() {
        hfactor = DEFAULT_HFACTOR;
        vfactor = DEFAULT_VFACTOR;
    }

    public FlexEmptyBorder(double hfactor, double vfactor) {
        this((float) hfactor, (float) vfactor);
    }

    public FlexEmptyBorder(float hfactor, float vfactor, Side... sides) {
        this.hfactor = Math.abs(hfactor);
        this.vfactor = Math.abs(vfactor);
        this.sides.clear();
        for (Side s : sides) {
            this.sides.add(s);
        }
    }

    public FlexEmptyBorder(float hfactor, float vfactor) {
        this.hfactor = Math.abs(hfactor);
        this.vfactor = Math.abs(vfactor);
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        // do nothing
    }

    @Override
    public Insets getBorderInsets(Component c) {
        if (c.getParent() instanceof JPanel
                && ((JPanel) c.getParent()).getBorder() instanceof FlexEmptyBorder) {
            return (Insets) EMPTY.clone();
        }
        FontMetrics fm = c.getFontMetrics(ComponentUtils.getFont(c));
        int charWidth = fm.stringWidth("O");
        int lineHeight = fm.getHeight();
        int rightInset;
        int leftInset = rightInset = (int) Math.ceil(charWidth * hfactor);
        int vertInsets = (int) Math.ceil(lineHeight * vfactor);
        Insets result = new Insets(
                sides.contains(Side.TOP) ? vertInsets : 0,
                sides.contains(Side.LEFT) ? leftInset : 0,
                sides.contains(Side.BOTTOM) ? vertInsets : 0,
                sides.contains(Side.RIGHT) ? rightInset : 0);
        return result;
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }

    public enum Side {
        TOP, LEFT, BOTTOM, RIGHT
    }
}
