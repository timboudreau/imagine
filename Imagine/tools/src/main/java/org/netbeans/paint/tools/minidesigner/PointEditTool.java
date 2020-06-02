package org.netbeans.paint.tools.minidesigner;

import org.imagine.geometry.uirect.ResizeMode;
import com.mastfrog.function.state.Bool;
import com.mastfrog.function.state.Dbl;
import com.mastfrog.function.state.Obj;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.java.dev.imagine.api.image.Surface;
import org.imagine.editor.api.AspectRatio;
import org.imagine.editor.api.ImageEditorBackground;
import org.imagine.editor.api.Zoom;
import org.imagine.geometry.Circle;
import org.imagine.geometry.EnhRectangle2D;
import org.imagine.geometry.EqLine;
import org.imagine.geometry.EqPointDouble;
import org.imagine.geometry.util.PooledTransform;
import org.netbeans.paint.api.cursor.Cursors;
import org.imagine.geometry.uirect.MutableRectangle2D;
import org.netbeans.paint.tools.responder.PaintingResponder;
import org.netbeans.paint.tools.responder.Responder;
import org.netbeans.paint.tools.responder.ResponderTool;
import org.netbeans.paint.tools.spi.CursorSupport;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;

/**
 * A Tool implementation specific to the generic design customizer, which uses
 * the mini-tool-canvas to allow the user to edit a portion of a path. This tool
 * should not be registered in the set of user-visible tools, as it is really
 * specific to that purpose, and doesn't offer any way to complete the shape
 * (which is always complete as far as that ui is concerned.
 *
 * @author Tim Boudreau
 */
final class PointEditTool extends ResponderTool implements ChangeListener, LookupListener {

    private PathSegmentModel mdl;
    private Lookup.Result<PathSegmentModel> res;
    private Shape lastShape;
    private Shape lastHitTest;

    public PointEditTool(Surface obj) {
        super(obj);
    }

    private void updateShape() {
        PointsProvider pp = currentLookup().lookup(PointsProvider.class);
        if (mdl == null || pp == null) {
            lastShape = null;
            lastHitTest = null;
        } else {
            pp.withStartAndEnd((start, end) -> {
                Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);
                path.moveTo(start.x, start.y);
                if (mdl.size() == 0) {
                    path.lineTo(end.x, end.y);
                }
                mdl.apply(path, start, end);
                lastShape = path;
                Zoom zoom = ctx().zoom();
                BasicStroke stroke = zoom.getStroke(7);
                lastHitTest = stroke.createStrokedShape(path);
                repaint();
            });
        }
    }

    @Override
    protected void onAttach() {
        res = currentLookup().lookupResult(PathSegmentModel.class);
        res.addLookupListener(this);
        ctx().zoom().addChangeListener(this);
        refresh();
    }

    private void refresh() {
        Collection<? extends PathSegmentModel> all = res.allInstances();
        PathSegmentModel nue = all.iterator().next();
        if (nue != mdl) {
            if (mdl != null) {
                mdl.removeChangeListener(this);
            }
            if (nue != null) {
                nue.addChangeListener(this);
            }
            mdl = nue;
        }
        updateShape();
    }

    @Override
    protected void reset() {
        if (mdl != null) {
            mdl.removeChangeListener(this);
        }
        if (res != null) {
            res.removeLookupListener(this);
        }
        Zoom zoom = ctx().zoom();
        zoom.removeChangeListener(this);
        mdl = null;
        res = null;
        lastShape = null;
        lastHitTest = null;
    }

    @Override
    protected Responder firstResponder() {
        return new PointSelectOrCreateResponder();
    }

    @Override
    protected Rectangle paintCommit(Graphics2D g) {
        return new Rectangle();
    }

    @Override
    protected Rectangle paintLive(Graphics2D g, Rectangle layerBounds) {
        return new Rectangle();
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() instanceof Zoom) {
            refresh();
        } else {
            updateShape();
        }
    }

    @Override
    public void resultChanged(LookupEvent le) {
        refresh();
    }

    class PointSelectOrCreateResponder extends Responder {

        private EqPointDouble hoverPoint = new EqPointDouble();
        private boolean hoverSet;
        private final Circle test = new Circle();

        @Override
        protected Responder onMove(double x, double y, MouseEvent e) {
            hoverSet = true;
            hoverPoint.setLocation(x, y);
            Zoom zoom = ctx().zoom();
            double radius = zoom.inverseScale(9);
            test.setRadius(radius);
            Bool cursorSet = Bool.create();
            mdl.visitPoints(currentLookup().lookup(PointsProvider.class), (seg, index, kind, point) -> {
                test.setCenter(point);
                if (test.contains(x, y)) {
                    Cursors cursors = cursors();
                    setCursor(cursors.arrowsCrossed());
                    cursorSet.set();
                }
            });
            cursorSet.ifUntrue(() -> {
                setCursor(Cursor.getDefaultCursor());
                if (lastHitTest.contains(new EqPointDouble(x, y))) {
                    Cursors cursors = cursors();
                    setCursor(cursors.star());
                }
            });
            return this;
        }

        @Override
        protected Responder onDrag(double x, double y, MouseEvent e) {
            if (hoverSet) {
                EqPointDouble pt = hoverPoint.copy();
                return new SelectionResponder(pt, new EqPointDouble(x, y));
            }
            onMove(x, y, e);
            return this;
        }

        @Override
        protected Responder onRelease(double x, double y, MouseEvent e) {
            onMove(x, y, e);
            return this;
        }

        @Override
        protected Responder onPress(double x, double y, MouseEvent e) {
            onMove(x, y, e);
            Obj<Point2D> best = Obj.create();
            Obj<SegmentEntry> bestEntry = Obj.create();
            Dbl dist = Dbl.of(Double.MAX_VALUE);
            mdl.visitPoints(currentLookup().lookup(PointsProvider.class), (seg, index, kind, point) -> {
                if (index == 0) {
                    // can't change the start point
                    //                        return;
                }
                double d = point.distance(x, y);
                if (d < dist.getAsDouble()) {
                    double old = dist.set(d);
                    if (old != d) {
                        best.set(point);
                        bestEntry.set(seg);
                    }
                }
            });
            if (best.isSet()) {
                double p = get().pointRadius() * 1.25;
                if (dist.get() <= p) {
                    return new AdjustPointResponder(best.get(), bestEntry.get());
                }
            }
            return this;
        }

        @Override
        protected Responder onClick(double x, double y, MouseEvent e) {
            onMove(x, y, e);
            if (e.getClickCount() == 2 && !e.isPopupTrigger()) {
                Obj<Point2D> startPoint = Obj.create();
                Obj<Point2D> targetPoint = Obj.create();
                Obj<SegmentEntry> targetSegment = Obj.create();
                Dbl bestDist = Dbl.of(Double.MAX_VALUE);
                PointsProvider pp = currentLookup().lookup(PointsProvider.class);
                EqLine currLine = pp.get();
                startPoint.set(currLine.startPoint());
                if (mdl.size() == 0) {
                    SegmentEntry entry = mdl.insertCubicAfter(currLine.startPoint(), currLine.endPoint(), null, x, y);
                    return new PositionControlPointsResponder(entry);
                }
                mdl.visitPoints(pp, (seg, index, kind, point) -> {
                    double dist = point.distance(x, y);
                    if (dist < bestDist.getAsDouble()) {
                        targetPoint.set(point);
                        bestDist.set(dist);
                        targetSegment.set(seg);
                    }
                });
                if (targetSegment.isSet()) {
                    Point2D pt = targetPoint.get();
                    SegmentEntry seg = targetSegment.get();
                    // XXX really we should trace the line and find the point
                    // that way, so a click in the case the shape contains a loop
                    // finds the right spot - would need to move the shape tracing
                    // code out of vector to do that without a dependency here
                    boolean before = x <= pt.getX();
                    double dx = x;
                    double dy = y;
                    if (before) {
                        SegmentEntry entry = mdl.insertCubicBefore(currLine.startPoint(), currLine.endPoint(), seg, dx, dy);
                        return new PositionControlPointsResponder(entry);
                    } else {
                        SegmentEntry entry = mdl.insertCubicAfter(currLine.startPoint(), currLine.endPoint(), seg, dx, dy);
                        return new PositionControlPointsResponder(entry);
                    }
                }
            }
            return this;
        }

        @Override
        protected Responder onExit(double x, double y, MouseEvent e) {
            hoverSet = false;
            return this;
        }

        @Override
        protected Responder onEnter(double x, double y, MouseEvent e) {
            onMove(x, y, e);
            return this;
        }

        @Override
        protected void resign(Rectangle addTo) {
            if (hoverSet) {
                addTo.add(hoverPoint);
            }
        }

        class SelectionResponder extends Responder implements PaintingResponder {

            private final MutableRectangle2D rect;

            SelectionResponder(EqPointDouble startPoint, EqPointDouble initialDragPoint) {
                rect = new MutableRectangle2D(startPoint, initialDragPoint);
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
                Color base = ImageEditorBackground.getDefault().style().contrasting();
                Color haze = new Color(base.getRed(), base.getGreen(), base.getBlue(), 90);
                g.setColor(haze);
                g.fill(rect);
                Color hazeLine = new Color(base.getRed(), base.getGreen(), base.getBlue(), 170);
                g.setColor(hazeLine);
                g.draw(rect);
                EnhRectangle2D r = new EnhRectangle2D(rect);
                mdl.visitPoints(currentLookup().lookup(PointsProvider.class), (seg, id, x, pt) -> {
                    if (r.contains(pt)) {
                        g.setColor(Color.BLUE);
                        circle.setCenter(pt);
                        circle.setRadius(get().pointRadius());
                        g.fill(circle);
                        r.add(circle);
                    }
                });
                return r.getBounds();
            }

            class WithSelectionResponder extends Responder implements PaintingResponder {

                private final MutableRectangle2D rect;
                private Point2D armedPoint;
                private boolean dragRectangleArmed;

                public WithSelectionResponder(MutableRectangle2D rect) {
                    this.rect = rect;
                }

                @Override
                protected Responder onKeyPress(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                        Set<SegmentEntry> toDelete = new HashSet<>();
                        mdl.visitPoints(currentLookup().lookup(PointsProvider.class), (seg, index, kind, point) -> {
                            if (rect.contains(point.getX(), point.getY())) {
                                toDelete.add(seg);
                            }
                        });
                        for (SegmentEntry se : toDelete) {
                            mdl.delete(se);
                        }
                        if (!toDelete.isEmpty()) {
                            repaint();
                            return firstResponder();
                        }
                    } else if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
                        if (armedPoint != null) {
                            setCursor(cursors().rotateMany());
                        }
                    }
                    return this;
                }

                @Override
                protected Responder onKeyRelease(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
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
                    mdl.visitPoints(currentLookup().lookup(PointsProvider.class), (seg, index, kind, point) -> {
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
                        setCursor(CursorSupport.cursor(rm));
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
                        return new PointSelectOrCreateResponder().onPress(x, y, e);
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
                List<Point2D> all;

                @Override
                protected Responder onDrag(double x, double y, MouseEvent e) {
                    if (armedPoint != null) {
                        if (e.isShiftDown()) {
                            if (all == null) {
                                all = new ArrayList<>(10);
                                mdl.visitPoints(currentLookup().lookup(PointsProvider.class), (seg, index, kind, point) -> {
                                    if (rect.contains(point)) {
                                        all.add(point);
                                    }
                                });
                            }
                            if (!all.isEmpty()) {
                                EnhRectangle2D hits = new EnhRectangle2D();
                                double[] pts = new double[all.size() * 2];
                                double rad = get().pointRadius();
                                for (int i = 0; i < all.size(); i++) {
                                    int offset = i * 2;
                                    Point2D pt = all.get(i);
                                    pts[offset] = pt.getX();
                                    pts[offset + 1] = pt.getY();
                                    if (hits.isEmpty()) {
                                        hits.setFrame(pts[offset] - (rad / 2), pts[offset + 1] - (rad / 2), rad, rad);
                                    } else {
                                        hits.add(pts[offset], pts[offset + 1]);
                                    }
                                }
                                double dist = dragBase.distance(x, y);
                                dist = ctx().zoom().scale(dist);
                                if (dragBase.getX() < x && dragBase.getY() < y) {
                                    dist = -dist;
                                }
                                dragBase.setLocation(x, y);
                                if (rotateCenter == null) {
                                    rotateCenter = hits.center();
                                }
                                PooledTransform.withRotateInstance(dist * 0.05, rotateCenter.x, rotateCenter.y, xf -> {
                                    xf.transform(pts, 0, pts, 0, all.size());
                                });
                                hits.width = 0;
                                hits.height = 0;
                                for (int i = 0; i < all.size(); i++) {
                                    int offset = i * 2;
                                    Point2D pt = all.get(i);
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
                                repaint();
                            }
                        } else {
                            double dx = x - armedPoint.getX();
                            double dy = y - armedPoint.getY();
                            if (all == null) {
                                armedPoint.setLocation(x, y);
                                all = new ArrayList<>();
                                all.add(armedPoint);
                                mdl.visitPoints(currentLookup().lookup(PointsProvider.class), (seg, index, kind, point) -> {
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
                            repaint();
                            rect.x += dx;
                            rect.y += dy;
                        }
                        return this;
                    }
                    rect.setFrame(x, y, x, y);
                    repaint();
                    // if on a point, move everything
//                    return SelectionResponder.this.onDrag(x, y, e);
                    return this;
                }

                @Override
                protected Responder onClick(double x, double y, MouseEvent e) {
                    return new PointSelectOrCreateResponder().onClick(x, y, e);
                }

                @Override
                public Rectangle paint(Graphics2D g, Rectangle bounds) {
                    Rectangle result = SelectionResponder.this.paint(g, bounds);
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
                        repaint(rect);
                        ResizeMode newMode = mode.apply(x, y, rect);
                        if (newMode != mode) {
                            setCursor(CursorSupport.cursor(newMode));
                        }
                        mode = newMode;
                        repaint(rect);
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

        class AdjustPointResponder extends Responder {

            private final Point2D pt;
            private final SegmentEntry entry;

            private AdjustPointResponder(Point2D pt, SegmentEntry entry) {
                this.pt = pt;
                this.entry = entry;
            }

            @Override
            protected Responder onDrag(double x, double y, MouseEvent e) {
                repaintPoint(pt);
                pt.setLocation(x, y);
                repaintPoint(pt);
                return this;
            }

            @Override
            protected Responder onRelease(double x, double y, MouseEvent e) {
                // XXX delete on drag outside bounds?
                PointSelectOrCreateResponder.this.onMove(x, y, e);
                AspectRatio ratio = ctx().aspectRatio();
                if (!ratio.rectangle().contains(x, y)) {
                    mdl.delete(entry);
                    return firstResponder();
                }
                return PointSelectOrCreateResponder.this;
            }
        }

        class PositionControlPointsResponder extends Responder implements Runnable {

            private final SegmentEntry entry;
            private LinkedList<Point2D> points = new LinkedList<>();
            private boolean armed;
            private Point2D start;
            private final EqLine line = new EqLine();

            public PositionControlPointsResponder(SegmentEntry entry) {
                this.entry = entry;
                Supplier<EqLine> pp = currentLookup().lookup(PointsProvider.class);
                EqLine line = pp.get();
                this.start = line.startPoint();
                PathSegmentModel mdl = currentLookup().lookup(PathSegmentModel.class);
                mdl.visitPoints(entry, pp, (kind, point) -> {
                    if (!kind.isDestination()) {
                        points.add(point);
                    }
                });
            }

            @Override
            protected Responder onMove(double x, double y, MouseEvent e) {
                Point2D pt = points.peek();
                repaintPoint(pt);
                line.setLine(start, pt);
                repaintLine(line);
                pt.setLocation(x, y);
                line.setLine(start, pt);
                repaintLine(line);
                repaintPoint(pt);
                //                    repaint();
                return this;
            }

            @Override
            protected Responder onDrag(double x, double y, MouseEvent e) {
                return onMove(x, y, e);
            }

            @Override
            protected Responder onPress(double x, double y, MouseEvent e) {
                armed = true;
                return onMove(x, y, e);
            }

            @Override
            protected Responder onRelease(double x, double y, MouseEvent e) {
                if (armed) {
                    Point2D pt = points.pop();
                    repaintPoint(pt);
                    pt.setLocation(x, y);
                    repaintPoint(pt);
                    //                        repaint();
                    if (points.isEmpty()) {
                        return firstResponder();
                    }
                }
                return this;
            }

            @Override
            public void run() {
                // do nothing
            }
        }
    }
}
