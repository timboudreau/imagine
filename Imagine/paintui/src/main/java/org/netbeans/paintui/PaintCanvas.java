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
package org.netbeans.paintui;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.dev.java.imagine.api.tool.Tool;
import net.dev.java.imagine.api.tool.ToolUIContextImplementation;
import net.java.dev.imagine.api.image.Layer;
import net.java.dev.imagine.spi.image.LayerImplementation;
import net.java.dev.imagine.spi.image.PictureImplementation;
import org.imagine.utils.painting.RepaintHandle;
import net.java.dev.imagine.spi.image.SurfaceImplementation;
import org.imagine.editor.api.Zoom;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant.Repainter;
import net.dev.java.imagine.spi.tool.ToolUIContext;
import net.java.dev.imagine.api.image.RenderingGoal;
import org.imagine.editor.api.ImageEditorBackground;
import net.java.dev.imagine.ui.common.PositionStatusLineElementProvider;
import org.imagine.editor.api.AspectRatio;
import org.imagine.utils.java2d.GraphicsUtils;
import org.openide.util.ChangeSupport;
import org.openide.util.Lookup;
import org.openide.util.WeakListeners;

/**
 *
 * @author Timothy Boudreau
 */
class PaintCanvas extends JComponent implements RepaintHandle, ChangeListener, PropertyChangeListener, Repainter {

    private String comma = ",";
    private AppPicture picture;
    private boolean showGrid = false;
    private Color gridColor = Color.BLACK;
    private PositionStatusLineElementProvider pslep;
    StringBuilder builder = new StringBuilder();

    public PaintCanvas() {
        this(new Dimension(400, 300));
    }

    public PaintCanvas(Dimension d) {
        this(d, true);
    }

    public PaintCanvas(Dimension d, boolean xpar) {
        init(new AppPicture(this, d), !xpar);
    }

    public PaintCanvas(BufferedImage img) {
        init(new AppPicture(this, img), false);
    }

    AspectRatio aspectRatio() {
        return picture.aspectRatio();
    }

    void pictureResized(int width, int height) {
        picture.resized(width, height);
        invalidate();
        revalidate();
        repaint();
    }

    private void init(AppPicture picture, boolean fill) {
        this.picture = picture;
        enableEvents(AWTEvent.KEY_EVENT_MASK
                | AWTEvent.MOUSE_EVENT_MASK
                | AWTEvent.MOUSE_MOTION_EVENT_MASK
                | AWTEvent.MOUSE_WHEEL_EVENT_MASK);
        picture.addChangeListener((ChangeListener) WeakListeners.change(this,
                picture));
        LayerImplementation l = (LayerImplementation) picture.getLayers().iterator().next();
        if (l == null) {
            throw new NullPointerException("No layer in picture");
        }
        if (fill) {
            assert l != null;
            SurfaceImplementation surface = l.getSurface(); //XXX surface.clear (Color)
            if (surface != null) {
                Graphics2D g = surface.getGraphics();
                Dimension d = l.getBounds().getSize();
                if (fill) {
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, d.width, d.height);
                }
            }
        }
        setFocusable(true);
    }

    ToolUIContext uiContext = new ToolUIContextImplementation() {
        @Override
        public Zoom zoom() {
            return zoomImpl;
        }

        @Override
        public AspectRatio aspectRatio() {
            return PaintCanvas.this.aspectRatio();
        }

        @Override
        public ToolUIContext toToolUIContext() {
            return uiContext;
        }

        @Override
        public void fetchVisibleBounds(Rectangle into) {
            into.setFrame(getVisibleRect());
        }
    }.toToolUIContext();

    AppPicture getPicture() {
        return picture;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension result = picture.getSize();
        if (zoom != 1.0f) {
            result.width *= zoom;
            result.height *= zoom;
        }
        return result;
    }
    private double zoom = 1.0f;

    public void setZoom(double zoom) {
        if (this.zoom != zoom) {
            Dimension old = getPreferredSize();
            this.zoom = zoom;
            firePropertyChange("preferredSize", old, //NOI18N
                    getPreferredSize());
            invalidate();
            revalidate();
            repaint();
        }
    }

    public double getZoom() {
        return zoom;
    }
    private Rectangle imageBounds = new Rectangle();

    private Rectangle getImageAreaBounds() {
        Dimension d = getPreferredSize();
        int x = 0;
        int y = 0;
        if (d.width < getWidth()) {
            x = (getWidth() / 2) - (d.width / 2);
        }
        if (d.height < getHeight()) {
            y = (getHeight() / 2) - (d.height / 2);
        }
        imageBounds.setBounds(x, y, d.width, d.height);
        return imageBounds;
    }
    AffineTransform scratchAt1 = AffineTransform.getTranslateInstance(0, 0);
    AffineTransform scratchAt2 = AffineTransform.getTranslateInstance(0, 0);
    AffineTransform scratchAt3 = AffineTransform.getTranslateInstance(0, 0);

    private AffineTransform getCurrentTransform() {
        Rectangle r = getImageAreaBounds();
        Point p = getLocation();
        r.x += p.x;
        r.y += p.y;
        scratchAt1.setToTranslation(r.x, r.y);
        scratchAt2.setToScale(zoom, zoom);
        scratchAt1.concatenate(scratchAt2);
        return scratchAt1;
    }

    @Override
    public void paint(Graphics g) {
        g.setColor(getBackground());

        g.fillRect(0, 0, getWidth(), getHeight());

        Rectangle r = getImageAreaBounds();
        Graphics2D g2d = (Graphics2D) g;

        if (r.x != 0 || r.y != 0) {
            g.setColor(getForeground());
            g.drawRect(r.x - 1, r.y - 1, r.width + 2, r.height + 2);
        }
        GraphicsUtils.setHighQualityRenderingHints(g2d);
        Shape clip = g2d.getClip();
        g2d.setClip(r.intersection(g2d.getClipBounds()));
        if (!showGrid) {
            g2d.setPaint(ImageEditorBackground.getDefault().style().getPaint());
            g.fillRect(r.x, r.y, r.width, r.height);
        }

        AffineTransform old = g2d.getTransform();

        scratchAt3.setToIdentity();
        scratchAt3.concatenate(old);
        scratchAt3.concatenate(getCurrentTransform());
        g2d.setTransform(scratchAt3);

        boolean painted = picture.paint(RenderingGoal.EDITING, g2d, null, true, zoomImpl);
        if (tool != null) {
            PaintParticipant participant = get(tool, PaintParticipant.class);
            if (participant != null) {
                participant.paint(g2d, imageBounds, false);
            }
        }
        g2d.setTransform(old);
        g2d.setClip(clip);
        if (showGrid) {
            int x = r.x + r.width;
            int y = r.y + r.height;

            double gridDistance = zoom;

            if (gridDistance < 3) // Disable painting of grid if no image pixels would be visible.
            {
                return;
            }

            g.setColor(gridColor);
            g.setXORMode(Color.WHITE);

            double actualDistance = r.x;

            for (int i = (int) actualDistance; i < x; actualDistance += gridDistance, i = (int) actualDistance) {
                g.drawLine(i, r.y, i, y);
            }

            actualDistance = r.y;

            for (int j = (int) actualDistance; j < y; actualDistance += gridDistance, j = (int) actualDistance) {
                g.drawLine(r.x, j, x, j);
            }
            g.setPaintMode();
        }
//	g2d.dispose();
    }

    public void repaintArea(int x, int y, int w, int h) {
        if (w < 0 || h < 0) {
            repaint();
        } else {
            Point2D.Float p2d = new Point2D.Float(x, y);
            getCurrentTransform().transform(p2d, scratchPoint);
            x = (int) Math.floor(scratchPoint.getX());
            y = (int) Math.floor(scratchPoint.getY());

            w = (int) Math.ceil((float) w * zoom) + 1;
            h = (int) Math.ceil((float) h * zoom) + 1;
            repaint(x, y, w, h);
        }
    }

    public BufferedImage getImage() {
        Dimension d = picture.getSize();
        return GraphicsUtils.newBufferedImage(d.width, d.height, g -> {
            picture.paint(RenderingGoal.PRODUCTION, g, null, true, Zoom.ONE_TO_ONE);
        });
    }
    private Tool tool;

    public void setActiveTool(Tool tool) {
        if (this.tool == tool) {
            return;
        }
        if (this.tool != null) {
            this.tool.detach();
        }
        this.tool = tool;
        if (tool != null && picture.getActiveLayer() != null) {
            LayerImplementation l = picture.getActiveLayer();
            tool.attach(l.getLayer(), uiContext);
            PaintParticipant participant = get(tool, PaintParticipant.class);
            if (participant != null) {
                participant.attachRepainter(this);
            }
            SurfaceImplementation surf = l.getSurface();
            if (surf != null) {
                l.getSurface().setTool(tool);
            }
        }
    }

    @Override
    public void setBorder(Border b) {
        //do nothing
    }

    @Override
    public Insets getInsets() {
        return new Insets(0, 0, 0, 0);
    }

    private <T extends Object> T get(Tool tool, Class<T> clazz) {
        return tool == null ? null : tool.getLookup().lookup(clazz);
    }
    int currMouseEventId = 0;

    @Override
    protected void processMouseEvent(MouseEvent me) {
        currMouseEventId = me.getID();
        super.processMouseEvent(me);
        MouseListener ml = get(tool, MouseListener.class);
        if (ml != null) {
            if (getImageAreaBounds().contains(me.getX(), me.getY())) {
                dispatchToTool(me);
            } else if (tool != null && me.getID() == me.MOUSE_RELEASED) {
                //Always let the tool detach if it was engaged
                dispatchToTool(me);
            }
        }

        if (me.getID() == MouseEvent.MOUSE_PRESSED) {
            PaintTopComponent ptc = (PaintTopComponent) SwingUtilities.getAncestorOfClass(PaintTopComponent.class, this);
            if (ptc != null && !ptc.isActive()) {
                ptc.requestActive();
            }
        }
    }
    private JScrollPane enclosingScrollPane = null;

    @Override
    public void addNotify() {
        super.addNotify();
        enclosingScrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(
                JScrollPane.class, this);
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        enclosingScrollPane = null;
    }

    @Override
    protected void processMouseWheelEvent(MouseWheelEvent e) {
        if ((e.getModifiersEx() & MouseWheelEvent.SHIFT_DOWN_MASK) != 0) {
            //Use Shift-MouseWheel to zoom
            float ct = -e.getWheelRotation();
            double zoomFactor = getZoom();
            zoomFactor += ct / 10;
            zoomFactor = Math.max(0.1f, zoomFactor);
            zoomImpl.setZoom(zoomFactor);
            e.consume();
        } else if (enclosingScrollPane != null) {
            //If we do any handling of mouse wheel events, then default
            //mouse wheel handling is dead, so we have to do it ourselves.

            //Try to decide which axis to scroll by proximity.
            Point p = SwingUtilities.convertPoint(this, e.getPoint(),
                    enclosingScrollPane);
            Dimension sz = enclosingScrollPane.getSize();
            boolean possiblyHorizontal = p.y > (sz.height / 4) * 3;
            JScrollBar bar;
            JScrollBar horiz = enclosingScrollPane.getHorizontalScrollBar();
            boolean both = false;
            if (possiblyHorizontal && horiz.isShowing()) {
                bar = horiz;
                both = enclosingScrollPane.getVerticalScrollBar().isShowing()
                        && p.x > (sz.width / 4) * 3;
            } else if (enclosingScrollPane.getVerticalScrollBar().isShowing()) {
                bar = enclosingScrollPane.getVerticalScrollBar();
            } else {
                bar = null;
            }
            if (bar != null) {
                int ct = e.getWheelRotation();
                adjustScrollBar(bar, ct);
                if (both) {
                    adjustScrollBar(enclosingScrollPane.getVerticalScrollBar(), ct);
                }
            }

        } else {
            super.processMouseWheelEvent(e);
        }
    }

    private void adjustScrollBar(JScrollBar bar, int ct) {
        ct *= 10;
        int val = bar.getModel().getValue();
        if (val + ct >= bar.getModel().getMinimum()
                && val + ct <= bar.getModel().getMaximum()) {

            bar.getModel().setValue(val + ct);
        }
    }
    private Point2D.Float scratchPoint = new Point2D.Float();

    private void dispatchToTool(MouseEvent me) {
        Point2D p2d = new Point2D.Float(me.getX(), me.getY());
        try {
            getCurrentTransform().inverseTransform(p2d, scratchPoint);
        } catch (NoninvertibleTransformException nte) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE,
                    null, nte);
            return;
        }
        int x = (int) scratchPoint.getX();
        int y = (int) scratchPoint.getY();
        if (pslep == null) {
            pslep = Lookup.getDefault().lookup(PositionStatusLineElementProvider.class);
        }
        builder = new StringBuilder();
        builder.append(x);
        builder.append(comma);
        builder.append(y);
        pslep.setStatus(builder.toString());
        MouseEvent te = new MouseEvent(this, me.getID(), me.getWhen(), me.getModifiers(),
                x, y, me.getClickCount(), me.isPopupTrigger(), me.getButton());

        MouseListener ml = get(tool, MouseListener.class);
        MouseMotionListener mml = get(tool, MouseMotionListener.class);
        switch (te.getID()) {
            case MouseEvent.MOUSE_PRESSED:
                if (ml != null) {
                    ml.mousePressed(te);
                }
                break;
            case MouseEvent.MOUSE_RELEASED:
                if (ml != null) {
                    ml.mouseReleased(te);
                }
                break;
            case MouseEvent.MOUSE_CLICKED:
                if (ml != null) {
                    ml.mouseClicked(te);
                }
                break;
            case MouseEvent.MOUSE_ENTERED:
                if (ml != null) {
                    ml.mouseEntered(te);
                }
                break;
            case MouseEvent.MOUSE_EXITED:
                if (ml != null) {
                    ml.mouseExited(te);
                }
                break;
            case MouseEvent.MOUSE_MOVED:
                if (mml != null) {
                    mml.mouseMoved(te);
                }
                break;
            case MouseEvent.MOUSE_DRAGGED:
                if (mml != null) {
                    mml.mouseDragged(te);
                }
                break;
        }
    }

    @Override
    protected void processKeyEvent(KeyEvent ke) {
        super.processKeyEvent(ke);
        KeyListener kl = get(tool, KeyListener.class);
        if (kl != null) {
            switch (ke.getID()) {
                case KeyEvent.KEY_PRESSED:
                    kl.keyPressed(ke);
                    break;
                case KeyEvent.KEY_RELEASED:
                    kl.keyReleased(ke);
                    break;
                case KeyEvent.KEY_TYPED:
                    kl.keyTyped(ke);
            }
        }
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent me) {
        currMouseEventId = me.getID();
        super.processMouseMotionEvent(me);
        MouseMotionListener mml = get(tool, MouseMotionListener.class);
        if (mml != null) {
            if (getImageAreaBounds().contains(me.getX(), me.getY())) {
                dispatchToTool(me);
            }
        }
    }

    Layer layerFor(LayerImplementation impl) {
        return impl.getLookup().lookup(Layer.class);
    }

    public void stateChanged(ChangeEvent e) {
        if (e.getSource() instanceof PictureImplementation) {
            PictureImplementation pictureImpl = (PictureImplementation) e.getSource();
            if (tool != null) {
                tool.detach();
                LayerImplementation layer = pictureImpl.getActiveLayer();
                if (layer != null) {
                    tool.attach(layer.getLayer(), uiContext);
                }
            }
            for (Iterator i = pictureImpl.getLayers().iterator(); i.hasNext();) {
                LayerImplementation layer = (LayerImplementation) i.next();
                layer.addPropertyChangeListener(WeakListeners.propertyChange(this, layer));
            }
            repaint();
        } else if (e.getSource() instanceof Tool) {
            repaint();
        } else {
            throw new IllegalArgumentException(e.getSource().getClass().getName());
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (Layer.PROP_BOUNDS.equals(evt.getPropertyName())
                || Layer.PROP_OPACITY.equals(evt.getPropertyName())
                || Layer.PROP_VISIBLE.equals(evt.getPropertyName())) {
            repaint();
        }
    }

    public void requestRepaint(Rectangle bounds) {
        if (bounds == null) {
            repaint();
        } else {
            repaint(bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }

    public void requestCommit() {
        LayerImplementation l = picture.getActiveLayer();
        if (l != null) {
            SurfaceImplementation surface = l.getSurface();
            if (surface != null) {
                surface.beginUndoableOperation(tool.getName());
                Graphics2D g = surface.getGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
                g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                        RenderingHints.VALUE_STROKE_PURE);
                PaintParticipant painter = get(tool, PaintParticipant.class);
                if (painter != null) {
                    painter.paint(g, imageBounds, true);
                }
                surface.endUndoableOperation();
                g.dispose();
            }
        }
    }

    public Component getDialogParent() {
        return this;
    }

    @Override
    public String toString() {
        return "PaintCanvas@" + System.identityHashCode(this);
    }
    Zoom zoomImpl = new ZoomImpl();

    private class ZoomImpl implements Zoom {

        private ChangeSupport supp = new ChangeSupport(this);

        public double getZoom() {
            return PaintCanvas.this.getZoom();
        }

        public void setZoom(double val) {
            if (val != getZoom()) {
                PaintCanvas.this.setZoom(val);
                supp.fireChange();
            }
        }

        public void addChangeListener(ChangeListener cl) {
            supp.addChangeListener(cl);
        }

        public void removeChangeListener(ChangeListener cl) {
            supp.removeChangeListener(cl);
        }
    }

    public void requestRepaint() {
        repaint();
    }
}
