package org.imagine.vector.editor.ui.tools.widget;

import com.mastfrog.function.TriConsumer;
import com.mastfrog.util.collections.DoubleMap;
import com.mastfrog.util.collections.DoubleSet;
import com.mastfrog.util.collections.IntSet;
import com.mastfrog.util.search.Bias;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import javax.swing.JComponent;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import org.imagine.geometry.Circle;
import org.imagine.geometry.EqPointDouble;
import org.imagine.utils.java2d.GraphicsUtils;
import org.imagine.vector.editor.ui.spi.ShapeControlPoint;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import org.imagine.vector.editor.ui.tools.MutableProxyLookup;
import org.imagine.vector.editor.ui.tools.widget.actions.DragHandler;
import org.imagine.vector.editor.ui.tools.widget.painting.DecorationController;
import org.imagine.vector.editor.ui.tools.widget.painting.DesignerProperties;
import org.imagine.vector.editor.ui.tools.widget.util.UIState;
import org.imagine.vector.editor.ui.tools.widget.util.UIState.UIStateListener;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.model.ObjectState;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Scene.SceneListener;
import org.netbeans.api.visual.widget.Widget;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 * Using one widget per control points does not scale when the number of control
 * points is enormous.
 *
 * @author Tim Boudreau
 */
public class ManyControlPointsWidget extends Widget {

    private final MutableProxyLookup lkp = new MutableProxyLookup();
    private final Lookup shapesLookup;
    private final DoubleMap<Set<ShapeControlPoint>> xPoints;
    private final DoubleMap<Set<ShapeControlPoint>> yPoints;
    private final Map<ShapeControlPoint, EqPointDouble> reverseIndex = new HashMap<>();
    private final Circle circ = new Circle(0, 0, 1);
    private final DesignerProperties renderingProperties;
    private final UIState uiState;
    private final Lookup selection;

    public ManyControlPointsWidget(Scene scene, ShapesCollection coll, DesignerProperties renderingProperties, UIState uiState, Lookup selection, DragHandler drag) {
        super(scene);
        xPoints = DoubleMap.create(coll.size() * 12);
        yPoints = DoubleMap.create(coll.size() * 12);
        this.renderingProperties = renderingProperties;
        this.uiState = uiState;
        shapesLookup = Lookups.fixed(coll, this, drag);
        this.selection = selection;
        init();
        lkp.updateLookups(shapesLookup);
        getActions().addAction(new Clicks());
    }

    class Clicks extends WidgetAction.Adapter {

        @Override
        public State mousePressed(Widget widget, WidgetMouseEvent event) {
            if (uiState.controlPointsDraggable()) {
                ShapeControlPoint atPoint = findForPoint(event.getPoint());
                if (atPoint != null) {
                    setSelected(atPoint);
                    widget.getScene().setFocusedWidget(widget);
                    return State.CHAIN_ONLY;
                } else {
                    ShapeControlPoint prev = setSelected(null);
                    if (prev != null) {
                        return State.CHAIN_ONLY;
                    }
                }
            }
            return super.mousePressed(widget, event);
        }
    }

    private ShapeControlPoint setSelected(ShapeControlPoint pt) {
        ShapeControlPoint old = lkp.lookup(ShapeControlPoint.class);
        if (!Objects.equals(old, pt)) {
            if (pt == null) {
                lkp.updateLookups(shapesLookup);
                repaint(old);
            } else {
                Lookup nue = Lookups.fixed(pt, pt.owner());
                lkp.updateLookups(shapesLookup, nue);
                if (old != null) {
                    repaint(old);
                }
                repaint(pt);
            }
        } else {
            old = null;
        }
        return old;
    }

    private boolean isSelected(ShapeControlPoint cp) {
        return Objects.equals(cp, lkp.lookup(ShapeControlPoint.class));
    }

    private ShapeControlPoint selected() {
        return lkp.lookup(ShapeControlPoint.class);
    }

    private double controlPointSize() {
        double factor = 1D / getScene().getZoomFactor();
        return renderingProperties.controlPointSize() * factor;
    }

    public void sync() {
        time("sync", () -> {
            prune(true, xPoints, (removeX, reAddX, removedPointsX) -> {
                prune(false, yPoints, (removeY, reAddY, removedPointsY) -> {
                    xPoints.removeIndices(removeX);
                    yPoints.removeIndices(removeY);
                    reAddX.addAll(reAddY);
                    removedPointsX.addAll(removedPointsY);
                    updateCachedControlPointInfo(reAddX, removedPointsX);
                });
            });
        });
    }

    public void shapeDeleted(ShapeElement el) {
        Rectangle2D.Double repaint = new Rectangle2D.Double();
        removeSomeControlPoints(el.getControlPointCount(), repaint, cp -> el.equals(cp.owner()));
    }

    public void shapeAdded(ShapeElement el) {
        Rectangle2D.Double repaint = new Rectangle2D.Double();
        shapeAdded(el, repaint);
        if (!repaint.isEmpty()) {
            repaint(repaint);
        }
    }

    public void controlPointDeleted(ShapeControlPoint cp) {
        EqPointDouble pt = reverseIndex.remove(cp);
        double x = pt == null ? cp.getX() : pt.x;
        double y = pt == null ? cp.getY() : pt.y;
        Set<ShapeControlPoint> xs = xPoints.get(x);
        if (xs != null) {
            if (xs.remove(cp)) {
                if (xs.isEmpty()) {
                    xPoints.remove(x);
                }
            }
        }
        Set<ShapeControlPoint> ys = yPoints.get(y);
        if (ys != null) {
            if (ys.remove(cp)) {
                if (ys.isEmpty()) {
                    yPoints.remove(y);
                }
            }
        }
        repaint(x, y);
    }

    private void removeSomeControlPoints(int ct, Rectangle2D.Double repaint, Predicate<ShapeControlPoint> test) {
        Set<ShapeControlPoint> toRemove = null;
        IntSet xRemove = null;
        IntSet yRemove = null;
        for (Map.Entry<ShapeControlPoint, EqPointDouble> e : reverseIndex.entrySet()) {
            ShapeControlPoint cp = e.getKey();
            if (test.test(cp)) {
                EqPointDouble pt = e.getValue();
                addToRect(repaint, pt);
                if (toRemove == null) {
                    toRemove = new HashSet<>(ct);
                }
                toRemove.add(cp);
                Set<ShapeControlPoint> xs = xPoints.get(pt.x);
                if (xs != null) {
                    xs.remove(e.getKey());
                    if (xs.isEmpty()) {
                        int ix = xPoints.indexOf(pt.x);
                        if (xRemove == null) {
                            xRemove = IntSet.create(ct);
                        }
                        if (ix >= 0) {
                            xRemove.add(ix);
                        }
                    }
                }
                Set<ShapeControlPoint> ys = xPoints.get(pt.y);
                if (ys != null) {
                    ys.remove(e.getKey());
                    if (ys.isEmpty()) {
                        int ix = yPoints.indexOf(pt.y);
                        if (yRemove == null) {
                            yRemove = IntSet.create(ct);
                        }
                        if (ix >= 0) {
                            yRemove.add(ix);
                        }
                    }
                }
            }
        }
        if (xRemove != null) {
            xPoints.removeIndices(xRemove);
        }
        if (yRemove != null) {
            yPoints.removeIndices(yRemove);
        }
        if (toRemove != null) {
            for (ShapeControlPoint rem : toRemove) {
                reverseIndex.remove(rem);
            }
        }
    }

    private final L l = new L();

    private boolean pendingSync;

    class L implements UIStateListener, Runnable {

        private final Set<ShapeElement> toSync = new HashSet<>();

        @Override
        public void onChange(UIState.Props prop, boolean val) {
            switch (prop) {
                case CONTROL_POINTS_VISIBLE:
                    if (val) {
                        System.out.println("control points vis");
                        done();
                    }
            }
        }

        private void done() {
            if (!toSync.isEmpty()) {
                pendingSync = true;
            }
            uiState.unlisten(this);
            listening = false;
            EventQueue.invokeLater(this);
        }

        @Override
        public void run() {
            pendingSync = false;
            for (ShapeElement el : toSync) {
                syncOne(el);
            }
            toSync.clear();
//            revalidate();
//            repaint();
        }

        private boolean listening;

        private void startListening(ShapeElement el) {
            toSync.add(el);
            if (!listening) {
                listening = true;
                uiState.listen(this);
            }
        }
    }

    @Override
    public boolean isValidated() {
        return super.isValidated();
    }

    public void syncOne(ShapeElement el) {
        if (!uiState.controlPointsVisible()) {
            l.startListening(el);
            return;
        }
        time("syncOne " + el.getName(), () -> {
            ShapeControlPoint[] pts = el.controlPoints(renderingProperties.controlPointSize(), this::controlPointMoved);
            Rectangle2D.Double repaint = new Rectangle2D.Double();

            removeSomeControlPoints(pts.length, repaint, cp -> {
                return el.equals(cp.owner()) && cp.index() >= pts.length;
            });

            DoubleSet toRemoveX = DoubleSet.create(30);
            DoubleSet toRemoveY = DoubleSet.create(30);
            DoubleMap<Set<ShapeControlPoint>> toAddX = DoubleMap.create(120);
            DoubleMap<Set<ShapeControlPoint>> toAddY = DoubleMap.create(120);
            for (ShapeControlPoint pt : pts) {
                double x = pt.getX();
                double y = pt.getY();
                addToRect(repaint, x, y);
                EqPointDouble cachedLocation = reverseIndex.get(pt);
                if (cachedLocation != null) {
                    addToRect(repaint, cachedLocation.x, cachedLocation.y);
                }
                addToRect(repaint, x, y);
                boolean absent = cachedLocation == null;
                if (absent) {
                    cachedLocation = addOneControlPoint(pt);
                    addToRect(repaint, x, y);
                } else {
                    boolean changed = absent
                            ? false
                            : x != cachedLocation.x
                            || y != cachedLocation.y;
                    if (changed) {
                        Set<ShapeControlPoint> xs = xPoints.get(cachedLocation.x);
                        if (xs != null) {
                            if (xs.remove(pt)) {
                                if (xs.isEmpty()) {
//                                    xPoints.remove(cachedLocation.x);
                                    toRemoveX.add(cachedLocation.x);
                                }
                            }
                        }
                        Set<ShapeControlPoint> ys = yPoints.get(cachedLocation.y);
                        if (ys != null) {
                            if (ys.remove(pt)) {
                                if (ys.isEmpty()) {
                                    toRemoveY.add(cachedLocation.y);
//                                    yPoints.remove(cachedLocation.y);
                                }
                            }
                        }
                        xs = xPoints.get(x);
                        if (xs == null) {
                            xs = new HashSet<>(pts.length);
                            toAddX.put(x, ys);
//                            xPoints.put(x, xs);
//                            toRemoveX.remove(x);
                        }
                        xs.add(pt);
                        ys = yPoints.get(y);
                        if (ys == null) {
                            ys = new HashSet<>(pts.length);
                            toAddY.put(y, ys);
//                            yPoints.put(y, ys);
//                            toRemoveY.remove(y);
                        }
                        ys.add(pt);
                        cachedLocation.x = x;
                        cachedLocation.y = y;
                    }
                }
            }
            System.out.println("REMOVE " + toRemoveX.size() + " Xs");
            System.out.println("REMOVE " + toRemoveY.size() + " Ys");
            // Bulk adds and removes are much cheaper - like 100x cheaper
            // to do these in one shot at the end of looping over the control
            // points
            // NOTE: It is critical that we do the removes FIRST, in the event
            // that we are removing a coordinate and adding back exactly the
            // same coordinate, BECAUSE the way DoubleMapImpl resolves conflicts
            // when duplicates are inserted is undefined (up to the order sort algorithm,
            // discovers the duplicate keys, really), so we could leave behind a
            // stale set and never add its replacement that contains data we need
            System.out.println("Old Sizes " + xPoints.size() + ", " + yPoints.size());
            xPoints.removeAll(toRemoveX);
            yPoints.removeAll(toRemoveY);
            System.out.println("Rem Sizes " + xPoints.size() + ", " + yPoints.size());

            System.out.println("ADD " + toAddX.size() + " Xs");
            System.out.println("ADD " + toAddY.size() + " Ys");
            xPoints.putAll(toAddX);
            yPoints.putAll(toAddY);
            System.out.println("New Sizes " + xPoints.size() + ", " + yPoints.size());
            if (!repaint.isEmpty()) {
                repaint(repaint);
            }
        });
    }

    private static void addToRect(Rectangle2D.Double repaintRect, Point2D pt) {
        addToRect(repaintRect, pt.getX(), pt.getY());
    }

    private static void addToRect(Rectangle2D.Double repaintRect, double x, double y) {
        if (repaintRect.isEmpty()) {
            repaintRect.x = x;
            repaintRect.y = y;
            repaintRect.width = repaintRect.height = 0.00001;
        } else {
            repaintRect.add(x, y);
        }
    }

    private void time(String what, Runnable r) {
        long then = System.currentTimeMillis();
        r.run();
        long elapsed = System.currentTimeMillis() - then;
        System.out.println(what + " took " + elapsed + "ms");
    }

    private void updateCachedControlPointInfo(Set<ShapeControlPoint> changedOrAdded, Set<ShapeControlPoint> removed) {
        double sz = controlPointSize();
        Rectangle currBounds = getClientArea();
        Rectangle2D.Double repaintRect = new Rectangle2D.Double();
        ShapeElement el = null;
        for (ShapeControlPoint cp : changedOrAdded) {
            ShapeElement own = cp.owner();
            if (own != el) {
                el = own;
                own.addToBounds(repaintRect);
            }
            EqPointDouble oldPoint = reverseIndex.get(cp);
            addOneControlPoint(cp);
            double newX = cp.getX();
            double newY = cp.getY();
            if (oldPoint != null) {
                addToRect(repaintRect, oldPoint.x - sz, oldPoint.y - sz);
                addToRect(repaintRect, oldPoint.x + sz, oldPoint.y + sz);
                oldPoint.setLocation(newX, newY);
            }
            addToRect(repaintRect, newX - sz, newY - sz);
            addToRect(repaintRect, newX + sz, newY + sz);
            if (oldPoint == null) {
                reverseIndex.put(cp, new EqPointDouble(newX, newY));
            }
        }
        for (ShapeControlPoint cp : removed) {
            EqPointDouble old = reverseIndex.remove(cp);
            double x, y;
            if (old != null) {
                x = old.x;
                y = old.y;
            } else {
                x = cp.getX();
                y = cp.getY();
            }
            addToRect(repaintRect, x - sz, y - sz);
            addToRect(repaintRect, x + sz, y + sz);
        }
        if (!repaintRect.isEmpty()) {
            Rectangle bds = repaintRect.getBounds();
            if (bds.x <= currBounds.x || bds.y <= currBounds.y || currBounds.x + currBounds.width <= repaintRect.width || currBounds.y + currBounds.height <= repaintRect.height) {
                revalidate();
            }
            repaint(repaintRect);
        }
    }

    private void repaint(ShapeControlPoint cp) {
        EqPointDouble p = reverseIndex.get(cp);
        if (p == null) {
            repaint(cp.getX(), cp.getY());
        } else {
            repaint(p.x, p.y);
        }
    }

    private void repaint(double cpx, double cpy) {
        double sz = controlPointSize();
        sz += renderingProperties.focusStrokeSize() * (1D / getScene().getZoomFactor());
        Rectangle2D.Double r = new Rectangle2D.Double();
        r.setFrameFromDiagonal(cpx - sz, cpy - sz, cpx + sz, cpy + sz);
        repaint(r);
    }

    private void repaint(Rectangle2D r) {
        if (!getClientArea().contains(r)) {
            revalidate();
        }
        JComponent view = getScene().getView();
        if (view != null) {
            Rectangle r1 = r.getBounds();
            r1 = super.convertLocalToScene(r1);
            r1 = getScene().convertSceneToView(r1);
//            System.out.println("REPAINT " + GeometryUtils.toString(r1));
            view.repaint(r1);
        }
    }

    private void prune(boolean isX, DoubleMap<Set<ShapeControlPoint>> pointMap, TriConsumer<IntSet, Set<ShapeControlPoint>, Set<ShapeControlPoint>> bc) {
        time("prune " + (isX ? "x" : "y"), () -> {
            ShapesCollection coll = shapesLookup.lookup(ShapesCollection.class);
            IntSet defunct = IntSet.create(pointMap.size());
            Set<ShapeControlPoint> reAdd = new HashSet<>(pointMap.size());
            Set<ShapeElement> removedShapes = new HashSet<>(coll.size());
            Set<ShapeControlPoint> removedPoints = new HashSet<>(pointMap.size());
            pointMap.forEach((int index, double value, Set<ShapeControlPoint> scps) -> {
                for (ShapeControlPoint scp : new HashSet<>(scps)) {
                    ShapeElement owner = scp.owner();
                    if (removedShapes.contains(owner) || !scp.isValid()) {
                        scps.remove(scp);
                        removedPoints.add(scp);
                        continue;
                    } else if (!coll.contains(owner)) {
                        removedShapes.add(owner);
                        scps.remove(scp);
                        removedPoints.add(scp);
                    }
                    double coord = isX ? scp.getX() : scp.getY();
                    if (coord != value) {
                        scps.remove(scp);
                        reAdd.add(scp);
                    }
                }
                if (scps.isEmpty()) {
                    defunct.add(index);
                }
            });
            bc.apply(defunct, reAdd, removedPoints);
        });
    }

    private void init() {
        ShapesCollection coll = shapesLookup.lookup(ShapesCollection.class);
        for (ShapeElement el : coll) {
            shapeAdded(el, null);
        }
    }

    public void shapeAdded(ShapeElement el, Rectangle2D.Double addTo) {
        ShapeControlPoint[] pts = el.controlPoints(controlPointSize(), this::controlPointMoved);
        for (ShapeControlPoint cp : pts) {
            EqPointDouble pt = addOneControlPoint(cp);
            if (addTo != null) {
                addToRect(addTo, cp.getX(), cp.getY());
                if (pt != null) {
                    addToRect(addTo, pt);
                }
            }
        }
    }

    private void controlPointMoved(ShapeControlPoint cp) {
        updateCachedControlPointInfo(Collections.singleton(cp), Collections.emptySet());
    }

    private EqPointDouble addOneControlPoint(ShapeControlPoint cp) {
        double x = cp.getX();
        double y = cp.getY();
        Set<ShapeControlPoint> scpsX = xPoints.get(x);
        if (scpsX == null) {
            scpsX = new HashSet<>(6);
            xPoints.put(x, scpsX);
        }
        Set<ShapeControlPoint> scpsY = yPoints.get(y);
        if (scpsY == null) {
            scpsY = new HashSet<>(6);
            yPoints.put(y, scpsY);
        }
        scpsX.add(cp);
        scpsY.add(cp);
        return reverseIndex.put(cp, new EqPointDouble(x, y));
    }

    private boolean shouldPaintControlPoints(ShapeElement entry) {
        if (uiState.selectedShapeControlPointsVisible() && !selection.lookupAll(ShapeElement.class).contains(entry)) {
            return false;
        }
        return true;
    }

    private final Cells cells = new Cells();

    @Override
    protected void paintWidget() {
//        time("paint", this::doPaintWidget);
        if (pendingSync) {
            return;
        }
        doPaintWidget();
    }

    private void doPaintWidget() {
        if (!uiState.controlPointsVisible()) {
            return;
        }
        Graphics2D g = getGraphics();
        Rectangle clip = g.getClipBounds();
        if (clip == null) {
            clip = getClientArea();
        }
        double cpSize = (renderingProperties.controlPointSize() + renderingProperties.selectionStrokeSize())
                * 1.5;

        Set<ShapeControlPoint> cps = findForRegion(clip.x, clip.y,
                clip.x + clip.width, clip.y + clip.height);
//        System.out.println("paint " + cps.size() + " points");
        ShapeControlPoint sel = selected();
        long id = sel == null ? Long.MIN_VALUE : sel.owner().id();
        int ix = sel == null ? Integer.MIN_VALUE : sel.index();
        double zoom = getScene().getZoomFactor();
        double sz = controlPointSize();
        circ.setRadius(sz / 2);
        Rectangle sceneClip = super.convertLocalToScene(clip);
        cells.run(sceneClip, cpSize, zoom, () -> {
            ObjectState state = ObjectState.createNormal();
            if (this.getState().isFocused()) {
                state = state.deriveSelected(true);
            }
            List<ShapeControlPoint> lowerPriority = new ArrayList<>(cps.size());
            for (ShapeControlPoint cp : cps) {
                if (shouldPaintControlPoints(cp.owner())) {
                    if (cp.isVirtual()) {
                        lowerPriority.add(cp);
                        continue;
                    }
                    paintOne(cp, ix, id, g, state, zoom);
                }
            }
            for (ShapeControlPoint virtual : lowerPriority) {
                paintOne(virtual, ix, id, g, state, zoom);
            }
        });
    }

    private void paintOne(ShapeControlPoint cp, int ix, long id, Graphics2D g, ObjectState state, double zoom) {
        boolean selected;
        double x, y;
        EqPointDouble pt = reverseIndex.get(cp);
        if (pt != null) {
            x = pt.x;
            y = pt.y;
        } else {
            x = cp.getX();
            y = cp.getY();
        }
        selected = ix == cp.index() && id == cp.owner().id();
        boolean avail = cells.occupy(x, y);
        if (selected || avail) {
            paintOneControlPoint(g, cells.bounds(), cp, selected, state, zoom);
        }
    }

    private void paintDecorations(Graphics2D g, ObjectState state, Shape shape) {
        if (!uiState.focusDecorationsPainted()) {
            return;
        }
        DecorationController decorations = renderingProperties.decorationController();
        if (state.isFocused()) {
            decorations.setupFocusedPainting(getScene().getZoomFactor(), g, g1 -> {
                g1.draw(shape);
            });
        } else if (state.isSelected()) {
            decorations.setupSelectedPainting(getScene().getZoomFactor(), g, g1 -> {
                g1.draw(shape);
            });
        }
    }

    private void paintOneControlPoint(Graphics2D g, Rectangle clip, ShapeControlPoint cp, boolean selected, ObjectState state, double zoom) {
        EqPointDouble cached = reverseIndex.get(cp);
        double x, y;
        if (cached != null) {
            x = cached.x;
            y = cached.y;
        } else {
            x = cp.getX();
            y = cp.getY();
        }
        if (selected) {
            state = state.deriveObjectFocused(true);
        }
        ControlPointKind kind = cp.kind();
        Shape sh = renderingProperties.shapeForControlPoint(x, y, kind, true, false, zoom, false, state);
//        clip = convertLocalToScene(clip);
//        clip = getScene().convertSceneToView(clip);
//        boolean isHit = g.hit(clip, sh, true);
//        if (!isHit) {
//            System.out.println("non hit");
//            return;
//        }
        paintConnectorLine(g, cp);
        Paint fill = renderingProperties.fillForControlPoint(kind, state);
        g.setPaint(fill);
        g.fill(sh);
        Paint draw = renderingProperties.drawForControlPoint(kind, state);
        if (draw != null) {
            g.setPaint(draw);
            g.setStroke(renderingProperties.strokeForControlPoint(zoom));
            g.draw(sh);
        }
        paintDecorations(g, state, sh);
    }
    private static final Line2D.Double scratchLine = new Line2D.Double();

    private void paintConnectorLine(Graphics2D g, ShapeControlPoint pt) {
        if (!uiState.connectorLinesVisible()) {
            return;
        }
        Line2D line = configureConnectorLine(pt);
        if (line != null) {
            g.setStroke(renderingProperties.strokeForControlPointConnectorLine(getScene().getZoomFactor()));
            g.setPaint(renderingProperties.colorForControlPointConnectorLine());
            g.draw(line);
        }
    }

    private Line2D.Double configureConnectorLine(ShapeControlPoint c) {
        if (c.isVirtual() && c.kind().isSplinePoint()) {
            ShapeControlPoint[] sibs = c.family();
            ShapeControlPoint physicalSibling = null;
            for (int i = c.index() + 1; i < sibs.length; i++) {
                ShapeControlPoint sib = sibs[i];
                if (!sib.isVirtual()) {
                    physicalSibling = sib;
                    break;
                }
            }
            if (physicalSibling != null) {
                scratchLine.setLine(c.getX(), c.getY(), physicalSibling.getX(), physicalSibling.getY());
                return scratchLine;
            }
        }
        return null;
    }

    @Override
    protected Rectangle calculateClientArea() {
//        if (true) {
//            JComponent v = getScene().getView();
//            if (v != null) {
//                Rectangle r = v.getBounds();
//                if (!r.isEmpty()) {
//                    r = getScene().convertViewToScene(r);
//                    return r;
//                }
//            }
//        }
        if (xPoints.size() == 0 || yPoints.size() == 0) {
            return new Rectangle();
        }
        Rectangle2D.Double area = new Rectangle2D.Double();
        double leastX = xPoints.keySet().least();
        double greatestX = xPoints.keySet().greatest();
        double leastY = yPoints.keySet().least();
        double greatestY = yPoints.keySet().greatest();
        double sz = controlPointSize();
        area.add(leastX - (sz / 2D), leastY - (sz / 2D));
        area.add(greatestX + (sz / 2D), greatestY + (sz / 2D));
        Rectangle r = area.getBounds();
        JComponent view = getScene().getView();
        if (view != null) {
            Rectangle viewBounds = view.getBounds();
            viewBounds.x = 0;
            viewBounds.y = 0;
            viewBounds = getScene().convertViewToScene(viewBounds);
            viewBounds = convertSceneToLocal(viewBounds);
            r.add(viewBounds);
        }
        return r;
    }

    @Override
    protected void notifyAdded() {
        getScene().addSceneListener(sl);
    }

    @Override
    protected void notifyRemoved() {
        getScene().removeSceneListener(sl);
    }

    SceneListener sl = new SceneListener() {
        @Override
        public void sceneRepaint() {
        }

        @Override
        public void sceneValidating() {

        }

        @Override
        public void sceneValidated() {
            Rectangle r = getClientArea();
            JComponent view = getScene().getView();
            if (view != null) {
                Rectangle viewBounds = view.getBounds();
                viewBounds.x = 0;
                viewBounds.y = 0;
                getScene().convertViewToScene(viewBounds);
                viewBounds = convertSceneToLocal(viewBounds);
                if (viewBounds.equals(r)) {
                    // Ugly, but we need our bounds to always enclose
                    // the ENTIRE scene, so we can work in scene coordinates
                    // and drags don't stop working outside of the existing
                    // rectangle bounding shown points
                    EventQueue.invokeLater(() -> {
                        revalidate();
                        getScene().validate();
                    });
                }
            }
        }
    };

    @Override
    public boolean isHitAt(Point localLocation) {
        if (dragging) {
            return true;
        }
        return hit(localLocation.x, localLocation.y, false);
    }

    private boolean hit(Point2D p, boolean forClick) {
        return hit(p.getX(), p.getY(), forClick);
    }

    private boolean hit(double x, double y, boolean forClick) {
        if (!cells.wasOccupied(x, y)) {
            return false;
        }
        double sz = controlPointSize();
        if (forClick) {
            sz += 2;
        }
        double minX = x - sz;
        double maxX = x + sz;
        double minY = y - sz;
        double maxY = y + sz;

        Set<ShapeControlPoint> candidates = findForRegion(minX, minY, maxX, maxY);

//        System.out.println("FOUND " + candidates.size() + " for region "
//                + minX + ", " + minY + ", " + maxX + ", " + maxY);
        for (ShapeControlPoint cp : candidates) {
//            System.out.println("  TRY " + cp.getX() + ", " + cp.getY() + " " + cp);
            if (cp.hit(x, y)) {
                return true;
            }
        }
//        return !findForRegion(minX, minY, maxX, maxY).isEmpty();
        return false;
    }

    private ShapeControlPoint findForPoint(Point2D pt) {
        return findForPoint(pt.getX(), pt.getY());
    }

    private ShapeControlPoint findForPoint(double x, double y) {
        double cpSize = controlPointSize();
        Set<ShapeControlPoint> candidates = findForRegion(x - cpSize, y - cpSize, x + cpSize, y + cpSize);
        ShapeControlPoint best = null;
        double dist = Double.MAX_VALUE;
        for (ShapeControlPoint cp : candidates) {
            if (cp.hit(x, y)) {
                double d = cp.distance(x, y);
                if (d < dist) {
                    dist = d;
                    best = cp;
                }
            }
        }
        return best;
    }

    private Set<ShapeControlPoint> findForRegion(double minX, double minY, double maxX, double maxY) {
        int minXKey = xPoints.keySet().nearestIndexTo(minX, Bias.FORWARD);
        if (minXKey < 0) {
            return Collections.emptySet();
        }
        int minYKey = yPoints.keySet().nearestIndexTo(minY, Bias.FORWARD);
        if (minYKey < 0) {
            return Collections.emptySet();
        }
        int maxXKey = xPoints.keySet().nearestIndexTo(maxX, Bias.BACKWARD);
        if (maxXKey < 0) {
            return Collections.emptySet();
        }
        int maxYKey = yPoints.keySet().nearestIndexTo(maxY, Bias.BACKWARD);
        if (maxYKey < 0) {
            return Collections.emptySet();
        }

        // Use a TreeSet here to try to keep some consistency in which points
        // get painted (still dependent on the clip rectangle therefore what
        // arguments get passed to this method, so imperfect)
        Set<ShapeControlPoint> xps = new TreeSet<>();
        Set<ShapeControlPoint> yps = new TreeSet<>();
        for (int xk = minXKey; xk <= maxXKey; xk++) {
            Set<ShapeControlPoint> curr = xPoints.valueAt(xk);
            if (curr != null) {
                xps.addAll(curr);
            }
        }
        for (int yk = minYKey; yk <= maxYKey; yk++) {
            Set<ShapeControlPoint> curr = yPoints.valueAt(yk);
            if (curr != null) {
                yps.addAll(curr);
            }
        }
        xps.retainAll(yps);
        return xps;
    }

    @Override
    protected boolean isRepaintRequiredForRevalidating() {
        return false;
    }

    private static final Cursor CP_CURSOR = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private static final Cursor DEF_CURSOR = Cursor.getDefaultCursor();

    @Override
    protected Cursor getCursorAt(Point localLocation) {
        if (isHitAt(localLocation)) {
            return CP_CURSOR;
        } else {
            return DEF_CURSOR;
        }
    }

    @Override
    public Lookup getLookup() {
        return lkp;
    }

    @Override
    protected Graphics2D getGraphics() {
        Graphics2D result = super.getGraphics();
        GraphicsUtils.setHighQualityRenderingHints(result);
        return result;
    }

    ShapeControlPoint temp;
    EqPointDouble pointAtDragStart;

    public void onBeginDrag(ShapeControlPoint temp) {
        this.temp = temp;
        setDragging(true);
//        revalidate();
        EqPointDouble pt = reverseIndex.get(temp);
        if (pt != null) {
            pointAtDragStart = pt;
            controlPointMoved(temp);
        }
        repaint(temp);
    }

    public void onEndDrag(boolean cancel) {
        ShapeControlPoint cp = this.temp;
        EqPointDouble atStart = pointAtDragStart;
        pointAtDragStart = null;
        this.temp = null;
        setDragging(false);
        if (cancel) {
            cp.set(atStart.x, atStart.y);
        }
        controlPointMoved(cp);
    }

    private boolean dragging;

    void setDragging(boolean dragging) {
        if (dragging != this.dragging) {
            this.dragging = dragging;
            repaint();
        } else {
            throw new IllegalStateException(
                    "Set dragging called asymmetrically - " + dragging);
        }
    }

    void onDrag(Point2D current) {
        if (temp != null) {
            controlPointMoved(temp);
        }
    }
}
