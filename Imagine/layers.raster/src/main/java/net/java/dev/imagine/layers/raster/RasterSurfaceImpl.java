package net.java.dev.imagine.layers.raster;

import net.java.dev.imagine.api.image.Picture;
import org.netbeans.paint.api.editing.UndoManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.RasterFormatException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BooleanSupplier;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.ToolTipManager;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import net.dev.java.imagine.api.selection.Selection;
import net.dev.java.imagine.api.tool.Tool;
import org.imagine.utils.painting.RepaintHandle;
import net.java.dev.imagine.spi.image.SurfaceImplementation;
import net.dev.java.imagine.api.tool.aspects.NonPaintingTool;
import net.java.dev.imagine.api.image.RenderingGoal;
import net.java.dev.imagine.effects.api.EffectReceiver;
import org.imagine.editor.api.Zoom;
import com.mastfrog.geometry.util.PooledTransform;
import org.imagine.utils.java2d.GraphicsUtils;
import org.imagine.utils.java2d.TrackingGraphics;
import org.netbeans.paint.misc.image.ByteNIOBufferedImage;
import org.netbeans.paint.misc.image.ImageHolder;
import org.openide.ErrorManager;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;

class RasterSurfaceImpl extends SurfaceImplementation implements RepaintHandle {

    private BufferedImage img;
    private RepaintHandle handle;
    private Tool currentTool = null;
    private Point location = new Point();
    private Selection<Shape> selection;
    private final PropertyChangeSupport supp = new PropertyChangeSupport(this);
    private final BooleanSupplier isVisible;

    RasterSurfaceImpl(RepaintHandle handle, Dimension d, Selection<Shape> selection, BooleanSupplier isVisible) {
        this(handle, d, null, selection, isVisible);
    }

    RasterSurfaceImpl(RepaintHandle handle, Dimension d, BufferedImage img, Selection<Shape> selection, BooleanSupplier isVisible) {
        this.img = img == null
                ? GraphicsUtils.newBufferedImage(d.width, d.height)
                : null;
        this.handle = handle;
        this.selection = selection;
        this.isVisible = isVisible;
        EventQueue.invokeLater(
                new Runnable() {
            public void run() {
                //XXX wouldn't a background thread be preferable?
                takeSnapshot();
            }
        });
    }

    RasterSurfaceImpl(RasterSurfaceImpl other, boolean isUserCopy, Selection<Shape> selection, BooleanSupplier isVisible) {
        this(other.handle,
                new Dimension(other.img.getWidth(), other.img.getHeight()), selection, isVisible);
        if (isUserCopy) {
            other.img.copyData(img.getRaster());
        } else {
            // XXX get rid of bi creation in the super constructor
            img = ByteNIOBufferedImage.copy(other.img);
        }
        this.selection = selection;
    }

    void addPropertyChangeListener(PropertyChangeListener pcl) {
        supp.addPropertyChangeListener(pcl);
    }

    void removePropertyChangeListener(PropertyChangeListener pcl) {
        supp.removePropertyChangeListener(pcl);
    }

    void firePropertyChange(String name, Object old, Object nue) {
        supp.firePropertyChange(name, old, nue);
    }

    @Override
    public BufferedImage getImage() {
        return img;
    }

    RasterSurfaceImpl(Dimension size, RepaintHandle handle, Picture picture, Selection<Shape> selection, BooleanSupplier isVisible) {
        this(handle, picture.getSize(), selection, isVisible);
        picture.paint(RenderingGoal.PRODUCTION, (Graphics2D) img.getGraphics(), null, false, Zoom.ONE_TO_ONE);
        this.selection = selection;
    }

    RasterSurfaceImpl(RepaintHandle handle, BufferedImage img, Selection<Shape> selection, BooleanSupplier isVisible) {
        this.img = img;
        this.handle = handle;
        this.selection = selection;
        this.isVisible = isVisible;
    }

    BufferedImage image() {
        //for unit tests
        return img;
    }

    public void setCursor(Cursor cursor) {
        handle.setCursor(cursor);
    }

    final EffectReceiver<Composite> compositeReceiver = new CompositeReceiver();
    final EffectReceiver<BufferedImageOp> bufferedImageOpReceiver = new BufferedImageOpReceiver();

    class BufferedImageOpReceiver extends EffectReceiver<BufferedImageOp> {

        public BufferedImageOpReceiver() {
            super(BufferedImageOp.class);
        }

        @Override
        public boolean canApplyEffects() {
            return isVisible.getAsBoolean();
        }

        @Override
        protected <ParamType> boolean onApply(BufferedImageOp effect) {
            Shape sel = selection.get();
            applyBufferedImageOp(effect, sel);
            return true;
        }

        @Override
        public Dimension getSize() {
            return RasterSurfaceImpl.this.getSize();
        }
    }

    class CompositeReceiver extends EffectReceiver<Composite> {

        CompositeReceiver() {
            super(Composite.class);
        }

        @Override
        public boolean canApplyEffects() {
            return isVisible.getAsBoolean();
        }

        @Override
        protected <ParamType> boolean onApply(Composite effect) {
            Shape sel = selection.get();
            if (sel != null) {
                applyComposite(effect, sel);
            } else {
                applyComposite(effect);
            }
            return true;
        }

        @Override
        public Dimension getSize() {
            return RasterSurfaceImpl.this.getSize();
        }
    }

    private Snapshot snapshot = null;

    void resizeCanvas(int width, int height) {
        img = GraphicsUtils.newBufferedImage(width, height, g -> {
            GraphicsUtils.setHighQualityRenderingHints(g);
            g.drawRenderedImage(img, null);
        });
    }

    void resize(int width, int height) {
        double w = width;
        double h = height;
        double ow = img.getWidth();
        double oh = img.getHeight();
        double factorX = w / ow;
        double factorY = h / oh;
        PooledTransform.withScaleInstance(factorX, factorY, xform -> {
//        AffineTransform xform = AffineTransform.getScaleInstance(factorX, factorY);
            img = GraphicsUtils.newBufferedImage(width, height, g -> {
                GraphicsUtils.setHighQualityRenderingHints(g);
                g.drawRenderedImage(img, xform);
            });
        });
    }

    private void takeSnapshot() {
        if (snapshot == null && (currentTool == null || !isNonPainting(currentTool))) {
            Point loc = getLocation();
            int width = img.getWidth();
            int height = img.getHeight();
            Shape shape = selection.asShape();
            if (shape != null) {
                Rectangle b = shape.getBounds();
                loc = b.getLocation();
                width = b.width;
                height = b.height;
                if (loc.x < 0) {
                    width += loc.x;
                    loc.x = 0;
                }
                if (loc.y < 0) {
                    height += loc.y;
                    loc.y = 0;
                }
                if (loc.x + width > img.getWidth()) {
                    width -= (loc.x + width) - img.getWidth();
                }
                if (loc.y + height > img.getHeight()) {
                    height -= (loc.y + height) - img.getHeight();
                }
            }
            snapshot = new Snapshot(img, loc, new Dimension(
                    width, height));
        }
    }

    boolean isNonPainting(Tool tool) {
        NonPaintingTool oldNP = tool == null ? null : tool.getLookup().lookup(NonPaintingTool.class);
        return tool == null ? true : oldNP != null;
    }

    public void setTool(Tool tool) {
        Tool old = currentTool;
        if (old != tool) {
            boolean wasNonPainting = isNonPainting(old);
            boolean isNonPainting = isNonPainting(tool);

            currentTool = tool;
            if (currentTool != null && !isNonPainting) {
                takeSnapshot();
            }

            if (wasNonPainting
                    && !isNonPainting & (location.x != 0 || location.y != 0)) {
                growImageIfNeeded();
            }
        }
    }

    public Graphics2D getGraphics() {
        unhibernateImmediately();
        Point p = getLocation();
        TrackingGraphics result = GraphicsUtils.wrap(this, img.createGraphics(), p, img.getWidth(),
                img.getHeight());
        Shape sel = getSelection();
        if (sel != null) {
            result.setClip(sel);
        }

        return result;
    }

    public boolean isModified() {
        return modifiedBounds.width > 0 || modifiedBounds.height > 0;
    }

    private static final Rectangle UNMODIFIED = new Rectangle(Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            Integer.MIN_VALUE,
            Integer.MIN_VALUE);
    private static final Rectangle ALL_MODIFIED = new Rectangle(-1, -1, -1, -1);
    private Rectangle modifiedBounds = new Rectangle(UNMODIFIED);

    @Override
    public void repaintArea(int x, int y, int w, int h) {
        if (!modifiedBounds.equals(ALL_MODIFIED)) {
            if (w != -1 && h != -1) {
                if (UNMODIFIED.equals(modifiedBounds)) {
                    modifiedBounds = new Rectangle(x, y, w, h);
                } else {
                    modifiedBounds.add(new Rectangle(x, y, w, h));
                }
            } else {
                modifiedBounds.setBounds(ALL_MODIFIED);
            }
        }
        _repaintArea(x, y, w < 0 ? img.getWidth() : w, h < 0
                ? img.getHeight() : h);
    }

    private void _repaintArea(int x, int y, int w, int h) {
        int maxW = Math.min(img.getWidth(), x + w);
        int maxH = Math.min(img.getHeight(), y + h);
        handle.repaintArea(x, y, maxW - x, maxH - y);
    }

    public Rectangle getChangeBounds() {
        if (modifiedBounds.width == -1) {
            return new Rectangle(0, 0, img.getWidth(), img.getHeight());
        }
        Rectangle changed = modifiedBounds;
        modifiedBounds = new Rectangle(UNMODIFIED);
        return changed;
    }

    boolean paintFull(Graphics2D g) {
        if (img instanceof ByteNIOBufferedImage) {
            return false;
        }
        PooledTransform.withTranslateInstance(location.x, location.y, xform -> {
            g.drawRenderedImage(img, xform);
        });
        return true;
    }

    public boolean paint(RenderingGoal goal, Graphics2D g2d, Rectangle r, Zoom zoom) {
        if (img instanceof ByteNIOBufferedImage) {
            return false;
        }
        if (r == null) {
            return paintFull(g2d);
        }

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        double xfactor = (double) r.width / (double) img.getWidth();
        double yfactor = (double) r.height / (double) img.getHeight();

        PooledTransform.withScaleInstance(xfactor, yfactor, scale -> {
            PooledTransform.withTranslateInstance(r.x, r.y, xlate -> {
                scale.concatenate(xlate);
                g2d.drawRenderedImage(img, scale);
            });
        });

//        AffineTransform xform = AffineTransform.getScaleInstance(xfactor,
//                yfactor);
//        xform.concatenate(AffineTransform.getTranslateInstance(r.x, r.y));
//        g2d.drawRenderedImage(img, xform);
//        g2d.setColor(Color.BLACK);
//        g2d.drawRect(r.x, r.y, r.width-1, r.height-1);
        return true;
    }

    public void applyComposite(Composite composite, Shape region) {
        Tool tool = currentTool;
        currentTool = null; //So undo works properly
        try {
            doApplyComposite(composite, region);
        } finally {
            currentTool = tool;
        }
    }

    public void applyBufferedImageOp(BufferedImageOp op, Shape clip) {
        Tool tool = currentTool;
        currentTool = null;
        try {
            doApplyBufferedImageOp(op, clip);
        } finally {
            currentTool = tool;
        }
    }

    private void doApplyBufferedImageOp(BufferedImageOp op, Shape region) {
        BufferedImage old = img;
        if (region == null || region.getBounds().contains(0, 0, img.getWidth(), img.getHeight())) {
            BufferedImage nue = op.filter(old, null);
            img = nue;
            boundsMayBeChanged(old, nue);
            repaintArea(0, 0, img.getWidth(), img.getHeight());
        } else {
            Rectangle regBounds = region.getBounds(); //XXX check that it fits within image!
            regBounds = regBounds.intersection(new Rectangle(0, 0, img.getWidth(), img.getHeight()));
            BufferedImage nue = img.getSubimage(regBounds.x, regBounds.y, regBounds.width, regBounds.height);
            nue = op.filter(nue, null);
            Graphics2D g = nue.createGraphics();
            g.setClip(region);
            g.drawRenderedImage(nue, null);
            repaintArea(regBounds);
        }
    }

    public void repaintArea(Rectangle r) {
        repaintArea(r.x, r.y, r.width, r.height);
    }

    private void doApplyComposite(Composite composite, Shape region) {
        if (location.x != 0 && location.y != 0) {
            // Rectangle r = new Rectangle (location.x, location.y,
            // img.getWidth(), img.getHeight());
            // if (!r.contains(region.getBounds())) {
            growImageIfNeeded();
        }
        beginUndoableOperation(composite.toString());
        try {
            // Create a new image that will become this surface's image at the
            // end of the operation
            BufferedImage applied = new BufferedImage(img.getWidth(),
                    img.getHeight(),
                    img.getType());
            Graphics2D g = (Graphics2D) applied.getGraphics();
            // Save its composite
            Composite old = g.getComposite();

            // If the selection is null, we just do a simple copy, applying the
            // composite
            if (region == null) {
                g.setComposite(composite);
                g.drawRenderedImage(img, null);
                repaintArea(0, 0, img.getWidth(), img.getHeight());
            } else {
                Rectangle bds = new Rectangle(location.x, location.y,
                        img.getWidth(), img.getHeight());
                // Store the last known clip
                Shape clip = g.getClip();
                int xOff = location.x;
                int yOff = location.y;

                g.translate(-xOff, -yOff);
                // Set our composite
                g.setComposite(composite);
                // Paint the rectanglular bounds of the shape first;  we'll cover
                // the outlying areas with the inverse paint later.  Some non-rectangular
                // clip shapes cause a RasterFormatException if combined with a
                // composite, so we need to paint with-composite into a rectangular
                // region, then mask the excess with the original image.
                g.setClip(region.getBounds());
                // First draw our effect-modified content into the
                // rectangle* surrounding the selection.  We have to
                // do it this way.
                Rectangle selBds = region.getBounds();

                try {
                    PooledTransform.withTranslateInstance(xOff, yOff, xform -> {
                        g.drawRenderedImage(img, xform);
                    });
//                    g.drawRenderedImage(img,
//                            AffineTransform.getTranslateInstance(xOff,
//                                    yOff));
                } catch (RasterFormatException rfe) {
                    // Debugging stuff
                    IllegalStateException ise = new IllegalStateException("Fail: src "
                            + img.getWidth()
                            + ","
                            + img.getHeight()
                            + " dest "
                            + ""
                            + applied.getWidth()
                            + ","
                            + applied.getHeight()
                            + " clip "
                            + g.getClipBounds()
                            + " actual clip "
                            + g.getClip());

                    ErrorManager.getDefault().annotate(ise, rfe);
                    throw ise;
                }
                // Now restore the original composite, presumably straight painting
                g.setComposite(old);
                // Get a rectangle for the whole image, and *subtract* the shape
                // of the selection
                Area inverse = new Area(bds);

                inverse.subtract(new Area(region));
                // Set the clip to that
                g.setClip(inverse);
                // Set the fill to transparent
                g.setBackground(new Color(255, 255, 255, 0));
                // And clear the excess from our painting of the effect-modified
                // selection
                g.clearRect(0, 0, img.getWidth(), img.getHeight());
                // Now that its empty, paint the original back into the
                // non-selection area
                PooledTransform.withTranslateInstance(xOff, yOff, xform -> {
                    g.drawRenderedImage(img, xform);
                });
//                g.drawRenderedImage(img,
//                        AffineTransform.getTranslateInstance(xOff,
//                                yOff));
                // And restore the clipping bounds
                g.setClip(clip);
                // And tell the editor what to repaint
                repaintArea(selBds.x, selBds.y, selBds.width, selBds.height);
            }
            // Swap the original image for our new one
            this.img = applied;
            // And restore the composite
            g.setComposite(old);
            g.dispose();
        } catch (RuntimeException re) {
            // Oops, something went wrong
            cancelUndoableOperation();
            throw re;
        }
        // Pushes our undo operation onto the UndoManager's stack.
        endUndoableOperation();
    }

    private volatile boolean shouldBeHibernated = false;

    void hibernate() {
        shouldBeHibernated = true;
        q.add(this, true, null);
    }

    void unhibernate(Runnable notify) {
        shouldBeHibernated = false;
        q.add(this, false, notify);
        this.repaintArea(0, 0, img.getWidth(), img.getHeight());
        if (notify != null) {
            notify.run();
        }
    }

    void unhibernateImmediately() {
        //If we were created as undo data, we may need to urgently
        //switch to being a standard buffered image
        if (!shouldBeHibernated && img instanceof ByteNIOBufferedImage) {
            img = getImageForDesiredState();
        }
    }

    private BufferedImage getImageForDesiredState() {
        if (shouldBeHibernated) {
            if (img instanceof ByteNIOBufferedImage) {
                return null;
            }
            return ByteNIOBufferedImage.copy(img);
        } else {
            if (!(img instanceof ByteNIOBufferedImage)) {
                return null;
            }
            return ByteNIOBufferedImage.toStandard(img);
        }
    }
    private static final RequestProcessor rp = new RequestProcessor("Image hibernate queue");
    private static final HibernateQueue q = new HibernateQueue();

    @Override
    public Dimension getSize() {
        return new Dimension(img.getWidth(), img.getHeight());
    }

    private static class HibernateQueue implements Runnable {

        private java.util.List<RasterSurfaceImpl> queue = Collections.<RasterSurfaceImpl>synchronizedList(new ArrayList<RasterSurfaceImpl>());
        private RequestProcessor.Task task = rp.create(this);
        private List<Runnable> toNotify = Collections.synchronizedList(new LinkedList<Runnable>());

        public void add(RasterSurfaceImpl surface, boolean hibernate, Runnable notify) {
            if (notify != null) {
                toNotify.add(notify);
            }
            if (!hibernate) {
                // prioritize unhibernate operations
                queue.add(0, surface);
            } else {
                queue.add(surface);
            }
            task.schedule(0);
        }

        public void run() {
            RasterSurfaceImpl[] s;

            synchronized (queue) {
                s = queue.toArray(new RasterSurfaceImpl[0]);
                queue.clear();
            }
            final BufferedImage[] imgs = new BufferedImage[s.length];
            final RasterSurfaceImpl[] surfaces = s;

            for (int i = 0; i < surfaces.length; i++) {
                imgs[i] = surfaces[i].getImageForDesiredState();
            }
            // Have to do this this way, otherwise we will
            // have to synchronize all access to the img field, which
            // will cause performance problems

            try {
                EventQueue.invokeAndWait(new Runnable() {
                    public void run() {
                        for (int i = 0; i < surfaces.length; i++) {
                            if (imgs[i] != null) {
                                surfaces[i].img = imgs[i];
                            }
                        }
                    }
                });
            } catch (InterruptedException ex) {
                ErrorManager.getDefault().notify(ex);
            } catch (InvocationTargetException ex1) {
                ErrorManager.getDefault().notify(ex1);
            }
            Runnable[] no = (Runnable[]) toNotify.toArray(new Runnable[toNotify.size()]);
            toNotify.clear();
            for (Runnable r : no) {
                r.run();
            }
        }

        public void waitFinished() {
            task.waitFinished();
        }
    }

    OwnedEdit[] myEdits() {
        //XXX the undomanager returned here may belong to a different image!
        UndoManager mgr
                = Utilities.actionsGlobalContext().lookup(UndoManager.class);
        if (mgr != null) {
            List l = mgr.getEdits();
            ArrayList<UndoableEdit> result = new ArrayList<UndoableEdit>(l.size());
            for (Iterator it = l.iterator(); it.hasNext();) {
                UndoableEdit ed = (UndoableEdit) it.next();
                if (ed instanceof OwnedEdit && ((OwnedEdit) ed).isChangeOf(this)) {
                    result.add(ed);
                }
            }
            OwnedEdit[] results = result.toArray(new OwnedEdit[result.size()]);
            return results;
        } else {
            return new OwnedEdit[0];
        }
    }

    public Point getLocation() {
        return new Point(location);
    }

    interface OwnedEdit extends UndoableEdit {

        public boolean isChangeOf(RasterSurfaceImpl impl);

        void zeroMoved(int x, int y);
    }

    private Shape getSelection() {
        return selection.asShape();
    }

    private void boundsMayBeChanged(BufferedImage old, BufferedImage nue) {
        Rectangle a = new Rectangle(location, new Dimension(old.getWidth(), old.getHeight()));
        Rectangle b = new Rectangle(location, new Dimension(nue.getWidth(), nue.getHeight()));
        if (!a.equals(b)) {
            firePropertyChange(PROP_BOUNDS, a, b);
        }
    }

    public static final String PROP_LOCATION = "Move";
    public static final String PROP_BOUNDS = "Bounds";
    private Dimension grow = null;
    private Point imageReplacePosition = null;
    private Point actualImagePosition = null;

    public void setLocation(Point p) {
        if (!location.equals(p)) {
            Point old = new Point(location);
            Point nue = new Point(p);
            firePropertyChange(PROP_LOCATION, old, nue);
            if (actualImagePosition == null) {
                actualImagePosition = new Point(location);
            }
            location.setLocation(p);

            int wdiff = Math.abs(location.x - actualImagePosition.x);
            int hdiff = Math.abs(location.y - actualImagePosition.y);
            int minX = Math.min(location.x, actualImagePosition.x);
            int minY = Math.min(location.y, actualImagePosition.y);

            imageReplacePosition = new Point();
            imageReplacePosition.x = Math.max(0, location.x);
            imageReplacePosition.y = Math.max(0, location.y);

            grow = new Dimension(wdiff, hdiff);
            _repaintArea(minX, minY, img.getWidth() + wdiff, img.getHeight()
                    + hdiff);
        }
    }

    void growImageIfNeeded() {
        if (grow != null) {
            int type = img.getType();
            if (type == 0) {
                //Tool probably being set while we unhibernate;  we never
                //need to grow the image in this state
                return;
            }
            img = GraphicsUtils.newBufferedImage(img.getWidth() + grow.width, img.getHeight() + grow.height, g2d -> {
                PooledTransform.withTranslateInstance(imageReplacePosition.x, imageReplacePosition.y, xform -> {
                    g2d.drawRenderedImage(img, xform);
                });
            });

//            BufferedImage nue = new BufferedImage(img.getWidth()
//                    + grow.width, img.getHeight() + grow.height, type);
//            Graphics2D g2d = nue.createGraphics();
//            PooledTransform.withTranslateInstance(imageReplacePosition.x, imageReplacePosition.y, xform -> {
//                g2d.drawRenderedImage(img, xform);
//            });
////            g2d.drawRenderedImage(img, AffineTransform.getTranslateInstance(
////                    imageReplacePosition.x,
////                    imageReplacePosition.y));
//            g2d.dispose();
//            img = nue;
            if (imageReplacePosition.x > 0 || imageReplacePosition.y > 0) {
                OwnedEdit[] edits = myEdits();
                for (int i = 0; i < edits.length; i++) {
                    edits[i].zeroMoved(imageReplacePosition.x,
                            imageReplacePosition.y);
                }
            }
            location.setLocation(Math.min(0, location.x), Math.min(0, location.y));

            grow = null;
            imageReplacePosition = null;
            actualImagePosition = null;
            if (inUndoableOperation) {
                undoableStartLocation = new Point(location);
            }
        }
    }

    Point undoableStartLocation = null;

    boolean inUndoableOperation = false;
    String undoName = "XX";

    public void beginUndoableOperation(String what) {
        what = what == null ? currentTool != null ? currentTool.getName() : "??" : what;
        inUndoableOperation = true;
        takeSnapshot();

//        if (!(currentTool instanceof NonPaintingTool)) {
        if (currentTool != null && !isNonPainting(currentTool)) {
            growImageIfNeeded();
        }

        undoableStartLocation = new Point(location);
        undoName = what;
    }

    public void endUndoableOperation() {
        inUndoableOperation = false;
        if (!location.equals(undoableStartLocation) && undoableStartLocation != null) {
            UndoManager mgr = (UndoManager) Utilities.actionsGlobalContext().lookup(UndoManager.class);

            if (mgr != null) {
                Point nue = new Point(location);
                MoveEdit ed = new MoveEdit(undoableStartLocation, nue);
                UndoableEditEvent evt = new UndoableEditEvent(this, ed);
                mgr.undoableEditHappened(evt);
            }
        } else if (snapshot != null && !UNMODIFIED.equals(modifiedBounds)) {
            Snapshot snap = snapshot;
            snapshot = null;
            OwnedEdit edit = null;
            edit = new PaintEdit(new PaintingUndoData(snap),
                    undoName);

            undoName = "--";
            UndoManager undo = (UndoManager) Utilities.actionsGlobalContext().lookup(UndoManager.class);
            if (undo != null) {
                undo.undoableEditHappened(new UndoableEditEvent(this, edit));
            }
            if (currentTool != null && !isNonPainting(currentTool)) {
                takeSnapshot();
            }
        }
        undoableStartLocation = null;
    }

    public void cancelUndoableOperation() {
        if (inUndoableOperation) {
            inUndoableOperation = false;
            snapshot = null;
            undoableStartLocation = null;
        }
    }

    private static class Snapshot {

        private Point location;
        private BufferedImage img;
        private Dimension size;

        private Snapshot(BufferedImage img, Point loc, Dimension size) {
            this.img = new BufferedImage(img.getWidth(), img.getHeight(),
                    img.getType() == 0 ? GraphicsUtils.DEFAULT_BUFFERED_IMAGE_TYPE
                    : img.getType());
            this.img.createGraphics().drawRenderedImage(img, null);
            this.location = loc;
            this.size = size;
        }

        void updateLocation(Point p) {
            this.location = new Point(p);
        }

        BufferedImage getImage() {
            return img;
        }

        Point getLocation() {
            return location;
        }

        Dimension getSize() {
            return size;
        }
    }

    class PaintingUndoData {

        ImageHolder undoImage;
        private Rectangle redoBounds;
        private Rectangle undoBounds;
        private Rectangle clearLeftRight = null;
        private Rectangle clearTopBottom = null;
        ImageHolder redoImage = null;

        public PaintingUndoData(Snapshot snapshot) {
            init(snapshot);
        }

        public PaintingUndoData() {
            clearLeftRight = new Rectangle(0, 0, img.getWidth(),
                    img.getHeight());
            undoImage = null;
            redoBounds = new Rectangle(clearLeftRight);
            undoBounds = new Rectangle(clearLeftRight);
        }

        private void rollClip(Rectangle r) {
            if (r != null && r.x < 0) {
                r.width += r.x;
                r.x = 0;
//                System.err.println("QUUB 1 " + r);
            }
            if (r != null && r.y < 0) {
                r.height += r.y;
                r.y = 0;
//                System.err.println("QUUB 2 " + r);
            }
        }

        private void calcClearRects(Rectangle common, Rectangle all, Rectangle startBounds, Rectangle endBounds, boolean zeroMoveX, boolean zeroMoveY) {
            //XXX clean up args - passing more info than needed
            if (zeroMoveX && common.width != all.width) {
                clearLeftRight = new Rectangle(all.x, all.y,
                        all.width - common.width, all.height);
            } else if (common.width != all.width) {
                clearLeftRight = new Rectangle(all.x + common.width, all.y,
                        all.width - common.width, all.height);
            }
            if (zeroMoveX && common.height != all.height) {
                clearTopBottom = new Rectangle(all.x, all.y,
                        all.width, all.height - common.height);
            } else if (common.height != all.height) {
                clearTopBottom = new Rectangle(all.x, all.y + common.height,
                        all.width, all.height - common.height);
            }
        }

        private void init(Snapshot snapshot) {
            BufferedImage before = snapshot.getImage();
            BufferedImage after = img;
            Rectangle r = getChangeBounds();

            boolean allModified = ALL_MODIFIED.equals(r);

            Dimension sizeAtStart = new Dimension(before.getWidth(), before.getHeight());
            Dimension sizeAtFinish = new Dimension(img.getWidth(),
                    img.getHeight());

            boolean sizeChanged = !sizeAtStart.equals(sizeAtFinish);
            Rectangle saveBounds = new Rectangle(r);
            if (sizeChanged) {
                Point startLoc = snapshot.getLocation();
                Point endLoc = getLocation();

                Rectangle startBounds = new Rectangle(startLoc.x, startLoc.y, sizeAtStart.width,
                        sizeAtStart.height);
                Rectangle endBounds = new Rectangle(endLoc.x, endLoc.y, sizeAtFinish.width,
                        sizeAtFinish.height);

                boolean zeroMoveX = startBounds.x == endBounds.x;
                if (zeroMoveX) {
                    startBounds.x += sizeAtFinish.width - sizeAtStart.width;
                }
                boolean zeroMoveY = startBounds.y == endBounds.y;
                if (zeroMoveY) {
                    startBounds.y += sizeAtFinish.height - sizeAtStart.height;
                }

                Rectangle all = startBounds.union(endBounds);
                Point allLoc = all.getLocation();
                Rectangle common = startBounds.intersection(endBounds);
                Point offsets = new Point(endBounds.x - startBounds.x, endBounds.y - startBounds.y);

                all.setLocation(0, 0);
                common.setLocation(-offsets.x, -offsets.y);

                startBounds.translate(-allLoc.x, -allLoc.y);
                endBounds.translate(-allLoc.x, -allLoc.y);

                calcClearRects(common, all, startBounds, endBounds, zeroMoveX, zeroMoveY);

                redoBounds = new Rectangle(r);
                r.translate(-allLoc.x, -allLoc.y);

                undoBounds = new Rectangle(r);
                saveBounds = new Rectangle(r);

                if (zeroMoveX) {
                    saveBounds.x += offsets.x;
                }
                if (zeroMoveY) {
                    saveBounds.y += offsets.y;
                }
                undoBounds = undoBounds.intersection(common);

                if (DEBUG) {
                    showFrame(new Rectangle[]{
                        startBounds, endBounds, all, common,
                        redoBounds, undoBounds, saveBounds,
                        clearLeftRight, clearTopBottom,}, new String[]{
                        "Start Bounds", "End bounds", "All", "Common",
                        "Redo bounds", "Undo bounds", "Save bounds",
                        "Clear L/R", "Clear T/B",});
                }

            } else {
                rollClip(r);
                redoBounds = new Rectangle(r);
                undoBounds = new Rectangle(r);
                saveBounds = r.intersection(new Rectangle(0, 0, sizeAtStart.width, sizeAtStart.height));
                if (saveBounds.width <= 0 || saveBounds.height <= 0) {
                    saveBounds = null;
                }
            }

            generateImages(saveBounds, allModified, before, after);

        }

        private void fitToImage(Rectangle r, BufferedImage img) {
            if (r == null) {
                return;
            }
            if (r.x < 0) {
                r.width += r.x;
                r.x = 0;
            }
            if (r.y < 0) {
                r.height += r.y;
                r.y = 0;
            }
            if (r.x + r.width > img.getWidth()) {
                r.width = img.getWidth() - r.x;
            }
            if (r.y + r.height > img.getHeight()) {
                r.height = img.getHeight() - r.y;
            }
        }

        private void generateImages(Rectangle saveBounds, boolean allModified,
                BufferedImage before, BufferedImage after) {
//            System.err.println("Generate images " + saveBounds + " allModified " + allModified + " before null? " + (before == null) + " after null? " + (after == null));
            if (saveBounds != null) {
                saveBounds = saveBounds.intersection(new Rectangle(0, 0, before.getWidth(),
                        before.getHeight()));
            }

            if (allModified) {
                undoImage = new ImageHolder(before);
                redoImage = new ImageHolder(after);
            } else {
                fitToImage(saveBounds, before);
                undoImage = saveBounds == null
                        || saveBounds.width <= 0
                        || saveBounds.height <= 0
                                ? null : new ImageHolder(before, saveBounds);
                try {
                    fitToImage(redoBounds, after);
                    redoImage = new ImageHolder(after, redoBounds);
                } catch (RasterFormatException ref) {
                    throw new IllegalStateException("RFE on redo image for "
                            + redoBounds + " from " + after.getWidth() + ","
                            + after.getHeight());
                }
            }
            if (DEBUG && undoImage != null) {
                showImageInFrame(undoImage.getImage(false), "Undo image");
            }
            if (DEBUG) {
                showImageInFrame(redoImage.getImage(false), "Redo image");
            }
        }

        Rectangle[] getRectanglesToClear() {
            if ((clearLeftRight == null && clearTopBottom == null)) {
                return new Rectangle[0];
            } else if ((clearLeftRight == null) != (clearTopBottom == null)) {
                return new Rectangle[]{
                    clearLeftRight == null ? clearTopBottom : clearLeftRight
                };
            } else {
                return new Rectangle[]{clearLeftRight, clearTopBottom};
            }
        }

        void dispose() {
            if (undoImage != null) {
                undoImage.dispose();
            }
            if (redoImage != null) {
                redoImage.dispose();
            }
        }

        public void zeroMoved(int padX, int padY) {
            if (undoBounds != null) { //XXX shouldn't ever be null
                undoBounds.translate(padX, padY);
            }
            if (redoBounds != null) { //XXX shouldn't ever be null
                redoBounds.translate(padX, padY);
            }
            //XXX for clear rects, expand to meet the edges of the new image size
            if (clearLeftRight != null) {
                clearLeftRight.translate(padX, padY);
            }
            if (clearTopBottom != null) {
                clearTopBottom.translate(padX, padY);
            }
        }

        Point getRedoLocation() {
            return redoBounds.getLocation();
        }

        Point getUndoLocation() {
            return undoBounds.getLocation();
        }

        Rectangle getRedoBounds() {
            return new Rectangle(redoBounds);
        }

        Rectangle getUndoBounds() {
            return new Rectangle(undoBounds);
        }

        BufferedImage getRedoImage() {
            return redoImage != null ? redoImage.getImage(false) : null;
        }

        BufferedImage getUndoImage() {
            return undoImage != null ? undoImage.getImage(false) : null;
        }
    }

    private static class SRect extends Rectangle implements Comparable {

        public SRect(Rectangle r, String s) {
            super(r == null ? new Rectangle(-1000, -1000, -1000, -1000) : r);
            this.s = s;
        }
        private String s;

        @Override
        public String toString() {
            return s + " [" + x + "," + y + "," + width + "," + height + "]";
        }

        public int compareTo(Object o) {
            Rectangle r = (Rectangle) o;
            return (width * height) - (r.width * r.height);
        }
    }

    private static void showFrame(final Rectangle[] r, String[] s) {
        int minX = 0;
        int minY = 0;
        for (int i = 0; i < r.length; i++) {
            if (r[i] != null) {
                minX = Math.min(r[i].x, minX);
                minY = Math.min(r[i].y, minY);
            }
        }

        assert r.length == s.length;
        for (int i = 0; i < r.length; i++) {
            r[i] = new SRect(r[i], s[i]);
        }
        Arrays.sort(r);

        class C extends JComponent {

            int minX;
            int minY;

            C(int minX, int minY) {
                this.minX = minX;
                this.minY = minY;
            }

            @Override
            public void addNotify() {
                super.addNotify();
                ToolTipManager.sharedInstance().registerComponent(this);
            }

            @Override
            public void removeNotify() {
                ToolTipManager.sharedInstance().unregisterComponent(this);
                super.removeNotify();
            }

            public String getToolTipText(Point p) {
                Point p2 = new Point(p);
                p2.x -= minX;
                p2.y -= minY;
                return p2.x + "," + p2.y + hitString(p2);
            }

            public String hitString(Point p) {
                Rectangle[] rects = new Rectangle[r.length];
                System.arraycopy(r, 0, rects, 0, r.length);
                Arrays.sort(rects);
                for (int i = 0; i < rects.length; i++) {
                    if (r[i].contains(p)) {
                        return " " + r[i].toString();
                    }
                }
                return " (no rect)";
            }

            @Override
            public Dimension getPreferredSize() {
                Dimension result = new Dimension(-1, -1);
                for (int i = 0; i < r.length; i++) {
                    result.width = Math.max(result.width, r[i].width);
                    result.height = Math.max(result.height, r[i].height);
                }
                result.width += 80;
                result.height += 80;
                return result;
            }
            private Color[] c = new Color[]{Color.RED, Color.GREEN, Color.BLUE,
                Color.ORANGE, Color.MAGENTA, Color.CYAN, Color.GRAY, Color.BLACK,
                Color.PINK, new Color(150, 150, 0), new Color(0, 150, 150),
                new Color(150, 0, 150),};

            @Override
            public void paint(Graphics g) {
                g.translate(-minX, -minY);
                g.translate(40, 40);
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setFont(getFont());
                int h = g.getFontMetrics().getHeight();
                int ix = h;
                for (int i = 0; i < r.length; i++) {
                    if (r[i] != null) {
                        g.setColor(c[i]);
                        g.drawRect(r[i].x, r[i].y, r[i].width, r[i].height);
                        g.drawString(r[i].toString(), r[i].x + 20, r[i].y + ix);
                        ix += h;
                    }
                }
                g.translate(-40, -40);
            }
        }
        JFrame jf = new JFrame();
        jf.getContentPane().setLayout(new BorderLayout());
        jf.getContentPane().add(new C(minX, minY));
        jf.pack();
        jf.setVisible(true);
    }

    private static void showImageInFrame(BufferedImage redoImage, String caption) {
        JFrame jf = new JFrame();
        jf.setLayout(new BorderLayout());
        jf.add(new JLabel(new ImageIcon(redoImage)), BorderLayout.CENTER);
        jf.setBounds(300, 300, redoImage.getWidth() + 10, redoImage.getHeight() + 80);
        if (caption != null) {
            jf.setTitle(caption);
        }
        jf.setVisible(true);
    }

    public static boolean DEBUG = false;

    final class PaintEdit implements OwnedEdit {

        PaintingUndoData data;
        final String what;

        public PaintEdit(PaintingUndoData data, String what) {
            this.data = data;
            this.what = what;
        }

        public boolean isUndo = true;

        public void undo() throws CannotUndoException {
            if (!canUndo()) {
                throw new CannotUndoException();
            }
            BufferedImage undoImage = data.getUndoImage();
            Graphics2D g2d = img.createGraphics();
            g2d.setBackground(new Color(0, 0, 0, 0));
            if (undoImage != null) {
                if (DEBUG) {
//                    showImageInFrame(undoImage, "Undo image");
                }
                Point p = data.getUndoLocation();
                g2d.clearRect(p.x, p.y, undoImage.getWidth(),
                        undoImage.getHeight());
                replaceArea(undoImage, g2d, p);
                if (DEBUG) {
                    g2d.setColor(new Color(128, 255, 128, 128));
                    g2d.fillRect(p.x, p.y, undoImage.getWidth(), undoImage.getHeight());
                }
            }
            Rectangle[] r = data.getRectanglesToClear();
            for (int i = 0; i < r.length; i++) {
                g2d.clearRect(r[i].x, r[i].y, r[i].width, r[i].height);
                if (DEBUG) {
                    g2d.setColor(new Color(255, 128, 128, 128));
                    g2d.fillRect(r[i].x, r[i].y, r[i].width, r[i].height);
                }
            }
            g2d.dispose();
            snapshot = null;
            takeSnapshot();
            isUndo = false;
            _repaintArea(-1, -1, -1, -1);
        }

        public boolean canUndo() {
            return isUndo && data != null;
        }

        public void redo() throws CannotRedoException {
            if (!canRedo()) {
                throw new CannotRedoException();
            }
            BufferedImage redoImage = data.getRedoImage();
            if (DEBUG) {
                showImageInFrame(redoImage, "Redo image");
            }
            Graphics2D g2d = img.createGraphics();
            replaceArea(redoImage, g2d, data.getRedoLocation());
            g2d.dispose();
            snapshot = null;
            takeSnapshot();
            isUndo = true;
            _repaintArea(-1, -1, -1, -1);
        }

        private void replaceArea(BufferedImage replaceData, Graphics2D g2d, Point loc) {
            Rectangle replaceBounds = new Rectangle(loc,
                    new Dimension(replaceData.getWidth(),
                            replaceData.getHeight()));
            g2d.setBackground(new Color(0, 0, 0, 0));
            g2d.clearRect(replaceBounds.x, replaceBounds.y, replaceData.getWidth(),
                    replaceData.getHeight());

            PooledTransform.withTranslateInstance(replaceBounds.x, replaceBounds.y, xform -> {
                g2d.drawRenderedImage(replaceData, xform);
            });

//            g2d.drawRenderedImage(replaceData,
//                    AffineTransform.getTranslateInstance(replaceBounds.x,
//                            replaceBounds.y));

//            if (DEBUG) {
//                g2d.setColor (new Color (128,128,255,128));
//                g2d.fillRect (replaceBounds.x, replaceBounds.y, replaceBounds.width, replaceBounds.height);
//                System.err.println("REPLACE BONUDS " + replaceBounds);
//            }
            _repaintArea(replaceBounds.x, replaceBounds.y, replaceBounds.width,
                    replaceBounds.height);
        }

        public boolean canRedo() {
            return !isUndo && data != null;
        }

        public void die() {
            if (data != null) {
                data.dispose();
                data = null;
            }
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
            return what;
        }

        public String getUndoPresentationName() {
            return getPresentationName();
        }

        public String getRedoPresentationName() {
            return getPresentationName();
        }

        public boolean isChangeOf(RasterSurfaceImpl impl) {
            return RasterSurfaceImpl.this == impl;
        }

        public void zeroMoved(int x, int y) {
            data.zeroMoved(x, y);
        }
    }

    private class MoveEdit implements OwnedEdit {

        private Point then;
        private Point now;

        MoveEdit(Point then, Point now) {
            this.then = then;
            this.now = now;
        }

        public void zeroMoved(int x, int y) {
            then.translate(-x, -y);
            now.translate(-x, -y);
        }

        public boolean isChangeOf(RasterSurfaceImpl impl) {
            return impl == RasterSurfaceImpl.this;
        }

        public void undo() throws CannotUndoException {
            setLocation(then);
            isRedo = true;
        }
        private boolean isRedo = false;

        public boolean canUndo() {
            return !isRedo;
        }

        public void redo() throws CannotRedoException {
            setLocation(now);
            isRedo = false;
        }

        public boolean canRedo() {
            return isRedo;
        }

        public void die() {
            // So we'll get an NPE if it's called illegally
            then = null;
            now = null;
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
            return "Move layer from " + then.x + "," + then.y + " to " + now.x
                    + "," + now.y;
        }

        public String getUndoPresentationName() {
            return getPresentationName();
        }

        public String getRedoPresentationName() {
            return getPresentationName();
        }
    }
}
