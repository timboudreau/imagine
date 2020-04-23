/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2005 Sun
 * Microsystems, Inc. All Rights Reserved.
 */
package net.java.dev.imagine.layers.raster;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import static java.lang.System.identityHashCode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import net.dev.java.imagine.api.selection.ShapeSelection;
import net.dev.java.imagine.api.selection.Universe;
import net.java.dev.imagine.api.image.Hibernator;
import net.java.dev.imagine.api.image.RenderingGoal;
import net.java.dev.imagine.effects.spi.ImageSource;
import net.java.dev.imagine.spi.image.LayerImplementation;
import org.imagine.editor.api.Zoom;
import org.imagine.utils.painting.RepaintHandle;
import org.netbeans.paint.api.editing.LayerFactory;
import org.imagine.utils.java2d.GraphicsUtils;
import org.imagine.utils.java2d.LazyGraphics;
import org.imagine.utils.java2d.GraphicsProvider;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

public class RasterLayerImpl extends LayerImplementation implements Hibernator {
    //XXX class public only temporarily

    private final Rectangle bounds = new Rectangle();
    private String name;
    private final RasterSurfaceImpl surface;
    private final ShapeSelection selection = new ShapeSelection(new UV());
    private final PCL pcl = new PCL();

    public RasterLayerImpl(LayerFactory factory, RasterLayerImpl copy, boolean isUserCopy) {
        super(factory, false);
        addRepaintHandle(copy.getMasterRepaintHandle());
        bounds.setBounds(copy.getBounds());
        if (isUserCopy) {
            name = NbBundle.getMessage(RasterLayerImpl.class,
                    "LBL_CopiedLayer", new Object[]{copy.getName()}); //NOI18N
        } else {
            name = copy.getName();
        }
        surface = new RasterSurfaceImpl(copy.surface(), isUserCopy, selection, this::isVisible);
        surface.addPropertyChangeListener(pcl);
    }

    public RasterLayerImpl(LayerFactory factory, RepaintHandle handle, Dimension size) {
        super(factory, false);
        bounds.setSize(size);
        addRepaintHandle(handle);
        name = NbBundle.getMessage(RasterLayerImpl.class,
                "LBL_Layer", new Object[]{new Integer(0)}); //NOI18N
        surface = new RasterSurfaceImpl(getMasterRepaintHandle(), size, selection, this::isVisible);
        surface.addPropertyChangeListener(pcl);
    }

    public RasterLayerImpl(LayerFactory factory, RepaintHandle handle, BufferedImage img) {
        super(factory, false);
        if (handle != null) {
            addRepaintHandle(handle);
        }
        bounds.width = img.getWidth();
        bounds.height = img.getHeight();
        name = NbBundle.getMessage(RasterLayerImpl.class,
                "LBL_Layer", new Object[]{new Integer(0)}); //NOI18N
        surface = new RasterSurfaceImpl(getMasterRepaintHandle(), img, selection, this::isVisible);
        surface.addPropertyChangeListener(pcl);
    }

    @Override
    public String toString() {
        return "RasterLayer(" + Long.toString(identityHashCode(this), 36)
                + " " + bounds.x + "," + bounds.y + "," + bounds.width
                + ", " + bounds.height + ", ";
    }

    class UV implements Universe<Rectangle> {

        @Override
        public Rectangle getAll() {
            return new Rectangle(getBounds());
        }
    }

    final class PCL implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (RasterSurfaceImpl.PROP_LOCATION.equals(evt.getPropertyName())) { //something is screwed up in property name
                Point old = (Point) evt.getOldValue();
                Point nue = (Point) evt.getNewValue();
                surfaceLocationChanged(old, nue);
            } else if (RasterSurfaceImpl.PROP_BOUNDS.equals(evt.getPropertyName())) {
                bounds.setBounds((Rectangle) evt.getNewValue());
            }
        }
    }

    @Override
    public LayerImplementation clone(boolean userCopy, boolean deepCopy) {
        return new RasterLayerImpl(getLookup().lookup(LayerFactory.class),
                this, userCopy);
    }

    @Override
    public boolean paint(RenderingGoal goal, Graphics2D g, Rectangle bounds, boolean showSelection, boolean ignoreVisibility, Zoom zoom) {
        if (!visible && !ignoreVisibility) {
            return false;
        }
        if (bounds != null) {
            return surface.paint(goal, g, bounds, zoom);
        }
        Composite comp = null;
        if (opacity != 1.0f) {
            comp = (g).getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                    opacity));
        }
        boolean result;
        result = surface.paint(goal, g, null, zoom);
        if (opacity != 1.0f) {
            g.setComposite(comp);
        }
        if (showSelection) {
            selection.paint(g, bounds);
        }
        return result;
    }

    void surfaceLocationChanged(Point old, Point loc) {
        Rectangle oldBounds = new Rectangle(old, bounds.getSize());
        Rectangle newBounds = new Rectangle(loc, bounds.getSize());
        fire(PROP_BOUNDS, oldBounds, newBounds);
    }

    public Rectangle getBounds() {
        Rectangle result = new Rectangle(bounds);
        result.setLocation(surface.getLocation());
        return result;
    }

//    public void setBounds(Rectangle r) {
//        Rectangle old = new Rectangle (bounds);
//        bounds.setBounds(r);
//        if (surface != null) { //wlll be in the copy constructor
//            surface.setSize (bounds.getSize(), false, false);
//        }
//        fire (PROP_BOUNDS, old, r);
//    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        String old = this.name;
        if (!name.equals(this.name)) {
            this.name = name;
            fire(PROP_NAME, old, name);
        }
    }
    private List<PropertyChangeListener> listeners = Collections.<PropertyChangeListener>synchronizedList(
            new ArrayList<PropertyChangeListener>());

    public void addPropertyChangeListener(PropertyChangeListener l) {
        listeners.add(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        listeners.remove(l);
    }
    private Object lastProp = null;
    private String lastPropertyName = null;
    private Object lastPropNewValue = null;

    private void fire(String prop, Object old, Object nue) {
        PropertyChangeListener[] l = (PropertyChangeListener[]) listeners.toArray(new PropertyChangeListener[0]);
        if (lastProp == null || lastProp.getClass() != old.getClass()) {
            lastProp = old;
            lastPropertyName = prop;
        }
        lastPropNewValue = nue;
        for (int i = 0; i < l.length; i++) {
            l[i].propertyChange(new PropertyChangeEvent(this, prop, old, nue));
        }
    }

    RasterSurfaceImpl surface() {
        return surface;
    }

    public void repaintArea(int x, int y, int w, int h) {
        x -= bounds.x;
        y -= bounds.y;
        bounds.width = Math.max(bounds.width, x + w);
        bounds.height = Math.max(bounds.height, y + h);
        getMasterRepaintHandle().repaintArea(x, y, w, h);
    }
    private boolean visible = true;

    public void setVisible(boolean visible) {
        if (this.visible != visible) {
            this.visible = visible;
            fire(PROP_VISIBLE, visible ? Boolean.FALSE : Boolean.TRUE,
                    visible ? Boolean.TRUE : Boolean.FALSE);
        }
    }

    public boolean isVisible() {
        return visible;
    }
    private float opacity = 1.0f;

    public float getOpacity() {
        return opacity;
    }

    public void setOpacity(float f) {
        if (f != opacity) {
            Float old = new Float(opacity);
            this.opacity = f;
            fire(PROP_OPACITY, old, new Float(f));
            getMasterRepaintHandle().repaintArea(bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }
    private boolean undoInProgress = false;
    private static final int OPACITY = 0;
    private static final int VISIBILITY = 1;
    private static final int NAME = 2;
    public static final String PROP_SIZE = "size";
    private static final String[] props2ints = new String[]{
        PROP_OPACITY, PROP_VISIBLE, PROP_NAME, PROP_BOUNDS, PROP_SIZE
    };
    private static final String[] locPropNames = new String[]{
        NbBundle.getMessage(RasterSurfaceImpl.class, "OPACITY"),
        NbBundle.getMessage(RasterSurfaceImpl.class, "VISIBILITY"),
        NbBundle.getMessage(RasterSurfaceImpl.class, "NAME"),
        NbBundle.getMessage(RasterSurfaceImpl.class, "BOUNDS"),
        NbBundle.getMessage(RasterSurfaceImpl.class, "SIZE"),};

    public void commitLastPropertyChangeToUndoHistory() {
        if (!undoInProgress && lastProp != null && lastPropNewValue != null
                && !lastProp.equals(lastPropNewValue)) {
            int ix = Arrays.asList(props2ints).indexOf(lastPropertyName);
            if (ix != -1) {
                PropertyUndoableEdit ed = new PropertyUndoableEdit(ix,
                        lastProp, lastPropNewValue);
                UndoManager mgr = (UndoManager) Utilities.actionsGlobalContext().lookup(UndoManager.class);
                if (mgr != null) {
                    mgr.undoableEditHappened(new UndoableEditEvent(this, ed));
                }
            }
        }
        lastProp = null;
        lastPropertyName = null;
        lastPropNewValue = null;
    }

    @Override
    protected Lookup createLookup() {
        Graphics2D lazyGraphics = LazyGraphics.create(new GraphicsProvider() {

            @Override
            public Graphics2D getGraphics() {
                return surface.getGraphics();
            }

            @Override
            public void onDispose(Graphics2D graphics) {
                //do nothing
            }
        }, true);

        Lookup a = Lookups.fixed(this, surface.getSurface(), layer, selection, lazyGraphics,
                surface.bufferedImageOpReceiver, surface.compositeReceiver,
                new ImgSrc(), RasterLayerSave.INSTANCE);
        InstanceContent c = new InstanceContent();
        ImgConverter conv = new ImgConverter();
        c.add(conv, conv);
        return new ProxyLookup(a, new AbstractLookup(c));
    }

    class ImgSrc implements ImageSource {

        @Override
        public BufferedImage getRawImage() {
            return surface.image();
        }

        @Override
        public BufferedImage createImageCopy(Dimension size) {
            BufferedImage img = surface.getImage();
            BufferedImage nue = new BufferedImage(size.width, size.height, GraphicsUtils.DEFAULT_BUFFERED_IMAGE_TYPE);
            double w = size.width;
            double h = size.height;
            double xfactor = w / (double) img.getWidth();
            double yfactor = h / (double) img.getHeight();
            AffineTransform xform = AffineTransform.getScaleInstance(xfactor, yfactor);
            Graphics2D g = nue.createGraphics();
            g.drawRenderedImage(img, xform);
            g.dispose();
            return nue;
        }

    }

    class ImgConverter implements InstanceContent.Convertor<ImgConverter, BufferedImage> {

        @Override
        public BufferedImage convert(ImgConverter t) {
            return surface.image();
        }

        @Override
        public Class<? extends BufferedImage> type(ImgConverter t) {
            return BufferedImage.class;
        }

        @Override
        public String id(ImgConverter t) {
            return getName();
        }

        @Override
        public String displayName(ImgConverter t) {
            return getName();
        }

    }

    public void setCursor(Cursor cursor) {
        getMasterRepaintHandle().setCursor(cursor);
    }

    public void hibernate() {
        surface.hibernate();
    }

    public void wakeup(boolean immediately, Runnable notify) {
        if (immediately) {
            surface.unhibernateImmediately();
            if (notify != null) {
                notify.run();
            }
        } else {
            surface.unhibernate(notify);
        }
    }

    private class PropertyUndoableEdit implements UndoableEdit {

        private final int type;
        private boolean isUndo = true;
        private final Object old;
        private final Object nue;

        public PropertyUndoableEdit(int type, Object old, Object nue) {
            this.type = type;
            this.old = old;
            this.nue = nue;
        }

        public void undo() throws CannotUndoException {
            if (!isUndo) {
                throw new CannotUndoException();
            }
            setValueTo(old);
            isUndo = false;
        }

        public boolean canUndo() {
            return isUndo;
        }

        public void redo() throws CannotRedoException {
            if (isUndo) {
                throw new CannotRedoException();
            }
            setValueTo(nue);
            isUndo = true;
        }

        private void setValueTo(Object val) {
            undoInProgress = true;
            try {
                switch (type) {
                    case OPACITY:
                        setOpacity(((Float) val).floatValue());
                        break;
                    case NAME:
                        setName((String) val);
                        break;
                    case VISIBILITY:
                        setVisible(((Boolean) val).booleanValue());
                        break;
                }
            } finally {
                undoInProgress = false;
            }
        }

        public boolean canRedo() {
            return !isUndo;
        }

        public void die() {
        }

        public boolean addEdit(UndoableEdit anEdit) {
            return false;
        }

        public boolean replaceEdit(UndoableEdit anEdit) {
            return false;
        }

        public boolean isSignificant() {
            return true;
        }

        public String getPresentationName() {
            return NbBundle.getMessage(getClass(), "LBL_Change", locPropNames[type]); //NOI18N
        }

        public String getUndoPresentationName() {
            return getPresentationName();
        }

        public String getRedoPresentationName() {
            return getPresentationName();
        }
    }

    @Override
    public void resize(int width, int height) {
        resize(width, height, false);
    }

    public void resize(int width, int height, boolean resizeCanvasOnly) {
        if (resizeCanvasOnly) {
            surface.resizeCanvas(width, height);
        }
        surface.resize(width, height);
    }
}
