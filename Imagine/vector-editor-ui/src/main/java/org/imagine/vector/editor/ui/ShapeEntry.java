package org.imagine.vector.editor.ui;

import com.mastfrog.util.collections.IntSet;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Set;
import java.util.function.Consumer;
import org.imagine.editor.api.snap.SnapAxis;
import net.java.dev.imagine.api.image.Hibernator;
import net.java.dev.imagine.api.image.RenderingGoal;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.Shaped;
import net.java.dev.imagine.api.vector.Textual;
import net.java.dev.imagine.api.vector.Transformable;
import net.java.dev.imagine.api.vector.Volume;
import net.java.dev.imagine.api.vector.design.ControlPoint;
import net.java.dev.imagine.api.vector.design.ControlPointController;
import net.java.dev.imagine.api.vector.design.DelegatingControlPointController;
import net.java.dev.imagine.api.vector.design.ShapeNames;
import net.java.dev.imagine.api.vector.elements.ImageWrapper;
import net.java.dev.imagine.api.vector.elements.PathIteratorWrapper;
import net.java.dev.imagine.api.vector.elements.PathText;
import net.java.dev.imagine.api.vector.graphics.BasicStrokeWrapper;
import net.java.dev.imagine.api.vector.util.Size;
import org.imagine.awt.GradientManager;
import org.imagine.awt.io.PaintKeyIO;
import org.imagine.awt.key.PaintKey;
import org.imagine.awt.key.TexturedPaintWrapperKey;
import org.imagine.editor.api.AspectRatio;
import org.imagine.editor.api.PaintingStyle;
import org.imagine.editor.api.snap.SnapPointsBuilder;
import com.mastfrog.geometry.Angle;
import com.mastfrog.geometry.Axis;
import com.mastfrog.geometry.CornerAngle;
import com.mastfrog.geometry.EqLine;
import org.imagine.io.KeyReader;
import org.imagine.io.KeyWriter;
import org.imagine.utils.Holder;
import org.imagine.vector.editor.ui.io.VectorIO;
import org.imagine.vector.editor.ui.spi.ShapeControlPoint;
import org.imagine.vector.editor.ui.spi.ShapeInfo;
import org.imagine.vector.editor.ui.spi.ShapeInfo.PointInfo;
import org.openide.util.WeakSet;

/**
 *
 * @author Tim Boudreau
 */
public final class ShapeEntry implements Hibernator, ShapeElement, ControlPointController {

    private static long IDS = idsBase();
    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);
    private static final PaintKey XPAR_KEY = PaintKey.forPaint(TRANSPARENT);
    private PaintKey<?> bg;
    private PaintKey<?> fg;
    private PaintingStyle paintingStyle;
    Shaped vect;
    private Rectangle2D.Double bds = new Rectangle2D.Double();
    private Shape shape;
    private BasicStroke stroke;
    private String name;
    private final long id;
    private final ShapeElementControlPointFactory CPF
            = new ShapeElementControlPointFactory();
    private final RevCache shapeInfoCache = new RevCache(this::item, () -> {
        return ShapeInfo.create(shape());
    });
    private static final byte IO_REV = 1;

    static final long idsBase() {
        long ts = System.currentTimeMillis();
        return (ts / 5);
    }

    public boolean isNameExplicitlySet() {
        return name != null;
    }

    public void writeTo(VectorIO io, KeyWriter writer) throws IOException {
        byte flags = 0;
        if (name != null) {
            flags |= 1;
        }
        if (bg != null && !bg.equals(XPAR_KEY)) {
            flags |= 2;
        }
        if (fg != null && !fg.equals(XPAR_KEY)) {
            flags |= 4;
        }
        if (stroke != null && !stroke.equals(new BasicStroke(1))) {
            flags |= 8;
        }
        writer.writeByte(IO_REV);
        writer.writeByte(flags);
        writer.writeLong(id);
        writer.writeEnum(paintingStyle);
        if ((flags & 2) != 0) {
            PaintKeyIO.write(writer, bg);
        }
        if ((flags & 4) != 0) {
            PaintKeyIO.write(writer, fg);
        }
        if ((flags & 8) != 0) {
            io.writeShape(new BasicStrokeWrapper(stroke), writer);
        }
        io.writeShape(vect, writer);
        if ((flags & 1) != 0) {
            writer.writeString(name);
        }
    }

    public static ShapeEntry read(VectorIO io, KeyReader reader) throws IOException {
        int rev = reader.readByte();
        if (rev != IO_REV) {
            throw new IOException("Unknown io revision " + rev + " expected "
                    + IO_REV);
        }
        byte flags = reader.readByte();
        long id = reader.readLong();
        PaintingStyle style = reader.readEnum(PaintingStyle.class);
        PaintKey<?> bg, fg;
        bg = fg = null;
        if ((flags & 2) != 0) {
            bg = PaintKeyIO.read(reader);
        }
        if ((flags & 4) != 0) {
            fg = PaintKeyIO.read(reader);
        }
        BasicStroke stroke = null;
        if ((flags & 8) != 0) {
            BasicStrokeWrapper strokeWrapper = (BasicStrokeWrapper) io.readShape(reader);
            stroke = strokeWrapper.toStroke();
        } else {
            stroke = new BasicStroke(1);
        }
        Shaped shape = (Shaped) io.readShape(reader);
        String name = null;
        if ((flags & 1) != 0) {
            name = reader.readString();
        }
        return new ShapeEntry(shape, bg, fg, style.isOutline(), style.isFill(), stroke, id, name);
    }

    ShapeEntry(Shaped vector, Paint background, Paint foreground, BasicStroke stroke, boolean draw, boolean fill) {
        id = IDS++;
        if (background != null) {
            bg = PaintKey.forPaint(background);
        }
        if (foreground != null) {
            fg = PaintKey.forPaint(foreground);
        }
        this.vect = vector;
        vect.getBounds(bds);
        this.stroke = stroke;
        this.paintingStyle = PaintingStyle.forDrawAndFill(draw, fill);
    }

    public ShapeEntry(Shaped vector, PaintKey<?> bg, PaintKey<?> fg, boolean draw, boolean fill, BasicStroke stroke, String name) {
        id = IDS++;
        this.vect = vector;
        this.bg = bg;
        this.fg = fg;
        this.stroke = stroke;
        this.paintingStyle = PaintingStyle.forDrawAndFill(draw, fill);
        this.name = name;
    }

    public ShapeEntry(Shaped vector, PaintKey<?> bg, PaintKey<?> fg, PaintingStyle style, BasicStroke stroke, String name) {
        id = IDS++;
        this.vect = vector;
        this.bg = bg;
        this.fg = fg;
        this.stroke = stroke;
        this.paintingStyle = style;
        this.name = name;
    }

    ShapeEntry(Shaped vector, PaintKey<?> bg, PaintKey<?> fg, boolean draw, boolean fill, BasicStroke stroke, long id, String name) {
        this.vect = vector;
        this.bg = bg;
        this.fg = fg;
        this.stroke = stroke;
        this.paintingStyle = PaintingStyle.forDrawAndFill(draw, fill);
        this.id = id;
        this.name = name;
    }

    @Override
    public void addToBounds(Rectangle2D r) {
        vect.addToBounds(r);
    }

    @Override
    public boolean isNameSet() {
        return name != null
                && !name.equals(defaultName());
    }

    @Override
    public Runnable restorableSnapshot() {
        PaintKey<?> obg = bg;
        PaintKey<?> ofg = fg;
        Runnable vectSnap = vect.restorableSnapshot();
        String on = name;
        BasicStroke os = stroke;
        return () -> {
            bg = obg;
            fg = ofg;
            name = on;
            stroke = os;
            vectSnap.run();
            changed();
        };
    }

    @Override
    public Runnable geometrySnapshot() {
        Runnable r = vect.restorableSnapshot();
        return () -> {
            r.run();
            changed();
        };
    }

    @Override
    public String getName() {
        if (name == null) {
            return defaultName();
        }
        return name;
    }

    private String defaultName() {
        return ShapeNames.nameOf(vect) + "-" + Long.toString(id, 36);
    }

    @Override
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            this.name = null;
        } else {
            this.name = name.trim();
        }
    }

    @Override
    public long id() {
        return id;
    }

    @Override
    public void setPaintingStyle(PaintingStyle style) {
        this.paintingStyle = style;
    }

    @Override
    public PaintingStyle getPaintingStyle() {
        return paintingStyle;
    }

    @Override
    public void setFill(Paint fill) {
        bg = fill == null ? null : PaintKey.forPaint(fill);
    }

    @Override
    public void setFill(PaintKey<?> key) {
        bg = key;
    }

    @Override
    public void setDraw(PaintKey<?> draw) {
        fg = draw;
    }

    @Override
    public PaintKey<?> getFillKey() {
        return XPAR_KEY.equals(bg) ? null : bg;
    }

    @Override
    public PaintKey<?> getDrawKey() {
        return XPAR_KEY.equals(fg) ? null : fg;
    }

    @Override
    public void setDraw(Paint draw) {
        fg = draw == null ? null : PaintKey.forPaint(draw);
    }

    @Override
    public Paint getFill() {
        return bg == null ? TRANSPARENT : bg.toPaint();
    }

    @Override
    public Paint getDraw() {
        return fg == null ? TRANSPARENT : fg.toPaint();
    }

    private void updateBoundsFromShape(Shape s) {
        bds.setFrame(s.getBounds2D());
        if (stroke != null) {
            double width = stroke.getLineWidth()
                    + stroke.getMiterLimit();
            double w2 = width * 2;
            bds.x -= width;
            bds.y -= width;
            bds.height += w2;
            bds.width += w2;
        }
    }

    @Override
    public void translate(double x, double y) {
        vect.as(Transformable.class, t -> {
            t.translate(x, y);
            shape = null;
            updateBoundsFromShape(shape());
        });
    }

    @Override
    public void applyTransform(AffineTransform xform) {
        vect.as(Transformable.class, t -> {
            t.applyTransform(xform);
            shape = null;
            updateBoundsFromShape(shape());
        });
    }

    @Override
    public boolean canApplyTransform(AffineTransform xform) {
        Transformable t = vect.as(Transformable.class);
        return t != null && t.canApplyTransform(xform);
    }

    @Override
    public void changed() {
        shape = null;
        updateBoundsFromShape(shape());
    }

    @Override
    public int getControlPointCount() {
        Adjustable adj = vect.as(Adjustable.class);
        if (adj != null) {
            return adj.getControlPointCount();
        }
        return 0;
    }

    private final DelegatingControlPointController ctrllr = new DelegatingControlPointController();

    @Override
    public void changed(ControlPoint pt) {
        changed();
    }

    @Override
    public Size getControlPointSize() {
        return cpSize;
    }

    @Override
    public void deleted(ControlPoint pt) {
        changed();
    }

    private Size cpSize = new Size(3, 3);
    private final Set<Consumer<ShapeControlPoint>> pointConsumers = new WeakSet<>();

    @Override
    public ShapeControlPoint[] controlPoints(double size, Consumer<ShapeControlPoint> c) {
        double newSize = Math.max(cpSize.w, size);
        if (newSize != cpSize.w) {
            cpSize = new Size(newSize, newSize);
        }
        pointConsumers.add(c);
        if (vect instanceof Adjustable) {
            Holder<ShapeControlPoint[]> h = c == null ? null : Holder.create();
            ShapeControlPoint[] result = CPF.getControlPoints(this, new ControlPointController() {
                @Override
                public void changed(ControlPoint pt) {
//                    System.out.println("CHANGED " + pt.getClass().getSimpleName() + " point " + pt.index()
//                        + " of " + pt.family().length + " with shape family of "
//                        + h.get().length + " " + Arrays.toString(h.get()));
                    if (h == null) {
                        return;
                    }
                    ShapeControlPoint[] pts = h.get();
                    // If the point count has changed, the
                    // original array will be out-of-date
                    if (pts.length > 0) {
                        pts = pts[0].family();
                    }
                    if (c != null && pt.index() < pts.length) {
                        c.accept(pts[pt.index()]);
                    }
                    ShapeEntry.this.changed(pt);
                }

                @Override
                public Size getControlPointSize() {
                    return cpSize;
                }

                @Override
                public void deleted(ControlPoint pt) {
                    ShapeEntry.this.changed(pt);
                }
            });
            if (c != null) {
                h.set(result);
            }
            return result == null ? new ShapeControlPoint[0] : result;
        }
        return new ShapeControlPoint[0];
    }

    public boolean contains(Point2D p) {
        if (vect instanceof Volume) {
            Volume v = (Volume) vect;
            if (v.getBounds().contains(p)) {
                return shape().contains(p);
            }
            return false;
        } else {
            return shape().contains(p);
        }
    }

    @Override
    public boolean setStroke(BasicStrokeWrapper wrapper) {
        boolean change = !wrapper.equals(this.stroke);
        if (change) {
            this.stroke = wrapper.toStroke();
            changed();
        }
        return change;
    }

    @Override
    public Shaped item() {
        return vect;
    }

    @Override
    public String toString() {
        return "Sh(" + id() + " "
                + Integer.toString(System.identityHashCode(this), 36)
                + " " + vect.getClass().getSimpleName() + " "
                + paintingStyle + " "
                + vect + ")";
    }

    @Override
    public boolean isPaths() {
        return vect instanceof PathIteratorWrapper;
    }

    @Override
    public void toPaths() {
        if (!(vect instanceof PathIteratorWrapper)) {
            vect = new PathIteratorWrapper(vect.toShape());
            changed();
        }
    }

    void setOutline(Paint paint) {
        if (paint != null) {
            fg = PaintKey.forPaint(paint);
            paintingStyle = paintingStyle.andDrawn();
        }
    }

    @Override
    public Paint fill(AspectRatio ratio) {
        return bg == null ? null : GradientManager.getDefault().findPaint(bg,
                (int) ratio.width(), (int) ratio.height());
    }

    @Override
    public Paint outline(AspectRatio ratio) {
        return fg == null ? null : GradientManager.getDefault().findPaint(fg,
                (int) ratio.width(), (int) ratio.height());
    }

    @Override
    public ShapeEntry copy() {
        return new ShapeEntry(vect.copy(), bg, fg,
                paintingStyle.isOutline(), paintingStyle.isFill(),
                stroke, id, name);
    }

    @Override
    public ShapeEntry duplicate() {
        return new ShapeEntry(vect.copy(), bg, fg,
                paintingStyle,
                stroke, name);
    }

    @Override
    public BasicStroke stroke() {
        return stroke == null ? new BasicStroke(1) : stroke;
    }

    @Override
    public boolean isFill() {
        return paintingStyle.isFill();
    }

    @Override
    public boolean isDraw() {
        return paintingStyle.isOutline();
    }

    @Override
    public void setShape(Shaped shape) {
        if (shape != vect) {
            vect = shape;
            changed();
        }
    }

    @Override
    public ShapeInfo shapeInfo() {
        return shapeInfoCache.get();
    }

    void addTo(SnapPointsBuilder<ShapeSnapPointEntry> bldr) {
        if (vect instanceof Adjustable) {
            Adjustable adj = (Adjustable) vect;
            int cpCount = adj.getControlPointCount();
            double[] pts = new double[cpCount * 2];
            adj.getControlPoints(pts);
            IntSet v = adj.virtualControlPointIndices();
//            int[] virt = adj.getVirtualControlPointIndices();
            for (int i = 0; i < pts.length; i += 2) {
                int ptIx = i / 2;
                if (!v.contains(ptIx)) {
//                if (Arrays.binarySearch(virt, ptIx) < 0) {
                    ShapeSnapPointEntry sn = new ShapeSnapPointEntry(this, ptIx, -1);
                    bldr.add(SnapAxis.X, pts[i], sn);
                    bldr.add(SnapAxis.Y, pts[i + 1], sn);
//                }
                }
            }
        }
        /*if (vect.is(EnhancedShape.class)) {
            vect.as(EnhancedShape.class, (sh) -> {
                sh.visitAnglesWithArcs(new ArcsVisitor() {
                    @Override
                    public void visit(int index, double angle1, double x1, double y1, double angle2,
                            double x2, double y2, Rectangle2D bounds, double apexX, double apexY,
                            double offsetX, double offsetY, double midAngle) {
//                    CornerAngle ang = new CornerAngle(x1, y1, apexX, apexY, x2, y2);
                        CornerAngle ang = new CornerAngle(angle1, angle2);
                        bldr.addCorner(ang.encodeNormalized(),
                                new ShapeSnapPointEntry(ShapeEntry.this, index, index - 1, ang.encodeNormalized()));
                    }
                }, 0);
            });
        } else */ if (!vect.is(Textual.class)) {
            ShapeInfo info = shapeInfo();

            info.forEachPoint((PointInfo pi) -> {
                CornerAngle corner = pi.angle;
                if (!corner.isExtreme()) {
                    // Note:  Every line we receive in a vector as a
                    // leading line, we will receive as the next trailing
                    // line, so we only need to care about the trailing
                    // items here
                    double enc = corner.encode();
                    boolean flipped = !corner.isNormalized();
                    bldr.addCorner(enc, new ShapeSnapPointEntry(
                            ShapeEntry.this,
                            pi.controlPointIndex,
                            pi.nextControlPointIndex,
                            enc));

                    double ext = corner.extent();
                    bldr.addExtent(ext,
                            new ShapeSnapPointEntry(ShapeEntry.this,
                                    pi.previousControlPointIndex,
                                    pi.nextControlPointIndex,
                                    ext));

                    double len = pi.vector.trailingLineLength();
                    if (len > 5) {
                        bldr.addLength(len,
                                new ShapeSnapPointEntry(ShapeEntry.this,
                                        pi.previousControlPointIndex,
                                        pi.controlPointIndex, len));
                    }

                    double trailing = Angle.canonicalize(corner.trailingAngle());
                    bldr.addAngle(trailing,
                            new ShapeSnapPointEntry(ShapeEntry.this,
                                    flipped
                                            ? pi.previousControlPointIndex
                                            : pi.controlPointIndex,
                                    flipped
                                            ? pi.controlPointIndex
                                            : pi.nextControlPointIndex,
                                    trailing));
                }

                EqLine line = pi.vector.trailingLine();
                Axis ax = line.nearestAxis();

                double axDist = line.distanceIn(ax);
                double ayDist = line.distanceIn(ax.opposite());

                if (axDist > 5) {
                    bldr.addDistance(
                            SnapAxis.forVertical(ax.isVertical()),
                            axDist,
                            new ShapeSnapPointEntry(this,
                                    pi.previousControlPointIndex,
                                    pi.controlPointIndex,
                                    axDist));
                }
                if (axDist > 5) {
                    bldr.addDistance(
                            SnapAxis.forVertical(!ax.isVertical()),
                            ayDist,
                            new ShapeSnapPointEntry(this, pi.controlPointIndex,
                                    pi.nextControlPointIndex,
                                    ayDist));
                }

            });
        }
    }

    @Override
    public boolean paint(RenderingGoal goal, Graphics2D g, Rectangle bounds, AspectRatio ratio) {
        Shape sh = shape();
        //            System.out.println("paint shape " + sh);
        if (bds != null && !sh.intersects(bds)) {
//            System.out.println("  does not intersect");
//            return false;
        }
        if (goal.isProduction()) {
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }
        int w = (int) Math.ceil(ratio.width());
        int h = (int) Math.ceil(ratio.height());
        if (vect.is(ImageWrapper.class)) {
            vect.as(ImageWrapper.class).paint(g);
        } else if (vect.is(PathText.class)) {
            if (bg != null) {
                if (goal.isProduction()) {
                    PaintKey<?> bgLocal = bg;
                    if (bgLocal instanceof TexturedPaintWrapperKey<?, ?>) {
                        bgLocal = ((TexturedPaintWrapperKey<?, ?>) bgLocal).delegate();
                    }
                    g.setPaint(bgLocal.toPaint());
                } else {
                    g.setPaint(GradientManager.getDefault().findPaint(bg, w, h));
                }
            }
            if (stroke != null) {
                g.setStroke(stroke);
            }
            vect.as(PathText.class).paint(g);
            if (isDraw()) {
                g.setPaint(goal.isProduction()
                        ? fg.toPaint()
                        : GradientManager.getDefault().findPaint(fg, w, h));
                vect.as(PathText.class).draw(g);
            }
        } else {
            Stroke oldStroke = null;
            if (stroke != null) {
                oldStroke = g.getStroke();
                g.setStroke(stroke);
//                AffineTransform xform = g.getTransform();
//                g.setStroke(GraphicsUtils.createTransformedStroke(stroke, xform));
            }
            if (bg != null) {
                if (goal.isProduction()) {
                    PaintKey<?> bgLocal = bg;
                    if (bgLocal instanceof TexturedPaintWrapperKey<?, ?>) {
                        bgLocal = ((TexturedPaintWrapperKey<?, ?>) bgLocal).delegate();
                    }
                    g.setPaint(bgLocal.toPaint());
                } else {
                    g.setPaint(goal.isProduction()
                            ? bg.toPaint()
                            : GradientManager.getDefault().findPaint(bg, w, h));
                }
            }
            if (isFill()) {
                g.fill(sh);
            }
            if (fg != null) {
                if (goal.isProduction()) {
                    PaintKey<?> fgLocal = fg;
                    if (fgLocal instanceof TexturedPaintWrapperKey<?, ?>) {
                        fgLocal = ((TexturedPaintWrapperKey<?, ?>) fgLocal).delegate();
                    }
                    g.setPaint(fgLocal.toPaint());
                } else {
                    g.setPaint(goal.isProduction()
                            ? fg.toPaint()
                            : GradientManager.getDefault().findPaint(fg, w, h));
                }
            }
            if (isDraw()) {
                g.draw(sh);
            }
            if (oldStroke != null) {
                g.setStroke(oldStroke);
            }
        }
        return true;
    }

    @Override
    public Shape shape() {
        if (shape == null) {
            synchronized (this) {
                if (shape == null) {
                    shape = vect.toShape();
                }
            }
        }
        return shape;
    }

    @Override
    public Rectangle getBounds() {
        Rectangle2D.Double b = bds;
        if (b == null || b.getWidth() == 0 || b.getHeight() == 0) {
            b = bds = new Rectangle2D.Double();
            vect.getBounds(b);
            return b.getBounds();
        }
        return b.getBounds();
    }

    @Override
    public void hibernate() {
        synchronized (this) {
            shape = null;
        }
    }

    @Override
    public void wakeup(boolean immediately, Runnable notify) {
        synchronized (this) {
            shape = vect.toShape();
            updateBoundsFromShape(shape);
        }
    }

    @Override
    public int hashCode() {
        long idx = id * 99371;
        return (int) (idx ^ (idx >> 32));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof ShapeEntry)) {
            return false;
        }
        return id == ((ShapeEntry) obj).id();
    }
}
