package org.imagine.vector.editor.ui.tools;

import com.mastfrog.util.collections.CollectionUtils;
import com.sun.glass.events.KeyEvent;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.dev.java.imagine.api.tool.aspects.snap.SnapPoints;
import net.dev.java.imagine.api.tool.aspects.snap.SnapPointsConsumer;
import net.dev.java.imagine.api.tool.aspects.snap.SnapPoint;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.design.ControlPoint;
import org.imagine.editor.api.Zoom;
import org.imagine.utils.Holder;
import org.imagine.vector.editor.ui.spi.LayerRenderingWidget;
import org.imagine.vector.editor.ui.spi.ShapeControlPoint;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.widget.LayerWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.paintui.widgetlayers.SetterResult;
import org.netbeans.paintui.widgetlayers.WidgetController;
import org.openide.util.Lookup;
import org.openide.util.WeakListeners;

/**
 *
 * @author Tim Boudreau
 */
class ShapeDesignToolWidget extends LayerRenderingWidget {

    private double opacity = 1;
    private final MutableProxyLookup lookup = new MutableProxyLookup();
    private final LayerWidget shapesLayer;
    private final LayerWidget controlPointsLayer;
    private final LayerWidget snapLinesLayer;

    private Map<ShapeElement, ShapeWidget> widgetForShape
            = new HashMap<>(50);
    private Map<ControlPoint, ControlPointWidget> controlPointWidgetForControlPoint = new HashMap<>(120);
    private Map<ShapeElement, Set<ControlPoint>> controlPointsForShape = new HashMap<>();

    private final ShapesCollection shapes;
    private final Holder<PaintParticipant.Repainter> repainter;
    private final WidgetController widgetController;
    private final WidgetController ctrllr;
    private final FocusAction focusAction;
    private final SnapLinesController snapLines;
    private final Supplier<SnapPoints> snapSupplier;

    public ShapeDesignToolWidget(Scene scene, ShapesCollection shapes,
            Lookup layerLookup, Holder<PaintParticipant.Repainter> repainter,
            WidgetController ctrllr) {
        super(scene);
        this.shapes = shapes;
        this.repainter = repainter;
        this.widgetController = ctrllr;
        this.ctrllr = ctrllr;
        shapesLayer = new LayerWidget(scene);
        controlPointsLayer = new LayerWidget(scene);
        snapLinesLayer = new LayerWidget(scene);
        snapLines = new SnapLinesController(scene, snapLinesLayer);
        snapSupplier = shapes.snapPoints(12, snapLines);
        getActions().addAction(new NextPrevAction(lookup, shapes, shapesLayer));
        addChild(shapesLayer);
        addChild(controlPointsLayer);
        addChild(snapLinesLayer);
        focusAction = new FocusAction(lookup);
        int ct = shapes.size();
        Runnable sync = this::sync;
        for (int i = ct - 1; i >= 0; i--) {
            ShapeElement entry = shapes.get(i);
            ShapeWidget sw = new ShapeWidget(scene, entry, shapes, repainter, ctrllr, lookup, sync);
            widgetForShape.put(entry, sw);
            Set<ControlPoint> cpForShape = new HashSet<>();
            controlPointsForShape.put(entry, cpForShape);
            sw.getActions().addAction(0, focusAction);
            shapesLayer.addChild(sw);
            ctrllr.getZoom().addChangeListener(WeakListeners.change((evt) -> {
                scene.validate();
            }, ctrllr.getZoom()));
            ControlPoint[] pts = controlPoints(entry);
            for (int j = 0; j < pts.length; j++) {
                ControlPoint cp = pts[j];
                cpForShape.add(cp);
                ControlPointWidget w = new ControlPointWidget(scene,
                        entry, cp, ctrllr, shapes, sw, sync,
                        snapSupplier);
                sw.addDependency(w);
                w.getActions().addAction(0, focusAction);
                controlPointWidgetForControlPoint.put(cp, w);
                controlPointsLayer.addChild(w);
            }
        }
    }

    static class SnapLinesController implements BiConsumer<SnapPoint, SnapPoint> {

        private final LineWidget vert;
        private final LineWidget horiz;

        public SnapLinesController(Scene scene, LayerWidget linesLayer) {
            vert = new LineWidget(scene);
            horiz = new LineWidget(scene);
            linesLayer.addChild(vert);
            linesLayer.addChild(horiz);
            vert.setVisible(false);
            horiz.setVisible(false);
        }

        @Override
        public void accept(SnapPoint x, SnapPoint y) {
            vert.setSnapPoint(x);
            horiz.setSnapPoint(y);
        }

        static final class LineWidget extends Widget {

            private SnapPoint point;
            private final Line2D.Double line
                    = new Line2D.Double();

            public LineWidget(Scene scene) {
                super(scene);
            }

            @Override
            protected boolean isRepaintRequiredForRevalidating() {
                return false;
            }

            void setSnapPoint(SnapPoint pt) {
                if (pt == null) {
                    point = null;
                    setVisible(false);
                } else {
                    System.out.println("SNAP " + pt.axis() + "-" + pt.coordinate());
                    point = pt;
                    Rectangle r = getScene().getBounds();
                    switch (pt.axis()) {
                        case X:
                            line.x1 = pt.coordinate();
                            line.x2 = pt.coordinate();
                            line.y1 = r.y;
                            line.y2 = r.y + r.height;
                            break;
                        case Y:
                            line.y1 = pt.coordinate();
                            line.y2 = pt.coordinate();
                            line.x1 = r.x;
                            line.x2 = r.x + r.width;
                            break;
                        default:
                            throw new AssertionError(pt.axis());
                    }
                    setVisible(true);
                    repaint();
                }
            }

            @Override
            protected Rectangle calculateClientArea() {
                return line.getBounds();
            }

            @Override
            protected void paintWidget() {
                if (point == null) {
                    return;
                }
                Graphics2D g = getGraphics();
                g.setColor(Color.WHITE);
//                g.setXORMode(Color.BLUE);
                g.draw(line);
//                g.setPaintMode();
            }
        }
    }

    private ShapeControlPoint[] controlPoints(ShapeElement entry) {
        ShapeControlPoint[] pts = entry.controlPoints(9, (cp) -> {
            entry.changed();
            repainter.ifSet((rp) -> {
                ShapeWidget w = widgetForShape.get(entry);
                if (w != null) {
                    w.revalidate();
                }
                ControlPointWidget cw = controlPointWidgetForControlPoint.get(cp);
                if (cw != null) {
                    cw.revalidate();
                }
                getScene().validate();
                rp.requestRepaint(entry.shape().getBounds());
            });
        });
        return pts;
    }

    private void sync2() {
        List<Widget> shapeWidgets = shapesLayer.getChildren();
        List<Widget> controlPointWidgets = controlPointsLayer.getChildren();

        Set<Widget> widgetsToRemove = new HashSet<>(10);

        Set<ShapeElement> removedShapes = new HashSet<>(10);

        Map<ShapeElement, Integer> expectedControlPointCount = new HashMap<>(shapes.size() * 4);

        Map<ShapeElement, Integer> foundControlPointWidgets = new HashMap<>(shapes.size() * 4);

        Map<ShapeElement, Widget> shapeWidgetForShape = new HashMap<>(shapes.size());

        Set<ShapeElement> shapesMissingControlPointWidgets = new HashSet<>(5);
        for (Widget w : shapeWidgets) {
            ShapeElement shape = w.getLookup().lookup(ShapeElement.class);
            if (shape != null) {
                shapeWidgetForShape.put(shape, w);
                foundControlPointWidgets.put(shape, 0);
                boolean present = shapes.contains(shape);
                if (!present) {
                    removedShapes.add(shape);
                    widgetsToRemove.add(w);
                } else {
                    expectedControlPointCount.put(shape, shape.getControlPointCount());
                    shapesMissingControlPointWidgets.add(shape);
                }
            }
        }
        for (Widget w : controlPointWidgets) {
            ShapeControlPoint scp = w.getLookup().lookup(ShapeControlPoint.class);
            if (scp != null) {
                if (removedShapes.contains(scp.owner())) {
                    widgetsToRemove.add(w);
                    controlPointWidgetForControlPoint.remove(scp);
                } else if (!scp.isValid()) {
                    widgetsToRemove.add(w);
                    controlPointWidgetForControlPoint.remove(scp);
                } else {
                    int ct = foundControlPointWidgets.getOrDefault(scp.owner(), 0) + 1;
                    foundControlPointWidgets.put(scp.owner(), ct);
                    if (ct >= expectedControlPointCount.getOrDefault(scp.owner(), 0)) {
                        shapesMissingControlPointWidgets.remove(scp.owner());
                    }
                }
            }
        }

        Set<? extends ShapeElement> missing = shapes.absent(expectedControlPointCount.keySet());
        boolean changed = !widgetsToRemove.isEmpty() || !missing.isEmpty()
                || !shapesMissingControlPointWidgets.isEmpty();
        if (changed) {
            for (Widget w : widgetsToRemove) {
                w.removeFromParent();
            }

            for (ShapeElement el : shapesMissingControlPointWidgets) {
                int expected = expectedControlPointCount.get(el);
                int current = foundControlPointWidgets.get(el);
                ShapeControlPoint[] cps = controlPoints(el);
                Widget wid = shapeWidgetForShape.get(el);
                for (int i = current; i < expected; i++) {
                    ControlPoint cp = cps[i];
                    ControlPointWidget cpw = new ControlPointWidget(
                            getScene(), el, cp, ctrllr, shapes, (ShapeWidget) wid, this::sync2,
                            snapSupplier);
                    controlPointsLayer.addChild(cpw);
                    controlPointWidgetForControlPoint.put(cp, cpw);
                }
            }

            // add in correct order
            for (ShapeElement el : CollectionUtils.reversed(new ArrayList<>(missing))) {
                ShapeWidget wid = new ShapeWidget(getScene(), el, shapes,
                        repainter, ctrllr, lookup, this::sync2);
                shapesLayer.addChild(wid);
                ShapeControlPoint[] cps = controlPoints(el);
                for (ShapeControlPoint cp : cps) {
                    ControlPointWidget cpw = new ControlPointWidget(
                            getScene(), el, cp, ctrllr, shapes, wid, this::sync2, snapSupplier);
                    controlPointsLayer.addChild(cpw);
                    controlPointWidgetForControlPoint.put(cp, cpw);
                }
            }
            getScene().validate();
        }

    }

    private void sync() {
        Set<ShapeElement> removed = new LinkedHashSet<>(widgetForShape.keySet());
        Set<ShapeElement> added = new LinkedHashSet<>();
        Set<ShapeElement> retained = new LinkedHashSet<>();
        Set<ControlPoint> removedControlPoints = new HashSet<>();
        Set<ControlPoint> addedControlPoints = new LinkedHashSet<>();
        Map<ControlPoint, ShapeElement> shapeForControlPoint = new HashMap<>();
        boolean changed = false;
        for (ShapeElement entry : shapes) {
            if (!widgetForShape.containsKey(entry)) {
                changed = true;
                added.add(entry);
                List<ControlPoint> pts = Arrays.asList(controlPoints(entry));
                addedControlPoints.addAll(pts);
                controlPointsForShape.put(entry, new HashSet<>(pts));
                for (ControlPoint p : pts) {
                    shapeForControlPoint.put(p, entry);
                }
            } else {
                removed.remove(entry);
                retained.add(entry);
                Adjustable adj = entry.item().as(Adjustable.class);
                if (adj != null) {
                    int ct = adj.getControlPointCount();
                    Set<ControlPoint> forShape = controlPointsForShape.get(entry);
                    if (forShape != null && forShape.size() != ct) {
                        changed = true;
                        // XXX they are just identified by index - we could
                        // keep some widgets
                        removedControlPoints.addAll(forShape);
                        forShape.clear();
                        List<ControlPoint> pts = Arrays.asList(controlPoints(entry));
                        forShape.addAll(pts);
                        addedControlPoints.addAll(pts);
                        for (ControlPoint p : pts) {
                            shapeForControlPoint.put(p, entry);
                        }
                    }
                }
            }
        }
//        if (!changed) {
//            return;
//        }
        for (ShapeElement rem : removed) {
            Set<ControlPoint> forShape = controlPointsForShape.get(rem);
            if (forShape != null) {
                removedControlPoints.addAll(forShape);
                for (ControlPoint cp : forShape) {
                    shapeForControlPoint.put(cp, rem);
                }
            }
            ShapeWidget widge = widgetForShape.remove(rem);
            if (widge != null) {
                widge.removeFromParent();
                widgetForShape.remove(widge);
            }
        }
        for (ShapeElement ad : added) {
            ShapeWidget nue = new ShapeWidget(getScene(), ad, shapes, repainter, ctrllr, lookup, this::sync);
            nue.getActions().addAction(focusAction);
            widgetForShape.put(ad, nue);
            addChild(nue);
        }
        for (ControlPoint cp : removedControlPoints) {
            ControlPointWidget widge = controlPointWidgetForControlPoint.remove(cp);
            if (widge != null) {
                Widget w = widge.getParentWidget();
                if (w != null) {
                    w.removeDependency(widge);
                    widge.removeFromParent();
                }
            }
            ShapeElement e = shapeForControlPoint.get(cp);
            if (e != null) {
                Set<ControlPoint> cp4s = controlPointsForShape.get(e);
                if (cp4s != null) {
                    cp4s.remove(cp);
                }
            }
        }
        for (ControlPoint cp : addedControlPoints) {
            ShapeElement e = shapeForControlPoint.get(cp);
            if (e != null) {
                Set<ControlPoint> cp4s = controlPointsForShape.get(e);
                if (cp4s == null) {
                    cp4s = new HashSet<>();
                    controlPointsForShape.put(e, cp4s);
                }
                cp4s.add(cp);
                ShapeWidget w = widgetForShape.get(e);
                if (w != null) {
                    ControlPointWidget cpw = new ControlPointWidget(
                            getScene(), e, cp, ctrllr, shapes, w, this::sync,
                            snapSupplier);
                    cpw.getActions().addAction(focusAction);
                    addChild(cpw);
                    w.addDependency(cpw);
                    controlPointWidgetForControlPoint.put(cp, cpw);
                }
            }
        }
        Set<ShapeElement> rem = new HashSet<>();
        for (Map.Entry<ShapeElement, Set<ControlPoint>> e : controlPointsForShape.entrySet()) {
            if (e.getValue().isEmpty()) {
                rem.add(e.getKey());
            }
        }
        for (ShapeElement r : rem) {
            controlPointsForShape.remove(r);
        }
        revalidate();
        getScene().validate();
    }

    static class NextPrevAction extends WidgetAction.Adapter {

        private final MutableProxyLookup lkp;
        private final ShapesCollection shapes;
        private final LayerWidget shapesLayer;

        NextPrevAction(MutableProxyLookup lkp, ShapesCollection shapes, LayerWidget shapesLayer) {
            this.lkp = lkp;
            this.shapes = shapes;
            this.shapesLayer = shapesLayer;
        }

        @Override
        public State keyReleased(Widget widget, WidgetKeyEvent event) {
            if (event.getKeyCode() == KeyEvent.VK_TAB && (!event.isAltDown() && !event.isControlDown() && !event.isMetaDown())) {
                boolean backward = event.isShiftDown();
                int sz = shapes.size();
                if (sz == 0) {
                    return super.keyReleased(widget, event);
                }
                ShapeElement current = lkp.lookup(ShapeElement.class);
                int target = backward ? sz - 1 : 0;
                if (current != null) {
                    int ix = shapes.indexOf(current);
                    if (ix >= 0) {
                        target = backward ? ix - 1 : ix + 1;
                    } else {
                        target = 0;
                    }
                    if (target >= sz) {
                        target = 0;
                    } else if (target < 0) {
                        target = sz - 1;
                    }
                }
                ShapeElement targetShape = shapes.get(target);
                for (Widget w : shapesLayer.getChildren()) {
                    if (w.getLookup().lookupAll(ShapeElement.class).contains(targetShape)) {
                        lkp.lookups(w.getLookup());
                        w.repaint();
                        break;
                    }
                }
                widget.repaint();
                return WidgetAction.State.CONSUMED;
            }
            return super.keyReleased(widget, event);
        }
    }

    static class FocusAction extends WidgetAction.Adapter {

        private final MutableProxyLookup lookup;

        public FocusAction(MutableProxyLookup lookup) {
            this.lookup = lookup;
        }

        @Override
        public State mousePressed(Widget widget, WidgetMouseEvent event) {
            lookup.lookups(widget.getLookup());
            return WidgetAction.State.CHAIN_ONLY;
        }

        @Override
        public State focusGained(Widget widget, WidgetFocusEvent event) {
            lookup.lookups(widget.getLookup());
            return WidgetAction.State.CHAIN_ONLY;
        }
    }

    @Override
    protected boolean isRepaintRequiredForRevalidating() {
        return false;
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }

    @Override
    public void setLookupConsumer(Consumer<Lookup[]> additionaLookupConsumer) {
        additionaLookupConsumer.accept(new Lookup[]{lookup});
    }

    @Override
    public SetterResult setOpacity(double opacity) {
        this.opacity = opacity;
        return SetterResult.HANDLED;
    }

    @Override
    public SetterResult setLocation(Point location) {
        setPreferredLocation(location);
        return SetterResult.HANDLED;
    }

    @Override
    public void setZoom(Zoom zoom) {
    }

    @Override
    protected Graphics2D getGraphics() {
        Graphics2D result = super.getGraphics();
        Composite oldComposite = null;
        try {
            if (result != null && opacity != 1D) {
                oldComposite = result.getComposite();
                AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) opacity);
                result.setComposite(alpha);
            }
            return result;
        } finally {
            if (oldComposite != null) {
                result.setComposite(oldComposite);
            }
        }
    }
}
