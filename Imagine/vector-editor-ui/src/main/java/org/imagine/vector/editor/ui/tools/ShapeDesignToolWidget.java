package org.imagine.vector.editor.ui.tools;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.java.dev.imagine.api.vector.design.ControlPoint;
import org.imagine.editor.api.Zoom;
import org.imagine.utils.Holder;
import org.imagine.vector.editor.ui.spi.LayerRenderingWidget;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.widget.LayerWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.paintui.widgetlayers.SetterResult;
import org.netbeans.paintui.widgetlayers.WidgetController;
import org.openide.util.Lookup;
import org.openide.util.WeakListeners;

/**
 *
 * @author Tim Boudreau
 */
class ShapeDesignToolWidget extends LayerRenderingWidget {

    private double opacity = 1;
    private final MutableProxyLookup lookup = new MutableProxyLookup();
    private final LayerWidget shapesLayer;
    private final LayerWidget controlPointsLayer;

    private Map<ShapeElement, ShapeWidget> widgetForShape
            = new HashMap<>(50);
    private Map<ControlPoint, ControlPointWidget> controlPointWidgetForControlPoint = new HashMap<>(120);
    private final ShapesCollection shapes;
    private final Holder<PaintParticipant.Repainter> repainter;
    private final WidgetController widgetController;
    private final WidgetController ctrllr;

    public ShapeDesignToolWidget(Scene scene, ShapesCollection shapes,
            Lookup layerLookup, Holder<PaintParticipant.Repainter> repainter,
            WidgetController ctrllr) {
        super(scene);
        this.shapes = shapes;
        this.repainter = repainter;
        this.widgetController = ctrllr;
        this.ctrllr = ctrllr;
        shapesLayer = new LayerWidget(scene);
        controlPointsLayer = new LayerWidget(scene);
        addChild(shapesLayer);
        addChild(controlPointsLayer);
        FocusAction fa = new FocusAction(lookup);
        for (ShapeElement entry : shapes) {
            ShapeWidget sw = new ShapeWidget(scene, entry, shapes, repainter, ctrllr, lookup, this::sync);
            sw.getActions().addAction(0, fa);
            shapesLayer.addChild(sw);
            ctrllr.getZoom().addChangeListener(WeakListeners.change((evt) -> {
                scene.validate();
            }, ctrllr.getZoom()));
            ControlPoint[] pts = entry.controlPoints(9, (cp) -> {
                entry.changed();
                repainter.ifSet((rp) -> {
                    scene.validate();
                    rp.requestRepaint(entry.shape().getBounds());
                });
            });

            for (int i = 0; i < pts.length; i++) {
                ControlPoint cp = pts[i];
                ControlPointWidget w = new ControlPointWidget(scene, entry, cp, ctrllr, shapes, sw, this::sync);
                sw.addDependency(w);
                w.getActions().addAction(0, fa);
                controlPointsLayer.addChild(w);
            }
        }
    }

    private void sync() {

    }

    static class FocusAction extends WidgetAction.Adapter {

        private final MutableProxyLookup lookup;

        public FocusAction(MutableProxyLookup lookup) {
            this.lookup = lookup;
        }

        @Override
        public State mousePressed(Widget widget, WidgetMouseEvent event) {
            lookup.lookups(widget.getLookup());
            return WidgetAction.State.CHAIN_ONLY;
        }

        @Override
        public State focusGained(Widget widget, WidgetFocusEvent event) {
            lookup.lookups(widget.getLookup());
            return WidgetAction.State.CHAIN_ONLY;
        }
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }

    @Override
    public void setLookupConsumer(Consumer<Lookup[]> additionaLookupConsumer) {
        additionaLookupConsumer.accept(new Lookup[]{lookup});
    }

    @Override
    public SetterResult setOpacity(double opacity) {
        this.opacity = opacity;
        return SetterResult.HANDLED;
    }

    @Override
    public SetterResult setLocation(Point location) {
        setPreferredLocation(location);
        return SetterResult.HANDLED;
    }

    @Override
    public void setZoom(Zoom zoom) {
    }

    @Override
    protected Graphics2D getGraphics() {
        Graphics2D result = super.getGraphics();
        Composite oldComposite = null;
        try {
            if (result != null && opacity != 1D) {
                oldComposite = result.getComposite();
                AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) opacity);
                result.setComposite(alpha);
            }
            return result;
        } finally {
            if (oldComposite != null) {
                result.setComposite(oldComposite);
            }
        }
    }
}
