package org.netbeans.paint.tools.responder;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import net.dev.java.imagine.api.tool.aspects.ScalingMouseListener;

/**
 * Listener that is forwarded input events for a ResponderTool, and invokes its
 * update handler and consumes the events.
 *
 * @author Tim Boudreau
 */
final class ResponderInputListener implements ScalingMouseListener, KeyListener, MouseWheelListener {

    private final ResponderTool tool;

    ResponderInputListener(ResponderTool tool) {
        this.tool = tool;
    }

    static boolean isSameMouseEvent(MouseEvent a, MouseEvent b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.getWhen() == b.getWhen() && a.getX() == b.getX()
                && a.getY() == b.getY() && a.getID() == b.getID()) {
            return true;
        }
        return false;
    }

    @Override
    public void mouseClicked(double x, double y, MouseEvent e) {
        Responder h = tool.currentHandler();
        h.onBeforeHandleInputEvent(e);
        if (tool.updateHandler(h.onClick(x, y, e))) {
            e.consume();
        }
        // Note we update the hover point AFTER processing the event,
        // so drag handlers have the old point to compare with when they
        // process the event; and it gets called on the handler that was
        // passed the event, WHETHER OR NOT the handler was replaced as
        // a result of the call
        h.onAnyMouseEvent(x, y, e);
    }

    @Override
    public void mousePressed(double x, double y, MouseEvent e) {
        Responder h = tool.currentHandler();
        h.onBeforeHandleInputEvent(e);
        if (tool.updateHandler(h.onPress(x, y, e))) {
            e.consume();
        }
        h.onAnyMouseEvent(x, y, e);
    }

    @Override
    public void mouseReleased(double x, double y, MouseEvent e) {
        Responder h = tool.currentHandler();
        h.onBeforeHandleInputEvent(e);
        if (tool.updateHandler(h.onRelease(x, y, e))) {
            e.consume();
        }
        h.onAnyMouseEvent(x, y, e);
    }

    @Override
    public void mouseDragged(double x, double y, MouseEvent e) {
        Responder h = tool.currentHandler();
        h.onBeforeHandleInputEvent(e);
        if (tool.updateHandler(h.onDrag(x, y, e))) {
            e.consume();
        }
        h.onAnyMouseEvent(x, y, e);
    }

    @Override
    public void mouseMoved(double x, double y, MouseEvent e) {
        Responder h = tool.currentHandler();
        h.onBeforeHandleInputEvent(e);
        if (tool.updateHandler(h.onMove(x, y, e))) {
            e.consume();
        }
        h.onAnyMouseEvent(x, y, e);
    }

    @Override
    public void mouseEntered(double x, double y, MouseEvent e) {
        Responder h = tool.currentHandler();
        h.onBeforeHandleInputEvent(e);
        if (tool.updateHandler(h.onEnter(x, y, e))) {
            e.consume();
        }
        h.onAnyMouseEvent(x, y, e);
    }

    @Override
    public void mouseExited(double x, double y, MouseEvent e) {
        Responder h = tool.currentHandler();
        h.onBeforeHandleInputEvent(e);
        if (tool.updateHandler(h.onExit(x, y, e))) {
            e.consume();
        }
        h.onAnyMouseEvent(x, y, e);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        Responder h = tool.currentHandler();
        h.onBeforeHandleInputEvent(e);
        if (tool.updateHandler(h.onTyped(e))) {
            e.consume();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        Responder h = tool.currentHandler();
        h.onBeforeHandleInputEvent(e);
        if (tool.updateHandler(h.onKeyPress(e))) {
            e.consume();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        Responder h = tool.currentHandler();
        h.onBeforeHandleInputEvent(e);
        if (tool.updateHandler(h.onKeyRelease(e))) {
            e.consume();
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        Responder h = tool.currentHandler();
        h.onBeforeHandleInputEvent(e);
        if (tool.updateHandler(h.onWheel(e))) {
            e.consume();
        }
    }
}
