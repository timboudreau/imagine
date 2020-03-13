/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui;

import com.mastfrog.util.collections.ArrayUtils;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import net.java.dev.imagine.spi.image.LayerImplementation;
import org.imagine.utils.painting.RepaintHandle;
import net.java.dev.imagine.spi.image.support.AbstractLayerImplementation;
import org.imagine.editor.api.Zoom;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

/**
 *
 * @author Tim Boudreau
 */
final class VectorLayer extends AbstractLayerImplementation {

    private final RepaintHandle repainter;
    private final Shapes shapes;
    private final VectorSurface surface;
    private final ToolWidgetSupplier widgetHooks = new ToolWidgetSupplier(this);
    private MPL lkp;

    VectorLayer(String name, RepaintHandle handle, VectorLayerFactory factory) {
        this(name, handle, factory, new Shapes(true));
    }

    VectorLayer(String name, RepaintHandle handle, VectorLayerFactory factory, Shapes shapes) {
        super(factory, true, name);
        this.shapes = shapes;
        repainter = handle;
        addRepaintHandle(handle);
        surface = new VectorSurface(shapes, getMasterRepaintHandle());
    }

    VectorLayer(VectorLayer layer, boolean deepCopy) {
        super(layer.getFactory(), layer.isResolutionIndependent(), layer.getName());
        repainter = layer.repainter;
        addRepaintHandle(layer.repainter);
        this.shapes = deepCopy ? layer.shapes.copy() : layer.shapes;
        surface = new VectorSurface(this.shapes, getMasterRepaintHandle());
    }

    @Override
    public String toString() {
        return "VectorLayer(" + Long.toString(System.identityHashCode(this), 36)
                + " with " + shapes.size() + " shapes hash "
                + shapes.hashCode() + ")";
    }

    void setAdditionalLookups(Lookup... lkps) {
        if (lkp == null) {
            getLookup();
        }
        lkp.setSecondaries(lkps);
    }

    public VectorSurface surface() {
        return surface;
    }

    @Override
    protected MPL createLookup() {
        return lkp != null ? lkp : (lkp = new MPL(
                Lookups.fixed(this, getLayer(), ShapesLayerSave.INSTANCE,
                        new RepaintProxyShapes(shapes, getMasterRepaintHandle()),
                        surface, surface.getSurface(), widgetHooks,
                        surface.transformReceiver())));
    }

    @Override
    public Rectangle getBounds() {
        return shapes.getBounds();
    }

    @Override
    public void resize(int width, int height) {
        Dimension oldSize = getBounds().getSize();
        if (width == oldSize.width && height == oldSize.height) {
            return;
        }
        if (width == 0 || height == 0) {
            return;
        }
        double ow = oldSize.width;
        double oh = oldSize.height;
        double nw = width;
        double nh = height;
        double xfactor = nw / ow;
        double yfactor = nh / oh;
        AffineTransform xform = AffineTransform.getScaleInstance(xfactor, yfactor);
        shapes.applyTransform(xform);
    }

    @Override
    public LayerImplementation clone(boolean isUserCopy, boolean deepCopy) {
        return new VectorLayer(this, deepCopy);
    }

    @Override
    public void commitLastPropertyChangeToUndoHistory() {
    }

    @Override
    protected boolean paint(Graphics2D g, Rectangle bounds, boolean showSelection, Zoom zoom) {
        if (bounds != null) {
            if (bounds.isEmpty()) {
                return false;
            }
            g = (Graphics2D) g.create();
            Rectangle r = getBounds();
            if (r.isEmpty()) {
                return false;
            }
            g.scale(bounds.getWidth() / r.getWidth(), bounds.getHeight() / r.getHeight());
        }
        boolean result = shapes.paint(g, bounds, zoom);
        result |= surface.paint(g, bounds, zoom);
        if (bounds != null) {
            g.dispose();
        }
        return result;
    }

    static class MPL extends ProxyLookup {

        private final Lookup[] defaultLookups;

        public MPL(Lookup... lookups) {
            super(lookups);
            defaultLookups = lookups;
        }

        void setSecondaries(Lookup... lkps) {
            if (lkps.length == 0) {
                super.setLookups(defaultLookups);
            } else {
                super.setLookups(ArrayUtils.concatenate(lkps,
                        defaultLookups));
            }
        }
    }
}
