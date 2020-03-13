package org.netbeans.paintui;

import com.mastfrog.util.collections.ArrayUtils;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import net.java.dev.imagine.api.image.Layer;
import net.java.dev.imagine.spi.image.LayerImplementation;
import org.imagine.utils.painting.RepaintHandle;
import org.netbeans.api.visual.border.BorderFactory;
import org.netbeans.api.visual.widget.Widget;
import org.imagine.utils.java2d.GraphicsUtils;
import org.netbeans.paintui.widgetlayers.WidgetLayer;
import org.netbeans.paintui.widgetlayers.WidgetFactory;
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
    private LKP lkp;

    public OneLayerWidget(LayerImplementation layer, PictureScene scene, WidgetLayer widgetLayer) {
        super(scene);
        this.layer = layer;
        setOpaque(false);
        layer.addRepaintHandle(repaintHandle);
        layer.addPropertyChangeListener(pcl);
        setBorder(BorderFactory.createEmptyBorder());
        this.widgetLayer = widgetLayer;
        lkp = new LKP(Lookups.fixed(this), layer.getLookup());
        addNotify();
    }

    @Override
    public Lookup getLookup() {
        return lkp;
    }

    private WidgetFactory factory;
    private boolean attached;
    void addNotify() {
        if (widgetLayer != null) {
            factory = widgetLayer.createWidgetController(this, (PictureScene) getScene());
            if (factory != null) {
                attached = true;
                factory.attach(lkp::lookups);
            } else {
                attached = false;
            }
        }
    }

    void removeNotify() {
        layer.removeRepaintHandle(repaintHandle);
        layer.removePropertyChangeListener(pcl);
        if (factory != null) {
            factory.detach();
            attached = false;
            factory = null;
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
        Rectangle result = layer.getBounds();
        result.width += Math.max(0, result.x);
        result.height += Math.max(0, result.y);
        result.x = 0;
        result.y = 0;
        return result;
    }

    @Override
    protected void paintWidget() {
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
//            layer.paint(g, layer.getBounds(), false, false, ((PictureScene)getScene()).getZoom());
            layer.paint(g, null, false, false, ((PictureScene)getScene()).getZoom());
        } finally {
            if (old != null) {
                g.setComposite(old);
            }
        }
    }

    @Override
    public String toString() {
        return "Widget for " + layer + " at " 
                + getPreferredBounds() + " "
                + getPreferredLocation();
    }

    private final class PCL implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            System.out.println("Prop change from layer " + evt.getPropertyName() + " " + evt.getNewValue());
            if (Layer.PROP_VISIBLE.equals(evt.getPropertyName())) {
                setVisible(layer.isVisible());
                getScene().validate();
            } else if (Layer.PROP_BOUNDS.equals(evt.getPropertyName())) {
                revalidate();
                repaintHandle.repaint();
                getScene().getView().paintImmediately(new Rectangle(0, 0, 2000, 2000)); //XXX
            } else if (!Layer.PROP_NAME.equals(evt.getPropertyName())) {
                repaintHandle.repaintArea(layer.getBounds());
            }
        }
    }

    private final class InternalRepaintHandle implements RepaintHandle {

        public void repaint() {
            repaintArea(layer.getBounds());
        }

        @Override
        public void repaintArea(Rectangle r) {
            repaintArea(r.x, r.y, r.width, r.height);
        }

        @Override
        public void repaintArea(int x, int y, int w, int h) {
            JComponent v = getScene().getView();
            if (v != null) {
                Rectangle r = new Rectangle(x, y, w, h);
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
        }
    }

    static class LKP extends ProxyLookup {
        private final Lookup[] defaults;

        public LKP(Lookup... defaults) {
            super(defaults);
            this.defaults = defaults;
        }

        void lookups(Lookup... lkps) {
            if (lkps.length > 0) {
                setLookups(ArrayUtils.concatenate(lkps, defaults));
            } else {
                setLookups(defaults);
            }
        }
    }
}
