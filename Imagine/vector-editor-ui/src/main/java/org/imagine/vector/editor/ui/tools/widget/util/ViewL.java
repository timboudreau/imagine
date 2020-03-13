package org.imagine.vector.editor.ui.tools.widget.util;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import javax.swing.JComponent;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Scene.SceneListener;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
public final class ViewL extends MouseAdapter implements MouseMotionListener {

    private final Point lastPoint = new Point();
    private int lastEventType;

    public static Point2D.Double lastPoint(Widget widget) {
        Point p = get(widget)._lastPoint();
        widget.getScene().convertViewToScene(p);
        return new Point2D.Double(p.x, p.y);
    }

    public static int lastEventType(Widget widget) {
        return get(widget)._lastEventType();
    }

    private Point _lastPoint() {
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

    public static void attach(Scene scene) {
        if (scene.getView() != null) {
            doAttach(scene);
        } else {
            scene.addSceneListener(new SceneL(scene));
        }
    }

    private static void doAttach(Scene scene) {
        ViewL l = get(scene);
        JComponent c = scene.getView();
        c.addMouseListener(l);
        c.addMouseMotionListener(l);

    }

    static class SceneL implements SceneListener {

        private final Scene scene;

        public SceneL(Scene scene) {
            this.scene = scene;
        }

        @Override
        public void sceneRepaint() {
        }

        @Override
        public void sceneValidating() {
        }

        @Override
        public void sceneValidated() {
            if (scene.getView() != null) {
                ViewL.doAttach(scene);
                scene.removeSceneListener(this);
            }
        }

    }

    public static void detach(Scene scene) {
        JComponent c = scene.getView();
        ViewL vl = (ViewL) c.getClientProperty(ViewL.class.getName());
        if (vl != null) {
            c.removeMouseListener(vl);
            c.removeMouseMotionListener(vl);
            c.putClientProperty(ViewL.class.getName(), null);
        }
    }

    static ViewL get(Widget widget) {
        JComponent c = widget.getScene().getView();
        if (c == null) {
            return null;
        }
        ViewL v = (ViewL) c.getClientProperty(ViewL.class.getName());
        if (v == null) {
            v = new ViewL();
            c.putClientProperty(ViewL.class.getName(), v);
            c.addMouseListener(v);
            c.addMouseMotionListener(v);
        }
        return v;
    }
}
