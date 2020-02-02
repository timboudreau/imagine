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
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import javax.swing.event.SwingPropertyChangeSupport;
import net.java.dev.imagine.effects.api.EffectReceiver;
import net.java.dev.imagine.effects.spi.ImageSource;
import net.java.dev.imagine.spi.image.LayerImplementation;
import net.java.dev.imagine.spi.image.RepaintHandle;
import org.netbeans.paint.api.editing.LayerFactory;
import org.netbeans.paint.api.util.GraphicsUtils;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 * Vector layer implementation.
 *
 * @author Tim Boudreau
 */
class VLayerImpl extends LayerImplementation {

    private final VSurfaceImpl surface;
    private String name = "Fred"; //NOI18N

    public VLayerImpl(LayerFactory factory, RepaintHandle handle, String name, Dimension d) {
        super(factory);
        addRepaintHandle(handle);
        surface = new VSurfaceImpl(getMasterRepaintHandle(), d);
        this.name = name;
    }

    VLayerImpl(VLayerImpl other) {
        super(other.getLookup().lookup(LayerFactory.class));
        addRepaintHandle(other.getMasterRepaintHandle());
        this.name = other.name;
        this.visible = other.visible;
        this.opacity = other.opacity;
        this.surface = new VSurfaceImpl(other.surface);
    }

    public Object clone() {
        return new VLayerImpl(this);
    }

    public Rectangle getBounds() {
        return surface.getBounds();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (!this.name.equals(name)) {
            String old = this.name;
            this.name = name;
            change.firePropertyChange(PROP_NAME, old, name);
        }
    }

    SwingPropertyChangeSupport change = new SwingPropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener l) {
        change.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        change.removePropertyChangeListener(l);
    }

    private boolean visible = true;

    public void setVisible(boolean visible) {
        if (visible != this.visible) {
            this.visible = visible;
            change.firePropertyChange(PROP_VISIBLE, !visible, visible);
            repaint();
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public float getOpacity() {
        return 1.0F;
    }

    private float opacity = 1.0F;

    public void setOpacity(float f) {
        if (opacity != f) {
            float old = opacity;
            opacity = f;
            change.firePropertyChange(PROP_OPACITY, old, f);
            repaint();
        }
    }

    private void repaint() {
        Rectangle bounds = getBounds();
        getMasterRepaintHandle().repaintArea(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    public void commitLastPropertyChangeToUndoHistory() {
    }

    public boolean paint(Graphics2D g, Rectangle bounds, boolean showSelection) {
        if (bounds == null && !visible) {
            return false;
        }
        Composite old = g.getComposite();
        if (opacity != 1.0F) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        }

        boolean result = surface.paint(g, bounds);
        if (old != null) {
            g.setComposite(old);
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
        return Lookups.fixed(layer, surface,
                surface.getSurface(),
                surface.stack, surface.getGraphics(),
                new IS());
    }

    class CompositeReceiver extends EffectReceiver<Composite> {

        public CompositeReceiver() {
            super(Composite.class);
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
            BufferedImage result = new BufferedImage(d.width, d.height, GraphicsUtils.DEFAULT_BUFFERED_IMAGE_TYPE);
            Graphics2D g = result.createGraphics();
            surface.paint(g, null);
            g.dispose();
            return result;
        }

        @Override
        public BufferedImage createImageCopy(Dimension size) {
            Dimension d = surface.getSize();
            BufferedImage result = new BufferedImage(size.width, size.height, GraphicsUtils.DEFAULT_BUFFERED_IMAGE_TYPE);
            AffineTransform xform = AffineTransform.getScaleInstance((double) size.getWidth() / (double) d.getWidth(),
                    (double) size.getHeight() / (double) d.getHeight());
            Graphics2D g = result.createGraphics();
            g.setTransform(xform);
            surface.paint(g, null);
            g.dispose();
            return result;
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
