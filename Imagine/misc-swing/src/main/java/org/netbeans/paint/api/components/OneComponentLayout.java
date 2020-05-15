package org.netbeans.paint.api.components;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.LayoutManager2;
import java.util.function.Function;

/**
 * Just a dirt-simple layout manager which fills the container with the most
 * recently added child component and sets any others' sizes to zero. Marginally
 * cheaper than BorderLayout, and if the child is a container, proxies its
 * alignment values. Stateless.
 *
 * @author Tim Boudreau
 */
public final class OneComponentLayout implements LayoutManager2 {

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return deriveSize(parent, Component::getPreferredSize);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return deriveSize(parent, Component::getMinimumSize);
    }

    @Override
    public Dimension maximumLayoutSize(Container parent) {
        return deriveSize(parent, Component::getMaximumSize);
    }

    private Dimension deriveSize(Container parent, Function<Component, Dimension> base) {
        int count = parent.getComponentCount();
        if (count == 0) {
            return new Dimension();
        }
        Insets ins = parent.getInsets();
        Dimension result = base.apply(parent.getComponent(count - 1));
        result.width += ins.left + ins.right;
        result.height += ins.top + ins.bottom;
        return result;
    }

    @Override
    public void layoutContainer(Container parent) {
        int count = parent.getComponentCount();
        if (parent.getComponentCount() == 0) {
            return;
        }
        Insets ins = parent.getInsets();
        boolean handled = false;
        for (int i = count - 1; i >= 0; i--) {
            Component c = parent.getComponent(i);
            if (!handled && c.isVisible()) {
                handled = true;
                c.setBounds(ins.left, ins.top,
                        parent.getWidth() - (ins.left + ins.right),
                        parent.getHeight() - (ins.top + ins.bottom));
            } else {
                c.setBounds(-1, -1, 0, 0);
            }
        }
    }

    private Component findVisibleComponent(Container parent) {
        int count = parent.getComponentCount();
        if (count == 0) {
            return null;
        }
        for (int i = count - 1; i >= 0; i--) {
            Component c = parent.getComponent(i);
            if (c.isVisible()) {
                return c;
            }
        }
        return null;
    }

    @Override
    public float getLayoutAlignmentX(Container parent) {
        Component single = findVisibleComponent(parent);
        if (single instanceof Container) {
            LayoutManager layout = ((Container) single).getLayout();
            if (layout instanceof LayoutManager2) {
                LayoutManager2 lm2 = (LayoutManager2) layout;
                return lm2.getLayoutAlignmentX((Container) single);
            }
        }
        return 0;
    }

    @Override
    public float getLayoutAlignmentY(Container parent) {
        Component single = findVisibleComponent(parent);
        if (single instanceof Container) {
            LayoutManager layout = ((Container) single).getLayout();
            if (layout instanceof LayoutManager2) {
                LayoutManager2 lm2 = (LayoutManager2) layout;
                return lm2.getLayoutAlignmentX((Container) single);
            }
        }
        return 0;
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
        // do nothing
    }

    @Override
    public void addLayoutComponent(Component comp, Object constraints) {
        // do nothing
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        // do nothing
    }

    @Override
    public void invalidateLayout(Container target) {
        // do nothing
    }
}
