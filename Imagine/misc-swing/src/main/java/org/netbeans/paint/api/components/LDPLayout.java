package org.netbeans.paint.api.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.LinkedList;
import java.util.Objects;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JSlider;
import net.java.dev.colorchooser.ColorChooser;

/**
 * A layout manager which gets data a shared ancestor about grid column
 * positions, so multiple LDPLayout components can be placed on a panel and
 * their columns will align.
 *
 * @author Tim Boudreau
 */
public final class LDPLayout implements LayoutManager {

    private static final int DEFAULT_GAP = -1;
    private final int gap;
    private final LinkedList<Reentry> reentry = new LinkedList<>();

    public LDPLayout(int gap) {
        this.gap = gap;
    }

    public LDPLayout() {
        this(DEFAULT_GAP);
    }

    private int gap(Container parent) {
        if (gap < 0) {
            return (int) Math.ceil(parent.getFontMetrics(parent.getFont()).stringWidth("A") * 2);
        }
        return gap;
    }

    private int minRowHeight(Container parent) {
        FontMetrics fm = parent.getFontMetrics(parent.getFont());
        int height = fm.getMaxAscent() + fm.getMaxDescent();
        return (int) Math.ceil(height * 1.5);
    }

    public int getColumnPosition(Container parent, int index) {
        Reentry r = new Reentry(parent, index);
        for (Reentry rr : reentry) {
            if (rr.equals(r)) {
                System.out.println("Use reentry value " + rr.x + " for " + parent);
                return rr.x;
            }
        }
        try {
            reentry.push(r);
            Insets ins = parent.getInsets();
            Component[] comps = parent.getComponents();
            int x = ins.left;
            r.x = x;
            int gap = gap(parent);
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
            return Objects.equals(this.parent, other.parent);
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

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return layoutSize(parent, false);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return layoutSize(parent, true);
    }

    private Dimension layoutSize(Container parent, boolean isMin) {
        Insets ins = parent.getInsets();
        Component[] comps = parent.getComponents();
        int x = ins.left + ins.right;
        int y = ins.top + ins.bottom;
        SharedLayoutData data = SharedLayoutData.find(parent);
        int h = minRowHeight(parent);
        int gap = gap(parent);
        if (data == null) {
            for (Component c : comps) {
                Dimension d = isMin ? c.getMinimumSize() : c.getPreferredSize();
                x += d.width + gap;
                h = Math.max(h, d.height);
            }
        } else {
            for (int i = 0; i < comps.length; i++) {
                int colpos = data.xPosForColumn(i);
                Dimension d = comps[i].getPreferredSize();
                x = colpos + d.width + gap;
                h = Math.max(h, d.height);
            }
        }
        return new Dimension(x, y + h);
    }

    @Override
    public void layoutContainer(Container parent) {
        Insets ins = parent.getInsets();
        Component[] comps = parent.getComponents();
        int x = ins.left;
        int y = ins.top;
        SharedLayoutData data = SharedLayoutData.find(parent);
        int h = minRowHeight(parent);
        int maxBaseline = 0;
        int workingWidth = parent.getWidth() - (ins.left + ins.right);
        int gap = gap(parent);
        for (Component c : comps) {
            Dimension d = c.getPreferredSize();
            int baseline = c.getBaseline(d.width, d.height);
            maxBaseline = Math.max(maxBaseline, baseline);
            h = Math.max(h, d.height);
        }
        if (data == null) {
            for (int i = 0; i < comps.length; i++) {
                Component c = comps[i];
                Dimension d = c.getPreferredSize();
                int baseline = c.getBaseline(d.width, d.height);
                if (baseline < 0) {
                    c.setBounds(x, y, d.width, h);
                } else {
                    int localY = y;
                    if (baseline != maxBaseline) {
                        localY += maxBaseline - baseline;
                    }
                    c.setBounds(x, localY, d.width, d.height);
                }
                x += d.width + gap;
            }
        } else {
            for (int i = 0; i < comps.length; i++) {
                Component c = comps[i];
                if (DEBUG) {
                    ((JComponent) c).setOpaque(true);
                    switch (i) {
                        case 0:
                            c.setBackground(new Color(180, 180, 255));
                            break;
                        case 1:
                            c.setBackground(new Color(255, 180, 180));
                            break;
                        case 2:
                            c.setBackground(new Color(180, 255, 180));
                            break;
                        case 3:
                            c.setBackground(new Color(255, 255, 180));
                            break;
                        case 4:
                            c.setBackground(new Color(255, 180, 255));
                            break;

                    }
                }
                int colpos = data.xPosForColumn(i);
                Dimension d = comps[i].getPreferredSize();
                int baseline = c.getBaseline(d.width, d.height);
                boolean fill = isFillComponent(c);
                if (i == comps.length - 1 && fill) {
                    d.width = workingWidth - colpos;
                } else if (fill) {
                    int next = data.xPosForColumn(i + 1);
                    if (next > colpos && next > colpos + d.width) {
                        d.width = (next - colpos) - gap;
                    }
                }

                if (colpos + d.width > workingWidth - ins.left) {
                    d.width = (ins.left + workingWidth) - (colpos + gap);
                }
                if (baseline < 0) {
                    c.setBounds(colpos, y, d.width, h);
                } else {
                    int localY = y;
                    if (baseline != maxBaseline) {
                        localY += maxBaseline - baseline;
                    }
                    c.setBounds(colpos, localY, d.width, d.height);
                }
            }
        }
    }

    static boolean DEBUG = true;

    private boolean isFillComponent(Component comp) {
        if (comp instanceof JSlider) {
            JSlider sl = (JSlider) comp;
            return !(sl.getUI() instanceof PopupSliderUI);
        }
        if (comp instanceof JComboBox || comp instanceof ColorChooser) {
            return false;
        }
        if (comp instanceof JComponent) {
            Object o = ((JComponent) comp).getClientProperty("noStretch");
            if (o instanceof Boolean) {
                return !(Boolean) o;
            }
        }
        return true;
    }
}
