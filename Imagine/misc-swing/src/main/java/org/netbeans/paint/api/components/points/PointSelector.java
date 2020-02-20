package org.netbeans.paint.api.components.points;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_STROKE_CONTROL;
import static java.awt.RenderingHints.KEY_TEXT_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_STROKE_PURE;
import java.awt.Stroke;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import static java.awt.geom.AffineTransform.getScaleInstance;
import static java.awt.geom.AffineTransform.getTranslateInstance;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import org.imagine.geometry.Circle;
import org.imagine.geometry.Triangle;
import org.netbeans.paint.api.components.RadialSliderUI;
import static org.netbeans.paint.api.components.points.PointSelectorMode.POINT_AND_LINE;
import org.openide.util.Exceptions;

/**
 *
 * @author Tim Boudreau
 */
public final class PointSelector extends JComponent {

    private final Point2D.Double targetScaled;
    private final Point2D.Double focusScaled;
    private final Rectangle2D.Double targetBounds;
    private final int sizeBase;
    private Color marginColor;
    private Color focusColor;
    private final Circle circle = new Circle(0, 0, 1);
    private L l = new L();
    private static final DecimalFormat FMT = new DecimalFormat("#####0.##");
    private PointSelectorMode mode = PointSelectorMode.POINT_AND_LINE;
    private static double TEXT_MARGIN = 3;
    private final BasicStroke FOCUS_STROKE = new BasicStroke(0.5F, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1F, new float[]{5F, 10F}, 0);
    private PointSelectorBackgroundPainter bgPainter;
    private Point2D.Double proposedTarget;
    private final Line2D.Double scratchLine = new Line2D.Double();
    private double angle;
    private Point2D.Double proposedFocus;
    private boolean gridPainted;

    public PointSelector(Rectangle2D.Double bounds) {
        this(bounds, 120);
    }

    public PointSelector(Rectangle2D.Double bounds, int sizeBase) {
        assert bounds != null : "bounds null";
        this.sizeBase = sizeBase;
        this.targetScaled = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
        this.focusScaled = new Point2D.Double(bounds.getCenterX()
                + (bounds.getWidth() / 2),
                bounds.getCenterY() + (bounds.getHeight() / 2));

        this.targetBounds = bounds;
        Color bg = UIManager.getColor("text");
        if (bg == null) {
            bg = UIManager.getColor("white");
            if (bg == null) {
                bg = Color.WHITE;
            }
        }
        setBackground(bg);
        Color fg = UIManager.getColor("textText");
        if (fg == null) {
            fg = UIManager.getColor("black");
            if (fg == null) {
                fg = Color.BLACK;
            }
        }
        setForeground(fg);
        Color margin = UIManager.getColor("control");
        if (margin == null) {
            margin = new Color(235, 180, 180);
            marginColor = margin;
        }
        Color focus = UIManager.getColor("Button.focus");
        if (focus == null) {
            focus = new Color(180, 180, 255);
        }
        focusColor = focus;
        Font font = UIManager.getFont("controlFont");
        if (font == null) {
            font = new Font("SansSerif", Font.PLAIN, 10);
        }
        setFont(font);
        setFocusable(true);
    }

    public void setMode(PointSelectorMode mode) {
        if (mode != this.mode) {
            PointSelectorMode oldMode = this.mode;
            this.mode = mode;
            firePropertyChange("mode", oldMode, mode);
        }
    }

    public PointSelectorMode getMode() {
        return mode;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        addFocusListener(l);
        addMouseListener(l);
        addMouseMotionListener(l);
        addKeyListener(l);
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    @Override
    public void removeNotify() {
        ToolTipManager.sharedInstance().unregisterComponent(this);
        removeKeyListener(l);
        removeMouseMotionListener(l);
        removeMouseListener(l);
        removeFocusListener(l);
        super.removeNotify();
    }

    public double aspectRatio() {
        double w = targetBounds.getWidth();
        double h = targetBounds.getHeight();
        return (w == 0 || h == 0) ? 1 : w / h;
    }

    private double inverseAspectRatio() {
        double w = targetBounds.getWidth();
        double h = targetBounds.getHeight();
        return (w == 0 || h == 0) ? 1 : h / w;
    }

    public void setGridPainted(boolean gridPainted) {
        if (gridPainted != this.gridPainted) {
            this.gridPainted = gridPainted;
            firePropertyChange("gridPainted", !gridPainted,
                    gridPainted);
        }
    }

    public boolean isGridPainted() {
        return gridPainted;
    }

    public void setMarginColor(Color color) {
        Color oldTray = marginColor;
        if (!Objects.equals(color, oldTray)) {
            marginColor = color;
            firePropertyChange("trayColor", oldTray, color);
            repaint();
        }
    }

    public void setFocusColor(Color color) {
        Color oldFocus = focusColor;
        if (!Objects.equals(color, oldFocus)) {
            focusColor = color;
            firePropertyChange("trayColor", oldFocus, color);
            repaint();
        }
    }

    public boolean isShowAngle() {
        return mode.isShowAngle();
    }

    public void setAngle(double angle) {
        if (mode.isDrawable()) {
            Circle circ = new Circle(targetScaled.x, targetScaled.y,
                    Point2D.distance(targetScaled.x, targetScaled.y,
                            focusScaled.x, focusScaled.y));
            Point2D nue = circ.getPosition(angle);
            setFocusPoint(nue);
        } else {
            if (angle != this.angle) {
                double old = this.angle;
                this.angle = angle;
                firePropertyChange("angle", old, this.angle);
                repaint();
            }
        }
    }

    public double getAngle() {
        if (mode.isDrawable()) {
            Circle circ = new Circle(targetScaled.x, targetScaled.y,
                    Point2D.distance(targetScaled.x, targetScaled.y,
                            focusScaled.x, focusScaled.y));

            return circ.angleOf(focusScaled.x, focusScaled.y);
        }
        return angle;
    }

    public Color getMarginColor() {
        return marginColor;
    }

    public Point2D.Double getTarget() {
        return new Point2D.Double(targetScaled.getX(), targetScaled.getY());
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(sizeBase, (int) Math.ceil(sizeBase * aspectRatio()));
    }

    @Override
    public Point getToolTipLocation(MouseEvent event) {
        return new Point(0, 0);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    Point2D.Double targetLocal() {
        Point2D.Double result = getTarget();
        toLocal().transform(result, result);
        return result;
    }

    public PointSelector setBackgroundPainter(PointSelectorBackgroundPainter p) {
        this.bgPainter = p;
        return this;
    }

    void setTargetLocal(Point2D inComponentCoords) {
        AffineTransform xform = toGlobal();
        Point2D.Double nue = new Point2D.Double(inComponentCoords.getX(), inComponentCoords.getY());
        xform.transform(nue, nue);
        targetScaled.setLocation(nue);
    }

    @Override
    public void paintComponent(Graphics gr) {
        Graphics2D g = (Graphics2D) gr;
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g.setRenderingHint(KEY_STROKE_CONTROL, VALUE_STROKE_PURE);
        g.setRenderingHint(KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        int w = getWidth();
        int h = getHeight();
        Insets ins = getInsets();

        g.setColor(getMarginColor());
        g.fillRect(0, 0, w, h);

        Rectangle2D.Double frame = scaledBounds();
        Point2D.Double targetLocal = targetLocal();
        if (bgPainter != null) {
            bgPainter.paintBackground(g, targetLocal, frame, angle, mode, this);
        } else {
            if (gridPainted) {
                g.setColor(getBackground());
                g.fill(frame);
                paintGrid(g, frame);
            } else {
                g.setColor(getBackground());
                g.fill(frame);
            }
        }
        g.setColor(getForeground());
        g.draw(frame);

        g.setFont(getFont());
        FontMetrics fm = g.getFontMetrics();
        double oneCharHeight = fm.getAscent() + fm.getDescent();

        prepareCircle(g, targetLocal, oneCharHeight);

        boolean focus = hasFocus();
        if (focus) {
            paintFocusRect(g, frame, oneCharHeight);
        }

        if (bgPainter != null) {
            g.setXORMode(Color.YELLOW);
        }

        paintPoints(g, frame, targetLocal, oneCharHeight);

        if (mode.isDrawable() && proposedFocus != null
                && proposedTarget != null) {
            paintDraggingFeedback(g, oneCharHeight);
        }
        if (bgPainter != null) {
            g.setPaintMode();
        }

        paintPointPosition(fm, frame, h, oneCharHeight, ins, g);
    }

    private void paintDraggingFeedback(Graphics2D g, double oneCharHeight) {
        double radius = oneCharHeight / 2;
        circle.setRadius(oneCharHeight / 2);
        circle.setCenter(proposedTarget);
        g.setColor(getForeground());
        g.fill(circle);
        g.draw(circle);

        Triangle triangle = Triangle.isoceles(proposedFocus, radius);
        g.draw(triangle);
        g.setColor(getBackground());
        g.fill(triangle);

        g.setColor(getForeground());
        Line2D.Double line = new Line2D.Double(proposedTarget, proposedFocus);
        g.draw(line);
    }

    private void paintPoints(Graphics2D g, Rectangle2D.Double frame,
            Point2D.Double targetLocal, double oneCharHeight) {
        if (mode.isShowAngle()) {
            Graphics2D g1 = (Graphics2D) g.create();
            try {
                paintAngle(g1, frame, targetLocal);
            } finally {
                g1.dispose();
            }
        }
        g.setColor(getBackground());
        g.fill(circle);
        g.setColor(getForeground());
        g.draw(circle);
        if (mode.isDrawable()) {
            Point2D.Double focus = copy(focusScaled);
            toLocal().transform(focus, focus);
            Triangle tri = Triangle.isoceles(focus, oneCharHeight / 2);
            g.setColor(getBackground());
            g.fill(tri);
            g.setColor(getForeground());
            g.draw(tri);
        }
    }

    private void prepareCircle(Graphics2D g, Point2D.Double targetLocal, double oneCharHeight) {
        g.setStroke(new BasicStroke(2));
        g.setColor(getForeground());
        circle.setCenter(targetLocal.getX(), targetLocal.getY());
        circle.setRadius(oneCharHeight / 2D);
    }

    private void paintGrid(Graphics2D g, Rectangle2D frame) {
        g.setColor(getGridColor());
        g.setStroke(FOCUS_STROKE);
        double xThird = frame.getWidth() / 3D;
        double yThird = frame.getHeight() / 3D;

        scratchLine.setLine(frame.getX(), frame.getY() + yThird,
                frame.getX() + frame.getWidth(), frame.getY() + yThird);
        g.draw(scratchLine);

        scratchLine.setLine(frame.getX(), frame.getY() + (yThird * 2),
                frame.getX() + frame.getWidth(), frame.getY() + (yThird * 2));
        g.draw(scratchLine);

        scratchLine.setLine(frame.getX() + xThird, frame.getY(), frame.getX() + xThird,
                frame.getY() + frame.getHeight());
        g.draw(scratchLine);

        scratchLine.setLine(frame.getX() + (xThird * 2), frame.getY(),
                frame.getX() + (xThird * 2),
                frame.getY() + frame.getHeight());
        g.draw(scratchLine);

        scratchLine.setLine(frame.getX(), frame.getY(),
                frame.getX() + frame.getWidth(),
                frame.getY() + frame.getHeight());
        g.draw(scratchLine);

        scratchLine.setLine(frame.getX() + frame.getWidth(), frame.getY(),
                frame.getX(),
                frame.getY() + frame.getHeight());
        g.draw(scratchLine);
    }

    private Color getGridColor() {
        Color c = UIManager.getColor("controlShadow");
        if (c == null) {
            c = UIManager.getColor("textInactiveText");
            if (c == null) {
                c = Color.LIGHT_GRAY;
            }
        }
        return c;
    }

    private void paintPointPosition(FontMetrics fm, Rectangle2D.Double frame, int h, double oneCharHeight, Insets ins, Graphics2D g) {
        String txt = getText();
        double txtW = fm.stringWidth(txt);
        double txtX = (frame.getX() + frame.getWidth()) - (TEXT_MARGIN + txtW);
        double txtY;
        double bottomRemainder = h - (frame.getY() + frame.getHeight());
        if (oneCharHeight < bottomRemainder) {
            g.setColor(textColor());
            txtY = h - (ins.bottom + TEXT_MARGIN);
        } else {
            g.setColor(getForeground());
            txtY = (frame.getY() + frame.getHeight()) - TEXT_MARGIN;
        }
        g.drawString(txt, (float) txtX, (float) txtY);
    }

    private Color textColor() {
        Color c = getMarginColor();
        if (c == null) {
            c = getForeground();
        }
        if (avgBrightness(c) > 128) {
            return Color.BLACK;
        } else {
            return Color.WHITE;
        }
    }

    private int avgBrightness(Color c) {
        return (c.getRed() + c.getGreen() + c.getBlue()) / 3;
    }

    private void paintFocusRect(Graphics2D g, Rectangle2D.Double frame, double oneCharHeight) {
        g.setColor(this.focusColor);
        Stroke old = g.getStroke();
        g.setStroke(FOCUS_STROKE);
        Rectangle2D.Double focusRect = new Rectangle2D.Double(frame.getX(), frame.getY(), frame.getWidth(), frame.getHeight());
        focusRect.x += oneCharHeight * 1.5;
        focusRect.y += oneCharHeight * 1.5;
        focusRect.width -= oneCharHeight * 3;
        focusRect.height -= oneCharHeight * 3;
        g.draw(focusRect);
        g.setStroke(old);
    }

    private void paintAngle(Graphics2D g, Rectangle2D frame, Point2D.Double localPoint) {
        if (mode.isDrawable()) {
            double[] dbls = new double[]{targetScaled.x, targetScaled.y,
                focusScaled.x, focusScaled.y};
            toLocal().transform(dbls, 0, dbls, 0, 2);
            Line2D.Double line = new Line2D.Double(dbls[0], dbls[1], dbls[2], dbls[3]);
            g.draw(line);
        } else {
            Circle circ = new Circle(localPoint).setRadius(getWidth() / 4);
            Rectangle frameBounds = frame.getBounds();
            Graphics2D g1 = (Graphics2D) g.create();
            g1.setClip(frameBounds);
            g1.setColor(Color.RED);
            g1.draw(circ.halfLine(angle));
            g1.dispose();
        }
    }

    private Rectangle2D.Double scaledBounds() {
        double[] pts = new double[]{targetBounds.getX(), targetBounds.getY(), targetBounds.getX() + targetBounds.getWidth(), targetBounds.getHeight() + targetBounds.getY()};
        toLocal().transform(pts, 0, pts, 0, 2);
        return new Rectangle2D.Double(pts[0], pts[1], pts[2] - pts[0], pts[3] - pts[1]);
    }

    public String getText() {
        if (mode.isDrawable()) {
            return FMT.format(targetScaled.x) + ", "
                    + FMT.format(targetScaled.y)
                    + " / " + FMT.format(focusScaled.x) + ", "
                    + FMT.format(focusScaled.y);
        } else {
            return FMT.format(targetScaled.x) + ", " + FMT.format(targetScaled.y);
        }
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        Point2D pt = e.getPoint();
        toGlobal().transform(pt, pt);
        return FMT.format(pt.getX()) + "," + FMT.format(pt.getY());
    }

    private AffineTransform toLocal() {
        double w = getWidth();
        double h = getHeight();
        Insets ins = getInsets();
        double workingHeight = Math.max(1, h - (ins.top + ins.bottom));
        double workingWidth = Math.max(1, w - (ins.left + ins.right));

        double rectX, rectY, rectW, rectH;

        if (targetBounds.getWidth() > targetBounds.getHeight()) {
            if (workingWidth > workingHeight) {
                // match
                rectW = workingWidth * (targetBounds.getHeight() / targetBounds.getWidth());
                rectH = workingHeight;
                rectY = ins.top;
                rectX = ins.left + (workingWidth / 2) - (rectW / 2);
            } else {
                // mismatched aspect ratio
                rectW = workingWidth;
                rectH = workingHeight * inverseAspectRatio();
                rectY = ins.top + (workingHeight / 2) - (rectH / 2);
                rectX = ins.left;
            }
        } else {
            if (workingWidth > workingHeight) {
                // match
                rectH = workingHeight;
                rectW = workingWidth * aspectRatio();
                rectY = ins.top;
                rectX = ins.left + (workingWidth / 2) - (rectW / 2);
            } else {
                // mismatched aspect ratio
                rectH = workingHeight;
                rectW = workingWidth * aspectRatio();
                rectY = ins.top;
                rectX = ins.left + (workingWidth / 2) - (rectW / 2);
            }
        }
        AffineTransform transform = getTranslateInstance(-targetBounds.x, -targetBounds.y);
        transform.concatenate(AffineTransform.getScaleInstance(rectW / targetBounds.width, rectH / targetBounds.height));
        transform.preConcatenate(getTranslateInstance(rectX, rectY));
        return transform;
    }

    private AffineTransform ytoLocal() {
        double w = getWidth();
        double h = getHeight();
        Insets ins = getInsets();
        double workingHeight = Math.max(1, h - (ins.top + ins.bottom));
        double workingWidth = Math.max(1, w - (ins.left + ins.right));

        double heightToUse = workingHeight * inverseAspectRatio();

        if (heightToUse > workingHeight) {
            heightToUse = workingHeight;
        }

        double centerY = ins.top + (workingHeight / 2D);
        double rectY = centerY - (heightToUse / 2D);
        double scaleX = (workingWidth / targetBounds.getWidth());
        double scaleY = scaleX;

        AffineTransform xform
                = getTranslateInstance(-targetBounds.getX(),
                        -targetBounds.getY());
        xform.concatenate(getScaleInstance(scaleX, scaleY));
        xform.concatenate(getTranslateInstance(
                ins.left, rectY));

//        AffineTransform xform = getTranslateInstance(
//                ins.left, rectY);
//        xform.concatenate(getScaleInstance(scaleX, scaleY));
//        xform.concatenate(getTranslateInstance(-targetBounds.getX(),
//                -targetBounds.getY()));
        return xform;
    }

    private AffineTransform xtoLocal() {
        double w = getWidth();
        double h = getHeight();
        Insets ins = getInsets();
        double workingHeight = Math.max(1, h - (ins.top + ins.bottom));
        double workingWidth = Math.max(1, w - (ins.left + ins.right));

        double heightToUse = workingWidth * (1D / aspectRatio());
        double centerY = ins.top + (workingHeight / 2D);
        double rectY = centerY - (heightToUse / 2D);
        double scaleX = (workingWidth / targetBounds.getWidth());
        double scaleY = scaleX;
        AffineTransform xform = getTranslateInstance(
                ins.left, rectY);
        xform.concatenate(getScaleInstance(scaleX, scaleY));
        xform.concatenate(getTranslateInstance(-targetBounds.getX(),
                -targetBounds.getY()));
        return xform;
    }

    private AffineTransform toGlobal() {
        try {
            return toLocal().createInverse();
        } catch (NoninvertibleTransformException ex) {
            Exceptions.printStackTrace(ex);
            return getTranslateInstance(0, 0);
        }
    }

    private void changeTarget(double dx, double dy) {
        Point2D.Double old = new Point2D.Double(targetScaled.getX(), targetScaled.getY());
        targetScaled.x += dx;
        targetScaled.y += dy;
        constrain(targetScaled);
        firePropertyChange("targetPoint", old, targetScaled);
        repaint();
    }

    private void changeFocus(double dx, double dy) {
        Point2D.Double old = new Point2D.Double(targetScaled.getX(), targetScaled.getY());
        focusScaled.x += dx;
        focusScaled.y += dy;
        constrain(focusScaled);
        firePropertyChange("focusPoint", old, focusScaled);
        repaint();
    }

    private Point2D constrain(Point2D p) {
        if (p.getX() < targetBounds.x) {
            p.setLocation(targetBounds.x, p.getY());
        }
        if (p.getY() < targetBounds.y) {
            p.setLocation(p.getX(), targetBounds.y);
        }
        if (p.getX() > targetBounds.x + targetBounds.width) {
            p.setLocation(targetBounds.x + targetBounds.width, p.getY());
        }
        if (p.getY() > targetBounds.y + targetBounds.height) {
            p.setLocation(p.getX(), targetBounds.y + targetBounds.height);
        }
        return p;
    }

    public void setTargetPoint(Point2D nue) {
        if (nue.getX() != targetScaled.getX() && nue.getY() != targetScaled.getY()) {
            Point2D.Double old = new Point2D.Double(targetScaled.getX(), targetScaled.getY());
            targetScaled.setLocation(nue);
            constrain(targetScaled);
            firePropertyChange("targetPoint", old, constrain(nue));
            repaint();
        }
    }

    public void setFocusPoint(Point2D nue) {
        if (nue.getX() != focusScaled.getX() && nue.getY() != focusScaled.getY()) {
            Point2D.Double old = new Point2D.Double(focusScaled.getX(), focusScaled.getY());
            this.focusScaled.setLocation(nue);
            constrain(focusScaled);
            firePropertyChange("focusPoint", old, constrain(nue));
            repaint();
        }
    }

    public Point2D getTargetPoint() {
        return copy(targetScaled);
    }

    public Point2D getFocusPoint() {
        return copy(focusScaled);
    }

    public Color getFocusColor() {
        return focusColor;
    }

    public void reset() {
        setTargetPoint(new Point2D.Double(targetBounds.getCenterX(), targetBounds.getCenterY()));
        setFocusPoint(new Point2D.Double(targetBounds.getCenterX() - (targetBounds.getWidth() / 2),
                targetBounds.getCenterY() - (targetBounds.getHeight() / 2)));
    }

    private static Point2D.Double copy(Point2D p) {
        return new Point2D.Double(p.getX(), p.getY());
    }

    class L extends MouseAdapter implements KeyListener, MouseMotionListener, FocusListener {

        private Point pressPoint;

        private void abort() {
            Rectangle repaintBounds = null;
            if (proposedFocus != null && proposedTarget != null) {
                repaintBounds = new Rectangle();
                repaintBounds.add(proposedTarget);
                repaintBounds.add(proposedFocus);
                // Compensate for painted circle size
                repaintBounds.x -= 5;
                repaintBounds.y -= 5;
                repaintBounds.width += 10;
                repaintBounds.height += 10;
            }
            pressPoint = null;
            proposedFocus = null;
            proposedTarget = null;
            if (repaintBounds != null) {
                repaint(repaintBounds);
            }
        }

        @Override
        public void focusGained(FocusEvent e) {
            repaint();
        }

        @Override
        public void focusLost(FocusEvent e) {
            repaint();
            abort();
        }

        @Override
        public void mouseMoved(MouseEvent e) {
        }

        @Override
        public void mousePressed(MouseEvent e) {
            pressPoint = e.getPoint();
            if (mode.isDrawable()) {
                proposedTarget = proposedFocus = copy(e.getPoint());
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (pressPoint != null) {
                boolean wasClick = pressPoint.equals(e.getPoint());
                pressPoint = null;
                if (wasClick) {
                    return;
                }
                if (mode.isDrawable() && proposedTarget != null && proposedFocus != null && !proposedTarget.equals(proposedFocus)) {
                    AffineTransform global = toGlobal();
                    global.transform(proposedTarget, proposedTarget);
                    global.transform(proposedFocus, proposedFocus);
                    setFocusPoint(proposedFocus);
                    setTargetPoint(proposedTarget);
                    proposedFocus = null;
                    proposedTarget = null;
                    e.consume();
                }
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (pressPoint != null) {
                switch (mode) {
                    case POINT_ONLY:
                    case POINT_AND_ANGLE:
                        Point2D pt = e.getPoint();
                        toGlobal().transform(pt, pt);
                        setTargetPoint(pt);
                        e.consume();
                    case POINT_AND_LINE:
                        proposedFocus = copy(e.getPoint());
                        repaint();
                        break;
                }
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (!e.isPopupTrigger() && e.getClickCount() == 1) {
                if (!hasFocus()) {
                    requestFocus();
                }
                Point2D pt = e.getPoint();
                toGlobal().transform(pt, pt);
                setTargetPoint(pt);
                abort();
                e.consume();
            } else if (e.isPopupTrigger() && e.getClickCount() == 1
                    && mode == POINT_AND_LINE) {
                Point2D pt = e.getPoint();
                toGlobal().transform(pt, pt);
                setFocusPoint(pt);
                abort();
                e.consume();
            } else if (!e.isPopupTrigger() && e.getClickCount() > 1) {
                if (!hasFocus()) {
                    requestFocus();
                }
                reset();
                e.consume();
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
            double mult = e.isShiftDown() ? 100 : e.isControlDown() ? 1 : 10;
            switch (e.getKeyCode()) {
                case KeyEvent.VK_DOWN:
                    if (e.isAltDown()) {
                        changeFocus(0, 0.1D * mult);
                    } else {
                        changeTarget(0, 0.1D * mult);
                    }
                    e.consume();
                    break;
                case KeyEvent.VK_UP:
                    if (e.isAltDown()) {
                        changeFocus(0, -0.1D * mult);
                    } else {
                        changeTarget(0, -0.1D * mult);
                    }
                    e.consume();
                    break;
                case KeyEvent.VK_LEFT:
                    if (e.isAltDown()) {
                        changeFocus(-0.1D * mult, 0);
                    } else {
                        changeTarget(-0.1D * mult, 0);
                    }
                    e.consume();
                    break;
                case KeyEvent.VK_RIGHT:
                    if (e.isAltDown()) {
                        changeFocus(0.1D * mult, 0);
                    } else {
                        changeTarget(0.1D * mult, 0);
                    }
                    e.consume();
                    break;
                case KeyEvent.VK_ESCAPE:
                    reset();
                    break;
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }
    }

    public static void main(String[] args) {
        JFrame jf = new JFrame();
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.setLayout(new BorderLayout());
        TestPointPainter ptr = new TestPointPainter();
        jf.add(ptr, BorderLayout.CENTER);

        PointSelector sel = new PointSelector(ptr.bds, 300);
        sel.setMode(POINT_AND_LINE);
        sel.addPropertyChangeListener("targetPoint", evt -> {
            ptr.setPoint((Point2D) evt.getNewValue());
        });

        sel.addPropertyChangeListener("focusPoint", evt -> {
            ptr.setOtherPoint((Point2D) evt.getNewValue());
        });

        ptr.addPropertyChangeListener("bds", e -> {
            sel.repaint();
        });
        ptr.addPropertyChangeListener("point", e -> {
            Point2D p = (Point2D) e.getNewValue();
            sel.setTargetPoint(p);
        });
        ptr.addPropertyChangeListener("otherPoint", e -> {
            Point2D p = (Point2D) e.getNewValue();
            sel.setTargetPoint(p);
        });
        sel.setMarginColor(Color.LIGHT_GRAY);
        sel.setFocusColor(Color.BLUE);
        sel.setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5, Color.WHITE));
        sel.setAngle(107.5);
        sel.setBackgroundPainter((Graphics2D g, Point2D target, Rectangle2D frame, double angle1, PointSelectorMode mode, PointSelector sel1) -> {
            Line2D line = new Line2D.Double(sel1.getTargetPoint(), sel1.getFocusPoint());

            MultipleGradientPaint gp = new LinearGradientPaint((float) line.getX1(),
                    (float) line.getY1(),
                    (float) line.getX2(), (float) line.getY2(),
                    new float[]{0F, 0.3F, 1F}, new Color[]{Color.RED, Color.GREEN, Color.BLUE});
            g.setPaint(gp);
            g.fill(frame);
        });

        JSlider slider = new JSlider(0, 360, 105);
        RadialSliderUI.attach(slider);
        jf.add(slider, BorderLayout.WEST);
        slider.addChangeListener(cl -> {
            sel.setAngle(slider.getValue());
        });

        JButton nothing = new JButton("Nothing");
        jf.add(nothing, BorderLayout.SOUTH);
        jf.add(sel, BorderLayout.EAST);
        jf.setBounds(20, 20, 700, 500);
        jf.setVisible(true);
    }

}
