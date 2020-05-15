package org.netbeans.paint.tools.path;

import org.netbeans.paint.tools.responder.PathUIProperties;
import org.netbeans.paint.tools.responder.ResponderTool;
import org.netbeans.paint.tools.responder.Responder;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant.Repainter;
import net.java.dev.imagine.api.image.Surface;
import org.imagine.geometry.Circle;
import org.imagine.editor.api.PaintingStyle;
import org.imagine.geometry.EqLine;
import org.imagine.geometry.EqPointDouble;
import org.imagine.geometry.util.GeometryStrings;
import org.netbeans.paint.tools.responder.PaintingResponder;

/**
 *
 * @author Tim Boudreau
 */
public final class PathTool extends ResponderTool implements Consumer<Rectangle>, Supplier<PathUIProperties> {

    private VectorPathModel currentState;

    public PathTool(Surface obj) {
        super(obj);
    }

    @Override
    protected void reset() {
        currentState = null;
    }

    @Override
    protected Responder firstResponder() {
        return new StartingPointEstablisher();
    }

    @Override
    protected void onAttachRepainter(Repainter rep) {
        rep.requestRepaint();
    }

    @Override
    public void accept(Rectangle r) {
        repaint(r);
    }

    @Override
    protected Rectangle paintCommit(Graphics2D g) {
        if (currentState != null) {
            Shape shape = currentState.toPath();
            if (shape != null) {
                PaintingStyle ps = ResponderTool.fillC.get();
                if (ps.isFill()) {
                    g.setPaint(paintC.get().getPaint());
                    g.fill(shape);
                }
                if (ps.isOutline()) {
                    g.setPaint(outlineC.get().getPaint());
                    g.setStroke(strokeC.get());
                    g.draw(shape);
                } else {
                    System.out.println("not outline");
                }
                return shape.getBounds();
            }
        }
        return null;
    }

    @Override
    protected Rectangle paintLive(Graphics2D g, Rectangle layerBounds) {
        g.setPaint(outlineC.get().getPaint());
        g.setStroke(get().lineStroke());
        return null;
    }

    class StartingPointEstablisher extends Responder implements PaintingResponder {

        private VectorPathModel state;
        private EqPointDouble lastMove;
        private final Circle circ = new Circle();

        @Override
        protected Responder onClick(double x, double y, MouseEvent e) {
            EqPointDouble pt = new EqPointDouble(x, y);
            // Note that the click comes *before* the call to attach(), if the
            // layer we're editing was not active - so colors is null at this point.
            state = new VectorPathModel(pt, () -> {
                return get();
            });
            return new AddPointHandler(state, pt);
        }

        private void repaintPoint() {
            double rad = get().pointRadius();
            Rectangle r = new Rectangle();
            r.setFrame(lastMove.x - rad, lastMove.y - rad, lastMove.x + rad, lastMove.y + rad);
            repaint(r);
        }

        @Override
        protected Responder onPress(double x, double y, MouseEvent e) {
            return onMove(x, y, e);
        }

        @Override
        protected Responder onMove(double x, double y, MouseEvent e) {
            lastMove = new EqPointDouble(x, y);
            return this;
        }

        @Override
        public Rectangle paint(Graphics2D g, Rectangle bounds) {
            if (lastMove != null) {
                circ.setCenter(lastMove);
                circ.setRadius(get().pointRadius());
                g.setPaint(get().initialPointFill());
                g.fill(circ);
                g.setPaint(get().lineDraw());
                g.draw(circ);
            }
            if (state != null) {
                return state.paint(g, bounds, false);
            }
            return null;
        }

        @Override
        protected Responder onKeyPress(KeyEvent e) {
            if (lastMove == null) {
                Rectangle r = new Rectangle();
                ctx().fetchVisibleBounds(r);
                lastMove = new EqPointDouble(r.getCenterX(), r.getCenterY());
            }
            double amt = get().ctx().zoom().inverseScale(1);
            switch (e.getKeyCode()) {
                case KeyEvent.VK_BACK_SPACE:
                    if (state.removeLast().isEmpty()) {
                        return new StartingPointEstablisher();
                    }
                    break;
                case KeyEvent.VK_LEFT:
                    lastMove.x -= amt;
                    repaintPoint();
                    break;
                case KeyEvent.VK_RIGHT:
                    lastMove.x += amt;
                    repaintPoint();
                    break;
                case KeyEvent.VK_UP:
                    lastMove.y -= amt;
                    repaintPoint();
                    break;
                case KeyEvent.VK_DOWN:
                    lastMove.y += amt;
                    repaintPoint();
                    break;
                case KeyEvent.VK_SPACE:
                case KeyEvent.VK_PERIOD:
                    state = new VectorPathModel(lastMove, PathTool.this);
                    return new AddPointHandler(state, lastMove);
            }
            return this;
        }

    }

    class AddPointHandler extends Responder implements PaintingResponder {

        final VectorPathModel state;
        private final Rectangle lastRepaintBounds = new Rectangle();
        private EqPointDouble lastMovePoint;
        private final Circle circle = new Circle(0, 0, 6);
        private final EqLine scratchLine = new EqLine();
        private final Rectangle scratchRect = new Rectangle();

        public AddPointHandler(VectorPathModel state, EqPointDouble initialMovePoint) {
            lastMovePoint = new EqPointDouble(initialMovePoint);
            this.state = state;
        }

        private int typeForModifiers(int modifiers) {
            if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0) {
                return PathIterator.SEG_CUBICTO;
            } else if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0) {
                return PathIterator.SEG_QUADTO;
            } else if ((modifiers & InputEvent.ALT_DOWN_MASK) != 0) {
                return PathIterator.SEG_CLOSE;
            }
            return PathIterator.SEG_LINETO;
        }

        private EqPointDouble hoverPoint;

        private void repaintPoint() {
            double rad = get().pointRadius();
            Rectangle r = new Rectangle();
            r.setFrame(lastMovePoint.x - rad, lastMovePoint.y - rad,
                    lastMovePoint.x + rad, lastMovePoint.y + rad);
            repaint(r);
        }

        @Override
        protected Responder onMove(double x, double y, MouseEvent e) {
            lastMovePoint = new EqPointDouble(x, y);
            // PathTool.this::repaint should work, but
            // getting BootstrapMethodError, so have PathTool implement
            // Consumer<Rectangle> instead.  May be due to mix of bits compiled
            // by jdk 8 and 12.  Implementing it is cheaper anyway.
            Hit hit = state.hit(lastMovePoint, PathTool.this);
            if (hit != null) {
                EqPointDouble overPoint = hit.get(EqPointDouble.class);
                hoverPoint = overPoint;
            } else {
                hoverPoint = null;
            }
            repaintPoint();
            return this;
        }

        @Override
        protected Responder onPress(double x, double y, MouseEvent e) {
            EqPointDouble pt = lastMovePoint = new EqPointDouble(x, y);
            Hit hit = state.hit(pt, PathTool.this);
            if (hit != null) {
                PointEditor ed = hit.get(PointEditor.class);
                if (ed != null) {
                    if (state.firstPoint() != ed.point) {
                        return new AdjustControlPointResponder(ed, true);
                    }
                }
            }
            return this;
        }

        MouseEvent last;
        Exception lastEx;

        @Override
        protected Responder onClick(double x, double y, MouseEvent e) {
            System.out.println("OnClick " + GeometryStrings.toString(x, y));
            if (last == e) {
                new Exception("Called twice with same event! " + x + "," + y, lastEx);
            }
            lastEx = new Exception();

            EqPointDouble p = lastMovePoint = new EqPointDouble(x, y);
            EqPointDouble first = state.firstPoint();
            boolean close = false;
            double dist = ctx().zoom().inverseScale(6);
            if (p.distance(first) < dist) {
                close = true;
                p = first;
            }
            int type = typeForModifiers(e.getModifiersEx());
            PointEditor editor = state.addPoint(new EqPointDouble(p), type, PathTool.this);
            if (editor != null) {
                return new AdjustControlPointResponder(editor, false);
            }
            if (type == PathIterator.SEG_CLOSE || close) {
                PathTool.this.currentState = state;
                try {
                    PathTool.this.commit();
                } finally {
                    PathTool.this.currentState = null;
                }
                return new StartingPointEstablisher();
            }
            return this;
        }

        @Override
        protected Responder onKeyPress(KeyEvent e) {
            double amt = ctx().zoom().inverseScale(1);
            if (e.isShiftDown() && e.isControlDown()) {
                amt *= 100;
            } else if (e.isShiftDown()) {
                amt *= 10;
            }
            if (lastMovePoint == null) {
                Rectangle r = new Rectangle();
                ctx().fetchVisibleBounds(r);
                lastMovePoint = new EqPointDouble(r.getCenterX(), r.getCenterY());
            }
            switch (e.getKeyCode()) {
                case KeyEvent.VK_ENTER:
                    if ((e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) == 0) {
                        state.addPoint(null, PathIterator.SEG_CLOSE, PathTool.this);
                    }
                    PathTool.this.commit();
                    return new StartingPointEstablisher();
                case KeyEvent.VK_ESCAPE:
                    repaint();
                    return new StartingPointEstablisher();
                case KeyEvent.VK_BACK_SPACE:
                    if (state.removeLast().isEmpty()) {
                        return new StartingPointEstablisher();
                    }
                    break;
                case KeyEvent.VK_LEFT:
                    lastMovePoint.x -= amt;
                    repaintPoint();
                    break;
                case KeyEvent.VK_RIGHT:
                    lastMovePoint.x += amt;
                    repaintPoint();
                    break;
                case KeyEvent.VK_UP:
                    lastMovePoint.y -= amt;
                    repaintPoint();
                    break;
                case KeyEvent.VK_DOWN:
                    lastMovePoint.y += amt;
                    repaintPoint();
                    break;
                case KeyEvent.VK_SPACE:
                case KeyEvent.VK_PERIOD:
                    PointEditor editor = state.addPoint(lastMovePoint, typeForModifiers(e.getModifiersEx()), PathTool.this);
                    repaintPoint();
                    if (editor != null) {
                        return new AdjustControlPointResponder(editor, false);
                    }
                    break;
            }
            return this;
        }

        @Override
        public Rectangle paint(Graphics2D g, Rectangle bounds) {
            lastRepaintBounds.setBounds(0, 0, 0, 0);
            double xl = ctx().zoom().inverseScale(1.1);

            g.setColor(get().lineDraw());
            g.setStroke(get().lineStroke());
            Shape p = state.toPath();
            if (p != null) {
                g.draw(p);
                lastRepaintBounds.add(p.getBounds());
                if (get().hasLineShadows()) {
                    g.translate(xl, xl);
                    g.setPaint(get().lineShadow());
                }
            }

            Rectangle sp = state.paint(g, bounds, false);
            if (sp != null) {
                lastRepaintBounds.add(sp);
            }
            float hitTolerance = get().ctx().zoom().inverseScale(1);
            BasicStroke stroke = get().lineStroke();
            if (lastMovePoint != null) {
                EqPointDouble point = state.lastPoint();
                if (point != null) {
                    EqPointDouble fp = state.firstPoint();
                    boolean pointChanged = !point.equals(lastMovePoint, hitTolerance);
                    boolean canPaint = pointChanged;
                    if (fp == point) {
                        canPaint = state.size() == 1;
                    }
                    if (canPaint) {
                        if (bounds == null || (bounds.contains(lastMovePoint) || bounds.contains(point))) {
                            BasicStroke pls = get().proposedLineStroke();
                            g.setStroke(pls);
                            g.setColor(get().proposedLineDraw());
                            assert lastMovePoint != null : "lastMovePoint null";
                            assert scratchLine != null : "scratchLine null";
                            scratchLine.setLine(lastMovePoint.x, lastMovePoint.y, point.x, point.y);
                            g.draw(scratchLine);
                            if (get().hasLineShadows()) {
                                double offset = get().ctx().zoom().inverseScale(pls.getLineWidth());
                                g.setColor(get().proposedLineShadow());
                                g.translate(offset, offset);
                                g.draw(scratchLine);
                                g.translate(-offset, -offset);
                            }
                            scratchRect.setFrameFromDiagonal(point, lastMovePoint);
                            lastRepaintBounds.add(scratchRect);

                            g.setStroke(stroke);
                            circle.setRadius(get().pointRadius());
                            circle.setCenter(point.x, point.y);

                            g.setColor(get().destinationPointFill());
                            g.fill(circle);
                            g.setColor(get().destinationPointDraw());
                            g.draw(circle);
                            Rectangle2D r = circle.getBounds2D();
                            r.add(r.getX() + r.getWidth() + stroke.getLineWidth(),
                                    r.getY() + r.getHeight() + stroke.getLineWidth());
                            r.add(r.getX() + r.getWidth() + stroke.getLineWidth(),
                                    r.getY() + r.getHeight() + stroke.getLineWidth());
                            lastRepaintBounds.add(r.getBounds());

                            circle.setCenter(lastMovePoint.x, lastMovePoint.y);

                            g.setColor(get().destinationPointFill());
                            g.fill(circle);
                            g.setColor(get().destinationPointDraw());
                            g.draw(circle);
                            lastRepaintBounds.add(circle.getBounds());

                            r = circle.getBounds2D();
                            r.add(r.getX() + r.getWidth() + stroke.getLineWidth(),
                                    r.getY() + r.getHeight() + stroke.getLineWidth());
                            r.add(r.getX() + r.getWidth() + stroke.getLineWidth(),
                                    r.getY() + r.getHeight() + stroke.getLineWidth());
                            lastRepaintBounds.add(r.getBounds());

                            return lastRepaintBounds;
                        } else {
                            System.out.println("  pt withing tol");
                        }
                    }
                }
            } else {
                System.out.println("  lastMovePoint is null");
            }

            if (hoverPoint != null) {
                circle.setRadius(get().pointRadius());
                circle.setCenter(hoverPoint.x, hoverPoint.y);

                g.setColor(get().hoveredDotFill());
                g.fill(circle);
                g.setColor(get().hoveredDotDraw());
                g.draw(circle);
                Rectangle2D r = circle.getBounds2D();
                r.add(r.getX() + r.getWidth() + stroke.getLineWidth(),
                        r.getY() + r.getHeight() + stroke.getLineWidth());
                r.add(r.getX() + r.getWidth() + stroke.getLineWidth(),
                        r.getY() + r.getHeight() + stroke.getLineWidth());
                lastRepaintBounds.add(r.getBounds());
            }
            return null;
        }

        class AdjustControlPointResponder extends Responder implements PaintingResponder {

            final PointEditor editor;
            final boolean onDrag;

            public AdjustControlPointResponder(PointEditor editor, boolean onDrag) {
                this.editor = editor;
                this.onDrag = onDrag;
            }

            @Override
            protected Responder onMove(double x, double y, MouseEvent e) {
                if (!onDrag) {
                    editor.updatePoint(x, y);
                }
                return this;
            }

            @Override
            protected Responder onDrag(double x, double y, MouseEvent e) {
                if (onDrag) {
                    editor.updatePoint(x, y);
                }
                return this;
            }

            @Override
            protected Responder onRelease(double x, double y, MouseEvent e) {
                if (onDrag) {
                    return onClick(x, y, e);
                }
                return super.onRelease(x, y, e);
            }

            @Override
            protected Responder onClick(double x, double y, MouseEvent e) {
                PointEditor nextEditor = editor.setPoint(x, y);
                if (nextEditor != null) {
                    return new AdjustControlPointResponder(nextEditor, false);
                }
                return AddPointHandler.this;
            }

            @Override
            protected Responder onKeyPress(KeyEvent e) {
                double amt = get().ctx().zoom().inverseScale(1);
                if (e.isShiftDown() && e.isControlDown()) {
                    amt *= 100;
                } else if (e.isShiftDown()) {
                    amt *= 10;
                }
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_ESCAPE:
                        repaint();
                        return new StartingPointEstablisher();
                    case KeyEvent.VK_BACK_SPACE:
                        if (state.removeLast().isEmpty()) {
                            return new StartingPointEstablisher();
                        }
                        break;
                    case KeyEvent.VK_LEFT:
                        editor.shiftX(-amt);
                        repaintPoint();
                        break;
                    case KeyEvent.VK_RIGHT:
                        editor.shiftX(amt);
                        repaintPoint();
                        break;
                    case KeyEvent.VK_UP:
                        editor.shiftY(-amt);
                        repaintPoint();
                        break;
                    case KeyEvent.VK_DOWN:
                        editor.shiftY(amt);
                        repaintPoint();
                        break;
                    case KeyEvent.VK_SPACE:
                    case KeyEvent.VK_PERIOD:
                        if (lastMovePoint != null) {
                            PointEditor nextEditor = editor.setPoint(editor.point.x, editor.point.y);
                            if (nextEditor != null) {
                                return new AdjustControlPointResponder(nextEditor, false);
                            }
                            return AddPointHandler.this;
                        }
                        break;
                }
                return this;
            }

            public Rectangle paint(Graphics2D g, Rectangle bounds) {
                lastRepaintBounds.setBounds(0, 0, 0, 0);
                g.setColor(get().lineDraw());
                g.setStroke(get().lineStroke());
                Shape p = state.toPath();
                if (p != null) {
                    g.draw(p);
                    Rectangle r = p.getBounds();
                    lastRepaintBounds.add(r);
                    if (get().hasLineShadows()) {
                        double xl = get().ctx().zoom().inverseScale(1.1);
                        g.translate(xl, xl);
                        g.setPaint(get().lineShadow());
                    }
                }
                Rectangle sp = state.paint(g, bounds, false);
                if (sp != null) {
                    lastRepaintBounds.add(sp);
                }
                return lastRepaintBounds;
            }
        }
    }
}
