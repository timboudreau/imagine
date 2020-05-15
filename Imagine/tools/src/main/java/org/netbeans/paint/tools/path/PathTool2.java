package org.netbeans.paint.tools.path;

import com.mastfrog.function.DoubleBiConsumer;
import com.mastfrog.function.state.Bool;
import com.mastfrog.function.state.Dbl;
import com.mastfrog.function.state.Obj;
import com.mastfrog.util.collections.CollectionUtils;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import net.java.dev.imagine.api.image.Surface;
import org.imagine.editor.api.CheckerboardBackground;
import org.imagine.editor.api.ImageEditorBackground;
import org.imagine.editor.api.PaintingStyle;
import org.imagine.geometry.Circle;
import org.imagine.geometry.EnhRectangle2D;
import org.imagine.geometry.EqLine;
import org.imagine.geometry.EqPointDouble;
import org.imagine.geometry.path.PathElement;
import org.imagine.geometry.path.PathElementKind;
import org.imagine.geometry.path.PointKind;
import org.imagine.geometry.util.PooledTransform;
import org.imagine.utils.java2d.GraphicsUtils;
import org.netbeans.paint.api.components.Cursors;
import org.netbeans.paint.tools.MutableRectangle2D;
import org.netbeans.paint.tools.minidesigner.MiniToolCanvas;
import org.netbeans.paint.tools.minidesigner.ResizeMode;
import org.netbeans.paint.tools.responder.HoverPointResponder;
import org.netbeans.paint.tools.responder.PaintingResponder;
import org.netbeans.paint.tools.responder.PathUIProperties;
import org.netbeans.paint.tools.responder.Responder;
import org.netbeans.paint.tools.responder.ResponderTool;
import org.openide.util.Utilities;

/**
 *
 * @author Tim Boudreau
 */
@ToolDef(name = "Path", iconPath = "org/netbeans/paint/tools/resources/path.svg",
        category = "vector")
@Tool(value = Surface.class, toolbarPosition = 2000)
public class PathTool2 extends ResponderTool {

    private Model model = new Model();

    public PathTool2(Surface obj) {
        super(obj);
    }

    @Override
    protected void reset() {
        model = new Model();
        scratchRect.clear();
        pointsHidden = false;
        repaint();
        model.onChange((x, y) -> {
            model.collectBounds(scratchRect);
            repaint(scratchRect);
        });
    }

    @Override
    protected void onAttachRepainter(PaintParticipant.Repainter rep) {
        rep.requestRepaint();
    }

    @Override
    protected Responder firstResponder() {
        return new InitialPointResponder();
    }

    private final EnhRectangle2D scratchRect = new EnhRectangle2D();
    private final Circle cir = new Circle();
    private final EqLine scratchLine = new EqLine();
    private boolean pointsHidden;

    @Override
    protected Rectangle paintCommit(Graphics2D g) {
        Shape shape = model.toShape();
        if (shape != null) {
            scratchRect.clear();
            PaintingStyle ps = ResponderTool.fillC.get();
            GraphicsUtils.setHighQualityRenderingHints(g);
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
            reset();
            return result;
        }
        return new Rectangle();
    }

    @Override
    protected Rectangle paintLive(Graphics2D g, Rectangle layerBounds) {
        Shape shape = model.toShape();
        if (shape == null) {
            return new Rectangle();
        }
        GraphicsUtils.setHighQualityRenderingHints(g);
        PathUIProperties props = get();
        BasicStroke stroke = props.lineStroke();
        g.setStroke(stroke);
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
        boolean mac = Utilities.isMac();
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
            jf.setContentPane(mini);
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
            Model.Entry entry = model.add(PathElementKind.MOVE, x, y);
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
                g.setStroke(stroke);
                g.setPaint(props.proposedLineDraw());
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
            System.out.println("Create a " + getClass().getSimpleName());
        }

        @Override
        protected void activate(Rectangle addTo) {
            setCursor(activeCursor());
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

        class AdjustOtherPointResponder extends BaseConnectingPointResponder implements PaintingResponder {

            private final Pt pt;
            private Circle cir = new Circle();
            Rectangle painted = new Rectangle();

            public AdjustOtherPointResponder(Pt pt) {
                this.pt = pt;
            }

            @Override
            public Rectangle paint(Graphics2D g, Rectangle bounds) {
                g.setColor(Color.BLUE);
                cir.setCenter(pt);
                cir.setRadius(get().pointRadius());
                BasicStroke strk = get().connectorStroke();
                g.setStroke(strk);
                g.fill(cir);
                g.setColor(new Color(128, 128, 255));
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
                return Cursor.getDefaultCursor();
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
                return new WithSelectionResponder(rect);
            }

            private final Circle circle = new Circle();

            @Override
            public Rectangle paint(Graphics2D g, Rectangle bounds) {
                return paint(g, bounds, null);
            }

            public Rectangle paint(Graphics2D g, Rectangle bounds, List<Pt> selected) {
                Color base = ImageEditorBackground.getDefault().style().contrasting();
                Color haze = new Color(base.getRed(), base.getGreen(), base.getBlue(), 90);
                g.setColor(haze);
                g.fill(rect);
                Color hazeLine = new Color(base.getRed(), base.getGreen(), base.getBlue(), 170);
                g.setColor(hazeLine);
                g.draw(rect);
                EnhRectangle2D r = new EnhRectangle2D(rect);
                model.visitPoints((pt) -> {
                    if (r.contains(pt)) {
                        if (selected != null && !selected.contains(pt)) {
                            return;
                        }
                        g.setColor(Color.BLUE);
                        circle.setCenter(pt);
                        circle.setRadius(get().pointRadius());
                        g.fill(circle);
                        g.setStroke(get().connectorStroke());
                        g.setColor(new Color(128, 128, 255));
                        g.draw(circle);
                        r.add(circle, get().connectorStroke());
                    }
                });
                return r.getBounds();
            }

            class WithSelectionResponder extends Responder implements PaintingResponder {

                private final MutableRectangle2D rect;
                private Pt armedPoint;
                private boolean dragRectangleArmed;

                public WithSelectionResponder(MutableRectangle2D rect) {
                    this.rect = rect;
                    System.out.println("Create a WithSelectionResponder");
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

                @Override
                protected Responder onKeyPress(KeyEvent e) {
                    double offset = offsetForInputEvent(e);
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_DELETE:
                            Set<Model.Entry> toDelete = new HashSet<>();
                            model.visitPoints((point) -> {
                                if (rect.contains(point.getX(), point.getY())) {
                                    toDelete.add(point.owner());
                                }
                            });
                            for (Model.Entry se : toDelete) {
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
                            if (armedPoint != null) {
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
                                rotateSelectedPoints(1);
                            } else {
                                rect.width += offset;
                                repaint(rect);
                                all = null;
                                e.consume();
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
                            rect.y -= offset;
                            rect.height += offset;
                            repaint(rect);
                            all = null;
                            e.consume();
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
                            if (armedPoint != null) {
                                setCursor(cursors().multiMove());
                            }
                    }
                    return this;
                }

                @Override
                protected Responder onMove(double x, double y, MouseEvent e) {
                    if (!rect.contains(x, y)) {
                        armedPoint = null;
                        setCursor(Cursor.getDefaultCursor());
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
                                armedPoint = point;
                                repaint(circle, get().lineStroke());
                            }
                        }
                    });
                    cursorSet.ifUntrue(() -> {
                        armedPoint = null;
                    });

                    ResizeMode rm = ResizeMode.forRect(x, y, hitDistance, rect);
                    if (rm != null) {
                        setCursor(rm.cursor());
                        cursorSet.set();
                        dragRectangleArmed = true;
                    }
                    cursorSet.ifUntrue(() -> {
                        setCursor(Cursor.getDefaultCursor());
                    });
                    return super.onMove(x, y, e); //To change body of generated methods, choose Tools | Templates.
                }

                EqPointDouble dragBase = new EqPointDouble();

                @Override
                protected Responder onPress(double x, double y, MouseEvent e) {
                    dragBase.setLocation(x, y);
                    onMove(x, y, e);
                    if (dragRectangleArmed) {
                        double hitDistance = get().pointRadius();
                        ResizeMode md = ResizeMode.forRect(x, y, hitDistance, rect);
                        if (md != null) {
                            return new ResizeSelectionResponder(md);
                        }
                    }
                    if (armedPoint == null) {
                        return owner().onPress(x, y, e);
                    }
                    return this;
                }

                @Override
                protected void resign(Rectangle addTo) {
                    setCursor(Cursor.getDefaultCursor());
                }

                @Override
                protected Responder onRelease(double x, double y, MouseEvent e) {
                    onMove(x, y, e);
                    return super.onRelease(x, y, e); //To change body of generated methods, choose Tools | Templates.
                }

                EqPointDouble rotateCenter;
                List<Pt> all;

                @Override
                protected Responder onDrag(double x, double y, MouseEvent e) {
                    if (armedPoint != null) {
                        if (e.isShiftDown()) {
                            double dist = dragBase.distance(x, y);
                            if (dragBase.getX() < x && dragBase.getY() < y) {
                                dist = -dist;
                            }
                            dragBase.setLocation(x, y);
                            rotateSelectedPoints(dist);
                        } else {
                            double dx = x - armedPoint.getX();
                            double dy = y - armedPoint.getY();
                            if (all == null) {
                                armedPoint.setLocation(x, y);
                                all = new ArrayList<>();
                                all.add(armedPoint);

                                model.visitPoints((point) -> {
                                    if (rect.contains(point)) {
                                        if (point.getX() != armedPoint.getX() || point.getY() != armedPoint.getY()) {
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
                    rect.setFrame(x, y, x, y);
                    repaint();
                    // if on a point, move everything
//                    return SelectionResponder.this.onDrag(x, y, e);
                    return this;
                }

                private void rotateSelectedPoints(double dist) {
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
                    }
                }

                @Override
                protected Responder onClick(double x, double y, MouseEvent e) {
                    return owner().onClick(x, y, e);
                }

                @Override
                public Rectangle paint(Graphics2D g, Rectangle bounds) {
                    Rectangle result = SelectionResponder.this.paint(g, bounds, all);
                    if (armedPoint != null) {
                        circle.setCenter(armedPoint);
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
                        System.out.println("Create a ResizeSelectionResponder");
                    }

                    ResizeSelectionResponder(double x, double y) {
                        mode = ResizeMode.forRect(x, y, get().pointRadius(), rect);
                        System.out.println("Create a ResizeSelectionResponder");
                    }

                    @Override
                    protected void resign(Rectangle addTo) {
                        setCursor(mode.cursor());
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
                            setCursor(newMode.cursor());
                        }
                        mode = newMode;
                        repaint(rect, strk);
                        resetSelectedPoints();
                        return this;
                    }

                    @Override
                    protected Responder onRelease(double x, double y, MouseEvent e) {
                        return WithSelectionResponder.this.onMove(x, y, e);
                    }

                    @Override
                    public Rectangle paint(Graphics2D g, Rectangle bounds) {
                        return WithSelectionResponder.this.paint(g, bounds);
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
                return new ArmedForMoveExistingPointResponder(pt);
            }
            return this;
        }

        @Override
        protected Responder commitPoint(double x, double y, InputEvent evt) {
            repaintLast();
            repaintPoint(x, y);
            repaintPoint(hoverPoint());
            Iterator<Pt> iter = model.add(kindForEvent(evt), x, y).secondaryPointsIterator();
            if (iter.hasNext()) {
                return new ControlPointPositioner(iter);
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
        protected Responder onClick(double x, double y, MouseEvent e) {
            int ct = e.getClickCount();
            if (!e.isPopupTrigger() && (ct == 1 || ct == 2)) {
                PathElementKind k = kindForEvent(e);
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
            return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
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

        private ControlPointPositioner(Iterator<Pt> iter) {
            this.iter = iter;
            assert iter.hasNext();
            currentPoint = iter.next();
        }

        protected void configureGraphicsForLine(Graphics2D g, PathUIProperties props) {
            BasicStroke pstroke = props.connectorStroke();
            g.setStroke(pstroke);
            g.setPaint(props.connectorLineDraw());
            lastRepaintBounds.add(line, pstroke);
        }

        protected void repaintLast() {
            currentPoint.addSiblingsToBounds(repaintBounds);
            super.repaintLast();
            repaint(repaintBounds);
            repaintBounds.clear();
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
                    return new ControlPointPositioner(iter);
                }
                if (ct == 2) {
                    commit();
                    return firstResponder();
                }
            }
            return new ConnectingPointResponder();
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
            return new ConnectingPointResponder();
        }

        @Override
        protected Cursor activeCursor() {
            return cursors().x();
        }
    }

    static class Model implements Iterable<Pt> {

        private Shape cachedShape;
        private final List<Entry> entries = new ArrayList<>(32);
        private DoubleBiConsumer onChange;

        Pt hit(double x, double y, double radius) {
            Obj<Pt> result = Obj.create();
            Dbl bestDistance = Dbl.of(Double.MAX_VALUE);
            for (Entry e : entries) {
                for (Pt pt : e.iterable()) {
                    pt.ifHit(x, y, radius, (point, dist) -> {
                        if (bestDistance.min(dist) > dist) {
                            result.set(pt);
                        }
                    });
                }
            }
            return result.get();
        }

        Pt firstPoint() {
            if (entries.isEmpty()) {
                return null;
            }
            return entries.get(0).pointsIterator().next();
        }

        Pt lastPoint() {
            if (entries.isEmpty()) {
                return null;
            }
            return entries.get(entries.size() - 1).destination();
        }

        void collectBounds(EnhRectangle2D bds) {
            bds.clear();
            for (Entry e : entries) {
                e.addToBounds(bds);
            }
        }

        boolean removeLast() {
            if (!entries.isEmpty()) {
                entries.remove(entries.size() - 1);
                return !entries.isEmpty();
            }
            return false;
        }

        void visitPoints(Consumer<Pt> c) {
            entries.forEach(e -> e.iterable().forEach(c));
        }

        @Override
        public Iterator<Pt> iterator() {
            List<Iterator<Pt>> all = new ArrayList<>();
            for (Entry e : entries) {
                all.add(e.pointsIterator());
            }
            return CollectionUtils.combine(all);
        }

        public int size() {
            return entries.size();
        }

        public Shape toShape() {
            if (cachedShape != null) {
                return cachedShape;
            }
            Path2D.Double path = new Path2D.Double();
            for (Entry e : entries) {
                e.applyTo(path);
            }
            return cachedShape = path;
        }

        Model onChange(DoubleBiConsumer onChange) {
            this.onChange = onChange;
            return this;
        }

        public Entry add(PathElementKind kind, double x, double y) {
            if (entries.isEmpty() && kind != PathElementKind.MOVE) {
                return add(PathElementKind.MOVE, x, y);
            }
            double[] arr = new double[kind.arraySize()];
            for (int i = 0; i < arr.length; i += 2) {
                arr[i] = x;
                arr[i + 1] = y;
            }
            Entry result = new Entry(kind, arr);
            entries.add(result);
            onChange(x, y);
            return result;
        }

        void onChange(double x, double y) {
            cachedShape = null;
            if (onChange != null) {
                onChange.accept(x, y);
            }
        }

        private void delete(Entry se) {
            entries.remove(se);
        }

        private boolean isEmpty() {
            return entries.isEmpty();
        }

        class Entry implements PathElement {

            private final PathElementKind kind;
            private final double[] points;

            public Entry(PathElementKind kind, double[] points) {
                switch (kind) {
                    case CLOSE:
                        break;
                    default:
                        if (kind.arraySize() != points.length) {
                            throw new IllegalArgumentException("Wrong size: " + points.length + " for " + kind);
                        }
                }
                this.kind = kind;
                this.points = points;
            }

            void addToBounds(EnhRectangle2D rect) {
                if (kind == PathElementKind.CLOSE) {
                    return;
                }
                for (int i = 0; i < points.length; i += 2) {
                    if (rect.isEmpty()) {
                        rect.x = points[i];
                        rect.y = points[i + 1];
                        rect.width = 1;
                        rect.height = 1;
                    } else {
                        rect.add(points[i], points[i + 1]);
                    }
                }
            }

            Iterator<Pt> secondaryPointsIterator() {
                switch (kind) {
                    case CLOSE:
                    case LINE:
                    case MOVE:
                        return Collections.emptyIterator();
                    case CUBIC:
                        return Arrays.asList(new Pt(1, this), new Pt(0, this)).iterator();
                    case QUADRATIC:
                        return Collections.singleton(new Pt(0, this)).iterator();
                    default:
                        throw new AssertionError(kind);
                }
            }

            Pt destination() {
                return new Pt(kind.pointCount() - 1, this);
            }

            Iterable<Pt> iterable() {
                return () -> {
                    return pointsIterator();
                };
            }

            private Iterator<Pt> pointsIterator() {
                return new Iterator<Pt>() {
                    int ix = -1;

                    @Override
                    public boolean hasNext() {
                        return ix + 1 < kind.pointCount();
                    }

                    @Override
                    public Pt next() {
                        return new Pt(++ix, Entry.this);
                    }
                };
            }

            @Override
            public int type() {
                return kind.intValue();
            }

            @Override
            public double[] points() {
                return points;
            }

            @Override
            public PathElementKind kind() {
                return kind;
            }

            Model model() {
                return Model.this;
            }

            int index() {
                return Model.this.entries.indexOf(this);
            }

        }
    }

    static class Pt extends EnhPoint2D {

        private final int index;
        private final Model.Entry owner;

        public Pt(int index, Model.Entry owner) {
            this.index = index;
            this.owner = owner;
            if (index < 0 || index >= owner.kind().pointCount()) {
                throw new IllegalArgumentException("Bad point index "
                        + index + " of " + owner.kind().pointCount() + " for "
                        + owner.kind());
            }
        }

        public <T extends Rectangle2D> T addSiblingsToBounds(T rect) {
            for (int i = 0; i < owner.points.length; i += 2) {
                rect.add(owner.points[i], owner.points[i + 1]);
            }
            return rect;
        }

        Pt next() {
            if (isDestination()) {
                int elIx = owner.index();
                if (elIx < owner.model().size()) {
                    return owner.model().entries.get(elIx + 1).pointsIterator().next();
                } else {
                    return owner.model().firstPoint();
                }
            } else {
                return new Pt(index + 1, owner);
            }
        }

        Pt prev() {
            if (index == 0) {
                int elIx = owner.index();
                if (elIx == 0) {
                    return owner.model().firstPoint();
                }
                return owner.model().entries.get(elIx - 1).destination();
            }
            return new Pt(index - 1, owner);
        }

        public boolean isDestination() {
            return kind().isDestination();
        }

        public Pt configureLineToSiblings(EqLine line, Runnable painter) {
            switch (owner.kind()) {
                case CLOSE:
                case MOVE:
                case LINE:
                    break;
                default:
                    if (isDestination()) {
                        line.x1 = getX();
                        line.y1 = getY();
                        for (int i = 0; i < owner.points.length - 2; i += 2) {
                            line.x2 = owner.points[i];
                            line.y2 = owner.points[i + 1];
                            painter.run();
                        }
                    }
            }
            return this;
        }

        @Override
        public Model.Entry owner() {
            return owner;
        }

        public PointKind kind() {
            return owner.kind.pointKindFor(index);
        }

        @Override
        public double getX() {
            return owner.points[index * 2];
        }

        @Override
        public double getY() {
            return owner.points[(index * 2) + 1];
        }

        @Override
        public void setLocation(double x, double y) {
            double oldX = owner.points[index * 2];
            double oldY = owner.points[(index * 2) + 1];
            if (oldX != x || oldY != y) {
                owner.points[index * 2] = x;
                owner.points[(index * 2) + 1] = y;
                owner.model().onChange(oldX, oldY);
                owner.model().onChange(x, y);
            }
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o == null) {
                return false;
            }
            if (o instanceof Pt) {
                Pt other = (Pt) o;
                return other.index == index && other.owner() == owner();
            }
            return false;
        }

        public int hashCode() {
            return 73 * index + (47 * owner.hashCode());
        }
    }

    static abstract class EnhPoint2D extends Point2D {

        public abstract PointKind kind();

        public abstract PathElement owner();

        public void setX(double x) {
            setLocation(x, getY());
        }

        public void setY(double y) {
            setLocation(getX(), y);
        }

        public void move(double dx, double dy) {
            setLocation(getX() + dx, getY() + dy);
        }

        public boolean ifHit(double x, double y, double radius, DistPointConsumer c) {
            double dist = distance(x, y, radius);
            boolean result = dist != java.lang.Double.MAX_VALUE;
            if (result) {
                c.hit(this, dist);
            }
            return result;
        }

        public double distance(double x, double y, double radius) {
            double dist = distance(x, y);
            if (dist > radius) {
                return java.lang.Double.MAX_VALUE;
            }
            return dist;
        }

        public boolean isDestination() {
            return kind().isDestination();
        }
    }

    interface DistPointConsumer {

        void hit(EnhPoint2D point, double distance);
    }

}
