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
import java.util.Arrays;
import net.java.dev.imagine.spi.image.LayerImplementation;
import org.imagine.utils.painting.RepaintHandle;
import net.java.dev.imagine.spi.image.support.AbstractLayerImplementation;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

/**
 *
 * @author Tim Boudreau
 */
final class VectorLayer extends AbstractLayerImplementation {

    private final RepaintHandle repainter;
    private final Shapes shapes = new Shapes(true);
    private final VectorSurface surface;
    private final ToolWidgetSupplier widgetHooks = new ToolWidgetSupplier(this);
    private MPL lkp;

    VectorLayer(String name, RepaintHandle handle, Dimension size, VectorLayerFactory factory) {
        super(factory, true, name);
        repainter = handle;
        addRepaintHandle(handle);
        surface = new VectorSurface(shapes, getMasterRepaintHandle());
    }

    VectorLayer(VectorLayer layer, boolean deepCopy) {
        super(layer.getFactory(), layer.isResolutionIndependent(), layer.getName());
        repainter = layer.repainter;
        addRepaintHandle(layer.repainter);
        surface = new VectorSurface(shapes, getMasterRepaintHandle());
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
                Lookups.fixed(this, getLayer(),
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
    protected boolean paint(Graphics2D g, Rectangle bounds, boolean showSelection) {
        return shapes.paint(g, bounds);
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
                System.out.println("SET SECONDARIES: " + Arrays.toString(lkps));
                super.setLookups(ArrayUtils.concatenate(lkps,
                        defaultLookups));
            }
        }
    }
}
