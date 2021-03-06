/*
 * Layer.java
 *
 * Created on October 14, 2005, 11:15 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.java.dev.imagine.api.image;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.java.dev.imagine.Accessor;
import net.java.dev.imagine.spi.image.LayerImplementation;
import net.java.dev.imagine.spi.image.PictureImplementation;
import net.java.dev.imagine.spi.image.SurfaceImplementation;
import org.imagine.editor.api.AspectRatio;
import org.imagine.editor.api.Zoom;
import org.openide.util.Lookup;
import org.openide.util.Mutex;

/**
 * A single "layer" - one element in a composite stack of images
 * (represented by a Picture object).
 *
 * @author Timothy Boudreau
 */
public final class Layer implements Lookup.Provider {
    public static final String PROP_BOUNDS = "bounds"; //NOI18N
    public static final String PROP_NAME = "name"; //NOI18N
    public static final String PROP_VISIBLE = "visible"; //NOI18N
    public static final String PROP_OPACITY = "opacity"; //NOI18N
    final LayerImplementation impl;

    static {
        init();
    }

    static void init() {
        if (Accessor.DEFAULT != null) return;
        Class c = LayerImplementation.class;
        try {
            Class.forName(c.getName(), true, c.getClassLoader());
        } catch (Exception e) {
            Logger.getLogger(Layer.class.getName()).log(Level.INFO, null, e);
        }
        Accessor.DEFAULT = new AccessorImpl();
    }


    /**
     * Constructor is package private.  Instances will only be constructed
     * inside of LayerImplementation.
     * @param impl The SPI object this class provides a backward-compatibility-proof
     * wrapper for
     */
    Layer (LayerImplementation impl) {
        if (Accessor.layerFor(impl) != null) {
            throw new IllegalStateException ("Constructing a second " + //NOI18N
                    "LayerImplementation for " + impl); //NOI18N
        }
        this.impl = impl;
    }

    public Layer clone (boolean isUserCopy, boolean deepCopy) {
        return Accessor.layerFor (impl.clone(isUserCopy, deepCopy));
    }

    /**
     * Get the bounds of all content within this layer, which may be include
     * contents outside the visible bounds.
     *
     * @return A rectangle
     */
    public Rectangle getContentBounds() {
        return impl.getContentBounds();
    }

    /**
     * Determine if this is a bitmap-backed layer, or if it can be
     * scaled up arbitrarily.
     *
     * @return Whether or not this layer is resolution-independent
     */
    public boolean isResolutionIndependent() {
        return impl.isResolutionIndependent();
    }

    /** Get the bounds of this layer.  This may or may not
     * correspond to the bounds of the image it is part of */
    public Rectangle getBounds() {
        return impl.getBounds();
    }
    /** Get the user supplied name of this layer */
    public String getName() {
        return impl.getName();
    }

    /** Set the display name of this layer */
    public void setName(String name) {
        impl.setName (name);
    }

    private final List <PropertyChangeListener> listeners =
            new LinkedList <PropertyChangeListener> ();

    public void addPropertyChangeListener (PropertyChangeListener l) {
        boolean first;
        synchronized (listeners) {
            first = listeners.isEmpty();
            listeners.add(l);
        }
        if (first) {
            startListening();
        }
    }

    public void removePropertyChangeListener (PropertyChangeListener l) {
        boolean last;
        synchronized (listeners) {
            listeners.remove(l);
            last = listeners.isEmpty();
        }
        if (last) {
            stopListening();
        }
    }

    private void fire(String prop, Object old, Object nue) {
        PropertyChangeListener[] ll;
        synchronized (listeners) {
            ll = listeners.toArray(new PropertyChangeListener [listeners.size()]);
        }
        if (ll.length > 0) {
            final PropertyChangeEvent pce = new PropertyChangeEvent (this,
                    prop, old, nue);
            final PropertyChangeListener[] l = ll;
            Runnable r = new Runnable() {
                public void run() {
                    for (int i = 0; i < l.length; i++) {
                        l[i].propertyChange (pce);
                    }
                }
            };
            Mutex.EVENT.readAccess(r);
        }
    }

    /** Get a surface for drawing into.  Some implementations of
     * layer may return null for this.
     */
    public Surface getSurface() {
        Surface surface = getLookup().lookup (Surface.class);
        if (surface != null) {
            SurfaceImplementation surf =
                    getLookup().lookup(SurfaceImplementation.class);
            if (surf != null) {
                surface = Accessor.surfaceFor(surf);
            }
        }
        return surface;
    }

    /** Mark this layer as being included in those layers which are
     * visibly composited into the overall picture */
    public void setVisible (boolean visible) {
        impl.setVisible (visible);
    }

    /** Returns true if setVisible(false) has not been called */
    public boolean isVisible() {
        return impl.isVisible();
    }

    /** Get this layer's opacity, affecting how it is composited */
    public float getOpacity() {
        return impl.getOpacity();
    }

    /** Set the opacity of this layer */
    public void setOpacity (float f) {
        impl.setOpacity(f);
    }

    /**
     * Resize this layer, causing its contents to be scaled or clipped
     * to the new size
     * @param width
     * @param height
     */
    public void resize (int width, int height) {
        resize(width, height, true);
    }

    /**
     * Resize this layer, possibly, causing its contents to be scaled or clipped
     * to the new size
     * @param width
     * @param height
     */
    public void resize (int width, int height, boolean resizeContents) {
        assert width > 0;
        assert height > 0;
        impl.resize (width, height, true);
    }

    /**
     * Paint the current contents of this Layer object to the supplied
     * Graphics2D context.
     * <p>
     * If a bounding rectangle is supplied, this method should assume that the
     * call is to paint a thumbnail, and that low quality rendering settings
     * should be used.  If the rectangle is null, then the image should be
     * rendered at full quality and full size (size will actually be determined
     * by the AffineTransform the Graphics is currently using, which will not
     * be modified if the rectangle is null).
     * @param g A graphics context
     * @param r A bounding rectangle if painting a thumbnail image, or null
     *  if full quality painting is desired
     */
    public boolean paint (RenderingGoal goal, Graphics2D g, Rectangle bounds, boolean showSelection,
            boolean paintWhenVisibleFalse, Zoom zoom, AspectRatio ratio) {
        return impl.paint (goal, g, bounds, showSelection, paintWhenVisibleFalse, zoom, ratio);
    }

    public void commitLastPropertyChangeToUndoHistory() {
        //XXX get this out of here
        impl.commitLastPropertyChangeToUndoHistory();
    }

    private PropertyChangeListener pcl;
    private void startListening() {
        PropertyChangeListener p;
        synchronized (this) {
            pcl = p = new PCL();
        }
        impl.addPropertyChangeListener (p);
    }

    private void stopListening() {
        PropertyChangeListener p;
        synchronized (this) {
            p = pcl;
            pcl = null;
        }
        impl.removePropertyChangeListener(pcl);
    }

    private class PCL implements PropertyChangeListener {
        public void propertyChange(final PropertyChangeEvent evt) {
            Mutex.EVENT.readAccess(new Runnable() {

                @Override
                public void run() {
                    fire (evt.getPropertyName(), evt.getOldValue(),
                        evt.getNewValue());
                }

            });
        }
    }

    /**
     * A lookup which supplies optional functionality for this layer.  For
     * example, if it can be hibernated (switched to a low memory usage
     * dormant state) it may supply an instance of Hibernator in its
     * lookup.
     *
     * Under all circumstances a Layer's lookup should contain itself.
     */
    public Lookup getLookup() {
        return impl.getLookup();
    }

    public boolean equals(Object o) {
        return o instanceof Layer && ((Layer) o).impl.equals(impl);
    }

    public int hashCode() {
        return impl.hashCode();
    }

    static final class AccessorImpl extends Accessor {
        public Surface createSurface (SurfaceImplementation impl) {
            return new Surface (impl);
        }

        public Layer createLayer (LayerImplementation impl) {
            return new Layer (impl);
        }

        public Picture createPicture (PictureImplementation impl) {
            return new Picture (impl);
        }

        public PictureImplementation getImpl(Picture layers) {
            return layers.impl;
        }

        public LayerImplementation getImpl(Layer layer) {
            return layer.impl;
        }

        public SurfaceImplementation getSurface(Surface surface) {
            return surface.impl;
        }
    }
}
