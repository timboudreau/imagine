package org.netbeans.paintui;

import net.java.dev.imagine.ui.common.PositionStatusLineElementProvider;
import net.java.dev.imagine.ui.common.BackgroundStyle;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.dev.java.imagine.api.selection.PictureSelection;
import net.dev.java.imagine.api.tool.Tool;
import net.dev.java.imagine.api.tool.aspects.NonPaintingTool;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant.Repainter;
import net.java.dev.imagine.api.image.Hibernator;
import net.java.dev.imagine.spi.image.LayerImplementation;
import net.java.dev.imagine.spi.image.RepaintHandle;
import net.java.dev.imagine.spi.image.SurfaceImplementation;
import net.java.dev.imagine.spi.image.support.AbstractPictureImplementation;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.border.BorderFactory;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.widget.LayerWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.paint.api.editing.LayerFactory;
import org.netbeans.paint.api.editor.Zoom;
import org.netbeans.paint.api.util.GraphicsUtils;
import org.netbeans.paintui.widgetlayers.WidgetLayer;
import org.openide.util.ChangeSupport;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
final class PictureScene extends Scene {

    private final PI picture;
    private final ZoomImpl zoom = new ZoomImpl();
    private Tool activeTool;
    private final Widget mainLayer = new LayerWidget(this);
    private final SelectionWidget selectionLayer = new SelectionWidget();
    private final GridWidget gridWidget = new GridWidget(this);

    public PictureScene() {
        this(new Dimension(640, 480), BackgroundStyle.TRANSPARENT);
    }

    public PictureScene(Dimension size, BackgroundStyle backgroundStyle) {
        picture = new PI(new RH(), size, backgroundStyle);
        init(size);
    }

    public PictureScene(BufferedImage img) {
        picture = new PI(new RH(), img);
        init(new Dimension(img.getWidth(), img.getHeight()));
    }

    private void init(Dimension size) {
        mainLayer.setPreferredBounds(new Rectangle(new Point(0, 0), size));
        setBackground(GraphicsUtils.checkerboardBackground());
        hasResolutionDependentLayers = false;
        //make thsi more efficient later
        List<Widget> widgets = new ArrayList<Widget>();
        for (Widget w : getChildren()) {
            if (w instanceof OneLayerWidget) {
                ((OneLayerWidget) w).removeNotify();
            }
        }
        for (LayerImplementation layer : picture.getLayers()) {
            hasResolutionDependentLayers |= !layer.isResolutionIndependent();
            Widget w1 = findWidget(layer);
            if (w1 == null) {
                w1 = createWidget(layer);
            }
            widgets.add(w1);
        }
        mainLayer.removeChildren();
        for (Widget w2 : widgets) {
            if (w2 instanceof OneLayerWidget) {
                OneLayerWidget wid = (OneLayerWidget) w2;
                wid.addNotify();
                assert wid.layer != null;
                assert toolAction != null;
                //picture is null if called from PI constructor as it populates
                //its layers
                if (picture == null || wid.layer == picture.getActiveLayer()) {
                    wid.getActions().addAction(toolAction);
                }
            }
            mainLayer.addChild(w2);
        }
        validate();
        mainLayer.setLayout(LayoutFactory.createAbsoluteLayout());
        mainLayer.setBorder(BorderFactory.createEmptyBorder());
        addChild(mainLayer);
        setBorder(BorderFactory.createEmptyBorder());
        addChild(gridWidget);
        addChild(selectionLayer);
        selectionLayer.attachToSelection();
    }

    @Override
    public Lookup getLookup() {
        return picture.getSelection().getLookup();
    }

    private static String getDefaultLayerName(int ix) {
        return NbBundle.getMessage(PI.class, "LAYER_NAME", "" + ix);
    }

    public PI getPicture() {
        return picture;
    }

    @Override
    protected boolean isRepaintRequiredForRevalidating() {
        return true;
    }

    private OneLayerWidget findWidget(LayerImplementation layer) {
        if (layer == null) {
            return null;
        }
        for (Widget w : mainLayer.getChildren()) {
            if (w instanceof OneLayerWidget && ((OneLayerWidget) w).layer == layer) {
                return (OneLayerWidget) w;
            }
        }
        return null;
    }

    private OneLayerWidget createWidget(LayerImplementation layer) {
        return new OneLayerWidget(layer, this, layer.getLookup().lookup(WidgetLayer.class));
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
    private boolean hasResolutionDependentLayers;

    public void setActiveTool(Tool tool) {
        System.out.println("canvas set active tool " + tool);
        if (this.activeTool == tool) {
            if (tool != null) {
                System.err.println("Active tool is already " + activeTool + " - do nothing");
            }
            return;
        }
        if (this.activeTool != null) {
            System.out.println("  deactivate old tool");
            detachTool(this.activeTool);
        }
        this.activeTool = null;
        LayerImplementation layer = picture.getActiveLayer();
        if (maybeAttachTool(tool, layer)) {
            System.out.println("  activating tool succeeded");
            this.activeTool = tool;
        } else {
            System.out.println("  activating tool failed");
            this.activeTool = null;
        }
    }

    private boolean detachTool(Tool tool) {
        if (tool != null) {
            System.out.println("DETACH TOOL " + tool);
            tool.detach();
            return true;
        }
        return false;
    }

    private boolean maybeAttachTool(Tool tool, LayerImplementation layer) {
        if (tool == null || layer == null) {
            System.out.println("  no attach due to tool " + tool + " layer " + layer);
            if (layer != null) {
                SurfaceImplementation surf = layer.getSurface();
                if (surf != null) {
                    System.out.println("  set surface tool to null");
                    surf.setTool(null);
                }
            }
            return false;
        }
        if (!tool.canAttach(layer.getLayer())) {
            System.out.println("  tool cannot attach to " + layer);
            SurfaceImplementation surf = layer.getSurface();
            if (surf != null) {
                System.out.println("  set surface tool to null");
                surf.setTool(null);
            }
            return false;
        }
        System.out.println("  trying to attach tool");
        //With new API, we must attach first, or the tool's lookup contents
        //are not initialized yet
        tool.attach(layer.getLayer());
        System.out.println("   attached " + tool + " to " + layer);
        PaintParticipant participant = get(tool, PaintParticipant.class);
        System.err.println("Paint participant for " + tool + " is " + participant + " - " + tool.getLookup().lookupAll(Object.class));
        SurfaceImplementation surface = layer.getSurface();
        if (participant != null) {
            System.err.println("attaching repainter to " + surface);
            participant.attachRepainter(selectionLayer.newRepainter(tool, participant, layer, surface));
        }
        if (surface != null) {
            System.out.println("  setting tool on surface");
            layer.getSurface().setTool(tool);
        }
        return true;
    }

    private <T> T get(Tool tool, Class<T> clazz) {
        return tool == null ? null : tool.getLookup().lookup(clazz);
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

        double oldZoom = getZoomFactor();
        Tool oldTool = activeTool;
        Paint background = getBackground();
        boolean oldOpaque = isOpaque();
        Graphics2D g = result.createGraphics();
        try {
            setOpaque(false);
            setBackground(null);
            setZoomFactor(1.0D);
            setActiveTool((Tool) null);
            validate();
            paint(g); //XXX background still visible
        } finally {
            g.dispose();
            setActiveTool(oldTool);
            setBackground(background);
            setOpaque(oldOpaque);
            setZoomFactor(oldZoom);
        }
        return result;
    }

    private void selectionChange() {
        selectionLayer.repaint();
    }

    private class GridWidget extends LayerWidget {

        GridWidget(Scene scene) {
            super(scene);
            setOpaque(false);
        }

        @Override
        protected void paintWidget() {
            double z = getScene().getZoomFactor();
            if (getScene().getZoomFactor() > 4) {
                //XXX most modern graphics pipelines do drawline very
                //inefficiently.  Believe it or not, long-term, it would
                //be much more efficient to build an offscreen grid image
                //of the right size and use it with a TexturePaint to tile

                Graphics2D g = getGraphics();
                AffineTransform old = g.getTransform();
                int skipPx = (int) Math.round(z);
                g.setColor(Color.BLACK);

                AffineTransform invertScaling = (AffineTransform) old.clone();
                invertScaling.concatenate(AffineTransform.getScaleInstance(1D / z, 1D / z));

                g.setTransform(invertScaling);
                try {
                    Dimension sceneSize = getScene().getBounds().getSize();
                    double endX = Math.ceil(sceneSize.getWidth() * z);
                    double endY = Math.ceil(sceneSize.getHeight() * z);
                    g.setXORMode(Color.RED);

                    for (int i = 0; i <= endX; i += skipPx) {
                        g.drawLine(0, i, (int) endX, i);
                    }
                    for (int i = 0; i <= endY; i += skipPx) {
                        g.drawLine(i, 0, i, (int) endY);
                    }
                    g.setPaintMode();
                } finally {
                    g.setTransform(old);
                }
            }
        }
    }

    private class SelectionWidget extends Widget implements ChangeListener {

        SelectionWidget() {
            super(PictureScene.this);
            setOpaque(false);
        }

        Repainter newRepainter(Tool tool, PaintParticipant pp, LayerImplementation layer,
                SurfaceImplementation surface) {
            return new RP(tool, pp, layer, surface);
        }

        void attachToSelection() {
            picture.getSelection().addChangeListener(new ChangeListener() {

                @Override
                public void stateChanged(ChangeEvent e) {
                    System.out.println("change from picture selection " + picture.getSelection());
                    repaint();
                }
            });
        }

        @Override
        protected void paintWidget() {
            PictureSelection s = picture.getSelection();
            // Avoid creating the Graphics unless we will really use it
            Graphics2D g = null;
            if (s != null) {
                Rectangle bds = PictureScene.this.getBounds();
                g = getGraphics();
                s.paint(g, bds);
            }

            LayerImplementation layer = picture.getActiveLayer();
            if (activeTool != null && layer != null && !isNonPainting(activeTool)) {
                PaintParticipant p = get(activeTool, PaintParticipant.class);
                if (p != null) {
                    if (g == null) {
                        g = getGraphics();
                    }
                    p.paint(g, layer.getBounds(), false);
                }
            }
        }

        @Override
        protected boolean isRepaintRequiredForRevalidating() {
            return true;
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            repaint();
        }

        private boolean isNonPainting(Tool tool) {
            return tool == null || tool.getLookup().lookup(NonPaintingTool.class) != null;
        }

        class RP implements Repainter {

            private final Tool tool;
            private final PaintParticipant painter;
            private final LayerImplementation layer;
            private final SurfaceImplementation surface;
            private final Widget currentLayerWidget;

            RP(Tool tool, PaintParticipant pp, LayerImplementation layer, SurfaceImplementation surface) {
                this.tool = tool;
                this.painter = pp;
                this.layer = layer;
                this.surface = surface;
                currentLayerWidget = findWidget(layer);
            }

            @Override
            public void requestRepaint() {
                if (!isNonPainting(tool)) {
                    validate();
                    mainLayer.repaint();
                } else {
                    PictureScene.this.repaint(currentLayerWidget,
                            currentLayerWidget.getBounds());
                }
            }

            @Override
            public void requestRepaint(Rectangle bounds) {
                if (bounds == null) {
                    mainLayer.repaint();
                    return;
                }
                bounds.translate(surface.getLocation().x, surface.getLocation().y);
                PictureScene.this.repaint(currentLayerWidget, bounds);
            }

            @Override
            public void setCursor(Cursor cursor) {
                currentLayerWidget.setCursor(cursor);
            }

            @Override
            public void requestCommit() {
                System.out.println("Request commit by " + activeTool + " for layer " + layer);
                System.out.println("Participant " + painter);
                if (painter != null) {
                    surface.beginUndoableOperation(activeTool.getName());
                    Graphics2D g = surface.getGraphics();
                    try {
                        GraphicsUtils.setHighQualityRenderingHints(g);
                        //XXX maybe not l.getBounds()?
                        painter.paint(g, null, true);
                    } finally {
                        g.dispose();
                        surface.endUndoableOperation();
                    }
                }
            }

            @Override
            public Component getDialogParent() {
                return getView();
            }
        }
    }

    private class ToolEventDispatchAction extends WidgetAction.Adapter {

        private final PositionStatusLineElementProvider pslep = Lookup.getDefault().lookup(PositionStatusLineElementProvider.class);

        <T> T toolAs(Class<T> what) {
            return get(PictureScene.this.activeTool, what);
        }

        private MouseEvent toMouseEvent(WidgetMouseEvent evt, int awtId) {
            Component source = getView();
            Point p = evt.getPoint();
            // XXX translate this point?
//            if (!(activeTool instanceof NonPaintingTool)) {
//                Rectangle activeLayerBounds = picture.getActiveLayer().getBounds();
//                p.translate(-activeLayerBounds.x, -activeLayerBounds.y);
//            }
            return new MouseEvent(source, awtId, evt.getWhen(), evt.getModifiers(), p.x, p.y, evt.getClickCount(), evt.isPopupTrigger(), evt.getButton());
        }

        private MouseWheelEvent toMouseWheelEvent(WidgetMouseWheelEvent evt) {
            Component source = getView();
            Point p = evt.getPoint();
            return new MouseWheelEvent(source,
                    MouseWheelEvent.MOUSE_WHEEL,
                    evt.getWhen(),
                    evt.getModifiers(),
                    p.x, p.y, evt.getClickCount(),
                    evt.isPopupTrigger(),
                    evt.getScrollType(), evt.getScrollAmount(),
                    evt.getWheelRotation());
        }

        private KeyEvent toKeyEvent(WidgetKeyEvent evt, int awtId) {
            Component source = getView();
            return new KeyEvent(source, awtId, evt.getWhen(), evt.getModifiers(), evt.getKeyCode(), evt.getKeyChar(), evt.getKeyLocation());
        }

        State dispatch(WidgetKeyEvent evt, int awtId) {
            KeyListener kl = toolAs(KeyListener.class);
            if (kl != null && picture.getActiveLayer() != null) {
                return dispatchToTool(toKeyEvent(evt, awtId), kl);
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
//            System.out.println("dispatch " + event.getX() + " " + event.getY() + " to " + ml);
            MouseMotionListener mml = toolAs(MouseMotionListener.class);
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

        @Override
        public State mouseWheelMoved(Widget widget, WidgetMouseWheelEvent event) {
            MouseWheelListener mwl = toolAs(MouseWheelListener.class);
            if (mwl != null) {
                MouseWheelEvent mwe = toMouseWheelEvent(event);
                mwl.mouseWheelMoved(mwe);
                if (mwe.isConsumed()) {
                    return State.CONSUMED;
                }
            }
            return State.REJECTED;
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

    class RH implements RepaintHandle {

        @Override
        public void repaintArea(int x, int y, int w, int h) {
            repaint(mainLayer, new Rectangle(x, y, w, h));
        }

        @Override
        public void setCursor(Cursor cursor) {
            PictureScene.this.setCursor(cursor);
        }
    }

    class PI extends AbstractPictureImplementation {

        public PI(RepaintHandle handle, Dimension size, BackgroundStyle backgroundStyle) {
            super(size);
            addRepaintHandle(handle);
            LayerImplementation initial = createInitialLayer(size);
            assert initial != null;
            add(0, initial);
            setActiveLayer(initial);
            if (backgroundStyle.isOpaque()) {
                SurfaceImplementation surf = initial.getSurface();
                if (surf != null) {
                    Graphics2D g = surf.getGraphics();
                    g.setColor(backgroundStyle.toColor());
                    g.fillRect(0, 0, size.width, size.height);
                    g.dispose();
                }
            }
            LayerImplementation active = getActiveLayer();
            if (active != null) {
                psel.activeLayerChanged(null, getActiveLayer());
            }
            initialized();
        }

        public PI(RepaintHandle handle, BufferedImage img) {
            this(handle, new Dimension(img.getWidth(), img.getHeight()), BackgroundStyle.TRANSPARENT);
            initFromImage(img);
        }

        void resized(int width, int height) {
            updateSize(new Dimension(width, height));
        }

        private LayerImplementation createInitialLayer(Dimension d) {
            LayerImplementation impl
                    = LayerFactory.getDefault().createLayer(
                            getDefaultLayerName(1), getMasterRepaintHandle(),
                            d);
            return impl;
        }

        @Override
        protected void onLayersChanged(List<LayerImplementation> old,
                List<LayerImplementation> newLayers,
                Set<LayerImplementation> removed,
                Set<LayerImplementation> added,
                Set<LayerImplementation> retained,
                LayerImplementation oldActiveLayer,
                LayerImplementation newActiveLayer) {
            System.out.println("Layers changed - active " + newActiveLayer);
            PictureScene.this.hasResolutionDependentLayers = false;
            Rectangle newBounds = new Rectangle(0, 0, 1, 1);
            Tool oldTool = activeTool;
            if (oldTool != null && oldActiveLayer != newActiveLayer) {
                System.out.println("  active layer changed, detach tool");
                detachTool(oldTool);
            }
            for (LayerImplementation removedLayer : removed) {
                OneLayerWidget widget = PictureScene.this.findWidget(removedLayer);
                widget.removeNotify();
                widget.removeFromParent();
            }
            for (LayerImplementation addedLayer : added) {
                OneLayerWidget w = PictureScene.this.createWidget(addedLayer);
                OneLayerWidget olw = (OneLayerWidget) w;
                olw.addNotify();
                mainLayer.addChild(w);
            }
            for (LayerImplementation layer : newLayers) {
                hasResolutionDependentLayers |= !layer.isResolutionIndependent();
                OneLayerWidget olw = PictureScene.this.findWidget(layer);
                assert olw != null : "No widget for " + layer;
                if (PictureScene.this.picture == null
                        || olw.layer == newActiveLayer) {
                    System.out.println("  add tool action to layer " + layer);
                    olw.getActions().addAction(PictureScene.this.toolAction);
                }
                olw.bringToFront();
                if (layer.isVisible()) {
                    Rectangle bds = layer.getBounds();
                    newBounds.add(bds);
                }
            }
            if (newActiveLayer != oldActiveLayer && !maybeAttachTool(oldTool, newActiveLayer)) {
                System.out.println("  could not attach - clearing tool");
                activeTool = null;
            }
            updateSize(newBounds.getSize());
            validate();
        }

        @Override
        protected void onActiveLayerChanged(LayerImplementation old, LayerImplementation nue) {
            if (old == nue) {
                return;
            }
            Tool tool = PictureScene.this.activeTool;
            //XXX attach tool action to the active layer?  May be
            //problematic if user clicks outside the official bounds of the
            //layer
            Widget oldW = PictureScene.this.findWidget(old);
            Widget newW = PictureScene.this.findWidget(nue);
            if (newW != null) {
                newW.getActions().addAction(PictureScene.this.toolAction);
            }
            if (oldW != null) {
                oldW.getActions().removeAction(PictureScene.this.toolAction);
            }
            if (tool != null) {
                detachTool(tool);
                if (nue != null && maybeAttachTool(tool, nue)) {
                    activeTool = tool;
                } else {
                    activeTool = null;
                }
            }
            if (getSelection() != null) {
                getSelection().activeLayerChanged(old, nue);
            }
        }

        @Override
        protected void onSizeChanged(Dimension old, Dimension nue) {
            mainLayer.setPreferredBounds(new Rectangle(0, 0, nue.width, nue.height));
            validate();
        }

        public boolean hibernated() {
            return isHibernated();
        }

        @Override
        public boolean paint(Graphics2D g, Rectangle bounds, boolean showSelection) {
            if (isHibernated()) {
                return false;
            }
            boolean result = false;
            for (LayerImplementation l : getLayers()) {
                result |= l.paint(g, bounds, showSelection, false);
            }
            return result;
        }

        @Override
        protected void onHibernate() {
            for (LayerImplementation layer : this.getLayers()) {
                for (Hibernator hib : layer.getLookup().lookupAll(Hibernator.class)) {
                    hib.hibernate();
                }
            }
        }

        @Override
        protected void onUnhibernate() {
            for (LayerImplementation layer : this.getLayers()) {
                for (Hibernator hib : layer.getLookup().lookupAll(Hibernator.class)) {
                    hib.hibernate();
                }
            }
        }
    }
}
