/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.function.Supplier;
import javax.swing.JPopupMenu;
import javax.swing.event.UndoableEditEvent;
import net.dev.java.imagine.api.tool.aspects.snap.SnapPoints;
import net.java.dev.imagine.api.vector.design.ControlPoint;
import net.java.dev.imagine.ui.common.UndoMgr;
import org.imagine.editor.api.Zoom;
import org.imagine.geometry.Circle;
import org.imagine.utils.java2d.GraphicsUtils;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import org.imagine.vector.editor.ui.tools.DMA.TranslateHandler;
import org.imagine.vector.editor.ui.undo.AbstractShapeEdit;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.api.visual.widget.Widget.Dependency;
import org.netbeans.paintui.widgetlayers.WidgetController;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Tim Boudreau
 */
class ControlPointWidget extends Widget implements Dependency {

    static final double BASE_SIZE = 5;
    private final ControlPoint cp;
    private final WidgetController ctrllr;
    private final ShapeElement element;
    private final DragNotifier notifier;
    private final ShapesCollection shapes;
    private final Supplier<SnapPoints> snapSupplier;

    public ControlPointWidget(Scene scene, ShapeElement en,
            ControlPoint cp, WidgetController ctrllr, ShapesCollection shapes,
            DragNotifier notifier, Runnable refresh,
            Supplier<SnapPoints> snapSupplier) {
        super(scene);
        this.element = en;
        this.shapes = shapes;
        this.cp = cp;
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        getActions().addAction(new DoubleMoveAction(
                DoubleMoveStrategy.FREE,
//                new SnapPointsMoveStrategy(snapSupplier),
                DMA.INSTANCE));
        this.ctrllr = ctrllr;
        this.notifier = notifier;
        getActions().addAction(ActionFactory.createPopupMenuAction((Widget widget, Point localLocation) -> {
            JPopupMenu menu = ShapeActions.populatePopup(cp, en, shapes, refresh, widget, localLocation);
            return menu;
        }));
        this.snapSupplier = snapSupplier;
    }

    class SnapPointsMoveStrategy implements DoubleMoveStrategy {

        private final Supplier<SnapPoints> supp;

        public SnapPointsMoveStrategy(Supplier<SnapPoints> supp) {
            this.supp = supp;
        }

        @Override
        public Point2D locationSuggested(Widget widget, Point2D originalLocation, Point2D suggestedLocation) {
            SnapPoints pts = supp.get();

            AffineTransform xform = DoubleMoveAction.localToScene(widget);
            Point2D.Double converted = new Point2D.Double();
            xform.transform(suggestedLocation, converted);

            Point2D snapped = pts.snap(converted);

            System.out.println(suggestedLocation.getX() + ", "
                    + suggestedLocation.getY() + " to scene "
                    + converted.getX() + ", " + converted.getY()
                    + " snapped to " + snapped.getX() + ", " + snapped.getY());

            if (snapped == converted) {
                return suggestedLocation;
            }

            Point2D unconverted = DoubleMoveAction.sceneToLocal(widget, snapped);
            System.out.println("  in local points " + unconverted.getX() + ", " + unconverted.getY());
            return unconverted;
        }
    }

    @Override
    protected boolean isRepaintRequiredForRevalidating() {
        return false;
    }

    @Override
    public boolean isHitAt(Point localLocation) {
        Circle circ = circle();
        circ.setRadius(Math.max(3, circ.radius()));
        return circ.contains(localLocation.x, localLocation.y);
    }

    TranslateHandler th = new TranslateHandler() {
        double ox, oy;

        @Override
        public void onStart(Point p) {
            if (!checkValid()) {
                return;
            }
            ox = cp.getX();
            oy = cp.getY();
            notifier.onStartControlPointDrag();
        }

        @Override
        public void onMove(double offX, double offY) {
            if (!checkValid()) {
                return;
            }
            notifier.onControlPointDragUpdate(cp.index(), cp.getX() + offX,
                    cp.getY() + offY);
        }

        @Override
        public void translate(double x, double y) {
            if (!checkValid()) {
                return;
            }
//            if (cp.getX() != ox || cp.getY() != oy) {
            UndoMgr mgr = Utilities.actionsGlobalContext().lookup(UndoMgr.class);
            if (mgr != null) {
                CPEdit edit = new CPEdit(cp, ox, oy);
                edit.hook(() -> {
                    revalidate();
                    getScene().validate();
                });
                mgr.undoableEditHappened(new UndoableEditEvent(this, edit));
            }
            cp.move(x, y);
//            }
            notifier.onEndControlPointDrag();
            revalidate();
            getScene().repaint();
        }
    };

    @Messages("MOVE_CONTROL_POINT=Move Control Point")
    static class CPEdit extends AbstractShapeEdit {

        private final ControlPoint pt;
        private final double ox, oy;
        private double nx, ny;
        private boolean undone;

        public CPEdit(ControlPoint pt, double ox, double oy) {
            super(Bundle.MOVE_CONTROL_POINT());
            this.pt = pt;
            this.ox = ox;
            this.oy = oy;
        }

        @Override
        protected void undoImpl() {
            nx = pt.getX();
            ny = pt.getY();
            pt.set(ox, oy);
            undone = true;
        }

        @Override
        public boolean canUndo() {
            return !undone;
        }

        @Override
        protected void redoImpl() {
            pt.set(nx, ny);
            undone = false;
        }

        @Override
        public boolean canRedo() {
            return undone;
        }

        @Override
        public void die() {
            // do nothing
        }
    }

    @Override
    public Lookup getLookup() {
        return Lookups.fixed(element, cp, th, element.item(), shapes);
    }

    private Circle circle() {
        Zoom zoom = ctrllr.getZoom();
//        Point loc = getPreferredLocation();
        Point loc = null;
        Circle circ;
        if (loc == null) {
            circ = new Circle(cp.getX(), cp.getY(), zoom.inverseScale(BASE_SIZE));
        } else {
            circ = new Circle(loc.x, loc.y, zoom.inverseScale(BASE_SIZE));
        }
        return circ;
    }

    private boolean removed;

    private boolean checkValid() {
        if (removed) {
            return false;
        }
        boolean result = cp.isValid();
        if (!result) {
            removed = true;
            EventQueue.invokeLater(() -> {
                Scene scene = getScene();
                removeFromParent();
                scene.validate();
            });
        }
        return result;
    }

    @Override
    protected void paintWidget() {
        if (!checkValid()) {
            return;
        }
        Graphics2D g1 = getGraphics();
        GraphicsUtils.setHighQualityRenderingHints(g1);
        Circle circ = circle();
        g1.setStroke(ctrllr.getZoom().getStroke());
        g1.setColor(cp.isVirtual() ? Color.WHITE : Color.ORANGE);
        g1.fill(circ);
        g1.setColor(new Color(128, 128, 255));
        g1.draw(circ);
    }

    @Override
    protected Rectangle calculateClientArea() {
        if (!checkValid()) {
            return new Rectangle();
        }
        return circle().getBounds();
    }

    @Override
    public void revalidateDependency() {
        if (removed) {
            return;
        }
        revalidate();
    }
}
