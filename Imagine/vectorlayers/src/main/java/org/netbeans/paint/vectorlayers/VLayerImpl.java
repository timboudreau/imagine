/*
 * VLayerImpl.java
 *
 * Created on October 25, 2006, 3:06 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.netbeans.paint.vectorlayers;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import net.dev.java.imagine.api.selection.ObjectSelection;
import net.dev.java.imagine.api.selection.ShapeConverter;
import net.dev.java.imagine.api.selection.Universe;
import net.java.dev.imagine.api.image.RenderingGoal;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.Volume;
import net.java.dev.imagine.effects.api.EffectReceiver;
import net.java.dev.imagine.effects.spi.ImageSource;
import net.java.dev.imagine.spi.image.LayerImplementation;
import org.imagine.utils.painting.RepaintHandle;
import net.java.dev.imagine.spi.image.support.AbstractLayerImplementation;
import org.imagine.editor.api.AspectRatio;
import org.imagine.editor.api.Zoom;
import org.netbeans.paint.api.editing.LayerFactory;
import org.imagine.utils.java2d.GraphicsUtils;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 * Vector layer implementation.
 *
 * @author Tim Boudreau
 */
class VLayerImpl extends AbstractLayerImplementation {

    private final VSurfaceImpl surface;

    public VLayerImpl(LayerFactory factory, RepaintHandle handle, String name, Dimension d) {
        super(factory, true, name);
        addRepaintHandle(handle);
        surface = new VSurfaceImpl(getMasterRepaintHandle(), d, this::isVisible);
    }

    VLayerImpl(VLayerImpl other) {
        super(other.getLookup().lookup(LayerFactory.class), true, other.getName());
        addRepaintHandle(other.getMasterRepaintHandle());
        this.surface = new VSurfaceImpl(other.surface);
    }

    @Override
    public Object clone() {
        return new VLayerImpl(this);
    }

    public Rectangle getBounds() {
        return surface.getBounds();
    }

    private final ObjectSelection<Collection<Primitive>> sel = new ObjectSelection(Collection.class, new Universe<Collection<Primitive>>() {
        @Override
        public Collection<Primitive> getAll() {
            return surface.stack.primitives;
        }
    }, new ShapeConverter<Collection<Primitive>>() {
        @Override
        public Class<Collection<Primitive>> type() {
            Class c = Collection.class;
            return c;
        }

        @Override
        public Shape toShape(Collection<Primitive> obj) {
            if (obj.isEmpty()) {
                return new Rectangle();
            }
            int minX, minY, maxX, maxY;
            minX = minY = Integer.MAX_VALUE;
            maxX = maxY = Integer.MIN_VALUE;
            Rectangle2D.Double scratch = new Rectangle2D.Double();
            for (Primitive p : obj) {
                if (p instanceof Volume) {
                    Volume v = (Volume) p;
                    v.getBounds(scratch);
                    minX = (int) Math.min(minX, Math.floor(scratch.x));
                    minY = (int) Math.min(minY, Math.floor(scratch.y));
                    maxX = (int) Math.max(maxX, Math.floor(scratch.x + scratch.width));
                    maxY = (int) Math.max(maxY, Math.floor(scratch.y + scratch.height));
                }
            }
            if (minX == Integer.MAX_VALUE) {
                return new Rectangle();
            }
            return new Rectangle(minX, minY, maxX - minX, maxY - minY);
        }
    });

    ;

    public void commitLastPropertyChangeToUndoHistory() {
    }

    @Override
    public boolean paint(RenderingGoal goal, Graphics2D g, Rectangle bounds, boolean showSelection, Zoom zoom, AspectRatio ratio) {
        Composite old = null;
        float opacity = getOpacity();
        if (opacity != 1.0F) {
            old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                    opacity));
        }

        boolean result = surface.paint(goal, g, bounds, zoom);
        if (old != null) {
            g.setComposite(old);
        }
        if (showSelection && goal.isEditing()) {
            sel.paint(g, bounds);
        }
//        System.out.println(surface.stack.dump());
        return result;
    }

    public LayerImplementation clone(boolean isUserCopy, boolean deepCopy) {
        VLayerImpl result = new VLayerImpl(getLookup().lookup(LayerFactory.class),
                getMasterRepaintHandle(),
                getName(), getBounds().getSize());
        return result;
    }

    @Override
    protected Lookup createLookup() {
        return Lookups.fixed(
                layer,
                surface,
                surface.getSurface(),
                surface.stack, 
                surface.getGraphics(),
                new CompositeReceiver(),
                new IS());
    }

    class CompositeReceiver extends EffectReceiver<Composite> {

        public CompositeReceiver() {
            super(Composite.class);
        }

        @Override
        public boolean canApplyEffects() {
            return isVisible();
        }

        @Override
        protected <ParamType> boolean onApply(Composite effect) {
            surface.applyComposite(effect);
            return true;
        }

        @Override
        public Dimension getSize() {
            return surface.getSize();
        }
    }

    class IS implements ImageSource {

        @Override
        public BufferedImage getRawImage() {
            Dimension d = surface.getSize();
            return GraphicsUtils.newBufferedImage(d.width, d.height, surface::paint);
        }

        @Override
        public BufferedImage createImageCopy(Dimension size) {
            Dimension d = surface.getSize();
            return GraphicsUtils.newBufferedImage(size.width, size.height, g -> {
                // XXX translate to surface location?
                AffineTransform xform = AffineTransform.getScaleInstance((double) size.getWidth() / (double) d.getWidth(),
                        (double) size.getHeight() / (double) d.getHeight());
                g.setTransform(xform);
                surface.paint(RenderingGoal.PRODUCTION, g, null, Zoom.ONE_TO_ONE);
            });
        }
    }

    @Override
    public void resize(int width, int height) {
        Dimension size = surface.getSize();
        AffineTransform xform = AffineTransform.getScaleInstance((double) size.getWidth() / (double) width,
                (double) size.getHeight() / (double) height);
        surface.transform(xform);
    }
}
