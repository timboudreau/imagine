package org.netbeans.paint.tools.minidesigner;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeListener;
import net.dev.java.imagine.api.tool.Tool;
import net.dev.java.imagine.api.tool.ToolUIContextImplementation;
import net.dev.java.imagine.api.tool.aspects.Attachable;
import net.dev.java.imagine.api.tool.aspects.LookupContentsContributor;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant.Repainter;
import net.dev.java.imagine.api.tool.aspects.ScalingMouseListener;
import net.dev.java.imagine.spi.tool.ToolImplementation;
import net.dev.java.imagine.spi.tool.ToolUIContext;
import net.java.dev.imagine.api.image.RenderingGoal;
import net.java.dev.imagine.api.image.Surface;
import net.java.dev.imagine.spi.image.SurfaceImplementation;
import org.imagine.editor.api.AspectRatio;
import org.imagine.editor.api.ImageEditorBackground;
import org.imagine.editor.api.Zoom;
import org.imagine.geometry.EqPointDouble;
import org.imagine.geometry.util.GeometryStrings;
import org.imagine.geometry.util.PooledTransform;
import org.netbeans.paint.tools.path.PathTool;
import org.openide.util.ChangeSupport;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 * A mini editor UI which implements a limited version of all of the guts of the
 * main image editor, for use by tools that need some form of preview - presumes
 * you know what the tool is and what you want from it - call onCommit() with a
 * runnable which will be called when the tool has completed some action, and
 * extract its state.
 *
 * @author Tim Boudreau
 */
public class MiniToolCanvas extends JComponent implements Lookup.Provider {

    private final InstanceContent content = new InstanceContent();
    private final Lookup lkp = new AbstractLookup(content);
    private double scale = 1;
    private AspectRatio ratio;
    private ToolHolder<?> tool;
    private int baseWidth = 400;
    private int baseHeight = 300;
    private final Ctx ctx = new Ctx();
    private final RH repaintHandle = new RH();
    private final List<Painter> painters = new ArrayList<>();
    private final SurfaceImpl surf = new SurfaceImpl();
    private final Supplier<Graphics2D> graphicsSupplier;

    public MiniToolCanvas() {
        this(null);
    }

    public MiniToolCanvas(Supplier<Graphics2D> graphicsSupplier) {
        this.graphicsSupplier = graphicsSupplier;
        ratio = new AR();
        content.add(ctx);
        content.add(surf.getSurface());
        content.add(ratio);
        setFocusable(true);
    }

    class AR implements AspectRatio {

        @Override
        public double width() {
            double w = getWidth();
            w *= 1D / scale;
            return w;
        }

        @Override
        public double height() {
            double h = getHeight();
            h *= 1D / scale;
            return h;
        }
    }

    private void updateAspectRatio() {
        // some tools will be listening for the instance to change,
        // so give that to them
        replaceInLookup(this.ratio, this.ratio = new AR());
        System.out.println("Update AR "
                + GeometryStrings.toString(ratio.width())
                + " x " + GeometryStrings.toString(ratio.height()));
    }

    public <T> void addToLookup(T obj) {
        content.add(obj);
    }

    public <T> void removeFromLookup(T obj) {
        content.remove(obj);
    }

    public <T> void replaceInLookup(T old, T nue) {
        content.add(nue);
        content.remove(old);
    }

    public double getScale() {
        return scale;
    }

    @Override
    public void reshape(int x, int y, int w, int h) {
        boolean changed = getX() != x || getY() != y || getWidth() != w || getHeight() != h;
        super.reshape(x, y, w, h);
        if (changed) {
            updateAspectRatio();
        }
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            JFrame jf = new JFrame();
            jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            MiniToolCanvas mini = new MiniToolCanvas();
            mini.attach(new PathTool(mini.getLookup().lookup(Surface.class)));
//            mini.attachGeneric(new CircleTool(mini.getLookup().lookup(Surface.class)));
            mini.onCommitRequest(() -> {
                System.out.println("Commit!");
            });
            jf.setContentPane(mini);
            jf.pack();
            jf.setVisible(true);
        });
    }

    public Lookup getLookup() {
        return lkp;
    }

    public interface Painter {

        void paint(Graphics2D g, ToolUIContext ctx);
    }

    private List<Runnable> onCommitRequest = new ArrayList<>();

    public Runnable onCommitRequest(Runnable r) {
        onCommitRequest.add(r);
        return () -> {
            onCommitRequest.remove(r);
        };
    }

    public void attachGeneric(Object tool) {
        ToolImplementation<?> wrap = new WrapperImplementation<>(surf.getSurface(), tool);
        attach(wrap);
    }

    public <T extends ToolImplementation<?>> void attach(T tool) {
        if (this.tool != null) {
            Attachable attach = this.tool.as(Attachable.class);
            if (attach != null) {
                attach.detach();
            }
            removeMouseListener(this.tool);
            removeMouseMotionListener(this.tool);
            removeKeyListener(this.tool);
        }
        setCursor(Cursor.getDefaultCursor());
        this.tool = new ToolHolder<>(tool);
        addMouseListener(this.tool);
        addMouseMotionListener(this.tool);
        addKeyListener(this.tool);
        Attachable attach = this.tool.as(Attachable.class);
        if (attach != null) {
            attach.attach(this, ctx.toToolUIContext());
        }
        PaintParticipant pp = this.tool.as(PaintParticipant.class);
        if (pp != null) {
            pp.attachRepainter(repaintHandle);
        }
        requestFocus();
    }

    @Override
    public Dimension getPreferredSize() {
        int scaledW = (int) Math.ceil(baseWidth * scale);
        int scaledH = (int) Math.ceil(baseHeight * scale);
        return new Dimension(scaledW, scaledH);
    }

    @Override
    public void paintComponent(Graphics g) {
        paintComponent((Graphics2D) g);
    }

    public void setScale(double scale) {
        if (scale != this.scale) {
            this.scale = scale;
            invalidate();
            revalidate();
            updateAspectRatio();
            repaint();
        }
    }

    public void addPainter(Painter p) {
        painters.add(p);
    }

    private Color randomTransparentColor() {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        int r = 128 + rand.nextInt(127);
        int g = 128 + rand.nextInt(127);
        int b = 128 + rand.nextInt(127);
        return new Color(r, g, b, 90);
    }

    private boolean debugClip;

    public MiniToolCanvas debugClip(boolean debug) {
        this.debugClip = debug;
        return this;
    }

    public MiniToolCanvas debugClip() {
        return debugClip(true);
    }

    private void paintComponent(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        ImageEditorBackground.getDefault().fill(g, new Rectangle(0, 0, getWidth(), getHeight()));

        if (debugClip) {
            Shape clip = g.getClip();
            if (clip != null) {
                g.setColor(randomTransparentColor());
                g.fill(g.getClip());
            }
        }
        g.setStroke(new BasicStroke((float) (1F / scale)));
        g.scale(scale, scale);
        for (Painter p : painters) {
            p.paint(g, ctx.toToolUIContext());
        }
        if (tool != null) {
            PaintParticipant pp = tool.as(PaintParticipant.class);
            if (pp != null) {
                pp.paint(g, null, false);
            }
        }
        g.scale(1D / scale, 1D / scale);
    }

    class RH implements Repainter {

        private final Rectangle rect = new Rectangle();

        void postDispatch() {
            if (!rect.isEmpty()) {
                if (scale != 1) {
                    PooledTransform.withScaleInstance(scale, scale, xf -> {
                        MiniToolCanvas.this.repaint(xf.createTransformedShape(rect).getBounds());
                    });
                } else {
                    MiniToolCanvas.this.repaint(rect);
                }
                rect.width = rect.height = rect.x = rect.y = 0;
            }
        }

        @Override
        public void requestRepaint() {
            rect.setBounds(0, 0, MiniToolCanvas.this.getWidth(), MiniToolCanvas.this.getHeight());
            repaint();
        }

        @Override
        public void requestRepaint(Rectangle bounds) {
            if (bounds == null) {
                requestRepaint();
                return;
            }
            if (rect.isEmpty()) {
                rect.setFrame(bounds);
            } else {
                rect.add(bounds);
            }
//            System.out.println("Repaint " + GeometryStrings.toCoordinatesString(bounds)
//                    + " " + bounds.width + " x " + bounds.height);
//            MiniToolCanvas.this.repaint(bounds.x - 2, bounds.y - 2, bounds.width + 4, bounds.height + 4);
        }

        @Override
        public void setCursor(Cursor cursor) {
            MiniToolCanvas.this.setCursor(cursor);
        }

        @Override
        public void requestCommit() {
            requestRepaint();
        }

        @Override
        public Component getDialogParent() {
            return MiniToolCanvas.this;
        }
    }

    class Ctx implements ToolUIContextImplementation, Zoom {

        private final ChangeSupport supp = new ChangeSupport(this);

        @Override
        public Zoom zoom() {
            return this;
        }

        @Override
        public AspectRatio aspectRatio() {
            return ratio;
        }

        @Override
        public void fetchVisibleBounds(Rectangle into) {
            Rectangle rect = getVisibleRect();
            rect = Zoom.super.getZoomTransform().createTransformedShape(rect).getBounds();
            into.setFrame(rect);
        }

        @Override
        public double getZoom() {
            return (float) scale;
        }

        @Override
        public void setZoom(double val) {
            setScale(val);
            supp.fireChange();
        }

        @Override
        public void addChangeListener(ChangeListener cl) {
            supp.addChangeListener(cl);
        }

        @Override
        public void removeChangeListener(ChangeListener cl) {
            supp.removeChangeListener(cl);
        }
    }

    private class ToolHolder<T extends ToolImplementation<?>>
            extends MouseAdapter implements KeyListener {

        private T tool;

        public ToolHolder(T tool) {
            this.tool = tool;
        }

        T tool() {
            return tool;
        }

        <R> R as(Class<R> type) {
            R result = tool.getLookup().lookup(type);
            if (result == null && type.isInstance(tool)) {
                result = type.cast(tool);
            }
            return result;
        }

        @Override
        public void keyTyped(KeyEvent e) {
            KeyListener l = as(KeyListener.class);
            if (l != null) {
                l.keyTyped(e);
            }
            repaintHandle.postDispatch();
        }

        @Override
        public void keyPressed(KeyEvent e) {
            KeyListener l = as(KeyListener.class);
            if (l != null) {
                l.keyPressed(e);
            }
            repaintHandle.postDispatch();
        }

        @Override
        public void keyReleased(KeyEvent e) {
            KeyListener l = as(KeyListener.class);
            if (l != null) {
                l.keyReleased(e);
            }
            repaintHandle.postDispatch();
        }

        @Override
        public void mouseMoved(MouseEvent orig) {
            MouseEvent e = transformMouseEvent(orig);
            MouseMotionListener mml = as(MouseMotionListener.class);
            if (mml != null) {
                mml.mouseMoved(e);
            }
            ScalingMouseListener sml = as(ScalingMouseListener.class);
            if (sml != null) {
                EqPointDouble pt = scalePoint(orig.getPoint());
                sml.mouseMoved(pt.x, pt.y, e);
            }
            repaintHandle.postDispatch();
        }

        @Override
        public void mouseDragged(MouseEvent orig) {
            MouseEvent e = transformMouseEvent(orig);
            MouseMotionListener mml = as(MouseMotionListener.class);
            if (mml != null) {
                mml.mouseDragged(e);
            }
            ScalingMouseListener sml = as(ScalingMouseListener.class);
            if (sml != null) {
                EqPointDouble pt = scalePoint(orig.getPoint());
                sml.mouseDragged(pt.x, pt.y, e);
            }
            repaintHandle.postDispatch();
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent orig) {
            MouseWheelEvent e = transformMouseEvent(orig);
            MouseWheelListener mml = as(MouseWheelListener.class);
            if (mml != null) {
                mml.mouseWheelMoved(e);
            }
            ScalingMouseListener sml = as(ScalingMouseListener.class);
            if (sml != null) {
                EqPointDouble pt = scalePoint(orig.getPoint());
                sml.mouseWheelMoved(pt.x, pt.y, e);
            }
            repaintHandle.postDispatch();
        }

        @Override
        public void mouseExited(MouseEvent orig) {
            MouseEvent e = transformMouseEvent(orig);
            MouseListener mml = as(MouseListener.class);
            if (mml != null) {
                mml.mouseExited(e);
            }
            ScalingMouseListener sml = as(ScalingMouseListener.class);
            if (sml != null) {
                EqPointDouble pt = scalePoint(orig.getPoint());
                sml.mouseExited(pt.x, pt.y, e);
            }
            repaintHandle.postDispatch();
        }

        @Override
        public void mouseEntered(MouseEvent orig) {
            MouseEvent e = transformMouseEvent(orig);
            MouseListener mml = as(MouseListener.class);
            if (mml != null) {
                mml.mouseEntered(e);
            }
            ScalingMouseListener sml = as(ScalingMouseListener.class);
            if (sml != null) {
                EqPointDouble pt = scalePoint(orig.getPoint());
                sml.mouseEntered(pt.x, pt.y, e);
            }
            repaintHandle.postDispatch();
        }

        @Override
        public void mouseReleased(MouseEvent orig) {
            MouseEvent e = transformMouseEvent(orig);
            MouseListener mml = as(MouseListener.class);
            if (mml != null) {
                mml.mouseReleased(e);
            }
            ScalingMouseListener sml = as(ScalingMouseListener.class);
            if (sml != null) {
                EqPointDouble pt = scalePoint(orig.getPoint());
                sml.mouseReleased(pt.x, pt.y, e);
            }
            repaintHandle.postDispatch();
        }

        @Override
        public void mousePressed(MouseEvent orig) {
            if (!isFocusOwner()) {
                requestFocus();
            }
            MouseEvent e = transformMouseEvent(orig);
            MouseListener mml = as(MouseListener.class);
            if (mml != null) {
                mml.mousePressed(e);
            }
            ScalingMouseListener sml = as(ScalingMouseListener.class);
            if (sml != null) {
                EqPointDouble pt = scalePoint(orig.getPoint());
                sml.mousePressed(pt.x, pt.y, e);
            }
            repaintHandle.postDispatch();
        }

        @Override
        public void mouseClicked(MouseEvent orig) {
            MouseEvent e = transformMouseEvent(orig);
            MouseListener mml = as(MouseListener.class);
            if (mml != null) {
                mml.mouseClicked(e);
            }
            ScalingMouseListener sml = as(ScalingMouseListener.class);
            if (sml != null) {
                EqPointDouble pt = scalePoint(orig.getPoint());
                sml.mouseClicked(pt.x, pt.y, e);
            }
            repaintHandle.postDispatch();
        }

        private MouseEvent transformMouseEvent(MouseEvent evt) {
            EqPointDouble pt = scalePoint(evt.getPoint());
            int awtId = evt.getID();
            int x = (int) Math.round(pt.x);
            int y = (int) Math.round(pt.y);
            return new MouseEvent(MiniToolCanvas.this, awtId, evt.getWhen(), evt.getModifiers(), x, y,
                    evt.getXOnScreen(), evt.getYOnScreen(),
                    evt.getClickCount(), evt.isPopupTrigger(), evt.getButton());
        }

        private MouseWheelEvent transformMouseEvent(MouseWheelEvent evt) {
            EqPointDouble pt = scalePoint(evt.getPoint());
            int awtId = evt.getID();
            int x = (int) Math.round(pt.x);
            int y = (int) Math.round(pt.y);
            return new MouseWheelEvent(MiniToolCanvas.this, awtId, evt.getWhen(), evt.getModifiers(), x, y,
                    evt.getXOnScreen(), evt.getYOnScreen(),
                    evt.getClickCount(), evt.isPopupTrigger(), evt.getScrollType(), evt.getScrollAmount(),
                    evt.getWheelRotation(), evt.getPreciseWheelRotation());
        }

        EqPointDouble scalePoint(Point orig) {
            EqPointDouble pt = new EqPointDouble(orig);
            PooledTransform.withScaleInstance(1D / scale, 1D / scale, (AffineTransform xform) -> {
                xform.transform(pt, pt);
            });
            return pt;
        }
    }

    class SurfaceImpl extends SurfaceImplementation {

        @Override
        public Graphics2D getGraphics() {
            if (graphicsSupplier != null) {
                return graphicsSupplier.get();
            }
            return (Graphics2D) MiniToolCanvas.this.getGraphics();
        }

        @Override
        public void setLocation(Point p) {
            // do nothing
        }

        @Override
        public Point getLocation() {
            return new Point();
        }

        @Override
        public void beginUndoableOperation(String name) {
            // do nothing
            System.out.println("begin undoable");
        }

        @Override
        public void endUndoableOperation() {
            System.out.println("end undoable");
            // do nothing
            for (Runnable r : onCommitRequest) {
                r.run();
            }
        }

        @Override
        public void cancelUndoableOperation() {
            // do nothing
        }

        @Override
        public void setCursor(Cursor cursor) {
            MiniToolCanvas.this.setCursor(cursor);
        }

        @Override
        public void setTool(Tool tool) {
            // do nothing
        }

        @Override
        public void applyComposite(Composite composite, Shape clip) {
            // do nothing
        }

        @Override
        public boolean paint(RenderingGoal goal, Graphics2D g, Rectangle r, Zoom zoom) {
            // do nothing
            return false;
        }

        @Override
        public Dimension getSize() {
            return MiniToolCanvas.this.getSize();
        }

    }

    static final class WrapperImplementation<T> extends ToolImplementation<T> {

        private final Object delegate;

        public WrapperImplementation(T obj, Object delegate) {
            super(obj);
            this.delegate = delegate;
        }

        @Override
        public void createLookupContents(Set<? super Object> addTo) {
            if (delegate instanceof LookupContentsContributor) {
                LookupContentsContributor c = (LookupContentsContributor) delegate;
                c.createLookupContents(addTo);
            }
            addTo.add(delegate);
            addTo.add(this);
        }

        @Override
        public void attach(Lookup.Provider layer, ToolUIContext ctx) {
            super.attach(layer);
            for (Attachable a : getLookup().lookupAll(Attachable.class)) {
                if (a != this) {
                    a.attach(layer, ctx);
                }
            }
        }

        @Override
        public void detach() {
            try {
                for (Attachable a : getLookup().lookupAll(Attachable.class)) {
                    if (a != this) {
                        a.detach();
                    }
                }
            } finally {
                super.detach();
            }
        }
    }

}
