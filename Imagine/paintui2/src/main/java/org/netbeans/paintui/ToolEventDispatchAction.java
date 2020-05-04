package org.netbeans.paintui;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.util.function.Supplier;
import net.dev.java.imagine.api.tool.aspects.ScalingMouseListener;
import net.java.dev.imagine.spi.image.LayerImplementation;
import net.java.dev.imagine.ui.common.PositionStatusLineElementProvider;
import org.imagine.editor.api.ContextLog;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.widget.Widget;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
class ToolEventDispatchAction extends WidgetAction.Adapter {

    private final PositionStatusLineElementProvider pslep = Lookup.getDefault().lookup(PositionStatusLineElementProvider.class);
    private final PictureScene scene;
    private static final ContextLog ALOG = ContextLog.get("toolactions");

    ToolEventDispatchAction(PictureScene scene) {
        this.scene = scene;
    }

    <T> T toolAs(Class<T> what) {
        return scene.toolAs(what);
    }

    private MouseEvent toMouseEvent(WidgetMouseEvent evt, int awtId) {
        Component source = scene.getView();
        Point p = evt.getPoint();
        //            if (!(activeTool instanceof NonPaintingTool)) {
        //                Rectangle activeLayerBounds = picture.getActiveLayer().getBounds();
        //                p.translate(-activeLayerBounds.x, -activeLayerBounds.y);
        //            }
        return new MouseEvent(source, awtId, evt.getWhen(), evt.getModifiers(), p.x, p.y, evt.getClickCount(), evt.isPopupTrigger(), evt.getButton());
    }

    private MouseWheelEvent toMouseWheelEvent(WidgetMouseWheelEvent evt) {
        Component source = scene.getView();
        Point p = evt.getPoint();
        return new MouseWheelEvent(source, MouseWheelEvent.MOUSE_WHEEL, evt.getWhen(), evt.getModifiers(), p.x, p.y, evt.getClickCount(), evt.isPopupTrigger(), evt.getScrollType(), evt.getScrollAmount(), evt.getWheelRotation());
    }

    private KeyEvent toKeyEvent(WidgetKeyEvent evt, int awtId) {
        Component source = scene.getView();
        return new KeyEvent(source, awtId, evt.getWhen(), evt.getModifiers(), evt.getKeyCode(), evt.getKeyChar(), evt.getKeyLocation());
    }

    State dispatch(Widget widget, WidgetKeyEvent evt, int awtId) {
        KeyListener kl = toolAs(KeyListener.class);
        if (kl != null && scene.picture().getActiveLayer() != null) {
            return dispatchToTool(toKeyEvent(evt, awtId), kl);
        }
        return State.REJECTED;
    }

    State dispatch(Widget widget, WidgetMouseEvent evt, int awtId) {
        Point pt = evt.getPoint();
        pslep.setStatus(new StringBuilder(Integer.toString(pt.x)).append(',').append(pt.y).toString());
        if (scene.picture().getActiveLayer() != null) {
            MouseListener ml = toolAs(MouseListener.class);
            //                if (ml != null) {
            return dispatchToTool(widget, toMouseEvent(evt, awtId), ml);
            //                }
        }
        return State.REJECTED;
    }

    private static String evtName(MouseEvent evt) {
        switch (evt.getID()) {
            case MouseEvent.MOUSE_PRESSED:
                return "press";
            case MouseEvent.MOUSE_RELEASED:
                return "release";
            case MouseEvent.MOUSE_CLICKED:
                return "click";
            case MouseEvent.MOUSE_DRAGGED:
                return "drag";
            case MouseEvent.MOUSE_MOVED:
                return "move";
            case MouseEvent.MOUSE_ENTERED:
                return "enter";
            case MouseEvent.MOUSE_EXITED:
                return "exit";
            default:
                return "unknown-" + evt.getID();
        }
    }

    private State dispatchToTool(Widget target, MouseEvent event, MouseListener ml) {
        ALOG.log(() -> "dispatch mouse " + evtName(event) + " to " + target);
        MouseMotionListener mml = toolAs(MouseMotionListener.class);
        ScalingMouseListener sml = toolAs(ScalingMouseListener.class);
        // Prefer ScalingMouseListener if both are present
        if (sml == null) {
            switch (event.getID()) {
                case MouseEvent.MOUSE_PRESSED:
                    if (ml != null) {
                        ml.mousePressed(event);
                    }
                    break;
                case MouseEvent.MOUSE_RELEASED:
                    if (ml != null) {
                        ml.mouseReleased(event);
                    }
                    break;
                case MouseEvent.MOUSE_CLICKED:
                    if (ml != null) {
                        ml.mouseClicked(event);
                    }
                    break;
                case MouseEvent.MOUSE_ENTERED:
                    if (ml != null) {
                        ml.mouseEntered(event);
                    }
                    break;
                case MouseEvent.MOUSE_EXITED:
                    if (ml != null) {
                        ml.mouseExited(event);
                    }
                    break;
                case MouseEvent.MOUSE_MOVED:
                    if (mml != null) {
                        mml.mouseMoved(event);
                    }
                    break;
                case MouseEvent.MOUSE_DRAGGED:
                    if (mml != null) {
                        mml.mouseDragged(event);
                    }
                    break;
            }
        } else {
            Point2D.Double pt = ViewL.lastPoint(target);
            switch (event.getID()) {
                case MouseEvent.MOUSE_PRESSED:
                    sml.mousePressed(pt.x, pt.y, event);
                    break;
                case MouseEvent.MOUSE_RELEASED:
                    sml.mouseReleased(pt.x, pt.y, event);
                    break;
                case MouseEvent.MOUSE_CLICKED:
                    sml.mouseClicked(pt.x, pt.y, event);
                    break;
                case MouseEvent.MOUSE_ENTERED:
                    sml.mouseEntered(pt.x, pt.y, event);
                    break;
                case MouseEvent.MOUSE_EXITED:
                    sml.mouseExited(pt.x, pt.y, event);
                    break;
                case MouseEvent.MOUSE_MOVED:
                    sml.mouseMoved(pt.x, pt.y, event);
                    break;
                case MouseEvent.MOUSE_DRAGGED:
                    sml.mouseDragged(pt.x, pt.y, event);
                    break;
            }
        }
        if (event.isConsumed() && scene.getFocusedWidget() != target) {
            // Ensure focus so key events get dispatched
            //                scene.setFocusedWidget(target);
        }
        return event.isConsumed() ? State.CONSUMED : State.REJECTED;
    }

    private State dispatchToTool(KeyEvent ke, KeyListener kl) {
        switch (ke.getID()) {
            case KeyEvent.KEY_PRESSED:
                kl.keyPressed(ke);
                break;
            case KeyEvent.KEY_RELEASED:
                kl.keyReleased(ke);
                break;
            case KeyEvent.KEY_TYPED:
                kl.keyTyped(ke);
                break;
        }
        return ke.isConsumed() ? State.CONSUMED : State.REJECTED;
    }

    State ifActiveLayer(Widget widget, Supplier<State> run) {
        return ifActiveLayer(false, widget, run);
    }

    State ifActiveLayer(boolean log, Widget widget, Supplier<State> run) {
        // If the current tool has installed its own layer, let that layer
        // handle input events
        if (scene.activeLayerWidget().isLayersWidgetActive()) {
            return State.REJECTED;
        }
        Widget w = targetWidget(widget);
        if (w instanceof OneLayerWidget) {
            OneLayerWidget olw = (OneLayerWidget) w;
            //                PictureScene.this
            LayerImplementation activeLayer = scene.picture().getActiveLayer();
            if (activeLayer != null) {
                if (olw.layer == activeLayer) {
                    State result = run.get();
                    if (log) {
                        ALOG.log(() -> "DID dispatch to " + w + " result " + result);
                    }
                    return result;
                } else {
                    if (log) {
                        ALOG.log(() -> "Not active layer, no dispatch to " + ((OneLayerWidget) w).layer);
                        ALOG.log(() -> "  focused widget: " + scene.getFocusedWidget());
                        if (scene.getFocusedWidget() != null) {
                            ALOG.log(() -> "    with actions: " + scene.getFocusedWidget().getActions().getActions());
                        }
                    }
                }
            } else {
                if (log) {
                    ALOG.log(() -> "No active layer.");
                }
            }
        } else {
            if (log) {
                ALOG.log(() -> "wrong widget type " + widget);
            }
        }
        return State.REJECTED;
    }

    private Widget targetWidget(Widget eventRecipient) {
        return scene.activeLayerWidget();
    }

    @Override
    public State mouseClicked(Widget widget, WidgetMouseEvent wme) {
        return ifActiveLayer(true, widget, () -> dispatch(widget, wme, MouseEvent.MOUSE_CLICKED));
    }

    @Override
    public State mousePressed(Widget widget, WidgetMouseEvent wme) {
        return ifActiveLayer(widget, () -> dispatch(widget, wme, MouseEvent.MOUSE_PRESSED));
    }

    @Override
    public State mouseReleased(Widget widget, WidgetMouseEvent wme) {
        return ifActiveLayer(widget, () -> dispatch(widget, wme, MouseEvent.MOUSE_RELEASED));
    }

    @Override
    public State mouseEntered(Widget widget, WidgetMouseEvent wme) {
        return ifActiveLayer(widget, () -> dispatch(widget, wme, MouseEvent.MOUSE_ENTERED));
    }

    @Override
    public State mouseExited(Widget widget, WidgetMouseEvent wme) {
        return ifActiveLayer(widget, () -> dispatch(widget, wme, MouseEvent.MOUSE_EXITED));
    }

    @Override
    public State mouseDragged(Widget widget, WidgetMouseEvent wme) {
        return ifActiveLayer(widget, () -> dispatch(widget, wme, MouseEvent.MOUSE_DRAGGED));
    }

    @Override
    public State mouseMoved(Widget widget, WidgetMouseEvent wme) {
        return ifActiveLayer(widget, () -> dispatch(widget, wme, MouseEvent.MOUSE_MOVED));
    }

    @Override
    public State keyTyped(Widget widget, WidgetKeyEvent wke) {
        return ifActiveLayer(widget, () -> dispatch(widget, wke, KeyEvent.KEY_TYPED));
    }

    @Override
    public State keyPressed(Widget widget, WidgetKeyEvent wke) {
        return ifActiveLayer(widget, () -> dispatch(widget, wke, KeyEvent.KEY_PRESSED));
    }

    @Override
    public State keyReleased(Widget widget, WidgetKeyEvent wke) {
        return ifActiveLayer(widget, () -> dispatch(widget, wke, KeyEvent.KEY_RELEASED));
    }

    @Override
    public State mouseWheelMoved(Widget widget, WidgetMouseWheelEvent event) {
        return ifActiveLayer(widget, () -> {
            MouseWheelListener mwl = toolAs(MouseWheelListener.class);
            if (mwl != null) {
                MouseWheelEvent mwe = toMouseWheelEvent(event);
                mwl.mouseWheelMoved(mwe);
                if (mwe.isConsumed()) {
                    return State.CONSUMED;
                }
            }
            ScalingMouseListener sml = toolAs(ScalingMouseListener.class);
            if (sml != null) {
                MouseWheelEvent mwe = toMouseWheelEvent(event);
                Point2D.Double pt = ViewL.lastPoint(widget);
                sml.mouseWheelMoved(pt.x, pt.y, mwe);
                if (mwe.isConsumed()) {
                    return State.CONSUMED;
                }
            }
            return State.REJECTED;
        });
    }
}
