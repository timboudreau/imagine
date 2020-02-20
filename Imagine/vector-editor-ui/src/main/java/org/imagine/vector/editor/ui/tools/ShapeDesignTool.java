/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Set;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import net.dev.java.imagine.spi.tool.ToolImplementation;
import org.imagine.vector.editor.ui.spi.WidgetSupplier;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.imagine.utils.Holder;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import org.netbeans.paintui.widgetlayers.WidgetController;
import org.openide.util.Lookup;
import org.openide.util.lookup.ProxyLookup;

/**
 *
 * @author Tim Boudreau
 */
@ToolDef(category = "vector", name = "Shape Design")
@Tool(ShapesCollection.class)
public class ShapeDesignTool extends ToolImplementation<ShapesCollection> implements WidgetSupplier, PaintParticipant {

    private MPL layerLookup = new MPL();
    private Holder<Repainter> repainter = Holder.create();

    public ShapeDesignTool(ShapesCollection obj) {
        super(obj);
    }

    @Override
    public void detach() {
        if (designToolWidget != null) {
            ViewL.detach(designToolWidget.getScene());
        }
        layerLookup.setOtherLookups();
        widgetLookup.setOtherLookups();
    }

    @Override
    public void attach(Lookup.Provider layer) {
        layerLookup.setOtherLookups(layer.getLookup());
    }

    @Override
    public void createLookupContents(Set<? super Object> addTo) {
        addTo.add(this);
    }

    @Override
    protected final Lookup additionalLookup() {
        return widgetLookup;
    }

    ShapeDesignToolWidget designToolWidget;

    @Override
    public Widget apply(Scene scene, WidgetController ctrllr) {
        ViewL.attach(scene);
        if (designToolWidget == null || designToolWidget.getScene() != scene) {
            if (designToolWidget != null && designToolWidget.getParentWidget() != null) {
                designToolWidget.removeFromParent();
            }
            designToolWidget = new ShapeDesignToolWidget(scene, obj, layerLookup, repainter, ctrllr);

            widgetLookup.setOtherLookups(designToolWidget.getLookup());
        }
        return designToolWidget;
    }

    @Override
    public void attachRepainter(Repainter repainter) {
        this.repainter.set(repainter);
    }

    @Override
    public void paint(Graphics2D g2d, Rectangle layerBounds, boolean commit) {

    }

    private final MPL widgetLookup = new MPL();

    static class MPL extends ProxyLookup {

        void setOtherLookups(Lookup... lkps) {
            super.setLookups(lkps);
        }
    }
}
