package org.netbeans.paint.api.components;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.LinkedList;
import java.util.Objects;
import javax.swing.SwingUtilities;

/**
 * A layout manager which gets data a shared ancestor about grid column
 * positions, so multiple LDPLayout components can be placed on a panel and
 * their columns will align.
 *
 * @author Tim Boudreau
 */
public final class LDPLayout implements LayoutManager {

    private final int gap;
    private final LinkedList<Reentry> reentry = new LinkedList<>();

    public LDPLayout(int gap) {
        this.gap = gap;
    }

    public LDPLayout() {
        this(5);
    }


    public int getColumnPosition(Container parent, int index) {
        Reentry r = new Reentry(parent, index);
        for (Reentry rr : reentry) {
            if (rr.equals(r)) {
                return r.x;
            }
        }
        try {
            reentry.push(r);
            Insets ins = parent.getInsets();
            Component[] comps = parent.getComponents();
            int x = ins.left + ins.right;
            r.x = x;
            for (int i = 0; i < comps.length; i++) {
                if (i == index) {
                    break;
                }
                x += comps[i].getPreferredSize().width + gap;
                r.x = x;
            }
            return x;
        } finally {
            reentry.pop();
        }
    }

    private static final class Reentry {

        private final Container parent;
        private final int index;
        int x;

        public Reentry(Container parent, int index) {
            this.parent = parent;
            this.index = index;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 97 * hash + Objects.hashCode(this.parent);
            hash = 97 * hash + this.index;
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
            final Reentry other = (Reentry) obj;
            if (this.index != other.index) {
                return false;
            }
            if (!Objects.equals(this.parent, other.parent)) {
                return false;
            }
            return true;
        }

    }

    public void addLayoutComponent(String name, Component comp) {
    }

    public void removeLayoutComponent(Component comp) {
    }

    public Dimension preferredLayoutSize(Container parent) {
        return layoutSize(parent, false);
    }

    public Dimension minimumLayoutSize(Container parent) {
        return layoutSize(parent, true);
    }

    private Dimension layoutSize(Container parent, boolean isMin) {
        Insets ins = parent.getInsets();
        Component[] comps = parent.getComponents();
        int x = ins.left + ins.right;
        int y = ins.top + ins.bottom;
        SharedLayoutData data = (SharedLayoutData) SwingUtilities.getAncestorOfClass(SharedLayoutData.class, parent);
        if (data == null) {
            for (Component c : comps) {
                Dimension d = isMin ? c.getMinimumSize() : c.getPreferredSize();
                x += d.width + gap;
                y = Math.max(y, d.height);
            }
        } else {
            for (int i = 0; i < comps.length; i++) {
                int colpos = data.xPosForColumn(i);
                Dimension d = comps[i].getPreferredSize();
                x = colpos + d.width + gap;
            }
        }
        y = Math.max(30, y);
        return new Dimension(x, y);
    }

    public void layoutContainer(Container parent) {
        Insets ins = parent.getInsets();
        Component[] comps = parent.getComponents();
        int x = ins.left;
        int y = ins.top;
        SharedLayoutData data = (SharedLayoutData) SwingUtilities.getAncestorOfClass(SharedLayoutData.class, parent);
        int h = 0;
        for (Component c : comps) {
            Dimension d = c.getPreferredSize();
            h = Math.max(h, d.height);
        }
        h = Math.max(30, h);
        if (data == null) {
            for (int i = 0; i < comps.length; i++) {
                Component c = comps[i];
                Dimension d = c.getPreferredSize();
                c.setBounds(x, y, d.width, h);
                x += d.width + gap;
            }
        } else {
            for (int i = 0; i < comps.length; i++) {
                int colpos = data.xPosForColumn(i);
                Dimension d = comps[i].getPreferredSize();
                Component c = comps[i];
                c.setBounds(colpos, y, d.width, h);
            }
        }
    }
}
