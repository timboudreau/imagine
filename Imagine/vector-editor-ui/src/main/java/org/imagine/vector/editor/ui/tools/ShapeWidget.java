/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import javax.swing.JPopupMenu;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.java.dev.imagine.api.vector.design.ControlPoint;
import org.imagine.utils.Holder;
import org.imagine.utils.java2d.GraphicsUtils;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import static org.imagine.vector.editor.ui.tools.ControlPointWidget.BASE_SIZE;
import org.imagine.vector.editor.ui.tools.DMA.TranslateHandler;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.PopupMenuProvider;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.paintui.widgetlayers.WidgetController;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Tim Boudreau
 */
class ShapeWidget extends Widget implements DragNotifier {

    private final ShapeElement entry;
    private final WidgetController ctrllr;
    private final ShapesCollection shapes;
    private ShapeElement temp;

    ShapeWidget(Scene scene, ShapeElement entry, ShapesCollection shapes, Holder<PaintParticipant.Repainter> repainter, WidgetController ctrllr, MutableProxyLookup lookup, Runnable refresh) {
        super(scene);
        this.shapes = shapes;
        this.ctrllr = ctrllr;
        this.entry = entry;
        getActions().addAction(new DoubleMoveAction(DoubleMoveStrategy.FREE, DMA.INSTANCE));
        getActions().addAction(ActionFactory.createPopupMenuAction(new PopupMenuProvider() {
            @Override
            public JPopupMenu getPopupMenu(Widget widget, Point localLocation) {
                Point2D scenePoint = DoubleMoveAction.convertLocalToScene(widget, localLocation);
                return ShapeActions.populatePopup(entry, shapes, refresh, widget, scenePoint);
            }
        }));
    }

    @Override
    public Lookup getLookup() {
        return Lookups.fixed(entry, entry.item(), th);
    }

    TranslateHandler th = new TranslateHandler() {
        @Override
        public void translate(double x, double y) {
            ShapeWidget.this.shapes.edit("Move", entry, () -> {
                entry.translate(x, y);
                revalidate();
                getScene().repaint();
            });
        }
    };

    @Override
    protected Rectangle calculateClientArea() {
        return entry().shape().getBounds();
    }
    @Override
    protected Graphics2D getGraphics() {
        Graphics2D g = super.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        return g;
    }

    @Override
    protected void paintWidget() {
        Graphics2D g = getGraphics();
        boolean adjusting = isAdjusting();
        if (adjusting) {
            g.setXORMode(Color.RED);
        } else {
            GraphicsUtils.setHighQualityRenderingHints(g);
        }
        entry().paint(g, g.getClipBounds());
        if (adjusting) {
            g.setPaintMode();
        }
    }

    boolean isAdjusting() {
        return temp != null;
    }

    @Override
    public boolean isHitAt(Point localLocation) {
        return entry().shape().contains(localLocation);
    }

    private ShapeElement entry() {
        if (temp != null) {
            return temp;
        }
        return entry;
    }

    @Override
    public ShapeElement onStartControlPointDrag() {
        return temp = entry.copy();
    }

    @Override
    public void onEndControlPointDrag() {
        temp = null;
        revalidate();
        repaint();
    }

    @Override
    public void onControlPointDragUpdate(int controlPointIndex, double offX, double offY) {
        if (temp != null) {
            ControlPoint[] pts = temp.controlPoints(BASE_SIZE, ignored -> {

            });
            ControlPoint pt = pts[controlPointIndex];
            pt.set(offX, offY);
        }
    }
}
