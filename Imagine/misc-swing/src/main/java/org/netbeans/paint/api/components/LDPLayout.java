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
import javax.swing.JButton;
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
            return defaultHorizontalGap(parent);
        }
        return gap;
    }

    public static int defaultHorizontalGap(Container parent) {
        return (int) Math.ceil(parent.getFontMetrics(parent.getFont()).stringWidth("A") * 2);
    }

    private int minRowHeight(Container parent) {
        FontMetrics fm = parent.getFontMetrics(parent.getFont());
        int height = fm.getMaxAscent() + fm.getMaxDescent();
        return (int) Math.ceil(height * 1.5);
    }

    public int getColumnPosition(Container parent, int index) {
        int count = parent.getComponentCount();
        if (index >= count) {
            return -1;
        }
        Reentry r = new Reentry(parent, index);
        for (Reentry rr : reentry) {
            if (rr.equals(r)) {
                return rr.x;
            }
        }
        try {
            reentry.push(r);
            Insets ins = parent.getInsets();
//            SharedLayoutData data = SharedLayoutData.find(parent);
//            if (data != null) {
//                ins.left += data.indentFor(parent);
//            }
            Component[] comps = parent.getComponents();
            int x = ins.left;
            r.x = x;
            int gap = gap(parent);
            for (int i = 0, workingColumn = 0; i < comps.length; i++, workingColumn++) {
                if (!comps[i].isVisible()) {
                    workingColumn--;
                    continue;
                }
                if (workingColumn == index) {
                    break;
                }
                x += Math.max(0, comps[i].getPreferredSize().width) + gap;
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
        return layoutSize(parent, false);
    }

    private Dimension layoutSize(Container parent, boolean isMin) {
        Insets ins = parent.getInsets();
        Component[] comps = parent.getComponents();
        int x = ins.left + ins.right;
        int y = ins.top + ins.bottom;
        SharedLayoutData data = SharedLayoutData.find(parent);
        int h = minRowHeight(parent);
        int gap = gap(parent);
        int maxBaseline = -1;
        for (Component c : comps) {
            if (c.isVisible()) {
                Dimension d = c.getPreferredSize();
                int baseline = c.getBaseline(d.width, d.height);
                maxBaseline = Math.max(maxBaseline, baseline);
                h = Math.max(h, d.height);
            }
        }
        if (data == null) {
            for (int i = 0; i < comps.length; i++) {
                if (i == comps.length - 1) {
                    gap = 0;
                }
                Component c = comps[i];
                if (!c.isVisible()) {
                    continue;
                }
                Dimension d = isMin ? c.getMinimumSize() : c.getPreferredSize();
                x += d.width + gap;
                int baseline = c.getBaseline(d.width, d.height);
                if (baseline <= 0) {
                    x += d.width + gap;
                    y = Math.max(h, d.height);
                } else {
                    y = Math.max(h, d.height + (maxBaseline - baseline));
                }
            }
            for (Component c : comps) {
                Dimension d = isMin ? c.getMinimumSize() : c.getPreferredSize();
                x += d.width + gap;
                h = Math.max(h, d.height);
            }
        } else {
            int ind = 0;
            if (parent instanceof LayoutDataProvider) {
                ind = ((LayoutDataProvider) parent).getIndent();
            }
            int localX = x + ind;
            int globalX = x + ind;
            int height = 0;
            int maxColPos = -2;
            int maxIx = -1;
            for (int i = 0, workingColumn = 0; i < comps.length; i++, workingColumn++) {
                if (!comps[i].isVisible()) {
                    workingColumn--;
                    continue;
                }
                if (i == comps.length - 1) {
                    // Don't add a gap for the last one, or we add right margin
                    // unnecessarily
                    gap = 0;
                }
                int colpos = data.xPosForColumn(workingColumn, parent);
                maxColPos = Math.max(colpos, maxColPos);
                if (colpos > maxColPos) {
                    // XXX once in a great while, we get a higher column with a lower value
                    // than a previous one
                    maxIx = workingColumn;
                }
                Dimension prefsize = isMin ? comps[i].getMinimumSize()
                        : comps[i].getPreferredSize();
                int baseline = comps[i].getBaseline(prefsize.width, prefsize.height);
                if (baseline <= 0) {
                    height = Math.max(height, prefsize.height);
                } else {
                    height = Math.max(height, prefsize.height + (maxBaseline - baseline));
                }
                localX += prefsize.width + gap;
                if (colpos >= 0) {
                    // using Math.max here fixes the case where column B < column A,
                    // which should really get figured out
                    globalX = Math.max(globalX + prefsize.width + gap, colpos + prefsize.width + gap);
                }
            }
            return new Dimension(Math.max(localX, globalX) + 24, height + ins.top + ins.bottom);
//            return new Dimension(Math.max(localX, globalX), height + ins.top + ins.bottom);
        }
        return new Dimension(x, y + h);
    }

    @Override
    public void layoutContainer(Container parent) {
        int indent = 0;
        Insets ins = parent.getInsets();
        Component[] comps = parent.getComponents();
        SharedLayoutData data = SharedLayoutData.find(parent);
        int x = ins.left + indent;
        int y = ins.top;
        int h = minRowHeight(parent);
        int maxBaseline = 0;
        int workingWidth = parent.getWidth() - (ins.left + ins.right);
        int gap = gap(parent);
        for (Component c : comps) {
            if (c.isVisible()) {
                Dimension d = getSanePreferredSize(c);
                int baseline = c.getBaseline(d.width, d.height);
                maxBaseline = Math.max(maxBaseline, baseline);
                h = Math.max(h, d.height);
            }
        }
        if (data == null) {
            for (int i = 0; i < comps.length; i++) {
                Component c = comps[i];
                if (!c.isVisible()) {
                    continue;
                }
                Dimension d = getSanePreferredSize(comps[i]);
                int baseline = c.getBaseline(d.width, d.height);
                if (baseline <= 0) {
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
            int lastX = ins.left;
            int hidden = 0;
            for (int i = 0; i < comps.length; i++) {
                Component c = comps[i];
                if (!c.isVisible()) {
                    hidden++;
                    continue;
                }
                if (i == comps.length - 1) {
                    gap = 0;
                }
                int column = i - hidden;
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
                int colpos = Math.max(lastX, data.xPosForColumn(column, parent));
                if (colpos < 0) {
                    colpos = lastX;
                }
                Dimension d = getSanePreferredSize(comps[i]);
                int baseline = c.getBaseline(d.width, d.height);
                boolean fill = isFillComponent(c);
                if (!fill && i != comps.length - 1) {
                    fill = isInnerFillComponent(c);
                }
                if (i == comps.length - 1 && fill) {
                    int proposedWidth = workingWidth - colpos;
                    if (proposedWidth > d.width) {
                        d.width = proposedWidth;
                    }
                } else if (fill) {
                    int next = data.xPosForColumn(column + 1, parent);
                    if (next >= 0 && next > lastX + d.width + gap) {
                        if (next > colpos && (next - colpos) - gap > d.width) {
                            d.width = (next - colpos) - gap;
                        }
                    }
                }
                if (colpos + d.width > workingWidth - ins.left) {
                    int propsedWidth = (ins.left + workingWidth) - (colpos + gap);
                    if (d.width < propsedWidth) {
                        d.width = (ins.left + workingWidth) - (colpos + gap);
                    }
                }
                if (baseline <= 0) {
                    c.setBounds(colpos, y, d.width, d.height);
                    lastX = colpos + d.width + gap;
                } else {
                    int localY = y;
                    if (baseline != maxBaseline) {
                        localY += maxBaseline - baseline;
                    }
                    c.setBounds(colpos, localY, d.width, d.height);
                    lastX = colpos + d.width + gap;
                }
                if (c.getWidth() == 2147483637) {
                    throw new IllegalStateException("Insane size " + c
                            + " pref size " + c.getPreferredSize() + " at index " + i
                            + " colpos " + colpos + " fill " + fill
                            + " base " + baseline + " min " + c.getMinimumSize()
                    );
                }
            }
        }
    }

    private Dimension getSanePreferredSize(Component c) {
        Dimension d = c.getPreferredSize();
        if (d.width > 10000 || d.height > 10000) {
            Dimension min = c.getMinimumSize();
            d.width = Math.min(min.width, d.width);
            d.height = Math.min(min.height, d.height);
        }
        return d;

    }

    private boolean isInnerFillComponent(Component comp) {
        return comp instanceof JComboBox && !(((JComboBox) comp).getUI() instanceof IconButtonComboBoxUI) //                || comp instanceof JSlider || comp instanceof JButton
                ;
    }

    static boolean DEBUG = false;

    private boolean isFillComponent(Component comp) {
        if (comp instanceof JSlider) {
            JSlider sl = (JSlider) comp;
            return !(sl.getUI() instanceof PopupSliderUI);
        }
        if (comp instanceof JComboBox || comp instanceof ColorChooser || comp instanceof JButton) {
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
