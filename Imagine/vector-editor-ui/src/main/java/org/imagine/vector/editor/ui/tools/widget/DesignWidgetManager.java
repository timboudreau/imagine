package org.imagine.vector.editor.ui.tools.widget;

import org.imagine.vector.editor.ui.tools.widget.util.OperationListenerBroadcaster;
import org.imagine.vector.editor.ui.tools.widget.util.OperationListener;
import com.mastfrog.function.TriConsumer;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import org.imagine.vector.editor.ui.tools.widget.painting.DesignerProperties;
import org.imagine.vector.editor.ui.tools.widget.actions.RectSelectAction;
import org.imagine.vector.editor.ui.tools.widget.actions.FocusAction;
import net.java.dev.imagine.api.vector.design.ControlPoint;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;
import net.java.dev.imagine.api.vector.Centered;
import net.java.dev.imagine.api.vector.Shaped;
import net.java.dev.imagine.api.vector.Transformable;
import org.imagine.awt.key.PaintKey;
import org.imagine.editor.api.Zoom;
import org.imagine.editor.api.grid.Grid;
import org.imagine.editor.api.grid.SnapSettings;
import org.imagine.editor.api.snap.SnapKind;
import org.imagine.editor.api.snap.SnapPoints;
import org.imagine.editor.api.snap.Thresholds;
import org.imagine.geometry.Circle;
import org.imagine.geometry.EqPointDouble;
import org.imagine.geometry.LineVector;
import org.imagine.geometry.util.PooledTransform;
import org.imagine.utils.painting.RepaintHandle;
import org.imagine.vector.editor.ui.ShapeEntry;
import org.imagine.vector.editor.ui.ShapeSnapPointEntry;
import org.imagine.vector.editor.ui.ThresholdsOverZoom;
import org.imagine.vector.editor.ui.palette.PaintPalettes;
import org.imagine.vector.editor.ui.spi.ShapeControlPoint;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import org.imagine.vector.editor.ui.tools.MutableProxyLookup;
import org.imagine.vector.editor.ui.tools.widget.actions.AdjustmentKeyAction;
import org.imagine.vector.editor.ui.tools.widget.actions.MoveInSceneCoordinateSpaceAction;
import org.imagine.vector.editor.ui.tools.widget.actions.DragHandler;
import org.imagine.vector.editor.ui.tools.widget.actions.HideNoiseAction;
import org.imagine.vector.editor.ui.tools.widget.actions.NextPrevKeyAction;
import org.imagine.vector.editor.ui.tools.widget.actions.RotateInSceneCoordinateSpaceAction;
import org.imagine.vector.editor.ui.tools.widget.actions.RotationHandler;
import org.imagine.vector.editor.ui.tools.widget.actions.ShapeActions;
import org.imagine.vector.editor.ui.tools.widget.util.UIState;
import org.imagine.vector.editor.ui.tools.widget.util.UIState.UIStateListener;
import org.netbeans.api.visual.action.AcceptProvider;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.ConnectorState;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.widget.LayerWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.api.visual.widget.Widget.Dependency;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Tim Boudreau
 */
public class DesignWidgetManager implements DesignerControl {

    private final ShapesCollection shapes;
    private final Scene scene;
    // Controls colors and shapes for decorations we draw
    private final DesignerProperties props = DesignerProperties.get();
    // The main layer we attach to
    private HetroObjectLayerWidget widget;
    // The lookup whose contents we manipulate on selection change
    private final MutableProxyLookup selectionLookup;

    // A proxy which rebroadcasts events
    private final OperationListenerBroadcaster opListeners
            = new OperationListenerBroadcaster();
    // Allows intermittently turning off painting of various decorations
    // during operations such as drags
    private final UIStateImpl uiState = new UIStateImpl();
    private Supplier<SnapPoints<ShapeSnapPointEntry>> pts;
    // Action handlers
    private final ShapeDragHandler shapeDrag = new ShapeDragHandler();
    private final ManyControlPointWidgetDragHandler manyCpDrag = new ManyControlPointWidgetDragHandler();
    private final ManyControlPointWidgetRotationHandler manyCpRotate = new ManyControlPointWidgetRotationHandler();
    private final ShapeAdjustmentKeyHandler shapeKeyHandler = new ShapeAdjustmentKeyHandler();
    private final ControlPointKeyHandler controlPointKeyHandler = new ControlPointKeyHandler();
    private ManyControlPointsWidget many;
    // layers for painting stuff:
    // Layer which paints snap guides when a drag is in progress
    // with snap-to-grid or similar active
//    private SnapDecorationsLayer snapLayer;
    private SnapDecorationsLayer2 snapLayer;
    // During palette drop operations, holds an IconNodeWidget that
    // shows an image of the dragged object
    private LayerWidget dragImageLayer;
    private AnimationLayer anim;
    // reusable actions
    private NextPrevKeyAction nextPrevKeyAction;
    private final WidgetAction focusAction;
    private final WidgetAction keyMoveAction = new AdjustmentKeyAction();
    private final MoveInSceneCoordinateSpaceAction moveAction;
    private final WidgetAction popupAction;
    // Action on the scene which accepts palette drag operations of
    // shapes to be dropped in the scene
    private final WidgetAction shapeAcceptAction = ActionFactory.createAcceptAction(new ShapeAcceptProvider());
    // Action on each shape widget which can replace the fill paint
    // when a color or paint is dropped from a palette
    private final WidgetAction paintAcceptAction = ActionFactory.createAcceptAction(new PaintAcceptProvider());
    private GridWidget grid;
    // Repaints the main widget
    private final ChangeListener gridAndSnapSettingsListener = evt -> {
        if (evt.getSource() instanceof Grid) {
            grid.revalidate(true);
        } else {
            widget.repaint();
        }
    };
    private final OperationListener opl = new OpListener();
    private final UIStateListener uiListener = new UIStateListenerImpl();
    private Widget temporaryDragImageWidget;
    private final ShapeActions shapeActions = new ShapeActions(this);
    private final Thresholds thresholds;
    private final SceneZoom sceneZoom;

    public DesignWidgetManager(Scene scene, ShapesCollection coll, MutableProxyLookup lookup) {
        this.scene = scene;
        this.shapes = coll;
        this.selectionLookup = lookup;
        sceneZoom = new SceneZoom();
        focusAction = new FocusAction(selectionLookup, this::onFocusedWidgetChanged);
        moveAction = new MoveInSceneCoordinateSpaceAction();
//        popupAction = ShapePopupActions.popupMenuAction(this);
        popupAction = shapeActions.popupMenuAction();
        thresholds = new ThresholdsOverZoom(sceneZoom);
    }

    public DesignWidgetManager(Scene scene, ShapesCollection coll) {
        this(scene, coll, new MutableProxyLookup());
    }

    class SceneRepaintHandle implements RepaintHandle {

        private final Rectangle rect = new Rectangle();

        @Override
        public void repaintArea(int x, int y, int w, int h) {
            rect.setBounds(x, y, w, h);
            if (scene.getView() != null) {
                Rectangle r = scene.convertSceneToView(rect);
                scene.getView().repaint(r);
            }
        }
    }

    class SceneZoom implements Zoom, Supplier<Zoom> {

        private final AffineTransform xform = PooledTransform.get(this);
        private final AffineTransform inverse = PooledTransform.get(this);

        @Override
        public AffineTransform getZoomTransform() {
            double z = scene.getZoomFactor();
            xform.setToScale(z, z);
            return xform;
        }

        @Override
        public AffineTransform getInverseTransform() {
            double z = 1D / scene.getZoomFactor();
            inverse.setToScale(z, z);
            return inverse;
        }

        @Override
        public float getZoom() {
            return (float) scene.getZoomFactor();
        }

        @Override
        public void setZoom(float val) {
            scene.setZoomFactor(val);
        }

        @Override
        public void addChangeListener(ChangeListener cl) {
        }

        @Override
        public void removeChangeListener(ChangeListener cl) {
        }

        @Override
        public Zoom get() {
            return this;
        }

    }

    public HetroObjectLayerWidget getMainWidget() {
        if (widget != null) {
            return widget;
        }
        widget = HetroObjectLayerWidget.create(scene, f -> {
            f.add(ShapeElement.class, this::createShapeWidget);
//            f.add(ShapeControlPoint.class, this::createControlPointWidget);
            f.withSelectionLookup(selectionLookup);
        });
        nextPrevKeyAction = new NextPrevKeyAction(new NextPrevProviderImpl(selectionLookup, widget, this::onFocusedWidgetChanged));
        LayerWidget selectionLayer = new LayerWidget(scene);

//        widget.addChild(snapLayer = new SnapDecorationsLayer(scene));
        widget.addChild(snapLayer = new SnapDecorationsLayer2(scene,
                new SceneRepaintHandle(), selectionLookup, sceneZoom));

//        widget.parentFor(ShapeElement.class).getActions().addAction(shapeAcceptAction);
        widget.addPriorAction(shapeAcceptAction);
        widget.getActions().addAction(
                RectSelectAction.createAction(selectionLayer,
                        selectionLookup, shapes, scene));
        widget.addPriorAction(new HideNoiseAction(uiState));

        grid = new GridWidget(scene);
        widget.addChild(grid);

        many = new ManyControlPointsWidget(scene, shapes, props, uiState, selectionLookup, manyCpDrag, manyCpRotate);
        WidgetAction.Chain manyLayerActions = many.getActions();
        manyLayerActions.addAction(popupAction);
        manyLayerActions.addAction(nextPrevKeyAction);
//        manyLayerActions.addAction(focusAction);
        manyLayerActions.addAction(moveAction);
        manyLayerActions.addAction(new RotateInSceneCoordinateSpaceAction());
        manyLayerActions.addAction(keyMoveAction);
        shapeActions.applyKeyboardActions(many);

        widget.addChild(many);
        widget.addChild(selectionLayer);

        anim = new AnimationLayer(scene);
        widget.addChild(anim);

        dragImageLayer = new LayerWidget(scene);
        widget.addChild(dragImageLayer);
        // Drag support will show it when needed

        // avoid self-resizing
        widget.setPreferredLocation(new Point(0, 0));
        pts = shapes.snapPoints(props.snapRadius(scene.getZoomFactor()), snapLayer.snapListener());

        Grid.getInstance().addChangeListener(gridAndSnapSettingsListener);
        SnapSettings.getGlobal().addChangeListener(gridAndSnapSettingsListener);

        init();
        uiState.listen(uiListener);
        opListeners.add(opl);

        return widget;
    }

    public void onFocusedWidgetChanged(Widget focused) {
        if (focused != scene.getFocusedWidget()) {
            scene.setFocusedWidget(focused);
            if (focused != null) {
                focused.revalidate(true);
                if (focused instanceof ControlPointWidget) {
                    anim.setShape(((ControlPointWidget) focused)
                            .controlPointShape(true));
                    scene.validate();
                } else {
                    ShapeElement el = focused.getLookup().lookup(ShapeElement.class);
                    if (el != null) {
                        anim.setShape(el.shape());
                        scene.validate();
                    }
                }
            }
        }
    }

    class UIStateListenerImpl implements UIStateListener {

        @Override
        public void onChange(UIState.Props prop, boolean val) {
            switch (prop) {
                case CONNECTOR_LINES_VISIBLE:
                case CONTROL_POINTS_VISIBLE:
                    widget.allWidgets(ShapeControlPoint.class, (pt, widge) -> {
                        widge.revalidate(true);
                    });
                    break;
            }
        }
    }

    class OpListener implements OperationListener {

        final Lookup.Template<ShapeControlPoint> tpl
                = new Lookup.Template<>(ShapeControlPoint.class);

        @Override
        public void onDragStarted(Lookup lkp) {
            // show the snap layer when dragging a control point
            if (!SnapSettings.getGlobal().isEnabled() || SnapSettings.getGlobal().getEnabledSnapKinds().isEmpty()) {
                return;
            }
            // ensure no turds
            snapLayer.snapListener().onSnap(null, null);
            snapLayer.setVisible(true);
            scene.validate();
        }

        @Override
        public void onDragCompleted(Lookup lkp) {
            // We are assuming this does not change *during* a drag
            if (!SnapSettings.getGlobal().isEnabled() || SnapSettings.getGlobal().getEnabledSnapKinds().isEmpty()) {
                return;
            }
            // clean up turds
            snapLayer.snapListener().onSnap(null, null);
            // hide it
            snapLayer.setVisible(false);
            scene.validate();
        }

        @Override
        public void onDragCancelled(Lookup lkp) {
            onDragCompleted(lkp);
        }
    }

    public void centerSelected() {
        center(selectionLookup.lookupAll(ShapeElement.class));
    }

    public void center(Collection<? extends ShapeElement> all) {
        if (all.isEmpty()) {
            return;
        }
        Rectangle bds = new Rectangle();
        for (ShapeElement el : all) {
            Widget w = widget.find(el);
            Rectangle r = w.getClientArea();
            if (r != null && !r.isEmpty()) {
                r = w.convertLocalToScene(r);
                r = scene.convertSceneToView(r);
                if (bds.isEmpty()) {
                    bds.setBounds(r);
                } else {
                    bds.add(r);
                }
            }
        }
        if (!bds.isEmpty()) {
            JComponent view = scene.getView();
            if (view != null) {
                double cx = bds.getCenterX();
                double cy = bds.getCenterY();
                Rectangle scrolled = view.getVisibleRect();
                double offX = cx - scrolled.getCenterX();
                double offY = cy - scrolled.getCenterY();
//                System.out.println("adjust scroll by " + offX + "," + offY);
                scrolled.translate((int) Math.round(offX), (int) Math.round(offY));
                view.scrollRectToVisible(scrolled);
            }
        }
    }

    private void controlPointMoved(ShapeElement shape, ControlPoint cp, Widget w) {
        w.revalidate();
    }

    private <T> boolean withWidgetFor(T obj, Consumer<Widget> c) {
        Widget w = widget.find(obj);
        if (w != null) {
            c.accept(w);
            return true;
        }
        return false;
    }

    private void init() {
        shapes.forEach(this::addWidgetsForShapeElement);
    }

    private Widget addWidgetsForShapeElement(ShapeElement en) {
        Widget w = widget.add(en);
        ShapeControlPoint[] cps = en.controlPoints(9, cp -> {
            controlPointMoved(en, cp, w);
        });
        return w;
    }

    private void addWidgetsForShapeElementToLiveScene(ShapeElement en) {
        trackingSelection(tracker -> {

            Widget w = tracker.addToScene(en);
            many.shapeAdded(en);
        });
    }

    @Override
    public void shapeGeometryChanged(ShapeElement el) {
        Widget w = widget.find(el);
        if (w != null) {
            el.changed();
            many.syncOne(el);
            w.revalidate();
            scene.validate();
            w.repaint();
        }
    }

    @Override
    public boolean shapeAdded(ShapeElement el) {
        Widget w = widget.find(el);
        if (w == null) {
            addWidgetsForShapeElementToLiveScene(el);
            return true;
        }
        return false;
    }

    @Override
    public void sync() {
        shapesMayBeDeleted();
        shapeMayBeAdded();
    }

    @Override
    public void pointCountMayBeChanged(ShapeElement el) {
        many.syncOne(el);
    }

    @Override
    public void updateSelection(ShapeElement el) {
        Widget w = widget.find(el);
        if (w != null) {
            selectionLookup.updateLookups(w.getLookup());
            scene.setFocusedWidget(w);
        }
    }

    @Override
    public void updateSelection(ShapeControlPoint pt) {
        Widget w = widget.find(pt);
        if (w != null) {
            selectionLookup.updateLookups(w.getLookup());
            scene.setFocusedWidget(w);
        }
    }

    @Override
    public boolean controlPointDeleted(ShapeControlPoint pt) {
        return trackingSelection(tracker -> {
            Widget w = widget.find(pt.owner());
            many.controlPointDeleted(pt);
            // do this first, so we capture the former bounds
            if (w != null) {
                ShapeElement el = w.getLookup().lookup(ShapeElement.class);
                el.changed();
            }
//            tracker.removeFromScene(pt);
            if (w != null) {
                pt.owner().changed();
                w.revalidate();
                scene.validate();
            }
        });
    }

    @Override
    public boolean shapeDeleted(ShapeElement el) {
        Widget wid = widget.find(el);
        if (wid == null) {
            return false;
        }
        return trackingSelection(tracker -> {
            tracker.removeFromScene(el);
            many.shapeDeleted(el);
        });
    }

    @Override
    public void shapesMayBeDeleted() {
        trackingSelection(tracker -> {
            widget.allWidgetsMatching(sh -> shapes.indexOf(sh) < 0, ShapeElement.class, (sh, wid) -> {
                tracker.removeFromScene(sh);
                many.shapeDeleted(sh);
            });
        });
    }

    private boolean trackingSelection(Consumer<SelectionTracker> c) {
        SelectionTracker tracker = new SelectionTracker();
        tracker.run(c);
        return tracker.needValidate;
    }

    /**
     * Ensures that if a new shape is added, that becomes the selection, and
     * that if the selected shape or control point is deleted, it doesn't remain
     * hanging around in the selection lookup.
     */
    class SelectionTracker {

        private final Widget initiallyFocusedWidget;
        private final Widget parentOfInitiallyFocusedWidget;
        private final int initiallyFocusedWidgetIndexInParent;
        private final Set<Lookup> toRemove = new HashSet<>(3);
        private final List<Widget> addedWidgets = new ArrayList<>(5);
        private boolean needValidate;
        private boolean focusedWidgetDeleted;

        SelectionTracker() {
            // Figure out where focus was initially
            initiallyFocusedWidget = scene.getFocusedWidget();
            parentOfInitiallyFocusedWidget = initiallyFocusedWidget != null
                    ? initiallyFocusedWidget.getParentWidget() : null;
            if (parentOfInitiallyFocusedWidget != null) {
                initiallyFocusedWidgetIndexInParent = parentOfInitiallyFocusedWidget.getChildren().indexOf(initiallyFocusedWidget);
            } else {
                initiallyFocusedWidgetIndexInParent = -1;
            }
        }

        void onDone() {
            try {
                for (Lookup lkp : toRemove) {
                    selectionLookup.removeLookup(lkp);
                }
                // XXX perhaps more intuitive would be to find the
                // widget in that layer closest in *physical* location
                // to the deleted one
                if (focusedWidgetDeleted) {
                    boolean selectionAlreadyChanged = selectBestAddedWidget();
                    if (parentOfInitiallyFocusedWidget != null && !selectionAlreadyChanged) {
                        if (parentOfInitiallyFocusedWidget != null) {
                            List<Widget> kids = parentOfInitiallyFocusedWidget.getChildren();
                            Widget newFocusedWidget = null;
                            if (initiallyFocusedWidgetIndexInParent >= kids.size() && !kids.isEmpty()) {
                                newFocusedWidget = kids.get(kids.size() - 1);
                            } else if (initiallyFocusedWidgetIndexInParent >= 0 && !kids.isEmpty()) {
                                newFocusedWidget = kids.get(initiallyFocusedWidgetIndexInParent);
                            }
                            if (newFocusedWidget != null) {
                                selectionLookup.addLookup(newFocusedWidget.getLookup());
                                scene.setFocusedWidget(newFocusedWidget);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            } finally {
                if (needValidate) {
                    scene.validate();
                }
            }
        }

        boolean selectBestAddedWidget() {
            Widget added = null;
            Widget addedControlPoint = null;
            for (Widget w : addedWidgets) {
                if (w instanceof OneShapeWidget) {
                    added = w;
                } else if (w instanceof ControlPointWidget) {
                    addedControlPoint = w;
                }
            }
            if (added != null) {
                selectionLookup.updateLookups(added.getLookup());
                scene.setFocusedWidget(added);
                return true;
            }
            if (addedControlPoint != null) {
                selectionLookup.updateLookups(addedControlPoint.getLookup());
                scene.setFocusedWidget(addedControlPoint);
                return true;
            }
            return false;
        }

        <T> Widget addToScene(T obj) {
            Widget result = widget.add(obj);
            addedWidgets.add(result);
            needValidate = true;
            return result;
        }

        SelectionTracker removeFromScene(Object obj) {
            Widget w = widget.remove(obj);
            if (w != null) {
                if (w == initiallyFocusedWidget) {
                    focusedWidgetDeleted = true;
                }
                Lookup l = w.getLookup();
                if (selectionLookup.containsLookup(l)) {
                    toRemove.add(l);
                }
                needValidate = true;
            }
            return this;
        }

        void run(Consumer<SelectionTracker> c) {
            try {
                c.accept(this);
            } finally {
                onDone();
            }
        }
    }

    @Override
    public void shapeMayBeAdded() {
        boolean found = false;
        Widget lastAdded = null;
        for (ShapeElement sh : shapes) {
            if (widget.find(sh) == null) {
                found = true;
                lastAdded = addWidgetsForShapeElement(sh);
            }
        }
        if (found) {
            scene.validate();
            if (lastAdded != null) {
                selectionLookup.updateLookups(lastAdded.getLookup());
                scene.setFocusedWidget(lastAdded);
            }
        }
    }

    public void revalidateControlPoints(ShapeElement e) {
        Widget w = widget.find(e);
        if (w != null) {
            w.revalidate();
        }
        many.revalidate();
    }

    OneShapeWidget createShapeWidget(Scene scene, ShapeElement entry) {
        OneShapeWidget result = new OneShapeWidget(scene, entry, shapes,
                props.decorationController(), shapeDrag, uiState,
                shapeKeyHandler, widget.aspectRatio());
        WidgetAction.Chain actions = result.getActions();
        actions.addAction(popupAction);
        actions.addAction(nextPrevKeyAction);
        actions.addAction(focusAction);
        actions.addAction(moveAction);
        actions.addAction(keyMoveAction);
        actions.addAction(paintAcceptAction);
        shapeActions.applyKeyboardActions(result);
        return result;
    }

    public Runnable onOperation(OperationListener listener) {
        return opListeners.add(listener);
    }

    private void abortAllDragOperations() {
        moveAction.abort();
        shapeDrag.clearDragState();
        manyCpDrag.clearDragState();
    }

    private static Image imageForTransferable(Transferable xfer) {
        if (xfer.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            try {
                return (Image) xfer.getTransferData(DataFlavor.imageFlavor);
            } catch (UnsupportedFlavorException | IOException ex) {
                Logger.getLogger(DesignWidgetManager.class.getName()).log(Level.INFO, "No image found", ex);
            }
        }
        return null;
    }

    private Image dragImage;

    private void setupDragImage(Transferable xfer, Point point) {
        try {
            if (xfer != null) {
                if (dragImage == null) {
                    dragImage = imageForTransferable(xfer);
                }
            } else {
                dragImage = null;
            }
        } finally {
            repaintDragImage(point);
        }
    }

    private void repaintDragImage(Point p) {
        boolean needValidate = false;
        if (dragImage == null) {
            if (temporaryDragImageWidget != null) {
                temporaryDragImageWidget.removeFromParent();
                temporaryDragImageWidget = null;
                needValidate = true;
            }
        } else {
            if (temporaryDragImageWidget == null) {
                temporaryDragImageWidget = new DragImageWidget(scene, dragImage);
                dragImageLayer.addChild(temporaryDragImageWidget);
                temporaryDragImageWidget.setPreferredLocation(p);
                dragImageLayer.revalidate();
                needValidate = true;
            } else {
                temporaryDragImageWidget.setPreferredLocation(p);
                temporaryDragImageWidget.revalidate();
                temporaryDragImageWidget.repaint();
                dragImageLayer.revalidate();
                needValidate = true;
            }
        }
        if (needValidate) {
            scene.validate();
        }
    }

    class ShapeAcceptProvider implements AcceptProvider {

        @Override
        public ConnectorState isAcceptable(Widget widget, Point point, Transferable transferable) {
            if (widget.isHitAt(point)) {
                if (PaintPalettes.containsShapeElement(transferable)) {
                    setupDragImage(transferable, point);
                    return ConnectorState.ACCEPT;
                }
            }
            return ConnectorState.REJECT;
        }

        @Override
        @Messages("DropShape=Drop Shape")
        public void accept(Widget widget, Point point, Transferable transferable) {
            try {
                ShapeEntry el = PaintPalettes.shapeElementFromTransferable(transferable);
                if (el != null) {
                    shapes.contentsEdit(Bundle.DropShape(), () -> {
                        // XXX translate to center, or mouse position?
                        Rectangle bds = el.getBounds();
                        int cx = (int) bds.getCenterX();
                        int cy = (int) bds.getCenterY();
                        el.translate(point.x - cx, point.y - cy);
                        ShapeElement real = shapes.addForeign(el);
                        shapeAdded(real);
                        selectionLookup.updateLookups(
                                DesignWidgetManager.this.widget
                                        .find(real).getLookup());
                    });
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            } finally {
                setupDragImage(null, null);
            }
        }
    }

    class PaintAcceptProvider implements AcceptProvider {

        @Override
        public ConnectorState isAcceptable(Widget widget, Point point, Transferable transferable) {
            if (widget.isHitAt(point)) {
                if (PaintPalettes.containsPaintKey(transferable)) {
                    setupDragImage(transferable, point);
                    return ConnectorState.ACCEPT;
                }
            }
//            System.out.println("PAINT REJECT " + point + " " + transferable
//                + " evt " + EventQueue.getCurrentEvent());
            return ConnectorState.REJECT;
        }

        @Override
        public void accept(Widget widget, Point point, Transferable transferable) {
            try {
                PaintKey<?> key = PaintPalettes.paintKeyFromTransferable(transferable);
                ShapeElement el = widget.getLookup().lookup(ShapeElement.class);
                el.setFill(key);
                widget.repaint();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            } finally {
                setupDragImage(null, point);
            }
        }
    }

    class ShapeDragHandler implements DragHandler {

        private Runnable snapshot;
        private Point2D last;
        private ShapeElement shapeCopy;
        private final Set<ControlPointWidget> restoreControlPointsOn = new HashSet<>();

        private void repaint(Widget widget, Rectangle bounds) {
            JComponent view = widget.getScene().getView();
            if (view != null) {
                bounds = widget.convertLocalToScene(bounds);
                bounds = widget.getScene().convertSceneToView(bounds);
                view.repaint(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        }

        private void shapeShift(Widget w, Point2D original, Point2D current, ShapeElement el) {
            Point2D diffTo;
            if (last != null) {
                diffTo = last;
            } else {
                diffTo = original;
            }
            double dx = current.getX() - diffTo.getX();
            double dy = current.getY() - diffTo.getY();
            last = current;
            Rectangle r = shapeCopy.getBounds();
            shapeCopy.translate(dx, dy);
            shapeCopy.addToBounds(r);
            w.revalidate(false);
            for (Dependency dep : w.getDependencies()) {
                dep.revalidateDependency();
            }
//            w.getScene().validate();
//            w.repaint();
            repaint(w, r);
        }

        private void withShapeInfo(Widget w, Point2D original, Point2D current, boolean moveShape,
                BiConsumer<ShapeElement, OneShapeWidget> c) {
            assert w instanceof OneShapeWidget : "Unexpected widget type " + w.getClass().getName() + " " + w;
            ShapeElement e = w.getLookup().lookup(ShapeElement.class);
            c.accept(e, (OneShapeWidget) w);
            if (moveShape) {
                shapeShift(w, original, current, e);
            }
        }

        @Override
        public void onBeginDrag(Widget w, Point2D original, Point2D current) {
            if (!uiState.shapesDraggable()) {
                return;
            }
            uiState.setConnectorLinesVisible(false);
            uiState.setControlPointsVisible(false);
            withShapeInfo(w, original, current, true, (e, osw) -> {
                // Save a snapshot
                // XXX - now that we are using a copy, do we need this?
                snapshot = e.geometrySnapshot();
                // onBeginDrag replaces the shape the shape widget uses for
                // painting and hit tests with an identical copy, which we
                // will modify - it will render that as if it were the real
                // one - that way we are not generating change events
                // for every mouse drag event, only for the final mouse-release,
                // at which point onEndDrag commits the change.
                shapeCopy = osw.onBeginDrag();
            });
            opListeners.onDragStarted(w.getLookup());
        }

        @Override
        public void onDrag(Widget w, Point2D original, Point2D current) {
            if (!uiState.shapesDraggable()) {
                return;
            }
            withShapeInfo(w, original, current, true, (e, osw) -> {
                // that's it, withShapeInfo updates the temporary shape
                // and revalidates the widget
            });
        }

        @Override
        public void onEndDrag(Widget w, Point2D original, Point2D current) {
            if (!uiState.shapesDraggable()) {
                return;
            }
            onDrag(w, original, current);
//            System.out.println("onEndDrag " + original + " / " + current);
            try {
                withShapeInfo(w, original, current, true, (e, osw) -> {
                    // Clears the temporary shape copy we used for
                    // rendering while dragging, and copies its vector
                    // into the original
                    osw.onEndDrag(true);
                    revalidateControlPoints(e);
                    many.syncOne(e);
                });
            } finally {
                clearDragState();
                cleanUp(w);
                opListeners.onDragCompleted(w.getLookup());
                uiState.setConnectorLinesVisible(true);
                uiState.setControlPointsVisible(true);
            }
        }

        private void cleanUp(Widget w) {
            w.revalidate(false);
            // Ensure dependencies are really revalidated - sometimes
            // they are not
            for (Dependency dep : w.getDependencies()) {
                dep.revalidateDependency();
            }
//            w.getScene().validate();
//            w.getScene().repaint();
        }

        @Override
        public void onCancelDrag(Widget w, Point2D original) {
            if (!uiState.shapesDraggable()) {
                return;
            }
            try {
                if (snapshot != null) {
                    withShapeInfo(w, original, null, false, (e, osw) -> {
                        snapshot.run();
                        osw.revalidate();
                        osw.getScene().validate();
                    });
                }
            } finally {
                clearDragState();
                cleanUp(w);
                opListeners.onDragCancelled(w.getLookup());
            }
        }

        private void clearDragState() {
            for (ControlPointWidget widge : restoreControlPointsOn) {
                widge.clearTempControlPoint();
            }
            restoreControlPointsOn.clear();
            snapshot = null;
            last = null;
            shapeCopy = null;
        }

        @Override
        public Point2D snap(Widget w, Point2D original, Point2D suggested) {
            SnapSettings snap = SnapSettings.getGlobal();
            if (!snap.isEnabled()) {
                return suggested;
            }
            Set<SnapKind> kinds = snap.getEnabledSnapKinds();
            if (kinds.isEmpty()) {
                return suggested;
            }
            // Don't need these for shape dragging,
            // and they don't make any sense here
            kinds.remove(SnapKind.CORNER);
            kinds.remove(SnapKind.ANGLE);
            kinds.remove(SnapKind.EXTENT);
            kinds.remove(SnapKind.LENGTH);

            Grid grid = Grid.getInstance();
            boolean gridEnabled = grid.isEnabled();

            int gridSize = gridEnabled ? grid.size() : 0;

            double dx = suggested.getX() - original.getX();
            double dy = suggested.getY() - original.getY();

            ShapeElement en = ((OneShapeWidget) w).realShape();
            ShapeControlPoint[] p = en.controlPoints(0, DesignWidgetManager::nullConsumer);
            Point2D.Double scratch = new Point2D.Double();
            for (ShapeControlPoint cp : p) {
                if (cp.isVirtual() || !cp.isValid()) {
                    continue;
                }
                // Create our proposed point - the control point's original
                // position, shifted by the x/y distance moved
                scratch.x = cp.getX() + dx;
                scratch.y = cp.getY() + dy;

                Point2D prev = null;
                ShapeControlPoint prevPoint = cp.previousPhysical();
                if (prevPoint != null) {
                    prev = new EqPointDouble(prevPoint.getX() + dx, prevPoint.getY() + dy);
                }
                Point2D next = null;
                ShapeControlPoint nextPoint = cp.nextPhysical();
                if (nextPoint != null) {
                    next = new EqPointDouble(nextPoint.getX() + dx, nextPoint.getY() + dy);
                }

                SnapPoints<ShapeSnapPointEntry> snapper = pts.get();

//                System.out.println("SNAP " + prev + " / " + scratch + " / " + next);
                // See if that snaps anywhere
                Point2D got = snapper.snapExclusive(prev, scratch,
                        next, gridSize, kinds, thresholds);
                if (got != scratch) {
                    double dx1 = scratch.x - got.getX();
                    double dy1 = scratch.y - got.getY();

                    double fx = suggested.getX() - dx1;
                    double fy = suggested.getY() - dy1;

                    return new EqPointDouble(fx, fy);
                }
            }
            return suggested;
        }
    }

    class ManyControlPointWidgetRotationHandler implements RotationHandler {

        private ShapeControlPoint currentProxyControlPoint;

        private void withShapeInfo(Widget w, Point2D original, TriConsumer<ShapeControlPoint, ManyControlPointsWidget, OneShapeWidget> c) {
            assert w instanceof ManyControlPointsWidget : "Unexpected widget type " + w.getClass().getName() + ": " + w;
            ShapeControlPoint e = w.getLookup().lookup(ShapeControlPoint.class);
            assert e != null : "No shape control point in lookup of " + w;
            if (!e.isValid()) {
//                System.err.println("Abort dragging invalid control point");
                abortAllDragOperations();
                return;
            }
            OneShapeWidget shapeWidget = widget.find(e.owner(), OneShapeWidget.class);
            assert shapeWidget != null : "No shape widget for owner " + e.owner()
                    + " of " + e.getClass().getName()
                    + " - " + e + " widget type "
                    + w.getClass().getName()
                    + " owner type " + e.owner().getClass().getName()
                    + " owner id " + Long.toString(e.owner().id(), 36)
                    + "(" + e.owner().id() + ")";

            ShapeElement el = shapeWidget.getLookup().lookup(ShapeElement.class);
            assert el != null : "No shape";

            ManyControlPointsWidget cpw = (ManyControlPointsWidget) w;
            c.apply(e, cpw, shapeWidget);
            shapeWidget.onRotated();
        }

        private AffineTransform angle(Point2D original, Point2D current, OneShapeWidget widget) {
            Centered cen = widget.realShape().item().as(Centered.class);
            assert cen != null;
            Point2D center = cen.center();
            double origAngle = Circle.angleOf(center, original);
            double currentAngle = Circle.angleOf(center, current);
            double diff = currentAngle - origAngle;
            return PooledTransform.getRotateInstance(Math.toRadians(diff),
                    center.getX(), center.getY(), null);
        }

        private void updateRotation(Point2D original, Point2D current, OneShapeWidget el) {
            ShapeElement temp = currentProxyControlPoint.owner();
            Shaped orig = el.realShape().item();
            AffineTransform xform = angle(original, current, el);
            Shaped rotated = (Shaped) orig.as(Transformable.class).copy(xform);
            PooledTransform.returnToPool(xform);
            assert el.realShape() != temp : "Have original, not temporary copy to edit";
            temp.setShape(rotated);
            el.revalidate();
        }

        final AffineTransform testRotateTransform = AffineTransform.getRotateInstance(Math.toRadians(90));

        @Override
        public boolean onPotentialDrag(Widget w, Point2D original) {
            ShapeControlPoint pt = w.getLookup().lookup(ShapeControlPoint.class);
            return pt != null
                    && pt.owner().item().is(Centered.class)
                    && pt.owner().item().is(Transformable.class)
                    && pt.owner().item().as(Transformable.class).canApplyTransform(testRotateTransform);
        }

        @Override
        public void onBeginDrag(Widget w, Point2D original, Point2D current) {
            if (!uiState.controlPointsDraggable()) {
                return;
            }
            ShapeControlPoint pt = w.getLookup().lookup(ShapeControlPoint.class);
            if (pt == null) {
                return;
            }
            withShapeInfo(w, original, (controlPoint, cpWidget, shapeWidget) -> {
                currentProxyControlPoint = shapeWidget.onBeginControlPointDrag(controlPoint);
                if (currentProxyControlPoint == null) {
                    abortAllDragOperations();
                    currentProxyControlPoint = shapeWidget.onBeginControlPointDrag(controlPoint);
                }
                assert currentProxyControlPoint != null : "Did not get a control point from " + shapeWidget;
                cpWidget.onBeginRotate(currentProxyControlPoint);
                updateRotation(original, current, shapeWidget);
                shapeWidget.onRotated();
                cpWidget.onRotate();
            });
            opListeners.onDragStarted(w.getLookup());
        }

        @Override
        public void onDrag(Widget w, Point2D original, Point2D current) {
            if (!uiState.controlPointsDraggable()) {
                return;
            }
//            setAngle(current, w);
            withShapeInfo(w, original, (controlPoint, cpWidget, shapeWidget) -> {
                assert currentProxyControlPoint != null : "Not currently rotating";
                updateRotation(original, current, shapeWidget);
                shapeWidget.onRotated();
                cpWidget.onRotate();
            });
        }

        private ShapeControlPoint clearProxy() {
            ShapeControlPoint proxy = currentProxyControlPoint;
            assert proxy != null : "No proxy";
            currentProxyControlPoint = null;
            return proxy;
        }

        @Override
        public void onEndDrag(Widget w, Point2D original, Point2D current) {
            if (!uiState.controlPointsDraggable()) {
                return;
            }
            withShapeInfo(w, original, (controlPoint, cpWidget, shapeWidget) -> {
                ShapeControlPoint proxy = clearProxy();
                cpWidget.onEndRotate(false);
                shapeWidget.onEndRotate(proxy, true);
                cpWidget.syncOne(shapeWidget.realShape());
            });
            opListeners.onDragCompleted(w.getLookup());
        }

        @Override
        public void onCancelDrag(Widget w, Point2D original) {
            clearProxy();
            if (!uiState.controlPointsDraggable()) {
                return;
            }
            withShapeInfo(w, original, (controlPoint, cpWidget, shapeWidget) -> {
                cpWidget.onEndRotate(true);
                shapeWidget.onCancelRotation();
            });
            opListeners.onDragCancelled(w.getLookup());
        }

    }

    class ManyControlPointWidgetDragHandler implements DragHandler {

        private ShapeControlPoint currentProxyControlPoint;

        private void clearDragState() {
            currentProxyControlPoint = null;
        }

        private void setPoint(Point2D current, Widget w) {
            Point loc = w.getLocation();
            currentProxyControlPoint.set(current.getX() - loc.x, current.getY() - loc.y);
            ((ManyControlPointsWidget) w).onDrag(current);

//            w.revalidate();
//            w.repaint();
        }

        private void withShapeInfo(Widget w, TriConsumer<ShapeControlPoint, ManyControlPointsWidget, OneShapeWidget> c) {
            assert w instanceof ManyControlPointsWidget : "Unexpected widget type " + w.getClass().getName() + ": " + w;
            ShapeControlPoint e = w.getLookup().lookup(ShapeControlPoint.class);
            assert e != null : "No shape control point in lookup of " + w;
            if (!e.isValid()) {
//                System.err.println("Abort dragging invalid control point");
                abortAllDragOperations();
                return;
            }
            OneShapeWidget shapeWidget = widget.find(e.owner(), OneShapeWidget.class);
            if (shapeWidget == null) {
                DesignWidgetManager.this.sync();
                // May have just been added?
                shapeWidget = widget.find(e.owner(), OneShapeWidget.class);
            }
            assert shapeWidget != null : "No shape widget for owner " + e.owner()
                    + " of " + e.getClass().getName()
                    + " - " + e + " widget type "
                    + w.getClass().getName()
                    + " owner type " + e.owner().getClass().getName()
                    + " owner id " + Long.toString(e.owner().id(), 36)
                    + "(" + e.owner().id() + ")";
            ManyControlPointsWidget cpw = (ManyControlPointsWidget) w;
            c.apply(e, cpw, shapeWidget);
            shapeWidget.onControlPointMove(e);
        }

        @Override
        public boolean onPotentialDrag(Widget w, Point2D original) {
            ShapeControlPoint pt = w.getLookup().lookup(ShapeControlPoint.class);
            if (pt != null && !pt.isEditable()) {
                return false;
            }
            return pt != null;
        }

        @Override
        public void onBeginDrag(Widget w, Point2D original, Point2D current) {
            if (!uiState.controlPointsDraggable()) {
                return;
            }
            ShapeControlPoint pt = w.getLookup().lookup(ShapeControlPoint.class);
            if (pt != null && !pt.isEditable()) {
                return;
            }
            withShapeInfo(w, (controlPoint, cpWidget, shapeWidget) -> {
                currentProxyControlPoint = shapeWidget.onBeginControlPointDrag(controlPoint);
                if (currentProxyControlPoint == null) {
                    abortAllDragOperations();
                    currentProxyControlPoint = shapeWidget.onBeginControlPointDrag(controlPoint);
                }
                assert currentProxyControlPoint != null : "Did not get a control point from " + shapeWidget;
                cpWidget.onBeginDrag(currentProxyControlPoint);
                setPoint(current, w);
            });
            opListeners.onDragStarted(w.getLookup());
        }

        @Override
        public void onDrag(Widget w, Point2D original, Point2D current) {
            if (!uiState.controlPointsDraggable()) {
                return;
            }
            setPoint(current, w);
        }

        private ShapeControlPoint clearProxy() {
            ShapeControlPoint proxy = currentProxyControlPoint;
            assert proxy != null : "No proxy";
            currentProxyControlPoint = null;
            return proxy;
        }

        @Override
        public void onEndDrag(Widget w, Point2D original, Point2D current) {
            if (!uiState.controlPointsDraggable()) {
                return;
            }
            withShapeInfo(w, (controlPoint, cpWidget, shapeWidget) -> {
                ShapeControlPoint proxy = clearProxy();
                cpWidget.onEndDrag(false);
                shapeWidget.onEndControlPointDrag(proxy, true);
            });
            opListeners.onDragCompleted(w.getLookup());
        }

        @Override
        public void onCancelDrag(Widget w, Point2D original) {
            if (!uiState.controlPointsDraggable()) {
                return;
            }
            clearProxy();
            withShapeInfo(w, (controlPoint, cpWidget, shapeWidget) -> {
                cpWidget.onEndDrag(true);
                shapeWidget.onCancelControlPointDrag();
            });
            opListeners.onDragCancelled(w.getLookup());
        }

        @Override
        public Point2D snap(Widget w, Point2D original, Point2D suggested) {
            SnapSettings snap = SnapSettings.getGlobal();
            if (!snap.isEnabled()) {
                return suggested;
            }
            Set<SnapKind> kinds = snap.getEnabledSnapKinds();
            if (kinds.isEmpty()) {
                return suggested;
            }
            switch (currentProxyControlPoint.kind()) {
                case CUBIC_CONTROL_POINT:
                case QUADRATIC_CONTROL_POINT:
                case EDGE_HANDLE:
                    // Remove things that don't make sense - angles relative to
                    // cubic and quadratic would show angle indicators over
                    // nothing, and a corner is nonsensical in the middle of
                    // a line
                    kinds.remove(SnapKind.EXTENT);
                    kinds.remove(SnapKind.CORNER);
                    break;
                case RADIUS:
                    // An angle relative to a radius means nothing
                    kinds.retainAll(EnumSet.of(SnapKind.DISTANCE, SnapKind.LENGTH, SnapKind.GRID));
                    break;
            }

            if (kinds.isEmpty()) {
                return original;
            }

            Grid grid = Grid.getInstance();
            int gridSize = grid.isEnabled() ? grid.size() : 0;
            if (currentProxyControlPoint != null) {
                ShapeControlPoint prev = currentProxyControlPoint.previousPhysical();
                ShapeControlPoint next = currentProxyControlPoint.nextPhysical();
                // For angle and extent matching to be correct, we need to
                // order the prev and next points such that we have an interior,
                // not exterior angle, or we'll match the outsides of things
                LineVector vect = LineVector.of(prev.toPoint(), suggested, next.toPoint());
                Shape owner = currentProxyControlPoint.owner().shape();
                if (vect.sample(owner::contains) != 3) {
                    vect = vect.inverse();
                }

                if (prev != null && next != null && prev.isValid() && next.isValid()) {
                    Point2D result = pts.get().snapExclusive(vect.trailingPoint(),
                            suggested, vect.leadingPoint(),
                            gridSize,
                            snap.getEnabledSnapKinds(), thresholds);
                    if (result == null) {
                        snapLayer.snapListener().onSnap(null, null);
                    }
                    return result;
                }
            }
            Point2D result = pts.get().snapExclusive(null, suggested, null,
                    gridSize,
                    snap.getEnabledSnapKinds(), thresholds);
            if (result == null) {
                snapLayer.snapListener().onSnap(null, null);
            }
            return result;
        }
    }

    private static void nullConsumer(ControlPoint c) {
        // fetching control points takes a callback that is notified when the
        // control point is moved - when dragging a shape, not its control
        // points, the callback will never be called - we just need this to
        // fetch them (probably should have a variant that doesn't take a
        // callback)
    }

}
