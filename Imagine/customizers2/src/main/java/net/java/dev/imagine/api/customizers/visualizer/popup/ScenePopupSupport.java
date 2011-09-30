package net.java.dev.imagine.api.customizers.visualizer.popup;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import javax.swing.JComponent;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.action.WidgetAction.State;
import org.netbeans.api.visual.action.WidgetAction.WidgetMouseEvent;
import org.netbeans.api.visual.action.WidgetAction.WidgetMouseWheelEvent;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Scene.SceneListener;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Tim Boudreau
 */
public class ScenePopupSupport {

    private final Scene scene;
    private final PopupCallback callback;
    private final PopupFactory fac = new PopupFactory();

    public ScenePopupSupport(final Scene scene, PopupCallback callback) {
        this.scene = scene;
        this.callback = new WrapPopupCallback(callback);
        //Hack the popup size (important for animated zoom-on-show)
        scene.addSceneListener(new SceneListener() {

            @Override
            public void sceneRepaint() {
            }

            @Override
            public void sceneValidating() {
                
            }

            @Override
            public void sceneValidated() {
                JComponent v = scene.getView();
                if (v != null) {
                    Dimension d = v.getPreferredSize();
                    if (!d.equals(v.getSize())) {
                        Window w = (Window) SwingUtilities.getAncestorOfClass(Window.class, v);
                        if (w != null && w.getClass().getSimpleName().equals("HeavyWeightWindow") && !w.getSize().equals(d)) {
                            w.setSize(d);
                        } else {
                            //Try to find the medium weight popup, if any, and
                            //force its size
                            Component x = v;
                            Component hww = null;
                            while (x != null) {
                                if ("javax.swing.PopupFactory$MediumWeightPopup$MediumWeightComponent".equals(x.getClass().getName())) {
                                    hww = x;
                                }
                                x = x.getParent();
                            }
                            if (hww != null && !hww.getSize().equals(d)) {
                                hww.setSize(d);
                                v.setSize(d);
                                hww.doLayout();
                                hww.repaint(0);
                            }
                        }
                    }
                }
            }
        });
        
    }

    public WidgetAction createAction() {
        class A extends WidgetAction.Adapter {

            private Popup popup;
            ML ml = new ML();
            class ML extends MouseAdapter {

                @Override
                public void mouseReleased(MouseEvent e) {
                    Component c = (Component) e.getSource();
                    c.removeMouseListener(this);
                    A.this.mouseReleased((Widget) null, (WidgetMouseEvent) null);
                }
                
            };

            @Override
            public State mousePressed(Widget widget, WidgetMouseEvent event) {
                if (popup != null) {
                    popup.hide();
                }
                scene.validate();
                widget.getScene().getView().addMouseListener(ml);
                popup = createPopup(widget, event);
                popup.show();
                return State.createLocked(widget, this);
            }

            @Override
            public State mouseReleased(Widget widget, WidgetMouseEvent event) {
                if (popup != null) {
                    popup.hide();
                    popup = null;
                }
                return State.CONSUMED;
            }

            @Override
            public State mouseWheelMoved(Widget widget, WidgetMouseWheelEvent event) {
                double curr = scene.getZoomFactor();
                if (curr >= 1) {
                    curr += event.getWheelRotation();
                } else {
                    double off = 0.1 * event.getWheelRotation();
                    curr = Math.max(0.1, curr + off);
                }
                scene.setZoomFactor(curr);
                scene.validate();
                return State.CONSUMED;
            }

            @Override
            public State mouseDragged(Widget widget, WidgetMouseEvent event) {
                System.out.println("dragged " + event.getPoint());
                JComponent v = scene.getView();
                if (v != null && v.isShowing()) {
                    Point p = event.getPoint();
                    p = widget.convertLocalToScene(p);
                    p = widget.getScene().convertSceneToView(p);
                    SwingUtilities.convertPointToScreen(p, widget.getScene().getView());
                    SwingUtilities.convertPointFromScreen(p, v);
                    p = scene.convertViewToScene(p);
                    for (Widget w : scene.getChildren()) {
                        Rectangle r = w.convertLocalToScene(w.getBounds());
                        if (r.contains(p)) {
                            callback.widgetSelected(w);
                            return State.CONSUMED;
                        }
                    }
                    callback.widgetSelected(null);
                    return State.REJECTED;
                }

                return super.mouseDragged(widget, event);
            }
        };
        return new A();
    }

    private Popup createPopup(Widget w, WidgetMouseEvent evt) {
        callback.onShowPopup();
        JComponent vw = scene.getView();
        if (vw == null) {
            vw = scene.createView();
        }
        PopupFactory pf = fac;
        Scene sourceScene = w.getScene();
        assert sourceScene != this.scene;
        Point p = evt.getPoint();
        p = w.convertLocalToScene(p);
        p = sourceScene.convertSceneToView(p);
        SwingUtilities.convertPointToScreen(p, sourceScene.getView());
        //use a null owner to force a heavyweight popup - medium-weight popups
        //flash when resized because they force a full repaint down to the
        //root pane since they are not JComponents
        Popup popup = pf.getPopup(null, vw, p.x, p.y);
        return new WrapPopup(popup);
    }

    class WrapPopup extends Popup {

        private final Popup delegate;

        WrapPopup(Popup delegate) {
            this.delegate = delegate;
        }

        public void show() {
            scene.setZoomFactor(0.1);
            scene.getSceneAnimator().animateZoomFactor(1.0);
            delegate.show();
            scene.validate();
        }

        public void hide() {
            delegate.hide();
        }
    }

    public interface PopupCallback {

        public void widgetSelected(Widget widget);

        public void onShowPopup();
    }

    static final class WrapPopupCallback implements PopupCallback {

        private Reference<Widget> last;
        private final PopupCallback delegate;

        public WrapPopupCallback(PopupCallback delegate) {
            this.delegate = delegate;
        }

        @Override
        public void widgetSelected(Widget widget) {
            Widget old = last == null ? null : last.get();
            if (old != widget) {
                if (widget == null) {
                    last = null;
                } else {
                    last = new WeakReference<Widget>(widget);
                }
                delegate.widgetSelected(widget);
            }
        }

        @Override
        public void onShowPopup() {
            delegate.onShowPopup();
        }
    }
}
