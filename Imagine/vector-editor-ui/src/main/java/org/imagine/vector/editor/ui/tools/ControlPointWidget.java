/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JPopupMenu;
import net.java.dev.imagine.api.vector.design.ControlPoint;
import org.imagine.editor.api.Zoom;
import org.imagine.geometry.Circle;
import org.imagine.utils.java2d.GraphicsUtils;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import org.imagine.vector.editor.ui.tools.DMA.TranslateHandler;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.api.visual.widget.Widget.Dependency;
import org.netbeans.paintui.widgetlayers.WidgetController;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Tim Boudreau
 */
class ControlPointWidget extends Widget implements Dependency {

    static final double BASE_SIZE = 6;
    private final ControlPoint cp;
    private final WidgetController ctrllr;
    private final ShapeElement element;
    private final DragNotifier notifier;

    public ControlPointWidget(Scene scene, ShapeElement en,
            ControlPoint cp, WidgetController ctrllr, ShapesCollection shapes,
            DragNotifier notifier, Runnable refresh) {
        super(scene);
        this.element = en;
        this.cp = cp;
        //            setPreferredLocation(new Point((int) Math.round(cp.getX()), (int) Math.round(cp.getY())));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        getActions().addAction(new DoubleMoveAction(
                DoubleMoveStrategy.FREE, DMA.INSTANCE));
        this.ctrllr = ctrllr;
        this.notifier = notifier;
        getActions().addAction(ActionFactory.createPopupMenuAction((Widget widget, Point localLocation) -> {
            JPopupMenu menu = ShapeActions.populatePopup(cp, en, shapes, refresh, widget, localLocation);
            return menu;
        }));
    }

    TranslateHandler th = new TranslateHandler() {
        @Override
        public void onStart(Point p) {
            if (!checkValid()) {
                return;
            }
            notifier.onStartControlPointDrag();
        }

        @Override
        public void onMove(double offX, double offY) {
            if (!checkValid()) {
                return;
            }
            notifier.onControlPointDragUpdate(cp.index(), cp.getX() + offX,
                    cp.getY() + offY);
        }

        @Override
        public void translate(double x, double y) {
            if (!checkValid()) {
                return;
            }
            cp.move(x, y);
            notifier.onEndControlPointDrag();
            revalidate();
            getScene().repaint();
        }
    };

    @Override
    public Lookup getLookup() {
        return Lookups.fixed(element, cp, th, element.item());
    }

    private Circle circle() {
        Zoom zoom = ctrllr.getZoom();
        Point loc = getPreferredLocation();
        Circle circ;
        if (loc == null) {
            circ = new Circle(cp.getX(), cp.getY(), zoom.inverseScale(BASE_SIZE));
        } else {
            circ = new Circle(loc.x, loc.y, zoom.inverseScale(BASE_SIZE));
        }
        return circ;
    }

    private boolean removed;

    private boolean checkValid() {
        if (removed) {
            return false;
        }
        boolean result = cp.isValid();
        if (!result) {
            removed = true;
            EventQueue.invokeLater(() -> {
                Scene scene = getScene();
                removeFromParent();
                scene.validate();
            });
        }
        return result;
    }

    @Override
    protected void paintWidget() {
        if (!checkValid()) {
            return;
        }
        Graphics2D g1 = getGraphics();
        GraphicsUtils.setHighQualityRenderingHints(g1);
        Circle circ = circle();
        g1.setStroke(ctrllr.getZoom().getStroke());
        g1.setColor(cp.isVirtual() ? Color.WHITE : Color.ORANGE);
        g1.fill(circ);
        g1.setColor(new Color(128, 128, 255));
        g1.draw(circ);
    }

    @Override
    protected Rectangle calculateClientArea() {
        if (!checkValid()) {
            return new Rectangle();
        }
        return circle().getBounds();
    }

    @Override
    public void revalidateDependency() {
        if (removed) {
            return;
        }
        revalidate();
    }
}
