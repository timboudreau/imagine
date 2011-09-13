/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paintui;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import net.java.dev.imagine.api.image.Layer;
import net.java.dev.imagine.spi.image.LayerImplementation;
import net.java.dev.imagine.spi.image.RepaintHandle;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.paint.api.util.GraphicsUtils;

/**
 *
 * @author Tim Boudreau
 */
class OneLayerWidget extends Widget {
    private final InternalRepaintHandle repaintHandle = new InternalRepaintHandle();
    final LayerImplementation layer;
    private boolean validated;
    private final PCL pcl = new PCL();

    public OneLayerWidget(LayerImplementation layer, Scene scene) {
        super(scene);
        this.layer = layer;
        setOpaque(false);
    }

    void addNotify() {
        layer.addRepaintHandle(repaintHandle);
        layer.addPropertyChangeListener(pcl);
    }

    void removeNotify() {
        layer.removeRepaintHandle(repaintHandle);
        layer.removePropertyChangeListener(pcl);
    }

    @Override
    public boolean isValidated() {
        return validated;
    }

    @Override
    protected Rectangle calculateClientArea() {
        return new Rectangle(layer.getBounds());
    }

    @Override
    protected void paintWidget() {
        validated = true;
        Rectangle r = getBounds();
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
        try {
            layer.paint(g, r, true);
        } finally {
            if (old != null) {
                g.setComposite(old);
            }
        }
    }

    public String toString() {
        return "Widget for " + layer + " at " + getPreferredBounds() + " " + getPreferredLocation();
    }

    private final class PCL implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (Layer.PROP_VISIBLE.equals(evt.getPropertyName())) {
                setVisible(layer.isVisible());
            } else if (Layer.PROP_BOUNDS.equals(evt.getPropertyName())) {
                setPreferredLocation(((Rectangle) evt.getNewValue()).getLocation());
                setPreferredSize(((Rectangle) evt.getNewValue()).getSize());
                repaintHandle.repaint();
            } else if (!Layer.PROP_NAME.equals(evt.getPropertyName())) {
                repaintHandle.repaintArea(layer.getBounds());
                ((PictureScene) getScene()).revalidate();
                ((PictureScene) getScene()).getView().paintImmediately(new Rectangle(0, 0, 1000, 1000)); //XXX
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
            validated = false;
            revalidate();
            OneLayerWidget.this.repaint();
        }

        @Override
        public void setCursor(Cursor cursor) {
            ((PictureScene) getScene()).setCursor(cursor);
            //                OneLayerWidget.this.setCursor(cursor);
        }
    }
    
}
