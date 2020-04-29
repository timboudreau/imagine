/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget.actions;

import java.awt.Point;
import java.awt.geom.Point2D;
import org.imagine.vector.editor.ui.tools.widget.util.ViewL;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.widget.Widget;

/**
 * A simplified MoveAction which deals solely in scene coordinates, instead of
 * requiring multiple (potentially lossy) coordinate spaces conversions in and
 * out of the widgets zero-based client bounds. Drag operations are initiated
 * only on the first drag event where the mouse position is not the mouse
 * pressed position, to avoid interfering with click processing. Updating is
 * accomplished via a DragHandler instance in the widget's Lookup.
 *
 * @author Tim Boudreau
 */
public class MoveInSceneCoordinateSpaceAction extends WidgetAction.LockedAdapter {

    private DragState state;

    public static Point2D scenePoint(Widget widget) {
        Point2D.Double scenePoint = ViewL.lastPoint(widget);
        // Maybe scene bounds x/y would do it?
        Point sceneLoc = widget.getScene().getLocation();
        double zoom = widget.getScene().getZoomFactor();
        double inv = 1D / zoom;
        scenePoint.x -= sceneLoc.x;
        scenePoint.y -= sceneLoc.y;

        scenePoint.x *= inv;
        scenePoint.y *= inv;
        return scenePoint;
    }

    private boolean isUnknownModifierKeys(WidgetMouseEvent evt) {
        return evt.isAltDown() || evt.isMetaDown();
    }

    @Override
    public State mousePressed(Widget widget, WidgetMouseEvent event) {
        if (event.isPopupTrigger() || isUnknownModifierKeys(event)) {
            abort();
            return State.REJECTED;
        }
        if (state != null) {
            abort();
//            state.resign();
            state = null;
        }
        DragHandler handler = widget.getLookup().lookup(DragHandler.class);
        if (handler == null) {
            return WidgetAction.State.REJECTED;
        }
        Point2D scenePoint = scenePoint(widget);
        boolean ok = handler.onPotentialDrag(widget, scenePoint);
        if (ok) {
            state = new DragState(widget, scenePoint(widget), handler);
            return State.createLocked(widget, this);
        }
        return State.REJECTED;
    }

    @Override
    public State mouseReleased(Widget widget, WidgetMouseEvent event) {
        if (event.isPopupTrigger() || isUnknownModifierKeys(event)) {
            return State.REJECTED;
        }
        if (state != null && widget == state.widget) {
            state.commit(scenePoint(widget));
            state = null;
            return WidgetAction.State.CONSUMED;
        } else if (state != null) {
            abort();
        }
        return WidgetAction.State.REJECTED;
    }

    @Override
    public State mouseDragged(Widget widget, WidgetMouseEvent event) {
        if (isUnknownModifierKeys(event)) {
            return State.REJECTED;
        }
        if (state != null && widget == state.widget) {
            boolean restrictHorizontal = event.isShiftDown();
            boolean restrictVertical = event.isControlDown();
            boolean dragInProgress = state.dragged(scenePoint(widget),
                    restrictVertical, restrictHorizontal);
//            if (dragInProgress) {
//            return State.createLocked(widget, this);
//            } else {
                return State.CONSUMED;
//            }
        }
        return WidgetAction.State.REJECTED;
    }

    @Override
    protected boolean isLocked() {
        return state != null;
    }

    public void abort() {
        if (state != null) {
            state.resign();
        }
    }

    private class DragState {

        private final Widget widget;
        private final Point2D startPoint;
        private final DragHandler handler;
        private Point2D lastDragPoint;

        public DragState(Widget widget, Point2D startPoint, DragHandler handler) {
            this.widget = widget;
            this.startPoint = startPoint;
            this.handler = handler;
        }

        private void initDrag(Point2D to) {
            handler.onBeginDrag(widget, startPoint, to);
        }

        @Override
        public String toString() {
            return "DS-" + Integer.toString(System.identityHashCode(this))
                    + "(" + widget + ")";
        }

        private boolean dragged(Point2D to, boolean restrictVertical, boolean restrictHorizontal) {
            if (lastDragPoint == null && (to.getX() != startPoint.getX() || to.getY() != startPoint.getY())) {
                initDrag(to);
                lastDragPoint = handler.snap(widget, startPoint, to);
                return true;
            } else if (lastDragPoint != null) {
                double oldX = lastDragPoint.getX();
                double oldY = lastDragPoint.getY();
                lastDragPoint = handler.snap(widget, startPoint, to);
                if (restrictVertical) {
                    lastDragPoint.setLocation(lastDragPoint.getX(), oldY);
                } else if (restrictHorizontal) {
                    lastDragPoint.setLocation(oldX, lastDragPoint.getY());
                }
                handler.onDrag(widget, startPoint, lastDragPoint);
                return true;
            } else {
                return false;
            }
        }

        private void resign() {
            if (lastDragPoint != null) {
                handler.onCancelDrag(widget, startPoint);
            }
            lastDragPoint = null;
            if (MoveInSceneCoordinateSpaceAction.this.state == this) {
                MoveInSceneCoordinateSpaceAction.this.state = null;
            }
        }

        private void commit(Point2D scenePoint) {
            if (lastDragPoint != null && (startPoint.getX() != lastDragPoint.getX()
                    || startPoint.getY() != lastDragPoint.getY())) {
                handler.onEndDrag(widget, startPoint, scenePoint);
                lastDragPoint = null;
            } else if (lastDragPoint != null) {
                resign();
                lastDragPoint = null;
            }
        }
    }
}
