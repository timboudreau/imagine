package org.imagine.vector.editor.ui.tools.widget;

import com.mastfrog.util.collections.CollectionUtils;
import java.awt.BasicStroke;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import javax.swing.JComponent;
import net.java.dev.imagine.api.vector.design.ControlPointKind;
import net.java.dev.imagine.api.vector.design.ShapeNames;
import net.java.dev.imagine.api.vector.elements.CircleWrapper;
import net.java.dev.imagine.api.vector.elements.RhombusWrapper;
import org.imagine.geometry.Circle;
import org.imagine.geometry.EqPointDouble;
import org.imagine.geometry.util.GeometryStrings;
import org.imagine.utils.java2d.GraphicsUtils;
import org.imagine.vector.editor.ui.spi.ShapeControlPoint;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import org.imagine.vector.editor.ui.tools.MutableProxyLookup;
import org.imagine.vector.editor.ui.tools.widget.actions.DragHandler;
import org.imagine.vector.editor.ui.tools.widget.actions.RotationHandler;
import org.imagine.vector.editor.ui.tools.widget.collections.CoordinateMap;
import org.imagine.vector.editor.ui.tools.widget.collections.CoordinateMapModifier;
import org.imagine.vector.editor.ui.tools.widget.collections.CoordinateMapPartitioned;
import org.imagine.vector.editor.ui.tools.widget.collections.Mover;
import org.imagine.vector.editor.ui.tools.widget.painting.DecorationController;
import org.imagine.vector.editor.ui.tools.widget.painting.DesignerProperties;
import org.imagine.vector.editor.ui.tools.widget.util.UIState;
import org.imagine.vector.editor.ui.tools.widget.util.UIState.UIStateListener;
import org.imagine.vector.editor.ui.tools.widget.util.ViewL;
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

    private final CoordinateMap<Set<ShapeControlPoint>> points
            = new CoordinateMapPartitioned(128);
//    private final CoordinateMap<Set<ShapeControlPoint>> points
//            = new CoordinateMapPartitioned(512, (x, y) -> {
//                return new DirtSimpleCoordinateMap(x, y, 512 / 4);
//            });

    private final Map<ShapeControlPoint, EqPointDouble> reverseIndex = new HashMap<>();
    private final Circle circ = new Circle(0, 0, 1);
    private final DesignerProperties renderingProperties;
    private final UIState uiState;
    private final Lookup selection;

    public ManyControlPointsWidget(Scene scene, ShapesCollection coll,
            DesignerProperties renderingProperties, UIState uiState, Lookup selection,
            DragHandler drag, RotationHandler rotate) {
        super(scene);
        this.renderingProperties = renderingProperties;
        this.uiState = uiState;
        shapesLookup = Lookups.fixed(coll, this, drag, rotate);
        this.selection = selection;
        init();
        lkp.updateLookups(shapesLookup);
        getActions().addAction(new Clicks());
    }

    class Clicks extends WidgetAction.Adapter {

        @Override
        public State mouseMoved(Widget widget, WidgetMouseEvent event) {
            Point p = event.getPoint();
            if (cells.wasOccupied(p.x, p.y)) {
                Set<ShapeControlPoint> cps = points.nearestValueTo(p.x, p.y, renderingProperties.controlPointSize());
                if (cps != null && !cps.isEmpty()) {
                    ShapeControlPoint cp = cps.iterator().next();
                    String info = cp.kind().toString() + " "
                            + cp.index() + " @ "
                            + GeometryStrings.toString(cp.getX(), cp.getY())
                            + " of " + ShapeNames.infoString(cp.getPrimitive());
                    if (!cp.isValid()) {
                        info = "INVALID " + info;
                    }
                    setToolTipText(info);
                } else {
                    setToolTipText(null);
                }
            } else {
                setToolTipText(null);
            }
            return super.mouseMoved(widget, event);
        }

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
        return Math.ceil(rawControlPointSize());
    }

    private double rawControlPointSize() {
        double factor = 1D / getScene().getZoomFactor();
        return renderingProperties.controlPointSize() * factor;
    }

    private double focusControlPointSize() {
        return Math.ceil(rawFocusControlPointSize());
    }

    private double rawFocusControlPointSize() {
        double factor = 1D / getScene().getZoomFactor();
        return renderingProperties.focusedControlPointSize() * factor;
    }

    public void sync() {
        time("sync", () -> {
            prune();
        });
    }

    private final Rectangle2D.Double repaintScratchRect = new Rectangle2D.Double();

    private Rectangle2D.Double repaintScratchRect() {
        repaintScratchRect.width = 0;
        repaintScratchRect.height = 0;
        return repaintScratchRect;
    }

    public void shapeDeleted(ShapeElement el) {
        Rectangle2D.Double repaint = repaintScratchRect();
        points.conditionallyRemove(cps -> {
            Set<ShapeControlPoint> toRemove = new HashSet<>(20);
            for (Iterator<ShapeControlPoint> it = cps.iterator(); it.hasNext();) {
                ShapeControlPoint scp = it.next();
                if (scp.owner().id() == el.id()) {
//                    it.remove();
                    toRemove.add(scp);
                    reverseIndex.remove(scp);
                }
            }
            cps.removeAll(toRemove);
            return cps.isEmpty();
        });
        repaint(repaint);
    }

    public void shapeAdded(ShapeElement el) {
        Rectangle2D.Double repaint = repaintScratchRect();
        shapeAdded(el, repaint);
        if (!repaint.isEmpty()) {
            repaint(repaint);
        }
    }

    public void controlPointDeleted(ShapeControlPoint cp) {
        EqPointDouble pt = reverseIndex.remove(cp);
        Set<ShapeControlPoint> set = points.get(pt.x, pt.y);
        if (set != null) {
            set.remove(cp);
        }
        repaint(pt.x, pt.y);
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
            MO mo = new MO();
            CoordinateMapModifier<Set<ShapeControlPoint>> mod = points.modifier(mo);
            Rectangle2D.Double repaint = repaintScratchRect();
            el.addToBounds(repaint);
            ShapeControlPoint[] pts = el.controlPoints(renderingProperties.controlPointSize(), this::controlPointMoved);
            Set<ShapeControlPoint> currentPoints = new HashSet<>();
            for (ShapeControlPoint p : pts) {
                currentPoints.add(p);
                EqPointDouble curr = reverseIndex.get(p);
                if (curr == null) {
                    Set<ShapeControlPoint> set = points.get(p.getX(), p.getY());
                    if (set != null) {
                        set.add(p);
                    } else {
                        Set<ShapeControlPoint> newSet = TinySets.of(p);
                        mod.add(p.getX(), p.getY(), newSet);
                        reverseIndex.put(p, new EqPointDouble(p.getX(), p.getY()));
                    }
                    addPointBoundsToRect(repaint, p.getX(), p.getY());
                } else {
                    double x = p.getX();
                    double y = p.getY();
                    if (x != curr.x || y != curr.y) {
                        reverseIndex.put(p, new EqPointDouble(x, y));
                        addPointBoundsToRect(repaint, x, y);
                        addPointBoundsToRect(repaint, curr.x, curr.y);
                        Set<ShapeControlPoint> existing
                                = points.get(curr.x, curr.y);
                        if (existing != null) {
                            mo.toMove.add(p);
                            mod.move(curr.x, curr.y, x, y);
                        } else {
                            Set<ShapeControlPoint> scp = TinySets.of(p);
                            mod.add(x, y, scp);
                        }
                    } else {
                        addPointBoundsToRect(repaint, x, y);
                    }
                }
            }
            Set<ShapeControlPoint> defunct = new HashSet<>();
            reverseIndex.forEach((scp, thePoint) -> {
                if (el.equals(scp.owner())) {
                    if (!currentPoints.contains(scp)) {
                        addPointBoundsToRect(repaint, thePoint);
                        defunct.add(scp);
                        Set<ShapeControlPoint> currPts = points.get(thePoint.x, thePoint.y);
                        if (currPts == null) {
                            thePoint = new EqPointDouble(scp.getX(), scp.getY());
                            currPts = points.get(thePoint.x, thePoint.y);
                            if (currPts != null && !currPts.isEmpty() && currPts.contains(scp)) {
                                addPointBoundsToRect(repaint, thePoint);
                            }
                        }
                        if (currPts != null) {
//                            System.out.println("remove defunct " + scp);
                            if (currPts.size() > 1) {
                                currPts.remove(scp);
                            } else if (currPts.contains(scp)) {
                                mod.remove(scp.getX(), scp.getY());
                            }
                        }
                    }
                }
            });
            if (!defunct.isEmpty()) {
                defunct.forEach(reverseIndex::remove);
            }

            mod.commit();
            revalidate();
            repaint(repaint);
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

    private void addPointBoundsToRect(Rectangle2D.Double repaintRect, Point2D pt) {
        addPointBoundsToRect(repaintRect, pt.getX(), pt.getY());
    }

    private void addPointBoundsToRect(Rectangle2D.Double repaintRect, double x, double y) {
        double sz = focusControlPointSize();
//        sz += renderingProperties.focusStrokeSize() * (1D / getScene().getZoomFactor());
        sz += lastControlPointStrokeSize * getScene().getZoomFactor();
        if (repaintRect.isEmpty()) {
            repaintRect.setFrame(x - sz, y - sz,
                    x + sz, y + sz);
        } else {
            repaintRect.add(x - sz, y - sz);
            repaintRect.add(x + sz, y + sz);
        }
    }

    private void time(String what, Runnable r) {
        long then = System.currentTimeMillis();
        r.run();
        long elapsed = System.currentTimeMillis() - then;
        System.out.println(what + " took " + elapsed + "ms");
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
        double sz = focusControlPointSize();
//        sz += renderingProperties.focusStrokeSize() * (getScene().getZoomFactor());
        sz += lastControlPointStrokeSize * getScene().getZoomFactor();
        repaintScratchRect.setFrameFromDiagonal(cpx - sz, cpy - sz, cpx + sz, cpy + sz);
        repaint(repaintScratchRect);
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

    static class MO implements Mover<Set<ShapeControlPoint>> {

        private final Set<ShapeControlPoint> toMove = TinySets.empty();

        public MO() {

        }

        public MO(ShapeControlPoint cp) {
            toMove.add(cp);
        }

        @Override
        public Set<ShapeControlPoint> coalesce(Set<ShapeControlPoint> oldValue, Set<ShapeControlPoint> intoValue, BiConsumer<Set<ShapeControlPoint>, Set<ShapeControlPoint>> oldNewConsumer) {
            if (intoValue == null) {
                intoValue = TinySets.empty();
            }
            if (oldValue != null && oldValue.size() == 1) {
                ShapeControlPoint scp = oldValue.iterator().next();
                if (toMove.contains(scp)) {
                    oldValue.remove(scp);
                    if (intoValue.isEmpty()) {
                        intoValue = TinySets.of(scp);
                    } else {
                        intoValue.add(scp);
                    }
                }
                oldNewConsumer.accept(oldValue, intoValue);
                return intoValue;
            }
            Set<ShapeControlPoint> intersection = CollectionUtils.intersection(oldValue, toMove);
            if (oldValue != null) {
                oldValue.removeAll(intersection);
            }
            intoValue.addAll(intoValue);
            oldNewConsumer.accept(oldValue, intoValue);
            return intoValue;
        }
    }

    private void prune() {
        time("prune ", () -> {
            MO mo = new MO();
            CoordinateMapModifier<Set<ShapeControlPoint>> mod = points.modifier(mo);
            ShapesCollection coll = shapesLookup.lookup(ShapesCollection.class);
            Set<ShapeElement> removedShapes = new HashSet<>(coll.size());
            Set<ShapeElement> retainedShapes = new HashSet<>();
            Rectangle2D.Double repaint = repaintScratchRect();

            points.visitAll((double x, double y, Set<ShapeControlPoint> val) -> {
                for (Iterator<ShapeControlPoint> it = val.iterator(); it.hasNext();) {
                    ShapeControlPoint scp = it.next();
                    ShapeElement owner = scp.owner();
                    boolean isRemoved = removedShapes.contains(owner);
                    if (!isRemoved && !retainedShapes.contains(owner)) {
                        if (coll.indexOf(owner) < 0) {
                            owner.addToBounds(repaint);
                            removedShapes.add(owner);
                            isRemoved = true;
                        } else {
                            retainedShapes.add(owner);
                        }
                    }
                    isRemoved |= !scp.isValid();
                    if (isRemoved) {
                        it.remove();
                        EqPointDouble pt = reverseIndex.get(scp);
                        if (pt != null) {
                            addPointBoundsToRect(repaint, pt);
                            if (val.isEmpty()) {
                                mod.remove(pt.getX(), pt.getY());
                            }
                            reverseIndex.remove(scp);
                        }
                    } else {
                        double px = scp.getX();
                        double py = scp.getY();
                        EqPointDouble loc = reverseIndex.get(scp);
                        if (loc.x != px || loc.y != py) {
                            addPointBoundsToRect(repaint, loc);
                            addPointBoundsToRect(repaint, px, py);
                            reverseIndex.put(scp, new EqPointDouble(px, py));
                            mod.move(loc.x, loc.y, px, py);
                            mo.toMove.add(scp);
                        }
                    }
                }
            });
            for (ShapeElement el : coll) {
                if (!retainedShapes.contains(el)) {
                    el.addToBounds(repaint);
                    ShapeControlPoint[] pts = el.controlPoints(controlPointSize(), this::controlPointMoved);
                    for (int i = 0; i < pts.length; i++) {
                        double px = pts[i].getX();
                        double py = pts[i].getY();
                        EqPointDouble nue = new EqPointDouble(px, py);
                        Set<ShapeControlPoint> set = points.get(px, py);
                        if (set == null) {
                            set = TinySets.empty();
                            mod.add(px, py, set);
                        }
                        set.add(pts[i]);
                        addPointBoundsToRect(repaint, nue);
                        reverseIndex.put(pts[i], nue);
                    }
                }
            }
//            points.conditionallyRemove(Set::isEmpty);
            mod.commit();
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
                addPointBoundsToRect(addTo, cp.getX(), cp.getY());
                if (pt != null) {
                    addPointBoundsToRect(addTo, pt);
                }
            }
        }
    }

    private void controlPointMoved(ShapeControlPoint cp) {
        Rectangle2D.Double repaint = repaintScratchRect();
        cp.owner().addToBounds(repaint);
        if (cp.owner().item().is(CircleWrapper.class) || cp.owner().item().is(RhombusWrapper.class)) {
            // Moving a control point can invalidate other ones - just sync it all up
            syncAllControlPoints(cp.owner(), cp, repaint);
        } else {
            controlPointMoved(cp, new HashSet<>(cp.family().length), repaint, false);
        }
        if (!repaint.isEmpty()) {
            repaint(repaint);
        }
    }

    private void syncAllControlPoints(ShapeElement el, ShapeControlPoint initiator, Rectangle2D.Double repaint) {
        for (ShapeControlPoint cp : initiator.family()) {
            EqPointDouble newLoc = new EqPointDouble(cp.getX(), cp.getY());
            EqPointDouble oldLoc = reverseIndex.get(cp);
            boolean eq = oldLoc.exactlyEqual(newLoc);
            if (oldLoc != null || !eq) {
                if (!eq && points.contains(oldLoc.x, oldLoc.y)) {
                    points.moveData(oldLoc.x, oldLoc.y, newLoc.x, newLoc.y, new MO(cp));
                } else if (!eq) {
                    points.put(newLoc.x, newLoc.y, TinySets.of(cp), (s1, s2) -> {
                        if (s2 == null) {
                            s2 = TinySets.empty();
                        }
                        s2.add(cp);
                        s2.addAll(s1);
                        return s2;
                    });
                }
                reverseIndex.put(cp, newLoc);
                addPointBoundsToRect(repaint, oldLoc);
                addPointBoundsToRect(repaint, newLoc);
            }
        }
    }

    private void controlPointMoved(ShapeControlPoint cp, Set<ShapeControlPoint> handled,
            Rectangle2D.Double repaint, boolean isRefresh) {
        EqPointDouble oldLoc = reverseIndex.get(cp);
        assert oldLoc != null : "Control point " + cp + " is not known";
        EqPointDouble newLoc = new EqPointDouble(cp.getX(), cp.getY());
        handled.add(cp);
        if (isRefresh || !oldLoc.exactlyEqual(newLoc)) {
            if (temp == null || cp.owner() != temp.owner()) {
                if (points.contains(oldLoc.x, oldLoc.y)) {
                    points.moveData(oldLoc.x, oldLoc.y, newLoc.x, newLoc.y, new MO(cp));
                } else {
                    points.put(newLoc.x, newLoc.y, TinySets.of(cp), (s1, s2) -> {
                        if (s2 == null) {
                            s2 = TinySets.empty();
                        }
                        s2.add(cp);
                        s2.addAll(s1);
                        return s2;
                    });
                }
            }
            reverseIndex.put(cp, newLoc);
            Set<ShapeControlPoint> related = new HashSet<>(Arrays.asList(cp.family()));
            related.removeAll(handled);
            for (ShapeControlPoint sibling : related) {
                ControlPointKind k = sibling.kind();
                switch (k) {
                    case OTHER:
                    case RADIUS:
                    case EDGE_HANDLE:
                        controlPointMoved(sibling, handled, repaint, true);
                        break;
                }
            }
            addPointBoundsToRect(repaint, oldLoc);
            addPointBoundsToRect(repaint, newLoc);
        }
    }

    private EqPointDouble addOneControlPoint(ShapeControlPoint cp) {
        double x = cp.getX();
        double y = cp.getY();
        points.put(x, y, TinySets.of(cp), (Set<ShapeControlPoint> a, Set<ShapeControlPoint> b) -> {
            if (a != null && !a.isEmpty()) {
                a.addAll(b);
                return a;
            }
            return b;
        });
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
        Rectangle r = g.getClipBounds();
        if (r == null) {
            r = getClientArea();
        }
        double cpSize = (renderingProperties.controlPointSize() + renderingProperties.selectionStrokeSize()) /* * 1.5 */;

        Rectangle clip = r;
        clip.height = Math.max(clip.height, (int) Math.ceil(cpSize));
        clip.width = Math.max(clip.width, (int) Math.ceil(cpSize));
//        time("paint", () -> {
        Set<ShapeControlPoint> cps = findForRegion(clip.x, clip.y,
                clip.x + clip.width, clip.y + clip.height);
//        System.out.println("paint " + cps.size() + " points");
        ShapeControlPoint sel = selected();
        long id = sel == null ? Long.MIN_VALUE : sel.owner().id();
        int ix = sel == null ? Integer.MIN_VALUE : sel.index();
        double zoom = getScene().getZoomFactor();
        lastControlPointStrokeSize = renderingProperties.strokeForControlPoint(zoom).getLineWidth();
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

    private float lastControlPointStrokeSize;
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
        Rectangle r = points.bounds();
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

    private Point2D maybeReplacePoint(Point p) {
        // There may be programmatic calls to isHitAt with a java.awt.Point
        // which we don't want to substitute the current mouse position,
        // so we test if the last mouse event's point is close enough
        // to the passed on eto be believably from the same event the
        // point we're called with is
        EqPointDouble scenePoint = ViewL.lastPoint2D(this);
        if (scenePoint.distance(p) < 2) {
            return scenePoint;
        }
        return p;
    }

    @Override
    public boolean isHitAt(Point localLocation) {
        if (dragging) {
            return true;
        }
        Point2D rep = maybeReplacePoint(localLocation);
        return isHitAt(rep);
    }

    public boolean isHitAt(Point2D localLocation) {
        if (dragging) {
            return true;
        }
        return hit(localLocation.getX(), localLocation.getY(), false);
    }

    private boolean hit(Point2D p, boolean forClick) {
        return hit(p.getX(), p.getY(), forClick);
    }

    private boolean hit(double x, double y, boolean forClick) {
//        if (cells.wasInLastBounds(x, y) && !cells.wasOccupied(x, y)) {
//            return false;
//        }
        double sz = rawControlPointSize();
        if (forClick) {
//            sz += 2D * (1D / getScene().getZoomFactor());
        }
        double minX = x - sz;
        double maxX = x + sz;
        double minY = y - sz;
        double maxY = y + sz;

        Set<ShapeControlPoint> candidates = findForRegion(minX, minY, maxX, maxY);

        return candidates.size() > 0;
    }

    private ShapeControlPoint findForPoint(Point2D pt) {
        return findForPoint(pt.getX(), pt.getY());
    }

    private ShapeControlPoint findForPoint(double x, double y) {
        double cpSize = Math.max(1, controlPointSize());
        Set<ShapeControlPoint> candidates = findForRegion(
                x - cpSize, y - cpSize, x + cpSize, y + cpSize);

//        if (candidates.size() > 0) {
//            System.out.println(candidates.size() + " hit candidates");
//        }
        ShapeControlPoint best = null;
        double dist = Double.MAX_VALUE;
        for (ShapeControlPoint cp : candidates) {
//            if (cp.hit(x, y)) {
            double d = cp.distance(x, y);
            if (d < dist) {
                dist = d;
                best = cp;
            }
//            }
        }
        return best;
    }

    Set<ShapeControlPoint> paintSet = new HashSet<>();

    private Set<ShapeControlPoint> findForRegion(double minX, double minY, double maxX, double maxY) {
        if (true) {
            paintSet.clear();
            for (Map.Entry<ShapeControlPoint, EqPointDouble> e : reverseIndex.entrySet()) {
                EqPointDouble p = e.getValue();
                if (p.x >= minX && p.x < maxX && p.y >= minY && p.y < maxY) {
                    paintSet.add(e.getKey());
                }
            }
            return paintSet;
        }

        Set<ShapeControlPoint> result = new HashSet<>();
        points.valuesWithin(minX, minY, maxX, maxY, (double x, double y, Set<ShapeControlPoint> val) -> {
            int oldSize = result.size();
            result.addAll(ShapeControlPointRefiner.INSTANCE.refine(minX, maxX, minY, maxY, val));
            if (result.size() == oldSize && !val.isEmpty()) {
//                System.out.println("Refiner eliminated ALL of " + val.size());
            }
//            result.addAll(val);
        });
        Rectangle2D.Double b = new Rectangle2D.Double();
        b.setFrameFromDiagonal(minX, minY, maxX, maxY);
        if (result.isEmpty() && !paintSet.isEmpty()) {
//            System.out.println("Nothing for " + minX + "," + minY + " -> " + maxX + ", " + maxY
//                    + " should have found " + paintSet.size() + " items");
            Rectangle2D.Double r = new Rectangle2D.Double();
            r.setFrameFromDiagonal(minX, minY, maxX, maxY);
            CoordinateMap.debug(() -> {
                Set<ShapeControlPoint> near = points.nearestValueTo(r.getCenterX(), r.getCenterY(), r.width);
//                System.out.println("  Nearest " + near + " for " + r.addToBounds());
            });
//        } else {
//            System.out.println("Have result " + result);
        }
//        System.out.println("Found " + result.size() + " for region "
//                + GeometryUtils.toString(b));
        return result;
    }

    @Override
    protected boolean isRepaintRequiredForRevalidating() {
        return false;
    }

    private static final Cursor CP_CURSOR = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private static final Cursor DEF_CURSOR = Cursor.getDefaultCursor();

    @Override
    protected Cursor getCursorAt(Point localLocation) {
        Point2D viewLoc = ViewL.lastPoint2D(this);
        System.out.println("LOC / VIEWLOC " + localLocation + " -> " 
                + viewLoc + " viewl says " + ViewL.lastPoint(this)
        + " myLoc " + getLocation() + " myBounds " + getBounds());
        if (isHitAt(viewLoc)) {
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
            cacheLastBounds(temp.owner());
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
        oldShapeBounds.width = 0;
        oldShapeBounds.height = 0;
        if (cancel) {
            cp.set(atStart.x, atStart.y);
        }
        controlPointMoved(cp);
    }

    public void onBeginRotate(ShapeControlPoint temp) {
        onBeginDrag(temp);
    }

    public void onEndRotate(boolean cancel) {
        onEndDrag(cancel);
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

    private Rectangle2D.Double oldShapeBounds = new Rectangle2D.Double();

    private void cacheLastBounds(ShapeElement en) {
        double factor = 1D / getScene().getZoomFactor();
        oldShapeBounds.width = 0;
        oldShapeBounds.height = 0;
        en.addToBounds(oldShapeBounds);
        BasicStroke stroke = en.stroke();
        double add = stroke == null ? 0 : factor * stroke.getLineWidth();
        oldShapeBounds.x -= add / 2;
        oldShapeBounds.y -= add / 2;
        oldShapeBounds.width += add;
        oldShapeBounds.height += add;
    }

    void onDrag(Point2D current) {
        if (temp != null) {
            repaint(oldShapeBounds);
            controlPointMoved(temp);
            cacheLastBounds(temp.owner());
        }
    }

    void onRotate() {
        if (temp != null) {
            repaint(oldShapeBounds);
            Rectangle2D.Double xp = repaintScratchRect();
            syncAllControlPoints(temp.owner(), temp, xp);
            cacheLastBounds(temp.owner());
            oldShapeBounds.add(xp);
            repaint(oldShapeBounds);
        }
    }
}
