package org.imagine.vector.editor.ui;

import com.mastfrog.util.collections.ArrayUtils;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import net.java.dev.imagine.api.image.RenderingGoal;
import net.java.dev.imagine.spi.image.LayerImplementation;
import org.imagine.utils.painting.RepaintHandle;
import net.java.dev.imagine.spi.image.support.AbstractLayerImplementation;
import org.imagine.editor.api.AspectRatio;
import org.imagine.editor.api.Zoom;
import com.mastfrog.geometry.util.GeometryStrings;
import com.mastfrog.geometry.util.PooledTransform;
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
    private Dimension canvasSize;

    VectorLayer(String name, RepaintHandle handle, Dimension canvasSize, VectorLayerFactoryImpl factory) {
        this(name, handle, canvasSize, factory, new Shapes(true));
    }

    VectorLayer(String name, RepaintHandle handle, Dimension canvasSize, VectorLayerFactoryImpl factory, Shapes shapes) {
        super(factory, true, name);
        this.canvasSize = canvasSize;
        assert canvasSize != null : "Null canvas size";
        this.shapes = shapes;
        repainter = handle;
        addRepaintHandle(handle);
        surface = new VectorSurface(shapes, getMasterRepaintHandle());
    }

    VectorLayer(VectorLayer layer, boolean deepCopy) {
        super(layer.getFactory(), layer.isResolutionIndependent(), layer.getName());
        repainter = layer.repainter;
        canvasSize = new Dimension(layer.canvasSize);
        addRepaintHandle(layer.repainter);
        this.shapes = deepCopy ? layer.shapes.copy() : layer.shapes;
        surface = new VectorSurface(this.shapes, getMasterRepaintHandle());
    }

    public boolean hasActiveInternalWidget() {
        return widgetHooks.isWidgetActive();
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
        Rectangle base = shapes.getBounds();
        if (base.width < 0) {
            base.width += base.width;
        }
        if (base.height < 0) {
            base.height += base.height;
        }
        return new Rectangle(0, 0, canvasSize.width, canvasSize.height);
    }

    @Override
    public Rectangle getContentBounds() {
        return shapes.getBounds();
    }

    @Override
    public void resize(int width, int height) {
        resize(width, height, false);
    }

    @Override
    public void resize(int width, int height, boolean canvasOnly) {
        int oldW = canvasSize.width;
        int oldH = canvasSize.height;
        canvasSize.width = width;
        canvasSize.height = height;
        if (canvasOnly) {
            firePropertyChange(PROP_BOUNDS, new Rectangle(0, 0, oldW, oldH),
                    new Rectangle(0, 0, width, height));
            return;
        }
        Rectangle oldBounds = getBounds();
        Dimension oldSize = oldBounds.getSize();
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
        PooledTransform.withScaleInstance(xfactor, yfactor, shapes::applyTransform);
        firePropertyChange(PROP_BOUNDS, oldBounds, getBounds());
    }

    @Override
    public LayerImplementation clone(boolean isUserCopy, boolean deepCopy) {
        return new VectorLayer(this, deepCopy);
    }

    @Override
    public void commitLastPropertyChangeToUndoHistory() {
    }

    @Override
    protected boolean paint(RenderingGoal goal, Graphics2D g, Rectangle bounds,
            boolean showSelection, Zoom zoom, AspectRatio ratio) {
        if (bounds != null && goal == RenderingGoal.THUMBNAIL) {
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
        boolean result = false;
        if (!surface.toolIsPaintingScene() || goal == RenderingGoal.THUMBNAIL || goal == RenderingGoal.PRODUCTION) {
            result = shapes.paint(goal, g, bounds, zoom, ratio);
        }
        if (goal.isEditing()) {
            result |= surface.paint(goal, g, bounds, zoom);
        }
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
