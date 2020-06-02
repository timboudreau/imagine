package org.netbeans.paint.tools.path;

import com.mastfrog.function.state.Bool;
import java.awt.AlphaComposite;
import static java.awt.AlphaComposite.SRC_OVER;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JToolBar;
import javax.swing.WindowConstants;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import net.java.dev.imagine.api.image.Surface;
import org.imagine.editor.api.CheckerboardBackground;
import org.imagine.editor.api.EditorBackground;
import org.imagine.editor.api.ImageEditorBackground;
import org.imagine.editor.api.PaintingStyle;
import org.imagine.geometry.Circle;
import org.imagine.geometry.EnhRectangle2D;
import org.imagine.geometry.EqLine;
import org.imagine.geometry.EqPointDouble;
import org.imagine.geometry.path.PathElementKind;
import org.imagine.geometry.util.PooledTransform;
import org.imagine.utils.java2d.GraphicsUtils;
import org.netbeans.paint.api.cursor.Cursors;
import org.netbeans.paint.api.components.EnumComboBoxModel;
import org.imagine.geometry.uirect.MutableRectangle2D;
import org.netbeans.paint.tools.minidesigner.MiniToolCanvas;
import org.imagine.geometry.uirect.ResizeMode;
import org.imagine.help.api.HelpItem;
import org.imagine.help.api.annotations.Help;
import org.imagine.help.api.annotations.Help.HelpText;
import org.netbeans.paint.tools.responder.HoverPointResponder;
import org.netbeans.paint.tools.responder.PaintingResponder;
import org.netbeans.paint.tools.responder.PathUIProperties;
import org.netbeans.paint.tools.responder.Responder;
import org.netbeans.paint.tools.responder.ResponderTool;
import org.netbeans.paint.tools.spi.CursorSupport;
import org.openide.util.Utilities;

/**
 *
 * @author Tim Boudreau
 */
@ToolDef(name = "Path", iconPath = "org/netbeans/paint/tools/resources/path.svg",
        category = "vector")
@Tool(value = Surface.class, toolbarPosition = 2000)
@Help(id="Overview", content = @HelpText("# Path Tool\n\nThe path tool allows you to design"
        + " shapes with precision, using\n\n"
        + " * Straight Lines\n"
        + " * Quadratic Curves - composed of a destination point and a single control point "
        + "which determines the shape of the curve\n"
        + " * Bezier Curves - composed of a destination point and _two_ control points "
        + "which determine the shape of the curve\n\n"
        + "These three elements are the basis of most two-dimensional shape drawing in "
        + "SVG, truetype fonts and many other areas of computer graphics."))
public class PathTool2 extends ResponderTool {

    PathModel model = new PathModel();

    public PathTool2(Surface obj) {
        super(obj);
//        printStackTraceOnTransitions(WithSelectionResponder.class, ConnectingPointResponder.class);
        logTransitions();
    }

    @Help(id="Using", content = @HelpText(value="# Using the Path Tool\n\nWhen activated, the Path Tool "
            + "is ready for you to click to create the first point in the shape - choose a spot "
            + "to create that initial point.\n\nAfter the initial point, you have a choice of "
            + "what _kind of path segment_ to create to the next point.  Simply clicking will "
            + "create a straight line.\n\nTo create a _quadratic curve_, hold down the *CTRL* "
            + "key when you click - you will then place the control point which changes the"
            + "shape of the curve visibly as you move the mouse.\n\nTo create a _cubic curve_,"
            + "hold down the *SHIFT* key when you click - you will then place the two control "
            + "points.\n\n"
            + "When placing a destination (not control) point, if you move the mouse cursor over "
            + "an existing point, you can drag that point to move it - so points are editable after "
            + "they are placed.\n\n"
            + "## Keyboard Shortcuts\n\n"
            + " * Escape - erases all points in the partially completed shape and starts over with "
            + "positioning the initial point\n"
            + " * Up / Down / Left / Right arrow keys - moves the position where the new point will "
            + "be placed using the keyboard rather than the mouse\n"
            + " * Backspace - delete the last-created destination points and any control points "
            + "belonging to it\n"
            + " * Tab - Tab between points which the Up / Down / Left / Right arrows will move\n"
            + " * Space - Create a point at the current mouse-cursor position or position moved "
            + "to using the keyboard - the same *CTRL* and *SHIFT* options determine if the new "
            + "line will be a straight line or a _quadratic_ or _cubic_ curve\n"
            + " * P - enable / disable preview mode, showing the shape with the currently selected "
            + "fill color or pattern and stroke\n"
            + " * Equals / Plus - Zoom in\n"
            + " * Minus - Zoom out\n"
            + " * Alt - While the ALT key is pressed, the circles that indicate points you have "
            + "created are hidden\n"
            + " * H - Hide points, showing only the lines of the shape, or un-hide them if H was "
            + "previously pressed\n", keywords = {"path", "cubic", "quadratic", "shape", "control",
            "bezier", "lines"}))
    @Override
    protected void reset() {
        model = new PathModel();
        scratchRect.clear();
        pointsHidden = false;
        repaint();
        model.onChange((x, y) -> {
            model.collectBounds(scratchRect);
            repaint(scratchRect);
        });
    }

    @Help(id="InitialStateTip", noIndex = true, content = {@HelpText(value="# Path Tool\n\n"
            + "Click to create the first point of the shape.  For subsequent points, "
            + "holding down *CTRL* will create a _quadratic_ curve, and *SHIFT* will "
            + "create a _cubic_curve.")})
    @Override
    protected void onAttachRepainter(PaintParticipant.Repainter rep) {
        rep.requestRepaint();
    }

    @Override
    protected HelpItem helpTip() {
        return HelpItems.InitialStateTip;
    }


    @Override
    protected Responder firstResponder() {
        return new InitialPointResponder();
    }

    private final EnhRectangle2D scratchRect = new EnhRectangle2D();
    private final Circle cir = new Circle();
    private final EqLine scratchLine = new EqLine();
    private boolean pointsHidden;
    private boolean previewMode;

    @Override
    protected Rectangle paintCommit(Graphics2D g) {
        Shape shape = model.toShape();
        if (shape != null && !shape.getBounds().isEmpty()) {
            GraphicsUtils.setHighQualityRenderingHints(g);
            Rectangle result = reallyPaintShape(g, shape);
            model.clear();
            return result;
        }
        return new Rectangle();
    }

    private Rectangle reallyPaintShape(Graphics2D g, Shape shape) {
        scratchRect.clear();
        PaintingStyle ps = ResponderTool.fillC.get();
        if (ps.isFill()) {
            g.setPaint(ResponderTool.paintC.get().getPaint());
            g.fill(shape);
        }
        if (ps.isOutline()) {
            BasicStroke stroke = ResponderTool.strokeC.get();
            g.setStroke(stroke);
            g.setPaint(ResponderTool.outlineC.get().getPaint());
            g.draw(shape);
            scratchRect.add(shape, stroke);
        } else {
            scratchRect.add(shape);
        }
        Rectangle result = scratchRect.getBounds();
        return result;
    }

    @Override
    protected Rectangle paintLive(Graphics2D g, Rectangle layerBounds) {
        Shape shape = model.toShape();
        if (shape == null) {
            return new Rectangle();
        }
        GraphicsUtils.setHighQualityRenderingHints(g);
        if (previewMode) {
            Composite oldComposite = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(SRC_OVER, 0.625F));
            reallyPaintShape(g, shape);
            g.setComposite(oldComposite);
        }
        PathUIProperties props = get();
        BasicStroke stroke = props.lineStroke();
        g.setStroke(stroke);
        if (props.hasLineShadows()) {
            g.setPaint(props.lineShadow());
            double scaledTranslate = (stroke.getLineWidth() / 2);
            g.translate(scaledTranslate, scaledTranslate);
            g.draw(shape);
            g.translate(-scaledTranslate, -scaledTranslate);
        }
        g.setPaint(props.lineDraw());
        g.draw(shape);
        scratchRect.clear();
        scratchRect.add(shape, stroke);

        cir.setRadius(props.pointRadius());
        boolean first = true;
        BasicStroke connStroke = props.connectorStroke();
        Paint connectorLineDraw = props.connectorLineDraw();
        if (!pointsHidden) {
            for (Pt pt : model) {
                g.setStroke(stroke);
                cir.setCenter(pt);
                g.setPaint(first ? props.initialPointFill()
                        : pt.isDestination() ? props.destinationPointFill() : props.controlPointFill());
                g.fill(cir);
                g.setPaint(pt.isDestination() ? props.destinationPointDraw() : props.controlPointDraw());
                g.draw(cir);
                scratchRect.add(cir, stroke);
                if (pt.isDestination() && pt.owner().kind().isCurve()) {
                    g.setStroke(connStroke);
                    g.setPaint(connectorLineDraw);
                    pt.configureLineToSiblings(scratchLine, () -> {
                        g.draw(scratchLine);
                        scratchRect.add(scratchLine, connStroke);
                    });
                }
                first = false;
            }
        }
        return scratchRect.getBounds();
    }

    static PathElementKind kindForEvent(InputEvent evt) {
        boolean ctrl1 = mac ? evt.isMetaDown() : evt.isControlDown();
        boolean ctrl2 = evt.isShiftDown();
        boolean ctrl3 = mac ? evt.isControlDown() : evt.isAltDown();
        if (ctrl1 && !ctrl2 && !ctrl3) {
            return PathElementKind.QUADRATIC;
        } else if (ctrl2 && !ctrl1 && !ctrl3) {
            return PathElementKind.CUBIC;
        } else if (ctrl2 && ctrl1 && !ctrl3) {
            return PathElementKind.MOVE;
        }
        return PathElementKind.LINE;
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
//            ImageEditorBackground.getDefault().setStyle(CheckerboardBackground.DARK);
            ImageEditorBackground.getDefault().setStyle(CheckerboardBackground.DARK);
            JFrame jf = new JFrame();
            jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            MiniToolCanvas mini = new MiniToolCanvas();//.debugClip();
            mini.attach(new PathTool2(mini.getLookup().lookup(Surface.class)));
//            mini.attachGeneric(new CircleTool(mini.getLookup().lookup(Surface.class)));
            mini.onCommitRequest(() -> {
                System.out.println("Commit!");
            });
            JToolBar tb = new JToolBar();
            JComboBox<?> box = EnumComboBoxModel.newComboBox((CheckerboardBackground) ImageEditorBackground.getDefault().style());
            box.addItemListener(e -> {
                ImageEditorBackground.getDefault().setStyle((EditorBackground) box.getSelectedItem());
            });
            tb.add(box);
            jf.setLayout(new BorderLayout());
            jf.add(tb, BorderLayout.NORTH);
            jf.add(mini, BorderLayout.CENTER);
            jf.pack();
            jf.setVisible(true);
        });
    }

    class InitialPointResponder extends HoverPointResponder implements PaintingResponder {

        private final Circle circ = new Circle();
        private final EnhRectangle2D repaintBounds = new EnhRectangle2D();

        @Override
        protected void resign(Rectangle addTo) {
            setCursor(Cursor.getDefaultCursor());
        }

        @Override
        protected void activate(Rectangle addTo) {
            setCursor(activeCursor());
        }

        protected Cursor activeCursor() {
            return cursors().star();
        }

        private boolean ensureHoverPoint() {
            if (!hasHoverPoint()) {
                Rectangle r = new Rectangle();
                ctx().fetchVisibleBounds(r);
                setHoverPoint(r.getCenterX(), r.getCenterY());
                return true;
            }
            return false;
        }

        @Override
        protected EqPointDouble moveHoverPoint(double dx, double dy) {
            if (!ensureHoverPoint()) {
                repaintPoint(hoverPoint(), get().proposedLineStroke().getLineWidth());
            }

            EqPointDouble result = super.moveHoverPoint(dx, dy);
            repaintPoint(result, get().proposedLineStroke().getLineWidth());
            repaint();
            return result;
        }

        @Override
        protected Responder onKeyPress(KeyEvent e) {
            double offset = offsetForInputEvent(e);
            switch (e.getKeyCode()) {
                case KeyEvent.VK_P:
                    previewMode = !previewMode;
                    repaint();
                    e.consume();
                    break;
                case KeyEvent.VK_DOWN:
                    moveHoverPoint(0, offset);
                    e.consume();
                    break;
                case KeyEvent.VK_UP:
                    moveHoverPoint(0, -offset);
                    e.consume();
                    break;
                case KeyEvent.VK_RIGHT:
                    moveHoverPoint(offset, 0);
                    e.consume();
                    break;
                case KeyEvent.VK_LEFT:
                    moveHoverPoint(-offset, 0);
                    e.consume();
                    break;
                case KeyEvent.VK_EQUALS:
                case KeyEvent.VK_PLUS:
                    ctx().zoom().zoomIn();
                    e.consume();
                    break;
                case KeyEvent.VK_MINUS:
                    ctx().zoom().zoomOut();
                    e.consume();
                    break;
                case KeyEvent.VK_SPACE:
                    EqPointDouble pt = hoverPoint();
                    if (pt != null) {
                        model.add(PathElementKind.MOVE, pt.x, pt.y);
                        return new ConnectingPointResponder().setHoverPoint(pt);
                    }
            }
            return this;
        }

        @Override
        protected Responder onRelease(double x, double y, MouseEvent e) {
            PathModel.Entry entry = model.add(PathElementKind.MOVE, x, y);
            return new ConnectingPointResponder();
        }

        @Override
        protected Responder onMove(double x, double y, MouseEvent e) {
            repaint(repaintBounds);
            repaintPoint(hoverPoint(), get().proposedLineStroke().getLineWidth());
            repaintPoint(new EqPointDouble(x, y), get().proposedLineStroke().getLineWidth());
            repaint();
            return this;
        }

        @Override
        public Rectangle paint(Graphics2D g, Rectangle bounds) {
            boolean painted = withHoverPoint(pt -> {
                GraphicsUtils.setHighQualityRenderingHints(g);
                repaintBounds.clear();
                circ.setCenter(pt);
                PathUIProperties props = get();
                circ.setRadius(props.pointRadius());
                g.setPaint(props.proposedPointFill());
                g.fill(circ);
                BasicStroke stroke = props.proposedLineStroke();
                g.setPaint(props.proposedPointDraw());
                g.setStroke(stroke);
                g.draw(circ);
                repaintBounds.add(circ, stroke);
            });
            if (!painted) {
                Rectangle result = repaintBounds.getBounds();
                repaintBounds.clear();
                return result;
            }
            return repaintBounds.getBounds();
        }
    }

    static final boolean mac = Utilities.isMac();

    double offsetForInputEvent(InputEvent e) {
        double offset = ctx().zoom().inverseScale(1);
        if (e.isShiftDown()) {
            offset *= 2;
        }
        if (mac && e.isMetaDown()) {
            offset *= 5;
        } else if (e.isControlDown()) {
            offset *= 5;
        }
        if (e.isAltDown()) {
            offset *= 10;
        }
        return offset;
    }

    abstract class BaseConnectingPointResponder extends HoverPointResponder implements PaintingResponder {

        final EnhRectangle2D lastRepaintBounds = new EnhRectangle2D();
        final Circle circ = new Circle();
        final EqLine line = new EqLine();

        protected BaseConnectingPointResponder() {
        }

        @Override
        protected void activate(Rectangle addTo) {
            setCursor(activeCursor());
        }

        @Override
        protected void resign(Rectangle addTo) {
            addTo.setFrame(lastRepaintBounds);
        }

        protected <T> T withPointMatchingLocation(double x, double y, Function<Pt, T> f) {
            Pt best = model.hit(x, y, get().hitRadius());
            if (best != null) {
                return f.apply(best);
            }
            return null;
        }

        protected abstract Cursor activeCursor();

        protected void updateCursor(boolean ctrlOrMeta, boolean shift, boolean alt, boolean hasHoverPoint) {

        }

        @Override
        protected void onBeforeHandleInputEvent(InputEvent evt) {
            updateCursor(mac ? evt.isMetaDown() : evt.isShiftDown(), evt.isShiftDown(), evt.isAltDown(), hasHoverPoint());
        }

        @Override
        protected Responder onKeyRelease(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_ALT:
                    pointsHidden = false;
                    repaint();
                    break;
                case KeyEvent.VK_H:
                    pointsHidden = !pointsHidden;
                    repaint();
                    break;
            }
            return this;
        }

        @Override
        protected Responder onKeyPress(KeyEvent e) {
            double offset = offsetForInputEvent(e);
            updateCursor(mac ? e.isMetaDown() : e.isControlDown(), e.isShiftDown(), e.isAltDown(), hasHoverPoint());
            switch (e.getKeyCode()) {
                case KeyEvent.VK_ALT:
                    pointsHidden = true;
                    repaint();
                    break;
                case KeyEvent.VK_BACK_SPACE:
                    if (model.removeLast()) {
                        return new ConnectingPointResponder().setHoverPoint(model.lastPoint());
                    } else {
                        return firstResponder();
                    }
                case KeyEvent.VK_RIGHT:
                    moveHoverPoint(offset, 0);
                    e.consume();
                    break;
                case KeyEvent.VK_LEFT:
                    moveHoverPoint(-offset, 0);
                    e.consume();
                    break;
                case KeyEvent.VK_P:
                    previewMode = !previewMode;
                    e.consume();
                    break;
                case KeyEvent.VK_UP:
                    moveHoverPoint(0, -offset);
                    e.consume();
                    break;
                case KeyEvent.VK_DOWN:
                    moveHoverPoint(0, offset);
                    e.consume();
                    break;
                case KeyEvent.VK_TAB:
                    if (e.isShiftDown()) {
                        return new AdjustOtherPointResponder(model.lastPoint());
                    } else {
                        return new AdjustOtherPointResponder(model.firstPoint());
                    }
                case KeyEvent.VK_EQUALS:
                case KeyEvent.VK_PLUS:
                    ctx().zoom().zoomIn();
                    e.consume();
                    break;
                case KeyEvent.VK_MINUS:
                    ctx().zoom().zoomOut();
                    e.consume();
                    break;
                case KeyEvent.VK_A:
                    if (mac ? e.isMetaDown() : e.isControlDown()) {
                        MutableRectangle2D rect = new MutableRectangle2D(0, 0, 0, 0);
                        model.visitPoints(rect::add);
                        rect.grow(get().hitRadius() * 2);
                        return new SelectionResponder(rect).with();
                    }
                case KeyEvent.VK_S:
                    if (hasHoverPoint()) {
                        repaint();
                        return new SelectionResponder(hoverPoint()).with();
                    }
                case KeyEvent.VK_ENTER:
                case KeyEvent.VK_SPACE:
                    if (hasHoverPoint()) {
                        EqPointDouble hp = hoverPoint();
                        Responder result = commitPoint(hp.x, hp.y, e);
                        if (result == this && e.getKeyCode() == KeyEvent.VK_ENTER) {
                            commit();
                            return firstResponder();
                        }
                        return result;
                    }
                    break;
                case KeyEvent.VK_ESCAPE:
                    reset();
                    return firstResponder();
            }
            return this;
        }

        protected abstract Responder commitPoint(double x, double y, InputEvent evt);

        @Override
        protected EqPointDouble moveHoverPoint(double dx, double dy) {
            EqPointDouble result = hoverPoint();
            if (result != null) {
                repaintPoint(result);
            }
            result = super.moveHoverPoint(dx, dy);
            repaintPoint(result);
            line.setLine(result, model.lastPoint());
            repaint(line, get().proposedLineStroke());
            return result;
        }

        protected void repaintLast() {
            repaint(lastRepaintBounds);
            PathUIProperties props = get();
            repaint(line, props.connectorStroke());
            repaint(circ, props.proposedLineStroke());
        }

        protected void configureCircleAndLine(double x, double y) {
            PathUIProperties props = get();
            circ.setCenterAndRadius(x, y, props.pointRadius());
            if (hasHoverPoint() && !model.entries.isEmpty()) {
                line.setLine(model.lastPoint(), new EqPointDouble(x, y));
//                System.out.println("Line " + line + " in " + getClass().getSimpleName());
            }
        }

        protected boolean prepareToPaint(Graphics2D g, PathUIProperties props) {
            EqPointDouble pt = hoverPoint();
            if (pt == null) {
                lastRepaintBounds.clear();
                return false;
            }
            lastRepaintBounds.clear();
            configureCircleAndLine(pt.x, pt.y);
            return true;
        }

        @Override
        protected void onHasHoverPointChanged(double x, double y, boolean hasHoverPoint) {
            if (!lastRepaintBounds.isEmpty()) {
                repaint(lastRepaintBounds.getBounds());
            } else {
                repaintPoint(x, y);
            }
        }

        protected void configureGraphicsForLine(Graphics2D g, PathUIProperties props) {
            BasicStroke pstroke = props.proposedLineStroke();
            g.setStroke(pstroke);
            g.setPaint(props.proposedLineDraw());
            lastRepaintBounds.add(line, pstroke);
        }

        protected void configureGraphicsForCircleFill(Graphics2D g, PathUIProperties props) {
            g.setPaint(props.proposedPointFill());

        }

        protected void configureGraphicsForCircleDraw(Graphics2D g, PathUIProperties props) {
            BasicStroke lstroke = props.lineStroke();
            g.setStroke(lstroke);
            g.setPaint(props.proposedLineDraw());
            lastRepaintBounds.add(circ, lstroke);
        }

        @Override
        public Rectangle paint(Graphics2D g, Rectangle bounds) {
            PathUIProperties props = get();
            if (prepareToPaint(g, props)) {
                configureGraphicsForLine(g, props);
                g.draw(line);

                if (!pointsHidden) {
                    configureGraphicsForCircleFill(g, props);
                    g.fill(circ);

                    configureGraphicsForCircleDraw(g, props);
                    g.draw(circ);
                }
            }
            return lastRepaintBounds.getBounds();
        }

        class CloseShapeOnReturnToFirstPointResponder extends BaseConnectingPointResponder implements PaintingResponder {

            private final Pt target;
            private final EnhRectangle2D lastBounds = new EnhRectangle2D();

            public CloseShapeOnReturnToFirstPointResponder(Pt target) {
                this.target = target;
            }

            @Override
            protected Cursor activeCursor() {
                return cursors().closeShape();
            }

            @Override
            protected void activate(Rectangle addTo) {
                Shape shape = model.toShape();
                if (shape != null) {
                    addTo.add(shape.getBounds());
                }
                super.activate(addTo);
            }

            @Override
            protected void resign(Rectangle addTo) {
                super.resign(addTo);
                addTo.add(lastBounds.getBounds());
            }

            @Override
            protected Responder onPress(double x, double y, MouseEvent e) {
                return super.onPress(x, y, e);
            }

            @Override
            protected Responder onMove(double x, double y, MouseEvent e) {
                if (target.distance(x, y) > get().hitRadius()) {
                    return BaseConnectingPointResponder.this.onMove(x, y, e);
                }
                return super.onMove(x, y, e);
            }

            boolean dragged;
            @Override
            protected Responder onDrag(double x, double y, MouseEvent e) {
                dragged = true;
                target.setLocation(x, y);
                repaint(lastBounds);
                return this;
            }

            @Override
            protected Responder onRelease(double x, double y, MouseEvent e) {
                if (dragged) {
                    dragged = false;
                    return this;
                }
                return commitPoint(x, y, e);
            }

            @Override
            protected Responder commitPoint(double x, double y, InputEvent evt) {
                PathElementKind kind = kindForEvent(evt);
                switch (kind) {
                    case CLOSE:
                    case MOVE:
                    case LINE:
                        model.add(kind, target.getX(), target.getY());
                        if (model.canClose()) {
                            model.close();
                            commit();
                            return firstResponder();
                        }
                        break;
                    case QUADRATIC:
                    case CUBIC:
                        PathModel.Entry e = model.add(kind, target.getX(), target.getY());
                        Iterator<Pt> iter = e.secondaryPointsIterator();
                        return defer(new ControlPointPositioner(iter, () -> {
                            if (model.close()) {
                                commit();
                                return firstResponder();
                            }
                            return BaseConnectingPointResponder.this;
                        }));
                    default:
                        throw new AssertionError();
                }
                return BaseConnectingPointResponder.this;
            }

            @Override
            public Rectangle paint(Graphics2D g, Rectangle bounds) {
                lastBounds.clear();
                Shape shape = model.toShape();
                if (shape != null) {
                    g.setColor(get().selectionShapeFill());
                    g.fill(shape);
                    lastBounds.add(shape.getBounds());
                }
                g.setColor(get().hoveredDotFill());
                circ.setRadius(get().pointRadius());
                circ.setCenter(target);
                g.fill(circ);
                return lastBounds.getBounds();
            }
        }

        class AdjustOtherPointResponder extends BaseConnectingPointResponder implements PaintingResponder {

            private final Pt pt;
            private Circle cir = new Circle();
            Rectangle painted = new Rectangle();

            public AdjustOtherPointResponder(Pt pt) {
                this.pt = pt;
            }

            @Override
            public Rectangle paint(Graphics2D g, Rectangle bounds) {
                PathUIProperties props = get();
                g.setPaint(props.selectedPointFill());
                cir.setCenter(pt);
                cir.setRadius(get().pointRadius());
                BasicStroke strk = get().connectorStroke();
                g.setStroke(strk);
                g.fill(cir);
                g.setPaint(props.selectedPointDraw());
                g.draw(cir);
                Rectangle result = painted;
                result.x -= strk.getLineWidth() + 1;
                result.y -= cir.radius() + 1;
                result.width += strk.getLineWidth() * 2;
                result.height += strk.getLineWidth() * 2;
                return result;
            }

            @Override
            protected Responder onKeyPress(KeyEvent e) {
                double off = offsetForInputEvent(e);
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_P:
                        previewMode = !previewMode;
                        e.consume();
                        break;
                    case KeyEvent.VK_ESCAPE:
                        return BaseConnectingPointResponder.this.onKeyPress(e);
                    case KeyEvent.VK_UP:
                        repaint(painted);
                        pt.move(0, -off);
                        repaintPoint(pt);
                        break;
                    case KeyEvent.VK_DOWN:
                        repaint(painted);
                        pt.move(0, off);
                        repaintPoint(pt);
                        break;
                    case KeyEvent.VK_LEFT:
                        repaint(painted);
                        pt.move(-off, 0);
                        repaintPoint(pt);
                        break;
                    case KeyEvent.VK_RIGHT:
                        repaint(painted);
                        pt.move(off, 0);
                        repaintPoint(pt);
                        break;
                    case KeyEvent.VK_TAB:
                        if (e.isShiftDown()) {
                            return new AdjustOtherPointResponder(pt.prev());
                        } else {
                            return new AdjustOtherPointResponder(pt.next());
                        }
                }
                return super.onKeyPress(e);
            }

            @Override
            protected Cursor activeCursor() {
                return cursors().arrowsCrossed();
            }

            @Override
            protected Responder commitPoint(double x, double y, InputEvent evt) {
                pt.setLocation(x, y);
                return this;
            }

        }

        class SelectionResponder extends Responder implements PaintingResponder {

            private final MutableRectangle2D rect;

            SelectionResponder(MutableRectangle2D rect) {
                this.rect = rect;
            }

            SelectionResponder(EqPointDouble startPoint, EqPointDouble initialDragPoint) {
                rect = new MutableRectangle2D(startPoint, initialDragPoint);
            }

            SelectionResponder(Point2D point) {
                this(point.getX(), point.getY());
            }

            SelectionResponder(double x, double y) {
                double pr = get().pointRadius() * 2;
                rect = new MutableRectangle2D(x - (pr / 2), y - (pr / 2), pr, pr);
            }

            WithSelectionResponder with() {
                return new WithSelectionResponder(rect);
            }

            BaseConnectingPointResponder owner() {
                return BaseConnectingPointResponder.this;
            }

            @Override
            protected void activate(Rectangle addTo) {
                setCursor(cursors().dottedRect());
            }

            @Override
            protected Responder onDrag(double x, double y, MouseEvent e) {
                EqPointDouble pt = new EqPointDouble(x, y);
                int point = rect.nearestCorner(pt);
                repaint(rect);
                rect.setPoint(pt, point);
                repaint(rect);
                return this;
            }

            @Override
            protected Responder onRelease(double x, double y, MouseEvent e) {
                onDrag(x, y, e);
                double min = get().pointRadius() * 2;
                if (rect.width < min || rect.height < min) {
                    return owner();
                }
                return new WithSelectionResponder(rect);
            }

            private final Circle circle = new Circle();

            @Override
            public Rectangle paint(Graphics2D g, Rectangle bounds) {
                return paint(g, bounds, null);
            }

            public Rectangle paint(Graphics2D g, Rectangle bounds, List<Pt> selected) {
                PathUIProperties props = get();
                Paint haze = props.selectionShapeFill();
                g.setPaint(haze);
                g.fill(rect);
                Paint hazeLine = props.selectionShapeDraw();
                g.setPaint(hazeLine);
                g.draw(rect);
                EnhRectangle2D r = new EnhRectangle2D(rect);
                model.visitPoints((pt) -> {
                    if (r.contains(pt)) {
                        if (selected != null && !selected.contains(pt)) {
                            return;
                        }
                        g.setPaint(props.selectedPointFill());
                        circle.setCenter(pt);
                        circle.setRadius(get().pointRadius());
                        g.fill(circle);
                        g.setStroke(get().connectorStroke());
                        g.setPaint(props.selectedPointDraw());
                        g.draw(circle);
                        r.add(circle, get().connectorStroke());
                    }
                });
                return r.getBounds();
            }

            class WithSelectionResponder extends Responder implements PaintingResponder {

                private final MutableRectangle2D rect;
                private Pt armed;
                private boolean dragRectangleArmed;

                public WithSelectionResponder(MutableRectangle2D rect) {
                    this.rect = rect;
                }

                WithSelectionResponder(Point2D point) {
                    this(point.getX(), point.getY());
                }

                WithSelectionResponder(double x, double y) {
                    double pr = get().pointRadius() + (2 * ctx().zoom().inverseScale(2));
                    rect = new MutableRectangle2D(x - (pr / 2), y - (pr / 2), pr, pr);
                }

                private boolean moveSelection(double dx, double dy) {
                    List<Pt> selected = selectedPoints();
                    if (!selected.isEmpty()) {
//                        repaint(rect);
                        for (Pt pt : selected) {
                            pt.move(dx, dy);
                        }
                        rect.x += dx;
                        rect.y += dy;
//                        repaint(rect);
                        repaint();
                        return true;
                    }
                    return false;
                }

                private boolean hasArmedPoint() {
                    return armed != null;
                }

                private EqPointDouble armedLocation = new EqPointDouble();

                private void setArmedPoint(Pt armed) {
                    if (hasArmedPoint()) {
                        repaintPoint(armedLocation);
                    }
                    if (armed != null) {
                        this.armed = armed;
                        armedLocation.setLocation(armed);
                        repaintPoint(armedLocation);
                    } else {
                        armed = null;
                    }
                }

                @Override
                protected Responder onKeyPress(KeyEvent e) {
                    double offset = offsetForInputEvent(e);
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_DELETE:
                            Set<PathModel.Entry> toDelete = new HashSet<>();
                            model.visitPoints((point) -> {
                                if (rect.contains(point.getX(), point.getY())) {
                                    toDelete.add(point.owner());
                                }
                            });
                            for (PathModel.Entry se : toDelete) {
                                model.delete(se);
                            }
                            if (model.isEmpty()) {
                                return firstResponder();
                            }
                            if (!toDelete.isEmpty()) {
                                repaint();
                                return owner();
                            }
                            break;
                        case KeyEvent.VK_ESCAPE:
                            rect.clear();
                            e.consume();
                            return owner();
                        case KeyEvent.VK_SHIFT:
                            if (hasArmedPoint()) {
                                setCursor(cursors().rotateMany());
                            }
                            break;
                        case KeyEvent.VK_UP:
                            if (moveSelection(0, -1)) {
                                e.consume();
                            }
                            break;
                        case KeyEvent.VK_DOWN:
                            if (moveSelection(0, 1)) {
                                e.consume();
                            }
                            break;
                        case KeyEvent.VK_LEFT:
                            if (moveSelection(-1, 0)) {
                                e.consume();
                            }
                            break;
                        case KeyEvent.VK_RIGHT:
                            if (moveSelection(1, 0)) {
                                e.consume();
                            }
                            break;
                        case KeyEvent.VK_P:
                            previewMode = !previewMode;
                            e.consume();
                            break;
                        case KeyEvent.VK_H:
                            pointsHidden = !pointsHidden;
                            repaint();
                            e.consume();
                            break;
                        case KeyEvent.VK_C:
                            resetSelectedPoints();
                            selectedPoints();
                            e.consume();
                            repaint();
                            break;
                        case KeyEvent.VK_EQUALS:
                        case KeyEvent.VK_PLUS:
                            ctx().zoom().zoomIn();
                            e.consume();
                            break;
                        case KeyEvent.VK_MINUS:
                            ctx().zoom().zoomOut();
                            e.consume();
                            break;
                        case KeyEvent.VK_ALT:
                            pointsHidden = true;
                            repaint();
                            break;
                        case KeyEvent.VK_L:
                            rect.x -= offset;
                            rect.width += offset;
                            repaint(rect);
                            all = null;
                            e.consume();
                            break;
                        case KeyEvent.VK_K:
                            if (rect.width - offset > 0) {
                                repaint(rect);
                                all = null;
                                rect.x += offset;
                                rect.width -= offset;
                                e.consume();
                            }
                            break;
                        case KeyEvent.VK_R:
                            if (mac ? e.isMetaDown() : e.isControlDown()) {
                                if (rotateSelectedPoints(1)) {
                                    e.consume();
                                }
                            } else {
                                if (rect.width + offset > 0) {
                                    rect.width += offset;
                                    repaint(rect);
                                    all = null;
                                    e.consume();
                                }
                            }
                            break;
                        case KeyEvent.VK_E:
                            if (rect.width - offset > 0) {
                                repaint(rect);
                                rect.width -= offset;
                                all = null;
                                e.consume();
                            }
                            break;
                        case KeyEvent.VK_U:
                            if (rect.height + offset > 0) {
                                rect.y -= offset;
                                rect.height += offset;
                                repaint(rect);
                                all = null;
                                e.consume();
                            }
                            break;
                        case KeyEvent.VK_Y:
                            if (rect.height - offset > 0) {
                                repaint(rect);
                                rect.y += offset;
                                rect.height -= offset;
                                all = null;
                                e.consume();
                            }
                            break;
                        case KeyEvent.VK_D:
                            rect.height += offset;
                            repaint(rect);
                            e.consume();
                            all = null;
                            break;
                        case KeyEvent.VK_A:
                            if (mac ? e.isMetaDown() : e.isControlDown()) {
                                rect.clear();
                                all = new ArrayList<>();
                                model.visitPoints(pt -> {
                                    rect.add(pt);
                                    all.add(pt);
                                });
                                rect.grow(get().pointRadius() * 2);
                                repaint();
                                break;
                            } else {
                                if (moveSelectedPointsApart(offset)) {
                                    e.consume();
                                }
                            }
                            break;
                        case KeyEvent.VK_T:
                            if (moveSelectedPointsApart(-offset)) {
                                e.consume();
                            }
                            break;

                    }
                    return this;
                }

                private List<Pt> selectedPoints() {
                    if (all != null) {
                        return all;
                    }
                    all = new ArrayList<>(10);
                    model.visitPoints((point) -> {
                        if (rect.contains(point)) {
                            all.add(point);
                        }
                    });
                    return all;
                }

                void resetSelectedPoints() {
                    all = null;
                }

                private boolean moveSelectedPointsApart(double offset) {
                    List<Pt> points = selectedPoints();
                    if (points.size() > 1) {
                        EnhRectangle2D hits = new EnhRectangle2D();
                        double[] pts = new double[points.size() * 2];
                        for (int i = 0; i < points.size(); i++) {
                            int off = i * 2;
                            Point2D pt = points.get(i);
                            pts[off] = pt.getX();
                            pts[off + 1] = pt.getY();
                            hits.add(pts[off], pts[off + 1]);
                        }
                        double cx = hits.getCenterX();
                        double cy = hits.getCenterY();

                        for (Pt pt : points) {
                            double x = pt.getX();
                            double y = pt.getY();
                            double ang;
                            if (cx == x && cy == y) {
                                ang = ThreadLocalRandom.current().nextDouble() * 180;
                            } else {
                                ang = Circle.angleOf(cx, cy, x, y);
                            }
                            Circle.positionOf(ang, cx, cy, Point2D.distance(cx, cy, x, y) + offset, pt::setLocation);
                        }
                        hits.grow(get().hitRadius() * 2);
                        rect.setFrame(hits);
                        repaint();
                        return true;
                    }

                    return false;
                }

                @Override
                protected Responder onKeyRelease(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_ALT:
                            pointsHidden = false;
                            repaint();
                            break;
                        case KeyEvent.VK_SHIFT:
                            if (hasArmedPoint()) {
                                setCursor(cursors().multiMove());
                            }
                    }
                    return this;
                }

                @Override
                protected Responder onMove(double x, double y, MouseEvent e) {
                    if (!rect.contains(x, y)) {
                        setArmedPoint(null);
                        setCursor(cursors().x());
                        return this;
                    }
                    double hitDistance = get().pointRadius();
                    Bool cursorSet = Bool.create();
                    circle.setRadius(hitDistance);
                    dragRectangleArmed = false;
                    Cursors cursors = cursors();
                    model.visitPoints((point) -> {
                        circle.setCenter(point);
                        if (rect.contains(point.getX(), point.getY())) {
                            if (circle.contains(x, y)) {
                                if (e.isShiftDown()) {
                                    setCursor(cursors.rotateMany());
                                } else {
                                    setCursor(cursors.multiMove());
                                }
                                cursorSet.set();
                                setArmedPoint(point);
                                repaint(circle, get().lineStroke());
                            }
                        }
                    });
                    cursorSet.ifUntrue(() -> {
                        setArmedPoint(null);
                    });

                    ResizeMode rm = ResizeMode.forRect(x, y, hitDistance, rect);
                    if (rm != null) {
                        setCursor(CursorSupport.cursor(rm));
                        cursorSet.set();
                        dragRectangleArmed = true;
                    }
                    cursorSet.ifUntrue(() -> {
                        setCursor(activeCursor());
                    });
                    return super.onMove(x, y, e); //To change body of generated methods, choose Tools | Templates.
                }

                EqPointDouble dragBase = new EqPointDouble();
                private boolean hasDragBase;

                private void setDragBase(double x, double y) {
                    if (hasDragBase) {
                        repaintPoint(dragBase);
                    }
                    dragBase.setLocation(x, y);
                    repaintPoint(dragBase);
                    hasDragBase = true;
                }

                @Override
                protected Responder onPress(double x, double y, MouseEvent e) {
                    setDragBase(x, y);
                    onMove(x, y, e);
                    if (dragRectangleArmed) {
                        double hitDistance = get().pointRadius();
                        ResizeMode md = ResizeMode.forRect(x, y, hitDistance, rect);
                        if (md != null) {
                            return new ResizeSelectionResponder(md);
                        }
                    }
                    return this;
                }

                WithSelectionResponder clearDragBase() {
                    if (hasDragBase) {
                        hasDragBase = false;
                        repaintPoint(dragBase);
                        dragBase.setLocation(0, 0);
                    }
                    return this;
                }

                @Override
                protected void resign(Rectangle addTo) {
                    clearDragBase();
                    setCursor(Cursor.getDefaultCursor());
                }

                @Override
                protected void activate(Rectangle addTo) {
                    setCursor(cursors().x());
                }

                @Override
                protected Responder onRelease(double x, double y, MouseEvent e) {
                    if (!hasArmedPoint()) {
                        return defer(BaseConnectingPointResponder.this);
                    }
                    setArmedPoint(null);
                    onMove(x, y, e);
                    return this;
                }

                EqPointDouble rotateCenter;
                List<Pt> all;

                @Override
                protected Responder onDrag(double x, double y, MouseEvent e) {
                    if (hasArmedPoint()) {
                        if (e.isShiftDown()) {
                            double dist = dragBase.distance(x, y);
                            if (dragBase.getX() < x && dragBase.getY() < y) {
                                dist = -dist;
                            }
                            setDragBase(x, y);
                            rotateSelectedPoints(dist);
                        } else {
                            double dx = x - armed.getX();
                            double dy = y - armed.getY();
                            if (all == null) {
                                armed.setLocation(x, y);
                                armedLocation.setLocation(x, y);
                                setHoverPoint(armed);
                                owner().setHoverPoint(x, y);
                                dragBase.setLocation(x, y);
                                all = new ArrayList<>();
                                all.add(armed);
                                model.visitPoints((point) -> {
                                    if (rect.contains(point)) {
                                        if (point.getX() != armed.getX() || point.getY() != armed.getY()) {
                                            point.setLocation(point.getX() + dx, point.getY() + dy);
                                            all.add(point);
                                        }
                                    }
                                });
                            } else {
                                for (Point2D point : all) {
                                    point.setLocation(point.getX() + dx, point.getY() + dy);
                                }
                            }
                            rect.x += dx;
                            rect.y += dy;
                            repaint();
                        }
                        return this;
                    }
                    rect.setFrame(x, y, 0, 0);
                    repaint();
                    return owner();
                }

                private boolean rotateSelectedPoints(double dist) {
                    List<Pt> points = selectedPoints();
                    if (!points.isEmpty()) {
                        EnhRectangle2D hits = new EnhRectangle2D();
                        double[] pts = new double[points.size() * 2];
                        double rad = get().pointRadius();
                        for (int i = 0; i < points.size(); i++) {
                            int offset = i * 2;
                            Point2D pt = points.get(i);
                            pts[offset] = pt.getX();
                            pts[offset + 1] = pt.getY();
                            if (hits.isEmpty()) {
                                hits.setFrame(pts[offset] - (rad / 2), pts[offset + 1] - (rad / 2), rad, rad);
                            } else {
                                hits.add(pts[offset], pts[offset + 1]);
                            }
                        }
                        dist = ctx().zoom().scale(dist);
                        if (rotateCenter == null) {
                            rotateCenter = hits.center();
                        }
//                                PooledTransform.withRotateInstance(Math.toRadians(dist * 360), rotateCenter.x, rotateCenter.y, xf -> {
                        PooledTransform.withRotateInstance(dist * 0.05, rotateCenter.x, rotateCenter.y, xf -> {
                            xf.transform(pts, 0, pts, 0, points.size());
                        });
                        hits.width = 0;
                        hits.height = 0;
                        for (int i = 0; i < points.size(); i++) {
                            int offset = i * 2;
                            Point2D pt = points.get(i);
                            pt.setLocation(pts[offset], pts[offset + 1]);
                            if (hits.isEmpty()) {
                                hits.setFrame(pt.getX() - (rad / 2), pt.getY() - (rad / 2), rad, rad);
                            } else {
                                hits.add(pts[offset], pts[offset + 1]);
                                hits.add(pt.getX() - rad, pt.getY() - rad);
                                hits.add(pt.getX() + rad, pt.getY() + rad);
                            }
                        }
                        rect.setFrame(hits);
                        rect.grow(get().pointRadius());
                        repaint();
                        return true;
                    }
                    return false;
                }

                @Override
                protected Responder onClick(double x, double y, MouseEvent e) {
                    // avoid the click event being processed by the owner,
                    // which will create a new point if the user just clicked to
                    // dismiss the selection
                    return defer(owner().clearHoverPoint());
//                    return owner().onClick(x, y, e);
                }

                Rectangle paintRect(Graphics2D g, Rectangle bounds) {
                    return SelectionResponder.this.paint(g, bounds, all);
                }

                @Override
                public Rectangle paint(Graphics2D g, Rectangle bounds) {
                    Rectangle result = paintRect(g, bounds);
                    if (hasArmedPoint()) {
                        circle.setCenter(armedLocation);
                        g.setColor(get().hoveredDotFill());
                        g.fill(circle);
                        g.setColor(get().hoveredDotDraw());
                        g.draw(circle);
                    }
                    return result;
                }

                class ResizeSelectionResponder extends Responder implements PaintingResponder {

                    private ResizeMode mode;

                    ResizeSelectionResponder(ResizeMode mode) {
                        this.mode = mode;
                    }

                    ResizeSelectionResponder(double x, double y) {
                        mode = ResizeMode.forRect(x, y, get().pointRadius(), rect);
                    }

                    @Override
                    protected void resign(Rectangle addTo) {
                        setCursor(CursorSupport.cursor(mode));
                    }

                    @Override
                    protected void activate(Rectangle addTo) {
                        setCursor(Cursor.getDefaultCursor());
                    }

                    @Override
                    protected Responder onDrag(double x, double y, MouseEvent e) {
                        BasicStroke strk = get().lineStroke();
                        repaint(rect, strk);
                        ResizeMode newMode = mode.apply(x, y, rect);
                        if (newMode != mode) {
                            setCursor(CursorSupport.cursor(newMode));
                        }
                        mode = newMode;
                        repaint(rect, strk);
                        resetSelectedPoints();
                        return this;
                    }

                    @Override
                    protected Responder onRelease(double x, double y, MouseEvent e) {
                        return WithSelectionResponder.this.clearDragBase().onMove(x, y, e);
                    }

                    @Override
                    public Rectangle paint(Graphics2D g, Rectangle bounds) {
                        return WithSelectionResponder.this.paintRect(g, bounds);
                    }
                }
            }
        }
    }

    class ConnectingPointResponder extends BaseConnectingPointResponder {

        @Override
        protected Responder onMove(double x, double y, MouseEvent e) {
            repaintLast();
            configureCircleAndLine(x, y);
            repaintPoint(hoverPoint());
            repaintPoint(x, y);
            repaint(line, get().connectorStroke());
            Pt pt = model.hit(x, y, get().hitRadius());
            if (pt != null) {
                if (model.canClose() && pt.isFirstPointInModel()) {
                    return new CloseShapeOnReturnToFirstPointResponder(pt);
                }
                return new ArmedForMoveExistingPointResponder(pt);
            }
            return this;
        }

        @Override
        protected Responder commitPoint(double x, double y, InputEvent evt) {
            repaintLast();
            repaintPoint(x, y);
            repaintPoint(hoverPoint());
            PathElementKind kind = kindForEvent(evt);
            Iterator<Pt> iter = model.add(kind, x, y).secondaryPointsIterator();
            if (iter.hasNext()) {
                return new ControlPointPositioner(iter);
            } else {
                if (kind.isCurve()) {
                    throw new IllegalStateException("Should have gotten a control point iterator for " + kind);
                }
            }
            return this;
        }

        @Override
        protected Responder onDrag(double x, double y, MouseEvent e) {
            if (hasHoverPoint()) {
                EqPointDouble pt = hoverPoint().copy();
                return new SelectionResponder(pt, new EqPointDouble(x, y));
            }
            onMove(x, y, e);
            return this;
        }

        @Override
        protected Responder onClick(double x, double y, MouseEvent evt) {
            int ct = evt.getClickCount();
            if (!evt.isPopupTrigger() && (ct == 1 || ct == 2)) {
                PathElementKind k = kindForEvent(evt);
                Iterator<Pt> iter = model.add(k, x, y).secondaryPointsIterator();
                if (iter.hasNext()) {
                    return new ControlPointPositioner(iter);
                }
                if (ct == 2) {
                    commit();
                    return firstResponder();
                }
            }
            return this;
        }

        @Override
        protected Cursor activeCursor() {
            return cursors().arrowPlus();
        }

        class ArmedForMoveExistingPointResponder extends BaseConnectingPointResponder {

            private final Pt pt;
            private boolean dragged;
            private final EnhRectangle2D lastBounds = new EnhRectangle2D();

            public ArmedForMoveExistingPointResponder(Pt pt) {
                this.pt = pt;
            }

            @Override
            protected Cursor activeCursor() {
                return cursors().arrowsCrossed();
            }

            @Override
            protected void resign(Rectangle addTo) {
                addTo.setFrame(lastBounds);
            }

            @Override
            protected void configureCircleAndLine(double x, double y) {
                super.configureCircleAndLine(x, y);
                circ.setCenter(pt);
                line.x1 = line.x2 = line.y1 = line.y2 = 0;
            }

            protected void configureGraphicsForCircleDraw(Graphics2D g, PathUIProperties props) {
                super.configureGraphicsForCircleDraw(g, props);
                g.setColor(props.hoveredDotDraw());
            }

            @Override
            protected void configureGraphicsForLine(Graphics2D g, PathUIProperties props) {
                super.configureGraphicsForLine(g, props);
                g.setColor(new Color(0, 0, 0, 0));
            }

            @Override
            protected void configureGraphicsForCircleFill(Graphics2D g, PathUIProperties props) {
                super.configureGraphicsForCircleDraw(g, props);
                g.setColor(props.hoveredDotFill());
            }

            @Override
            public Rectangle paint(Graphics2D g, Rectangle bounds) {
                Rectangle r = super.paint(g, bounds);
                double rad = circ.radius();
                rad *= 1.5;
                circ.setRadius(rad);
                configureGraphicsForCircleDraw(g, get());
                g.draw(circ);
                circ.setRadius(rad * 1.25);
                r.add(circ.getBounds());
                lastBounds.setFrame(r);
                return r;
            }

            @Override
            protected Responder onKeyPress(KeyEvent e) {
                Responder result = ConnectingPointResponder.this.onKeyPress(e);
                if (result == ConnectingPointResponder.this) {
                    return result;
                }
                return super.onKeyPress(e);
            }

            @Override
            protected Responder onKeyRelease(KeyEvent e) {
                Responder result = ConnectingPointResponder.this.onKeyRelease(e);
                if (result == ConnectingPointResponder.this) {
                    return result;
                }
                return super.onKeyRelease(e);
            }

            @Override
            protected Responder onMove(double x, double y, MouseEvent e) {
                if (pt.distance(x, y) > get().hitRadius()) {
                    return ConnectingPointResponder.this.onMove(x, y, e);
                }
                return this;
            }

            @Override
            protected Responder onPress(double x, double y, MouseEvent e) {
                if (pt.distance(x, y) > get().hitRadius()) {
                    return ConnectingPointResponder.this.onPress(x, y, e);
                }
                return this;
            }

            @Override
            protected Responder onDrag(double x, double y, MouseEvent e) {
                dragged = true;
                repaint(lastBounds);
                lastBounds.clear();
                if (hasHoverPoint()) {
                    repaintPoint(hoverPoint());
                }
                pt.setLocation(x, y);
                pt.addSiblingsToBounds(lastBounds);
                repaint(lastBounds);
                return this;
            }

            @Override
            protected Responder onRelease(double x, double y, MouseEvent e) {
                if (dragged) {
                    return commitPoint(x, y, e);
                }
                return ConnectingPointResponder.this.onPress(x, y, e);
            }

            @Override
            protected Responder commitPoint(double x, double y, InputEvent evt) {
                repaint(lastBounds);
                lastBounds.clear();
                repaintPoint(hoverPoint());
                pt.setLocation(x, y);
                if (hasHoverPoint()) {
                    lastBounds.add(hoverPoint());
                }
                pt.addSiblingsToBounds(lastBounds);
                repaint(lastBounds);
                return ConnectingPointResponder.this;
            }
        }
    }

    class ControlPointPositioner extends BaseConnectingPointResponder {

        private final Pt currentPoint;
        private final Iterator<Pt> iter;
        private final EnhRectangle2D repaintBounds = new EnhRectangle2D();
        private final Supplier<Responder> nextSupplier;

        private ControlPointPositioner(Iterator<Pt> iter) {
            this(iter, ConnectingPointResponder::new);
        }

        private ControlPointPositioner(Iterator<Pt> iter, Supplier<Responder> nextSupplier) {
            this.iter = iter;
            assert iter.hasNext();
            currentPoint = iter.next();
            this.nextSupplier = nextSupplier == null ? ConnectingPointResponder::new : nextSupplier;
        }

        protected void configureGraphicsForLine(Graphics2D g, PathUIProperties props) {
            BasicStroke pstroke = props.connectorStroke();
            g.setStroke(pstroke);
            g.setPaint(props.connectorLineDraw());
            lastRepaintBounds.add(line, pstroke);
        }

        @Override
        protected void repaintLast() {
            currentPoint.addSiblingsToBounds(repaintBounds);
            super.repaintLast();
            repaint(repaintBounds);
            repaintBounds.clear();
        }

        @Override
        protected EqPointDouble moveHoverPoint(double dx, double dy) {
            EqPointDouble result = super.moveHoverPoint(dx, dy);
            if (result != null) {
                currentPoint.setLocation(result);
                repaint(repaintBounds);
                repaintBounds.clear();
                currentPoint.owner().addToBounds(repaintBounds);
                repaint(repaintBounds);
            }
            return result;
        }

        @Override
        protected Responder onMove(double x, double y, MouseEvent e) {
            repaintLast();
            currentPoint.setLocation(x, y);
            repaintPoint(x, y);
            return this;
        }

        @Override
        protected Responder onDrag(double x, double y, MouseEvent e) {
            if (hasHoverPoint()) {
                EqPointDouble pt = hoverPoint().copy();
                return new SelectionResponder(pt, new EqPointDouble(x, y));
            }
            onMove(x, y, e);
            return this;
        }

        @Override
        protected Responder onClick(double x, double y, MouseEvent e) {
            int ct = e.getClickCount();
            repaintLast();
            if (!e.isPopupTrigger() && (ct == 1 || (ct == 2 && !iter.hasNext()))) {
                currentPoint.setLocation(x, y);
                repaintPoint(x, y);
                repaintPoint(hoverPoint());
                if (iter.hasNext()) {
                    // Don't return this, so the logic for VK_ENTER is correct
                    return new ControlPointPositioner(iter, nextSupplier);
                }
                if (ct == 2) {
                    commit();
                    return firstResponder();
                }
            }
            return nextSupplier.get();
        }

        @Override
        protected Responder commitPoint(double x, double y, InputEvent evt) {
            repaintLast();
            currentPoint.setLocation(x, y);
            repaintPoint(x, y);
            repaintPoint(hoverPoint());
            if (iter.hasNext()) {
                // Don't return this, so the logic for VK_ENTER is correct
                return new ControlPointPositioner(iter);
            }
            return nextSupplier.get();
        }

        @Override
        protected Cursor activeCursor() {
            return cursors().arrowTilde();
        }
    }
}
