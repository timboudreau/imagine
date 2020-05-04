package org.imagine.vector.editor.ui.tools.widget.actions;

import java.awt.geom.Point2D;
import static org.imagine.vector.editor.ui.tools.widget.actions.MoveInSceneCoordinateSpaceAction.scenePoint;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
public class RotateInSceneCoordinateSpaceAction extends WidgetAction.LockedAdapter {

    RotateState state;

    private boolean isUnknownModifierKeys(WidgetMouseEvent evt) {
        return !(evt.isAltDown() || evt.isShiftDown()) || evt.isControlDown();
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
        RotationHandler handler = widget.getLookup().lookup(RotationHandler.class);
        if (handler == null) {
            return WidgetAction.State.REJECTED;
        }
        Point2D scenePoint = scenePoint(widget);
        boolean ok = handler.onPotentialDrag(widget, scenePoint);
        if (ok) {
            state = new RotateState(widget, scenePoint(widget), handler);
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
//            return WidgetAction.State.CONSUMED;
            return State.createLocked(widget, this);
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
            state.dragged(scenePoint(widget));
            return State.createLocked(widget, this);
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

    private class RotateState {

        private final Widget widget;
        private final Point2D startPoint;
        private final RotationHandler handler;
        private Point2D lastDragPoint;

        public RotateState(Widget widget, Point2D startPoint, RotationHandler handler) {
            this.widget = widget;
            this.startPoint = startPoint;
            this.handler = handler;
        }

        private void initDrag(Point2D to) {
            handler.onBeginDrag(widget, startPoint, to);
        }

        @Override
        public String toString() {
            return "RS-" + Integer.toString(System.identityHashCode(this))
                    + "(" + widget + ")";
        }

        private boolean dragged(Point2D to) {
            if (lastDragPoint == null && (to.getX() != startPoint.getX() || to.getY() != startPoint.getY())) {
                initDrag(to);
                lastDragPoint = to;
                return true;
            } else if (lastDragPoint != null) {
                lastDragPoint = to;
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
            if (RotateInSceneCoordinateSpaceAction.this.state == this) {
                RotateInSceneCoordinateSpaceAction.this.state = null;
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
