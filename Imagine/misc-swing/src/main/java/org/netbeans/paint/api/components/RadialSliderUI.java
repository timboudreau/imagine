package org.netbeans.paint.api.components;

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
import java.awt.Rectangle;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_STROKE_CONTROL;
import static java.awt.RenderingHints.KEY_TEXT_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_STROKE_PURE;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB;
import java.awt.Stroke;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.BoundedRangeModel;
import org.imagine.geometry.Circle;
import org.imagine.geometry.Quadrant;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.SliderUI;

/**
 *
 * @author Tim Boudreau
 */
public class RadialSliderUI extends SliderUI {

    private static final Insets EMPTY_INSETS = new Insets(0, 0, 0, 0);
    private static final int MARGIN = 1;
    private static final Stroke THIN = new BasicStroke(0.5F);
    private static final Stroke NORMAL = new BasicStroke(1);
    private static final Stroke WIDE = new BasicStroke(1.5F);
    private static RadialSliderUI INSTANCE;
    private static final String CLIENT_PROP_SMALL_CAPTION = "smallCaption";
    private static final String CLIENT_PROP_SLIDER_STATE = "sliderState";

    public static RadialSliderUI instance() {
        if (INSTANCE == null) {
            INSTANCE = new RadialSliderUI();
        }
        return INSTANCE;
    }

    public static void attach(JSlider slider) {
        RadialSliderUI result = instance();
        slider.setUI(result);
    }

    public static void setSmallCaptions(JSlider slider) {
        slider.putClientProperty(CLIENT_PROP_SMALL_CAPTION, slider);
    }

    public static void setStringConverter(JSlider slider, StringConverter cv) {
        slider.putClientProperty(StringConverter.CLIENT_PROP_CONVERTER, cv);
    }

    @Override
    public boolean contains(JComponent c, int x, int y) {
        return state(c).circle(c).contains(x, y);
    }

    @Override
    public Dimension getMaximumSize(JComponent c) {
        return new Dimension(512, 512);
    }

    @Override
    public Dimension getMinimumSize(JComponent c) {
        return new Dimension(36, 36);
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return new Dimension(96, 96);
    }

    @Override
    public void update(Graphics g, JComponent c) {
        super.update(g, c);
    }

    @Override
    public int getBaseline(JComponent c, int width, int height) {
        return height / 2;
    }

    private static Insets insets(JComponent c) {
        Insets result = c.getInsets();
        return result == null ? EMPTY_INSETS : result;
    }

    @Override
    public void paint(Graphics gr, JComponent c) {
        JSlider slider = (JSlider) c;
        Graphics2D g = (Graphics2D) gr;
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g.setRenderingHint(KEY_STROKE_CONTROL, VALUE_STROKE_PURE);
        SliderState state = state(slider);
        state.withCircleAndLine(c, (circ, line, angle) -> {
            Color shad = UIManager.getColor("controlShadow"); //NOI18N
            if (shad == null) {
                shad = Color.GRAY;
            }
            g.setColor(shad); // NOI18N
            g.setStroke(NORMAL);
            g.draw(circ);
            g.setStroke(WIDE);
            g.setColor(slider.getForeground());
            g.draw(line);
            Circle fob = state.scratchCircle;
            fob.setRadius(circ.radius() * 0.075D);
            fob.setCenter(line.x2, line.y2);
            g.setColor(shad);
            g.fill(fob);
            g.setColor(slider.getForeground());
            g.draw(fob);
            fob.setCenter(circ.centerX(), circ.centerY());
            fob.setRadius(fob.radius() * 0.625D);
            g.fill(fob);
            g.draw(fob);

            Color lineColor = g.getColor();
            if (slider.hasFocus()) {
                lineColor = drawFocusRing(lineColor, circ, g);
            }

            drawCaption(g, slider, angle, state, circ);

            if (state.isDragging()) {
                Line2D.Double dragLine = state.dragLine(slider);
                g.setColor(lineColor);
                if (dragLine != null) {
                    g.setStroke(NORMAL);
                    g.setColor(UIManager.getColor("controlShadow"));
                    g.draw(dragLine);
                }
                paintDragCaption(state, slider, g, lineColor, circ, angle);
            }
        });
    }

    private void drawCaption(Graphics2D g, JSlider slider, double angle, SliderState state, Circle circ) {
        if (slider.getClientProperty(CLIENT_PROP_SMALL_CAPTION) != null) {
            drawSmallCaption(g, slider, angle, state, circ);
            return;
        }
        String txt = state.valueText(slider);
        Font font = slider.getFont();
        FontMetrics fm = g.getFontMetrics(font);
        int txtW = fm.stringWidth(txt);
        int txtH = fm.getMaxAscent() + fm.getMaxDescent();

        Quadrant quad = Quadrant.forAngle(angle);
        boolean north = quad.opposite().isNorth();

        Rectangle2D textAreaBounds = circ.halfBounds(Quadrant.forAngle(angle).opposite(), false, true);
        double widthFactor = textAreaBounds.getWidth() / txtW;
        double heightFactor = textAreaBounds.getHeight() / txtH;
        font = font.deriveFont(AffineTransform.getScaleInstance(widthFactor, heightFactor));
        fm = g.getFontMetrics(font);
        double txtTop = textAreaBounds.getCenterY() + (fm.getHeight() / (north ? -2D : 2D));
        if (!north) {
            txtTop = textAreaBounds.getCenterY() + (fm.getHeight() / 2D);
        } else {
            txtTop = textAreaBounds.getCenterY();
        }
        double txtX = textAreaBounds.getX();
        g.setFont(font);
        Color fg = slider.getForeground();
        fg = new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 128);
        g.setColor(fg);
        g.drawString(txt, (float) txtX, (float) txtTop);
    }

    private void drawSmallCaption(Graphics2D g, JSlider slider, double angle, SliderState state, Circle circ) {
        g.setColor(slider.getForeground());
        boolean north = Quadrant.forAngle(angle).isNorth();
        String txt = state.valueText(slider);
        Font font = slider.getFont();
        FontMetrics fm = g.getFontMetrics(font);
        int txtW = fm.stringWidth(txt);
        int txtH = fm.getMaxAscent() + fm.getMaxDescent();
        int offset = north ? txtH + fm.getMaxAscent() : -txtH;
        Insets insets = insets(slider);
        int availW = slider.getWidth() - (insets.left + insets.right);
        int txtX = insets.left + ((availW / 2) - (txtW / 2));
        int txtY = offset + (int) Math.round(circ.centerY());
        g.drawString(txt, txtX, txtY);
    }

    private Color drawFocusRing(Color lineColor, Circle circ, Graphics2D g) {
        lineColor = UIManager.getColor("Tree.line"); // NOI18N
        if (lineColor == null) {
            lineColor = new Color(180, 180, 255);
        }
        double focusRadius = circ.radius() - (circ.radius() / 3D);
        Circle fc = new Circle(circ.centerX(), circ.centerY(), focusRadius);
        g.setColor(lineColor);
        g.setStroke(THIN);
        g.draw(fc);
        return lineColor;
    }

    private void paintDragCaption(SliderState state, JSlider slider,
            Graphics2D g, Color lineColor, Circle circ, double angle) throws AssertionError {
        String txt = state.dragValueText(slider);
        Font font = slider.getFont();
        FontMetrics fm = g.getFontMetrics(font);
        int txtW = fm.stringWidth(txt);
        int txtH = fm.getMaxAscent() + fm.getMaxDescent();

        Quadrant quad = Quadrant.forAngle(state.dragAngle());
        boolean north = quad.isNorth();
        Rectangle2D textAreaBounds = circ.outBounds(quad);
        double widthFactor = textAreaBounds.getWidth() / txtW;
        double heightFactor = textAreaBounds.getHeight() / txtH;
        font = font.deriveFont(AffineTransform.getScaleInstance(widthFactor, heightFactor));
        fm = g.getFontMetrics(font);
        double txtTop;
        if (!north) {
            txtTop = textAreaBounds.getCenterY() + (fm.getDescent() / 1D);
        } else {
            txtTop = textAreaBounds.getCenterY() + (fm.getHeight() / 2D);
        }
        double txtX = textAreaBounds.getX();
        g.setFont(font);
        g.setColor(lineColor.darker());
        g.drawString(txt, (float) txtX, (float) txtTop);
    }

    private interface LC {

        void apply(Circle circ, Line2D.Double line, double angle);
    }

    @Override
    public void uninstallUI(JComponent c) {
        M m = state(c).m;
        if (m != null) {
            ((JSlider) c).getModel().removeChangeListener(m);
            c.removeMouseListener(m);
            c.removeMouseMotionListener(m);
            c.removeMouseWheelListener(m);
            c.removeKeyListener(m);
            c.removeFocusListener(m);
            c.removePropertyChangeListener("minimum", m); // NOI18N
            c.removePropertyChangeListener("maximum", m); // NOI18N
            c.removePropertyChangeListener("value", m); // NOI18N
            c.removePropertyChangeListener("model", m); // NOI18N
            c.putClientProperty(CLIENT_PROP_SLIDER_STATE, null); // NOI18N
        }
    }

    @Override
    public void installUI(JComponent c) {
        Color bg = UIManager.getColor("TextArea.background"); // NOI18N
        if (bg == null) {
            bg = UIManager.getColor("text"); // NOI18N
            if (bg == null) {
                bg = Color.WHITE;
            }
        }
        c.setBackground(bg);
        Color fg = UIManager.getColor("TextArea.foreground"); // NOI18N
        if (fg == null) {
            fg = UIManager.getColor("textText"); // NOI18N
            if (fg == null) {
                fg = Color.BLACK;
            }
        }
        Font f = c.getFont();
        if (f == null) {
            f = UIManager.getFont("controlFont"); //NOI18N
        }
        if (f != null) {
            f = f.deriveFont(Font.PLAIN, Math.max(6F, f.getSize2D() - 2));
            c.setFont(f);
        }
        M m = new M(c);
        c.setForeground(fg);
        c.setFocusable(true);
        c.putClientProperty(CLIENT_PROP_SLIDER_STATE, new SliderState(m)); // NOI18N
        c.addFocusListener(m);
        c.addMouseListener(m);
        c.addMouseMotionListener(m);
        c.addKeyListener(m);
        c.addMouseWheelListener(m);
        c.addPropertyChangeListener("minimum", m); // NOI18N
        c.addPropertyChangeListener("maximum", m); // NOI18N
        c.addPropertyChangeListener("value", m); // NOI18N
        c.addPropertyChangeListener("model", m); // NOI18N
        c.setOpaque(true);
        ((JSlider) c).getModel().addChangeListener(m);
    }

    SliderState state(JComponent c) {
        SliderState result = (SliderState) c.getClientProperty(CLIENT_PROP_SLIDER_STATE); // NOI18N
        return result.checkState((JSlider) c);
    }

    class M extends MouseAdapter implements KeyListener, MouseWheelListener, PropertyChangeListener, FocusListener, ChangeListener {

        private final JComponent c;

        public M(JComponent c) {
            this.c = c;
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            c.repaint();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            JSlider slider = (JSlider) e.getSource();
            if (!slider.hasFocus()) {
                slider.requestFocus();
            }
            SliderState state = state(slider);
            Point p = e.getPoint();
            if (state.isHit(slider, p)) {
                state.initDrag(slider, p);
                slider.repaint();
                e.consume();
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            JSlider slider = (JSlider) e.getSource();
            SliderState state = state(slider);
            if (state.isDragging()) {
                state.setLastDragPoint(slider, e.getPoint());
                slider.setCursor(Cursors.forComponent(slider)
                        .cursorPerpendicularTo(state.dragAngle()));
                e.consume();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            JSlider slider = (JSlider) e.getSource();
            SliderState state = state(slider);
            if (state.isDragging()) {
                state.setLastDragPoint(slider, e.getPoint());
                state.endDrag(slider, e.getPoint(), true);
                e.consume();
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            JSlider slider = (JSlider) e.getSource();
            SliderState state = state(slider);
            Point p = e.getPoint();
            if (state.isHit(slider, p)) {
                Cursor cursor = Cursors.forComponent(slider)
                        .cursorPerpendicularTo(
                                state.angle);
                slider.setCursor(cursor);
//                sl.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            } else {
                slider.setCursor(Cursor.getDefaultCursor());
            }
            e.consume();
        }

        @Override
        public void focusGained(FocusEvent e) {
            ((JSlider) e.getSource()).repaint();
        }

        @Override
        public void focusLost(FocusEvent e) {
            JSlider sl = (JSlider) e.getSource();
            sl.repaint();
            state(sl).abortDrag(sl);
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            JSlider slider = (JSlider) e.getSource();
            int val = slider.getValue();
            int min = slider.getMinimum();
            int max = slider.getMaximum();
            val += e.getUnitsToScroll();
            while (val < 0) {
                val = max + val;
            }
            while (val > max) {
                val -= (max - min);
            }
            slider.setValue(val);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            JSlider slider = (JSlider) e.getSource();
            int val = slider.getValue();
            int factor;
            if (e.isShiftDown()) {
                double max = slider.getMaximum();
                double min = slider.getMinimum();
                double tenth = (max - min) / 10D;
                factor = Math.max(1, (int) Math.round(tenth));
            } else {
                factor = 1;
            }
            SliderState state = state(slider);
            switch (e.getKeyCode()) {
                case KeyEvent.VK_O:
                case KeyEvent.VK_BACK_SPACE:

                    state.withCircleAndLine(slider, (circ, line, ang) -> {
                        Quadrant q = Quadrant.forAngle(ang);
                        double newAng = q.opposite().translate(q, ang);
                        int oppValue = state.valueForAngle(newAng, slider);
                        slider.setValue(oppValue);
                    });
                    break;
                case KeyEvent.VK_RIGHT:
                    int incremented = val + factor;
                    if (incremented >= slider.getMaximum()) {
                        incremented = slider.getMinimum()
                                + (incremented - slider.getMaximum());
                    }
                    slider.setValue(incremented);
                    e.consume();
                    break;
                case KeyEvent.VK_LEFT:
                    int decremented = val - factor;
                    if (decremented < slider.getMinimum()) {
                        decremented = slider.getMaximum()
                                - (slider.getMinimum() - decremented);
                    }
                    slider.setValue(decremented);
                    e.consume();
                    break;
                case KeyEvent.VK_UP:
                case KeyEvent.VK_DOWN:
                    state.withCircleAndLine(slider, (circ, line, ang) -> {
                        if (ang == 360) {
                            ang = 0;
                        }
                        Quadrant quad = Quadrant.forAngle(ang);
                        Quadrant next = e.getKeyCode() == KeyEvent.VK_DOWN
                                ? quad.next() : quad.prev();

                        double newAngle = next.translate(quad, val);
                        int nv = state.valueForAngle(newAngle, slider);
                        slider.setValue(nv);
                    });
                    e.consume();
                    break;
            }

        }

        @Override
        public void keyReleased(KeyEvent e) {
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            JComponent c = (JComponent) evt.getSource();
            if ("model".equals(evt.getPropertyName())) {
                BoundedRangeModel old = (BoundedRangeModel) evt.getOldValue();
                if (old != null) {
                    old.removeChangeListener(this);
                }
                BoundedRangeModel nue = (BoundedRangeModel) evt.getNewValue();
                if (nue != null) {
                    nue.addChangeListener(this);
                }
            }
            c.repaint();
        }
    }

    static class SliderState {

        private static final int HIT_DISTANCE = 7;
        M m;
        private Rectangle lastBounds = new Rectangle();
        private int min, max, val;
        private Circle circle;
        private Line2D.Double line;
        private double angle;
        private boolean dragging = false;
        private Point dragStartPoint;
        private Point lastDragPoint;
        private Line2D.Double[] dragLineHolder = new Line2D.Double[1];
        final Circle scratchCircle = new Circle();

        SliderState(M m) {
            this.m = m;
        }

        SliderState checkState(JSlider comp) {
            Rectangle newBounds = comp.getBounds();
            int newMin = comp.getMinimum();
            int newMax = comp.getMaximum();
            int newVal = comp.getValue();
            if (newMin != min || newMax != max || newVal != val || !newBounds.equals(lastBounds)) {
                circle = null;
                line = null;
                angle = 0;
                min = newMin;
                max = newMax;
                val = newVal;
                lastBounds.setFrame(newBounds);
            }
            return this;
        }

        boolean initDrag(JComponent comp, Point point) {
            dragStartPoint = point;
            comp.repaint();
            dragging = true;
            return true;
        }

        boolean isDragging() {
            return dragging;
        }

        private final double[] angScratch = new double[1];

        double angleForDragText(JComponent c) {
            withCircleAndLine(c, (circ, ln, ang) -> {
                if (dragging && lastDragPoint != null) {
                    angScratch[0] = circ.angleOf(lastDragPoint.x, lastDragPoint.y);
                } else {
                    angScratch[0] = ang;
                }
            });
            return angScratch[0];
        }

        void abortDrag(JComponent comp) {
            dragging = false;
            dragStartPoint = null;
            dragLineHolder[0] = null;
            lastDragPoint = null;
            comp.repaint();
        }

        void setLastDragPoint(JComponent comp, Point p) {
            if (!p.equals(lastDragPoint)) {
                lastDragPoint = p;
                comp.repaint();
            }
        }

        String valueText(JSlider comp) {
            StringConverter cvt = (StringConverter) comp.getClientProperty(StringConverter.CLIENT_PROP_CONVERTER); //NOI18N
            if (cvt != null) {
                return cvt.valueToString(comp);
            } else {
                return Integer.toString(comp.getValue());
            }
        }

        String dragValueText(JSlider comp) {
            StringConverter cvt = (StringConverter) comp.getClientProperty(StringConverter.CLIENT_PROP_CONVERTER); //NOI18N
            if (lastDragPoint == null) {
                if (cvt != null) {
                    return cvt.valueToString(comp);
                } else {
                    return Integer.toString(comp.getValue());
                }
            }
            String[] result = new String[1];
            withCircleAndLine(comp, (circ, line, ang) -> {
                assert circ != null;
                double a = circ.angleOf(lastDragPoint.x, lastDragPoint.y);
                int val = valueForAngle(a, comp);
                if (cvt != null) {
                    result[0] = cvt.valueToString(val);
                } else {
                    result[0] = Integer.toString(val);
                }
            });;
            return result[0];
        }

        Line2D.Double dragLine(JComponent comp) {
            if (lastDragPoint != null) {
                return dragLine(comp, lastDragPoint);
            }
            return null;
        }

        Line2D.Double dragLine(JComponent comp, Point point) {
            dragLineHolder[0] = null;
            withCircleAndLine(comp, (circ, line, ang) -> {
                double lineAng = circ.angleOf(point.x, point.y);
                Line2D.Double dragLine = circ.halfLine(lineAng, circ.radius() - 3);
                dragLineHolder[0] = dragLine;
            });
            return dragLineHolder[0];
        }

        void endDrag(JComponent comp, Point p, boolean updateValue) {
            if (lastDragPoint == null || lastDragPoint.equals(dragStartPoint)) {
                return;
            }
            withCircleAndLine(comp, (circ, line, ang) -> {
                double targetAngle = circ.angleOf(p.x, p.y);
                int newValue = valueForAngle(targetAngle, comp);
                ((JSlider) comp).setValue(newValue);
                abortDrag(comp);
            });
        }

        boolean[] hitScratch = new boolean[1];

        boolean isHit(JComponent comp, Point point) {
            hitScratch[0] = false;
            withCircleAndLine(comp, (circ, line, ang) -> {
                double dist = line.ptLineDist(point);
                boolean result = dist <= HIT_DISTANCE;
                double pointAngle = circ.angleOf(point.x, point.y);
                Quadrant q = Quadrant.forAngle(pointAngle);
                Quadrant angQ = Quadrant.forAngle(ang);
                if (q != angQ && q != angQ.prev() && q != angQ.next()) {
                    result = false;
                }
                hitScratch[0] = result;
            });
            return hitScratch[0];
        }

        void withCircleAndLine(JComponent comp, LC cns) {
            if (circle != null && line != null) {
                cns.apply(circle, line, angle);
                return;
            }
            circle = circle(comp);
            angle = angle(comp);
            double[] pos = circle.positionOf(angle, Math.max(3, circle.radius() - (circle.radius() * 0.1)));
            line = new Line2D.Double(circle.centerX(),
                    circle.centerY(), pos[0], pos[1]);
            cns.apply(circle, line, angle);
        }

        Circle circle(JComponent comp) {
            if (circle != null) {
                return circle;
            }
            Insets ins = comp.getInsets();
            if (ins == null) {
                ins = EMPTY_INSETS;
            }
            int w = comp.getWidth() - (ins.left + ins.right);
            int h = comp.getHeight() - (ins.top + ins.bottom);
            int radius = (Math.min(w, h) - 2) / 2;
            int centerx = ins.left + (w / 2);
            int centery = ins.right + (h / 2);
            return new Circle(centerx, centery, radius);
        }

        public int valueForAngle(double angle, JComponent c) {
            JSlider slider = (JSlider) c;
            double min = slider.getMinimum();
            double max = slider.getMaximum();
            double val = slider.getValue();

            double range = (max - min);
            double value = ((angle / 360) * range) + min;
            return (int) Math.round(value);
        }

        private double angle(JComponent c) {
            JSlider slider = (JSlider) c;
            double min = slider.getMinimum();
            double max = slider.getMaximum();
            double val = slider.getValue();
            val -= min;
            if (max - min == 0) {
                return 0;
            }
            return 360D * (val / (max - min));
        }

        private double dragAngle() {
            if (lastDragPoint == null) {
                return angle;
            }
            return circle.angleOf(lastDragPoint.x, lastDragPoint.y);
        }
    }
    /*
    public static void main(String[] args) {
        UIManager.put("control", new Color(80, 40, 40));
        UIManager.put("controlShadow", new Color(120, 60, 50));
        UIManager.put("Slider.foreground", new Color(255, 255, 180));
        UIManager.put("textText", new Color(255, 255, 180));
        UIManager.put("text", new Color(80, 40, 40));
        UIManager.put("white", new Color(80, 40, 40));
        UIManager.put("Tree.line", new Color(220, 150, 80));

        JFrame jf = new JFrame();
        jf.getContentPane().setLayout(new BorderLayout());
        JPanel pnl = new JPanel(new FlowLayout());
        jf.getContentPane().add(pnl, BorderLayout.CENTER);
        JLabel lbl = new JLabel("Slider!"); // NOI18N
        pnl.add(lbl);
        JSlider slider = new JSlider(0, 360, 80);

        RadialSliderUI.attach(slider);

        slider.setBackground(Color.black);
        slider.setForeground(Color.WHITE);

        RadialSliderUI.setSmallCaptions(slider);
        RadialSliderUI.setStringConverter(slider, new StringConverter() {
            @Override
            public String valueToString(JSlider sl) {
                return valueToString(sl.getValue());
            }

            @Override
            public int maxChars() {
                return 7;
            }

            @Override
            public String valueToString(int val) {
                return val + "\u00B0";
            }
        });

        pnl.add(slider);
        JSlider other = new JSlider(0, 360, 90);
        other.getModel().addChangeListener(ev -> {
            slider.setValue(other.getValue());
        });
        pnl.add(other);
        JButton b = new JButton("Reset");
        pnl.add(b);
        b.addActionListener(ae -> {
            slider.setValue(0);
        });
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.pack();
        jf.setVisible(true);
    }
     */
}
