package org.netbeans.paintui;

import com.mastfrog.util.strings.Strings;
import net.java.dev.imagine.ui.common.BackgroundStyle;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.dev.java.imagine.api.selection.PictureSelection;
import net.dev.java.imagine.api.tool.Tool;
import net.dev.java.imagine.api.tool.aspects.NonPaintingTool;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant.Repainter;
import net.dev.java.imagine.spi.tool.ToolUIContext;
import net.java.dev.imagine.api.image.Hibernator;
import net.java.dev.imagine.api.image.RenderingGoal;
import net.java.dev.imagine.spi.image.LayerImplementation;
import net.java.dev.imagine.spi.image.SurfaceImplementation;
import org.imagine.utils.painting.RepaintHandle;
import net.java.dev.imagine.spi.image.support.AbstractPictureImplementation;
import net.java.dev.imagine.ui.common.BackgroundStyleApplier;
import org.imagine.editor.api.ImageEditorBackground;
import org.imagine.editor.api.AspectRatio;
import org.imagine.editor.api.ContextLog;
import org.netbeans.api.visual.border.BorderFactory;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.widget.LayerWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.imagine.editor.api.Zoom;
import org.imagine.editor.api.snap.SnapPointsSupplier;
import org.imagine.utils.java2d.GraphicsUtils;
import org.netbeans.api.visual.model.ObjectState;
import org.netbeans.api.visual.widget.EventProcessingType;
import org.netbeans.paint.api.editing.LayerFactory;
import org.netbeans.paintui.widgetlayers.WidgetController;
import org.netbeans.paintui.widgetlayers.WidgetLayer;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
final class PictureScene extends Scene implements WidgetController, ChangeListener {

    private final PI picture;
    private Tool activeTool;
    private final ZoomImpl zoom = new ZoomImpl(this);
    private final ToolEventDispatchAction toolAction = new ToolEventDispatchAction(this);
    private final Widget mainLayer = new LayerWidget(this);
    private final SelectionWidget selectionLayer = new SelectionWidget();
    private final GridWidget gridWidget = new GridWidget(this);
    private final RH rh = new RH();
    private boolean hasResolutionDependentLayers;
    private final ContextLog CLOG = ContextLog.get("selection");
    private final ContextLog ALOG = ContextLog.get("toolactions");
    final ToolUIContext uiContext = new PictureSceneToolUIContextImpl(this).toToolUIContext();

    public PictureScene() {
        this(new Dimension(640, 480), BackgroundStyle.TRANSPARENT);
    }

    public PictureScene(Dimension size, BackgroundStyle backgroundStyle) {
        this(size, backgroundStyle, true);
    }

    public PictureScene(Dimension size, BackgroundStyle backgroundStyle, boolean createInitialLayer) {
        picture = new PI(rh, size, backgroundStyle, createInitialLayer);
//        setKeyEventProcessingType(EventProcessingType.FOCUSED_WIDGET_AND_ITS_PARENTS);
//        setKeyEventProcessingType(EventProcessingType.FOCUSED_WIDGET_AND_ITS_CHILDREN_AND_ITS_PARENTS);
        setKeyEventProcessingType(EventProcessingType.ALL_WIDGETS);
        setCheckClipping(false);
        init(size);
        ViewL.attach(this);
        getPriorActions().addAction(toolAction);
    }

    public PictureScene(BufferedImage img) {
        picture = new PI(new RH(), img);
        init(new Dimension(img.getWidth(), img.getHeight()));
    }

    void onOwningComponentActivated() {
        toolAction.activate();
    }

    void onOwningComponentDeactivated() {
        toolAction.deactivate();
    }

    PI picture() {
        return picture;
    }

    RH rh() {
        return rh;
    }

    Zoom zoom() {
        return zoom;
    }

    AspectRatio aspectRatio() {
        return picture.aspectRatio();
    }

    public void pictureResized(Dimension newSize) {
        mainLayer.setPreferredBounds(new Rectangle(new Point(0, 0), newSize));
        validate();
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        setBackground(ImageEditorBackground.getDefault().style().getPaint());
        if (getView() != null && getView().isShowing()) {
            getView().repaint();
        }
    }

    private void init(Dimension size) {
        mainLayer.setPreferredBounds(new Rectangle(new Point(0, 0), size));
        setBackground(ImageEditorBackground.getDefault().style().getPaint());
        hasResolutionDependentLayers = false;
        //make thsi more efficient later
        List<Widget> widgets = new ArrayList<Widget>();
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
            mainLayer.addChild(w2);
        }
        validate();
        mainLayer.setLayout(LayoutFactory.createAbsoluteLayout());
        mainLayer.setBorder(BorderFactory.createEmptyBorder());
        addChild(mainLayer);
        setBorder(BorderFactory.createEmptyBorder());
        ImageEditorBackground.getDefault().addChangeListener(this);
    }

    @Override
    public Lookup getLookup() {
        return picture.getSelection().getLookup();
    }

    public static String getDefaultLayerName(int ix) {
        return NbBundle.getMessage(PI.class, "LAYER_NAME", "" + ix);
    }

    public PI getPicture() {
        return picture;
    }

    @Override
    protected boolean isRepaintRequiredForRevalidating() {
        return false;
    }

    OneLayerWidget activeLayerWidget() {
        return findWidget(picture.getActiveLayer());
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
        OneLayerWidget w = new OneLayerWidget(layer, this,
                layer.getLookup().lookup(WidgetLayer.class));
        return w;
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

    Tool activeTool() {
        return activeTool;
    }

    public void setActiveTool(Tool tool) {
        CLOG.log(() -> "PS.setActiveTool " + tool);
        LayerImplementation activeLayer = picture.getActiveLayer();
        if (this.activeTool == tool) {
            if (tool != null) {
                CLOG.log(() -> "Active tool is already " + activeTool.getName() + " - do nothing");
                if (tool.isAttachedTo(activeLayer.getLayer())) {
                    return;
                }
            } else {
                CLOG.log(() -> "Setting active tool null when already null");
                return;
            }
        }
        if (this.activeTool != null) {
            detachTool(this.activeTool);
        }
        this.activeTool = null;
        if (maybeAttachTool(tool, activeLayer)) {
            this.activeTool = tool;
            CLOG.log(() -> "Active tool for " + activeTool.getName() + " in " + this + " now " + tool.getName());
        } else {
            this.activeTool = null;
            CLOG.log(() -> "Failed to activate tool " + (tool == null ? "<null>" : tool.getName()) + " for " + this);
        }
    }

    void detachForClose() {
        CLOG.log(() -> "Detach for close " + this);
        LayerImplementation layer = picture.getActiveLayer();
        if (layer != null) {
            SurfaceImplementation surf = layer.getSurface();
            if (surf != null) {
                surf.setTool(null);
            }
        }
        if (activeTool != null) {
            ALOG.log(() -> "Really detach " + activeTool.getName());
            activeTool.detach();
            activeTool = null;
        }
        setActiveTool((Tool) null);
    }

    private boolean detachTool(Tool tool) {
        if (tool != null) {
            CLOG.log(() -> "Detach tool " + tool.getName());
            tool.detach();
            return true;
        }
        return false;
    }

    private boolean maybeAttachTool(Tool tool, LayerImplementation layer) {
        if (tool == null || layer == null) {
            CLOG.log(() -> "PicSc  no attach due to tool " + tool + " layer " + layer);
            if (layer != null) {
                SurfaceImplementation surf = layer.getSurface();
                if (surf != null) {
                    CLOG.log(() -> "PicSc  set surface tool to null");
                    surf.setTool(null);
                }
            }
            return false;
        }
        if (!tool.canAttach(layer.getLayer())) {
            CLOG.log(() -> "PicSc  tool " + tool.getName() + " cannot attach to " + layer);
            SurfaceImplementation surf = layer.getSurface();
            if (surf != null) {
                CLOG.log(() -> "PicSc  set surface tool to null");
                surf.setTool(null);
            }
            return false;
        }
        CLOG.log(() -> "PicSc  trying to attach tool " + tool.getName());
        //With new API, we must attach first, or the tool's lookup contents
        //are not initialized yet
        tool.attach(layer.getLayer(), uiContext);
        CLOG.log(() -> "   attached " + tool + " to " + layer);
        PaintParticipant participant = get(tool, PaintParticipant.class);
        SurfaceImplementation surface = layer.getSurface();
        if (participant != null) {
            participant.attachRepainter(selectionLayer.newRepainter(tool, participant, layer, surface));
        }
        if (surface != null) {
            CLOG.log(() -> "PicSc  setting tool on surface to " + tool.getName());
            layer.getSurface().setTool(tool);
        }
        setFocusedWidget(selectionLayer);
        return true;
    }

    private <T> T get(Tool tool, Class<T> clazz) {
        return tool == null ? null : tool.getLookup().lookup(clazz);
    }

    @Override
    public Zoom getZoom() {
        return zoom;
    }

    @Override
    public SnapPointsSupplier snapPoints() {
        SnapPointsSupplier supp = picture.getActiveLayer().getLookup().lookup(SnapPointsSupplier.class);
        if (supp == null) {
            supp = SnapPointsSupplier.NONE;
        }
        return supp;
    }

    public BufferedImage toImage() {
        //XXX handle zoom, etc.
        Dimension d = picture.getSize();
        return GraphicsUtils.newBufferedImage(d.width, d.height, g -> {
            picture.paint(RenderingGoal.PRODUCTION, g, null, false, Zoom.ONE_TO_ONE);
        });
    }

    static String stateToString(ObjectState st) {
        Set<String> s = new TreeSet<>();
        if (st.isFocused()) {
            s.add("focused");
        }
        if (st.isHighlighted()) {
            s.add("highlighted");
        }
        if (st.isHovered()) {
            s.add("hovered");
        }
        if (st.isObjectFocused()) {
            s.add("objectFocused");
        }
        if (st.isObjectHovered()) {
            s.add("objectHovered");
        }
        if (st.isSelected()) {
            s.add("selected");
        }
        if (st.isWidgetAimed()) {
            s.add("widgetAimed");
        }
        if (st.isWidgetHovered()) {
            s.add("widgetHovered");
        }
        return s.isEmpty() ? "<none>" : Strings.join(',', s);
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
            picture.getSelection().addChangeListener(this);
        }

        void detach() {
            picture.getSelection().removeChangeListener(this);
        }

        @Override
        protected void notifyStateChanged(ObjectState previousState, ObjectState state) {
            System.out.println("SelLayer state " + stateToString(previousState) + " -> " + stateToString(state));
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
            return false;
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
                SelectionWidget.this.setCursor(cursor);
                getScene().getFocusedWidget().setCursor(cursor);
                getScene().setCursor(cursor);
                mainLayer.setCursor(cursor);
            }

            @Override
            public void requestCommit() {
//                System.out.println("Request commit by " + activeTool + " for layer " + layer);
//                System.out.println("Participant " + painter);
                if (painter != null) {
                    surface.beginUndoableOperation(activeTool.getName());
                    Graphics2D g = surface.getGraphics();
                    try {
                        GraphicsUtils.setHighQualityRenderingHints(g);
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

    <T> T toolAs(Class<T> what) {
        return get(activeTool, what);
    }

    class RH implements RepaintHandle {

        PictureScene scene() {
            return PictureScene.this;
        }

        @Override
        public void repaintArea(int x, int y, int w, int h) {
            repaint(mainLayer, new Rectangle(x, y, w, h));
        }

        @Override
        public void setCursor(Cursor cursor) {
            PictureScene.this.setCursor(cursor);
        }
    }

    private static int DEBUG_IDS = 0;

    final class PI extends AbstractPictureImplementation {

        private final int id = DEBUG_IDS++;

        private AspectRatio ratio = new AR();
        private BackgroundStyle backgroundStyle;

        class AR implements AspectRatio {

            @Override
            public double width() {
                double w = getSize().width;
                return getZoomFactor() * w;
            }

            @Override
            public double height() {
                double h = getSize().height;
                return getZoomFactor() * h;
            }

            @Override
            public boolean isFlexible() {
                return !hasResolutionDependentLayers;
            }
        }

        public PI(RepaintHandle handle, Dimension size, BackgroundStyle backgroundStyle) {
            this(handle, size, backgroundStyle, true);
        }

        public PI(RepaintHandle handle, Dimension size, BackgroundStyle backgroundStyle, boolean createInitialLayer) {
            super(size);
            this.backgroundStyle = backgroundStyle == null ? BackgroundStyle.TRANSPARENT : backgroundStyle;
            addRepaintHandle(handle);
            if (createInitialLayer) {
                LayerImplementation initial = createInitialLayer(size);
                assert initial != null;
                add(0, initial);
                setActiveLayer(initial);
                if (backgroundStyle.isOpaque()) {
                    BackgroundStyleApplier applier = initial.getLookup().lookup(BackgroundStyleApplier.class);
                    if (applier == null) {
                        SurfaceImplementation surf = initial.getSurface();
                        if (surf != null) {
                            Graphics2D g = surf.getGraphics();
                            g.setColor(backgroundStyle.toColor());
                            g.fillRect(0, 0, size.width, size.height);
                            g.dispose();
                        }
                    } else {
                        applier.applyBackground(backgroundStyle.toColor());
                    }
                }
                LayerImplementation active = getActiveLayer();
                if (active != null) {
                    psel.activeLayerChanged(null, getActiveLayer());
                }
            }
            initialized();
        }

        public PictureScene scene() {
            return PictureScene.this;
        }

        AspectRatio aspectRatio() {
            return ratio;
        }

        BackgroundStyle backgroundStyle() {
            return backgroundStyle;
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
            PictureScene.this.hasResolutionDependentLayers = false;
            Rectangle newBounds = new Rectangle(0, 0, 1, 1);
            Tool oldTool = activeTool;
            if (oldTool != null && oldActiveLayer != newActiveLayer) {
//                System.out.println("  active layer changed, detach tool");
                detachTool(oldTool);
            }
            for (LayerImplementation removedLayer : removed) {
                OneLayerWidget widget = PictureScene.this.findWidget(removedLayer);
//                widget.removeNotify();
                widget.removeFromParent();
            }
            for (LayerImplementation addedLayer : added) {
                OneLayerWidget w = PictureScene.this.createWidget(addedLayer);
//                w.addNotify();
                mainLayer.addChild(w);
            }
            for (LayerImplementation layer : newLayers) {
                hasResolutionDependentLayers |= !layer.isResolutionIndependent();
                OneLayerWidget olw = findWidget(layer);
                assert olw != null : "No widget for " + layer;
//                if (PictureScene.this.picture == null
//                        || olw.layer == newActiveLayer) {
//                    if (!olw.getActions().getActions().contains(PictureScene.this.toolAction)) {
//                        System.out.println("add tool action to " + olw);
//                        olw.getActions().addAction(PictureScene.this.toolAction);
//                    }
//                }
                olw.bringToFront();
                if (layer.isVisible()) {
                    Rectangle bds = layer.getBounds();
                    newBounds.add(bds);
                }
            }
            if (newActiveLayer != oldActiveLayer && !maybeAttachTool(oldTool, newActiveLayer)) {
                if (oldTool != null) {
                    oldTool.detach();
                }
                activeTool = null;
            }
            if (newActiveLayer != oldActiveLayer) {
                if (newActiveLayer != null) {
                    OneLayerWidget olw = (OneLayerWidget) findWidget(newActiveLayer);
//                    setFocusedWidget(olw);
                }
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
//            Widget oldW = PictureScene.this.findWidget(old);
//            Widget newW = PictureScene.this.findWidget(nue);
//            if (newW != null) {
//                if (!newW.getActions().getActions().contains(PictureScene.this.toolAction)) {
//                    System.out.println("add tool action to " + newW);
//                    newW.getActions().addAction(0, PictureScene.this.toolAction);
//                }
//            }
//            if (oldW != null && oldW != newW) {
//                while (oldW.getActions().getActions().contains(toolAction)) {
//                    System.out.println("remove tool action to " + oldW);
//                    oldW.getActions().removeAction(PictureScene.this.toolAction);
//                }
//            }
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
            if (nue != null) {
                OneLayerWidget olw = (OneLayerWidget) findWidget(nue);
//                setFocusedWidget(olw);
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
        public boolean paint(RenderingGoal goal, Graphics2D g, Rectangle bounds, boolean showSelection, Zoom zoom) {
            if (isHibernated()) {
                return false;
            }
            boolean result = false;
            for (LayerImplementation l : getLayers()) {
                result |= l.paint(goal, g, bounds, showSelection, bounds == null, zoom, aspectRatio());
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
                    hib.wakeup(hasResolutionDependentLayers, () -> {
                        PictureScene.this.validate();
                        PictureScene.this.repaint();
                    });
                }
            }
        }
    }

    @Override
    public String toString() {
        List<Widget> kids = mainLayer.getChildren();
        return "PicSc-" + picture.id + " with " + kids.size()
                + " main layer children: " + kids;
    }
}
