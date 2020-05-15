package org.imagine.vector.editor.ui.tools;

import com.mastfrog.function.state.Obj;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.dev.java.imagine.api.tool.aspects.CustomizerProvider;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import net.dev.java.imagine.spi.tool.ToolImplementation;
import net.dev.java.imagine.spi.tool.ToolUIContext;
import org.imagine.editor.api.ContextLog;
import org.imagine.editor.api.snap.SnapPointsSupplier;
import org.imagine.inspectors.spi.Inspectors;
import org.imagine.vector.editor.ui.palette.PaintPalettes;
import org.imagine.vector.editor.ui.spi.WidgetSupplier;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import org.imagine.vector.editor.ui.tools.widget.DesignWidgetManager;
import org.netbeans.paintui.widgetlayers.WidgetController;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.ProxyLookup;

/**
 *
 * @author Tim Boudreau
 */
@ToolDef(category = "vector", name = "SHAPE_DESIGN",
        iconPath = "org/netbeans/paint/tools/resources/points.svg")
@Tool(value = ShapesCollection.class, toolbarPosition = 3100)
@Messages("SHAPE_DESIGN=Shape Design")
public class ShapeDesignTool extends ToolImplementation<ShapesCollection>
        implements WidgetSupplier, PaintParticipant, CustomizerProvider {

    private final MPL layerLookup = new MPL();
    private final Obj<Repainter> repainter = Obj.create();
    private Lookup.Provider currentLayerLookup = NO_LOOKUP;
    DesignWidgetManager manager;
    private final MutableProxyLookup widgetLookup = new MutableProxyLookup();
    private Cust cust;
    Widget designToolWidget;
    private static final ContextLog CLOG = ContextLog.get("toolactions");

    public ShapeDesignTool(ShapesCollection obj) {
        super(obj);
    }

    @Override
    public void detach() {
        CLOG.log("SDT.detach");
        if (designToolWidget != null) {
//            ViewL.detach(designToolWidget.getScene());
            CLOG.stack(() -> "Detach retaining existing DTW " + designToolWidget);
            designToolWidget.setVisible(false);
        }
        currentLayerLookup = NO_LOOKUP;
        layerLookup.setOtherLookups();
        widgetLookup.updateLookups();
        cust = null;
//        manager = null;
        PaintPalettes.closePalettes();
        repainter.set(null);
    }

    @Override
    public void attach(Lookup.Provider layer, ToolUIContext ctx) {
        CLOG.log(() -> "SDT.attach - existing DTW " + designToolWidget);
        currentLayerLookup = layer;
        PaintPalettes.openPalettes();
        Inspectors.openUI(true);
        if (designToolWidget != null) {
            designToolWidget.setVisible(true);
            layerLookup.setOtherLookups(designToolWidget.getLookup(), layer.getLookup());
        } else {
            layerLookup.setOtherLookups(layer.getLookup());
        }
    }

    @Override
    public boolean takesOverPaintingScene() {
        return true;
    }

    @Override
    public void createLookupContents(Set<? super Object> addTo) {
        addTo.add(this);
    }

    @Override
    protected final Lookup additionalLookup() {
        return widgetLookup;
    }

    @Override
    public Widget apply(Scene scene, WidgetController ctrllr, SnapPointsSupplier snapPoints) {
        CLOG.log(() -> "SDT.apply(" + ctrllr + ")");
        if (designToolWidget == null || designToolWidget.getScene() != scene) {
            if (designToolWidget != null && designToolWidget.getParentWidget() != null) {
                CLOG.log(() -> "Have an existing design tool widget - remove it");
                designToolWidget.removeFromParent();
            }
            manager = new DesignWidgetManager(scene, obj, widgetLookup, ctrllr.getZoom(), currentLayerLookup.getLookup());
            designToolWidget = manager.getMainWidget();
            layerLookup.setOtherLookups(designToolWidget.getLookup(), currentLayerLookup.getLookup());
        } else {
            CLOG.log(() -> "Proceed with existing design tool widget in " + designToolWidget.getParentWidget());
            EventQueue.invokeLater(manager::sync);
        }
        return designToolWidget;
    }

    @Override
    public void attachRepainter(Repainter repainter) {
        this.repainter.set(repainter);
    }

    @Override
    public void paint(Graphics2D g, Rectangle layerBounds, boolean commit) {
        // we don't actually paint here, but we need the repainter
        // to pass to the sub-tool
    }

    @Override
    public Customizer getCustomizer() {
        if (cust != null) {
            return cust;
        }
        ProxyLookup lkp = new ProxyLookup(widgetLookup, currentLayerLookup.getLookup());
        return cust = new Cust(super.obj, lkp, newSelection -> {
            if (manager != null) {
                manager.updateSelection(newSelection);
            }
        }, (popupElement) -> {
            if (manager != null) {
                return manager.getPopupMenu(popupElement);
            }
            return null;
        }, currentLayerLookup.getLookup());
    }

    static final class Cust implements Customizer<ShapesCollection> {

        private final ShapesCollection shapes;
        private final ShapeCollectionPanel pnl;

        public Cust(ShapesCollection shapes, Lookup lookup, Consumer<ShapeElement> onChange,
                Function<ShapeElement, JPopupMenu> popupProvider, Lookup layerLookup) {
            this.shapes = shapes;
            pnl = new ShapeCollectionPanel(shapes, lookup, onChange, popupProvider, layerLookup);
        }

        @Override
        public JComponent getComponent() {
            return pnl;
        }

        @Override
        public String getName() {
            return "Shapes";
        }

        @Override
        public ShapesCollection get() {
            return shapes;
        }
    }

    static class MPL extends ProxyLookup {

        void setOtherLookups(Lookup... lkps) {
            super.setLookups(lkps);
        }
    }

    static Lookup.Provider NO_LOOKUP = () -> Lookup.EMPTY;
}
