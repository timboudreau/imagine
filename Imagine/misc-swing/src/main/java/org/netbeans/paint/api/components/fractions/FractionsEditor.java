package org.netbeans.paint.api.components.fractions;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.imagine.geometry.Rhombus;
import org.netbeans.paint.api.cursor.Cursors;

/**
 *
 * @author Tim Boudreau
 */
public final class FractionsEditor extends JComponent {

    private final FractionsModel fractions;
    private boolean gridPainted = true;
    private Color gridColor;
    private final ML ml = new ML();
    private final List<ChangeListener> listeners = new ArrayList<>();
    private Line2D.Float scratchLine = new Line2D.Float();
    private Color selectedColor;
    private Color selectedLineCenterColor;
    private final BasicStroke FOCUS_STROKE = new BasicStroke(0.5F, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1F, new float[]{5F, 10F}, 0);
    private static final DecimalFormat FMT = new DecimalFormat("0.0");
    private final Rhombus scratchRhombus = new Rhombus(0, 0, 5, 9, 0);

    public FractionsEditor() {
        this(null);
    }

    public FractionsEditor(float[] fractions) {
        this.fractions = new FractionsModel(fractions);
        Color bg = UIManager.getColor("text");
        if (bg == null) {
            bg = Color.WHITE;
        }
        Color fg = UIManager.getColor("textText");
        if (fg == null) {
            fg = Color.DARK_GRAY;
        }
        Color grid = UIManager.getColor("controlDkShadow");
        if (grid == null) {
            grid = Color.LIGHT_GRAY;
        }
        gridColor = grid;
        Color sel = UIManager.getColor("Tree.selectionBackground");
        if (sel == null) {
            sel = new Color(120, 120, 255);
        } else {
            sel = sel.darker();
        }
        selectedColor = sel;
        Color selCenter = UIManager.getColor("Fractions.selectionForeground");
        if (selCenter == null) {
            selCenter = Color.ORANGE;
        }
        selectedLineCenterColor = selCenter;
        Font font = UIManager.getFont("controlFont");
        if (font == null) {
            font = UIManager.getFont("Label.font");
            if (font == null) {
                font = new Font("SansSerif", Font.PLAIN, 14);
            } else {
                font = font.deriveFont(14F);
            }
        }
        setFont(font);
        setBackground(bg);
        setForeground(fg);
        setOpaque(true);
        setFocusable(true);
        setCursor(Cursors.forComponent(this).hin());
    }

    public static void main(String[] args) {
        FractionsEditor ed = new FractionsEditor(new float[]{0F, 0.375F, 0.625F, 0.75F, 1F});
        ed.setPreferredSize(new Dimension(600, 300));
        JPanel pnl = new JPanel();
        pnl.add(ed);
        JButton b = new JButton("Reset");
        pnl.add(b);
        JFrame jf = new JFrame();
        jf.setContentPane(pnl);
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.pack();
        jf.setVisible(true);
    }

    public void setGridPainted(boolean painted) {
        if (painted != gridPainted) {
            gridPainted = painted;
            firePropertyChange("gridPainted", !painted, painted);
            repaint();
        }
    }

    public boolean isGridPainted() {
        return gridPainted;
    }

    public Color getGridColor() {
        return gridColor;
    }

    public void setGridColor(Color grid) {
        if (!Objects.equals(grid, gridColor)) {
            Color old = gridColor;
            if (grid == null) {
                grid = UIManager.getColor("Tree.line");
                if (grid == null) {
                    grid = Color.LIGHT_GRAY;
                }
                if (Objects.equals(grid, gridColor)) {
                    return;
                }
            }
            gridColor = grid;
            firePropertyChange("gridColor", old, grid);
            repaint();
        }
    }

    public void setSelectedColor(Color selected) {
        if (!Objects.equals(selected, selectedColor)) {
            Color old = selectedColor;
            if (selected == null) {
                selected = UIManager.getColor("Tree.selectionBackground");
                if (selected == null) {
                    selected = new Color(120, 120, 255);
                }
                if (Objects.equals(selected, selectedColor)) {
                    return;
                }
            }
            selectedColor = selected;
            firePropertyChange("selectedColor", old, selected);
            repaint();
        }
    }

    public Color getSelectedColor() {
        return selectedColor;
    }

    public void setSelectedLineCenterColor(Color selected) {
        if (!Objects.equals(selected, selectedLineCenterColor)) {
            Color old = selectedLineCenterColor;
            if (selected == null) {
                selected = UIManager.getColor("Tree.selectionBackground");
                if (selected == null) {
                    selected = new Color(120, 120, 255);
                }
                if (Objects.equals(selected, selectedLineCenterColor)) {
                    return;
                }
            }
            selectedLineCenterColor = selected;
            firePropertyChange("selectedLineCenterColor", old, selectedLineCenterColor);
            repaint();
        }
    }

    public Color getSelectedLineCenterColor() {
        return selectedLineCenterColor;
    }

    private void fire() {
        for (ChangeListener cl : listeners) {
            cl.stateChanged(new ChangeEvent(this));
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        ml.attach();
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    @Override
    public void removeNotify() {
        ToolTipManager.sharedInstance().unregisterComponent(this);
        ml.detach();
        super.removeNotify();
    }

    private Fraction fractionForPoint(Point p) {
        for (Fraction f : fractions) {
            float xpos = xFloatCoordFor(f);
            float off = Math.abs(p.x - xpos);
            if (off < 7) {
                return f;
            }
        }
        return null;
    }

    private float workingWidth() {
        Insets ins = getInsets();
        float w = getWidth();
        if (w == 0) {
            return 0;
        }
        return w - (ins.left + ins.right + (reserved * 2));
    }
    private Runnable onSelectionChanged;

    public void onSelectionChanged(Runnable run) {
        onSelectionChanged = run;
    }

    public float zeroPos() {
        Insets ins = getInsets();
        return ins.left + reserved;
    }

    public boolean isSelected(Fraction fraction) {
        return fraction == ml.selectedFraction;
    }

    private int reserved = 2;

    public float onePos() {
        Insets ins = getInsets();
        return getWidth() - (ins.right + reserved);
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        return FMT2.format(valueAtPoint(event.getPoint()));
    }

    @Override
    public Point getToolTipLocation(MouseEvent event) {
        return new Point(0, 0);
    }

    private float valueAtPoint(Point point) {
        float w = workingWidth();
        float z = zeroPos();
        float o = onePos();
        if (point.x <= 0) {
            return 0;
        }
        if (point.x >= o) {
            return 1;
        }
        float val = (float) point.x - z;
        return val / w;
    }

    public int xCoordForValue(float value) {
        if (value >= 1) {
            return (int) Math.round(onePos());
        } else if (value <= 0) {
            return (int) Math.round(zeroPos());
        }
        float z = zeroPos();
        float w = workingWidth();
        float result = z + (w * value);
        return (int) Math.round(result);
    }

    public float xFloatCoordFor(Fraction fraction) {
        if (fraction.isZero()) {
            return zeroPos();
        } else if (fraction.isOne()) {
            return onePos();
        }
        float z = zeroPos();
        float w = workingWidth();
        float result = z + (w * fraction.getValue());
        return result;
    }

    public float xFloatCoordFor(float value) {
        if (value <= 0) {
            return zeroPos();
        } else if (value >= 1) {
            return onePos();
        }
        float z = zeroPos();
        float w = workingWidth();
        float result = z + (w * value);
        return result;
    }

    public int xCoordFor(Fraction fraction) {
        if (fraction.isZero()) {
            return (int) Math.ceil(zeroPos());
        } else if (fraction.isOne()) {
            return (int) Math.floor(onePos());
        }
        return xCoordForValue(fraction.getValue());
    }

    private boolean addFraction(Point p) {
        if (fractionForPoint(p) != null) {
            return false;
        }
        float val = valueAtPoint(p);
        if (fractions.canAdd(val)) {
            fractions.add(val);
            fire();
            repaint();
            return true;
        }
        return false;
    }

    public float[] getFractions() {
        return fractions.toFloatArray();
    }

    public void setFractions(float[] fractions) {
        ml.abort();
        int ix = -1;
        if (ml.selectedFraction != null) {
            ix = this.fractions.indexOf(ml.selectedFraction);
        }
        this.fractions.set(fractions);
        if (ix > 0 && ix < this.fractions.size() - 1) {
            ml.selectedFraction = this.fractions.get(ix);
        }
        fire();
        repaint();
    }

    private int minimumPracticalWidth() {
        float minGap = Float.MAX_VALUE;
        for (int i = 1; i < fractions.size(); i++) {
            Fraction prev = fractions.get(i - 1);
            Fraction curr = fractions.get(i);
            float gap = curr.getValue() - prev.getValue();
            minGap = Math.min(gap, minGap);
        }
        return (int) (2 / minGap);
    }

    public Dimension getPreferredSize() {
        if (super.isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        int w = minimumPracticalWidth();
        w = Math.min(100, Math.max(400, w));
        int h = 60;
        return new Dimension(w, h);
    }

    public Dimension getMinimumSize() {
        int w = minimumPracticalWidth();
        if (w < 400) {
            return new Dimension(w, 60);
        }
        return new Dimension(60, 400);
    }

    private static final int TOP_GAP = 3;

    public void paint(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paint(g);
    }

    @Override
    public void paintComponent(Graphics gr) {
        Graphics2D g = (Graphics2D) gr;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g.setColor(getBackground());

        int w = getWidth();
        int h = getHeight();

        Insets ins = getInsets();
        g.fillRect(ins.top, ins.left, w - (ins.left + ins.right), h + (ins.top + ins.bottom));

        int y = ins.top + TOP_GAP;

        boolean hasFocus = hasFocus();
        if (gridPainted) {
            g.setStroke(FOCUS_STROKE);
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            g.setColor(getGridColor());
            paintGrid(g, y, w, h, ins, fm, hasFocus);
        }
        g.setColor(getForeground());
        g.setStroke(new BasicStroke(2.5F));
        int ix = 0;
        for (Fraction f : fractions) {
            float x = xFloatCoordFor(f);
            boolean selected = f == ml.selectedFraction;
            boolean armed = !selected && f == ml.hoveredFraction;
            boolean dragged = f == ml.draggingFraction;
            boolean hovered = f == ml.hoveredFraction;
            paintFraction(hasFocus, x, y, g, f, w, h, ins, selected, armed, dragged, hovered, ix++);
        }

        paintProposedValue(g, y, w, h);

        Border b = getBorder();
        if (b != null) {
            b.paintBorder(this, gr, 0, 0, w, h);
        }
    }

    private static final DecimalFormat FMT2 = new DecimalFormat("#.###");
    private Rectangle2D.Float lastPValueText = new Rectangle2D.Float();

    private void paintProposedValue(Graphics2D g, float y, int w, int h) {
        if (ml.draggingFraction != null && ml.draggingFraction.canSet(ml.proposedValue)) {
            float x = xFloatCoordFor(ml.proposedValue);
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            String txt = FMT2.format(ml.proposedValue);
            int sw = fm.stringWidth(txt);
            g.setXORMode(Color.RED);
            scratchLine.setLine(x, y + (fm.getAscent() + 3), x, (h - y));
            g.draw(scratchLine);
            g.setPaintMode();

            float txtX;
            if (x + sw + 2 < w) {
                txtX = x + 2;
            } else {
                txtX = x - (sw + 2);
            }
            g.drawString(txt, txtX, (y + h) - (fm.getAscent() + 7));
            lastPValueText.setFrame(txtX, (y + h) - fm.getAscent(), sw, fm.getAscent());
        }
    }

    private void paintGrid(Graphics2D g, float y, int w, int h, Insets ins,
            FontMetrics fm, boolean hasFocus) {
        int sw = fm.stringWidth("0.0");
        int txtH = fm.getMaxAscent() + fm.getMaxDescent() + 1;
        if (txtH % 2 == 0) {
            txtH++;
        }
        float txtY = y + fm.getAscent();
        int postCount = h / (sw + 2);
        float add = 0.1F;
        if (postCount > 20) {
            add = 0.05F;
        }
        float curr = 0.1F;
        while (curr < 1) {
            float xPos = xFloatCoordFor(curr);
            scratchLine.setLine(xPos, y + fm.getAscent() + 3, xPos, h - y);
            g.draw(scratchLine);
            String txt = FMT.format(curr);
            float stringW = fm.stringWidth(txt);
            float txtX = xPos - (stringW / 2);
            g.drawString(txt, txtX, txtY);
            curr += add;
        }
        g.drawString("0", ins.left, txtY);
        String one = "1";
        int sn = fm.stringWidth(one);
        g.drawString(one, w - (sn + ins.right - 2), txtY);
    }

    private BitSet hidden;

    void setHiddenArrowHeads(BitSet hidden) {
        this.hidden = hidden;
    }

    boolean isArrowHeadHidden(int ix) {
        if (hidden == null) {
            return false;
        }
        return hidden.get(ix);
    }

    public FractionsModel getModel() {
        return fractions;
    }

    public Set<Fraction> allFractions() {
        return new LinkedHashSet<>(fractions.fractions);
    }

    private void paintFraction(boolean hasFocus, float x, int y, Graphics2D g,
            Fraction f, int w, int h, Insets ins,
            boolean selected, boolean armed, boolean dragged,
            boolean hovered, int index) {
        FontMetrics fm = g.getFontMetrics(getFont());
        float lineY = y + 3;
        float lineH = h;
        if (selected) {
            g.setColor(selectedColor);
        } else {
            if (f.isEnd()) {
                g.setColor(UIManager.getColor("textInactiveText"));
            } else {
                g.setColor(getForeground());
            }
        }
        if (dragged) {
            g.setColor(selectedLineCenterColor);
        }
        float xPos = xFloatCoordFor(f);
//        float lineBottom = (lineH - lineY) - (fm.getAscent() + 3);
        float lineBottom = h;
        lineY += fm.getAscent();
        g.setStroke(new BasicStroke(1.5F));
        scratchLine.setLine(xPos, lineY, xPos, lineBottom);
        float cy = y + (h / 2F) - TOP_GAP;
        g.draw(scratchLine);
//        g.setStroke(new BasicStroke(0.5F));
        int fh = fm.getMaxAscent() + fm.getMaxDescent();
        float stringY = (y + h) - ((fh * index) + ins.bottom);
        if (stringY < y) {
            stringY = y + h;
        }
        if (stringY + fm.getHeight() >= cy - scratchRhombus.getYRadius()) {
            stringY += ((float) scratchRhombus.getYRadius() * 2);
        }
        if (!f.isEnd()) {
            g.drawArc((int) xPos - 5, (int) lineY, 10, 7, 0, 180);
        }
        if (!isArrowHeadHidden(index)) {
            scratchLine.setLine(xPos, lineBottom, xPos - 4, lineBottom - 4);
            g.draw(scratchLine);
            scratchLine.setLine(xPos, lineBottom, xPos + 4, lineBottom - 4);
            g.draw(scratchLine);
        }
        if (!f.isEnd()) {
            scratchRhombus.setCenter(xPos, cy);
            g.draw(scratchRhombus);
            if (hasFocus) {
                if (selected) {
                    g.setColor(selectedLineCenterColor);
                } else {
                    g.setColor(getGridColor());
                }
            }
            g.fill(scratchRhombus);
            if (hovered || selected || ml.shiftDown) {
                String txt = FMT2.format(f.getValue());
                int sw = fm.stringWidth(txt);
                float txtX = xPos + 2;
                if (xPos + (sw + 2) >= w) {
                    txtX = xPos - (sw + 2);
                }
                g.drawString(txt, txtX, stringY);
            }
        }
    }

    public void setSelectedFraction(Fraction fraction) {
        ml.abort();
        ml.selectedFraction = fraction;
        if (onSelectionChanged != null) {
            onSelectionChanged.run();;
        }
        repaint();
    }

    class ML extends MouseAdapter implements FocusListener, ChangeListener, KeyListener {

        Fraction armedFraction;
        Fraction draggingFraction;
        Fraction selectedFraction;
        Fraction hoveredFraction;
        float proposedValue = 0;
        boolean shiftDown;

        @Override
        public void keyTyped(KeyEvent e) {
        }

        private void addRandomFraction() {
            Fraction targetFraction = selectedFraction;
            if (targetFraction == null) {
                int largestGap = -1;
                float range = Float.MIN_VALUE;
                for (int i = 1; i < fractions.size(); i++) {
                    float currRange
                            = fractions.get(i).getValue()
                            - fractions.get(i - 1).getValue();
                    if (currRange < range) {
                        range = currRange;
                        largestGap = i - 1;
                    }
                }
                if (largestGap == -1) {
                    selectedFraction = fractions.add(ThreadLocalRandom.current().nextFloat());
                    if (onSelectionChanged != null) {
                        onSelectionChanged.run();
                    }
                } else {
                    targetFraction = fractions.get(largestGap);
                    float val = targetFraction.getValue()
                            + (ThreadLocalRandom.current().nextFloat() * range);
                    selectedFraction = fractions.add(val);
                    if (onSelectionChanged != null) {
                        onSelectionChanged.run();
                    }
                }
            } else {
                Fraction next;
                if (fractions.indexOf(targetFraction) == fractions.size() - 2) {
                    next = fractions.get(fractions.size() - 1);
                } else {
                    next = fractions.nextEditableFraction(targetFraction);
                    if (next == null) {
                        next = fractions.get(fractions.size() - 1);
                    }
                }
                float range = next.getValue() - targetFraction.value;
                float val = targetFraction.getValue()
                        + (ThreadLocalRandom.current().nextFloat() * range);
                selectedFraction = fractions.add(val);
                if (onSelectionChanged != null) {
                    onSelectionChanged.run();
                }
            }

        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (selectedFraction == null) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_PLUS:
                    case KeyEvent.VK_MINUS:
                        addRandomFraction();
                        break;
                    case KeyEvent.VK_EQUALS:
                        if (e.isShiftDown()) {
                            addRandomFraction();
                        }
                        break;
                    case KeyEvent.VK_BACK_SPACE:
                        if (fractions.size() >= 3) {
                            selectedFraction = fractions.get(fractions.size() - 2);
                            if (onSelectionChanged != null) {
                                onSelectionChanged.run();
                            }
                            repaint();
                        }
                        break;
                    case KeyEvent.VK_SPACE:
                    case KeyEvent.VK_ENTER:
                        if (fractions.size() >= 3) {
                            selectedFraction = fractions.get(1);
                            if (onSelectionChanged != null) {
                                onSelectionChanged.run();
                            }
                            repaint();
                        }
                        break;
                    case KeyEvent.VK_SHIFT:
                        if (!shiftDown) {
                            shiftDown = true;
                            repaint();
                        }
                        break;
                }
                return;
            }
            switch (e.getKeyCode()) {
                case KeyEvent.VK_PLUS:
                case KeyEvent.VK_MINUS:
                    addRandomFraction();
                    break;
                case KeyEvent.VK_EQUALS:
                    if (e.isShiftDown()) {
                        addRandomFraction();
                    }
                    break;
                case KeyEvent.VK_RIGHT:
                    float inc = e.isShiftDown() ? 0.01F : e.isControlDown() ? 0.1F : 0.0025F;
                    if (selectedFraction.setValue(selectedFraction.getValue() + inc)) {
                        repaint();
                    }
                    break;
                case KeyEvent.VK_LEFT:
                    float dec = e.isShiftDown() ? -0.01F : e.isControlDown() ? -0.1F : -0.0025F;
                    if (selectedFraction.setValue(selectedFraction.getValue() + dec)) {
                        repaint();
                    }
                    break;
                case KeyEvent.VK_UP:
                case KeyEvent.VK_BACK_SPACE:
                    selectedFraction = fractions.previousEditableFraction(selectedFraction);
                    if (onSelectionChanged != null) {
                        onSelectionChanged.run();;
                    }
                    repaint();
                    break;
                case KeyEvent.VK_SPACE:
                case KeyEvent.VK_DOWN:
                    selectedFraction = fractions.nextEditableFraction(selectedFraction);
                    if (onSelectionChanged != null) {
                        onSelectionChanged.run();;
                    }
                    repaint();
                    break;
                case KeyEvent.VK_DELETE:
                    if (selectedFraction != null) {
                        Fraction newSelection = fractions.nextEditableFraction(selectedFraction);
                        if (fractions.delete(selectedFraction)) {
                            abort();
                            selectedFraction = newSelection;
                            if (onSelectionChanged != null) {
                                onSelectionChanged.run();;
                            }
                        }
                    }
                    break;
                case KeyEvent.VK_SHIFT:
                    if (!shiftDown) {
                        shiftDown = true;
                        repaint();
                    }
                    break;
            }
        }

        void ensureFocus() {
            if (!hasFocus()) {
                requestFocus();
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_SHIFT:
                    if (shiftDown) {
                        shiftDown = false;
                        repaint();
                    }
                    break;
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            Fraction frac = fractionForPoint(e.getPoint());
            hoveredFraction = frac;
            if (hoveredFraction != null && !hoveredFraction.isEnd()) {
                Cursors cursors = Cursors.forComponent(FractionsEditor.this);
                setCursor(cursors.hin());
                e.consume();
            } else {
                hoveredFraction = null;
                float val = valueAtPoint(e.getPoint());
                if (draggingFraction == null && fractions.canAdd(val)) {
                    Cursors cursors = Cursors.forComponent(FractionsEditor.this);
                    setCursor(cursors.triangleDown());
                    e.consume();
                } else {
                    setCursor(Cursor.getDefaultCursor());
                    e.consume();
                }
            }
            repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (draggingFraction != null) {
                float val = valueAtPoint(e.getPoint());
                if (draggingFraction.canSet(val)) {
                    System.out.println("Change " + draggingFraction + " to " + val);
                    draggingFraction.setValue(val);
                    repaint();
                    e.consume();
                }
                selectedFraction = draggingFraction;
                draggingFraction = null;
                if (onSelectionChanged != null) {
                    onSelectionChanged.run();;
                }
                abort();
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (armedFraction != null && draggingFraction == null) {
                draggingFraction = armedFraction;
                selectedFraction = null;
                armedFraction = null;
            }
            if (draggingFraction != null) {
                ensureFocus();
                Point p = e.getPoint();
                Insets ins = getInsets();
                if (proposedValue == draggingFraction.getValue() || draggingFraction.canSet(proposedValue)) {
                    float newValue = valueAtPoint(p);
                    int x = (int) xFloatCoordFor(proposedValue);
                    repaint(x - 3, ins.top, 4, getHeight() - ins.bottom);
                    proposedValue = newValue;
                    x = xCoordFor(draggingFraction);
                    repaint(x - 2, ins.top, 4, getHeight() - ins.bottom);
                    x = (int) xFloatCoordFor(proposedValue);
                    repaint(x - 2, ins.top, 4, getHeight() - ins.bottom);
                    repaint(lastPValueText.getBounds());
                    Cursors cursors = Cursors.forComponent(FractionsEditor.this);
                    if (proposedValue == draggingFraction.getValue()) {
                        setCursor(cursors.no());
                    } else {
                        setCursor(cursors.horizontal());
                    }
                    e.consume();
                } else {
                    Cursors cursors = Cursors.forComponent(FractionsEditor.this);
                    setCursor(cursors.no());
                    int x = (int) xFloatCoordFor(proposedValue);
                    repaint(x - 1, ins.top, 3, getHeight() - ins.bottom);
                    proposedValue = draggingFraction.value;
                    repaint();
                    e.consume();
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            armedFraction = fractionForPoint(e.getPoint());
            if (armedFraction != null && armedFraction.isEnd()) {
                armedFraction = null;
            }
            if (armedFraction != null) {
                hoveredFraction = null;
                draggingFraction = null;
                proposedValue = valueAtPoint(e.getPoint());
            }
            repaint();
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            ensureFocus();
            if (e.getClickCount() >= 2) {
                float val = valueAtPoint(e.getPoint());
                System.out.println("double click " + val);
                if (val != 0F && val != 1F) {
                    if (fractions.canAdd(val)) {
                        Fraction f = fractions.add(val);
                        selectedFraction = f;
                        if (onSelectionChanged != null) {
                            onSelectionChanged.run();
                        }
                        e.consume();
                    }
                }
            } else if (e.getClickCount() == 1) {
                Fraction fraction = fractionForPoint(e.getPoint());
                if (fraction != selectedFraction) {
                    if (fraction != null && fraction.isEnd()) {
                        fraction = null;
                    }
                    selectedFraction = fraction;
                    if (onSelectionChanged != null) {
                        onSelectionChanged.run();;
                    }
                    repaint();
                    e.consume();
                }
            }
        }

        private void abort() {
            setCursor(Cursor.getDefaultCursor());
            draggingFraction = null;
            shiftDown = false;
            armedFraction = null;
            hoveredFraction = null;
            if (isDisplayable()) {
                repaint();
            }
        }

        private void attach() {
            addMouseListener(this);
            addMouseMotionListener(this);
            addFocusListener(this);
            addMouseWheelListener(this);
            addKeyListener(this);
            fractions.addChangeListener(this);
        }

        private void detach() {
            fractions.removeChangeListener(this);
            removeKeyListener(this);
            removeMouseWheelListener(this);
            removeFocusListener(this);
            removeMouseMotionListener(this);
            removeMouseListener(this);
            abort();
        }

        @Override
        public void focusGained(FocusEvent e) {
            repaint();
        }

        @Override
        public void focusLost(FocusEvent e) {
            abort();
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            fire();
            repaint();
        }
    }
}
