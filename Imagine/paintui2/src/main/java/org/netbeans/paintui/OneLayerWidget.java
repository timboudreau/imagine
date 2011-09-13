/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paintui;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import net.java.dev.imagine.api.image.Layer;
import net.java.dev.imagine.spi.image.LayerImplementation;
import net.java.dev.imagine.spi.image.RepaintHandle;
import org.netbeans.api.visual.border.BorderFactory;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.paint.api.util.GraphicsUtils;
import org.netbeans.paintui.widgetlayers.WidgetLayer;
import org.netbeans.paintui.widgetlayers.WidgetLayer.WidgetController;
import org.netbeans.paintui.widgetlayers.WidgetLayer.WidgetFactory;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

/**
 *
 * @author Tim Boudreau
 */
final class OneLayerWidget extends Widget {
    private final InternalRepaintHandle repaintHandle = new InternalRepaintHandle();
    final LayerImplementation layer;
    private final PCL pcl = new PCL();
    private final WidgetLayer widgetLayer;

    public OneLayerWidget(LayerImplementation layer, Scene scene, WidgetLayer widgetLayer) {
        super(scene);
        this.layer = layer;
        setOpaque(false);
        layer.addRepaintHandle(repaintHandle);
        layer.addPropertyChangeListener(pcl);
        setBorder(BorderFactory.createEmptyBorder());
        this.widgetLayer = widgetLayer;
        addNotify();
    }

    @Override
    public Lookup getLookup() {
        return new ProxyLookup(super.getLookup(), Lookups.fixed(this), layer.getLookup());
    }
    
    private WidgetFactory factory;
    void addNotify() {
        if (widgetLayer != null) {
            factory = widgetLayer.createWidgetController(this, new WidgetController(){});
            if (factory != null) {
                factory.attach();
            }
        }
    }

    void removeNotify() {
        layer.removeRepaintHandle(repaintHandle);
        layer.removePropertyChangeListener(pcl);
        if (factory != null) {
            factory.detach();
        }
    }

    @Override
    protected Rectangle calculateClientArea() {
        if (factory != null) {
            return super.calculateClientArea();
        }
        /*
        //Have the layer corner always at 0,0
        Rectangle r = new Rectangle(layer.getBounds());
        if (r.x > 0) {
            r.width += r.x;
            r.x = 0;
        }
        if (r.y > 0) {
            r.height += r.y;
            r.y = 0;
        }
        return r;
        */
        return layer.getBounds();
    }

    @Override
    protected void paintWidget() {
        Rectangle r = getBounds();
        Point p = r.getLocation();
        r.x = 0;
        r.y = 0;
        Graphics2D g = getGraphics();
        GraphicsUtils.setHighQualityRenderingHints(g);
        Composite old = null;
        float opacity = layer.getOpacity();
        if (opacity != 1.0F) {
            old = g.getComposite();
            AlphaComposite comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
            g.setComposite(comp);
        }
        g.translate(p.x, p.y);
        try {
            layer.paint(g, r, true);
        } finally {
            if (old != null) {
                g.setComposite(old);
            }
            g.translate(-p.x, -p.y);
        }
    }

    public String toString() {
        return "Widget for " + layer + " at " + getPreferredBounds() + " " + getPreferredLocation();
    }

    private final class PCL implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            System.out.println("Prop change from layer " + evt.getPropertyName() + " " + evt.getNewValue());
            if (Layer.PROP_VISIBLE.equals(evt.getPropertyName())) {
                setVisible(layer.isVisible());
            } else if (Layer.PROP_BOUNDS.equals(evt.getPropertyName())) {
                revalidate();
//                setPreferredLocation(((Rectangle) evt.getNewValue()).getLocation());
//                setPreferredBounds((Rectangle) evt.getNewValue());
                repaintHandle.repaint();
                getScene().getView().paintImmediately(new Rectangle(0,0, 2000,2000)); //XXX
            } else if (!Layer.PROP_NAME.equals(evt.getPropertyName())) {
                repaintHandle.repaintArea(layer.getBounds());
//                ((PictureScene) getScene()).revalidate();
//                ((PictureScene) getScene()).getView().paintImmediately(new Rectangle(0, 0, 1000, 1000)); //XXX
            }
        }
    }

    private final class InternalRepaintHandle implements RepaintHandle {

        public void repaint() {
            repaintArea(layer.getBounds());
        }

        public void repaintArea(Rectangle r) {
            repaintArea(r.x, r.y, r.width, r.height);
        }

        @Override
        public void repaintArea(int x, int y, int w, int h) {
            JComponent v = getScene().getView();
            if (v != null) {
                Rectangle r = new Rectangle (x, y, w, h);
                r = convertLocalToScene(r);
                r = getScene().convertSceneToView(r);
                v.repaint(r);
            } else {
                OneLayerWidget.this.repaint();
            }
            
        }

        @Override
        public void setCursor(Cursor cursor) {
            ((PictureScene) getScene()).setCursor(cursor);
            //                OneLayerWidget.this.setCursor(cursor);
        }
    }
    
}
