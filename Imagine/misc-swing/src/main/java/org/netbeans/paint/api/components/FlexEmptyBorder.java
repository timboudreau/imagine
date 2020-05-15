package org.netbeans.paint.api.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.Border;

/**
 * An empty border that self-sizes based on the font size.
 *
 * @author Tim Boudreau
 */
public final class FlexEmptyBorder implements Border {

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
        if (TitledPanel2.isDebugLayout()) {
            JComponent jc = (JComponent) c;
            Color color = (Color) jc.getClientProperty("randomBorderColor");
            if (color == null) {
                color = new Color(Color.HSBtoRGB(ThreadLocalRandom.current().nextFloat(), 0.8F, 0.625F));
                color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 180);
                jc.putClientProperty("randomBorderColor", color);
            }
            Insets ins = getBorderInsets(c);
            g.setColor(color);
            g.fillRect(x, y, ins.left, y + height - ins.bottom);
            g.fillRect(x, y + height - ins.bottom, width, ins.bottom);
            g.fillRect(x + ins.left, y, x + width, ins.top);
            g.fillRect(x + width - ins.right, y, x + width, height - ins.bottom);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 14));
            FontMetrics fm = g.getFontMetrics();
            int w = fm.stringWidth(getClass().getSimpleName());
            int h = fm.getMaxAscent() + fm.getMaxDescent();
            if (ins.bottom > 0) {
                g.drawString(getClass().getSimpleName(), x + (width / 2) - (w / 2), y + height - h);
            } else {
                g.drawString(getClass().getSimpleName(), x + (width / 2) - (w / 2), y + fm.getMaxAscent() + 1);
            }
        }
    }

    @Override
    public Insets getBorderInsets(Component c) {
        if (sides.isEmpty() || c.getParent() instanceof JPanel
                && ((JPanel) c.getParent()).getBorder() instanceof FlexEmptyBorder) {
            return (Insets) EMPTY.clone();
        }
        FontMetrics fm = c.getFontMetrics(c.getFont());
        int charWidth = fm.stringWidth("O");
        int lineHeight = fm.getHeight();
        int rightInset;
        int leftInset = rightInset = (int) Math.ceil(charWidth * hfactor);
        int vertInsets = (int) Math.ceil(lineHeight * vfactor);
        if (c instanceof SharedLayoutRootPanel && ((SharedLayoutRootPanel) c).hasExpandables()) {
            leftInset = 0;
        }
        Insets result = new Insets(vertInsets, leftInset, vertInsets, rightInset);
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
