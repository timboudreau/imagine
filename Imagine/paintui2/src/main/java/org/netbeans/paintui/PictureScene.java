package org.netbeans.paintui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.UndoManager;
import net.dev.java.imagine.api.selection.Selection;
import net.dev.java.imagine.spi.tools.NonPaintingTool;
import net.dev.java.imagine.spi.tools.PaintParticipant;
import net.dev.java.imagine.spi.tools.PaintParticipant.Repainter;
import net.dev.java.imagine.spi.tools.Tool;
import net.java.dev.imagine.api.image.Hibernator;
import net.java.dev.imagine.api.image.Layer;
import net.java.dev.imagine.spi.image.LayerImplementation;
import net.java.dev.imagine.spi.image.PictureImplementation;
import net.java.dev.imagine.spi.image.RepaintHandle;
import net.java.dev.imagine.spi.image.SurfaceImplementation;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.border.BorderFactory;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.widget.LayerWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.paint.api.editing.LayerFactory;
import org.netbeans.paint.api.editor.Zoom;
import org.netbeans.paint.api.util.GraphicsUtils;
import org.netbeans.paint.api.util.RasterConverter;
import org.netbeans.paintui.widgetlayers.WidgetLayer;
import org.openide.util.ChangeSupport;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 *
 * @author Tim Boudreau
 */
final class PictureScene extends Scene {

    private final LayersState.Observer observer = new ObserverImpl();
    private final PI picture;
    private final ZoomImpl zoom = new ZoomImpl();
    private Tool activeTool;
    private static final TexturePaint CHECKERBOARD_BACKGROUND = new TexturePaint(
            ((BufferedImage) ImageUtilities.loadImage(
            "org/netbeans/paintui/resources/backgroundpattern.png")), //NOI18N
            new Rectangle(0, 0, 16, 16));
    private final Widget mainLayer = new LayerWidget(this);

    public PictureScene() {
        this(new Dimension(640, 480), BackgroundStyle.TRANSPARENT);
    }

    public PictureScene(Dimension size, BackgroundStyle backgroundStyle) {
        picture = new PI(new RH(), size, backgroundStyle);
        init();
    }

    public PictureScene(BufferedImage img) {
        picture = new PI(new RH(), img);
        init();
    }

    private void init() {
        setBackground(CHECKERBOARD_BACKGROUND);
        syncLayers(picture.getLayers());
        mainLayer.setLayout(LayoutFactory.createAbsoluteLayout());
        mainLayer.setBorder(BorderFactory.createEmptyBorder());
        addChild(mainLayer);
        setBorder(BorderFactory.createEmptyBorder());
    }

    private static String getDefaultLayerName(int ix) {
        return NbBundle.getMessage(PI.class, "LAYER_NAME", "" + ix);
    }

    public PI getPicture() {
        return picture;
    }

    private Widget findWidget(LayerImplementation layer) {
        if (layer == null) {
            return null;
        }
        for (Widget w : mainLayer.getChildren()) {
            if (w instanceof OneLayerWidget && ((OneLayerWidget) w).layer == layer) {
                return w;
            }
        }
        return null;
    }

    @Override
    protected boolean isRepaintRequiredForRevalidating() {
        return true;
    }

    private Widget createWidget(LayerImplementation layer) {
        Widget result = layer.getLookup().lookup(Widget.class);
        if (result == null) {
            result = new OneLayerWidget(layer, this, layer.getLookup().lookup(WidgetLayer.class));
        }
        return result;
    }

    public void repaint(Widget widget, Rectangle bounds) {
        JComponent v = getView();
        if (v == null) {
            repaint();
        } else {
            Rectangle r = widget.convertLocalToScene(bounds);
            r = convertSceneToView(r);
            v.repaint(r);
        }
    }
    private final WidgetAction toolAction = new ToolEventDispatchAction();

    public void syncLayers(List<LayerImplementation> layers) {
        //make thsi more efficient later
        List<Widget> widgets = new ArrayList<Widget>();
        for (Widget w : getChildren()) {
            if (w instanceof OneLayerWidget) {
                ((OneLayerWidget) w).removeNotify();
            }
        }
        for (LayerImplementation layer : layers) {
            Widget w = findWidget(layer);
            if (w == null) {
                w = createWidget(layer);
            }
            widgets.add(w);
        }
        mainLayer.removeChildren();
        for (Widget w : widgets) {
            if (w instanceof OneLayerWidget) {
                OneLayerWidget wid = (OneLayerWidget) w;
                wid.addNotify();
                assert wid.layer != null;
                assert toolAction != null;
                //picture is null if called from PI constructor as it populates
                //its layers
                if (picture == null || wid.layer == picture.getActiveLayer()) {
                    wid.getActions().addAction(toolAction);
                }
            }
            mainLayer.addChild(w);
        }
        validate();
    }

    public void setActiveTool(Tool tool) {
        if (this.activeTool == tool) {
            return;
        }
        if (this.activeTool != null) {
            this.activeTool.deactivate();
        }
        this.activeTool = tool;
        if (tool != null && picture.getActiveLayer() != null) {
            LayerImplementation l = picture.getActiveLayer();
            PaintParticipant participant = get(tool, PaintParticipant.class);
            if (participant != null) {
                participant.attachRepainter(rp);
            }
            tool.activate(l.getLookup().lookup(Layer.class));
            SurfaceImplementation surf = l.getSurface();
            if (surf != null) {
                l.getSurface().setTool(tool);
            }
        }
    }

    private <T extends Object> T get(Tool tool, Class<T> clazz) {
        return tool == null ? null : clazz.isInstance(tool)
                ? clazz.cast(tool) : tool.getLookup().lookup(clazz);
    }

    Zoom getZoom() {
        return zoom;
    }

//    public BufferedImage toImage() {
//        Dimension d = picture.getSize();
//        BufferedImage result = new BufferedImage(d.width, d.height,
//                GraphicsUtils.DEFAULT_BUFFERED_IMAGE_TYPE);
//        picture.paint((Graphics2D) result.createGraphics(), null, true);
//        return result;
//    }
//
    public BufferedImage toImage() {
        //XXX handle zoom, etc.
        Dimension d = picture.getSize();
        BufferedImage result = new BufferedImage(d.width, d.height,
                GraphicsUtils.DEFAULT_BUFFERED_IMAGE_TYPE);
        Graphics2D g = result.createGraphics();
        paint(g);
        g.dispose();
        return result;
    }

    private class ToolEventDispatchAction extends WidgetAction.Adapter {

        private final PositionStatusLineElementProvider pslep = Lookup.getDefault().lookup(PositionStatusLineElementProvider.class);

         <T> T toolAs(Class<T> what) {
            return get(PictureScene.this.activeTool, what);
        }

        private MouseEvent toMouseEvent(WidgetMouseEvent evt, int awtId) {
            Component source = getView();
            Point p = evt.getPoint();

//            if (!(activeTool instanceof NonPaintingTool)) {
//                Rectangle activeLayerBounds = picture.getActiveLayer().getBounds();
//                p.translate(-activeLayerBounds.x, -activeLayerBounds.y);
//            }
            return new MouseEvent(source, awtId, evt.getWhen(), evt.getModifiers(), p.x, p.y, evt.getClickCount(), evt.isPopupTrigger(), evt.getButton());
        }

        private KeyEvent toKeyEvent(WidgetKeyEvent evt, int awtId) {
            Component source = getView();
            return new KeyEvent(source, awtId, evt.getWhen(), evt.getModifiers(), evt.getKeyCode(), evt.getKeyChar(), evt.getKeyLocation());
        }

        State dispatch(WidgetKeyEvent evt, int awtId) {
            KeyListener kl = toolAs(KeyListener.class);
            if (picture.getActiveLayer() != null) {
                if (kl != null) {
                    return dispatchToTool(toKeyEvent(evt, awtId), kl);
                }
            }
            return State.REJECTED;
        }

        State dispatch(WidgetMouseEvent evt, int awtId) {
            Point pt = evt.getPoint();
            pslep.setStatus(new StringBuilder(Integer.toString(pt.x)).append(',').append(pt.y).toString());
            if (picture.getActiveLayer() != null) {
                MouseListener ml = toolAs(MouseListener.class);
                if (ml != null) {
                    return dispatchToTool(toMouseEvent(evt, awtId), ml);
                }
            }
            return State.REJECTED;
        }

        private State dispatchToTool(MouseEvent event, MouseListener ml) {
            MouseMotionListener mml = get(activeTool, MouseMotionListener.class);
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

        @Override
        public State mouseClicked(Widget widget, WidgetMouseEvent wme) {
            return dispatch(wme, MouseEvent.MOUSE_CLICKED);
        }

        @Override
        public State mousePressed(Widget widget, WidgetMouseEvent wme) {
            return dispatch(wme, MouseEvent.MOUSE_PRESSED);
        }

        @Override
        public State mouseReleased(Widget widget, WidgetMouseEvent wme) {
            return dispatch(wme, MouseEvent.MOUSE_RELEASED);
        }

        @Override
        public State mouseEntered(Widget widget, WidgetMouseEvent wme) {
            return dispatch(wme, MouseEvent.MOUSE_ENTERED);
        }

        @Override
        public State mouseExited(Widget widget, WidgetMouseEvent wme) {
            return dispatch(wme, MouseEvent.MOUSE_EXITED);
        }

        @Override
        public State mouseDragged(Widget widget, WidgetMouseEvent wme) {
            return dispatch(wme, MouseEvent.MOUSE_DRAGGED);
        }

        @Override
        public State mouseMoved(Widget widget, WidgetMouseEvent wme) {
            return dispatch(wme, MouseEvent.MOUSE_MOVED);
        }

        @Override
        public State keyTyped(Widget widget, WidgetKeyEvent wke) {
            return dispatch(wke, KeyEvent.KEY_TYPED);
        }

        @Override
        public State keyPressed(Widget widget, WidgetKeyEvent wke) {
            return dispatch(wke, KeyEvent.KEY_PRESSED);
        }

        @Override
        public State keyReleased(Widget widget, WidgetKeyEvent wke) {
            return dispatch(wke, KeyEvent.KEY_RELEASED);
        }
    }

    private class ZoomImpl implements Zoom {

        private final ChangeSupport supp = new ChangeSupport(this);

        public float getZoom() {
            return (float) PictureScene.this.getZoomFactor();
        }

        public void setZoom(float val) {
            if (val != getZoom()) {
                PictureScene.this.setZoomFactor(val);
                supp.fireChange();
                validate();
            }
        }

        public void addChangeListener(ChangeListener cl) {
            supp.addChangeListener(cl);
        }

        public void removeChangeListener(ChangeListener cl) {
            supp.removeChangeListener(cl);
        }
    }
    private final Repainter rp = new RP();

    class RP implements Repainter {

        @Override
        public void requestRepaint() {
            if (picture.getActiveLayer() != null) {
                Widget w = findWidget(picture.getActiveLayer());
                repaint(w, w.getBounds());
            }
            if (activeTool instanceof NonPaintingTool) {
                validate();
                repaint();
            }
        }

        @Override
        public void requestRepaint(Rectangle bounds) {
            if (bounds == null) {
                mainLayer.repaint();
                return;
            }
            JComponent v = getScene().getView();
            if (picture.getActiveLayer() != null) {
                Widget w = findWidget(picture.getActiveLayer());
                repaint(w, bounds);
            } else {
                getScene().repaint();
            }
        }

        @Override
        public void setCursor(Cursor cursor) {
            PictureScene.this.setCursor(cursor);
        }

        @Override
        public void requestCommit() {
            LayerImplementation l = picture.getActiveLayer();
            if (l != null) {
                SurfaceImplementation surface = l.getSurface();
                if (surface != null) {
                    surface.beginUndoableOperation(activeTool.getName());
                    Graphics2D g = surface.getGraphics();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g.setRenderingHint(RenderingHints.KEY_RENDERING,
                            RenderingHints.VALUE_RENDER_QUALITY);
                    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                            RenderingHints.VALUE_STROKE_PURE);
                    PaintParticipant painter = activeTool == null ? null
                            : activeTool.getLookup().lookup(PaintParticipant.class);
                    if (painter == null && activeTool instanceof PaintParticipant) {
                        painter = (PaintParticipant) activeTool;
                    }
                    if (painter != null) {
                        //XXX maybe not l.getBounds()?
                        painter.paint(g, null, true);
                    }
                    surface.endUndoableOperation();
                    g.dispose();
                }
            }
        }

        @Override
        public Component getDialogParent() {
            return getView();
        }
    }

    class RH implements RepaintHandle {

        @Override
        public void repaintArea(int x, int y, int w, int h) {
            JComponent v = getView();
            if (v != null) {
                Rectangle r = new Rectangle(x, y, w, h);
                convertSceneToView(r);
                v.repaint(r);
            }
        }

        @Override
        public void setCursor(Cursor cursor) {
            PictureScene.this.setCursor(cursor);
        }
    }

    class ObserverImpl implements LayersState.Observer {

        @Override
        public void layersChanged(List<LayerImplementation> layers) {
            syncLayers(layers);
        }

        @Override
        public void activeLayerChanged(LayerImplementation old, LayerImplementation nue) {
            //XXX attach tool action to the active layer?  May be
            //problematic if user clicks outside the official bounds of the
            //layer
            Widget oldW = findWidget(old);
            Widget newW = findWidget(nue);
            if (oldW != null) {
                oldW.getActions().removeAction(toolAction);
            }
            if (newW != null) {
                newW.getActions().addAction(toolAction);
            }
        }

        @Override
        public void sizeChanged(Dimension old, Dimension nue) {
            validate();
        }
    }

    class PI extends PictureImplementation {

        private ChangeSupport changes = new ChangeSupport(this);
        private boolean pendingChange = false;
        private Selection storedSelection;
        private LayersUndoableOperation currentOp = null;
        private int undoEntryCount = 0;
        private boolean hibernated = false;
        private LayersState state = new LayersState(this);

        LayersState getState() {
            return state;
        }

        public LayersState.Observer getObserver(LayersState requester) {
            if (requester == state) {
                return observer;
            }
            return null;
        }

        public PI(RepaintHandle handle, Dimension size, BackgroundStyle backgroundStyle) {
            addRepaintHandle(handle);
            state.setSize(size);
            LayerImplementation initial = createInitialLayer(size);
            assert initial != null;
            state.addLayer(initial);
            state.setActiveLayer(initial);
            if (backgroundStyle == BackgroundStyle.WHITE) {
                SurfaceImplementation surf = initial.getSurface();
                if (surf != null) {
                    Graphics2D g = surf.getGraphics();
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, size.width, size.height);
                    g.dispose();
                }
            }
        }

        public PI(RepaintHandle handle, BufferedImage img) {
            this(handle, new Dimension(img.getWidth(), img.getHeight()), BackgroundStyle.TRANSPARENT);
            Graphics2D g = state.getLayer(0).getSurface().getGraphics();
            try {
                g.drawRenderedImage(img, AffineTransform.getTranslateInstance(0, 0));
            } catch (NullPointerException e) {
            } finally {
                g.dispose();
            }
        }

        void resized(int width, int height) {
            state.setSize(width, height);
        }

        private LayerImplementation createInitialLayer(Dimension d) {
            LayerImplementation impl =
                    LayerFactory.getDefault().createLayer(
                    getDefaultLayerName(1), getMasterRepaintHandle(),
                    d);
            return impl;
        }

        public void add(int ix, LayerImplementation l) {
            if (l == null) {
                throw new NullPointerException("Null layer"); //NOI18N
            }
            if (ix == POSITION_BOTTOM) {
                ix = 0;
            } else if (ix == POSITION_TOP) {
                ix = state.layerCount() - 1;
            }
            l.addRepaintHandle(getMasterRepaintHandle());
            state.addLayer(ix, l);
            //XXX what is 1000 here?
            Rectangle r = getScene().getBounds();
            getMasterRepaintHandle().repaintArea(r.x, r.y, r.width, r.height);
            setActiveLayer(l);
        }

        public RepaintHandle getRepaintHandle() {
            return getMasterRepaintHandle();
        }

        public boolean paint(Graphics2D g, Rectangle r, boolean showSelection) {
            if (hibernated) {
                return false;
            }
            boolean result = false;
            for (LayerImplementation l : state) {
                result |= l.paint(g, r, showSelection);
            }
            if (activeTool != null) {
                PaintParticipant participant = get(activeTool, PaintParticipant.class);
                if (participant != null) {
                    participant.paint(g, getBounds(), false);
                }
            }
            return result;
        }

        public List<LayerImplementation> getLayers() {
            return Collections.unmodifiableList(Arrays.asList(state.getLayers()));
        }

        public void move(LayerImplementation layer, int pos) {
            int oldPos = state.indexOf(layer);

            if (oldPos == -1) {
                throw new IllegalArgumentException();
            }
            if (oldPos == pos) {
                return;
            }
            beginUndoableOperation(false,
                    NbBundle.getMessage(PI.class, "MSG_MOVE_LAYER",
                    layer.getName(), "" + pos));
            try {
                state.removeLayer(oldPos);
                if (pos > oldPos) {
                    pos--;
                }
                state.addLayer(pos, layer);
            } catch (RuntimeException re) {
                cancelUndoableOperation();
                throw re;
            }
            endUndoableOperation();
            fire();
        }

        public void delete(LayerImplementation layer) {
            beginUndoableOperation(false, NbBundle.getMessage(PI.class,
                    "MSG_DELETE_LAYER", layer.getName()));
            try {
                int ix = state.indexOf(layer);

                if (ix == -1) {
                    throw new IllegalArgumentException();
                }
                state.removeLayer(layer);
                layer.removeRepaintHandle(getMasterRepaintHandle());
                if (state.getActiveLayer() == layer) {
                    if (ix != 0) {
                        state.setActiveLayer(getLayer(ix - 1));
                    } else {
                        if (state.layerCount() > 0) {
                            state.setActiveLayer(getLayer(0));
                        } else {
                            state.setActiveLayer(null);
                        }
                    }
                }
            } catch (RuntimeException re) {
                cancelUndoableOperation();
                throw re;
            } finally {
                endUndoableOperation();
                fire();
            }
        }

        public LayerImplementation getActiveLayer() {
            return state.getActiveLayer();
        }

        LayerImplementation activeLayer() {
            return getActiveLayer();
        }

        public void setActiveLayer(LayerImplementation l) {
            if (l != state.getActiveLayer()) {
                beginUndoableOperation(false, l == null ? NbBundle.getMessage(PI.class,
                        "MSG_CLEAR_ACTIVE_LAYER") : NbBundle.getMessage(PI.class,
                        "MSG_ACTIVATE_LAYER", l.getName()));
                /*
                try {
                assert l == null || state.layers.contains(l);
                //XXX shouldn't we fire after we set the field?
                //                fire();
                }
                catch (RuntimeException re) {
                cancelUndoableOperation();
                throw re;
                }
                 */
                LayerImplementation old = state.getActiveLayer();
                state.setActiveLayer(l);
                Selection oldSelection = old == null ? storedSelection : old.getLookup().lookup(Selection.class);
                if (oldSelection != null) {
                    Selection newSelection = l == null ? null : l.getLookup().lookup(Selection.class);
                    if (newSelection != null) {
                        newSelection.translateFrom(oldSelection);
                        oldSelection.clearNoUndo();
                        storedSelection = null;
                    } else {
                        storedSelection = oldSelection;
                    }
                }
                endUndoableOperation();
                fire();
            }
        }

        public LayerImplementation add(int index) {
            beginUndoableOperation(false, NbBundle.getMessage(PI.class,
                    "MSG_ADD_NEW_LAYER"));
            LayerFactory factory;
            if (state.isEmpty()) {
                factory = LayerFactory.getDefault();
            } else {
                LayerImplementation l = index >= state.layerCount()
                        ? state.getLayer(state.layerCount() - 1)
                        : state.getLayer(index);

                factory = l.getLookup().lookup(LayerFactory.class);
                if (factory == null) {
                    factory = LayerFactory.getDefault();
                }
            }

            LayerImplementation result;

            try {
                result = factory.createLayer("foo", getMasterRepaintHandle(), //XXX
                        state.getSize());
                if (index == POSITION_TOP) {
                    state.addLayer(result);
                } else if (index == POSITION_BOTTOM) {
                    state.addLayer(0, result);
                } else {
                    state.addLayer(index, result);
                }
                setActiveLayer(result);
            } catch (RuntimeException re) {
                cancelUndoableOperation();
                throw re;
            }
            endUndoableOperation();
            return result;
        }

        private LayerImplementation getLayer(int ix) {
            return (LayerImplementation) state.getLayer(ix);
        }

        public LayerImplementation duplicate(LayerImplementation toClone) {
            beginUndoableOperation(false, NbBundle.getMessage(PI.class,
                    "MSG_DUPLICATE_LAYER", toClone.getName()));
            LayerImplementation nue;

            try {
                int ix = state.indexOf(toClone);
                if (ix == -1) {
                    throw new IllegalArgumentException();
                }
                nue = getLayer(ix).clone(true, true);
                state.addLayer(ix, nue);
                setActiveLayer(nue);
            } catch (RuntimeException re) {
                cancelUndoableOperation();
                throw re;
            }
            endUndoableOperation();
            return nue;
        }

        void fire() {
            // We suspend firing until the end of an undoable operation
            if (undoEntryCount <= 0) {
                changes.fireChange();
                pendingChange = false;
            } else {
                pendingChange = true;
            }
        }

        public void addChangeListener(ChangeListener cl) {
            changes.addChangeListener(cl);
        }

        public void removeChangeListener(ChangeListener cl) {
            changes.removeChangeListener(cl);
        }

        public Dimension getSize() {
            return state.getSize();
        }

        public void repaintArea(int x, int y, int w, int h) {
            getMasterRepaintHandle().repaintArea(x, y, w, h);
        }

        public void flatten() {
            beginUndoableOperation(false, NbBundle.getMessage(PI.class,
                    "MSG_FLATTEN_LAYERS"));
            try {
                LayerImplementation nue = LayerFactory.getDefault().createLayer("foo", //XXX
                        getMasterRepaintHandle(), getSize());
                SurfaceImplementation surface = nue.getSurface();
                if (surface != null) {
                    this.paint(surface.getGraphics(), null, true);
                } else {
                    Logger.getLogger("global").log(Level.SEVERE,
                            "Tried to flatten image but default layer factory"
                            + "provides a layer instance with no surface to "
                            + "paint into");
                }
                state.setLayers(nue);
                fire();
            } catch (RuntimeException re) {
                cancelUndoableOperation();
                throw re;
            }
            endUndoableOperation();
        }

        private void beginUndoableOperation(boolean needDeepCopy, String what) {
            UndoManager undo = (UndoManager) Utilities.actionsGlobalContext().lookup(UndoManager.class);

            if (undo != null) {
                undoEntryCount++;
                if (currentOp == null) {
                    currentOp = new LayersUndoableOperation(state, needDeepCopy);
                } else if (needDeepCopy != currentOp.isDeepCopy()) {
                    currentOp.becomeDeepCopy();
                }
                currentOp.opName = what;
            }
        }

        private void cancelUndoableOperation() {
            if (currentOp != null) {
                currentOp.undo();
            }
            currentOp = null;
            undoEntryCount = 0;
            pendingChange = false;
        }

        private void endUndoableOperation() {
            undoEntryCount--;
            if (undoEntryCount == 0) {
                UndoManager undo = (UndoManager) Utilities.actionsGlobalContext().lookup(UndoManager.class);

                if (undo != null) {
                    assert undo != null;
                    assert currentOp != null;
                    undo.undoableEditHappened(new UndoableEditEvent(this, currentOp));
                    // Thread.dumpStack();
                    currentOp = null;
                }
                if (pendingChange) {
                    fire();
                }
            }
        }

        @Override
        public void hibernate() {
            setHibernated(true, false, null);
        }

        @Override
        public void wakeup(boolean immediate, Runnable run) {
            setHibernated(false, immediate, run);
        }

        synchronized void setHibernated(boolean val, boolean immediate, Runnable run) {
            if (hibernated == val) {
                return;
            }
            hibernated = val;
            for (LayerImplementation layer : state) {
                Hibernator hibernator = layer.getLookup().lookup(Hibernator.class);
                if (hibernator != null) {
                    if (val) {
                        hibernator.hibernate();
                    } else {
                        hibernator.wakeup(immediate, run);
                    }
                }
            }
        }

        @Override
        public Transferable copy(Clipboard clipboard, boolean allLayers) {
            LayerSelection sel = new LayerSelection(this, allLayers, false);
            return sel;
        }

        @Override
        public Transferable cut(Clipboard clipboard, boolean allLayers) {
            LayerSelection sel = new LayerSelection(this, allLayers, true);
            return sel;
        }

        @Override
        public boolean paste(Clipboard clipboard) {
            if (clipboard.getContents(this) != null) {
                for (DataFlavor flavor : clipboard.getAvailableDataFlavors()) {
                    if (DataFlavor.imageFlavor.equals(flavor)) {
                        try {
                            Image img = (Image) clipboard.getData(flavor);
                            if (!(img instanceof BufferedImage)) {
                                int w = img.getWidth(null);
                                int h = img.getHeight(null);
                                LayerImplementation current = getActiveLayer();
                                Selection sel = current == null ? null
                                        : getActiveLayer().getLookup().lookup(Selection.class);
                                Shape clip = sel == null ? null : sel.asShape();
                                if (clip != null && current != null) {
                                    Point loc = current.getBounds().getLocation();
                                    if (loc.x != 0 || loc.y != 0) {
                                        AffineTransform xform =
                                                AffineTransform.getTranslateInstance(-loc.x, -loc.y);
                                        clip = xform.createTransformedShape(clip);
                                    }
                                }
                                String name = NbBundle.getMessage(PI.class,
                                        "LBL_PASTED_LAYER"); //NOI18N
                                LayerImplementation newLayer = RasterConverter.getLayerFactory().createLayer(name,
                                        getMasterRepaintHandle(), new Dimension(w, h));
                                SurfaceImplementation surf = newLayer.getSurface();
                                Graphics2D g = surf.getGraphics();
                                try {
                                    if (clip != null) {
                                        g.setClip(clip);
                                    }
                                    GraphicsUtils.setHighQualityRenderingHints(g);
                                    g.drawImage(img,
                                            AffineTransform.getTranslateInstance(0,
                                            0), null);
                                } finally {
                                    g.dispose();
                                }
                                add(0, newLayer);
                            }
                            return true;
                        } catch (UnsupportedFlavorException ex) {
                            Exceptions.printStackTrace(ex);
                        } catch (IOException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                }
            }
            return false;
        }

        void setState(LayersState st) {
            state = st;
            syncLayers(Arrays.asList(st.getLayers()));
        }
    }
}
