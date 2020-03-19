package org.netbeans.paint.tools;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant.Repainter;
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import net.java.dev.imagine.api.image.Surface;
import org.imagine.geometry.Circle;
import org.netbeans.paint.tools.FreehandTool.ShapeState.Hit;
import static org.netbeans.paint.tools.MultiStateTool.zoomFactor;
import org.imagine.editor.api.PaintingStyle;

/**
 *
 * @author Tim Boudreau
 */
@ToolDef(name = "Freehand", iconPath = "org/netbeans/paint/tools/resources/freehand.png")
@Tool(Surface.class)
public class FreehandTool extends MultiStateTool {

    private ShapeState currentState;
    private static final double pointRadius = 7;

    public FreehandTool(Surface obj) {
        super(obj);
    }

    @Override
    protected void clearState() {
        currentState = null;
    }

    @Override
    protected InputHandler createInitialInputHandler() {
        return new InitialPoint();
    }

    @Override
    protected void onAttachRepainter(Repainter rep) {
        rep.requestRepaint();
    }

    @Override
    protected Rectangle paintCommit(Graphics2D g) {
        if (currentState != null) {
            Shape shape = currentState.toPath();
            if (shape != null) {
                PaintingStyle ps = MultiStateTool.fillC.get();
                if (ps.isFill()) {
                    g.setPaint(MultiStateTool.paintC.get().getPaint());
                    g.fill(shape);
                }
                if (ps.isOutline()) {
                    g.setPaint(MultiStateTool.outlineC.get());
                    g.setStroke(new BasicStroke(MultiStateTool.strokeC.get()));
                    g.draw(shape);
                }
                return shape.getBounds();
            }
        }
        return null;
    }

    @Override
    protected Rectangle paintLive(Graphics2D g, Rectangle layerBounds) {
        g.setPaint(MultiStateTool.outlineC.get());
        g.setStroke(new BasicStroke(1F / zoomFactor()));
        return null;
    }

    class InitialPoint extends InputHandler implements Paintable {

        private ShapeState state;

        @Override
        protected InputHandler onClick(MouseEvent e) {
            Point initialPoint = e.getPoint();
            ShapeState state = new ShapeState(initialPoint);
            return new AddPointHandler(state);
        }

        @Override
        public Rectangle paint(Graphics2D g, Rectangle bounds) {
            if (state != null) {
                return state.paint(g, bounds, false);
            }
            return null;
        }
    }

    class AddPointHandler extends InputHandler implements Paintable {

        private final ShapeState state;
        private final Rectangle lastRepaintBounds = new Rectangle();
        private Point lastMovePoint;
        private final Circle circle = new Circle(0, 0, 6);

        public AddPointHandler(ShapeState state) {
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

        private Point hoverPoint;

        @Override
        protected InputHandler onMove(MouseEvent e) {
            lastMovePoint = e.getPoint();
            Hit hit = state.hit(e.getPoint(), FreehandTool.this::repaint);
            if (hit != null) {
                Point overPoint = hit.get(Point.class);
                hoverPoint = overPoint;
            } else {
                hoverPoint = null;
            }
            return this;
        }

        @Override
        protected InputHandler onPress(MouseEvent e) {
            Hit hit = state.hit(e.getPoint(), FreehandTool.this::repaint);
            if (hit != null) {
                ShapeState.PointEditor ed = hit.get(ShapeState.PointEditor.class);
                if (ed != null) {
                    if (state.firstPoint() != ed.point) {
                        return new AdjustControlPointEditor(ed, true);
                    }
                }
            }
            return this;
        }

        @Override
        protected InputHandler onClick(MouseEvent e) {
            Point p = e.getPoint();
            Point first = state.firstPoint();
            boolean close = false;
            if (Point2D.distance(p.x, p.y, first.x, first.y) < 6) {
                close = true;
                p = first;
            }
            int type = typeForModifiers(e.getModifiersEx());
            ShapeState.PointEditor editor = state.addPoint(p, type, FreehandTool.this::repaint);
            if (editor != null) {
                return new AdjustControlPointEditor(editor, false);
            }
            if (type == PathIterator.SEG_CLOSE || close) {
                FreehandTool.this.currentState = state;
                try {
                    FreehandTool.this.commit();
                } finally {
                    FreehandTool.this.currentState = null;
                }
                return new InitialPoint();
            }
            return this;
        }

        class AdjustControlPointEditor extends InputHandler implements Paintable {

            private final ShapeState.PointEditor editor;
            private final boolean onDrag;

            public AdjustControlPointEditor(ShapeState.PointEditor editor, boolean onDrag) {
                this.editor = editor;
                this.onDrag = onDrag;
            }

            @Override
            protected InputHandler onMove(MouseEvent e) {
                if (!onDrag) {
                Point p = e.getPoint();
                editor.updatePoint(p.x, p.y);
                }
                return this;
            }

            @Override
            protected InputHandler onDrag(MouseEvent e) {
                if (onDrag) {
                    Point p = e.getPoint();
                    editor.updatePoint(p.x, p.y);
                }
                return this;
            }

            @Override
            protected InputHandler onRelease(MouseEvent e) {
                if (onDrag) {
                    return onClick(e);
                }
                return super.onRelease(e);
            }

            @Override
            protected InputHandler onClick(MouseEvent e) {
                Point p = e.getPoint();
                ShapeState.PointEditor nextEditor = editor.setPoint(p.x, p.y);
                if (nextEditor != null) {
                    return new AdjustControlPointEditor(nextEditor, false);
                }
                return AddPointHandler.this;
            }

            public Rectangle paint(Graphics2D g, Rectangle bounds) {
                lastRepaintBounds.setBounds(0, 0, 0, 0);
//                g.setColor(outlineC.get());
//                g.setStroke(new BasicStroke(strokeC.get()));
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke(1));
                Path2D p = state.toPath();
                if (p != null) {
                    g.draw(p);
                    lastRepaintBounds.add(p.getBounds());
                }

                Rectangle sp = state.paint(g, bounds, false);
                if (sp != null) {
                    lastRepaintBounds.add(sp);
                }
                return lastRepaintBounds;
            }
        }

        @Override
        public Rectangle paint(Graphics2D g, Rectangle bounds) {
            lastRepaintBounds.setBounds(0, 0, 0, 0);

            float zoom = zoomFactor();
//            g.setColor(MultiStateTool.outlineC.get());
//            g.setStroke(new BasicStroke(1F / zoom));
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(1));
            Path2D p = state.toPath();
            if (p != null) {
                g.draw(p);
                lastRepaintBounds.add(p.getBounds());
//            } else {
//                System.out.println("no path yet");
            }

            Rectangle sp = state.paint(g, bounds, false);
            if (sp != null) {
                lastRepaintBounds.add(sp);
            }
            float inverseZoom = 1f / zoomFactor();
            if (lastMovePoint != null) {
                Point point = state.lastPoint();
                if (point != null && !point.equals(lastMovePoint)) {
                    if (bounds == null || (bounds.contains(lastMovePoint) || bounds.contains(point))) {
                        BasicStroke stroke = new BasicStroke(inverseZoom);
                        g.setStroke(stroke);
                        g.setColor(Color.BLACK);
                        g.drawLine(lastMovePoint.x, lastMovePoint.y, point.x, point.y);
                        Rectangle r = new Rectangle();
                        r.setFrameFromDiagonal(point, lastMovePoint);
                        lastRepaintBounds.add(r);

                        circle.setRadius(pointRadius * (inverseZoom));
                        circle.setCenter(point.x, point.y);

                        g.setColor(Color.WHITE);
                        g.fill(circle);
                        g.setColor(Color.BLACK);
                        g.draw(circle);
                        lastRepaintBounds.add(circle.getBounds());

                        circle.setCenter(lastMovePoint.x, lastMovePoint.y);

                        g.setColor(Color.WHITE);
                        g.fill(circle);
                        g.setColor(Color.BLACK);
                        g.draw(circle);
                        lastRepaintBounds.add(circle.getBounds());
                        return lastRepaintBounds;
                    }
                }
                if (hoverPoint != null) {
                    circle.setRadius(pointRadius * (inverseZoom));
                    circle.setCenter(hoverPoint.x, hoverPoint.y);
                    g.setColor(Color.RED);
                    g.fill(circle);
                }
            }
            return null;
        }
    }

    static class ShapeState {

        private final List<ShapeEntry> entries = new ArrayList<>();
        private final Rectangle bounds = new Rectangle();
        private Path2D lastPath;
        private Shape stroked;
        private final Circle scratchCircle = new Circle(0, 0, 6);

        ShapeState(Point p) {
            addPoint(p, PathIterator.SEG_MOVETO, null);
        }

        public Point firstPoint() {
            if (entries.isEmpty()) {
                return null;
            }
            return entries.get(0).lastPoint();
        }

        public Rectangle paint(Graphics2D g, Rectangle clip, boolean isCommit) {
            Rectangle painted = new Rectangle();
            Shape path = toPath();
            if (path != null) {
                g.draw(path);
                painted.add(path.getBounds());
            }
            if (!isCommit) {
                Stroke oldStroke = g.getStroke();
                Paint oldPaint = g.getPaint();
                float zoomedStroke = (1F / zoomFactor());
                float[] dashes = new float[]{5F * zoomedStroke, 10F * zoomedStroke};
                BasicStroke dashed = new BasicStroke(zoomedStroke,
                        BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                        1F, dashes, 0F
                );

                scratchCircle.setRadius(zoomedStroke * pointRadius);
                BasicStroke stroke = new BasicStroke(zoomedStroke);
                try {
                    g.setStroke(stroke);
                    for (int i = 0; i < entries.size(); i++) {
                        ShapeEntry se = entries.get(i);
                        Color fill = i == 0 ? Color.ORANGE : Color.PINK;
                        se.paintPoints(g, scratchCircle, Color.white, fill, Color.BLUE, Color.gray, clip, painted, dashed);
                    }
                } finally {
                    if (oldStroke != null) {
                        g.setStroke(oldStroke);
                    }
                    g.setPaint(oldPaint);
                }
            }
            return painted;
        }

        interface Hit {

            final Hit NON_POINT = new Hit() {
            };

            default <T> T get(Class<T> type) {
                return type.isInstance(this) ? type.cast(this) : null;
            }
        }

        public Hit hit(Point p, Consumer<Rectangle> repainter) {
            for (ShapeEntry e : entries) {
                Hit result = e.hit(p.x, p.y, 5, bounds, r -> {
                    bounds.setBounds(r);
                    repainter.accept(r);
                });
                if (result != null) {
                    return result;
                }
            }
            if (stroked != null && stroked.contains(p.x, p.y)) {
                return Hit.NON_POINT;
            }
            return null;
        }

        public Point lastPoint() {
            int ix = entries.size() - 1;
            Point result = null;
            while (result == null && ix > 0) {
                result = entries.get(ix).lastPoint();
            }
            return result;
        }

        public Path2D toPath() {
//            if (lastPath != null) {
//                return lastPath;
//            }
            if (entries.size() == 1) {
                return null;
            }
            Path2D.Double pth = new Path2D.Double();
            for (ShapeEntry se : entries) {
                se.addTo(pth);
            }
            Path2D p = lastPath = pth;
            float zoomedStrokeSize = (1F / zoomFactor()) * 10F;
            BasicStroke stroke = new BasicStroke(zoomedStrokeSize);
            stroked = stroke.createStrokedShape(p);
            return lastPath = p;
        }

        private PointEditor addPoint(Point point, int type, Consumer<Rectangle> onRepaint) {
            point = new Point(point);
            if (entries.isEmpty()) {
                type = PathIterator.SEG_MOVETO;
            }
            if (type != PathIterator.SEG_CLOSE) {
                bounds.add(point);
            }
            ShapeEntry entry;
            switch (type) {
                case PathIterator.SEG_MOVETO:
                case PathIterator.SEG_LINETO:
                    entries.add(entry = new ShapeEntry(type, point));
                    break;
                case PathIterator.SEG_CUBICTO:
                    entries.add(entry = new ShapeEntry(type, point, new Point(point), new Point(point)));
                    break;
                case PathIterator.SEG_QUADTO:
                    entries.add(entry = new ShapeEntry(type, point, new Point(point)));
                    break;
                case PathIterator.SEG_CLOSE:
                    entries.add(entry = new ShapeEntry(type));
                    break;
                default:
                    throw new AssertionError("Unknown type " + type);
            }
            return entry.editor(bounds, onRepaint);
        }

        static class ShapeEntry {

            private final Point[] points;
            private final int type;
            private int editCursor;
            private final Circle hitCircle = new Circle(0, 0, 1);

            public ShapeEntry(int type, Point... points) {
                this.points = points;
                this.type = type;
            }

            Point lastPoint() {
                if (points.length > 0) {
                    return points[points.length - 1];
                }
                return null;
            }

            PointEditor hit(int x, int y, int tolerance, Rectangle bds, Consumer<Rectangle> repainter) {
                double zoom = 1D / zoomFactor();
                double rad = tolerance * zoom;
                hitCircle.setRadius(rad);
                for (int i = 0; i < points.length; i++) {
//                    hitCircle.setCenter(points[i].x, points[i].y);
//                    if (hitCircle.contains(points[i])) {
//                        return new PointEditor(points[i], bds, repainter, () -> null);
//                    }
                    int dx = Math.abs(x - points[i].x);
                    int dy = Math.abs(y - points[i].y);
                    if (dx <= tolerance && dy <= tolerance) {
                        return new PointEditor(points[i], bds, repainter, () -> null);
                    }
                }
                return null;
            }

            void paintPoints(Graphics2D g, Circle circle, Color ctrlFill, Color fill, Color ctrlDraw, Color draw, Rectangle clip, Rectangle addTo, BasicStroke dashStroke) {
                Point lp = lastPoint();
                BasicStroke stroke = (BasicStroke) g.getStroke();
                g.setColor(Color.LIGHT_GRAY);
                g.setStroke(dashStroke);
                // Draw control lines
                for (int i = 0; i < points.length - 1; i++) {
                    Point p = points[i];
                    if (clip != null && !clip.contains(p) && !clip.contains(lp)) {
                        continue;
                    }
                    g.drawLine(lp.x, lp.y, p.x, p.y);
                }
                g.setStroke(stroke);
                // draw points, coloring control points differently
                for (int i = 0; i < points.length; i++) {
                    boolean last = i == points.length - 1;
                    Point p = points[i];
                    if (clip != null && !clip.contains(p)) {
                        continue;
                    }
                    if (!last) {
                        g.setColor(ctrlFill);
                    } else {
                        g.setColor(fill);
                    }
                    circle.setCenter(p.x, p.y);
                    g.fill(circle);
                    if (!last) {
                        g.setColor(ctrlDraw);
                    } else {
                        g.setColor(draw);
                    }
                    g.draw(circle);
                    addTo.add(circle.getBounds());
                }
            }

            void addTo(Path2D path) {
                switch (type) {
                    case PathIterator.SEG_MOVETO:
                        path.moveTo(points[0].x, points[0].y);
                        break;
                    case PathIterator.SEG_LINETO:
                        path.lineTo(points[0].x, points[0].y);
                        break;
                    case PathIterator.SEG_CUBICTO:
                        path.curveTo(points[0].x, points[0].y, points[1].x,
                                points[1].y, points[2].x, points[2].y);
                        break;
                    case PathIterator.SEG_QUADTO:
                        path.quadTo(points[0].x, points[0].y, points[1].x, points[1].y);
                        break;
                }
            }

            Point currentPoint() {
                if (editCursor >= points.length - 1) {
                    return null;
                }
                return points[editCursor];
            }

            Point nextPoint() {
                editCursor++;
                return currentPoint();
            }

            PointEditor editor(Rectangle bds, Consumer<Rectangle> onChange) {
                if (points.length < 2) {
                    return null;
                }
                Supp supp = new Supp(bds, onChange);

                return new PointEditor(points[0], bds, onChange, supp);
            }

            class Supp implements Supplier<PointEditor> {

                private final Rectangle bds;
                private final Consumer<Rectangle> onChange;

                public Supp(Rectangle bds, Consumer<Rectangle> onChange) {
                    this.bds = bds;
                    this.onChange = onChange;
                }

                @Override
                public PointEditor get() {
                    Point np = nextPoint();
                    if (np != null) {
                        return new PointEditor(np, bds, onChange, this);
                    }
                    return null;
                }

            }
        }

        static class PointEditor implements Hit {

            private final Point point;
            private final Rectangle bds;
            private final Consumer<Rectangle> onChange;
            private final Supplier<PointEditor> nextPointEditor;

            public PointEditor(Point point, Rectangle bds, Consumer<Rectangle> onChange, Supplier<PointEditor> nextPointEditor) {
                this.point = point;
                this.bds = bds;
                this.onChange = onChange;
                this.nextPointEditor = nextPointEditor;
            }

            @Override
            public <T> T get(Class<T> type) {
                if (Point.class == type) {
                    return type.cast(point);
                }
                return Hit.super.get(type);
            }

            public void updatePoint(int x, int y) {
                point.x = x;
                point.y = y;
                bds.add(point);
                onChange.accept(bds);
            }

            PointEditor setPoint(int x, int y) {
                point.x = x;
                point.y = y;
                bds.add(point);
                onChange.accept(bds);
                return nextPointEditor.get();
            }

        }
    }
}
