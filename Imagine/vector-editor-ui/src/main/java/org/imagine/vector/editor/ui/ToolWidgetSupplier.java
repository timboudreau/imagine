package org.imagine.vector.editor.ui;

import java.awt.Point;
import java.util.function.BiConsumer;
import net.dev.java.imagine.api.tool.Tool;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.paintui.widgetlayers.WidgetController;
import org.netbeans.paintui.widgetlayers.WidgetFactory;
import org.netbeans.paintui.widgetlayers.WidgetLayer;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Tim Boudreau
 */
class ToolWidgetSupplier extends WidgetLayer implements BiConsumer<Tool, Tool> {

    private Widget lastContainer;
    private ToolWidgetLayer lastLayer;
    private boolean listening;
    private final VectorLayer layer;

    public ToolWidgetSupplier(VectorLayer layer) {
        this.layer = layer;
    }

    void sceneResized() {
        if (lastLayer != null) {
            lastLayer.sceneResized();
        }
    }

    public boolean isWidgetActive() {
        return lastLayer != null && lastLayer.isUsingInternalWidget();
    }

    @Override
    public WidgetFactory createWidgetController(Widget container, WidgetController controller) {
        if (container == lastContainer && lastLayer != null) {
            return lastLayer;
        }
        lastLayer = new ToolWidgetLayer(container, layer, controller);
        lastContainer = container;
        if (!listening) {
            layer.surface().onToolChange(this);
            layer.surface().onPositionChange(this::surfaceMoved);
        }
        return lastLayer;
    }

    private void surfaceMoved(Point old, Point nue) {

    }

    @Override
    public void accept(Tool old, Tool nue) {
        if (lastLayer != null) {
            lastLayer.toolChanged(old, nue);
            if (nue != null) {
                Lookup lkp = Lookups.exclude(nue.getLookup(), Tool.class);
                layer.setAdditionalLookups(lkp);
            } else {
                layer.setAdditionalLookups();
            }
        }
    }
}
