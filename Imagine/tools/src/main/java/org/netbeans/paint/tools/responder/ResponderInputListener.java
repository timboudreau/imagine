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

//    MouseEvent lastMe = null;
//    Exception lm = null;

    @Override
    public void mouseClicked(double x, double y, MouseEvent e) {
//        if (isSameMouseEvent(lastMe, e)) {
//            throw new IllegalStateException("Same click event sent twice: " + x + ", " + y, lm);
//        }
//        lm = new Exception("MClick " + x + ", " + y);
//        lastMe = e;
        if (tool.updateHandler(tool.currentHandler().onClick(x, y, e))) {
            e.consume();
        }
    }

    @Override
    public void mousePressed(double x, double y, MouseEvent e) {
        if (tool.updateHandler(tool.currentHandler().onPress(x, y, e))) {
            e.consume();
        }
    }

    @Override
    public void mouseReleased(double x, double y, MouseEvent e) {
        if (tool.updateHandler(tool.currentHandler().onRelease(x, y, e))) {
            e.consume();
        }
    }

    @Override
    public void mouseDragged(double x, double y, MouseEvent e) {
        if (tool.updateHandler(tool.currentHandler().onDrag(x, y, e))) {
            e.consume();
        }
    }

    @Override
    public void mouseMoved(double x, double y, MouseEvent e) {
        if (tool.updateHandler(tool.currentHandler().onMove(x, y, e))) {
            e.consume();
        }
    }

    @Override
    public void mouseEntered(double x, double y, MouseEvent e) {
        if (tool.updateHandler(tool.currentHandler().onEnter(x, y, e))) {
            e.consume();
        }
    }

    @Override
    public void mouseExited(double x, double y, MouseEvent e) {
        if (tool.updateHandler(tool.currentHandler().onExit(x, y, e))) {
            e.consume();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (tool.updateHandler(tool.currentHandler().onTyped(e))) {
            e.consume();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (tool.updateHandler(tool.currentHandler().onKeyPress(e))) {
            e.consume();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (tool.updateHandler(tool.currentHandler().onKeyRelease(e))) {
            e.consume();
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (tool.updateHandler(tool.currentHandler().onWheel(e))) {
            e.consume();
        }
    }
}
