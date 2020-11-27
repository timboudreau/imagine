/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.api.components;

import org.netbeans.paint.api.cursor.Cursors;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import com.mastfrog.geometry.EqLine;
import org.openide.awt.Mnemonics;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
public class TitledPanel2 extends JPanel implements LayoutDataProvider {

    private static boolean debugLayout = false;
    private final ChildPanel centerComponentHolder = new ChildPanel();
    private final JPanel titlePanel = new JPanel(new TitlePanelLayout());
    private final JLabel titleLabel = new JLabel();
    private final JPanel customizeButtonPanel = new JPanel(new IconPanelLayout());
    private final ExpIcon expIcon = new ExpIcon(this);
    private final JButton expandButton = new JButton(expIcon);
    private final JButton customizeButton = new JButton(ImageUtilities.loadImageIcon(
            "org/netbeans/paint/api/components/info.png", true)); //NOI18N
    private final ExpandButtonListener expandListener = new ExpandButtonListener();
    private boolean expanded;
    private final Runnable onCustomize;
    private final Function<Boolean, JComponent> componentFactory;
    private boolean hovered;
    private static final Border EMPTY_BORDER = BorderFactory.createEmptyBorder();
    private static final Border HOLDER_BORDER = BorderFactory.createCompoundBorder(
            VerticalFlowLayout.topGapBorder(),
            SharedLayoutPanel.createIndentBorder());
    private SharedLayoutData data;

    public TitledPanel2(String displayNameKey, Class<?> lookupOn, boolean initiallyExpanded, Function<Boolean, JComponent> componentFactory, Runnable onCustomize) {
        this(NbBundle.getMessage(lookupOn, displayNameKey), initiallyExpanded, componentFactory, onCustomize);
    }

    public TitledPanel2(String displayNameKey, Class<?> lookupOn, boolean initiallyExpanded, Function<Boolean, JComponent> componentFactory) {
        this(NbBundle.getMessage(lookupOn, displayNameKey), initiallyExpanded, componentFactory, null);
    }

    public TitledPanel2(String displayName, boolean initiallyExpanded, Function<Boolean, JComponent> componentFactory) {
        this(displayName, initiallyExpanded, componentFactory, null);
    }

    public TitledPanel2(String displayName, boolean initiallyExpanded, Function<Boolean, JComponent> componentFactory, Runnable onCustomize) {
        setName(displayName);
        this.componentFactory = componentFactory;
        this.onCustomize = onCustomize;
        titlePanel.add(expandButton);
        titlePanel.add(titleLabel);

        Mnemonics.setLocalizedText(titleLabel, displayName);
        titleLabel.setLabelFor(expandButton);
        centerComponentHolder.setBorder(HOLDER_BORDER);
        customizeButton.setContentAreaFilled(false);
        customizeButton.setBorder(EMPTY_BORDER);

        expandButton.setBorder(EMPTY_BORDER);
        expandButton.setContentAreaFilled(false);
        expandButton.addMouseListener(expandListener);
        expandButton.addMouseMotionListener(expandListener);
        expandButton.addActionListener(expandListener);

        centerComponentHolder.addMouseListener(expandListener);
        titleLabel.addMouseMotionListener(expandListener);
        titleLabel.addMouseListener(expandListener);
        if (onCustomize != null) {
            customizeButtonPanel.add(customizeButton);
            customizeButton.addActionListener(ae -> {
                onCustomize.run();
            });
            titlePanel.add(customizeButtonPanel);
        }
        add(titlePanel);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        onExpandedChanged(initiallyExpanded);
        if (debugLayout) {
            initDebugLayout();
        }
    }

    private void initDebugLayout() {
        // Colorize everything randomly and produce debug output on expansion
        float[] floats = new float[]{0.47F, 0.625F, 0.8725F};
        // A supplier of random colors that avoids adjacent similar ones
        Supplier<Color> supp = () -> {
            Color result = new Color(Color.HSBtoRGB(floats[0], floats[1], floats[2]));
            floats[0] += 0.23F;
            if (floats[0] > 1) {
                floats[0] -= 1;
            }
            return result;
        };
        setBackground(supp.get());
        expandButton.setContentAreaFilled(true);
        customizeButton.setContentAreaFilled(true);
        expandButton.setBackground(supp.get());
        titleLabel.setOpaque(true);
        titleLabel.setBackground(supp.get());
        titlePanel.setBackground(supp.get());
        centerComponentHolder.setBackground(supp.get());
        customizeButton.setBackground(supp.get());
        customizeButtonPanel.setBackground(supp.get());
    }

    static boolean isDebugLayout() {
        return debugLayout;
    }

    public static void debugLayout(boolean debug) {
        debugLayout = true;
    }

    public boolean showCustomizer() {
        if (onCustomize != null) {
            onCustomize.run();
            return true;
        }
        return false;
    }

    SharedLayoutData dataUnsafe() {
        return data;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        data = SharedLayoutData.find(this);
        if (data != null) {
            data.register(this);
        }
    }

    @Override
    public void removeNotify() {
        data = SharedLayoutData.find(this);
        if (data != null) {
            data.unregister(this);
            data = null;
        }
        super.removeNotify();
    }

    public void setExpanded(boolean val) {
        if (val != expanded) {
            expanded = val;
            onExpandedChanged(val);
        }
    }

    public TitledPanel2 setTitle(String title) {
        titleLabel.setText(title);
        invalidate();
        revalidate();
        repaint();
        return this;
    }

    private JComponent center;
    private Border centerOldBorder;

    private void onExpandedChanged(boolean val) {
        if (data != null) {
            data.expanded(this, val);
        }
        if (!val) {
            if (center != null) {
                remove(centerComponentHolder);
                centerComponentHolder.removeAll();
                center = null;
            }
            if (onCustomize != null) {
                titlePanel.remove(customizeButtonPanel);
            }
            expandButton.setCursor(Cursors.forComponent(this).triangleDown());
            titleLabel.setCursor(Cursors.forComponent(this).triangleDown());
        } else {
            if (center != null) {
                titlePanel.remove(center);
                if (centerOldBorder != null) {
                    center.setBorder(centerOldBorder);
                    centerOldBorder = null;
                }
                center = null;
            }
            if (onCustomize != null) {
                titlePanel.add(customizeButtonPanel);
            }
            expandButton.setCursor(Cursors.forComponent(this).triangleRight());
            titleLabel.setCursor(Cursors.forComponent(this).triangleRight());
        }
        center = componentFactory.apply(val);
        if (center != null) {
            if (val) {
                if (!(center instanceof SharedLayoutPanel)) {
                    centerOldBorder = center.getBorder();
                    center.setBorder(EMPTY_BORDER);
                } else {
                    centerOldBorder = null;
                }
                centerComponentHolder.add(center, BorderLayout.CENTER);
                add(centerComponentHolder);
            } else {
                centerOldBorder = null;
                titlePanel.add(center);
            }
        }
        expIcon.setExpanded(val);
        expandButton.repaint();
        invalidate();
        revalidate();
        repaint();
        if (debugLayout) {
            performDebugLogging();
        }
    }

    private void performDebugLogging() {
        System.out.println("TitledPanel talking to \n" + data);
    }

    private void setHovered(boolean val) {
        if (val != hovered) {
            hovered = val;
            expIcon.setHovered(val);
            expandButton.repaint();
            if (expanded) {
                centerComponentHolder.repaintLineArea();
            }
            if (hovered) {
                centerComponentHolder.setCursor(
                        Cursors.forComponent(TitledPanel2.this).triangleRight());
            } else {
                centerComponentHolder.setCursor(Cursor.getDefaultCursor());
            }
        }
    }

    /**
     * Indents appropriately, and draws the expanded line as needed.
     */
    static final class ChildPanel extends JPanel {

        private final EqLine scratchLine = new EqLine();
        private int lastIndent;

        ChildPanel() {
            super(new OneComponentLayout());
            HoverML ml = new HoverML();
            addMouseListener(ml);
            addMouseMotionListener(ml);
            // layout/border debug
            setName("TitledPanel-ChildPanel"); //NOI18N
        }

        boolean testFirstChild(Predicate<Component> pred) {
            int count = getComponentCount();
            return count == 0 ? false : pred.test(getComponent(0));
        }

        void repaintLineArea() {
            repaint(0, 0, lastIndent, getHeight());
        }

        class HoverML extends MouseAdapter {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getX() <= lastIndent) {
                    TitledPanel2 pnl = ancestor();
                    if (pnl != null) {
                        pnl.setExpanded(!pnl.isExpanded());
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                TitledPanel2 pnl = ancestor();
                if (pnl != null) {
                    pnl.setHovered(false);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                mouseMoved(e);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (e.getX() <= lastIndent) {
                    TitledPanel2 pnl = ancestor();
                    if (pnl != null) {
                        pnl.setHovered(true);
                        repaintLineArea();
                    }
                } else {
                    TitledPanel2 pnl = ancestor();
                    if (pnl != null) {
                        pnl.setHovered(false);
                        repaintLineArea();
                    }
                }
            }
        }

        private TitledPanel2 ancestor() {
            return (TitledPanel2) SwingUtilities.getAncestorOfClass(TitledPanel2.class, this);
        }

        @Override
        public void addNotify() {
            super.addNotify();
            lastIndent = 0;
        }

        @Override
        protected void paintChildren(Graphics g) {
            super.paintChildren(g);
            TitledPanel2 pnl = ancestor();
            if (pnl != null && pnl.isExpanded()) {
                paintExpandedLine((Graphics2D) g, pnl);
            } else {
                lastIndent = 0;
            }
        }

        protected void paintExpandedLine(Graphics2D g, TitledPanel2 pnl) {
            g.setStroke(new BasicStroke(1.25F, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_MITER, 1F, new float[]{4, 3}, 0F));
            if (pnl.hovered) {
                g.setColor(Color.BLUE);
            } else {
                g.setColor(UIManager.getColor("controlDkShadow"));
            }

            Rectangle bds = pnl.expandButton.getBounds();

            bds = SwingUtilities.convertRectangle(pnl, bds, this);
            lastIndent = bds.x + bds.width;

            double xTop = bds.getCenterX();
            double yTop = 0;
            Insets ins = getInsets();
            int h = getHeight();
            double yBottom = h - (ins.bottom + 1);
            scratchLine.setLine(xTop, yTop, xTop, yBottom);
            g.draw(scratchLine);
            double xRight = bds.x + bds.width;
            scratchLine.x1 = xRight;
            scratchLine.y1 = scratchLine.y2;
            g.draw(scratchLine);
        }
    }

    JComponent center() {
        return center;
    }

    @Override
    public int getColumnPosition(int col) {
        switch(col) {
            case 0 :
                return getIndent();
            case 1 :
                return getIndent() + titleLabel.getPreferredSize().width
                        + (LDPLayout.defaultHorizontalGap(this));
        }
        return -1;
    }

    @Override
    public boolean isExpanded() {
        return expanded;
    }

    @Override
    public void doSetExpanded(boolean val) {
        setExpanded(val);
    }

    @Override
    public int getIndent() {
//        return expandButton.getPreferredSize().width + LDPLayout.defaultHorizontalGap(this);
        return expandButton.getPreferredSize().width + (LDPLayout.defaultHorizontalGap(this));
    }

    @Override
    public void doLayout() {
        // Another layout manager? Meh...
        Insets ins = getInsets();
        int w = getWidth();
        int h = getHeight();
        Dimension tp = customizeButton.isDisplayable()
                ? titleLabel.getPreferredSize()
                : titlePanel.getPreferredSize();
        titlePanel.setBounds(ins.top, ins.left,
                w - (ins.left + ins.right), tp.height);
        boolean hasExtraComponents = hasExtraComponents();
        int yy = ins.top + tp.height;
        if (expanded && center != null) {
            int y = yy;
            int centerHeight = h - (y + ins.bottom);
            int bottom = h - (y + ins.bottom);

            if (hasExtraComponents) {
                centerHeight = centerComponentHolder.getPreferredSize().height;
                bottom = y + centerHeight;
            }
            centerComponentHolder.setBounds(ins.left, y,
                    w - (ins.left + ins.right), centerHeight);
            yy = bottom;
        }
        if (hasExtraComponents) {
            if (expanded) {
                int vgap = VerticalFlowLayout.defaultGap(this);
                yy += vgap;
                for (Component c : getComponents()) {
                    if (isBuiltInComponent(c) || !c.isVisible()) {
                        continue;
                    }
                    Dimension d = c.getPreferredSize();
                    int ww = Math.min(w - (ins.left + ins.right), d.width);
                    c.setBounds(ins.left, yy, ww, d.height);
                    yy += vgap + d.height;
                }
            } else {
                for (Component c : getComponents()) {
                    if (!isBuiltInComponent(c)) {
                        c.setBounds(-1, -1, 0, 0);
                    }
                }
            }
        }
    }

    private boolean isBuiltInComponent(Component c) {
        return c == titlePanel || c == centerComponentHolder;
    }

    private boolean hasExtraComponents() {
        if (isExpanded()) {
            return getComponentCount() > 2;
        } else {
            return getComponentCount() > 1;
        }
    }

    private Dimension combine(IntBinaryOperator op, Dimension nue, Dimension into, int gap) {
        into.width = op.applyAsInt(nue.width, into.width);
        into.height += gap + nue.height;
        return into;
    }

    private Dimension size(IntBinaryOperator combiner, Function<Component, Dimension> minMaxPreferred) {
        int gap = VerticalFlowLayout.defaultGap(this);
        Dimension result = minMaxPreferred.apply(titlePanel);
        if (expanded && centerComponentHolder.isDisplayable()) {
            combine(combiner, minMaxPreferred.apply(centerComponentHolder), result, gap);
        }
        if (isExpanded() && hasExtraComponents()) {
            Component[] all = getComponents();
            for (int i = 0; i < all.length; i++) {
                Component c = all[i];
                if (isBuiltInComponent(c) || !c.isVisible()) {
                    continue;
                }
                Dimension d = minMaxPreferred.apply(c);
                combine(combiner, d, result, i == all.length - 1 ? 0 : gap);
            }
        }
        Insets ins = getInsets();
        result.width += ins.left + ins.right;
        result.height += ins.right + ins.bottom;
        return result;
    }

    @Override
    public Dimension getPreferredSize() {
        return size(Math::max, Component::getPreferredSize);
    }

    @Override
    public Dimension getMinimumSize() {
        return size(Math::max, Component::getMinimumSize);
    }

    @Override
    public Dimension getMaximumSize() {
        return size(Math::min, Component::getMaximumSize);
    }

    private static final class TitlePanelLayout implements LayoutManager2 {

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
            Dimension result = size(Math::max, parent, Component::getPreferredSize);
            return result;
        }

        @Override
        public void layoutContainer(Container parent) {
            SharedLayoutData data = SharedLayoutData.find(parent);
            Insets ins = parent.getInsets();
            int w = parent.getWidth();
            int h = parent.getHeight();
            int left = data == null ? 0 : data.xPosForColumn(0, parent);
            int gap = LDPLayout.defaultHorizontalGap(parent);
            Component[] comps = parent.getComponents();
            int x = left;
            int y = ins.top;
            int maxBaseline = 0;
            int workingWidth = w - (ins.left + ins.right);
            int workingHeight = h - (ins.top + ins.bottom);
            for (Component c : comps) {
                if (c.isVisible()) {
                    Dimension d = c.getPreferredSize();
                    int baseline = c.getBaseline(d.width, d.height);
                    maxBaseline = Math.max(maxBaseline, baseline);
                    h = Math.max(h, d.height);
                }
            }
            int localY;
            for (int i = 0; i < comps.length; i++) {
                Component c = comps[i];
                if (!c.isVisible()) {
                    continue;
                }
                Dimension d = c.getPreferredSize();
                switch (i) {
                    case 0:
                        int availWidth = left - (ins.left + LDPLayout.defaultHorizontalGap(parent));
                        if (availWidth <= 0) {
                            availWidth = d.width;
                        } else {
                            d.width = availWidth;
                            d.height = availWidth;
                        }
                        localY = y;
                        if (d.height < workingHeight) {
                            localY += (workingHeight - d.height) / 2;
                        }
                        //adjustForBaseline(c, maxBaseline, y, d);
                        d.height = Math.min(d.height, workingHeight);
//                            d1.height = Math.max(d1.height, h);
                        c.setBounds(ins.left, localY, availWidth, d.height);
                        x += availWidth + gap;
                        break;
                    default:
                        if (data != null) {
                            int colpos = data.xPosForColumn(i - 1, parent);
                            if (colpos != -1) {
                                x = colpos;
                            }
                        }
                        if (i == comps.length - 1) {
                            if (c instanceof JPanel) {
                                c.setBounds(x, y, workingWidth - x, workingHeight);
                                return;
                            }
                        }
                        if (!(c instanceof JComboBox) && !(c instanceof JSlider)) {
                            d.height = workingHeight;
                            localY  = y;
                        } else {
                            localY = adjustForBaseline(c, maxBaseline, y, d);
                        }
                        c.setBounds(x, localY, d.width, d.height);
                        x += d.width + gap;
                }
            }
        }

        private int adjustForBaseline(Component c, int maxBaseline, int y, Dimension d) {
            if (maxBaseline <= 0) {
                return y;
            }
            int baseline = c.getBaseline(d.width, d.height);
            if (baseline <= 0) {
                return y;
            }
            int localY = y;
            if (baseline != maxBaseline) {
                localY += maxBaseline - baseline;
            }
            return localY;
        }

        private Dimension size(IntBinaryOperator heightCombiner, Container parent, Function<Component, Dimension> f) {
            SharedLayoutData data = SharedLayoutData.find(parent);
            Insets ins = parent.getInsets();

            int left = data == null ? 0 : data.xPosForColumn(0, parent);
            int gap = LDPLayout.defaultHorizontalGap(parent);
            Component[] comps = parent.getComponents();
            int x = left;
            int y = ins.top;
            int maxBaseline = 0;
            int h = 0;
            for (Component c : comps) {
                if (c.isVisible()) {
                    Dimension d = f.apply(c);
                    int baseline = c.getBaseline(d.width, d.height);
                    maxBaseline = Math.max(maxBaseline, baseline);
                    h = heightCombiner.applyAsInt(h, d.height);
                }
            }
            int maxX = 0;
            int maxY = 0;
            for (int i = 0; i < comps.length; i++) {
                Component c = comps[i];
                if (!c.isVisible()) {
                    continue;
                }
                switch (i) {
                    case 0:
                        // Here, we want to position the icon button *before* the first
                        // column position from the shared layout data
                        int availWidth = left - ins.left;
                        int ht = c.getPreferredSize().height;
                        x += availWidth;
                        maxY = heightCombiner.applyAsInt(maxY, y + ht);
                        maxX = left + gap;
                        break;
                    default:
                        Dimension d = f.apply(c);
                        int localY = adjustForBaseline(c, maxBaseline, y, d);
                        maxY = heightCombiner.applyAsInt(maxY, localY + d.height);
                        maxX = x + d.width + gap;
                        if (data != null) {
                            int colpos = data.xPosForColumn(i - 1, parent);
                            if (colpos > 0) {
                                maxX = Math.max(maxX, colpos + d.width + gap);
                            }
                        }
                }
            }
            return new Dimension(maxX + ins.left + ins.right, maxY + ins.bottom + ins.top);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return size(Math::max, parent, Component::getMinimumSize);
        }

        @Override
        public void addLayoutComponent(Component comp, Object constraints) {
            // do nothing
        }

        @Override
        public Dimension maximumLayoutSize(Container parent) {
            // note use of Math.min, not max here
            return size(Math::min, parent, Component::getMaximumSize);
        }

        @Override
        public float getLayoutAlignmentX(Container target) {
            return 0;
        }

        @Override
        public float getLayoutAlignmentY(Container target) {
            return 0.5F; // ??
        }

        @Override
        public void invalidateLayout(Container target) {
            // do nothing
        }
    }

    private static final class IconPanelLayout implements LayoutManager2 {

        @Override
        public void addLayoutComponent(String name, Component comp) {

        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            Insets ins = parent.getInsets();
            Dimension d = new Dimension(ins.left + ins.right, ins.top + ins.bottom);
            for (Component c : parent.getComponents()) {
                Dimension d1 = c.getPreferredSize();
                d.width += d1.width;
                d.height = Math.max(d1.height, d.height);
            }
            return d;
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return preferredLayoutSize(parent);
        }

        @Override
        public void layoutContainer(Container parent) {
            Component[] comps = parent.getComponents();
            Rectangle bds = parent.getBounds();
            Insets ins = parent.getInsets();
            int x = bds.width - ins.right;
            Container ancestor = parent.getParent();
            if (ancestor != null) {
                // If our width is > than the width of the parent -
                // a large component below us is shoving us offscreen,
                // make sure the button gets positioned at the last
                // VISIBLE location (if there isn't room, it will have
                // negative offsets and not be painted)
                if (!ancestor.contains(bds.x + bds.width - 1, bds.y + 1)) {
                    x = (bds.x + bds.width) - ancestor.getWidth();
                }
            }
            int h = bds.height - (ins.top + ins.bottom);
            for (int i = comps.length - 1; i >= 0; i--) {
                Dimension d = comps[i].getPreferredSize();
                int newX = x - d.width;
                comps[i].setBounds(newX, ins.top, d.width, h);
                x = newX;
            }
        }

        @Override
        public void addLayoutComponent(Component comp, Object constraints) {
        }

        @Override
        public Dimension maximumLayoutSize(Container parent) {
            Insets ins = parent.getInsets();
            Dimension d = new Dimension(ins.left + ins.right, ins.top + ins.bottom);
            for (Component c : parent.getComponents()) {
                Dimension d1 = c.getMaximumSize();
                d.width += d1.width;
                d.height = Math.min(d1.height, d.height);
            }
            return d;
        }

        @Override
        public float getLayoutAlignmentX(Container target) {
            return 0.5F;
        }

        @Override
        public float getLayoutAlignmentY(Container target) {
            return 0F;
        }

        @Override
        public void invalidateLayout(Container target) {
        }
    }

    private final class ExpandButtonListener extends MouseAdapter implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            setExpanded(!expanded);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            setHovered(true);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getSource() == titleLabel && e.getClickCount() == 1 && !e.isPopupTrigger()) {
                expandButton.doClick();
            } else if (expanded && data != null && e.getSource() == centerComponentHolder && e.getClickCount() == 1 && !e.isPopupTrigger() && center != null) {
                int right = data.xPosForColumn(0, TitledPanel2.this);
                if (right > 0 && e.getPoint().x <= right) {
                    expandButton.doClick();
                }
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            Component newTarget = getComponentAt(e.getPoint());
            if (newTarget != titleLabel && newTarget != expandButton) {
                setHovered(false);
            }
        }
    }
}
