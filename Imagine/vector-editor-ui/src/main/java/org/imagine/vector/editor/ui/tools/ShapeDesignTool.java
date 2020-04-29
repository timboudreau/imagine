package org.imagine.vector.editor.ui.tools;

import com.mastfrog.function.state.Obj;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Set;
import javax.swing.JComponent;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.dev.java.imagine.api.tool.aspects.CustomizerProvider;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import net.dev.java.imagine.spi.tool.ToolImplementation;
import org.imagine.editor.api.snap.SnapPointsSupplier;
import org.imagine.geometry.Circle;
import org.imagine.inspectors.spi.Inspectors;
import org.imagine.vector.editor.ui.spi.WidgetSupplier;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.imagine.vector.editor.ui.palette.PaintsPaletteTC;
import org.imagine.vector.editor.ui.palette.ShapesPaletteTC;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import org.imagine.vector.editor.ui.tools.widget.DesignWidgetManager;
import org.imagine.vector.editor.ui.tools.widget.util.ViewL;
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
@Tool(ShapesCollection.class)
@Messages("SHAPE_DESIGN=Shape Design")
public class ShapeDesignTool extends ToolImplementation<ShapesCollection>
        implements WidgetSupplier, PaintParticipant, CustomizerProvider {

    private MPL layerLookup = new MPL();
    private Obj<Repainter> repainter = Obj.create();

    public ShapeDesignTool(ShapesCollection obj) {
        super(obj);
    }

    @Override
    public void detach() {
        if (designToolWidget != null) {
            ViewL.detach(designToolWidget.getScene());
        }
        layerLookup.setOtherLookups();
        widgetLookup.updateLookups();
        ShapesPaletteTC.closePalette();
        PaintsPaletteTC.closePalette();
    }

    @Override
    public void attach(Lookup.Provider layer) {
        System.out.println("Open palettes");
        ShapesPaletteTC.openPalette();
        PaintsPaletteTC.openPalette();
        Inspectors.openUI(true);
        layerLookup.setOtherLookups(layer.getLookup());
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

    Widget designToolWidget;

    @Override
    public Widget apply(Scene scene, WidgetController ctrllr, SnapPointsSupplier snapPoints) {
        if (designToolWidget == null || designToolWidget.getScene() != scene) {
            if (designToolWidget != null && designToolWidget.getParentWidget() != null) {
                designToolWidget.removeFromParent();
            }
//            designToolWidget = new ShapeDesignToolWidget(scene, obj, layerLookup, repainter, ctrllr, snapPoints);
            DesignWidgetManager man = new DesignWidgetManager(scene, obj, widgetLookup);
            designToolWidget = man.getMainWidget();

            layerLookup.setOtherLookups(designToolWidget.getLookup());
//            widgetLookup.updateLookups(designToolWidget.getLookup());
        }
        return designToolWidget;
    }

    @Override
    public void attachRepainter(Repainter repainter) {
        this.repainter.set(repainter);
    }

    private static final double SEL_CP_RADIUS = 9F;
    private static final float SEL_HIGHLIGHT_STROKE_WIDTH = 1.5F;
    private BasicStroke[] forZoom;
    private double lastZoom = -1;

    private BasicStroke[] strokesForZoom(double zoom) {
        if (zoom == lastZoom) {
            return forZoom;
        }
        lastZoom = zoom;
        float factor = (float) (1D / zoom);
        float sz = SEL_HIGHLIGHT_STROKE_WIDTH * factor;
        forZoom = new BasicStroke[4];
        for (int i = 0; i < 4; i++) {
            forZoom[i] = new BasicStroke(sz, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL,
                    1F, new float[]{5F * factor, 3F * factor, 5F * factor}, i + 1);
        }
        return forZoom;
    }

    private int strokeIter;

    private double currentZoom() {
        if (designToolWidget != null) {
            return designToolWidget.getScene().getZoomFactor();
        }
        return 1;
    }

    private BasicStroke stroke() {
        BasicStroke[] strokes = strokesForZoom(currentZoom());
        return strokes[(strokeIter++) % strokes.length];
    }

    @Override
    public void paint(Graphics2D g, Rectangle layerBounds, boolean commit) {
        /*
        Collection<? extends ShapeElement> coll = widgetLookup.lookupAll(ShapeElement.class);
        Stroke stroke = null;
        if (!coll.isEmpty()) {
            g.setXORMode(Color.BLUE);
            g.setStroke(stroke = stroke());
            for (ShapeElement se : coll) {
                Shape shape = se.shape();
                g.draw(shape);
            }
            g.setPaintMode();
        }
        Collection<? extends ControlPoint> cps = widgetLookup.lookupAll(ControlPoint.class);
        if (!cps.isEmpty()) {
            double factor = 1D / currentZoom();
            g.setXORMode(Color.BLUE);
            g.setStroke(stroke == null ? stroke = stroke() : stroke);
            for (ControlPoint se : cps) {
                circ.setCenter(se.getX(), se.getY());
                circ.setRadius(SEL_CP_RADIUS * factor);
                g.draw(circ);
            }
            g.setPaintMode();
        }
         */
    }
    private final Circle circ = new Circle(0, 0, 1);

    private final MutableProxyLookup widgetLookup = new MutableProxyLookup();

    @Override
    public Customizer getCustomizer() {
        return new Cust(super.obj, widgetLookup);
    }

    static final class Cust implements Customizer<ShapesCollection> {

        private final ShapesCollection shapes;
        private final Lookup lookup;

        public Cust(ShapesCollection shapes, Lookup lookup) {
            this.shapes = shapes;
            this.lookup = lookup;
        }

        @Override
        public JComponent getComponent() {
            return new ShapeCollectionPanel(shapes, lookup);
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
}
