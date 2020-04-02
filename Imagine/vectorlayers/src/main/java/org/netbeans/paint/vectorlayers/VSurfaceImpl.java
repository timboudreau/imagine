/*
 * VSurfaceImpl.java
 *
 * Created on October 25, 2006, 9:28 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.netbeans.paint.vectorlayers;

import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.dev.java.imagine.api.tool.Tool;
import org.imagine.editor.api.snap.SnapAxis;
import org.imagine.editor.api.snap.SnapPointsConsumer;
import org.imagine.editor.api.snap.SnapPoints;
import org.imagine.editor.api.snap.SnapPointsSupplier;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.Primitive;
import org.imagine.utils.painting.RepaintHandle;
import net.java.dev.imagine.spi.image.SurfaceImplementation;
import net.java.dev.imagine.api.vector.painting.VectorWrapperGraphics;
import net.java.dev.imagine.api.vector.util.Pt;
import org.imagine.editor.api.Zoom;
import org.imagine.editor.api.snap.SnapPointsBuilder;
import org.imagine.utils.java2d.GraphicsUtils;
import org.imagine.utils.java2d.LazyGraphics;
import org.imagine.utils.java2d.GraphicsProvider;
import org.openide.util.Pair;
import net.java.dev.imagine.api.vector.Vectors;

/**
 *
 * @author Tim Boudreau
 */
class VSurfaceImpl extends SurfaceImplementation implements RepaintHandle {

    private Point location = new Point();
    ShapeStack stack;
    private final Graphics2D graphics;
    private final RepaintHandle owner;
    private static Logger logger = Logger.getLogger(VSurfaceImpl.class.getName());
//    static {
//        logger.setLevel(Level.ALL);
//    }
    private static final boolean log = logger.isLoggable(Level.FINE);

    private final Dimension initialSize;
    private final BooleanSupplier isVisible;

    public VSurfaceImpl(RepaintHandle owner, Dimension d, BooleanSupplier isVisible) {
        stack = new ShapeStack(this);
        initialSize = d;
//        graphics = new VectorWrapperGraphics(stack,
//                GraphicsUtils.noOpGraphics(),
//                new Point(), 1, 1);

        graphics = LazyGraphics.create(new GraphicsProvider() {
            @Override
            public Graphics2D getGraphics() {
                return new VectorWrapperGraphics(stack,
                        GraphicsUtils.noOpGraphics(),
                        new Point(), 1, 1);
            }

            @Override
            public void onDispose(Graphics2D graphics) {
                stack.endDraw();
            }
        }, true);

        this.owner = owner;
        this.isVisible = isVisible;
    }

    public VSurfaceImpl(VSurfaceImpl other) {
        this(other.owner, other.getBounds().getSize(), other.isVisible);
        stack = new ShapeStack(other.stack);
    }

    class SurfaceRepaintHandle implements RepaintHandle {

        private final RepaintHandle orig;

        public SurfaceRepaintHandle(RepaintHandle orig) {
            this.orig = orig;
        }

        @Override
        public void repaintArea(int x, int y, int w, int h) {
            snapPointSupplier.discardCache();
            orig.repaintArea(x, y, w, h);
        }

        @Override
        public void setCursor(Cursor cursor) {
            orig.setCursor(cursor);
        }

    }

    public boolean canApplyEffects() {
        return isVisible.getAsBoolean();
    }

    void transform(AffineTransform xform) {
        if (stack == null) {
            return;
        }
        Dimension sz = getSize();
        ShapeStack nue = new ShapeStack(this);
        for (Primitive p : stack.primitives) {
            if (p instanceof Vectors) {
                Vectors v = (Vectors) p;
                nue.add(v.copy(xform));
            } else {
                nue.add(p);
            }
        }
        stack = nue;
        Dimension sz2 = getSize();
        repaintArea(0, 0, Math.max(sz.width, sz2.width), Math.max(sz.height,
                sz2.height));
    }

    public Graphics2D getGraphics() {
        return graphics;
    }

    private void locationChanged(Point old, Point nue) {
        Dimension d = stack.getSize();
        Rectangle oldBds = new Rectangle(old.x, old.y, d.width, d.height);
        Rectangle newBds = new Rectangle(nue.x, nue.y, d.width, d.height);
        if (cachedBounds != null) {
            cachedBounds.setBounds(newBds);
        } else {
            cachedBounds = newBds;
        }
        Rectangle union = oldBds.union(newBds);
        repaintArea(union.x, union.y, union.width, union.height);
    }

    private Rectangle cachedBounds = new Rectangle();

    Rectangle getBounds() {
        if (cachedBounds == null) {
            Dimension d = stack.getSize();
            cachedBounds = new Rectangle(location.x, location.y, d.width,
                    d.height);
            cachedBounds.width = Math.max(cachedBounds.width,
                    initialSize.width);

            cachedBounds.height = Math.max(cachedBounds.height,
                    initialSize.height);
        }
        return cachedBounds;
    }

    public void setLocation(Point p) {
//        System.err.println("xx SET LOCATION " + p + " old is " + location);
        if (!location.equals(p)) {
            Point old = new Point(location);
//            System.err.println("Set location " + p);
            location.setLocation(p);
            locationChanged(old, p);
            cachedBounds = null;
            stack.location = new Pt(p.x, p.y);
//            System.err.println("SET LOC TO " + stack.location);
        }
        Rectangle r = getBounds();
        repaintArea(r.x, r.y, r.width, r.height);
    }

    public Point getLocation() {
        return new Point(location);
    }

    public void beginUndoableOperation(String name) {
        stack.beginDraw();
    }

    public void endUndoableOperation() {
        stack.endDraw();
    }

    public void cancelUndoableOperation() {
        stack.cancelDraw(); //XXX
    }

    public void setCursor(Cursor cursor) {
        owner.setCursor(cursor);
    }

    private Tool tool;

    @Override
    public void setTool(Tool tool) {
        if (tool != this.tool) {
            this.tool = tool;
            if (!NO_CACHE) {
                if (tool == null) {
                    createCache();
                } else {
                    dumpCache();
                }
            }
            if (tool != null) {
                SnapPointsConsumer c = tool.getLookup().lookup(SnapPointsConsumer.class);
                if (c != null) {
                    c.accept(snapPointSupplier);
                }
            }
        }
    }

    SnapPointsSupplier snapPoints() {
        return snapPointSupplier;
    }

    private final SnSupplier snapPointSupplier = new SnSupplier();

    class SnSupplier implements SnapPointsSupplier {

        SnapPoints cached;

        @Override
        public SnapPoints get() {
            if (cached != null) {
                return cached;
            }
            SnapPointsBuilder b
                    = SnapPoints.builder(5);
            for (Primitive p : stack.primitives) {
                addTo(p, b);
            }
            return cached = b.build();
        }

        private void discardCache() {
            cached = null;
        }
    }

    private double[] pts = new double[8];

    private void addTo(Primitive vect, SnapPointsBuilder bldr) {
        if (vect instanceof Adjustable) {
            Adjustable adj = (Adjustable) vect;
            int cpCount = adj.getControlPointCount();
            adj.getControlPoints(pts);
            int[] virt = adj.getVirtualControlPointIndices();
            for (int i = 0; i < cpCount * 2; i += 2) {
                int ptIx = i / 2;
                if (Arrays.binarySearch(virt, ptIx) < 0) {
                    bldr.add(SnapAxis.X, pts[i], null);
                    bldr.add(SnapAxis.Y, pts[i + 1], null);
                }
            }
        }
    }

    private void dumpCache() {
        cache = null;
    }

    private BufferedImage cache;

    private void createCache() {
        Rectangle r = getBounds();
        if (r.width == 0 || r.height == 0) {
            return;
        }
        cache = GraphicsEnvironment.getLocalGraphicsEnvironment().
                getDefaultScreenDevice().getDefaultConfiguration().
                createCompatibleImage(r.width - location.x,
                        r.height - location.x, Transparency.TRANSLUCENT);
    }

    private List<AppliedComposite> composites = Collections.synchronizedList(
            new ArrayList<>());

    public void applyComposite(Composite composite, Shape clip) {
        composites.clear(); //XXX handle stacking/merging these
        composites.add(new AppliedComposite(composite, clip));
        repaint();
    }

    private void repaint() {
        owner.repaintArea(getBounds());
    }

    @Override
    public Dimension getSize() {
        return stack.getSize();
    }

    private static final class AppliedComposite {

        public final Composite composite;
        public final Shape clip;

        public AppliedComposite(Composite composite, Shape clip) {
            this.composite = composite;
            this.clip = clip;
        }

        public boolean hasClip() {
            return clip != null;
        }

        public void apply(Graphics2D g) {
            g.setComposite(composite);
        }
    }
    private final AppliedBufferedImageOp ops = new AppliedBufferedImageOp();

    @Override
    public void applyBufferedImageOp(BufferedImageOp op, Shape clip) {
        ops.add(op, clip);
        repaint();
    }

    private final class AppliedBufferedImageOp {

        final List<Pair<BufferedImageOp, Shape>> ops = new ArrayList<>();

        AppliedBufferedImageOp() {

        }

        void add(BufferedImageOp op, Shape clip) {
            ops.add(Pair.of(op, clip));
        }

        boolean apply(Graphics2D target, Rectangle rect, BiPredicate<Graphics2D, Rectangle> internalPaint) {
            if (ops.isEmpty()) {
                return internalPaint.test(target, rect);
            }
            boolean result = false;
            Dimension d = getSize();
            BufferedImage buffer = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = buffer.createGraphics();
            try {
                GraphicsUtils.setHighQualityRenderingHints(g);
                result = internalPaint.test(g, null);
                if (!result) {
                    return false;
                }
                BufferedImage output = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g1 = output.createGraphics();
                try {
                    g1.drawRenderedImage(buffer, null);
                    for (Pair<BufferedImageOp, Shape> p : ops) {
                        BufferedImageOp op = p.first();
                        g1.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g1.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                        g1.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                        g1.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                        g1.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
                        g1.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        g1.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                        RenderingHints rh = op.getRenderingHints();
                        if (rh != null) {
                            g1.setRenderingHints(op.getRenderingHints());
                        }
                        if (p.second() != null) {
                            g1.setClip(p.second());
                        } else {
                            g1.setClip(null);
                        }
                        op.filter(buffer, output);
                    }
                } finally {
                    g1.dispose();
                }
                if (rect != null) {
                    Rectangle bds = getBounds();
                    double rw = rect.width;
                    double rh = rect.height;
                    double bw = bds.width;
                    double bh = bds.height;
                    AffineTransform xform = AffineTransform.getScaleInstance(rw / bw,
                            rh / bh);
                    target.drawRenderedImage(output, xform);
                } else {
                    target.drawRenderedImage(output, null);
                }
                output.flush();
            } finally {
                buffer.flush();
            }
            return result;
        }
    }

    static boolean NO_CACHE = true;//Boolean.getBoolean("vector.no.cache");
//    static boolean NO_CACHE = Boolean.getBoolean("vector.no.cache");

    public boolean paint(Graphics2D g) {
        return paint(g, null, Zoom.ONE_TO_ONE);
    }

    public boolean paint(Graphics2D g, Rectangle r, Zoom zoom) {
        return ops.apply(g, r, this::internalPaint);
    }

    private boolean internalPaint(Graphics2D g, Rectangle r) {
//        System.err.println("VSurfaceImpl.paint " + r + " loc " + location + " with a " + g);
        if (g instanceof VectorWrapperGraphics) {
            throw new IllegalStateException("Wrong graphics object");
        }
        if (stack.isEmpty()) {
            return false;
        }

        if (r == null) {
            if (!NO_CACHE) {
                if (cache == null && tool == null) {
                    createCache();
                }
                if (cache != null) {
                    g.drawRenderedImage(cache,
                            AffineTransform.getTranslateInstance(location.x, location.y));
                    return true;
                }
            }
            if (log) {
                logger.log(Level.FINER, "Start paint of stack");
            }
            g.translate(location.x, location.y);
            Composite old = g.getComposite();
            if (!composites.isEmpty()) {
                g.setComposite(composites.iterator().next().composite);
            }
            boolean result = stack.paint(g);
            if (old != null) {
                g.setComposite(old);
            }
            g.translate(-location.x, -location.y);
            return result;
        } else {
            Rectangle bds = getBounds();
            double rw = r.width;
            double rh = r.height;
            double bw = bds.width;
            double bh = bds.height;
            Graphics2D g2 = (Graphics2D) g.create();
            g.translate(location.x, location.y);
            AffineTransform xform = AffineTransform.getScaleInstance(rw / bw,
                    rh / bh);

            AffineTransform curr = g2.getTransform();
            curr.concatenate(xform);
            g2.setTransform(curr);
            stack.paint(g2);
            g2.dispose();
            return true;
        }
    }

    public void repaintArea(int x, int y, int w, int h) {
        owner.repaintArea(x, y, w, h);
        if (cachedBounds != null && (x + w > cachedBounds.x + cachedBounds.width
                || y + h > cachedBounds.height)) {
            cachedBounds = null;
            cache = null;
        }
    }
}
