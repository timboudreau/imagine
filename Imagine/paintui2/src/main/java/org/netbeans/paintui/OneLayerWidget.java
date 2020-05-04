package org.netbeans.paintui;

import com.mastfrog.util.collections.ArrayUtils;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import net.java.dev.imagine.api.image.Layer;
import net.java.dev.imagine.api.image.RenderingGoal;
import net.java.dev.imagine.spi.image.LayerImplementation;
import org.imagine.editor.api.ContextLog;
import org.imagine.utils.painting.RepaintHandle;
import org.netbeans.api.visual.border.BorderFactory;
import org.netbeans.api.visual.widget.Widget;
import org.imagine.utils.java2d.GraphicsUtils;
import org.netbeans.api.visual.model.ObjectState;
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

    private static final ContextLog ALOG = ContextLog.get("toolactions");
    private final InternalRepaintHandle repaintHandle = new InternalRepaintHandle();
    final LayerImplementation layer;
    private final PCL pcl = new PCL();
    private final WidgetLayer widgetLayer;
    private LKP lkp;
    private WidgetFactory currentFactory;
    private boolean attached;

    public OneLayerWidget(LayerImplementation layer, PictureScene scene, WidgetLayer widgetLayer) {
        super(scene);
        this.layer = layer;
        setOpaque(false);
        setVisible(layer.isVisible());
        setBorder(BorderFactory.createEmptyBorder());
        // XXX listen for changes of WidgetLayer result in layer's lookup?
        this.widgetLayer = widgetLayer;
        lkp = new LKP(Lookups.fixed(this), layer.getLookup());
    }

    @Override
    public Lookup getLookup() {
        return lkp;
    }

    @Override
    protected void notifyStateChanged(ObjectState previousState, ObjectState state) {
        ALOG.log(() -> "LayerWidget " + layer.getName() + " state "
            + previousState + " -> " + state);
    }

    public boolean hasWidgetLayer() {
        return attached;
    }

    public boolean isLayersWidgetActive() {
        return hasWidgetLayer() && currentFactory != null
                && currentFactory.isUsingInternalWidget();
    }

    @Override
    protected void notifyAdded() {
        layer.addRepaintHandle(repaintHandle);
        layer.addPropertyChangeListener(pcl);
        if (widgetLayer != null) {
            currentFactory = widgetLayer.createWidgetController(this, scene());
            if (currentFactory != null) {
                attached = true;
                currentFactory.setAspectRatio(scene().aspectRatio());
                currentFactory.setOpacity(layer.getOpacity());
                currentFactory.setName(layer.getName());
//                factory.setLocation(layer.getBounds().getLocation());
                currentFactory.attach(lkp::lookups);
            } else {
                attached = false;
            }
        }
    }

    @Override
    protected void notifyRemoved() {
        layer.removeRepaintHandle(repaintHandle);
        layer.removePropertyChangeListener(pcl);
        if (currentFactory != null) {
            currentFactory.detach();
            attached = false;
            currentFactory = null;
        }
    }

    @Override
    protected Rectangle calculateClientArea() {
        if (currentFactory != null) {
            return super.calculateClientArea();
        }
        Rectangle result = layer.getBounds();
        result.width += Math.max(0, result.x);
        result.height += Math.max(0, result.y);
        result.x = 0;
        result.y = 0;
        if (result.isEmpty()) {
            Dimension d = ((PictureScene) getScene()).picture().getPicture().getSize();
            return new Rectangle(0, 0, d.width, d.height);
        }
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
            layer.paint(RenderingGoal.EDITING, g, null, false, false, scene().getZoom(),
                    scene().aspectRatio());
        } finally {
            if (old != null) {
                g.setComposite(old);
            }
        }
    }

    @Override
    public String toString() {
        return "Widget for " + layer;
    }

    private PictureScene scene() {
        return (PictureScene) getScene();
    }

    private final class PCL implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
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
