package org.netbeans.paint.api.components;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.function.Function;

/**
 * A simple layout that lays components out vertically from top to bottom.
 *
 * @author Tim Boudreau
 */
public final class VerticalFlowLayout implements LayoutManager {

    private final int gap;

    public VerticalFlowLayout() {
        this(-1);
    }

    public VerticalFlowLayout(int gap) {
        this.gap = gap;
    }

    private int gap(Container c) {
        if (gap < 0) {
            FontMetrics fm = c.getFontMetrics(c.getFont());
            return fm.getHeight();
        }
        return gap;
    }

    private int expectedLineHeight(Container c) {
        FontMetrics fm = c.getFontMetrics(c.getFont());
        return fm.getMaxAscent() + fm.getMaxDescent() + 4;
    }

    private Dimension computeSize(Container parent, Function<Component, Dimension> f) {
        Insets ins = parent.getInsets();
        Dimension result = new Dimension();
        Component[] comps = parent.getComponents();
        int gap = gap(parent);
        for (int i = 0; i < comps.length; i++) {
            Component c = comps[i];
            if (!c.isVisible()) {
                continue;
            }
            Dimension ps = f.apply(c);
            result.width = Math.max(result.width, ps.width);
            if (i != comps.length - 1) {
                result.height += ps.height + gap;
            } else {
                result.height += ps.height;
            }
        }
        result.width += ins.left + ins.right;
        result.height += ins.top + ins.bottom;
        return result;
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return computeSize(parent, Component::getPreferredSize);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return computeSize(parent, Component::getMinimumSize);
    }

    @Override
    public void layoutContainer(Container parent) {
        Dimension sz = parent.getSize();
        Dimension pref = preferredLayoutSize(parent);
        Component[] comps = parent.getComponents();
        // If everything won't fit at preferred size,
        // use minimum size for everything and pray
        boolean useMinimum = false;
        if (sz.height < pref.height) {
            useMinimum = true;
        }
        Insets ins = parent.getInsets();
        int x = ins.left;
        int y = ins.top;
        int bottom = sz.height - ins.bottom;
        int lh = expectedLineHeight(parent);
        int gap = gap(parent);
        for (int i = 0; i < comps.length; i++) {
            Component c = comps[i];
            if (!c.isVisible()) {
                continue;
            }
            Dimension ps = useMinimum ? c.getMinimumSize() : c.getPreferredSize();
            if (x + ps.width < sz.width - ins.right) {
                ps.width = (sz.width - ins.right) - x;
            }
            // Give any extra space to the last component
            if (i == comps.length - 1 && y + ps.height < bottom) {
                c.setBounds(x, y, ps.width, bottom - y);
                break;
            }
            int workingGap = gap;
            int anticipatedBottom = y + lh;
            if (y + ps.height < anticipatedBottom) {
                workingGap += anticipatedBottom - (y + ps.height);
            }
            c.setBounds(x, y, ps.width, ps.height);
            y += ps.height + workingGap;
        }
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
        // do nothing
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        // do nothing
    }
}
