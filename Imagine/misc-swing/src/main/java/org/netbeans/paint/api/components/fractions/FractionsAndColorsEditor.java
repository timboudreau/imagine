package org.netbeans.paint.api.components.fractions;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.java.dev.colorchooser.ColorChooser;
import static net.java.dev.colorchooser.ColorChooser.PROP_COLOR;

/**
 *
 * @author Tim Boudreau
 */
public final class FractionsAndColorsEditor extends JComponent {

    private final Map<Fraction, Color> colorForFraction = new LinkedHashMap<>(7);
    private final Map<Fraction, ColorChooser> chooserForFraction = new LinkedHashMap<>(7);
    private final Dimension chooserPreferredSize = new Dimension(20, 20);
    private final FractionsEditor editor;

    public FractionsAndColorsEditor(float[] fractions, Color[] colors) {
        editor = new FractionsEditor(fractions);
        editor.setPreferredSize(new Dimension(400, 200));
        add(editor);
        setBackground(editor.getBackground());
        setOpaque(true);
        editor.setBorder(BorderFactory.createMatteBorder(0, 9, 0, 9,
                editor.getBackground()));
        initialSync(colors);
        editor.onSelectionChanged(this::repaint);
    }

    public static void main(String[] args) {
        FractionsAndColorsEditor ed = new FractionsAndColorsEditor(
                new float[]{
                    0F, 0.375F, 0.625F, 0.75F, 1F
                }, new Color[]{
                    Color.BLUE,
                    Color.YELLOW,
                    Color.PINK,
                    Color.ORANGE,
                    Color.RED
                });

        ed.setPreferredSize(new Dimension(600, 300));

//        ed.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
//        ed.setBorder(BorderFactory.createMatteBorder(5,5,5,5, new Color(255, 128, 128, 128)));
        JPanel pnl = new JPanel(new BorderLayout());
//        pnl.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        pnl.add(ed, BorderLayout.CENTER);
        JPanel x = new JPanel(new GridBagLayout());
        JButton b = new JButton("Reset");
        x.add(b);
        pnl.add(x, BorderLayout.EAST);
        JFrame jf = new JFrame();
        jf.setContentPane(pnl);
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.pack();
        jf.setVisible(true);
    }

    private void initialSync(Color[] colors) {
        FractionsModel model = editor.getModel();
        int cursor = 0;
        for (int i = 0; i < model.size(); i++) {
            if (cursor < colors.length) {
                colorForFraction.put(model.get(i), colors[cursor++]);
            } else {
                colorForFraction.put(model.get(i), randomColor());
            }
        }
        syncComponents(editor.allFractions(), Collections.emptySet());
    }

    public void setFractionsAndColors(float[] fractions, Color[] colors) {
        info = null;
        colorForFraction.clear();
        chooserForFraction.clear();
        editor.setFractions(fractions);
        initialSync(colors);
        invalidate();
        revalidate();
        repaint();
    }

    @Override
    public void addNotify() {
        firstPaint = true;
        super.addNotify();
        syncColors();
        editor.getModel().addChangeListener(fractionsModelListener);
    }

    @Override
    public void removeNotify() {
        editor.getModel().removeChangeListener(fractionsModelListener);
        super.removeNotify();
    }

    private final FractionsModelListener fractionsModelListener = new FractionsModelListener();
    private final ColorChooserListener chooserListener = new ColorChooserListener();

    private final List<ChangeListener> listeners = new ArrayList<>(3);

    public void addChangeListener(ChangeListener l) {
        listeners.add(l);
    }

    public void removeChangeListener(ChangeListener l) {
        listeners.remove(l);
    }

    private void fire() {
        if (!listeners.isEmpty()) {
            ChangeEvent evt = new ChangeEvent(this);
            for (ChangeListener l : listeners) {
                l.stateChanged(evt);
            }
        }
    }

    private static Color randomColor() {
        Random rnd = ThreadLocalRandom.current();
        int red = rnd.nextInt(128) + 90;
        int green = rnd.nextInt(128) + 90;
        int blue = rnd.nextInt(128) + 90;
        return new Color(red, green, blue);
    }

    private void syncColors() {
        Set<Fraction> fractions = editor.allFractions();
        Set<Fraction> added = new HashSet<>(fractions);
        added.removeAll(colorForFraction.keySet());
        Set<Fraction> removed = new HashSet<>(colorForFraction.keySet());
        removed.removeAll(fractions);
        for (Fraction f : added) {
//            colorForFraction.put(f, randomColor());
            interpolateColorFor(f);
        }
        for (Fraction f : removed) {
            colorForFraction.remove(f);
        }
        if (!added.isEmpty() || !removed.isEmpty()) {
            syncComponents(added, removed);
        }
    }

    private void interpolateColorFor(Fraction f) {
        FractionsModel mdl = editor.getModel();
        int ix = mdl.indexOf(f);
        if (ix <= 0 || ix == mdl.size() - 1) {
            colorForFraction.put(f, randomColor());
            return;
        }
        Fraction prev = mdl.fraction(ix - 1);
        Fraction next = mdl.fraction(ix + 1);
        Color prevColor = colorForFraction.get(prev);
        Color nextColor = colorForFraction.get(next);
        if (prevColor == null || nextColor == null) {
            colorForFraction.put(f, randomColor());
            return;
        }
        Color interp = interpolateColor(prevColor, nextColor, prev.getValue(), next.getValue(), f.getValue());
        colorForFraction.put(f, interp);
    }

    private Color interpolateColor(Color a, Color b, double firstOff, double secondOff, double newOff) {
        double range = (secondOff - firstOff);
        double offset = (newOff - firstOff);
        double aFactor = offset / range;
        double bFactor = (secondOff - newOff) / range;
        int r = Math.max(0, Math.min(255, (int) ((a.getRed() * aFactor) + (b.getRed() * bFactor))));
        int g = Math.max(0, Math.min(255, (int) ((a.getGreen() * aFactor) + (b.getGreen() * bFactor))));
        int bl = Math.max(0, Math.min(255, (int) ((a.getBlue() * aFactor) + (b.getBlue() * bFactor))));
        int al = Math.max(0, Math.min(255, (int) ((a.getAlpha() * aFactor) + (b.getAlpha() * bFactor))));
        Color nue = new Color(r, g, bl, al);
        return nue;
    }

    private void syncComponents(Set<Fraction> added, Set<Fraction> removed) {
        boolean changed = false;
        for (Fraction add : added) {
            ColorChooser chooser = chooserForFraction.get(add);
            if (chooser == null) {
                System.out.println("sync create chooser for " + add);
                chooser = new ColorChooser(colorForFraction.get(add));
                chooser.setFocusable(true);
                chooser.addPropertyChangeListener("color", chooserListener);
                chooser.setPreferredSize(chooserPreferredSize);
                add(chooser, add);
                chooserForFraction.put(add, chooser);
                changed = true;
            }
        }
        for (Fraction rem : removed) {
            ColorChooser chooser = chooserForFraction.get(rem);
            if (chooser != null) {
                chooser.removePropertyChangeListener("color", chooserListener);
                remove(chooser);
                changed = true;
            }
        }
        if (changed) {
            System.out.println("  was changed");
            invalidate();
            revalidate();
            repaint();
        }
    }

    public void invalidate() {
        info = null;
        super.invalidate();
    }

    private LayoutInfo layoutInfo() {
        if (info == null) {
            LayoutInfo result = computeLayoutInfo();
            if (!isDisplayable() || getWidth() == 0 || getHeight() == 0) {
                return result;
            }
            info = result;
        } else {
            if (info.targetWidth != getWidth() || info.targetHeight != getHeight()) {
                LayoutInfo result = computeLayoutInfo();
                if (!isDisplayable() || getWidth() == 0 || getHeight() == 0) {
                    return result;
                }
                info = result;
            }
        }
        return info;
    }

    private LayoutInfo computeLayoutInfo() {
        LayoutInfo result = new LayoutInfo(getWidth(),
                getHeight(), editor.getModel().size());
        int chooserCount = editor.getModel().size();
        result.chooserX = new int[chooserCount];
        result.chooserY = new int[chooserCount];
        Insets ins = getInsets();
        Dimension editorPreferredSize = editor.getPreferredSize();
        int w = getWidth();
        int h = getHeight();

        if (editorPreferredSize.width < w - ins.left + ins.right) {
            editorPreferredSize.width = w - ins.left + ins.right;
        }

        result.prefWidth = editorPreferredSize.width;
        int chooserMinGap = 3;

        result.editorWidth = editorPreferredSize.width;
        if (result.editorWidth > w - (ins.left + ins.right)) {
            result.editorWidth = Math.max(60, w - (ins.left + ins.right));
        }
        result.editorTop = ins.top;
//        result.editorLeft = ins.left + ((chooserPreferredSize.width / 2)
//                + chooserMinGap) + 2;
        result.editorLeft = ins.left;

        result.editorBottom = result.editorTop
                + editorPreferredSize.height;

        int gap = chooserMinGap;
        int vertGap = 5;

        result.defaultChooserTop = result.editorBottom + vertGap + gap;

        int chooserMinX = 0;
        int chooserMaxX = 0;
        int chooserMaxY = 0;
        int defaultChooserY = result.editorBottom + chooserMinGap;
        int ix = 0;
        int lastChooserRight = ins.left;
        for (Fraction frac : editor.getModel()) {
            float chooserCenter = editor.xCoordFor(frac) + result.editorLeft;
            int chooserX = Math.round(chooserCenter - ((chooserPreferredSize.width / 2F)));
            if (frac.isZero()) {
                chooserX = result.editorLeft;
            } else if (frac.isOne()) {
                chooserX = result.editorLeft + result.editorWidth
                        - chooserPreferredSize.width;
            }

            int chooserY = defaultChooserY;
            int chooserRight = chooserX + chooserPreferredSize.width + chooserMinGap;
            boolean isOverlap = chooserX < lastChooserRight;
            if (isOverlap && ix != 0) {
                boolean found = false;
                for (int i = ix - 1; i >= 0; i--) {
                    if (result.chooserY[i] != defaultChooserY) {
                        int neighborX = result.chooserX[i];
                        int neighborRight = neighborX
                                + chooserPreferredSize.width
                                + chooserMinGap;

                        if (neighborRight <= chooserX) {
                            found = true;
                            chooserY = result.chooserY[i];
                        }
                    } else {
                        break;
                    }
                }
                if (!found) {
                    chooserY = result.chooserY[ix - 1] + vertGap + chooserPreferredSize.height;
                    result.offsetItems.set(ix);
                }
            }
            result.chooserX[ix] = chooserX;
            result.chooserY[ix] = chooserY;
            int chooserBottom = chooserY + chooserPreferredSize.height + gap;
            chooserMinX = Math.min(chooserX, chooserMinX);
            chooserMaxX = Math.max(chooserX, chooserMaxX);
            chooserMaxY = Math.max(chooserBottom, chooserMaxY);
            lastChooserRight = chooserRight;
            ix++;
        }
        result.width = Math.max(result.editorLeft + editorPreferredSize.width + ins.right,
                chooserMaxX - chooserMinX);
        result.height = chooserMaxY + chooserMinGap + ins.bottom;
        if (chooserMinX < 0) {
            result.shiftX(-chooserMinX);
        }
//        if (result.height + ((chooserPreferredSize.height + gap) * 3) < h) {
//            int diff = h - result.height;
//            result.shortenEditor(-diff);
//        }
        result.prefHeight = result.height;

        if (result.height > h) {
            int diff = result.height - h;
            if (diff <= editorPreferredSize.height / 2) {
                result.shortenEditor(diff);
            } else {
                result.shortenEditor(editorPreferredSize.height / 2);
            }
        }
        result.maxHeight = editorPreferredSize.height
                + ((editor.getModel().size() - 2) * (chooserPreferredSize.width
                + vertGap));

        editor.setHiddenArrowHeads(result.offsetItems);

        // if not enough room, adjust editor height smaller and
        // recompute
        return result;
    }

    private LayoutInfo info;

    public float[] getFractions() {
        return this.editor.getFractions();
    }

    public void setColors(Color[] colors) {
        int sz = editor.getModel().size();
        for (int i = 0; i < Math.min(sz, colors.length); i++) {
            Fraction f = editor.getModel().get(i);
            ColorChooser chooser = this.chooserForFraction.get(f);
            if (chooser != null) {
                chooser.setColor(colors[i]);
            }
        }
        repaint();
    }

    public Color[] getColors() {
        int sz = editor.getModel().size();
        Color[] result = new Color[sz];
        for (int i = 0; i < sz; i++) {
            Fraction f = editor.getModel().get(i);
            ColorChooser chooser = this.chooserForFraction.get(f);
            Color color;
            if (chooser != null) {
                color = chooser.getColor();
            } else {
                color = randomColor();
            }
            result[i] = color;
        }
        return result;
    }

    public void colorsAndFractions(BiConsumer<float[], Color[]> c) {
        c.accept(getFractions(), getColors());
    }

    private static class LayoutInfo {

        private int maxHeight;
        private int prefHeight;
        private int prefWidth;
        private int editorLeft;
        private int editorTop;
        private int editorBottom;
        private int editorWidth;
        private int defaultChooserTop;
        private int[] chooserX;
        private int[] chooserY;
        private int height;
        private int width;
        private final int targetWidth;
        private final int targetHeight;
        private final BitSet offsetItems;

        LayoutInfo(int targetWidth, int targetHeight, int modelSize) {
            this.targetWidth = targetWidth;
            this.targetHeight = targetHeight;
            offsetItems = new BitSet(modelSize);
        }

        private void layoutEditor(JComponent editor) {
            editor.setBounds(editorLeft, editorTop, editorWidth,
                    editorBottom - editorTop);
        }

        private void shortenEditor(int by) {
            for (int i = 0; i < chooserY.length; i++) {
                chooserY[i] -= by;
            }
            editorBottom -= by;
            height -= by;
        }

        private void shiftX(int by) {
            for (int i = 0; i < chooserX.length; i++) {
                chooserX[i] += by;
            }
        }

        Dimension preferredSize() {
            return new Dimension(prefWidth, prefHeight);
        }

        Dimension minSize() {
            int minH = height - ((editorBottom - editorTop) / 2);
            return new Dimension(width, minH);
        }

        Dimension maxSize() {
            return new Dimension(prefWidth, maxHeight);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension result = layoutInfo().preferredSize();
        // Provide for 2 rows of choosers by default
        result.height += 3 + chooserPreferredSize.height;
        return result;
    }

    @Override
    public Dimension getMinimumSize() {
        return layoutInfo().minSize();
    }

    private Line2D.Double scratchLine = new Line2D.Double();

    private boolean firstPaint = true;

    public void paint(Graphics g) {
        if (firstPaint) {
            firstPaint = false;
            info = null;
            doLayout();
        }
        super.paint(g);
    }

    @Override
    public void paintComponent(Graphics gr) {
        Graphics2D g = (Graphics2D) gr;
        g.setBackground(getBackground());
        g.clearRect(0, 0, getWidth(), getHeight());

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        int ix = 0;
        LayoutInfo ifo = layoutInfo();
        g.setStroke(new BasicStroke(1.5F));
        Insets ins = editor.getInsets();
        for (Fraction f : editor.getModel()) {
            if (ifo.offsetItems.get(ix)) {
                ColorChooser ch = chooserForFraction.get(f);
                if (ch != null) {
                    if (editor.isSelected(f)) {
                        g.setColor(editor.getSelectedColor());
                    } else {
                        g.setColor(editor.getForeground());
                    }
                    float lineX = editor.xCoordFor(f) + ifo.editorLeft;
                    float lineY = ch.getY();
                    scratchLine.setLine(lineX, lineY, lineX, ifo.editorBottom);
                    g.draw(scratchLine);
                    scratchLine.x2 = lineX - ARROW_HEAD_SIZE;
                    scratchLine.y2 = lineY - ARROW_HEAD_SIZE;
                    g.draw(scratchLine);
                    scratchLine.x2 = lineX + ARROW_HEAD_SIZE;
                    scratchLine.y2 = lineY - ARROW_HEAD_SIZE;
                    g.draw(scratchLine);
                }
            }
            ix++;
        }
    }
    private static final int ARROW_HEAD_SIZE = 3;

    @Override
    public void doLayout() {
        LayoutInfo ifo = layoutInfo();
        int ix = 0;
        ifo.layoutEditor(editor);
        for (Fraction frac : editor.getModel()) {
            ColorChooser c = chooserForFraction.get(frac);
            c.setBounds(ifo.chooserX[ix],
                    ifo.chooserY[ix], chooserPreferredSize.width,
                    chooserPreferredSize.height);
            ix++;
        }
    }

    private class FractionsModelListener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            syncColors();
            fire();
            invalidate();
            revalidate();
            repaint();
        }
    }

    private class ColorChooserListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (PROP_COLOR.equals(evt.getPropertyName())) {
                ColorChooser chooser = (ColorChooser) evt.getSource();
                for (Map.Entry<Fraction, ColorChooser> e : chooserForFraction.entrySet()) {
                    if (e.getValue() == chooser) {
                        Fraction f = e.getKey();
                        if (f != null) {
                            colorForFraction.put(f, (Color) evt.getNewValue());
                        }
                    }
                }
                fire();
            }
        }
    }
}
