package org.imagine.vector.editor.ui;

import java.awt.Point;
import java.util.function.Consumer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.dev.java.imagine.api.tool.Tool;
import org.imagine.editor.api.AspectRatio;
import org.imagine.editor.api.ContextLog;
import org.imagine.vector.editor.ui.spi.LayerRenderingWidget;
import org.imagine.vector.editor.ui.spi.WidgetSupplier;
import org.netbeans.api.visual.widget.LayerWidget;
import org.netbeans.api.visual.widget.Widget;
import org.imagine.editor.api.Zoom;
import org.netbeans.paintui.widgetlayers.SetterResult;
import org.netbeans.paintui.widgetlayers.WidgetController;
import org.netbeans.paintui.widgetlayers.WidgetFactory;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
final class ToolWidgetLayer implements WidgetFactory, ChangeListener {

    final Widget container;
    private LayerWidget layerWidget;
    private Widget lastWidget;
    final VectorLayer layer;
    private final WidgetController controller;
    private final DelegatingLookupConsumer lookupConsumer
            = new DelegatingLookupConsumer();
    private static final ContextLog CLOG = ContextLog.get("toolactions");

    public ToolWidgetLayer(Widget container, VectorLayer layer,
            WidgetController controller) {
        this.container = container;
        this.layer = layer;
        this.controller = controller;
    }

    void sceneResized() {
        stateChanged(null);
    }

    void toolChanged(Tool old, Tool nue) {
        CLOG.log(() -> "toolchanged " + (old == null ? "<null>" : old.getDisplayName())
            + " -> " + (nue == null ? "<null>" : nue.getDisplayName()));
        if (lastWidget != null && lastWidget.getParentWidget() != null) {
            CLOG.log(() -> "  have last widget " + lastWidget + " with parent, remove");
            layerWidget.removeChild(lastWidget);
            if (container.getScene().getView() != null && container.getScene().getView().isDisplayable()) {
                CLOG.log(() -> "  revalidate the scene");
                container.getScene().validate();
            }
            lastWidget = null;
        }
        if (nue != null) {
            WidgetSupplier widgeter = nue.getLookup().lookup(WidgetSupplier.class);
            if (widgeter != null) {
                lastWidget = widgeter.apply(container.getScene(), controller,
                        controller.snapPoints());
                CLOG.log(() -> "  have new WidgetSupplier " + widgeter + " with " + lastWidget);
                if (lastWidget instanceof LayerRenderingWidget) {
                    CLOG.log(() -> "   is a LayerRenderingWidget");
                    LayerRenderingWidget lrw = (LayerRenderingWidget) lastWidget;
                    lrw.setZoom(controller.getZoom());
                    lrw.setOpacity(layer.getOpacity());
                    lastWidget.setPreferredLocation(layer.getSurface()
                            .getLocation());
                    lrw.setLookupConsumer(lookupConsumer);
                }
                layerWidget.addChild(lastWidget);
                container.getScene().validate();
            }
        }
        toolWidgetActive = lastWidget != null;
    }

    private boolean toolWidgetActive;
    public boolean isUsingInternalWidget() {
        return toolWidgetActive;
    }

    @Override
    public void attach(Consumer<Lookup[]> addtlLookupConsumer) {
        CLOG.log("ToolWidgetLayer.attach");
        lookupConsumer.setDelegate(addtlLookupConsumer);
        Zoom zoom = controller.getZoom();
        zoom.addChangeListener(this);
        if (layerWidget == null) {
            layerWidget = new LayerWidget(container.getScene());
        }
        if (layerWidget.getParentWidget() != container) {
            if (layerWidget.getParentWidget() != null) {
                layerWidget.removeFromParent();
            }
            toolWidgetActive = true;
            container.addChild(layerWidget);
            container.getScene().validate();
        }
    }

    @Override
    public void detach() {
        CLOG.log("ToolWidgetLayer.detach");
        Zoom zoom = controller.getZoom();
        zoom.removeChangeListener(this);
        layerWidget.removeFromParent();
//        layerWidget.removeChildren();
        lookupConsumer.setDelegate(null);
        toolWidgetActive = false;
        lastWidget = null;
    }

    @Override
    public SetterResult setLocation(Point location) {
        Widget w = lastWidget;
        if (w != null && w instanceof LayerRenderingWidget) {
            return ((LayerRenderingWidget) w).setLocation(location);
        }
        return SetterResult.NOT_HANDLED;
    }

    @Override
    public SetterResult setOpacity(float opacity) {
        Widget w = lastWidget;
        if (w != null && w instanceof LayerRenderingWidget) {
            return ((LayerRenderingWidget) w).setOpacity(opacity);
        }
        return SetterResult.HANDLED;
    }

    @Override
    public SetterResult setAspectRatio(AspectRatio ratio) {
        Widget w = lastWidget;
        if (w != null && w instanceof LayerRenderingWidget) {
            return ((LayerRenderingWidget) w).setAspectRatio(ratio);
        }
        return SetterResult.HANDLED;
    }

    @Override
    public void setName(String name) {
        layer.setName(name);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (lastWidget != null && lastWidget.getScene() != null) {
            lastWidget.getScene().validate();
        }
    }
}
