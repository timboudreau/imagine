package org.imagine.vector.editor.ui.tools;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import javax.swing.JComponent;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
class ViewL extends MouseAdapter implements MouseMotionListener {

    private Point2D.Double lastPoint = new Point2D.Double();
    private int lastEventType;

    public static Point2D.Double lastPoint(Widget widget) {
        return get(widget)._lastPoint();
    }

    public static int lastEventType(Widget widget) {
        return get(widget)._lastEventType();
    }

    private Point2D.Double _lastPoint() {
        return lastPoint;
    }

    private int _lastEventType() {
        return lastEventType;
    }

    private void setLocation(MouseEvent e) {
        lastPoint.setLocation(e.getPoint());
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        setLocation(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        setLocation(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        setLocation(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        setLocation(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        setLocation(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        setLocation(e);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        setLocation(e);
    }

    static void attach(Scene scene) {
        ViewL l = get(scene);
        JComponent c = scene.getView();
        c.addMouseListener(l);
        c.addMouseMotionListener(l);
    }

    static void detach(Scene scene) {
        JComponent c = scene.getView();
        ViewL vl = (ViewL) c.getClientProperty("ViewL");
        if (vl != null) {
            c.removeMouseListener(vl);
            c.removeMouseMotionListener(vl);
            c.putClientProperty("ViewL", null);
        }
    }

    static ViewL get(Widget widget) {
        JComponent c = widget.getScene().getView();
        ViewL v = (ViewL) c.getClientProperty("ViewL");
        if (v == null) {
            v = new ViewL();
            c.putClientProperty("ViewL", v);
            c.addMouseListener(v);
            c.addMouseMotionListener(v);
        }
        return v;
    }
}
